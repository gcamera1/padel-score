package com.gonzalocamera.padelcounter.mobile.ui.history

import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.Winner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/** Tope de games por set en la carga manual (7 cubre el 7-5 y el 7-6 del tie-break). */
internal const val MAX_GAMES = 7

/**
 * Borrador de un partido cargado a mano desde el historial.
 *
 * [sets] siempre tiene exactamente [bestOf] filas: se muestran todas y las que quedan
 * en 0-0 se descartan al guardar. [dateMillis] es el mediodía local del día elegido —
 * un partido manual no tiene hora ni duración reales.
 */
internal data class ManualMatchDraft(
    val bestOf: Int = 3,
    val sets: List<Pair<Int, Int>> = List(3) { 0 to 0 },
    val dateMillis: Long,
) {
    val playedSets: List<List<Int>>
        get() = sets
            .filterNot { it.first == 0 && it.second == 0 }
            .map { listOf(it.first, it.second) }

    val mySetsWon: Int get() = playedSets.count { it[0] > it[1] }
    val oppSetsWon: Int get() = playedSets.count { it[1] > it[0] }

    /**
     * Habilita "Guardar". Dos condiciones, a propósito: no se validan reglas de pádel
     * (un 3-2 o un 7-0 se aceptan), solo que haya algo que guardar y un ganador claro.
     */
    val isValid: Boolean get() = playedSets.isNotEmpty() && mySetsWon != oppSetsWon

    /** Cambia el formato conservando lo ya cargado; si achica, las filas sobrantes se pierden. */
    fun withBestOf(n: Int): ManualMatchDraft = copy(
        bestOf = n,
        sets = List(n) { i -> sets.getOrElse(i) { 0 to 0 } },
    )

    fun withSet(index: Int, mine: Int, theirs: Int): ManualMatchDraft = copy(
        sets = sets.mapIndexed { i, set ->
            if (i == index) mine.coerceIn(0, MAX_GAMES) to theirs.coerceIn(0, MAX_GAMES) else set
        },
    )
}

/**
 * Deriva el [Match] persistible. Los campos que la carga manual no pregunta
 * (decider, modo de juego, golpes) quedan en su default y el detalle los oculta.
 */
internal fun ManualMatchDraft.toMatch(id: String = UUID.randomUUID().toString()): Match {
    val score = playedSets
    return Match(
        id = id,
        startedAt = dateMillis,
        finishedAt = dateMillis,
        setsScore = score,
        // Mismo criterio que ScoringViewModel.finalizeMatch.
        tieBreakUsed = score.any { it[0] == MAX_GAMES || it[1] == MAX_GAMES },
        decider = Decider.TB7,
        goldenPoint = false,
        scoringMode = ScoringMode.DEUCE,
        winner = if (mySetsWon > oppSetsWon) Winner.MY else Winner.OPP,
        origin = MatchOrigin.MANUAL,
        bestOf = bestOf,
        strokesPerSet = null,
    )
}

/**
 * El DatePicker de Material3 trabaja en medianoche **UTC**; anclar el partido al
 * mediodía local evita que en husos negativos caiga en el día anterior.
 */
internal fun utcDateMillisToLocalNoon(
    utcMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Long = Instant.ofEpochMilli(utcMillis)
    .atZone(ZoneOffset.UTC)
    .toLocalDate()
    .atTime(12, 0)
    .atZone(zone)
    .toInstant()
    .toEpochMilli()

/** Camino inverso: un instante local → la medianoche UTC que espera el DatePicker. */
internal fun localMillisToUtcMidnight(
    localMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Long = Instant.ofEpochMilli(localMillis)
    .atZone(zone)
    .toLocalDate()
    .atStartOfDay(ZoneOffset.UTC)
    .toInstant()
    .toEpochMilli()

/** Hoy, en el formato que espera `initialSelectedDateMillis` (medianoche UTC). */
internal fun todayUtcMidnight(zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
