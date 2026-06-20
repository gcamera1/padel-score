package com.gonzalocamera.padelcounter.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gonzalocamera.padelcounter.mobile.data.UserPreferences
import com.gonzalocamera.padelcounter.mobile.ui.components.MatchCard
import com.gonzalocamera.padelcounter.mobile.ui.history.HistoryScreenContent
import com.gonzalocamera.padelcounter.mobile.ui.history.MatchDetailContent
import com.gonzalocamera.padelcounter.mobile.ui.scoring.CourtScreen
import com.gonzalocamera.padelcounter.mobile.ui.scoring.MatchEndSheet
import com.gonzalocamera.padelcounter.mobile.ui.scoring.NewMatchSheet
import com.gonzalocamera.padelcounter.mobile.ui.scoring.ServeSelectionScreen
import com.gonzalocamera.padelcounter.mobile.ui.settings.SettingsContent
import com.gonzalocamera.padelcounter.mobile.ui.stats.StatsContent
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelMobileTheme
import com.gonzalocamera.padelcounter.shared.AggregateStats
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.MatchSummary
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.Winner
import org.junit.Rule
import org.junit.Test

private val PIXEL_6 = DeviceConfig(
    screenWidth = 1080,
    screenHeight = 2400,
    density = com.android.resources.Density.XXHIGH
)

private val NORMAL_STATE = PadelState(
    mySets = 1, oppSets = 0,
    myGames = 3, oppGames = 2,
    myPointsIdx = 2, oppPointsIdx = 3,
    scoringMode = ScoringMode.DEUCE,
    courtColor = CourtColorOption.GREEN,
    isServeSet = true,
    myServe = true
)

private val TIEBREAK_STATE = PadelState(
    mySets = 0, oppSets = 0,
    myGames = 6, oppGames = 6,
    inTieBreak = true,
    myTbPoints = 5, oppTbPoints = 4,
    courtColor = CourtColorOption.BLUE,
    isServeSet = true,
    myServe = false
)

private val FINISHED_STATE = PadelState(
    mySets = 2, oppSets = 1,
    myGames = 6, oppGames = 4
)

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

private val SAMPLE_MATCH_WIN = Match(
    id = "1",
    startedAt = 1716100000000,
    finishedAt = 1716103600000,
    setsScore = listOf(listOf(6, 3), listOf(4, 6), listOf(7, 5)),
    tieBreakUsed = true,
    decider = Decider.TB7,
    scoringMode = ScoringMode.DEUCE,
    winner = Winner.MY,
    origin = MatchOrigin.MOBILE,
    bestOf = 3
)

private val SAMPLE_MATCH_LOSS = Match(
    id = "2",
    startedAt = 1716000000000,
    finishedAt = 1716003600000,
    setsScore = listOf(listOf(3, 6), listOf(6, 7)),
    tieBreakUsed = true,
    decider = Decider.SUPER10,
    scoringMode = ScoringMode.GOLDEN_POINT,
    winner = Winner.OPP,
    origin = MatchOrigin.WEAR
)

private val SAMPLE_SUMMARY_WIN = MatchSummary(
    id = "1",
    finishedAt = 1716103600000,
    setsScore = listOf(listOf(6, 3), listOf(4, 6), listOf(7, 5)),
    winner = Winner.MY,
    origin = MatchOrigin.MOBILE
)

private val SAMPLE_SUMMARY_LOSS = MatchSummary(
    id = "2",
    finishedAt = 1716003600000,
    setsScore = listOf(listOf(3, 6), listOf(6, 7)),
    winner = Winner.OPP,
    origin = MatchOrigin.WEAR,
    bestOf = 5
)

private val SAMPLE_STATS = AggregateStats(
    totalMatches = 15,
    totalWins = 10,
    totalLosses = 5,
    winPercentage = 67,
    totalSetsWon = 24,
    totalSetsLost = 14,
    currentWinStreak = 3,
    currentLossStreak = 0
)

class MobileScreenshot_Scoring {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PIXEL_6)

    @Test fun courtScreen_normalGame() {
        paparazzi.snapshot {
            PadelMobileTheme {
                CourtScreen(
                    state = NORMAL_STATE,
                    onTapMy = {},
                    onTapOpp = {},
                    onLongPressMy = {},
                    onLongPressOpp = {}
                )
            }
        }
    }

    @Test fun courtScreen_tieBreak() {
        paparazzi.snapshot {
            PadelMobileTheme {
                CourtScreen(
                    state = TIEBREAK_STATE,
                    onTapMy = {},
                    onTapOpp = {},
                    onLongPressMy = {},
                    onLongPressOpp = {}
                )
            }
        }
    }

    @Test fun courtScreen_starPointDecider() {
        paparazzi.snapshot {
            PadelMobileTheme {
                CourtScreen(
                    state = STARPOINT_DECIDER_STATE,
                    onTapMy = {},
                    onTapOpp = {},
                    onLongPressMy = {},
                    onLongPressOpp = {}
                )
            }
        }
    }

    @Test fun courtScreen_orangeCourt() {
        paparazzi.snapshot {
            PadelMobileTheme {
                CourtScreen(
                    state = NORMAL_STATE.copy(courtColor = CourtColorOption.ORANGE),
                    onTapMy = {},
                    onTapOpp = {},
                    onLongPressMy = {},
                    onLongPressOpp = {}
                )
            }
        }
    }

    @Test fun serveSelection() {
        paparazzi.snapshot {
            PadelMobileTheme {
                ServeSelectionScreen(
                    onSelectMyServe = {},
                    onSelectOppServe = {}
                )
            }
        }
    }

    @Test fun matchEndLoss() {
        paparazzi.snapshot {
            PadelMobileTheme {
                MatchEndSheet(
                    state = FINISHED_STATE.copy(mySets = 1, oppSets = 2),
                    won = false,
                    onConfirm = {},
                    onDiscard = {}
                )
            }
        }
    }

    @Test fun matchEndVictory() {
        paparazzi.snapshot {
            PadelMobileTheme {
                MatchEndSheet(
                    state = FINISHED_STATE,
                    won = true,
                    onConfirm = {},
                    onDiscard = {}
                )
            }
        }
    }
}

class MobileScreenshot_History {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PIXEL_6)

    @Test fun emptyHistory() {
        paparazzi.snapshot {
            PadelMobileTheme {
                HistoryScreenContent(
                    matches = emptyList(),
                    totalMatches = 0,
                    winPct = 0,
                    onMatchClick = {},
                    onPlayMatch = {}
                )
            }
        }
    }

    @Test fun historyGrouped() {
        paparazzi.snapshot {
            PadelMobileTheme {
                HistoryScreenContent(
                    matches = listOf(SAMPLE_SUMMARY_WIN, SAMPLE_SUMMARY_LOSS),
                    totalMatches = 2,
                    winPct = 50,
                    onMatchClick = {},
                    onPlayMatch = {}
                )
            }
        }
    }

    @Test fun matchCard_win() {
        paparazzi.snapshot {
            PadelMobileTheme {
                MatchCard(summary = SAMPLE_SUMMARY_WIN, onClick = {})
            }
        }
    }

    @Test fun matchCard_loss() {
        paparazzi.snapshot {
            PadelMobileTheme {
                MatchCard(summary = SAMPLE_SUMMARY_LOSS, onClick = {})
            }
        }
    }
}

class MobileScreenshot_MatchDetail {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PIXEL_6)

    @Test fun matchDetail_win() {
        paparazzi.snapshot {
            PadelMobileTheme {
                MatchDetailContent(match = SAMPLE_MATCH_WIN)
            }
        }
    }

    @Test fun matchDetail_loss() {
        paparazzi.snapshot {
            PadelMobileTheme {
                MatchDetailContent(match = SAMPLE_MATCH_LOSS)
            }
        }
    }
}

class MobileScreenshot_Stats {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PIXEL_6)

    @Test fun statsWithData() {
        paparazzi.snapshot {
            PadelMobileTheme {
                StatsContent(stats = SAMPLE_STATS)
            }
        }
    }

    @Test fun statsEmpty() {
        paparazzi.snapshot {
            PadelMobileTheme {
                StatsContent(stats = AggregateStats())
            }
        }
    }
}

class MobileScreenshot_Settings {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = PIXEL_6)

    @Test fun settingsDefault() {
        paparazzi.snapshot {
            PadelMobileTheme {
                SettingsContent(prefs = UserPreferences())
            }
        }
    }

    @Test fun settingsCustom() {
        paparazzi.snapshot {
            PadelMobileTheme {
                SettingsContent(
                    prefs = UserPreferences(
                        keepScreenOn = false,
                        courtColor = CourtColorOption.PURPLE,
                        defaultDecider = Decider.SUPER10,
                        defaultScoringMode = ScoringMode.GOLDEN_POINT
                    )
                )
            }
        }
    }
}
