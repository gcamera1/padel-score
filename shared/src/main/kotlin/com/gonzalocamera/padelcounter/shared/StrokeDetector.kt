package com.gonzalocamera.padelcounter.shared

/**
 * Umbral de magnitud de aceleración (m/s²) por nivel de sensibilidad.
 *
 * Semántica: HIGH = más sensible = umbral bajo = cuenta más golpes (capta hasta los suaves).
 * LOW = menos sensible = umbral alto = cuenta menos (solo golpes fuertes).
 *
 * Valores calibrados en cancha: "Medio" (50) es el punto de referencia validado.
 */
fun StrokeSensitivity.thresholdMs2(): Float = when (this) {
    StrokeSensitivity.HIGH -> 32f
    StrokeSensitivity.MEDIUM -> 50f
    StrokeSensitivity.LOW -> 68f
}

/**
 * Detección de golpes por picos de magnitud de aceleración con debounce temporal.
 *
 * Función pura y testeable: no depende de Android ni del SensorManager. El reloj le
 * alimenta la magnitud de cada sample (= √(x²+y²+z²) del acelerómetro) junto con su
 * timestamp, y el detector decide si representa un golpe nuevo.
 *
 * Mantiene estado mínimo (timestamp del último golpe) para aplicar el debounce y no
 * contar dos veces los múltiples picos de un mismo swing.
 */
class StrokeDetector(
    private val thresholdMs2: Float,
    private val debounceMs: Long = 350L
) {
    private var lastStrokeAt: Long? = null

    /**
     * Devuelve true si [magnitude] en [timestampMs] (milisegundos monotónicos) representa
     * un golpe nuevo: supera el umbral y cae fuera de la ventana de debounce respecto del
     * golpe anterior.
     */
    fun onSample(magnitude: Float, timestampMs: Long): Boolean {
        if (magnitude < thresholdMs2) return false
        val last = lastStrokeAt
        if (last != null && timestampMs - last < debounceMs) return false
        lastStrokeAt = timestampMs
        return true
    }

    fun reset() {
        lastStrokeAt = null
    }
}
