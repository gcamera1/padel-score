package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.material3.MaterialTheme
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
        text = text,
        style = PadelTheme.sportType.sectionHeader,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        modifier = modifier,
    )
}
