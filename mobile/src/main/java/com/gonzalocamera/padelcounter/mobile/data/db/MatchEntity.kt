package com.gonzalocamera.padelcounter.mobile.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gonzalocamera.padelcounter.shared.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val finishedAt: Long,
    val setsScoreJson: String,
    val tieBreakUsed: Boolean,
    val decider: String,
    val goldenPoint: Boolean,
    val scoringMode: String = "DEUCE",
    val winner: String,
    val origin: String,
    val bestOf: Int = 3
)

fun Match.toEntity(): MatchEntity = MatchEntity(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt,
    setsScoreJson = Json.encodeToString(setsScore),
    tieBreakUsed = tieBreakUsed,
    decider = decider.name,
    goldenPoint = (scoringMode == ScoringMode.GOLDEN_POINT),
    scoringMode = scoringMode.name,
    winner = winner.name,
    origin = origin.name,
    bestOf = bestOf
)

fun MatchEntity.toMatch(): Match = Match(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt,
    setsScore = Json.decodeFromString(setsScoreJson),
    tieBreakUsed = tieBreakUsed,
    decider = Decider.valueOf(decider),
    goldenPoint = goldenPoint,
    scoringMode = runCatching { ScoringMode.valueOf(scoringMode) }.getOrDefault(
        if (goldenPoint) ScoringMode.GOLDEN_POINT else ScoringMode.DEUCE
    ),
    winner = Winner.valueOf(winner),
    origin = MatchOrigin.valueOf(origin),
    bestOf = bestOf
)

fun Match.toSummary(): MatchSummary = MatchSummary(
    id = id,
    finishedAt = finishedAt,
    setsScore = setsScore,
    winner = winner,
    origin = origin,
    bestOf = bestOf
)
