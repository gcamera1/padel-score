package com.gonzalocamera.padelcounter.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gonzalocamera.padelcounter.mobile.data.MatchRepository
import com.gonzalocamera.padelcounter.mobile.ui.history.HistoryViewModel
import com.gonzalocamera.padelcounter.mobile.ui.scoring.ScoringViewModel
import com.gonzalocamera.padelcounter.mobile.ui.settings.SettingsViewModel
import com.gonzalocamera.padelcounter.mobile.ui.stats.StatsViewModel

class ViewModelFactory(private val repository: MatchRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ScoringViewModel::class.java) -> ScoringViewModel(repository)
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(repository)
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(repository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository)
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        } as T
    }
}
