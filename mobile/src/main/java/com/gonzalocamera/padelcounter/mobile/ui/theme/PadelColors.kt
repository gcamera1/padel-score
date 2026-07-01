package com.gonzalocamera.padelcounter.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Premium "Premier Padel" palette — matte black + metallic gold.
 * Single source of truth for the brand tokens shared across screens.
 */
object PadelPalette {
    val Gold = Color(0xFFC5A85A)
    val GoldLight = Color(0xFFE5C453)
    val GoldDark = Color(0xFF8A6F30)

    val Background = Color(0xFF0B0C0D)   // app background
    val BackgroundDeep = Color(0xFF070809) // outermost / behind cards
    val Card = Color(0xFF151719)         // surface / cards
    val Gray = Color(0xFF222528)         // chips, dividers, inert buttons
    val Text = Color(0xFFE2E5E8)         // primary text

    val TextMuted = Color(0xFF9CA3AF)    // gray-400
    val TextFaint = Color(0xFF6B7280)    // gray-500

    val Live = Color(0xFFDC2626)         // red-600 "EN VIVO"
    val LiveSoft = Color(0xFFF87171)     // red-400

    // Stat distribution (donut / bars)
    val DonutTrack = Color(0xFF1E2225)
    val Wear = Color(0xFF444A50)         // "desgaste" grey segment
    val Normal = Color(0xFFFFFFFF)       // "normal" white segment
}

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
    // Premium tokens
    val gold: Color,
    val goldLight: Color,
    val goldDark: Color,
    val card: Color,
    val hairline: Color,
    val textMuted: Color,
    val textFaint: Color,
    val live: Color,
)

internal val PadelDarkColors = PadelColors(
    courtSurface = PadelPalette.Card,
    courtLine = PadelPalette.Gold.copy(alpha = 0.28f),
    accentMine = PadelPalette.GoldLight,
    accentRival = PadelPalette.Text,
    serveBall = PadelPalette.Gold,
    tieBreakHighlight = PadelPalette.Gold,
    goldenGlow = PadelPalette.Gold,
    winSurface = PadelPalette.Gold.copy(alpha = 0.12f),
    lossSurface = PadelPalette.Live.copy(alpha = 0.14f),
    premiumGold = PadelPalette.Gold,
    gold = PadelPalette.Gold,
    goldLight = PadelPalette.GoldLight,
    goldDark = PadelPalette.GoldDark,
    card = PadelPalette.Card,
    hairline = PadelPalette.Gold.copy(alpha = 0.20f),
    textMuted = PadelPalette.TextMuted,
    textFaint = PadelPalette.TextFaint,
    live = PadelPalette.Live,
)

// Dark-only premium app: light variant mirrors dark so any stray
// isSystemInDarkTheme()==false path still renders on-brand.
internal val PadelLightColors = PadelDarkColors

val LocalPadelColors = staticCompositionLocalOf { PadelDarkColors }
