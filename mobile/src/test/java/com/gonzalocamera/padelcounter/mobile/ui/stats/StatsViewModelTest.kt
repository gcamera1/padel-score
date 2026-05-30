package com.gonzalocamera.padelcounter.mobile.ui.stats

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.gonzalocamera.padelcounter.mobile.MainDispatcherRule
import com.gonzalocamera.padelcounter.mobile.data.FakeMatchRepository
import com.gonzalocamera.padelcounter.shared.Winner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `winLossLast7 is empty when no matches`() = runTest {
        val repo = FakeMatchRepository()
        val vm = StatsViewModel(repo)

        vm.winLossLast7.test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `winLossLast7 returns chronological order oldest first`() = runTest {
        val repo = FakeMatchRepository().apply {
            // Seeded with increasing finishedAt: m0 oldest, m2 newest
            seedWinners(listOf(Winner.OPP, Winner.MY, Winner.MY))
        }
        val vm = StatsViewModel(repo)
        advanceUntilIdle()

        vm.winLossLast7.test {
            // Skip initial empty emission
            skipItems(1)
            val results = awaitItem()
            // Expect order from oldest to newest
            assertThat(results).containsExactly(Winner.OPP, Winner.MY, Winner.MY).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `winLossLast7 caps at 7 most recent`() = runTest {
        val winners = List(10) { i -> if (i % 2 == 0) Winner.MY else Winner.OPP }
        val repo = FakeMatchRepository().apply { seedWinners(winners) }
        val vm = StatsViewModel(repo)
        advanceUntilIdle()

        vm.winLossLast7.test {
            skipItems(1)
            val results = awaitItem()
            assertThat(results).hasSize(7)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
