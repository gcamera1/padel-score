# SPEC — Contador de Golpes (Wear)

**PRD:** [`docs/prd/stroke-counter-wear.md`](../prd/stroke-counter-wear.md)
**Módulos:** `:shared`, `:wear`
**Estado:** Final — listo para implementar
**Fecha:** 2026-06-19

---

## 1. Resumen técnico

Se agrega detección de golpes con el acelerómetro en el reloj. La **lógica de detección es pura** (vive en `:shared`, testeable sin emulador). El `:wear` la alimenta con samples del `SensorManager` desde un **foreground service** durante el partido, acumula el conteo por set en un singleton en memoria (con respaldo en DataStore por game), y al finalizar inyecta `strokesPerSet` en el `Match` que ya se sincroniza vía `WearSyncQueue`.

**Decisión de diseño clave (anti-race):** como el service y el Activity corren en el **mismo proceso**, el conteo en vivo vive en un singleton en memoria (`StrokeCounter`). El Activity lo lee directo al finalizar (sin binding, sin carreras). El DataStore solo es **respaldo** para sobrevivir a la muerte del proceso. El **test mode no usa el service**: registra el sensor directo desde el composable (pantalla activa = no hace falta foreground).

---

## 2. Mapa de cambios

### `:shared` (puro)
| Archivo | Cambio |
|---|---|
| `Enums.kt` | + `enum class StrokeSensitivity { LOW, MEDIUM, HIGH }` |
| `StrokeDetector.kt` *(nuevo)* | Detector puro de picos + mapeo sensibilidad→umbral |
| `Match.kt` | + `val strokesPerSet: List<Int>? = null` |
| `PadelState.kt` | + `strokeCountingEnabled: Boolean = true`, `strokeSensitivity: StrokeSensitivity = StrokeSensitivity.MEDIUM` |

### `:wear`
| Archivo | Cambio |
|---|---|
| `presentation/StrokeCounter.kt` *(nuevo)* | Singleton en memoria con el acumulado por set (`StateFlow<List<Int>>`) |
| `presentation/StrokeCounterService.kt` *(nuevo)* | Foreground service: sensor + detector + acumulación + snapshot por game |
| `presentation/PadelDataStore.kt` | + keys/flows/setters para los 2 settings; respaldo del conteo; preservar settings en `resetMatchWithConfig` |
| `presentation/MainActivity.kt` | Settings (3 opciones), `Screen.STROKE_TEST` + `StrokeTestScreen`, start/stop del service, inyección de `strokesPerSet`, reset al nuevo partido, step en walkthrough |
| `AndroidManifest.xml` | Permisos FGS + sensores + `<service>` con `foregroundServiceType="health"` |

---

## 3. `:shared` — lógica pura

### 3.1 `Enums.kt`
```kotlin
enum class StrokeSensitivity { LOW, MEDIUM, HIGH }
```
Semántica: `HIGH` = más sensible = umbral **bajo** = cuenta más. `LOW` = umbral **alto** = cuenta menos.

### 3.2 `StrokeDetector.kt` (nuevo)
```kotlin
package com.gonzalocamera.padelcounter.shared

/** Umbral de magnitud de aceleración (m/s²) por nivel de sensibilidad.
 *  Calibrados en cancha: "Medio" (50) es el punto de referencia validado. */
fun StrokeSensitivity.thresholdMs2(): Float = when (this) {
    StrokeSensitivity.HIGH   -> 32f   // capta hasta golpes suaves
    StrokeSensitivity.MEDIUM -> 50f   // default (validado en cancha)
    StrokeSensitivity.LOW    -> 68f   // solo golpes fuertes
}

/**
 * Detección de golpes por picos de magnitud con debounce temporal.
 * Puro y testeable. Mantiene estado mínimo (timestamp del último golpe).
 */
class StrokeDetector(
    private val thresholdMs2: Float,
    private val debounceMs: Long = 350L
) {
    private var lastStrokeAt: Long = Long.MIN_VALUE

    /** Devuelve true si [magnitude] (= √(x²+y²+z²) del accel) en [timestampMs]
     *  representa un golpe nuevo (sobre umbral y fuera de la ventana de debounce). */
    fun onSample(magnitude: Float, timestampMs: Long): Boolean {
        if (magnitude < thresholdMs2) return false
        if (timestampMs - lastStrokeAt < debounceMs) return false
        lastStrokeAt = timestampMs
        return true
    }

    fun reset() { lastStrokeAt = Long.MIN_VALUE }
}
```
> Nota: se usa `TYPE_ACCELEROMETER` crudo (incluye gravedad ≈ 9.81 en reposo). Los umbrales están muy por encima de la gravedad y del movimiento normal del brazo, así que no hace falta restar gravedad en v1. Si la calibración lo pide, se ajustan los valores de `thresholdMs2()`.

### 3.3 `Match.kt`
```kotlin
@Serializable
data class Match(
    // ... campos actuales sin tocar ...
    val bestOf: Int = 3,
    val strokesPerSet: List<Int>? = null   // ← nuevo, nullable = backward-compatible
)
```
Backward-compat garantizado por el default `null` + `Json { ignoreUnknownKeys = true }` ya existente en `MatchCodec`.

### 3.4 `PadelState.kt`
```kotlin
data class PadelState(
    // ... campos actuales ...
    val setsHistory: List<List<Int>> = emptyList(),
    val strokeCountingEnabled: Boolean = true,                    // ← nuevo (setting)
    val strokeSensitivity: StrokeSensitivity = StrokeSensitivity.MEDIUM  // ← nuevo (setting)
)
```
> Se siguen los settings dentro de `PadelState` por consistencia con `keepScreenOn`/`courtColor` (patrón existente). **Importante:** como `resetMatchWithConfig` crea un `PadelState` nuevo, debe preservar estos dos (igual que ya preserva `keepScreenOn`). Ver 4.3.

---

## 4. `:wear` — implementación

### 4.1 `StrokeCounter.kt` (nuevo) — acumulado en memoria
```kotlin
object StrokeCounter {
    private val _perSet = MutableStateFlow<List<Int>>(emptyList())
    val perSet: StateFlow<List<Int>> = _perSet

    /** Suma 1 golpe al set [setIdx] (0-based), creciendo la lista si hace falta. */
    fun recordStroke(setIdx: Int) {
        _perSet.update { cur ->
            val list = cur.toMutableList()
            while (list.size <= setIdx) list.add(0)
            list[setIdx] = list[setIdx] + 1
            list
        }
    }

    fun snapshot(): List<Int> = _perSet.value
    fun restore(list: List<Int>) { _perSet.value = list }
    fun reset() { _perSet.value = emptyList() }
}
```

### 4.2 `StrokeCounterService.kt` (nuevo) — foreground service
Responsabilidades:
- `onCreate`: obtener `SensorManager`, restaurar acumulado desde el respaldo (`StrokeCounter.restore(repo.readStrokeBackup())`), arrancar como foreground con notificación mínima ("Contando golpes…").
- Registrar listener de `Sensor.TYPE_ACCELEROMETER`:
  - **partido:** `samplingPeriodUs = SensorManager.SENSOR_DELAY_GAME`, `maxReportLatencyUs = 5_000_000` (batching ~5 s → menos wakeups).
- Crear `StrokeDetector(sensibilidadActual.thresholdMs2())`.
- Observar `PadelRepository.stateFlow` en el scope del service para:
  - mantener `currentSetIdx = state.setsHistory.size`;
  - detectar aumento de **games jugados totales** → persistir snapshot (`repo.writeStrokeBackup(StrokeCounter.snapshot())`).
- En `onSensorChanged`: `mag = sqrt(x²+y²+z²)`; si `detector.onSample(mag, SystemClock.elapsedRealtime())` → `StrokeCounter.recordStroke(currentSetIdx)`.
- `onDestroy`: desregistrar listener; persistir snapshot final.

Games jugados totales (helper):
```kotlin
fun gamesPlayed(s: PadelState): Int =
    s.setsHistory.sumOf { it[0] + it[1] } + s.myGames + s.oppGames
```

> Si la sensibilidad cambia mientras el service corre, el listener observa el state y recrea el `StrokeDetector` con el nuevo umbral.

### 4.3 `PadelDataStore.kt`
- Nuevas keys: `STROKE_ENABLED` (boolean), `STROKE_SENS` (string), `STROKE_BACKUP` (string, JSON del `List<Int>`).
- En `stateFlow`: leer `strokeCountingEnabled` (default `true`) y `strokeSensitivity` (default `MEDIUM`, con `runCatching` como `decider`).
- En `save`: persistir ambos.
- Nuevos setters: `setStrokeCountingEnabled(Boolean)`, `setStrokeSensitivity(StrokeSensitivity)`.
- Respaldo del conteo: `suspend fun writeStrokeBackup(List<Int>)`, `suspend fun readStrokeBackup(): List<Int>`, `suspend fun clearStrokeBackup()`.
- **`resetMatchWithConfig`:** preservar `strokeCountingEnabled` y `strokeSensitivity` (leerlos de `current()` como ya hace con `keepOn`) y llamar `clearStrokeBackup()` + `StrokeCounter.reset()`.

### 4.4 `MainActivity.kt`

**(a) Start/stop del service** — nuevo `LaunchedEffect`:
```kotlin
val matchActive = state.isServeSet && !isMatchFinished(state)
LaunchedEffect(matchActive, state.strokeCountingEnabled) {
    val intent = Intent(context, StrokeCounterService::class.java)
    if (matchActive && state.strokeCountingEnabled) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.stopService(intent)
    }
}
```

**(b) Inyección en el `Match`** — en el bloque `LaunchedEffect(state.mySets, state.oppSets)` (hoy `MainActivity:163`), antes de armar el `Match`:
```kotlin
context.stopService(Intent(context, StrokeCounterService::class.java))
val strokes = if (state.strokeCountingEnabled) StrokeCounter.snapshot().takeIf { it.isNotEmpty() } else null
val match = Match( /* ...campos actuales... */, strokesPerSet = strokes)
syncQueue.enqueue(match)
syncSender.trySendPending()
StrokeCounter.reset(); repo.clearStrokeBackup()
screen = Screen.MATCH_FINISHED
```
> Como el partido termina al cerrarse un game, el último snapshot ya está cubierto; además `snapshot()` lee memoria en vivo, así que no hay pérdida.

**(c) Settings UI** — 3 items nuevos en `SettingsScreen` (mismo estilo que "Pantalla siempre encendida"):
1. `ToggleChip` "Contador de golpes" → `repo.setStrokeCountingEnabled(it)`.
2. Selector Alto/Medio/Bajo (Chip cíclico como el de color, o 3 chips) → `repo.setStrokeSensitivity(...)`.
3. `OutlinedButton` "Probar contador" → `screen = Screen.STROKE_TEST` (solo visible si el contador está ON).

Requiere pasar nuevos callbacks a `SettingsScreen` y agregarlos en su invocación (`MainActivity:223`).

**(d) `Screen.STROKE_TEST` + `StrokeTestScreen`** — nuevo valor en el enum `Screen` y nuevo `AnimatedVisibility`. Composable:
- Fondo: reutiliza `CourtBackgroundVertical` (cancha) a pantalla completa.
- Contador grande centrado (estado local `var count by remember`).
- `DisposableEffect`: registra el accel en **tiempo real** (`SENSOR_DELAY_GAME`, sin batching) con un `StrokeDetector(state.strokeSensitivity.thresholdMs2())`; en cada golpe `count++`; al salir, desregistra. **No toca `StrokeCounter` ni el service.**
- Dos botones circulares inline abajo: **↻** (`count = 0`) y **✕** (`screen = Screen.SETTINGS`).
- Swipe-right vuelve a Settings (consistente con el resto).

**(e) Walkthrough** — agregar un `WalkthroughStep` (en `WalkthroughScreen`, ~`MainActivity:900`):
```kotlin
WalkthroughStep(
    title = "Contador de golpes",
    description = "Contamos tus golpes durante el partido.\nUsá el reloj en la muñeca de la paleta."
)
```

### 4.5 `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />
<uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
```xml
<service
    android:name=".presentation.StrokeCounterService"
    android:exported="false"
    android:foregroundServiceType="health" />
```
> `targetSdk = 34` exige `foregroundServiceType`. Se usa `health` (tracking de actividad física) + su permiso. `HIGH_SAMPLING_RATE_SENSORS` cubre muestreo del accel sobre 200 Hz. `POST_NOTIFICATIONS` es para la notificación del FGS (en Wear el impacto visual es mínimo; el service arranca igual).

---

## 5. Plan de tests

### `:shared` (unitarios, sin emulador)
- **`StrokeDetectorTest`**:
  - Un pico aislado sobre umbral → 1 golpe.
  - Dos picos dentro de `debounceMs` → 1 golpe.
  - Dos picos separados por > `debounceMs` → 2 golpes.
  - Magnitud bajo umbral → 0.
  - `thresholdMs2()`: HIGH < MEDIUM < LOW.
- **`MatchCodecTest`** (extender el existente):
  - `Match` con `strokesPerSet = [40,38]` → round-trip íntegro.
  - JSON viejo sin el campo → decodifica con `strokesPerSet == null` (backward-compat).

### `:wear` (unitarios)
- **`StrokeCounterTest`**: `recordStroke` incrementa el set correcto y crece la lista; `restore`/`snapshot`/`reset`.

### Manual (lo habilita el feature mismo)
- Calibración real con **"Probar contador"**: tandas controladas contra la pared, ajuste de sensibilidad.
- Verificar que con el feature OFF el service no arranca y `strokesPerSet` va `null`.
- Verificar conteo con pantalla apagada (FGS).

---

## 6. Orden de implementación (bloques)

1. **Bloque 1 — `:shared`**: `StrokeSensitivity`, `StrokeDetector` + `thresholdMs2()`, `Match.strokesPerSet`, `PadelState` (2 campos). + tests `StrokeDetectorTest`, `MatchCodecTest`. *(Todo verde y testeable sin reloj.)*
2. **Bloque 2 — estado/persistencia wear**: `StrokeCounter`, keys/flows/setters + respaldo en `PadelDataStore`, preservación en `resetMatchWithConfig`. + `StrokeCounterTest`.
3. **Bloque 3 — service + sync**: `StrokeCounterService`, manifest, start/stop e inyección de `strokesPerSet` en `MainActivity`.
4. **Bloque 4 — UI**: 3 opciones en Settings, `StrokeTestScreen` + navegación, step de walkthrough.

> Tras el Bloque 4 ya podés instalar en el reloj, abrir "Probar contador" y calibrar para tu partido.

## 7. Riesgos técnicos

| Riesgo | Mitigación |
|---|---|
| Umbrales tentativos mal calibrados | Test mode + sensibilidad ajustable; valores fáciles de tocar en `thresholdMs2()` |
| FGS `health` rechazado por policy de Play (a futuro) | Alternativa: `foregroundServiceType="specialUse"` con justificación |
| Batería por muestreo largo | Batching (`maxReportLatencyUs`) en partido; captura solo con feature ON |
| Gravedad en magnitud cruda infla la base | Umbrales calibrados por encima de gravedad+ruido; opción de `TYPE_LINEAR_ACCELERATION` si se necesita |
