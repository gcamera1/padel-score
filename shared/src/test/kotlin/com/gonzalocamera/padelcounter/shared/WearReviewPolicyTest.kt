package com.gonzalocamera.padelcounter.shared

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Un test por criterio de aceptación de la invitación automática del reloj. */
class WearReviewPolicyTest {

    private fun state(matches: Int = 0, prompts: Int = 0, rated: Boolean = false) =
        WearReviewState(finishedMatches = matches, promptsShown = prompts, rated = rated)

    @Test
    fun `no invita antes de los 3 partidos`() {
        assertThat(WearReviewPolicy.shouldPrompt(state(matches = 2), matchInProgress = false)).isFalse()
    }

    @Test
    fun `invita a partir del tercer partido`() {
        assertThat(WearReviewPolicy.shouldPrompt(state(matches = 3), matchInProgress = false)).isTrue()
    }

    @Test
    fun `nunca invita durante un partido en juego`() {
        assertThat(WearReviewPolicy.shouldPrompt(state(matches = 10), matchInProgress = true)).isFalse()
    }

    @Test
    fun `quien califico no vuelve a recibir la invitacion`() {
        assertThat(WearReviewPolicy.shouldPrompt(state(matches = 10, rated = true), matchInProgress = false)).isFalse()
    }

    @Test
    fun `un Ahora no deja que vuelva a aparecer`() {
        assertThat(WearReviewPolicy.shouldPrompt(state(matches = 5, prompts = 1), matchInProgress = false)).isTrue()
    }

    @Test
    fun `se corta en la segunda aparicion`() {
        assertThat(WearReviewPolicy.shouldPrompt(state(matches = 50, prompts = 2), matchInProgress = false)).isFalse()
    }
}
