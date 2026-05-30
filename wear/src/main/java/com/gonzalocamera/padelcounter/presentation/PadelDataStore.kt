package com.gonzalocamera.padelcounter.presentation

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.ScoringMode

private val Context.dataStore by preferencesDataStore(name = "padel_counter")

class PadelRepository(private val context: Context) {

    private object Keys {
        val MY_SETS = intPreferencesKey("my_sets")
        val OPP_SETS = intPreferencesKey("opp_sets")
        val MY_GAMES = intPreferencesKey("my_games")
        val OPP_GAMES = intPreferencesKey("opp_games")
        val MY_POINTS = intPreferencesKey("my_points_idx")
        val OPP_POINTS = intPreferencesKey("opp_points_idx")
        val MY_TB = intPreferencesKey("my_tb_points")
        val OPP_TB = intPreferencesKey("opp_tb_points")
        val IN_TB = booleanPreferencesKey("in_tiebreak")

        val KEEP_ON = booleanPreferencesKey("keep_screen_on")
        val GOLDEN = booleanPreferencesKey("golden_point") // legacy
        val SCORING_MODE = stringPreferencesKey("scoring_mode")
        val DEUCE_COUNT = intPreferencesKey("deuce_count")
        val DECIDER = stringPreferencesKey("decider")
        val COURT = stringPreferencesKey("court_color")

        val IS_SERVE_SET = booleanPreferencesKey("is_serve_set")
        val MY_SERVE = booleanPreferencesKey("my_serve")
        val SERVE_FROM_RIGHT = booleanPreferencesKey("serve_from_right")
        val TB_STARTED_BY_ME = booleanPreferencesKey("tb_started_by_me")
        val BEST_OF = intPreferencesKey("best_of")
        val SETS_HISTORY = stringPreferencesKey("sets_history")
        val MATCH_STARTED_AT = longPreferencesKey("match_started_at")

        val HAS_SEEN_WALKTHROUGH = booleanPreferencesKey("has_seen_walkthrough")
    }

    val stateFlow: Flow<PadelState> = context.dataStore.data.map { prefs ->
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
            keepScreenOn = prefs[Keys.KEEP_ON] ?: true,
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

    suspend fun save(newState: PadelState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MY_SETS] = newState.mySets
            prefs[Keys.OPP_SETS] = newState.oppSets
            prefs[Keys.MY_GAMES] = newState.myGames
            prefs[Keys.OPP_GAMES] = newState.oppGames
            prefs[Keys.MY_POINTS] = newState.myPointsIdx
            prefs[Keys.OPP_POINTS] = newState.oppPointsIdx
            prefs[Keys.MY_TB] = newState.myTbPoints
            prefs[Keys.OPP_TB] = newState.oppTbPoints
            prefs[Keys.IN_TB] = newState.inTieBreak

            prefs[Keys.KEEP_ON] = newState.keepScreenOn
            prefs[Keys.SCORING_MODE] = newState.scoringMode.name
            prefs[Keys.DEUCE_COUNT] = newState.deuceCount
            prefs[Keys.DECIDER] = newState.decider.name
            prefs[Keys.COURT] = newState.courtColor.name

            prefs[Keys.IS_SERVE_SET] = newState.isServeSet
            prefs[Keys.MY_SERVE] = newState.myServe
            prefs[Keys.SERVE_FROM_RIGHT] = newState.serveFromRight
            prefs[Keys.TB_STARTED_BY_ME] = newState.tieBreakStartedByMe
            prefs[Keys.BEST_OF] = newState.bestOf
            prefs[Keys.SETS_HISTORY] = Json.encodeToString(newState.setsHistory)
        }
    }

    val hasSeenWalkthrough: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_WALKTHROUGH] ?: false
    }

    suspend fun setHasSeenWalkthrough() {
        context.dataStore.edit { prefs -> prefs[Keys.HAS_SEEN_WALKTHROUGH] = true }
    }

    val matchStartedAt: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.MATCH_STARTED_AT]
    }

    suspend fun setKeepScreenOn(on: Boolean) = save(current().copy(keepScreenOn = on))
    suspend fun setScoringMode(mode: ScoringMode) = save(current().copy(scoringMode = mode))
    suspend fun setDecider(decider: Decider) = save(current().copy(decider = decider))
    suspend fun setCourtColor(color: CourtColorOption) = save(current().copy(courtColor = color))

    suspend fun resetMatchWithConfig(decider: Decider, scoringMode: ScoringMode, courtColor: CourtColorOption, bestOf: Int = 3) {
        val keepOn = current().keepScreenOn
        save(
            PadelState(
                keepScreenOn = keepOn,
                scoringMode = scoringMode,
                decider = decider,
                courtColor = courtColor,
                bestOf = bestOf
            )
        )
        context.dataStore.edit { prefs ->
            prefs[Keys.MATCH_STARTED_AT] = System.currentTimeMillis()
        }
    }

    private suspend fun current(): PadelState = stateFlow.first()

    private fun deserializeSetsHistory(json: String?): List<List<Int>> {
        if (json.isNullOrBlank()) return emptyList()
        return try { Json.decodeFromString(json) } catch (_: Exception) { emptyList() }
    }
}
