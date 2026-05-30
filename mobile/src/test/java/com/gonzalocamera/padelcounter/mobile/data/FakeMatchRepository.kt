package com.gonzalocamera.padelcounter.mobile.data

import com.gonzalocamera.padelcounter.shared.AggregateStats
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.MatchSummary
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.Winner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory MatchRepository for unit tests. Drives the ViewModels without Room or DataStore.
 */
class FakeMatchRepository(
    initialMatches: List<Match> = emptyList(),
    initialCurrentState: PadelState? = null,
    initialMatchStartedAt: Long? = null,
    initialPreferences: UserPreferences = UserPreferences(),
) : MatchRepository {

    private val matchesFlow = MutableStateFlow(initialMatches)
    private val currentStateFlow = MutableStateFlow(initialCurrentState)
    private val matchStartedAtFlow = MutableStateFlow(initialMatchStartedAt)
    private val preferencesFlow = MutableStateFlow(initialPreferences)

    /** When non-null, the next insertMatch call throws this exception. */
    var insertFailure: Throwable? = null

    var insertCount = 0
        private set
    var clearCount = 0
        private set
    var savedStates: List<PadelState> = emptyList()
        private set

    override val matchSummaries: Flow<List<MatchSummary>> = matchesFlow.map { list ->
        list.map { match ->
            MatchSummary(
                id = match.id,
                finishedAt = match.finishedAt,
                setsScore = match.setsScore,
                winner = match.winner,
                origin = match.origin,
                bestOf = match.bestOf,
            )
        }
    }

    override val aggregateStats: Flow<AggregateStats> = matchesFlow.map { matches ->
        AggregateStats.fromMatches(matches)
    }

    override val currentState: Flow<PadelState?> = currentStateFlow.asStateFlow()
    override val matchStartedAt: Flow<Long?> = matchStartedAtFlow.asStateFlow()
    override val userPreferences: Flow<UserPreferences> = preferencesFlow.asStateFlow()

    override suspend fun insertMatch(match: Match) {
        insertFailure?.let { throw it }
        insertCount++
        matchesFlow.value = matchesFlow.value + match
    }

    override suspend fun deleteMatch(matchId: String) {
        matchesFlow.value = matchesFlow.value.filterNot { it.id == matchId }
    }

    override suspend fun getMatch(matchId: String): Match? {
        return matchesFlow.value.firstOrNull { it.id == matchId }
    }

    override suspend fun saveCurrentState(state: PadelState, startedAt: Long) {
        currentStateFlow.value = state
        matchStartedAtFlow.value = startedAt
        savedStates = savedStates + state
    }

    override suspend fun clearCurrentState() {
        clearCount++
        currentStateFlow.value = null
        matchStartedAtFlow.value = null
    }

    override suspend fun savePreferences(prefs: UserPreferences) {
        preferencesFlow.value = prefs
    }

    fun seedWinners(winners: List<Winner>, baseTime: Long = 1_700_000_000_000L) {
        matchesFlow.value = winners.mapIndexed { i, w ->
            Match(
                id = "m$i",
                startedAt = baseTime + i * 1000L,
                finishedAt = baseTime + i * 1000L + 100L,
                setsScore = if (w == Winner.MY) listOf(listOf(6, 3)) else listOf(listOf(3, 6)),
                tieBreakUsed = false,
                decider = Decider.TB7,
                winner = w,
                origin = MatchOrigin.MOBILE,
            )
        }
    }
}
