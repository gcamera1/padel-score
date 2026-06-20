package com.gonzalocamera.padelcounter.presentation

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class StrokeCounterTest {

    @After
    fun tearDown() = StrokeCounter.reset()

    @Test
    fun `recordStroke increments the given set`() {
        StrokeCounter.reset()
        StrokeCounter.recordStroke(0)
        StrokeCounter.recordStroke(0)
        StrokeCounter.recordStroke(1)
        assertThat(StrokeCounter.snapshot()).containsExactly(2, 1).inOrder()
    }

    @Test
    fun `recordStroke grows the list with zeros for skipped sets`() {
        StrokeCounter.reset()
        StrokeCounter.recordStroke(2) // primer golpe cae en el set índice 2
        assertThat(StrokeCounter.snapshot()).containsExactly(0, 0, 1).inOrder()
    }

    @Test
    fun `negative set index is ignored`() {
        StrokeCounter.reset()
        StrokeCounter.recordStroke(-1)
        assertThat(StrokeCounter.snapshot()).isEmpty()
    }

    @Test
    fun `restore replaces the accumulated state`() {
        StrokeCounter.reset()
        StrokeCounter.restore(listOf(10, 20))
        assertThat(StrokeCounter.snapshot()).containsExactly(10, 20).inOrder()
        StrokeCounter.recordStroke(1)
        assertThat(StrokeCounter.snapshot()).containsExactly(10, 21).inOrder()
    }

    @Test
    fun `reset clears the state`() {
        StrokeCounter.restore(listOf(5, 5))
        StrokeCounter.reset()
        assertThat(StrokeCounter.snapshot()).isEmpty()
    }
}
