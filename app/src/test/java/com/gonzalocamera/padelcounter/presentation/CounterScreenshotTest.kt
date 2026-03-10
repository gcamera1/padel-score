package com.gonzalocamera.padelcounter.presentation

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenRound
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot tests for the counter screen on different Wear OS devices.
 *
 * Run:   ./gradlew recordPaparazziDebug --tests "*CounterScreenshot*"
 * Output: app/src/test/snapshots/images/
 */

private val DEFAULT_STATE = PadelState(
    mySets = 1, oppSets = 0,
    myGames = 3, oppGames = 2,
    myPointsIdx = 2, oppPointsIdx = 3, // 30-40
    goldenPoint = false,
    courtColor = CourtColorOption.GREEN
)

private val TIEBREAK_STATE = PadelState(
    mySets = 0, oppSets = 0,
    myGames = 6, oppGames = 6,
    inTieBreak = true, myTbPoints = 5, oppTbPoints = 4,
    courtColor = CourtColorOption.BLUE
)

// ── Device configs for common Wear OS watches ──

/** Samsung Galaxy Watch 4/5 (40mm): 396x396px, round */
private val GALAXY_WATCH_40MM = DeviceConfig(
    screenWidth = 396, screenHeight = 396,
    density = Density.XHIGH, // 320dpi
    screenRound = ScreenRound.ROUND
)

/** Samsung Galaxy Watch 4/5 (44mm): 450x450px, round */
private val GALAXY_WATCH_44MM = DeviceConfig(
    screenWidth = 450, screenHeight = 450,
    density = Density.XHIGH,
    screenRound = ScreenRound.ROUND
)

/** Samsung Galaxy Watch 6 (40mm): 432x432px, round */
private val GALAXY_WATCH6_40MM = DeviceConfig(
    screenWidth = 432, screenHeight = 432,
    density = Density.XHIGH,
    screenRound = ScreenRound.ROUND
)

/** Samsung Galaxy Watch 6 (44mm): 480x480px, round */
private val GALAXY_WATCH6_44MM = DeviceConfig(
    screenWidth = 480, screenHeight = 480,
    density = Density.XHIGH,
    screenRound = ScreenRound.ROUND
)

/** Google Pixel Watch 1/2/3 (41mm): 450x450px, higher density, round */
private val PIXEL_WATCH = DeviceConfig(
    screenWidth = 450, screenHeight = 450,
    density = Density.XXHIGH, // 480dpi
    screenRound = ScreenRound.ROUND
)

/** Google Pixel Watch 3 (45mm): 456x456px, round */
private val PIXEL_WATCH_3_45MM = DeviceConfig(
    screenWidth = 456, screenHeight = 456,
    density = Density.XXHIGH,
    screenRound = ScreenRound.ROUND
)

// ── Test classes (one per device, Paparazzi needs one DeviceConfig per Rule) ──

class CounterScreenshot_GalaxyWatch40mm {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = GALAXY_WATCH_40MM)

    @Test fun normalGame() {
        paparazzi.snapshot { androidx.wear.compose.material.MaterialTheme { CounterScreen(state = DEFAULT_STATE, onSave = {}, onUndo = {}, onOpenSettings = {}) } }
    }
}

class CounterScreenshot_GalaxyWatch44mm {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = GALAXY_WATCH_44MM)

    @Test fun normalGame() {
        paparazzi.snapshot { androidx.wear.compose.material.MaterialTheme { CounterScreen(state = DEFAULT_STATE, onSave = {}, onUndo = {}, onOpenSettings = {}) } }
    }
}

class CounterScreenshot_GalaxyWatch6_40mm {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = GALAXY_WATCH6_40MM)

    @Test fun normalGame() {
        paparazzi.snapshot { androidx.wear.compose.material.MaterialTheme { CounterScreen(state = DEFAULT_STATE, onSave = {}, onUndo = {}, onOpenSettings = {}) } }
    }
}

class CounterScreenshot_GalaxyWatch6_44mm {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = GALAXY_WATCH6_44MM)

    @Test fun normalGame() {
        paparazzi.snapshot { androidx.wear.compose.material.MaterialTheme { CounterScreen(state = DEFAULT_STATE, onSave = {}, onUndo = {}, onOpenSettings = {}) } }
    }
}

class CounterScreenshot_PixelWatch {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PIXEL_WATCH)

    @Test fun normalGame() {
        paparazzi.snapshot { androidx.wear.compose.material.MaterialTheme { CounterScreen(state = DEFAULT_STATE, onSave = {}, onUndo = {}, onOpenSettings = {}) } }
    }

    @Test fun tieBreak() {
        paparazzi.snapshot { androidx.wear.compose.material.MaterialTheme { CounterScreen(state = TIEBREAK_STATE, onSave = {}, onUndo = {}, onOpenSettings = {}) } }
    }
}

class CounterScreenshot_PixelWatch3_45mm {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PIXEL_WATCH_3_45MM)

    @Test fun normalGame() {
        paparazzi.snapshot { androidx.wear.compose.material.MaterialTheme { CounterScreen(state = DEFAULT_STATE, onSave = {}, onUndo = {}, onOpenSettings = {}) } }
    }
}
