package com.gonzalocamera.padelcounter.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

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

    // El estilo de las barras del sistema lo fija `enableEdgeToEdge` en MainActivity.
    // `window.statusBarColor` / `navigationBarColor` quedaron deprecados y sin efecto
    // desde API 35, así que no se setean acá.

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
