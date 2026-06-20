package com.gonzalocamera.padelcounter.shared

import kotlin.math.roundToInt

/**
 * Lógica pura de interpretación de golpes (etapa 2 — celular).
 *
 * El reloj manda el dato crudo (`Match.strokesPerSet`: golpes del usuario por set).
 * Acá se deriva el PGG (Promedio de Golpes por Game) y se traduce en un veredicto
 * calibrado por categoría. Función pura, testeable sin Android.
 */

/**
 * Banda de veredicto para un [pgg] dado, según los umbrales de la categoría.
 * Bordes (6ta): `<6.5` Fridge · `6.5–11.0` Normal · `11.1–14.5` HighLoad · `>14.5` Marathon.
 */
fun PadelCategory.verdict(pgg: Float): StrokeVerdict = when {
    pgg < b1 -> StrokeVerdict.FRIDGE
    pgg <= b2 -> StrokeVerdict.NORMAL
    pgg <= b3 -> StrokeVerdict.HIGH_LOAD
    else -> StrokeVerdict.MARATHON
}

data class SetStrokeStat(
    val setIndex: Int,
    val strokes: Int,
    val games: Int,
    val pgg: Float,
    val verdict: StrokeVerdict,
)

data class StrokeStats(
    val perSet: List<SetStrokeStat>,
    val totalStrokes: Int,
    val totalGames: Int,
    val pgg: Float,
    val verdict: StrokeVerdict,
)

data class StrokeAggregate(
    val matchesWithData: Int,
    val totalStrokes: Int,
    val avgPgg: Float,
    val verdict: StrokeVerdict?,   // null si no hay datos
    val maxStrokesInMatch: Int,
)

private fun pggOf(strokes: Int, games: Int): Float =
    if (games > 0) strokes.toFloat() / games else 0f

private fun gamesInSet(set: List<Int>): Int = set[0] + set[1]

/** Métricas de golpes del partido. `null` si no hay dato (`strokesPerSet == null`). */
fun Match.strokeStats(category: PadelCategory): StrokeStats? {
    val strokes = strokesPerSet ?: return null
    val perSet = strokes.mapIndexed { i, s ->
        val games = setsScore.getOrNull(i)?.let { gamesInSet(it) } ?: 0
        val pgg = pggOf(s, games)
        SetStrokeStat(i, s, games, pgg, category.verdict(pgg))
    }
    val totalStrokes = strokes.sum()
    val totalGames = setsScore.sumOf { gamesInSet(it) }
    val pgg = pggOf(totalStrokes, totalGames)
    return StrokeStats(perSet, totalStrokes, totalGames, pgg, category.verdict(pgg))
}

data class StrokeEstimate(
    val totalStrokes: Int,
    val pgg: Float,
)

/**
 * Estimación manual de golpes (calculadora). Pura.
 * @param involvement fracción 0..1 (ej. 0.25 = 25%).
 *
 * Nota: el PGG no depende de [games] (se cancela); games solo escala el total.
 */
fun estimateStrokes(
    games: Int,
    intensity: PointIntensity,
    involvement: Float,
    category: PadelCategory,
): StrokeEstimate {
    val totalPoints = games * intensity.pointsPerGame
    val totalShots = totalPoints * category.shotsPerPoint
    val yourStrokes = (totalShots * involvement).roundToInt()
    val pgg = if (games > 0) yourStrokes.toFloat() / games else 0f
    return StrokeEstimate(yourStrokes, pgg)
}

/** Agregado histórico sobre los partidos que tienen dato de golpes. */
fun strokeAggregate(matches: List<Match>, category: PadelCategory): StrokeAggregate {
    val withData = matches.filter { it.strokesPerSet != null }
    if (withData.isEmpty()) return StrokeAggregate(0, 0, 0f, null, 0)
    val totalStrokes = withData.sumOf { it.strokesPerSet!!.sum() }
    val totalGames = withData.sumOf { m -> m.setsScore.sumOf { gamesInSet(it) } }
    val avgPgg = pggOf(totalStrokes, totalGames)
    val maxInMatch = withData.maxOf { it.strokesPerSet!!.sum() }
    return StrokeAggregate(withData.size, totalStrokes, avgPgg, category.verdict(avgPgg), maxInMatch)
}
