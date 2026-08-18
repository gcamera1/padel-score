package com.gonzalocamera.padelcounter.mobile.ui.rating

import com.google.common.truth.Truth.assertThat
import com.gonzalocamera.padelcounter.mobile.MainDispatcherRule
import com.gonzalocamera.padelcounter.mobile.data.FakeMatchRepository
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.ReviewPolicy
import com.gonzalocamera.padelcounter.shared.ReviewPromptState
import com.gonzalocamera.padelcounter.shared.ReviewPromptStatus
import com.gonzalocamera.padelcounter.shared.Winner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RatingViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    /** El VM usa el reloj real, así que las fechas se anclan a `now` de verdad. */
    private val now = System.currentTimeMillis()

    private fun matches(count: Int): List<Match> = (0 until count).map { i ->
        Match(
            id = "m$i",
            startedAt = now - (i + 1) * ReviewPolicy.DAY_MS,
            finishedAt = now - (i + 1) * ReviewPolicy.DAY_MS + 3_600_000L,
            setsScore = listOf(listOf(6, 3)),
            tieBreakUsed = false,
            decider = Decider.TB7,
            winner = Winner.MY,
            origin = MatchOrigin.WEAR,
        )
    }

    /** Estado que ya cumple todas las guardas de [ReviewPolicy]. */
    private fun eligibleState(score: Int = ReviewPolicy.SCORE_THRESHOLD) = ReviewPromptState(
        score = score,
        firstSeenAt = now - 10 * ReviewPolicy.DAY_MS,
    )

    private fun repo(
        matchCount: Int = 3,
        state: ReviewPromptState = eligibleState(),
    ) = FakeMatchRepository(
        initialMatches = matches(matchCount),
        initialReviewPromptState = state,
    )

    @Test
    fun `muestra el modal en un momento de valor con uso suficiente`() = runTest {
        val repo = repo()
        val vm = RatingViewModel(repo)
        advanceUntilIdle()

        vm.onMatchDetailViewed()
        advanceUntilIdle()

        assertThat(vm.visible.value).isTrue()
    }

    @Test
    fun `no muestra el modal sin puntaje suficiente`() = runTest {
        val repo = repo(state = eligibleState(score = 0))
        val vm = RatingViewModel(repo)
        advanceUntilIdle()

        vm.onMatchDetailViewed()
        advanceUntilIdle()

        assertThat(vm.visible.value).isFalse()
    }

    @Test
    fun `no muestra el modal con el historial casi vacio`() = runTest {
        val repo = repo(matchCount = 1, state = eligibleState(score = 100))
        val vm = RatingViewModel(repo)
        advanceUntilIdle()

        vm.onMatchDetailViewed()
        advanceUntilIdle()

        assertThat(vm.visible.value).isFalse()
    }

    @Test
    fun `ver estadisticas suma un punto`() = runTest {
        val repo = repo(state = eligibleState(score = 0))
        val vm = RatingViewModel(repo)
        advanceUntilIdle()

        vm.onStatsViewed()
        advanceUntilIdle()

        assertThat(repo.reviewPromptState.first().score).isEqualTo(1)
    }

    @Test
    fun `compartir suma puntos pero no interrumpe con el modal`() = runTest {
        val repo = repo(state = eligibleState(score = 0))
        val vm = RatingViewModel(repo)
        advanceUntilIdle()

        vm.onMatchShared()
        advanceUntilIdle()

        assertThat(vm.visible.value).isFalse()
        assertThat(repo.reviewPromptState.first().score).isEqualTo(4)
    }

    @Test
    fun `mas tarde cierra el modal y lo deja en snooze`() = runTest {
        val repo = repo()
        val vm = RatingViewModel(repo)
        advanceUntilIdle()
        vm.onMatchDetailViewed()
        advanceUntilIdle()

        vm.onLater()
        advanceUntilIdle()

        assertThat(vm.visible.value).isFalse()
        val state = repo.reviewPromptState.first()
        assertThat(state.status).isEqualTo(ReviewPromptStatus.SNOOZED)
        assertThat(state.promptCount).isEqualTo(1)
        assertThat(state.scoreAtLastPrompt).isEqualTo(state.score)
    }

    @Test
    fun `no gracias no vuelve a pedir nunca`() = runTest {
        val repo = repo()
        val vm = RatingViewModel(repo)
        advanceUntilIdle()

        vm.onNever()
        advanceUntilIdle()

        val state = repo.reviewPromptState.first()
        assertThat(state.status).isEqualTo(ReviewPromptStatus.DISMISSED)
        assertThat(ReviewPolicy.shouldPrompt(state, matchCount = 50, now = now)).isFalse()
    }

    @Test
    fun `calificar cierra el pedido para siempre`() = runTest {
        val repo = repo()
        val vm = RatingViewModel(repo)
        advanceUntilIdle()

        vm.onRate()
        advanceUntilIdle()

        val state = repo.reviewPromptState.first()
        assertThat(state.status).isEqualTo(ReviewPromptStatus.RATED)
        assertThat(ReviewPolicy.shouldPrompt(state, matchCount = 50, now = now)).isFalse()
    }

    @Test
    fun `el arranque siembra el ancla temporal y el puntaje del historial previo`() = runTest {
        val repo = FakeMatchRepository(initialMatches = matches(4))
        RatingViewModel(repo)
        advanceUntilIdle()

        val state = repo.reviewPromptState.first()
        assertThat(state.firstSeenAt).isGreaterThan(0L)
        assertThat(state.score).isEqualTo(ReviewPolicy.seedScore(4))
    }

    @Test
    fun `el usuario recien llegado no ve el modal aunque tenga puntaje`() = runTest {
        val repo = repo(
            state = ReviewPromptState(score = 100, firstSeenAt = now - ReviewPolicy.DAY_MS),
        )
        val vm = RatingViewModel(repo)
        advanceUntilIdle()

        vm.onMatchDetailViewed()
        advanceUntilIdle()

        assertThat(vm.visible.value).isFalse()
    }
}
