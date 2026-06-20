package com.gonzalocamera.padelcounter.mobile.data.db

import com.google.common.truth.Truth.assertThat
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.Winner
import org.junit.Test

class MatchEntityTest {

    private fun match(strokesPerSet: List<Int>?) = Match(
        id = "m1",
        startedAt = 0L,
        finishedAt = 1L,
        setsScore = listOf(listOf(6, 4), listOf(6, 3)),
        tieBreakUsed = false,
        decider = Decider.TB7,
        winner = Winner.MY,
        origin = MatchOrigin.WEAR,
        strokesPerSet = strokesPerSet,
    )

    @Test
    fun `round-trip preserves strokesPerSet`() {
        val restored = match(listOf(42, 38)).toEntity().toMatch()
        assertThat(restored.strokesPerSet).containsExactly(42, 38).inOrder()
    }

    @Test
    fun `round-trip preserves null strokesPerSet`() {
        val entity = match(null).toEntity()
        assertThat(entity.strokesPerSetJson).isNull()
        assertThat(entity.toMatch().strokesPerSet).isNull()
    }
}
