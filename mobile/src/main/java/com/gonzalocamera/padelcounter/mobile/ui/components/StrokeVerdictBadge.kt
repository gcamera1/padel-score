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
import com.gonzalocamera.padelcounter.shared.StrokeVerdict

/** Emoji + copy en español para cada veredicto de golpes. */
fun StrokeVerdict.display(): Pair<String, String> = when (this) {
    StrokeVerdict.FRIDGE -> "🧊" to "Heladera"
    StrokeVerdict.NORMAL -> "⚖️" to "Normal"
    StrokeVerdict.HIGH_LOAD -> "🔨" to "Alto desgaste"
    StrokeVerdict.MARATHON -> "🦸" to "Maratón"
}

private fun StrokeVerdict.tone(): Pair<Color, Color> = when (this) {
    StrokeVerdict.FRIDGE -> Color(0xFF1E3A5F) to Color(0xFFBFE0FF)
    StrokeVerdict.NORMAL -> Color(0xFF1F3D2B) to Color(0xFFB8F0C9)
    StrokeVerdict.HIGH_LOAD -> Color(0xFF5A3A12) to Color(0xFFFFD8A8)
    StrokeVerdict.MARATHON -> Color(0xFF5A1A1A) to Color(0xFFFFC2C2)
}

/** Badge con el diagnóstico de volumen de golpes (emoji + texto). */
@Composable
fun StrokeVerdictBadge(
    verdict: StrokeVerdict,
    modifier: Modifier = Modifier,
) {
    val (emoji, label) = verdict.display()
    val (bg, fg) = verdict.tone()
    Text(
        text = "$emoji $label",
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = PadelTheme.sportType.sectionHeader,
        color = fg,
    )
}
