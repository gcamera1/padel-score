package com.gonzalocamera.padelcounter.mobile.data

import com.gonzalocamera.padelcounter.shared.AggregateStats
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchSummary
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.ReviewPromptState
import com.gonzalocamera.padelcounter.shared.ReviewSignal
import kotlinx.coroutines.flow.Flow

/**
 * Boundary the ViewModels depend on. Production wiring is [MobileRepository];
 * tests provide an in-memory fake.
 */
interface MatchRepository {
    val matchHistory: Flow<List<Match>>
    val matchSummaries: Flow<List<MatchSummary>>
    val aggregateStats: Flow<AggregateStats>
    val currentState: Flow<PadelState?>
    val matchStartedAt: Flow<Long?>
    val userPreferences: Flow<UserPreferences>
    val reviewPromptState: Flow<ReviewPromptState>

    suspend fun insertMatch(match: Match)

    /** Inserta los que falten (por id) y devuelve cuántos entraron realmente. */
    suspend fun insertMatches(matches: List<Match>): Int
    suspend fun deleteMatch(matchId: String)
    suspend fun getMatch(matchId: String): Match?
    suspend fun saveCurrentState(state: PadelState, startedAt: Long)
    suspend fun clearCurrentState()
    suspend fun savePreferences(prefs: UserPreferences)

    /** Ancla temporal + puntaje inicial del pedido de calificación. Idempotente. */
    suspend fun seedReviewPrompt(now: Long)
    suspend fun recordReviewSignal(signal: ReviewSignal, now: Long)
    suspend fun saveReviewPromptState(state: ReviewPromptState)
}
