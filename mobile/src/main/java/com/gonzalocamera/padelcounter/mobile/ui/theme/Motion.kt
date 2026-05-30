package com.gonzalocamera.padelcounter.mobile.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class PadelMotion(
    val durationFast: Int = 120,
    val durationMedium: Int = 220,
    val durationSlow: Int = 400,
    val durationCelebration: Int = 700,
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val standard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
    val bounceOut: Easing = CubicBezierEasing(0.2f, 1.3f, 0.4f, 1f),
)

val LocalPadelMotion = staticCompositionLocalOf { PadelMotion() }
