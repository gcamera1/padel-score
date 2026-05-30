package com.gonzalocamera.padelcounter.mobile.ui.scoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonzalocamera.padelcounter.mobile.data.MatchRepository
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.Winner
import com.gonzalocamera.padelcounter.shared.addPointToMy
import com.gonzalocamera.padelcounter.shared.addPointToOpp
import com.gonzalocamera.padelcounter.shared.subtractPointFromMy
import com.gonzalocamera.padelcounter.shared.subtractPointFromOpp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ScoringUiEvent {
    data object MatchSaved : ScoringUiEvent
    data class ShowError(val message: String) : ScoringUiEvent
}

class ScoringViewModel(private val repository: MatchRepository) : ViewModel() {

    private val _state = MutableStateFlow(PadelState())
    val state: StateFlow<PadelState> = _state.asStateFlow()

    private val _matchStartedAt = MutableStateFlow<Long?>(null)

    private val _events = Channel<ScoringUiEvent>(Channel.BUFFERED)
    val events: Flow<ScoringUiEvent> = _events.receiveAsFlow()

    val hasActiveMatch: StateFlow<Boolean> = _state.map { s ->
        s.mySets > 0 || s.oppSets > 0 || s.myGames > 0 || s.oppGames > 0 ||
            s.myPointsIdx > 0 || s.oppPointsIdx > 0 || s.inTieBreak
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var courtColor = CourtColorOption.BLUE
    private var keepScreenOn = true

    init {
        viewModelScope.launch {
            val savedState = repository.currentState.first()
            if (savedState != null) {
                _state.value = savedState.copy(courtColor = courtColor, keepScreenOn = keepScreenOn)
                _matchStartedAt.value = repository.matchStartedAt.first()
            }
        }
        viewModelScope.launch {
            repository.userPreferences.collect { prefs ->
                courtColor = prefs.courtColor
                keepScreenOn = prefs.keepScreenOn
                _state.value = _state.value.copy(
                    courtColor = prefs.courtColor,
                    keepScreenOn = prefs.keepScreenOn,
                )
            }
        }
    }

    fun addPointToMy() = updateState(addPointToMy(_state.value))
    fun addPointToOpp() = updateState(addPointToOpp(_state.value))
    fun subtractPointFromMy() = updateState(subtractPointFromMy(_state.value))
    fun subtractPointFromOpp() = updateState(subtractPointFromOpp(_state.value))

    fun startNewMatch(decider: Decider, scoringMode: ScoringMode, bestOf: Int = 3) {
        val newState = PadelState(
            decider = decider,
            scoringMode = scoringMode,
            bestOf = bestOf,
            courtColor = courtColor,
            keepScreenOn = keepScreenOn,
        )
        _state.value = newState
        _matchStartedAt.value = System.currentTimeMillis()
        persistState()
    }

    fun finalizeMatch(winner: Winner) {
        val s = _state.value
        val startedAt = _matchStartedAt.value ?: System.currentTimeMillis()
        val setsScore = buildSetsScore(s)
        val match = Match(
            id = UUID.randomUUID().toString(),
            startedAt = startedAt,
            finishedAt = System.currentTimeMillis(),
            setsScore = setsScore,
            tieBreakUsed = setsScore.any { set -> set[0] == 7 || set[1] == 7 },
            decider = s.decider,
            goldenPoint = (s.scoringMode == ScoringMode.GOLDEN_POINT),
            scoringMode = s.scoringMode,
            winner = winner,
            origin = MatchOrigin.MOBILE,
            bestOf = s.bestOf,
        )

        viewModelScope.launch {
            try {
                repository.insertMatch(match)
                repository.clearCurrentState()
                _state.value = PadelState(
                    courtColor = courtColor,
                    keepScreenOn = keepScreenOn,
                    decider = s.decider,
                    scoringMode = s.scoringMode,
                    bestOf = s.bestOf,
                )
                _matchStartedAt.value = null
                _events.send(ScoringUiEvent.MatchSaved)
            } catch (e: Exception) {
                _events.send(ScoringUiEvent.ShowError(e.message ?: "Error desconocido"))
            }
        }
    }

    fun retryFinalize(winner: Winner) = finalizeMatch(winner)

    fun discardMatch() {
        val s = _state.value
        viewModelScope.launch { repository.clearCurrentState() }
        _state.value = PadelState(
            courtColor = courtColor,
            keepScreenOn = keepScreenOn,
            decider = s.decider,
            scoringMode = s.scoringMode,
            bestOf = s.bestOf,
        )
        _matchStartedAt.value = null
    }

    fun setServe(myServe: Boolean) {
        updateState(_state.value.copy(isServeSet = true, myServe = myServe, serveFromRight = true))
    }

    private fun updateState(newState: PadelState) {
        _state.value = newState
        persistState()
    }

    private fun persistState() {
        val startedAt = _matchStartedAt.value
            ?: System.currentTimeMillis().also { _matchStartedAt.value = it }
        viewModelScope.launch { repository.saveCurrentState(_state.value, startedAt) }
    }

    private fun buildSetsScore(state: PadelState): List<List<Int>> {
        return state.setsHistory.ifEmpty { listOf(listOf(0, 0)) }
    }
}
