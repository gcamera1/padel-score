package com.gonzalocamera.padelcounter.mobile.data

import com.gonzalocamera.padelcounter.mobile.data.db.MatchDao
import com.gonzalocamera.padelcounter.mobile.data.db.toEntity
import com.gonzalocamera.padelcounter.mobile.data.db.toMatch
import com.gonzalocamera.padelcounter.mobile.data.db.toSummary
import com.gonzalocamera.padelcounter.shared.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MobileRepository(
    private val matchDao: MatchDao,
    private val preferences: MobilePreferences,
) : MatchRepository {
    val matchHistory: Flow<List<Match>> = matchDao.observeAll().map { entities ->
        entities.map { it.toMatch() }
    }

    override val matchSummaries: Flow<List<MatchSummary>> = matchHistory.map { matches ->
        matches.map { it.toSummary() }
    }

    override val aggregateStats: Flow<AggregateStats> = matchHistory.map { matches ->
        AggregateStats.fromMatches(matches)
    }

    override val currentState: Flow<PadelState?> = preferences.currentState

    override val matchStartedAt: Flow<Long?> = preferences.matchStartedAt

    override val userPreferences: Flow<UserPreferences> = preferences.userPreferences

    override suspend fun insertMatch(match: Match) {
        matchDao.insertIfAbsent(match.toEntity())
    }

    override suspend fun deleteMatch(matchId: String) {
        matchDao.deleteById(matchId)
    }

    override suspend fun getMatch(matchId: String): Match? {
        return matchDao.getById(matchId)?.toMatch()
    }

    override suspend fun saveCurrentState(state: PadelState, startedAt: Long) {
        preferences.saveCurrentState(state, startedAt)
    }

    override suspend fun clearCurrentState() {
        preferences.clearCurrentState()
    }

    override suspend fun savePreferences(prefs: UserPreferences) {
        preferences.savePreferences(prefs)
    }
}
