package com.gonzalocamera.padelcounter.shared

import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long,
    val setsScore: List<List<Int>>,
    val tieBreakUsed: Boolean,
    val decider: Decider,
    val goldenPoint: Boolean = false,
    val scoringMode: ScoringMode = ScoringMode.DEUCE,
    val winner: Winner,
    val origin: MatchOrigin,
    val bestOf: Int = 3,
    val strokesPerSet: List<Int>? = null
)
