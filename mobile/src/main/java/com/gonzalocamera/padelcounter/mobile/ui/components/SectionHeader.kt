package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = PadelTheme.sportType.sectionHeader,
        color = PadelTheme.colors.textMuted,
        modifier = modifier,
    )
}
