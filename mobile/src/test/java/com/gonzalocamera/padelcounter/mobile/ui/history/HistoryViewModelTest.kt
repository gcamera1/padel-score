package com.gonzalocamera.padelcounter.mobile.ui.history

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.gonzalocamera.padelcounter.mobile.MainDispatcherRule
import com.gonzalocamera.padelcounter.mobile.data.FakeMatchRepository
import com.gonzalocamera.padelcounter.shared.MatchOrigin
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

    @Test
    fun `saveManualMatch inserts a MANUAL match`() = runTest {
        val repo = FakeMatchRepository()
        val vm = HistoryViewModel(repo)

        vm.matches.test {
            skipItems(1) // emisión inicial vacía
            vm.saveManualMatch(validDraft())
            advanceUntilIdle()

            val summaries = awaitItem()
            assertThat(summaries).hasSize(1)
            assertThat(summaries.single().origin).isEqualTo(MatchOrigin.MANUAL)
            assertThat(summaries.single().winner).isEqualTo(Winner.MY)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repo.insertCount).isEqualTo(1)
    }

    @Test
    fun `saveManualMatch emits ManualMatchSaved on success`() = runTest {
        val repo = FakeMatchRepository()
        val vm = HistoryViewModel(repo)

        vm.events.test {
            vm.saveManualMatch(validDraft())
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(HistoryUiEvent.ManualMatchSaved)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveManualMatch ignores an invalid draft`() = runTest {
        val repo = FakeMatchRepository()
        val vm = HistoryViewModel(repo)

        vm.events.test {
            // Todos los sets en 0-0: no hay nada que guardar.
            vm.saveManualMatch(ManualMatchDraft(dateMillis = MANUAL_DATE))
            advanceUntilIdle()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repo.insertCount).isEqualTo(0)
    }

    @Test
    fun `saveManualMatch emits ShowError when the repository fails`() = runTest {
        val repo = FakeMatchRepository().apply { insertFailure = RuntimeException("disco lleno") }
        val vm = HistoryViewModel(repo)

        vm.events.test {
            vm.saveManualMatch(validDraft())
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(HistoryUiEvent.ShowError("disco lleno"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repo.insertCount).isEqualTo(0)
    }

    private fun validDraft() = ManualMatchDraft(
        bestOf = 3,
        sets = listOf(6 to 4, 6 to 3, 0 to 0),
        dateMillis = MANUAL_DATE,
    )
}

private const val MANUAL_DATE = 1_716_103_600_000L
