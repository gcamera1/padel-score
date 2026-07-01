package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

/**
 * Signature surface of the premium redesign: a matte-black card with a thin
 * gold hairline border and a gentle top-to-bottom gradient, plus an optional
 * faint gold grid texture — mirrors the "Premier Padel" reference cards.
 *
 * @param featured draws a full-strength gold border (hero cards) vs a faint one.
 * @param grid overlays the subtle 14dp gold grid used on the scoreboard card.
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    featured: Boolean = false,
    grid: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = PadelTheme.colors
    val shape = MaterialTheme.shapes.medium
    val borderColor = if (featured) colors.gold else colors.hairline

    var base = modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(PadelPalette.Card, PadelPalette.Background),
            ),
        )
        .border(1.dp, borderColor, shape)
    if (onClick != null) base = base.clickable(onClick = onClick)

    Box(modifier = base) {
        if (grid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 14.dp.toPx()
                val line = PadelPalette.Gold.copy(alpha = 0.05f)
                var x = 0f
                while (x < size.width) {
                    drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
        }
        content()
    }
}
