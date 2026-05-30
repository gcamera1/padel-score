package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.Winner

@Composable
fun WinLossSparkline(
    results: List<Winner>,
    modifier: Modifier = Modifier,
) {
    val mine = PadelTheme.colors.accentMine
    val rival = PadelTheme.colors.accentRival
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
    ) {
        if (results.isEmpty()) return@Canvas
        val barWidth = size.width / results.size
        val gap = barWidth * 0.18f
        results.forEachIndexed { i, w ->
            val x = i * barWidth + gap / 2
            val barH = if (w == Winner.MY) size.height * 0.95f else size.height * 0.45f
            val y = size.height - barH
            val color = if (w == Winner.MY) mine else rival
            drawRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth - gap, barH),
            )
        }
    }
}
