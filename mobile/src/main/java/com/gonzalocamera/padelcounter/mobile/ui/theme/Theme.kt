package com.gonzalocamera.padelcounter.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB49058),
    onPrimary = Color(0xFF1A0E00),
    secondary = Color(0xFFD4AA70),
    onSecondary = Color(0xFF1A0E00),
    tertiary = Color(0xFF22D17A),
    onTertiary = Color(0xFF00210F),
    background = Color(0xFF050505),
    onBackground = Color(0xFFF0F0F0),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFFF5A6B),
    onError = Color(0xFF410008),
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF2C2C2C),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF7A5C30),
    onPrimary = Color.White,
    secondary = Color(0xFF96723E),
    onSecondary = Color.White,
    tertiary = Color(0xFF0E8A4F),
    onTertiary = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF0E1113),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1113),
    surfaceVariant = Color(0xFFEBEBEB),
    onSurfaceVariant = Color(0xFF4A5056),
    error = Color(0xFFC8364A),
    onError = Color.White,
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFFD4DADE),
)

@Composable
fun PadelMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val padelColors = if (darkTheme) PadelDarkColors else PadelLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalPadelColors provides padelColors,
        LocalPadelSpacing provides PadelSpacing(),
        LocalPadelMotion provides PadelMotion(),
        LocalPadelSportTypography provides DefaultSportTypography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PadelTypography,
            shapes = PadelShapes,
            content = content,
        )
    }
}
