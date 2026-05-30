package com.gonzalocamera.padelcounter.mobile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonzalocamera.padelcounter.mobile.data.MatchRepository
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchSummary
import com.gonzalocamera.padelcounter.shared.Winner
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistorySummary(
    val totalMatches: Int,
    val winPct: Int,
) {
    companion object {
        val Empty = HistorySummary(0, 0)
    }
}

class HistoryViewModel(private val repository: MatchRepository) : ViewModel() {

    val matches: StateFlow<List<MatchSummary>> = repository.matchSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aggregateLite: StateFlow<HistorySummary> = matches
        .map { list ->
            if (list.isEmpty()) HistorySummary.Empty
            else {
                val wins = list.count { it.winner == Winner.MY }
                HistorySummary(
                    totalMatches = list.size,
                    winPct = (wins * 100) / list.size,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistorySummary.Empty)

    fun deleteMatch(matchId: String) {
        viewModelScope.launch {
            repository.deleteMatch(matchId)
        }
    }

    suspend fun getMatchDetail(matchId: String): Match? {
        return repository.getMatch(matchId)
    }
}
