package com.gonzalocamera.padelcounter.mobile

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.gonzalocamera.padelcounter.mobile.ui.history.MatchDetailContent
import com.gonzalocamera.padelcounter.mobile.ui.scoring.CourtScreen
import com.gonzalocamera.padelcounter.mobile.ui.scoring.NewMatchSheetContent
import com.gonzalocamera.padelcounter.mobile.ui.stats.StatsContent
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelMobileTheme
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
import com.gonzalocamera.padelcounter.shared.AggregateStats
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.Winner
import org.junit.Rule
import org.junit.Test

// mdpi so screen px == dp: exercises the most common (360dp) and a small (320dp) width.
private val PHONE_360 = DeviceConfig(screenWidth = 360, screenHeight = 760, density = Density.MEDIUM)
private val PHONE_320 = DeviceConfig(screenWidth = 320, screenHeight = 640, density = Density.MEDIUM)

private val MATCH = Match(
    id = "1",
    startedAt = 1716100000000,
    finishedAt = 1716105400000,
    setsScore = listOf(listOf(6, 3), listOf(4, 6), listOf(7, 5)),
    tieBreakUsed = true,
    decider = Decider.TB7,
    scoringMode = ScoringMode.STAR_POINT,
    winner = Winner.MY,
    origin = MatchOrigin.WEAR,
    bestOf = 3,
    strokesPerSet = listOf(175, 104, 106),
)

private val STATS = AggregateStats(
    totalMatches = 15, totalWins = 10, totalLosses = 5, winPercentage = 67,
    totalSetsWon = 24, totalSetsLost = 14, currentWinStreak = 3, currentLossStreak = 0,
)

private val COURT = PadelState(
    mySets = 1, oppSets = 0, myGames = 3, oppGames = 2,
    myPointsIdx = 2, oppPointsIdx = 3, scoringMode = ScoringMode.DEUCE,
    courtColor = CourtColorOption.GREEN, isServeSet = true, myServe = true,
)

class Responsive360 {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PHONE_360)

    @Test fun matchDetail() = paparazzi.snapshot { PadelMobileTheme { MatchDetailContent(match = MATCH) } }
    @Test fun stats() = paparazzi.snapshot { PadelMobileTheme { StatsContent(stats = STATS) } }
    @Test fun court() = paparazzi.snapshot {
        PadelMobileTheme { CourtScreen(state = COURT, onTapMy = {}, onTapOpp = {}, onLongPressMy = {}, onLongPressOpp = {}) }
    }
    @Test fun newMatch() = paparazzi.snapshot {
        PadelMobileTheme {
            androidx.compose.foundation.layout.Box(modifier = Modifier.background(PadelPalette.Card)) {
                NewMatchSheetContent(
                    selectedDecider = Decider.TB7, selectedMode = ScoringMode.GOLDEN_POINT, selectedBestOf = 3,
                    onDeciderChange = {}, onModeChange = {}, onBestOfChange = {}, onConfirm = {},
                )
            }
        }
    }
}

class Responsive320 {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PHONE_320)

    @Test fun matchDetail() = paparazzi.snapshot { PadelMobileTheme { MatchDetailContent(match = MATCH) } }
    @Test fun stats() = paparazzi.snapshot { PadelMobileTheme { StatsContent(stats = STATS) } }
}
