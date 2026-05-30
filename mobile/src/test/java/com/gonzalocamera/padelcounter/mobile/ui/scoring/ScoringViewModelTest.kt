package com.gonzalocamera.padelcounter.mobile.ui.scoring

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.gonzalocamera.padelcounter.mobile.MainDispatcherRule
import com.gonzalocamera.padelcounter.mobile.data.FakeMatchRepository
import com.gonzalocamera.padelcounter.mobile.data.UserPreferences
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.Winner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScoringViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `addPointToMy increments my points and persists state`() = runTest {
        val repo = FakeMatchRepository()
        val vm = ScoringViewModel(repo)
        advanceUntilIdle()

        vm.addPointToMy()
        advanceUntilIdle()

        assertThat(vm.state.value.myPointsIdx).isEqualTo(1)
        assertThat(repo.savedStates).isNotEmpty()
    }

    @Test
    fun `finalize match success emits MatchSaved and clears state`() = runTest {
        val repo = FakeMatchRepository()
        val vm = ScoringViewModel(repo)
        advanceUntilIdle()
        vm.startNewMatch(Decider.TB7, ScoringMode.DEUCE, bestOf = 3)
        advanceUntilIdle()

        vm.events.test {
            vm.finalizeMatch(Winner.MY)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ScoringUiEvent.MatchSaved)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(repo.insertCount).isEqualTo(1)
        assertThat(repo.clearCount).isAtLeast(1)
    }

    @Test
    fun `finalize match failure emits ShowError with message`() = runTest {
        val repo = FakeMatchRepository().apply {
            insertFailure = RuntimeException("disk full")
        }
        val vm = ScoringViewModel(repo)
        advanceUntilIdle()
        vm.startNewMatch(Decider.TB7, ScoringMode.DEUCE, bestOf = 3)
        advanceUntilIdle()

        vm.events.test {
            vm.finalizeMatch(Winner.MY)
            advanceUntilIdle()
            val event = awaitItem()
            assertThat(event).isInstanceOf(ScoringUiEvent.ShowError::class.java)
            assertThat((event as ScoringUiEvent.ShowError).message).contains("disk full")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setServe updates state and persists`() = runTest {
        val repo = FakeMatchRepository()
        val vm = ScoringViewModel(repo)
        advanceUntilIdle()

        vm.setServe(myServe = true)
        advanceUntilIdle()

        assertThat(vm.state.value.isServeSet).isTrue()
        assertThat(vm.state.value.myServe).isTrue()
        assertThat(repo.savedStates).isNotEmpty()
    }

    @Test
    fun `court color from preferences is applied to state and new match`() = runTest {
        val repo = FakeMatchRepository(
            initialPreferences = UserPreferences(courtColor = CourtColorOption.PURPLE),
        )
        val vm = ScoringViewModel(repo)
        advanceUntilIdle()

        assertThat(vm.state.value.courtColor).isEqualTo(CourtColorOption.PURPLE)

        vm.startNewMatch(Decider.TB7, ScoringMode.DEUCE, bestOf = 3)
        advanceUntilIdle()

        assertThat(vm.state.value.courtColor).isEqualTo(CourtColorOption.PURPLE)
    }

    @Test
    fun `discardMatch resets state without inserting`() = runTest {
        val repo = FakeMatchRepository()
        val vm = ScoringViewModel(repo)
        advanceUntilIdle()
        vm.startNewMatch(Decider.SUPER10, ScoringMode.GOLDEN_POINT, bestOf = 5)
        advanceUntilIdle()
        vm.addPointToMy()
        advanceUntilIdle()

        vm.discardMatch()
        advanceUntilIdle()

        assertThat(repo.insertCount).isEqualTo(0)
        assertThat(repo.clearCount).isAtLeast(1)
        assertThat(vm.state.value.myPointsIdx).isEqualTo(0)
    }
}
