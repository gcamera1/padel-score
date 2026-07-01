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
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.StrokeVerdict

/** Emoji + copy en español para cada veredicto de golpes. */
fun StrokeVerdict.display(): Pair<String, String> = when (this) {
    StrokeVerdict.FRIDGE -> "🧊" to "Heladera"
    StrokeVerdict.NORMAL -> "⚖️" to "Normal"
    StrokeVerdict.HIGH_LOAD -> "🔨" to "Alto desgaste"
    StrokeVerdict.MARATHON -> "🦸" to "Maratón"
}

// bg, fg, border — tuned to the matte-black + gold premium palette.
private fun StrokeVerdict.tone(): Triple<Color, Color, Color> = when (this) {
    StrokeVerdict.FRIDGE -> Triple(Color(0xFF12181F), Color(0xFF8FB4D6), Color(0xFF2A3A4A))
    StrokeVerdict.NORMAL -> Triple(Color(0xFF1A1D1F), Color(0xFFE2E5E8), Color(0xFF33383C))
    StrokeVerdict.HIGH_LOAD -> Triple(Color(0xFF2A2310), Color(0xFFE5C453), Color(0xFF8A6F30))
    StrokeVerdict.MARATHON -> Triple(Color(0xFF2A1414), Color(0xFFF09A9A), Color(0xFF5A2424))
}

/** Badge con el diagnóstico de volumen de golpes (emoji + texto). */
@Composable
fun StrokeVerdictBadge(
    verdict: StrokeVerdict,
    modifier: Modifier = Modifier,
) {
    val (emoji, label) = verdict.display()
    val (bg, fg, border) = verdict.tone()
    Text(
        text = "$emoji $label",
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = PadelTheme.sportType.sectionHeader,
        color = fg,
        maxLines = 1,
        softWrap = false,
    )
}
