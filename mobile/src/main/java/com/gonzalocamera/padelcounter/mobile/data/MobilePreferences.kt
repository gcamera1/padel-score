package com.gonzalocamera.padelcounter.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gonzalocamera.padelcounter.shared.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "mobile_preferences")

data class UserPreferences(
    val keepScreenOn: Boolean = true,
    val courtColor: CourtColorOption = CourtColorOption.BLUE,
    val defaultDecider: Decider = Decider.TB7,
    val defaultScoringMode: ScoringMode = ScoringMode.DEUCE,
    val defaultBestOf: Int = 3,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val category: PadelCategory = PadelCategory.SEXTA,
)

class MobilePreferences(private val context: Context) {

    private object Keys {
        val KEEP_ON = booleanPreferencesKey("keep_screen_on")
        val COURT_COLOR = stringPreferencesKey("court_color")
        val DEFAULT_DECIDER = stringPreferencesKey("default_decider")
        val DEFAULT_GOLDEN = booleanPreferencesKey("default_golden_point") // legacy
        val DEFAULT_SCORING_MODE = stringPreferencesKey("default_scoring_mode")
        val DEFAULT_BEST_OF = intPreferencesKey("default_best_of")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CATEGORY = stringPreferencesKey("category")

        val MY_SETS = intPreferencesKey("cs_my_sets")
        val OPP_SETS = intPreferencesKey("cs_opp_sets")
        val MY_GAMES = intPreferencesKey("cs_my_games")
        val OPP_GAMES = intPreferencesKey("cs_opp_games")
        val MY_POINTS = intPreferencesKey("cs_my_points_idx")
        val OPP_POINTS = intPreferencesKey("cs_opp_points_idx")
        val MY_TB = intPreferencesKey("cs_my_tb_points")
        val OPP_TB = intPreferencesKey("cs_opp_tb_points")
        val IN_TB = booleanPreferencesKey("cs_in_tiebreak")
        val GOLDEN = booleanPreferencesKey("cs_golden_point") // legacy
        val SCORING_MODE = stringPreferencesKey("cs_scoring_mode")
        val DEUCE_COUNT = intPreferencesKey("cs_deuce_count")
        val DECIDER = stringPreferencesKey("cs_decider")
        val COURT = stringPreferencesKey("cs_court_color")
        val KEEP_SCREEN = booleanPreferencesKey("cs_keep_screen_on")
        val IS_SERVE_SET = booleanPreferencesKey("cs_is_serve_set")
        val MY_SERVE = booleanPreferencesKey("cs_my_serve")
        val SERVE_FROM_RIGHT = booleanPreferencesKey("cs_serve_from_right")
        val TB_STARTED_BY_ME = booleanPreferencesKey("cs_tb_started_by_me")
        val BEST_OF = intPreferencesKey("cs_best_of")
        val SETS_HISTORY = stringPreferencesKey("cs_sets_history")
        val HAS_ACTIVE_MATCH = booleanPreferencesKey("cs_has_active_match")
        val MATCH_STARTED_AT = stringPreferencesKey("cs_match_started_at")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            keepScreenOn = prefs[Keys.KEEP_ON] ?: true,
            courtColor = runCatching {
                CourtColorOption.valueOf(prefs[Keys.COURT_COLOR] ?: CourtColorOption.BLUE.name)
            }.getOrDefault(CourtColorOption.BLUE),
            defaultDecider = runCatching {
                Decider.valueOf(prefs[Keys.DEFAULT_DECIDER] ?: Decider.TB7.name)
            }.getOrDefault(Decider.TB7),
            defaultScoringMode = runCatching {
                ScoringMode.valueOf(prefs[Keys.DEFAULT_SCORING_MODE] ?: "")
            }.getOrElse {
                if (prefs[Keys.DEFAULT_GOLDEN] == true) ScoringMode.GOLDEN_POINT
                else ScoringMode.DEUCE
            },
            defaultBestOf = prefs[Keys.DEFAULT_BEST_OF] ?: 3,
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name)
            }.getOrDefault(ThemeMode.DARK),
            category = runCatching {
                PadelCategory.valueOf(prefs[Keys.CATEGORY] ?: PadelCategory.SEXTA.name)
            }.getOrDefault(PadelCategory.SEXTA),
        )
    }

    suspend fun savePreferences(prefs: UserPreferences) {
        context.dataStore.edit { p ->
            p[Keys.KEEP_ON] = prefs.keepScreenOn
            p[Keys.COURT_COLOR] = prefs.courtColor.name
            p[Keys.DEFAULT_DECIDER] = prefs.defaultDecider.name
            p[Keys.DEFAULT_SCORING_MODE] = prefs.defaultScoringMode.name
            p[Keys.DEFAULT_BEST_OF] = prefs.defaultBestOf
            p[Keys.THEME_MODE] = prefs.themeMode.name
            p[Keys.CATEGORY] = prefs.category.name
        }
    }

    val currentState: Flow<PadelState?> = context.dataStore.data.map { prefs ->
        if (prefs[Keys.HAS_ACTIVE_MATCH] != true) return@map null
        PadelState(
            mySets = prefs[Keys.MY_SETS] ?: 0,
            oppSets = prefs[Keys.OPP_SETS] ?: 0,
            myGames = prefs[Keys.MY_GAMES] ?: 0,
            oppGames = prefs[Keys.OPP_GAMES] ?: 0,
            myPointsIdx = prefs[Keys.MY_POINTS] ?: 0,
            oppPointsIdx = prefs[Keys.OPP_POINTS] ?: 0,
            myTbPoints = prefs[Keys.MY_TB] ?: 0,
            oppTbPoints = prefs[Keys.OPP_TB] ?: 0,
            inTieBreak = prefs[Keys.IN_TB] ?: false,
            keepScreenOn = prefs[Keys.KEEP_SCREEN] ?: true,
            scoringMode = runCatching {
                ScoringMode.valueOf(prefs[Keys.SCORING_MODE] ?: "")
            }.getOrElse {
                if (prefs[Keys.GOLDEN] == true) ScoringMode.GOLDEN_POINT
                else ScoringMode.DEUCE
            },
            deuceCount = prefs[Keys.DEUCE_COUNT] ?: 0,
            decider = runCatching {
                Decider.valueOf(prefs[Keys.DECIDER] ?: Decider.TB7.name)
            }.getOrDefault(Decider.TB7),
            courtColor = runCatching {
                CourtColorOption.valueOf(prefs[Keys.COURT] ?: CourtColorOption.BLUE.name)
            }.getOrDefault(CourtColorOption.BLUE),
            isServeSet = prefs[Keys.IS_SERVE_SET] ?: false,
            myServe = prefs[Keys.MY_SERVE] ?: true,
            serveFromRight = prefs[Keys.SERVE_FROM_RIGHT] ?: true,
            tieBreakStartedByMe = prefs[Keys.TB_STARTED_BY_ME] ?: true,
            bestOf = prefs[Keys.BEST_OF] ?: 3,
            setsHistory = deserializeSetsHistory(prefs[Keys.SETS_HISTORY])
        )
    }

    val matchStartedAt: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.MATCH_STARTED_AT]?.toLongOrNull()
    }

    suspend fun saveCurrentState(state: PadelState, startedAt: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_ACTIVE_MATCH] = true
            prefs[Keys.MATCH_STARTED_AT] = startedAt.toString()
            prefs[Keys.MY_SETS] = state.mySets
            prefs[Keys.OPP_SETS] = state.oppSets
            prefs[Keys.MY_GAMES] = state.myGames
            prefs[Keys.OPP_GAMES] = state.oppGames
            prefs[Keys.MY_POINTS] = state.myPointsIdx
            prefs[Keys.OPP_POINTS] = state.oppPointsIdx
            prefs[Keys.MY_TB] = state.myTbPoints
            prefs[Keys.OPP_TB] = state.oppTbPoints
            prefs[Keys.IN_TB] = state.inTieBreak
            prefs[Keys.SCORING_MODE] = state.scoringMode.name
            prefs[Keys.DEUCE_COUNT] = state.deuceCount
            prefs[Keys.DECIDER] = state.decider.name
            prefs[Keys.COURT] = state.courtColor.name
            prefs[Keys.KEEP_SCREEN] = state.keepScreenOn
            prefs[Keys.IS_SERVE_SET] = state.isServeSet
            prefs[Keys.MY_SERVE] = state.myServe
            prefs[Keys.SERVE_FROM_RIGHT] = state.serveFromRight
            prefs[Keys.TB_STARTED_BY_ME] = state.tieBreakStartedByMe
            prefs[Keys.BEST_OF] = state.bestOf
            prefs[Keys.SETS_HISTORY] = Json.encodeToString(state.setsHistory)
        }
    }

    suspend fun clearCurrentState() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_ACTIVE_MATCH] = false
            prefs.remove(Keys.MATCH_STARTED_AT)
            prefs.remove(Keys.SETS_HISTORY)
        }
    }

    private fun deserializeSetsHistory(json: String?): List<List<Int>> {
        if (json.isNullOrBlank()) return emptyList()
        return try { Json.decodeFromString(json) } catch (_: Exception) { emptyList() }
    }
}
