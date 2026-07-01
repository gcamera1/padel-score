package com.gonzalocamera.padelcounter.mobile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Premium "Premier Padel" scheme — matte black + metallic gold. Dark only.
private val PremiumScheme = darkColorScheme(
    primary = PadelPalette.Gold,
    onPrimary = Color(0xFF14110A),
    primaryContainer = PadelPalette.GoldDark,
    onPrimaryContainer = PadelPalette.GoldLight,
    secondary = PadelPalette.GoldLight,
    onSecondary = Color(0xFF14110A),
    // Drives the selected SegmentedButton / chip fill — solid gold like the reference tabs.
    secondaryContainer = PadelPalette.Gold,
    onSecondaryContainer = Color(0xFF14110A),
    tertiary = PadelPalette.Gold,
    onTertiary = Color(0xFF14110A),
    background = PadelPalette.Background,
    onBackground = PadelPalette.Text,
    surface = PadelPalette.Card,
    onSurface = PadelPalette.Text,
    surfaceVariant = PadelPalette.Gray,
    onSurfaceVariant = PadelPalette.TextMuted,
    surfaceContainer = PadelPalette.Card,
    surfaceContainerHigh = PadelPalette.Gray,
    error = PadelPalette.Live,
    onError = Color.White,
    outline = PadelPalette.Gold.copy(alpha = 0.25f),
    outlineVariant = PadelPalette.Gold.copy(alpha = 0.10f),
)

@Composable
fun PadelMobileTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    // The app is dark-only premium; we ignore the system/user light setting.
    val colorScheme = PremiumScheme
    val padelColors = PadelDarkColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
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
