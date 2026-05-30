package com.gonzalocamera.padelcounter.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class PadelColors(
    val courtSurface: Color,
    val courtLine: Color,
    val accentMine: Color,
    val accentRival: Color,
    val serveBall: Color,
    val tieBreakHighlight: Color,
    val goldenGlow: Color,
    val winSurface: Color,
    val lossSurface: Color,
    val premiumGold: Color,
)

internal val PadelDarkColors = PadelColors(
    courtSurface = Color(0xFF1A1A1A),
    courtLine = Color(0xFFFFFFFF).copy(alpha = 0.30f),
    accentMine = Color(0xFF22D17A),
    accentRival = Color(0xFFFF5A6B),
    serveBall = Color(0xFFB49058),
    tieBreakHighlight = Color(0xFFB49058),
    goldenGlow = Color(0xFFC9A46C),
    winSurface = Color(0xFF22D17A).copy(alpha = 0.18f),
    lossSurface = Color(0xFFFF5A6B).copy(alpha = 0.18f),
    premiumGold = Color(0xFFB49058),
)

internal val PadelLightColors = PadelColors(
    courtSurface = Color(0xFFEBEBEB),
    courtLine = Color(0xFF0E1113).copy(alpha = 0.45f),
    accentMine = Color(0xFF0E8A4F),
    accentRival = Color(0xFFC8364A),
    serveBall = Color(0xFF96723E),
    tieBreakHighlight = Color(0xFF96723E),
    goldenGlow = Color(0xFFB49058),
    winSurface = Color(0xFF0E8A4F).copy(alpha = 0.14f),
    lossSurface = Color(0xFFC8364A).copy(alpha = 0.14f),
    premiumGold = Color(0xFF96723E),
)

val LocalPadelColors = staticCompositionLocalOf { PadelDarkColors }
