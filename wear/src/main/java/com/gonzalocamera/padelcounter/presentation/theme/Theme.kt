package com.gonzalocamera.padelcounter.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/**
 * Tema Wear negro-oro, alineado con el módulo :mobile (DESIGN.md).
 * El oro es el único acento de marca; se reserva para lo destacado
 * (botones primarios, selecciones, switches activos, títulos).
 */
private val PadelWearColors = Colors(
    primary = WearBrand.Gold,
    primaryVariant = WearBrand.GoldDark,
    secondary = WearBrand.GoldLight,
    secondaryVariant = WearBrand.GoldDark,
    background = WearBrand.Background,
    surface = WearBrand.Card,
    error = WearBrand.Live,
    onPrimary = WearBrand.OnGold,
    onSecondary = WearBrand.OnGold,
    onBackground = WearBrand.Text,
    onSurface = WearBrand.Text,
    onSurfaceVariant = WearBrand.TextMuted,
    onError = WearBrand.Text
)

@Composable
fun PadelCounterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = PadelWearColors,
        content = content
    )
}
