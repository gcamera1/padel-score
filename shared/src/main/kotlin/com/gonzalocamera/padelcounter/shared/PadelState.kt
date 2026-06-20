package com.gonzalocamera.padelcounter.shared

data class PadelState(
    val mySets: Int = 0,
    val oppSets: Int = 0,
    val myGames: Int = 0,
    val oppGames: Int = 0,
    val myPointsIdx: Int = 0,
    val oppPointsIdx: Int = 0,
    val myTbPoints: Int = 0,
    val oppTbPoints: Int = 0,
    val inTieBreak: Boolean = false,
    val keepScreenOn: Boolean = true,
    val scoringMode: ScoringMode = ScoringMode.DEUCE,
    val deuceCount: Int = 0,
    val decider: Decider = Decider.TB7,
    val courtColor: CourtColorOption = CourtColorOption.BLUE,
    val isServeSet: Boolean = false,
    val myServe: Boolean = true,
    val serveFromRight: Boolean = true,
    val tieBreakStartedByMe: Boolean = true,
    val bestOf: Int = 3,
    val setsHistory: List<List<Int>> = emptyList(),
    val strokeCountingEnabled: Boolean = true,
    val strokeSensitivity: StrokeSensitivity = StrokeSensitivity.MEDIUM
)
