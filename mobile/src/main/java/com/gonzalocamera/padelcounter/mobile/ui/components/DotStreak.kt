package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement

@Composable
fun DotStreak(
    streak: Int,
    total: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val filled = streak.coerceAtMost(total)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (i < filled) accent
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                    ),
            )
        }
    }
}
