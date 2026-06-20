package com.gonzalocamera.padelcounter.mobile.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonzalocamera.padelcounter.mobile.data.MatchRepository
import com.gonzalocamera.padelcounter.shared.AggregateStats
import com.gonzalocamera.padelcounter.shared.StrokeAggregate
import com.gonzalocamera.padelcounter.shared.Winner
import com.gonzalocamera.padelcounter.shared.strokeAggregate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(private val repository: MatchRepository) : ViewModel() {

    val stats: StateFlow<AggregateStats> = repository.aggregateStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AggregateStats())

    val strokeStats: StateFlow<StrokeAggregate> =
        combine(repository.matchHistory, repository.userPreferences) { matches, prefs ->
            strokeAggregate(matches, prefs.category)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StrokeAggregate(0, 0, 0f, null, 0),
        )

    val winLossLast7: StateFlow<List<Winner>> = repository.matchSummaries
        .map { list ->
            list.sortedByDescending { it.finishedAt }
                .take(7)
                .map { it.winner }
                .reversed()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
