package com.gonzalocamera.padelcounter.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonzalocamera.padelcounter.mobile.data.MatchRepository
import com.gonzalocamera.padelcounter.mobile.data.UserPreferences
import com.gonzalocamera.padelcounter.shared.ArchiveDecodeException
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.MatchArchive
import com.gonzalocamera.padelcounter.shared.PadelCategory
import com.gonzalocamera.padelcounter.shared.ThemeMode
import com.gonzalocamera.padelcounter.shared.decodeArchive
import com.gonzalocamera.padelcounter.shared.encodeArchive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExportResult(val json: String, val matchCount: Int)

sealed interface ImportResult {
    data class Success(val imported: Int, val skipped: Int) : ImportResult
    data class Invalid(val reason: String) : ImportResult
}

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

    /**
     * Serializa el historial completo. Devuelve el texto y no escribe el archivo:
     * el IO lo hace la UI, que es la dueña del `Uri` que eligió el usuario.
     */
    suspend fun exportJson(nowMillis: Long): ExportResult {
        val matches = repository.matchHistory.first()
        return ExportResult(
            json = encodeArchive(MatchArchive(exportedAt = nowMillis, matches = matches)),
            matchCount = matches.size,
        )
    }

    /**
     * Importa un backup **fusionando**: los partidos cuyo id ya existe se ignoran, así
     * que reimportar el mismo archivo es inofensivo y nunca se pierde lo que ya había.
     */
    suspend fun importJson(text: String): ImportResult = try {
        val archive = decodeArchive(text)
        val imported = repository.insertMatches(archive.matches)
        ImportResult.Success(imported = imported, skipped = archive.matches.size - imported)
    } catch (e: ArchiveDecodeException) {
        ImportResult.Invalid(e.message ?: "El archivo no es válido")
    }

    private fun updatePrefs(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            val current = preferences.value
            repository.savePreferences(transform(current))
        }
    }
}
