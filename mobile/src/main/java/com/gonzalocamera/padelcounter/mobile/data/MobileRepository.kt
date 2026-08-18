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
    override val matchHistory: Flow<List<Match>> = matchDao.observeAll().map { entities ->
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

    override val reviewPromptState: Flow<ReviewPromptState> = preferences.reviewPromptState

    override suspend fun insertMatch(match: Match) {
        val inserted = matchDao.insertIfAbsent(match.toEntity()) != -1L
        // El reloj puede reenviar el mismo partido, así que solo el insert real puntúa:
        // sincronizar dos veces no debe empujar el pedido de calificación.
        if (inserted) {
            preferences.addReviewSignal(
                ReviewPolicy.signalFor(match.origin),
                System.currentTimeMillis(),
            )
        }
    }

    // insertIfAbsent es INSERT OR IGNORE: devuelve -1 cuando el id ya existía, así que
    // importar dos veces el mismo backup no duplica nada.
    override suspend fun insertMatches(matches: List<Match>): Int =
        matches.count { matchDao.insertIfAbsent(it.toEntity()) != -1L }

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

    // Importar un backup no puntúa: restaurar partidos viejos no es uso de la app, así
    // que `insertMatches` no pasa por `addReviewSignal`.

    override suspend fun seedReviewPrompt(now: Long) {
        preferences.seedReviewPrompt(now, matchDao.countAll())
    }

    override suspend fun recordReviewSignal(signal: ReviewSignal, now: Long) {
        preferences.addReviewSignal(signal, now)
    }

    override suspend fun saveReviewPromptState(state: ReviewPromptState) {
        preferences.saveReviewPromptState(state)
    }
}
