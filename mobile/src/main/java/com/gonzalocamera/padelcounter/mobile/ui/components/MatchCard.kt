package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
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
    val win = summary.winner == Winner.MY
    val outcome = if (win) "Victoria" else "Derrota"
    val scoreText = summary.setsScore.joinToString(", ") { "${it[0]} a ${it[1]}" }

    PremiumCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$outcome, $scoreText"
            },
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Outcome chip — gold for win, neutral for loss (red reserved for LIVE).
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (win) PadelPalette.Gold.copy(alpha = 0.16f)
                        else PadelPalette.Gray,
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (win) "WIN" else "LOSS",
                    style = PadelTheme.sportType.sectionHeader,
                    color = if (win) PadelTheme.colors.goldLight else PadelTheme.colors.textMuted,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = summary.setsScore.joinToString("   ") { "${it[0]}-${it[1]}" },
                    style = PadelTheme.sportType.matchCardScore,
                    color = if (win) PadelTheme.colors.goldLight else PadelPalette.Text,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = relativeFormat.format(Date(summary.finishedAt)).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = PadelTheme.colors.textFaint,
                    )
                    OriginPill(origin = summary.origin)
                }
            }
        }
    }
}

@Composable
private fun OriginPill(origin: MatchOrigin) {
    Text(
        text = when (origin) {
            MatchOrigin.MOBILE -> "MÓVIL"
            MatchOrigin.WEAR -> "RELOJ"
        },
        style = MaterialTheme.typography.labelSmall,
        color = PadelTheme.colors.textMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PadelPalette.Gray)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
