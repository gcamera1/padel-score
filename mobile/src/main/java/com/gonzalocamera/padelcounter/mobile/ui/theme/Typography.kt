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

// Downloadable Google Fonts. If the cert array is invalid or the provider is
// unavailable, Compose falls back to the platform default font automatically —
// the app keeps working with system typography.
private val FontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val BebasNeue = GoogleFont("Bebas Neue")
private val Inter = GoogleFont("Inter")

private val DisplayFamily = FontFamily(
    GoogleFontFont(googleFont = BebasNeue, fontProvider = FontProvider, weight = FontWeight.Normal, style = FontStyle.Normal),
)

private val UiFamily = FontFamily(
    GoogleFontFont(googleFont = Inter, fontProvider = FontProvider, weight = FontWeight.Normal),
    GoogleFontFont(googleFont = Inter, fontProvider = FontProvider, weight = FontWeight.Medium),
    GoogleFontFont(googleFont = Inter, fontProvider = FontProvider, weight = FontWeight.SemiBold),
    GoogleFontFont(googleFont = Inter, fontProvider = FontProvider, weight = FontWeight.Bold),
)

val PadelTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 1.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
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
    scoreNumeral = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 96.sp,
        lineHeight = 96.sp,
        letterSpacing = (-2).sp,
        textAlign = TextAlign.Center,
    ),
    setGameNumeral = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 44.sp,
    ),
    matchCardScore = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    scoreLabel = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
    ),
    sectionHeader = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.6.sp,
    ),
)

val LocalPadelSportTypography = staticCompositionLocalOf { DefaultSportTypography }
