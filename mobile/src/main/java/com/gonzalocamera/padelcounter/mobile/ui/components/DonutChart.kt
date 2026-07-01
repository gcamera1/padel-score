package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

data class DonutSegment(val value: Float, val color: Color)

/**
 * Segmented donut used on the Stats screen (mirrors "Rendimiento Total" /
 * "GOLPES TOTALES" in the reference). Renders rounded arcs over a track and
 * stacks a centered numeral + caption.
 */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    centerValue: String,
    centerCaption: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    strokeWidth: Dp = 16.dp,
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val sw = strokeWidth.toPx()
            val inset = sw / 2f
            val arcSize = Size(size.width - sw, size.height - sw)
            val topLeft = Offset(inset, inset)
            // Track
            drawArc(
                color = PadelPalette.DonutTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw),
            )
            var start = -90f
            val gap = 4f
            segments.filter { it.value > 0f }.forEach { seg ->
                val sweep = (seg.value / total) * 360f
                drawArc(
                    color = seg.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
                start += sweep
            }
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerCaption.uppercase(),
                style = PadelTheme.sportType.sectionHeader,
                color = PadelTheme.colors.textFaint,
            )
            Text(
                text = centerValue,
                style = PadelTheme.sportType.setGameNumeral,
                color = PadelPalette.Text,
            )
        }
    }
}
