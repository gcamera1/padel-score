package com.gonzalocamera.padelcounter.shared

data class MatchSummary(
    val id: String,
    val finishedAt: Long,
    val setsScore: List<List<Int>>,
    val winner: Winner,
    val origin: MatchOrigin,
    val bestOf: Int = 3
)
