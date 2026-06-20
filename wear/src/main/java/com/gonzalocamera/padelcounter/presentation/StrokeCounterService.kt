package com.gonzalocamera.padelcounter.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.gonzalocamera.padelcounter.R
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.StrokeDetector
import com.gonzalocamera.padelcounter.shared.thresholdMs2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Servicio foreground que detecta golpes con el acelerómetro durante el partido,
 * independiente del estado de la pantalla.
 *
 * - Muestrea el acelerómetro con batching (menos wakeups de CPU; en partido no se
 *   necesita el dato en vivo).
 * - Cada golpe detectado se acumula en [StrokeCounter] en el set actual (derivado de
 *   `PadelState.setsHistory.size`).
 * - Al cerrarse cada game persiste un snapshot del acumulado como respaldo.
 *
 * El ciclo de vida lo controla el Activity (start al iniciar partido, stop al finalizar).
 */
class StrokeCounterService : Service(), SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var repo: PadelRepository
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    @Volatile private var detector: StrokeDetector? = null
    @Volatile private var currentSetIdx: Int = 0
    @Volatile private var lastGamesPlayed: Int = -1
    @Volatile private var lastSensitivityName: String = ""

    override fun onCreate() {
        super.onCreate()
        repo = PadelRepository(applicationContext)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        startAsForeground()

        // Restaurar el acumulado por si el proceso fue recreado a mitad de partido.
        scope.launch {
            val backup = repo.readStrokeBackup()
            if (backup.isNotEmpty()) StrokeCounter.restore(backup)
        }

        // Observar el estado del partido: set actual, sensibilidad y cierres de game.
        scope.launch {
            repo.stateFlow.collectLatest { state -> onStateChanged(state) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (accelerometer == null) {
            // Sin sensor disponible: nada que muestrear (el partido sigue, conteo queda null).
            stopSelf()
            return START_NOT_STICKY
        }
        sensorManager.registerListener(
            this,
            accelerometer,
            SAMPLING_PERIOD_US,
            MAX_REPORT_LATENCY_US
        )
        return START_STICKY
    }

    private fun onStateChanged(state: PadelState) {
        currentSetIdx = state.setsHistory.size

        // Recrear el detector si cambió la sensibilidad.
        if (state.strokeSensitivity.name != lastSensitivityName) {
            lastSensitivityName = state.strokeSensitivity.name
            detector = StrokeDetector(state.strokeSensitivity.thresholdMs2())
        }

        // Snapshot al cerrarse un game (sube el total de games jugados).
        val games = gamesPlayed(state)
        if (lastGamesPlayed >= 0 && games > lastGamesPlayed) {
            val snapshot = StrokeCounter.snapshot()
            scope.launch { repo.writeStrokeBackup(snapshot) }
        }
        lastGamesPlayed = games
    }

    override fun onSensorChanged(event: SensorEvent) {
        val det = detector ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val timestampMs = event.timestamp / 1_000_000L // ns → ms (monotónico)
        if (det.onSample(magnitude, timestampMs)) {
            StrokeCounter.recordStroke(currentSetIdx)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        // Persistir el acumulado final antes de morir.
        val snapshot = StrokeCounter.snapshot()
        scope.launch { repo.writeStrokeBackup(snapshot) }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Contador de golpes",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Contando golpes")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "stroke_counter"
        private const val NOTIF_ID = 4201
        private const val SAMPLING_PERIOD_US = 10_000   // ~100 Hz
        private const val MAX_REPORT_LATENCY_US = 5_000_000 // batching ~5 s

        /** Total de games jugados en el partido (sets cerrados + games del set en curso). */
        fun gamesPlayed(s: PadelState): Int =
            s.setsHistory.sumOf { it.getOrElse(0) { 0 } + it.getOrElse(1) { 0 } } + s.myGames + s.oppGames
    }
}
