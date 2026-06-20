package com.gonzalocamera.padelcounter.presentation

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenRound
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.ScoringMode
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot tests para la pantalla COUNTER en Wear OS.
 *
 * Run:   ./gradlew :wear:recordPaparazziDebug --tests "*CounterScreenshot*"
 * Output: wear/src/test/snapshots/images/
 */

/** Estado en punto definitorio de Star Point (40-40, deuceCount>=2 → muestra "SP"). */
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

/** Google Pixel Watch (41mm): 450x450px, round */
private val PIXEL_WATCH = DeviceConfig(
    screenWidth = 450, screenHeight = 450,
    density = Density.XXHIGH,
    screenRound = ScreenRound.ROUND
)

class CounterScreenshot_PixelWatch_StarPoint {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PIXEL_WATCH)

    @Test fun starPointDecider() {
        paparazzi.snapshot {
            androidx.wear.compose.material.MaterialTheme {
                CounterScreen(
                    state = STARPOINT_DECIDER_STATE,
                    onSave = {},
                    onUndo = {},
                    onOpenSettings = {}
                )
            }
        }
    }
}
