package com.gonzalocamera.padelcounter.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.gonzalocamera.padelcounter.mobile.R

// Downloadable Google Fonts. If the provider is unavailable, Compose falls back
// to the platform default font automatically — the app keeps working.
private val FontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val OutfitFont = GoogleFont("Outfit")          // display / headings
private val InterFont = GoogleFont("Inter")            // body / UI
private val MonoFont = GoogleFont("JetBrains Mono")    // scores / numerals

private val DisplayFamily = FontFamily(
    GoogleFontFont(googleFont = OutfitFont, fontProvider = FontProvider, weight = FontWeight.Medium),
    GoogleFontFont(googleFont = OutfitFont, fontProvider = FontProvider, weight = FontWeight.SemiBold),
    GoogleFontFont(googleFont = OutfitFont, fontProvider = FontProvider, weight = FontWeight.Bold),
    GoogleFontFont(googleFont = OutfitFont, fontProvider = FontProvider, weight = FontWeight.ExtraBold),
)

private val UiFamily = FontFamily(
    GoogleFontFont(googleFont = InterFont, fontProvider = FontProvider, weight = FontWeight.Normal),
    GoogleFontFont(googleFont = InterFont, fontProvider = FontProvider, weight = FontWeight.Medium),
    GoogleFontFont(googleFont = InterFont, fontProvider = FontProvider, weight = FontWeight.SemiBold),
    GoogleFontFont(googleFont = InterFont, fontProvider = FontProvider, weight = FontWeight.Bold),
)

private val MonoFamily = FontFamily(
    GoogleFontFont(googleFont = MonoFont, fontProvider = FontProvider, weight = FontWeight.Medium),
    GoogleFontFont(googleFont = MonoFont, fontProvider = FontProvider, weight = FontWeight.Bold),
    GoogleFontFont(googleFont = MonoFont, fontProvider = FontProvider, weight = FontWeight.ExtraBold),
)

val PadelTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
    ),
)

@Immutable
data class PadelSportTypography(
    val scoreNumeral: TextStyle,
    val setGameNumeral: TextStyle,
    val matchCardScore: TextStyle,
    val scoreLabel: TextStyle,
    val sectionHeader: TextStyle,
)

internal val DefaultSportTypography = PadelSportTypography(
    // Big live point numbers — JetBrains Mono, like the reference scoreboard.
    scoreNumeral = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1).sp,
        textAlign = TextAlign.Center,
    ),
    // Sets / games / stat values — bold mono.
    setGameNumeral = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
    ),
    matchCardScore = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
    scoreLabel = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
    ),
    // Section eyebrows — mono uppercase, like "RENDIMIENTO TOTAL".
    sectionHeader = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
    ),
)

val LocalPadelSportTypography = staticCompositionLocalOf { DefaultSportTypography }
