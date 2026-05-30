package com.gonzalocamera.padelcounter.shared

import kotlin.math.roundToInt

data class AggregateStats(
    val totalMatches: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val winPercentage: Int? = null,
    val totalSetsWon: Int = 0,
    val totalSetsLost: Int = 0,
    val currentWinStreak: Int = 0,
    val currentLossStreak: Int = 0
) {
    companion object {
        fun fromMatches(matches: List<Match>): AggregateStats {
            if (matches.isEmpty()) return AggregateStats()

            val totalWins = matches.count { it.winner == Winner.MY }
            val totalLosses = matches.count { it.winner == Winner.OPP }
            val n = matches.size

            var setsWon = 0
            var setsLost = 0
            for (m in matches) {
                for (set in m.setsScore) {
                    val my = set[0]
                    val opp = set[1]
                    if (my > opp) setsWon++ else if (opp > my) setsLost++
                }
            }

            val sorted = matches.sortedByDescending { it.finishedAt }
            var winStreak = 0
            var lossStreak = 0
            for (m in sorted) {
                if (m.winner == Winner.MY) {
                    if (lossStreak > 0) break
                    winStreak++
                } else {
                    if (winStreak > 0) break
                    lossStreak++
                }
            }

            return AggregateStats(
                totalMatches = n,
                totalWins = totalWins,
                totalLosses = totalLosses,
                winPercentage = (totalWins * 100.0 / n).roundToInt(),
                totalSetsWon = setsWon,
                totalSetsLost = setsLost,
                currentWinStreak = winStreak,
                currentLossStreak = lossStreak
            )
        }
    }
}
