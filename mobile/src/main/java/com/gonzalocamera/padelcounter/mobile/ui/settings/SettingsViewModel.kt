package com.gonzalocamera.padelcounter.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonzalocamera.padelcounter.mobile.data.MatchRepository
import com.gonzalocamera.padelcounter.mobile.data.UserPreferences
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.PadelCategory
import com.gonzalocamera.padelcounter.shared.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: MatchRepository) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun setKeepScreenOn(enabled: Boolean) {
        updatePrefs { it.copy(keepScreenOn = enabled) }
    }

    fun setCourtColor(color: CourtColorOption) {
        updatePrefs { it.copy(courtColor = color) }
    }

    fun setThemeMode(mode: ThemeMode) {
        updatePrefs { it.copy(themeMode = mode) }
    }

    fun setCategory(category: PadelCategory) {
        updatePrefs { it.copy(category = category) }
    }

    private fun updatePrefs(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            val current = preferences.value
            repository.savePreferences(transform(current))
        }
    }
}
