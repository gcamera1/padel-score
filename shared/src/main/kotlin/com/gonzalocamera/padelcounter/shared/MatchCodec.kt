package com.gonzalocamera.padelcounter.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MatchDecodeException(message: String) : Exception(message)

private val json = Json { ignoreUnknownKeys = true }

fun encodeMatch(match: Match): ByteArray {
    return json.encodeToString(match).toByteArray(Charsets.UTF_8)
}

fun decodeMatch(bytes: ByteArray): Match {
    if (bytes.isEmpty()) throw MatchDecodeException("Empty payload")
    val str = bytes.toString(Charsets.UTF_8)
    return try {
        val match = json.decodeFromString<Match>(str)
        if (match.scoringMode == ScoringMode.DEUCE && match.goldenPoint) {
            match.copy(scoringMode = ScoringMode.GOLDEN_POINT)
        } else {
            match
        }
    } catch (e: Exception) {
        throw MatchDecodeException("Invalid match data: ${e.message}")
    }
}
