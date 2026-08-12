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
import com.gonzalocamera.padelcounter.shared.StrokeSensitivity

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
        val MATCH_FINISHED_AT = longPreferencesKey("match_finished_at")

        val HAS_SEEN_WALKTHROUGH = booleanPreferencesKey("has_seen_walkthrough")
        val STARTUP_COMPANION_PROMPT_COUNT = intPreferencesKey("startup_companion_prompt_count")
        val MATCHEND_COMPANION_PROMPT_COUNT = intPreferencesKey("matchend_companion_prompt_count")

        val STROKE_ENABLED = booleanPreferencesKey("stroke_counting_enabled")
        val STROKE_SENS = stringPreferencesKey("stroke_sensitivity")
        val STROKE_BACKUP = stringPreferencesKey("stroke_backup")
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
            setsHistory = deserializeSetsHistory(prefs[Keys.SETS_HISTORY]),
            strokeCountingEnabled = prefs[Keys.STROKE_ENABLED] ?: true,
            strokeSensitivity = runCatching {
                StrokeSensitivity.valueOf(prefs[Keys.STROKE_SENS] ?: StrokeSensitivity.MEDIUM.name)
            }.getOrDefault(StrokeSensitivity.MEDIUM)
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
            prefs[Keys.STROKE_ENABLED] = newState.strokeCountingEnabled
            prefs[Keys.STROKE_SENS] = newState.strokeSensitivity.name
        }
    }

    val hasSeenWalkthrough: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_WALKTHROUGH] ?: false
    }

    suspend fun setHasSeenWalkthrough() {
        context.dataStore.edit { prefs -> prefs[Keys.HAS_SEEN_WALKTHROUGH] = true }
    }

    // Contadores de avisos "instalá la app de teléfono" (máx 3 c/u), persistentes
    // fuera de PadelState para que NO se reseteen al empezar un partido nuevo.
    val startupCompanionPromptCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.STARTUP_COMPANION_PROMPT_COUNT] ?: 0
    }

    suspend fun incrementStartupCompanionPromptCount() {
        context.dataStore.edit { prefs ->
            prefs[Keys.STARTUP_COMPANION_PROMPT_COUNT] = (prefs[Keys.STARTUP_COMPANION_PROMPT_COUNT] ?: 0) + 1
        }
    }

    val matchEndCompanionPromptCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MATCHEND_COMPANION_PROMPT_COUNT] ?: 0
    }

    suspend fun incrementMatchEndCompanionPromptCount() {
        context.dataStore.edit { prefs ->
            prefs[Keys.MATCHEND_COMPANION_PROMPT_COUNT] = (prefs[Keys.MATCHEND_COMPANION_PROMPT_COUNT] ?: 0) + 1
        }
    }

    /**
     * Instantes de inicio y fin de un partido terminado.
     *
     * @param firstTime `true` solo la primera vez que este partido se marca como terminado.
     *   En los re-disparos del efecto vale `false`, y ahí **no hay que volver a encolarlo**.
     */
    data class MatchTimestamps(
        val startedAt: Long,
        val finishedAt: Long,
        val firstTime: Boolean,
    )

    /**
     * Fija —y devuelve— los timestamps del partido que acaba de terminar. Es **idempotente**:
     * si ya estaban persistidos, devuelve los mismos valores en vez de recalcularlos.
     *
     * Esto es lo que evita que la duración crezca sola. El efecto que sincroniza el partido
     * se vuelve a disparar en cada arranque en frío mientras el estado tenga un partido
     * terminado sin resetear; con `System.currentTimeMillis()` en el sitio de la llamada,
     * cada reapertura movía `finishedAt` hasta ese momento y `startedAt` se quedaba en el
     * valor original. Así se llegó a ver un partido de 85h 30min: la duración no medía el
     * partido, medía cuánto tardaste en volver a abrir la app.
     *
     * `MATCH_STARTED_AT` se rellena acá también por si falta (partido de una versión
     * anterior); sin él, el id derivado del contenido cambiaría en cada reenvío.
     *
     * El `firstTime` del resultado es la defensa principal: corta el reenvío de raíz. Sin él,
     * el re-disparo encolaría el partido otra vez y —como `StrokeCounter` vive en memoria y
     * un arranque en frío lo deja vacío— ese segundo envío iría **sin los golpes**. Al
     * compartir path con el original, le pisaría los datos al partido en el teléfono si el
     * primer envío todavía no había llegado.
     */
    suspend fun markMatchFinished(): MatchTimestamps {
        val now = System.currentTimeMillis()
        // Se lee antes de escribir: `edit` no informa si la clave ya estaba. No hay carrera
        // real —el único llamador está detrás del guard `matchSynced`— y si la hubiera, el
        // peor caso es no encolar un partido que ya está en la cola.
        val alreadyFinished = context.dataStore.data.first()[Keys.MATCH_FINISHED_AT] != null
        val prefs = context.dataStore.edit { p ->
            if (p[Keys.MATCH_STARTED_AT] == null) p[Keys.MATCH_STARTED_AT] = now
            if (p[Keys.MATCH_FINISHED_AT] == null) p[Keys.MATCH_FINISHED_AT] = now
        }
        return MatchTimestamps(
            startedAt = prefs[Keys.MATCH_STARTED_AT] ?: now,
            finishedAt = prefs[Keys.MATCH_FINISHED_AT] ?: now,
            firstTime = !alreadyFinished,
        )
    }

    suspend fun setKeepScreenOn(on: Boolean) = save(current().copy(keepScreenOn = on))
    suspend fun setScoringMode(mode: ScoringMode) = save(current().copy(scoringMode = mode))
    suspend fun setDecider(decider: Decider) = save(current().copy(decider = decider))
    suspend fun setCourtColor(color: CourtColorOption) = save(current().copy(courtColor = color))
    suspend fun setStrokeCountingEnabled(enabled: Boolean) = save(current().copy(strokeCountingEnabled = enabled))
    suspend fun setStrokeSensitivity(sensitivity: StrokeSensitivity) = save(current().copy(strokeSensitivity = sensitivity))

    suspend fun writeStrokeBackup(perSet: List<Int>) {
        context.dataStore.edit { prefs -> prefs[Keys.STROKE_BACKUP] = Json.encodeToString(perSet) }
    }

    suspend fun readStrokeBackup(): List<Int> {
        val raw = context.dataStore.data.first()[Keys.STROKE_BACKUP]
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { Json.decodeFromString<List<Int>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun clearStrokeBackup() {
        context.dataStore.edit { prefs -> prefs.remove(Keys.STROKE_BACKUP) }
    }

    suspend fun resetMatchWithConfig(decider: Decider, scoringMode: ScoringMode, courtColor: CourtColorOption, bestOf: Int = 3) {
        val prev = current()
        save(
            PadelState(
                keepScreenOn = prev.keepScreenOn,
                scoringMode = scoringMode,
                decider = decider,
                courtColor = courtColor,
                bestOf = bestOf,
                strokeCountingEnabled = prev.strokeCountingEnabled,
                strokeSensitivity = prev.strokeSensitivity
            )
        )
        context.dataStore.edit { prefs ->
            prefs[Keys.MATCH_STARTED_AT] = System.currentTimeMillis()
            // Imprescindible: si el fin del partido anterior quedara persistido, el partido
            // nuevo lo heredaría y se guardaría con una duración negativa o absurda.
            prefs.remove(Keys.MATCH_FINISHED_AT)
        }
        // Conteo de golpes: arrancar de cero para el nuevo partido.
        clearStrokeBackup()
        StrokeCounter.reset()
    }

    private suspend fun current(): PadelState = stateFlow.first()

    private fun deserializeSetsHistory(json: String?): List<List<Int>> {
        if (json.isNullOrBlank()) return emptyList()
        return try { Json.decodeFromString(json) } catch (_: Exception) { emptyList() }
    }
}
