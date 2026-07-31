package com.gonzalocamera.padelcounter.presentation

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenRound
import com.gonzalocamera.padelcounter.presentation.theme.PadelCounterTheme
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.ScoringMode
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot tests para las pantallas de Wear OS.
 *
 * Doble propósito:
 *  1. Regresión visual del layout en relojes chicos y grandes.
 *  2. Fuente de las capturas de la ficha de Google Play (form factor Wear OS).
 *
 * Run:    ./gradlew :wear:recordPaparazziDebug
 * Output: wear/src/test/snapshots/images/
 *
 * Para la ficha de Play NO se suben estos PNG tal cual: tienen las esquinas
 * transparentes (recorte redondo) y Play exige capturas sin alfa (WO-G5).
 * Usar `scripts/wear-store-screenshots.sh`, que graba y aplana sobre negro.
 *
 * Solo se captura `CounterScreen`. Las pantallas construidas con
 * `ScalingLazyColumn` (ajustes, fin de partido, tutorial) renderizan vacías en
 * Paparazzi: el componente necesita una pasada de scroll que el render estático
 * no hace, y forzarla implicaría inyectarle el `ScalingLazyListState` desde
 * afuera solo para el test.
 */

// --- Estados de referencia ---------------------------------------------------

/** Partido en curso: 40-30 con un set ganado. */
private val IN_GAME_STATE = PadelState(
    mySets = 1, oppSets = 0,
    myGames = 3, oppGames = 2,
    myPointsIdx = 3, oppPointsIdx = 2,
    courtColor = CourtColorOption.BLUE,
    isServeSet = true,
    myServe = true,
    setsHistory = listOf(listOf(6, 4))
)

/** Punto definitorio de Star Point (40-40, deuceCount>=2 → muestra "SP"). */
private val STARPOINT_DECIDER_STATE = PadelState(
    mySets = 1, oppSets = 0,
    myGames = 4, oppGames = 5,
    myPointsIdx = 3, oppPointsIdx = 3,
    scoringMode = ScoringMode.STAR_POINT,
    deuceCount = 2,
    courtColor = CourtColorOption.GREEN,
    isServeSet = true,
    myServe = true
)

/** Tie-break a 7, 5-4 arriba. */
private val TIE_BREAK_STATE = PadelState(
    mySets = 1, oppSets = 1,
    myGames = 6, oppGames = 6,
    myTbPoints = 5, oppTbPoints = 4,
    inTieBreak = true,
    decider = Decider.TB7,
    courtColor = CourtColorOption.BLUE,
    isServeSet = true,
    myServe = true,
    setsHistory = listOf(listOf(6, 4), listOf(3, 6))
)

/** Match point en el tercer set, con la cancha en naranja (color configurable). */
private val MATCH_POINT_STATE = PadelState(
    mySets = 1, oppSets = 1,
    myGames = 5, oppGames = 4,
    myPointsIdx = 3, oppPointsIdx = 1,
    courtColor = CourtColorOption.ORANGE,
    isServeSet = true,
    myServe = true,
    setsHistory = listOf(listOf(6, 4), listOf(3, 6))
)

// --- Configs de dispositivo -------------------------------------------------
//
// Lo que fija el layout es el ancho en **dp**, no en px. Renderizamos al doble
// de la resolución del hardware (px y dpi x2) para que las capturas de la ficha
// de Play salgan nítidas sin cambiar un solo dp del layout:
//
//   reloj real            render (2x)                 dp
//   450px @ 320dpi   ->   900px @ 640dpi   ->   225dp  (Pixel Watch, Galaxy Watch 44mm)
//   396px @ 320dpi   ->   792px @ 640dpi   ->   198dp  (Galaxy Watch 40mm — el más
//                                                       chico que soportamos, sobre
//                                                       el mínimo de 192dp de WO-V16)

/** Google Pixel Watch (41mm) / Galaxy Watch 44mm — 225dp, round. */
private val PIXEL_WATCH = DeviceConfig(
    screenWidth = 900, screenHeight = 900,
    density = Density.XXXHIGH,
    screenRound = ScreenRound.ROUND
)

/** Galaxy Watch 40mm — 198dp, round. */
private val GALAXY_WATCH_40MM = DeviceConfig(
    screenWidth = 792, screenHeight = 792,
    density = Density.XXXHIGH,
    screenRound = ScreenRound.ROUND
)

// --- Tests ------------------------------------------------------------------
//
// `maxPercentDifference` sube del 0.1% que trae Paparazzi por default porque el
// `Scaffold` del reloj dibuja un `TimeText` con la hora del sistema: entre grabar
// y verificar cambia el minuto y el diff da ~0.10%, justo por encima del default.
// Con 1% el reloj deja de dar falsos positivos y cualquier cambio real de layout
// —que mueve una fracción mucho mayor de los 900x900 px— sigue fallando.

private const val CLOCK_TOLERANCE = 1.0

class CounterScreenshot_PixelWatch {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = PIXEL_WATCH,
        maxPercentDifference = CLOCK_TOLERANCE
    )

    @Test fun inGame() = paparazzi.snapshotCounter(IN_GAME_STATE)

    @Test fun starPointDecider() = paparazzi.snapshotCounter(STARPOINT_DECIDER_STATE)

    @Test fun tieBreak() = paparazzi.snapshotCounter(TIE_BREAK_STATE)

    @Test fun matchPoint() = paparazzi.snapshotCounter(MATCH_POINT_STATE)
}

class CounterScreenshot_GalaxyWatch40mm {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = GALAXY_WATCH_40MM,
        maxPercentDifference = CLOCK_TOLERANCE
    )

    @Test fun inGame() = paparazzi.snapshotCounter(IN_GAME_STATE)

    @Test fun tieBreak() = paparazzi.snapshotCounter(TIE_BREAK_STATE)
}

// --- Helpers ----------------------------------------------------------------

private fun Paparazzi.snapshotCounter(state: PadelState) = snapshot {
    PadelCounterTheme {
        CounterScreen(
            state = state,
            onSave = {},
            onUndo = {},
            onOpenSettings = {}
        )
    }
}

