package com.gonzalocamera.padelcounter.mobile.ui.history

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
class HistoryViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `aggregateLite is Empty when no matches`() = runTest {
        val repo = FakeMatchRepository()
        val vm = HistoryViewModel(repo)

        vm.aggregateLite.test {
            assertThat(awaitItem()).isEqualTo(HistorySummary.Empty)
            advanceUntilIdle()
            // StateFlow stays at Empty — no further emission expected.
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `aggregateLite reports 100 percent for all wins`() = runTest {
        val repo = FakeMatchRepository().apply {
            seedWinners(listOf(Winner.MY, Winner.MY, Winner.MY))
        }
        val vm = HistoryViewModel(repo)
        advanceUntilIdle()

        vm.aggregateLite.test {
            // Skip initial Empty emission
            skipItems(1)
            val summary = awaitItem()
            assertThat(summary.totalMatches).isEqualTo(3)
            assertThat(summary.winPct).isEqualTo(100)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `aggregateLite computes winPct correctly for mixed`() = runTest {
        val repo = FakeMatchRepository().apply {
            seedWinners(listOf(Winner.MY, Winner.OPP, Winner.MY, Winner.OPP))
        }
        val vm = HistoryViewModel(repo)
        advanceUntilIdle()

        vm.aggregateLite.test {
            skipItems(1)
            val summary = awaitItem()
            assertThat(summary.totalMatches).isEqualTo(4)
            assertThat(summary.winPct).isEqualTo(50)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
