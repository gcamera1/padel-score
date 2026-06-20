# Padel Score

Aplicación multi-módulo para registrar puntos, games y sets en partidos de pádel.
Arquitectura **companion**: una app de reloj **Wear OS** (`:wear`) emparejada con una
app de **celular** (`:mobile`), que comparten la lógica de scoring vía `:shared`.
Ambos módulos comparten `applicationId = com.gonzalocamera.padelcounter` y se publican
como un único listado en Play Store.

**Versión actual:** 1.0.0 (versionCode 5)

## Stack

- **Kotlin** (proyecto Gradle multi-módulo)
- **Jetpack Compose** — Compose for Wear OS (`:wear`) + Material3 (`:mobile`)
- **DataStore Preferences** — estado en curso y preferencias
- **Room** — persistencia del historial de partidos (`:mobile`)
- **kotlinx.serialization** — codec JSON para el sync reloj↔celular (`:shared`)
- **Wearable DataClient** — transporte del sync entre dispositivos
- **Testing:** JUnit4 + Truth + Coroutines Test + **Paparazzi** (screenshot tests)

---

## Arquitectura

Tres módulos Gradle (`settings.gradle`: `:shared`, `:mobile`, `:wear`):

### `:shared` — Kotlin/JVM puro (sin dependencias Android)
- `PadelState` — data class inmutable del estado del partido
- `PadelLogic` — funciones puras de scoring: `addPointToMy`, `addPointToOpp`,
  `subtractPointFrom*`, `pointsLabel`, `isStarPointDecider`. Reciben `PadelState`,
  devuelven `PadelState`. Sin side effects.
- `Match` / `MatchSummary` / `AggregateStats` — modelos de dominio de partidos finalizados
  (`Match.strokesPerSet: List<Int>?` — conteo de golpes por set, nullable/backward-compatible)
- `MatchCodec` — serialización JSON (kotlinx.serialization) para el sync reloj↔celular
- `StrokeDetector` — detección pura de golpes por picos de aceleración (umbral + debounce),
  testeable sin Android; `StrokeSensitivity.thresholdMs2()` mapea Alto/Medio/Bajo → umbral
- `Enums` — `Decider`, `CourtColorOption`, `Winner`, `MatchOrigin`, `ScoringMode`, `StrokeSensitivity`

### `:wear` — Wear OS (API 30–34, Compose for Wear)
- Single-activity (`MainActivity.kt`) con pantallas (COUNTER, SETTINGS, NEW_MATCH, TUTORIAL,
  WALKTHROUGH, MATCH_FINISHED, STROKE_TEST) navegadas con `mutableStateOf` +
  `AnimatedVisibility`. Sin ViewModel ni DI.
- `PadelDataStore` — `PadelRepository` envuelve DataStore y expone `PadelState` como `Flow`
- `sync/` — `WearSyncQueue` + `WearSyncSender` encolan partidos finalizados y los envían
  al celular vía Wearable DataClient
- **Contador de golpes:** `StrokeCounterService` (foreground service) muestrea el
  acelerómetro durante el partido y, vía `StrokeDetector` (`:shared`), cuenta golpes
  agrupados por set en `StrokeCounter` (singleton en memoria, respaldo en DataStore por
  game). Al finalizar, el conteo viaja en `Match.strokesPerSet`
- Gestos: tap para sumar, double-tap para restar, swipe-left para Ajustes
- `ScreenMetrics` adapta el layout para pantallas redondas vs cuadradas
  (restricción de safe-area: `fw² + fh² ≤ 1.0`)

### `:mobile` — App de celular (API 26+, Material3 Compose)
- ViewModels + `ViewModelFactory` para DI (manual, sin framework)
- Navigation Compose con bottom nav: Scoring, History, Stats, Settings
- `data/db/` — Room (`PadelDatabase`, `MatchDao`, `MatchEntity`) para el historial
- `data/MobilePreferences` — DataStore para estado en curso y preferencias
- `data/MobileRepository` — repositorio único que coordina Room + DataStore
- `sync/SyncBridgeListener` — recibe partidos del reloj vía Wearable DataClient
- `sync/SyncBridgeClient` — chequea conectividad con el reloj

### Flujo de sync: Reloj → Celular
El reloj finaliza un partido → `WearSyncQueue.enqueue()` → `WearSyncSender.trySendPending()`
envía vía `DataClient` → `SyncBridgeListener` del celular recibe → inserta en Room
vía `MobileRepository`. El payload incluye `Match.strokesPerSet` (conteo de golpes por set,
`null` si el feature está apagado o el sensor no estaba disponible); el celular es el
responsable de derivar métricas a partir de ese dato crudo.

> Nota: el directorio `app/` es un módulo Wear OS **legacy** que ya no forma parte del
> build (no está incluido en `settings.gradle`). El módulo de reloj vigente es `:wear`.

---

## Conceptos de dominio

- **Progresión de puntos:** 0 → 15 → 30 → 40 → Game (índices 0–4 en `myPointsIdx`/`oppPointsIdx`)
- **Modos de scoring (`ScoringMode`):**
  - `DEUCE` — clásico: 40-40 → ventaja (AD) → game
  - `GOLDEN_POINT` — 40-40 → el siguiente punto gana el game (muerte súbita)
  - `STAR_POINT` — híbrido: permite dos ciclos AD/Deuce; agotadas las dos ventajas
    (`deuceCount >= 2`), el siguiente punto a 40-40 define el game
- **Indicador "SP":** badge dorado que aparece en reloj y celular **solo** en el punto
  definitorio de Star Point (`isStarPointDecider`), para avisar que el próximo punto
  cierra el game sí o sí
- **Tie-break:** se activa a 6-6 en games. `TB7` (a 7) o `SUPER10` (a 10), se gana por 2
- **Best-of:** longitud configurable del partido (default: al mejor de 3 sets)
- **Estado inmutable:** siempre se actualiza con `PadelState.copy()`
- **Contador de golpes (reloj):** detección on-device por picos de aceleración
  (`StrokeDetector`: magnitud `√(x²+y²+z²)` sobre umbral + debounce ~350 ms). El reloj
  solo cuenta y agrupa por set (dato crudo); toda la interpretación queda para el celular.
  Sensibilidad ajustable (Alto/Medio/Bajo → umbral), calibrable con el modo "Probar contador".
  Requiere llevar el reloj en la muñeca de la paleta

---

## Reglas de UX (reloj)

**Scoring**
- Tap zona **superior** → punto al rival · Tap zona **inferior** → punto a vos
  (háptica `TextHandleMove`)
- Double-tap → resta punto en la zona correspondiente (háptica `LongPress`)
- Al presionar, la zona se ilumina (verde vos, rojo rival)
- Swipe horizontal → abre Ajustes

**Ajustes**
- Pantalla siempre encendida (toggle, aplica `FLAG_KEEP_SCREEN_ON` en tiempo real)
- Color de cancha: Verde, Naranja, Violeta, Azul (persiste entre partidos)
- Contador de golpes (toggle ON/OFF, global, default ON)
- Sensibilidad del sensor: Alto / Medio / Bajo (default Medio)
- Probar contador: pantalla de calibración con la cancha de fondo, contador en vivo y
  botones ↺ reiniciar / ✕ salir (registra el acelerómetro en tiempo real)

**Nuevo partido**
- Elige desempate (TB7 / SUPER10) y modo de scoring; reinicia puntos/games/sets
  preservando preferencias

**Responsive**
- `ScreenMetrics` adapta padding, fracciones de cancha y tamaños de fuente según
  tamaño y forma (redonda/cuadrada) de la pantalla

---

## Convenciones

- **Texto UI:** español · **Código (identificadores):** inglés
- **Funciones puras** preferidas en `:shared` (sin side effects)
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/) —
  `feat(wear): ...`, `fix(mobile): ...`, `refactor(shared): ...`
- **Versionado:** `PADEL_VERSION_CODE` / `PADEL_VERSION_NAME` en `gradle.properties`,
  compartidos por `:mobile` y `:wear` (el build valida consistencia con `checkVersionConsistency`)

---

## Build & Test

```bash
# Build completo
./gradlew build

# Tests por módulo
./gradlew :shared:test                               # Lógica de scoring + codec (JVM puro, rápido)
./gradlew :shared:test --tests "*PadelLogicTest"     # Solo lógica de scoring
./gradlew :shared:test --tests "*MatchCodecTest"     # Solo el codec de serialización
./gradlew :mobile:test                               # Tests de mobile (ViewModels, etc.)
./gradlew :wear:test                                 # Tests de wear

# Instalar en dispositivo/emulador
./gradlew :wear:installDebug
./gradlew :mobile:installDebug

# Screenshot tests (Paparazzi — sin emulador)
./gradlew :wear:recordPaparazziDebug --tests "*CounterScreenshot*"
./gradlew :mobile:recordPaparazziDebug --tests "*MobileScreenshot*"

# Release (keystore en ~/.gradle/gradle.properties, ver docs/publishing-guide.md)
./gradlew :mobile:bundleRelease :wear:bundleRelease
# Output: mobile/build/outputs/bundle/release/mobile-release.aab
#         wear/build/outputs/bundle/release/wear-release.aab
```

---

## Licencia

MIT
