package com.gonzalocamera.padelcounter.mobile.data

import com.gonzalocamera.padelcounter.shared.AggregateStats
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchSummary
import com.gonzalocamera.padelcounter.shared.PadelState
import kotlinx.coroutines.flow.Flow

/**
 * Boundary the ViewModels depend on. Production wiring is [MobileRepository];
 * tests provide an in-memory fake.
 */
interface MatchRepository {
    val matchSummaries: Flow<List<MatchSummary>>
    val aggregateStats: Flow<AggregateStats>
    val currentState: Flow<PadelState?>
    val matchStartedAt: Flow<Long?>
    val userPreferences: Flow<UserPreferences>

    suspend fun insertMatch(match: Match)
    suspend fun deleteMatch(matchId: String)
    suspend fun getMatch(matchId: String): Match?
    suspend fun saveCurrentState(state: PadelState, startedAt: Long)
    suspend fun clearCurrentState()
    suspend fun savePreferences(prefs: UserPreferences)
}
