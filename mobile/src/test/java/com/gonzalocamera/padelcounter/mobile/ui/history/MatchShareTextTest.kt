package com.gonzalocamera.padelcounter.mobile.ui.history

import com.google.common.truth.Truth.assertThat
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.PadelCategory
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.Winner
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

/** 2024-05-19 12:00 UTC-3 */
private const val MAY_19 = 1716130800000L

class MatchShareTextTest {

    private lateinit var originalTimeZone: TimeZone
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        originalLocale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
        Locale.setDefault(originalLocale)
    }

    private fun match(
        setsScore: List<List<Int>> = listOf(listOf(6, 4), listOf(6, 3)),
        winner: Winner = Winner.MY,
        bestOf: Int = 3,
        strokesPerSet: List<Int>? = null,
        origin: MatchOrigin = MatchOrigin.WEAR,
        scoringMode: ScoringMode = ScoringMode.DEUCE,
        decider: Decider = Decider.TB7,
        tieBreakUsed: Boolean = false,
        durationMinutes: Long = 107,
    ) = Match(
        id = "m1",
        startedAt = MAY_19,
        finishedAt = MAY_19 + durationMinutes * 60_000,
        setsScore = setsScore,
        tieBreakUsed = tieBreakUsed,
        decider = decider,
        scoringMode = scoringMode,
        winner = winner,
        origin = origin,
        bestOf = bestOf,
        strokesPerSet = strokesPerSet,
    )

    @Test
    fun `win without stroke data`() {
        val text = match().toShareText(PadelCategory.SEXTA)

        assertThat(text).isEqualTo(
            """
            🎾 *VICTORIA* 6-4 6-3

            🥇 Set 1: *6-4*
            🥇 Set 2: *6-3*

            📅 19/05/2024 · 1h 47min
            ⚙️ Al mejor de 3 sets · Deuce / Ventaja · Tie-break a 7

            _Simple Padel Score_
            """.trimIndent()
        )
    }

    @Test
    fun `loss says DERROTA and marks the lost sets with silver`() {
        val text = match(
            setsScore = listOf(listOf(4, 6), listOf(6, 3), listOf(2, 6)),
            winner = Winner.OPP,
        ).toShareText(PadelCategory.SEXTA)

        assertThat(text).startsWith("🎾 *DERROTA* 4-6 6-3 2-6")
        assertThat(text).contains("🥈 Set 1: *4-6*")
        assertThat(text).contains("🥇 Set 2: *6-3*")
        assertThat(text).contains("🥈 Set 3: *2-6*")
    }

    @Test
    fun `includes the stroke block when the watch sent data`() {
        val text = match(strokesPerSet = listOf(175, 104)).toShareText(PadelCategory.SEXTA)

        // 279 golpes / 19 games = 14.7 PGG -> MARATHON en SEXTA (b3 = 14.5)
        assertThat(text).contains("💪 *279 golpes* · 14.7 PGG")
        assertThat(text).contains("🦸 Maratón")
    }

    @Test
    fun `omits the stroke block without data`() {
        val text = match(strokesPerSet = null).toShareText(PadelCategory.SEXTA)

        assertThat(text).doesNotContain("golpes")
        assertThat(text).doesNotContain("PGG")
    }

    @Test
    fun `verdict follows the chosen category`() {
        val withStrokes = match(strokesPerSet = listOf(70, 63))

        // 133 golpes / 19 games = 7.0 PGG: alto desgaste en SEPTIMA (banda 6.5-8.5),
        // pero normal en SEXTA (banda 6.5-11.0). El veredicto no se persiste: se
        // recalcula con la categoría elegida en Ajustes.
        assertThat(withStrokes.toShareText(PadelCategory.SEPTIMA)).contains("🔨 Alto desgaste")
        assertThat(withStrokes.toShareText(PadelCategory.SEXTA)).contains("⚖️ Normal")
    }

    @Test
    fun `pgg always uses a decimal dot even under a comma locale`() {
        Locale.setDefault(Locale("es", "AR"))

        val text = match(strokesPerSet = listOf(175, 104)).toShareText(PadelCategory.SEXTA)

        assertThat(text).contains("14.7 PGG")
        assertThat(text).doesNotContain("14,7")
    }

    @Test
    fun `single set match uses the singular label`() {
        val text = match(setsScore = listOf(listOf(6, 4)), bestOf = 1)
            .toShareText(PadelCategory.SEXTA)

        assertThat(text).contains("Al mejor de 1 set")
        assertThat(text).doesNotContain("Al mejor de 1 sets")
    }

    @Test
    fun `manual match shares fine without stroke data`() {
        val text = match(origin = MatchOrigin.MANUAL).toShareText(PadelCategory.SEXTA)

        assertThat(text).startsWith("🎾 *VICTORIA* 6-4 6-3")
        assertThat(text).contains("📅 19/05/2024")
    }

    @Test
    fun `each scoring mode gets its own label`() {
        assertThat(match(scoringMode = ScoringMode.DEUCE).toShareText(PadelCategory.SEXTA))
            .contains("Deuce / Ventaja")
        assertThat(match(scoringMode = ScoringMode.GOLDEN_POINT).toShareText(PadelCategory.SEXTA))
            .contains("Punto de Oro")
        assertThat(match(scoringMode = ScoringMode.STAR_POINT).toShareText(PadelCategory.SEXTA))
            .contains("Star Point")
    }

    @Test
    fun `decider is spelled out, not abbreviated like in the detail screen`() {
        assertThat(match(decider = Decider.TB7).toShareText(PadelCategory.SEXTA))
            .contains("Tie-break a 7")
        assertThat(match(decider = Decider.SUPER10).toShareText(PadelCategory.SEXTA))
            .contains("Súper tie-break a 10")
    }

    @Test
    fun `the extra tie-break line only shows up when one was actually played`() {
        assertThat(match(tieBreakUsed = true).toShareText(PadelCategory.SEXTA))
            .contains("🎯 Se definió en tie-break")
        assertThat(match(tieBreakUsed = false).toShareText(PadelCategory.SEXTA))
            .doesNotContain("Se definió en tie-break")
    }

    @Test
    fun `a match under an hour reports only minutes`() {
        val text = match(durationMinutes = 48).toShareText(PadelCategory.SEXTA)

        assertThat(text).contains("📅 19/05/2024 · 48 min")
    }

    /**
     * Un partido cargado a mano guarda `startedAt == finishedAt` y deja modo y desempate
     * en el default del constructor. Publicar "0 min · Deuce / Ventaja · Tie-break a 7"
     * sería inventar datos que el usuario nunca cargó, así que se omiten — el mismo
     * criterio que aplica el detalle.
     */
    @Test
    fun `manual match omits duration and rules it never recorded`() {
        val text = match(
            origin = MatchOrigin.MANUAL,
            scoringMode = ScoringMode.STAR_POINT,
            decider = Decider.SUPER10,
            tieBreakUsed = true,
        ).toShareText(PadelCategory.SEXTA)

        assertThat(text).contains("📅 19/05/2024\n")
        assertThat(text).doesNotContain("min")
        assertThat(text).doesNotContain("Star Point")
        assertThat(text).doesNotContain("Súper tie-break")

        // El formato y el tie-break jugado sí son reales: el primero lo elige el usuario
        // al cargar el partido, el segundo se deriva del marcador.
        assertThat(text).contains("⚙️ Al mejor de 3 sets")
        assertThat(text).contains("🎯 Se definió en tie-break")
    }
}
