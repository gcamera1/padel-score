package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
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
    val border: Color
    when (tone) {
        BadgeTone.TieBreak -> {
            bg = PadelPalette.Gold
            fg = Color(0xFF14110A)
            border = PadelPalette.GoldLight
        }
        BadgeTone.Golden -> {
            bg = PadelPalette.Gold.copy(alpha = 0.18f)
            fg = PadelPalette.GoldLight
            border = PadelPalette.Gold.copy(alpha = 0.45f)
        }
        BadgeTone.Mode -> {
            bg = PadelPalette.Gray
            fg = PadelPalette.Text
            border = Color.White.copy(alpha = 0.06f)
        }
        BadgeTone.Neutral -> {
            bg = PadelPalette.Gray.copy(alpha = 0.6f)
            fg = PadelTheme.colors.textMuted
            border = Color.White.copy(alpha = 0.06f)
        }
    }

    Text(
        text = text.uppercase(),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = PadelTheme.sportType.sectionHeader,
        color = fg,
    )
}
