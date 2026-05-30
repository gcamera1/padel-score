package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

@Composable
fun SetsBars(
    setsScore: List<List<Int>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        setsScore.forEachIndexed { index, set ->
            val my = set[0]
            val opp = set[1]
            val total = (my + opp).coerceAtLeast(1)
            val myFrac = my.toFloat() / total
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "S${index + 1}",
                    style = PadelTheme.sportType.scoreLabel,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(myFrac.coerceAtLeast(0.001f))
                            .fillMaxWidth()
                            .background(PadelTheme.colors.accentMine),
                    )
                    Box(
                        modifier = Modifier
                            .weight((1f - myFrac).coerceAtLeast(0.001f))
                            .fillMaxWidth()
                            .background(PadelTheme.colors.accentRival),
                    )
                }
                Text(
                    text = "$my–$opp",
                    style = PadelTheme.sportType.matchCardScore.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
