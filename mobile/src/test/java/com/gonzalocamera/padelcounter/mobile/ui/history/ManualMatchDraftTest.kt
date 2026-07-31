package com.gonzalocamera.padelcounter.mobile.ui.history

import com.google.common.truth.Truth.assertThat
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.Winner
import org.junit.Test

private const val ANY_DATE = 1_716_103_600_000L

private fun draft(vararg sets: Pair<Int, Int>, bestOf: Int = sets.size) =
    ManualMatchDraft(bestOf = bestOf, sets = sets.toList(), dateMillis = ANY_DATE)

class ManualMatchDraftTest {

    // --- playedSets ---

    @Test
    fun `discards 0-0 sets preserving order`() {
        val d = draft(6 to 4, 0 to 0, 6 to 2)
        assertThat(d.playedSets).containsExactly(listOf(6, 4), listOf(6, 2)).inOrder()
    }

    @Test
    fun `keeps sets where only one side is zero`() {
        val d = draft(6 to 0, 0 to 6)
        assertThat(d.playedSets).containsExactly(listOf(6, 0), listOf(0, 6)).inOrder()
    }

    // --- isValid ---

    @Test
    fun `all sets empty is not valid`() {
        assertThat(draft(0 to 0, 0 to 0, 0 to 0).isValid).isFalse()
    }

    @Test
    fun `tied sets won is not valid`() {
        // 1-1 en un best of 3 con el tercer set sin cargar: no hay ganador derivable.
        assertThat(draft(6 to 4, 3 to 6, 0 to 0).isValid).isFalse()
    }

    @Test
    fun `single 6-6 set is not valid`() {
        // Nadie gana el set, así que quedan 0 vs 0 sets ganados = empate.
        assertThat(draft(6 to 6).isValid).isFalse()
    }

    @Test
    fun `two sets to zero is valid`() {
        assertThat(draft(6 to 4, 6 to 3, 0 to 0).isValid).isTrue()
    }

    @Test
    fun `impossible scores are allowed`() {
        // Decisión explícita: no se validan reglas de pádel, solo que haya un ganador.
        assertThat(draft(3 to 2).isValid).isTrue()
        assertThat(draft(7 to 0).isValid).isTrue()
    }

    // --- winner ---

    @Test
    fun `winner is MY when winning more sets`() {
        assertThat(draft(6 to 4, 6 to 3).toMatch().winner).isEqualTo(Winner.MY)
        assertThat(draft(6 to 4, 3 to 6, 7 to 5).toMatch().winner).isEqualTo(Winner.MY)
    }

    @Test
    fun `winner is OPP when losing more sets`() {
        assertThat(draft(4 to 6, 3 to 6).toMatch().winner).isEqualTo(Winner.OPP)
    }

    // --- tieBreakUsed ---

    @Test
    fun `tieBreakUsed is true when any set reaches seven`() {
        assertThat(draft(7 to 5, 6 to 3).toMatch().tieBreakUsed).isTrue()
        assertThat(draft(6 to 7, 6 to 3, 6 to 4).toMatch().tieBreakUsed).isTrue()
    }

    @Test
    fun `tieBreakUsed is false without any seven`() {
        assertThat(draft(6 to 4, 6 to 3).toMatch().tieBreakUsed).isFalse()
    }

    // --- withBestOf / withSet ---

    @Test
    fun `withBestOf pads with empty sets keeping what was loaded`() {
        val d = draft(6 to 4, 6 to 3, 0 to 0).withBestOf(5)

        assertThat(d.bestOf).isEqualTo(5)
        assertThat(d.sets).hasSize(5)
        assertThat(d.sets.take(2)).containsExactly(6 to 4, 6 to 3).inOrder()
        assertThat(d.sets.drop(2)).containsExactly(0 to 0, 0 to 0, 0 to 0)
    }

    @Test
    fun `withBestOf truncates dropping the extra sets`() {
        val d = draft(6 to 4, 6 to 3, 6 to 2).withBestOf(1)

        assertThat(d.bestOf).isEqualTo(1)
        assertThat(d.sets).containsExactly(6 to 4)
    }

    @Test
    fun `withSet only touches the target index`() {
        val d = draft(6 to 4, 6 to 3, 0 to 0).withSet(1, 2, 6)

        assertThat(d.sets).containsExactly(6 to 4, 2 to 6, 0 to 0).inOrder()
    }

    @Test
    fun `withSet clamps outside the allowed range`() {
        val d = draft(0 to 0).withSet(0, -1, 99)

        assertThat(d.sets).containsExactly(0 to MAX_GAMES)
    }

    // --- toMatch ---

    @Test
    fun `toMatch fills the fields the manual form does not ask for`() {
        val match = draft(6 to 4, 6 to 3, 0 to 0, bestOf = 3).toMatch(id = "fixed-id")

        assertThat(match.id).isEqualTo("fixed-id")
        assertThat(match.origin).isEqualTo(MatchOrigin.MANUAL)
        assertThat(match.scoringMode).isEqualTo(ScoringMode.DEUCE)
        assertThat(match.goldenPoint).isFalse()
        assertThat(match.decider).isEqualTo(Decider.TB7)
        assertThat(match.strokesPerSet).isNull()
        assertThat(match.bestOf).isEqualTo(3)
        assertThat(match.setsScore).containsExactly(listOf(6, 4), listOf(6, 3)).inOrder()
    }

    @Test
    fun `toMatch has no duration`() {
        val match = draft(6 to 4, 6 to 3).toMatch()

        assertThat(match.startedAt).isEqualTo(ANY_DATE)
        assertThat(match.finishedAt).isEqualTo(ANY_DATE)
    }

    @Test
    fun `toMatch generates a distinct id per call`() {
        val a = draft(6 to 4, 6 to 3).toMatch()
        val b = draft(6 to 4, 6 to 3).toMatch()

        assertThat(a.id).isNotEqualTo(b.id)
    }

    // --- defaults ---

    @Test
    fun `default draft is best of three with three empty sets`() {
        val d = ManualMatchDraft(dateMillis = ANY_DATE)

        assertThat(d.bestOf).isEqualTo(3)
        assertThat(d.sets).containsExactly(0 to 0, 0 to 0, 0 to 0)
        assertThat(d.isValid).isFalse()
    }
}
