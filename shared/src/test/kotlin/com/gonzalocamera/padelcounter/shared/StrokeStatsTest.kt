package com.gonzalocamera.padelcounter.shared

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StrokeStatsTest {

    private fun match(
        setsScore: List<List<Int>>,
        strokesPerSet: List<Int>? = null,
    ) = Match(
        id = "m1",
        startedAt = 0L,
        finishedAt = 1L,
        setsScore = setsScore,
        tieBreakUsed = false,
        decider = Decider.TB7,
        winner = Winner.MY,
        origin = MatchOrigin.WEAR,
        strokesPerSet = strokesPerSet,
    )

    // --- verdict() bordes de banda en 6ta ---

    @Test
    fun `verdict 6ta respects band edges`() {
        val c = PadelCategory.SEXTA
        assertThat(c.verdict(6.4f)).isEqualTo(StrokeVerdict.FRIDGE)
        assertThat(c.verdict(6.5f)).isEqualTo(StrokeVerdict.NORMAL)
        assertThat(c.verdict(11.0f)).isEqualTo(StrokeVerdict.NORMAL)
        assertThat(c.verdict(11.1f)).isEqualTo(StrokeVerdict.HIGH_LOAD)
        assertThat(c.verdict(14.5f)).isEqualTo(StrokeVerdict.HIGH_LOAD)
        assertThat(c.verdict(14.6f)).isEqualTo(StrokeVerdict.MARATHON)
    }

    @Test
    fun `verdict depends on category`() {
        // PGG 7.0 → Normal en 6ta, pero Alto desgaste en 7ma
        assertThat(PadelCategory.SEXTA.verdict(7.0f)).isEqualTo(StrokeVerdict.NORMAL)
        assertThat(PadelCategory.SEPTIMA.verdict(7.0f)).isEqualTo(StrokeVerdict.HIGH_LOAD)
    }

    // --- strokeStats ---

    @Test
    fun `strokeStats null when no data`() {
        val m = match(setsScore = listOf(listOf(6, 4)), strokesPerSet = null)
        assertThat(m.strokeStats(PadelCategory.SEXTA)).isNull()
    }

    @Test
    fun `strokeStats total pgg is total strokes over total games`() {
        // 2 sets: 6-4 (10 games) + 6-3 (9 games) = 19 games; 90+95 = 185 golpes
        val m = match(setsScore = listOf(listOf(6, 4), listOf(6, 3)), strokesPerSet = listOf(90, 95))
        val s = m.strokeStats(PadelCategory.SEXTA)!!
        assertThat(s.totalStrokes).isEqualTo(185)
        assertThat(s.totalGames).isEqualTo(19)
        assertThat(s.pgg).isWithin(0.01f).of(185f / 19f)   // ≈ 9.74 → Normal en 6ta
        assertThat(s.verdict).isEqualTo(StrokeVerdict.NORMAL)
    }

    @Test
    fun `strokeStats per-set verdicts are independent`() {
        // Set 1: 30 golpes / 10 games = 3.0 PGG → Heladera
        // Set 2: 90 golpes / 6 games = 15.0 PGG → Maratón
        val m = match(setsScore = listOf(listOf(6, 4), listOf(6, 0)), strokesPerSet = listOf(30, 90))
        val s = m.strokeStats(PadelCategory.SEXTA)!!
        assertThat(s.perSet).hasSize(2)
        assertThat(s.perSet[0].verdict).isEqualTo(StrokeVerdict.FRIDGE)
        assertThat(s.perSet[1].verdict).isEqualTo(StrokeVerdict.MARATHON)
        assertThat(s.perSet[1].games).isEqualTo(6)
    }

    @Test
    fun `strokeStats handles zero games without crashing`() {
        val m = match(setsScore = listOf(listOf(0, 0)), strokesPerSet = listOf(0))
        val s = m.strokeStats(PadelCategory.SEXTA)!!
        assertThat(s.pgg).isEqualTo(0f)
        assertThat(s.perSet[0].pgg).isEqualTo(0f)
    }

    // --- strokeAggregate ---

    @Test
    fun `strokeAggregate filters matches without data`() {
        val matches = listOf(
            match(listOf(listOf(6, 4)), strokesPerSet = listOf(100)),       // 100 / 10 games
            match(listOf(listOf(6, 3)), strokesPerSet = null),              // sin dato → ignorado
            match(listOf(listOf(6, 2)), strokesPerSet = listOf(80)),        // 80 / 8 games
        )
        val agg = strokeAggregate(matches, PadelCategory.SEXTA)
        assertThat(agg.matchesWithData).isEqualTo(2)
        assertThat(agg.totalStrokes).isEqualTo(180)
        assertThat(agg.maxStrokesInMatch).isEqualTo(100)
        assertThat(agg.avgPgg).isWithin(0.01f).of(180f / 18f)   // 10.0
        assertThat(agg.verdict).isEqualTo(StrokeVerdict.NORMAL)
    }

    @Test
    fun `strokeAggregate empty when no data`() {
        val agg = strokeAggregate(
            listOf(match(listOf(listOf(6, 4)), strokesPerSet = null)),
            PadelCategory.SEXTA,
        )
        assertThat(agg.matchesWithData).isEqualTo(0)
        assertThat(agg.verdict).isNull()
        assertThat(agg.totalStrokes).isEqualTo(0)
    }
}
