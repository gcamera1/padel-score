package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.MatchSummary
import com.gonzalocamera.padelcounter.shared.Winner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val relativeFormat = SimpleDateFormat("dd MMM · HH:mm", Locale("es"))

@Composable
fun MatchCard(
    summary: MatchSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (summary.winner == Winner.MY) PadelTheme.colors.accentMine else PadelTheme.colors.accentRival
    val outcome = if (summary.winner == Winner.MY) "Victoria" else "Derrota"
    val scoreText = summary.setsScore.joinToString(", ") { "${it[0]} a ${it[1]}" }
    val cardShape = MaterialTheme.shapes.medium
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
                shape = cardShape,
            )
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$outcome, $scoreText"
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = cardShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = summary.setsScore.joinToString("  ") { "${it[0]}-${it[1]}" },
                    style = PadelTheme.sportType.matchCardScore.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = relativeFormat.format(Date(summary.finishedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    OriginPill(origin = summary.origin)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
private fun OriginPill(origin: MatchOrigin) {
    Text(
        text = when (origin) {
            MatchOrigin.MOBILE -> "Móvil"
            MatchOrigin.WEAR -> "Reloj"
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

