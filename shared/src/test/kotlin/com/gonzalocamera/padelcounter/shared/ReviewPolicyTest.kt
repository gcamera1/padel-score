package com.gonzalocamera.padelcounter.shared

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReviewPolicyTest {

    private val day = ReviewPolicy.DAY_MS
    private val now = 1_700_000_000_000L

    /** Estado que cumple todas las guardas: cada test rompe una sola cosa. */
    private fun eligible(
        status: ReviewPromptStatus = ReviewPromptStatus.NEVER_ASKED,
        score: Int = ReviewPolicy.SCORE_THRESHOLD,
        firstSeenAt: Long = now - 5 * day,
        lastPromptAt: Long = 0L,
        scoreAtLastPrompt: Int = 0,
        promptCount: Int = 0,
    ) = ReviewPromptState(
        status = status,
        score = score,
        firstSeenAt = firstSeenAt,
        lastPromptAt = lastPromptAt,
        scoreAtLastPrompt = scoreAtLastPrompt,
        promptCount = promptCount,
    )

    @Test
    fun `pide la calificacion cuando se cumplen todas las guardas`() {
        assertThat(ReviewPolicy.shouldPrompt(eligible(), matchCount = 2, now = now)).isTrue()
    }

    @Test
    fun `no pide por debajo del umbral de puntos`() {
        val state = eligible(score = ReviewPolicy.SCORE_THRESHOLD - 1)
        assertThat(ReviewPolicy.shouldPrompt(state, matchCount = 5, now = now)).isFalse()
    }

    @Test
    fun `no pide con el historial casi vacio`() {
        assertThat(ReviewPolicy.shouldPrompt(eligible(), matchCount = 1, now = now)).isFalse()
    }

    @Test
    fun `no pide en los primeros dias`() {
        val state = eligible(firstSeenAt = now - (ReviewPolicy.MIN_DAYS_INSTALLED - 1) * day)
        assertThat(ReviewPolicy.shouldPrompt(state, matchCount = 5, now = now)).isFalse()
    }

    @Test
    fun `no pide si nunca se inicializo el ancla temporal`() {
        assertThat(ReviewPolicy.shouldPrompt(eligible(firstSeenAt = 0L), 5, now)).isFalse()
    }

    @Test
    fun `no vuelve a pedir despues de un no gracias`() {
        val state = eligible(status = ReviewPromptStatus.DISMISSED, score = 100)
        assertThat(ReviewPolicy.shouldPrompt(state, matchCount = 50, now = now)).isFalse()
    }

    @Test
    fun `no vuelve a pedir despues de calificar`() {
        val state = eligible(status = ReviewPromptStatus.RATED, score = 100)
        assertThat(ReviewPolicy.shouldPrompt(state, matchCount = 50, now = now)).isFalse()
    }

    @Test
    fun `no vuelve a pedir tras el tope de pedidos`() {
        val state = eligible(promptCount = ReviewPolicy.MAX_PROMPTS, score = 100)
        assertThat(ReviewPolicy.shouldPrompt(state, matchCount = 50, now = now)).isFalse()
    }

    // --- "Más tarde": hacen falta tiempo Y uso nuevo ---

    @Test
    fun `snooze no reaparece antes de la espera minima`() {
        val snoozed = ReviewPolicy.snooze(eligible(), now)
        val later = now + (ReviewPolicy.SNOOZE_DAYS - 1) * day
        val withUse = snoozed.copy(score = snoozed.score + ReviewPolicy.SNOOZE_SCORE_DELTA)
        assertThat(ReviewPolicy.shouldPrompt(withUse, matchCount = 5, now = later)).isFalse()
    }

    @Test
    fun `snooze no reaparece sin uso nuevo aunque pase el tiempo`() {
        val snoozed = ReviewPolicy.snooze(eligible(), now)
        val muchLater = now + 60 * day
        assertThat(ReviewPolicy.shouldPrompt(snoozed, matchCount = 5, now = muchLater)).isFalse()
    }

    @Test
    fun `snooze reaparece con tiempo y uso nuevo`() {
        val snoozed = ReviewPolicy.snooze(eligible(), now)
        val later = now + (ReviewPolicy.SNOOZE_DAYS + 1) * day
        val withUse = snoozed.copy(score = snoozed.score + ReviewPolicy.SNOOZE_SCORE_DELTA)
        assertThat(ReviewPolicy.shouldPrompt(withUse, matchCount = 5, now = later)).isTrue()
    }

    @Test
    fun `tres mas tarde agotan el cupo`() {
        var state = eligible()
        repeat(ReviewPolicy.MAX_PROMPTS) { i ->
            state = ReviewPolicy.snooze(state, now + i * 30 * day)
            state = state.copy(score = state.score + 10)
        }
        val muchLater = now + 365 * day
        assertThat(ReviewPolicy.shouldPrompt(state, matchCount = 50, now = muchLater)).isFalse()
    }

    // --- Semilla y mapeo de señales ---

    @Test
    fun `el usuario con historial arranca por encima del umbral`() {
        assertThat(ReviewPolicy.seedScore(0)).isEqualTo(0)
        assertThat(ReviewPolicy.seedScore(1)).isEqualTo(2)
        assertThat(ReviewPolicy.seedScore(3)).isAtLeast(ReviewPolicy.SCORE_THRESHOLD)
        assertThat(ReviewPolicy.seedScore(50)).isEqualTo(ReviewPolicy.seedScore(3))
    }

    @Test
    fun `el partido del reloj pesa mas que el manual`() {
        val wear = ReviewPolicy.signalFor(MatchOrigin.WEAR)
        val manual = ReviewPolicy.signalFor(MatchOrigin.MANUAL)
        val mobile = ReviewPolicy.signalFor(MatchOrigin.MOBILE)
        assertThat(wear.points).isGreaterThan(mobile.points)
        assertThat(mobile.points).isGreaterThan(manual.points)
    }

    @Test
    fun `dayOf agrupa el mismo dia y separa el siguiente`() {
        assertThat(ReviewPolicy.dayOf(now)).isEqualTo(ReviewPolicy.dayOf(now + 1000))
        assertThat(ReviewPolicy.dayOf(now + day)).isNotEqualTo(ReviewPolicy.dayOf(now))
    }
}
