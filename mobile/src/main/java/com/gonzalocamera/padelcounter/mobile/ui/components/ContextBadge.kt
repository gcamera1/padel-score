package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

enum class BadgeTone { Neutral, TieBreak, Golden, Mode }

@Composable
fun ContextBadge(
    text: String,
    tone: BadgeTone = BadgeTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val bg: Color
    val fg: Color
    when (tone) {
        BadgeTone.TieBreak -> {
            bg = PadelTheme.colors.tieBreakHighlight
            fg = Color(0xFF1A0E00)
        }
        BadgeTone.Golden -> {
            bg = PadelTheme.colors.goldenGlow
            fg = Color(0xFF1A0E00)
        }
        BadgeTone.Mode -> {
            bg = Color.Black.copy(alpha = 0.32f)
            fg = Color.White.copy(alpha = 0.92f)
        }
        BadgeTone.Neutral -> {
            bg = Color.Black.copy(alpha = 0.24f)
            fg = Color.White.copy(alpha = 0.82f)
        }
    }

    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = PadelTheme.sportType.sectionHeader,
        color = fg,
    )
}
