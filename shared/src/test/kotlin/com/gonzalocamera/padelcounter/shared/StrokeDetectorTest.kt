package com.gonzalocamera.padelcounter.shared

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StrokeDetectorTest {

    private val threshold = 30f

    @Test
    fun `single peak above threshold counts as one stroke`() {
        val detector = StrokeDetector(thresholdMs2 = threshold)
        assertThat(detector.onSample(magnitude = 45f, timestampMs = 1000L)).isTrue()
    }

    @Test
    fun `magnitude below threshold is not a stroke`() {
        val detector = StrokeDetector(thresholdMs2 = threshold)
        assertThat(detector.onSample(magnitude = 12f, timestampMs = 1000L)).isFalse()
    }

    @Test
    fun `two peaks within debounce window count as one`() {
        val detector = StrokeDetector(thresholdMs2 = threshold, debounceMs = 350L)
        assertThat(detector.onSample(45f, 1000L)).isTrue()
        // 200ms después: mismo swing, debe ignorarse
        assertThat(detector.onSample(48f, 1200L)).isFalse()
    }

    @Test
    fun `two peaks separated beyond debounce count as two`() {
        val detector = StrokeDetector(thresholdMs2 = threshold, debounceMs = 350L)
        assertThat(detector.onSample(45f, 1000L)).isTrue()
        // 400ms después: golpe nuevo
        assertThat(detector.onSample(45f, 1400L)).isTrue()
    }

    @Test
    fun `sub-threshold samples between strokes do not affect debounce`() {
        val detector = StrokeDetector(thresholdMs2 = threshold, debounceMs = 350L)
        assertThat(detector.onSample(45f, 1000L)).isTrue()
        assertThat(detector.onSample(5f, 1100L)).isFalse()   // ruido bajo umbral
        assertThat(detector.onSample(45f, 1500L)).isTrue()   // golpe nuevo (500ms después)
    }

    @Test
    fun `reset allows immediate next stroke`() {
        val detector = StrokeDetector(thresholdMs2 = threshold, debounceMs = 350L)
        assertThat(detector.onSample(45f, 1000L)).isTrue()
        detector.reset()
        assertThat(detector.onSample(45f, 1100L)).isTrue()   // tras reset, sin debounce previo
    }

    @Test
    fun `sensitivity thresholds are ordered HIGH lower than MEDIUM lower than LOW`() {
        val high = StrokeSensitivity.HIGH.thresholdMs2()
        val medium = StrokeSensitivity.MEDIUM.thresholdMs2()
        val low = StrokeSensitivity.LOW.thresholdMs2()
        assertThat(high).isLessThan(medium)
        assertThat(medium).isLessThan(low)
    }

    @Test
    fun `higher sensitivity counts a soft stroke that lower sensitivity misses`() {
        val softStroke = 40f   // entre HIGH(32) y MEDIUM(50)
        val ts = 1000L
        assertThat(StrokeDetector(StrokeSensitivity.HIGH.thresholdMs2()).onSample(softStroke, ts)).isTrue()
        assertThat(StrokeDetector(StrokeSensitivity.MEDIUM.thresholdMs2()).onSample(softStroke, ts)).isFalse()
    }
}
