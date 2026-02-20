package com.gonzalocamera.padelcounter.presentation

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "padel_counter")

enum class CourtColorOption { BLUE, ORANGE, GREEN, PURPLE }
enum class Decider { TB7, SUPER10 }

data class PadelState(
    val mySets: Int = 0,
    val oppSets: Int = 0,

    val myGames: Int = 0,
    val oppGames: Int = 0,

    // points del game normal: idx => 0/15/30/40/(AD)
    val myPointsIdx: Int = 0,
    val oppPointsIdx: Int = 0,

    // tie-break points (solo si inTieBreak=true)
    val myTbPoints: Int = 0,
    val oppTbPoints: Int = 0,
    val inTieBreak: Boolean = false,

    val keepScreenOn: Boolean = true,
    val goldenPoint: Boolean = true, // sin AD
    val decider: Decider = Decider.TB7, // TB cuando 6-6
    val courtColor: CourtColorOption = CourtColorOption.BLUE
)

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
        val GOLDEN = booleanPreferencesKey("golden_point")
        val DECIDER = stringPreferencesKey("decider")
        val COURT = stringPreferencesKey("court_color")
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
            goldenPoint = prefs[Keys.GOLDEN] ?: true,
            decider = runCatching {
                Decider.valueOf(prefs[Keys.DECIDER] ?: Decider.TB7.name)
            }.getOrDefault(Decider.TB7),
            courtColor = runCatching {
                CourtColorOption.valueOf(prefs[Keys.COURT] ?: CourtColorOption.BLUE.name)
            }.getOrDefault(CourtColorOption.BLUE)
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
            prefs[Keys.GOLDEN] = newState.goldenPoint
            prefs[Keys.DECIDER] = newState.decider.name
            prefs[Keys.COURT] = newState.courtColor.name
        }
    }

    suspend fun setKeepScreenOn(on: Boolean) = save(current().copy(keepScreenOn = on))
    suspend fun setGoldenPoint(on: Boolean) = save(current().copy(goldenPoint = on))
    suspend fun setDecider(decider: Decider) = save(current().copy(decider = decider))
    suspend fun setCourtColor(color: CourtColorOption) = save(current().copy(courtColor = color))

    suspend fun resetMatchWithConfig(decider: Decider, goldenPoint: Boolean, courtColor: CourtColorOption) {
        val keepOn = current().keepScreenOn
        save(
            PadelState(
                keepScreenOn = keepOn,
                goldenPoint = goldenPoint,
                decider = decider,
                courtColor = courtColor
            )
        )
    }

    private suspend fun current(): PadelState = stateFlow.first()
}