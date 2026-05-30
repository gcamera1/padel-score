package com.gonzalocamera.padelcounter.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object PadelTheme {
    val colors: PadelColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPadelColors.current

    val sportType: PadelSportTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalPadelSportTypography.current

    val spacing: PadelSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalPadelSpacing.current

    val motion: PadelMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalPadelMotion.current
}
