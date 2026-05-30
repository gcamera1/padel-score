package com.gonzalocamera.padelcounter.mobile.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.gonzalocamera.padelcounter.mobile.ui.components.DotStreak
import com.gonzalocamera.padelcounter.mobile.ui.components.SectionHeader
import com.gonzalocamera.padelcounter.mobile.ui.components.StatTile
import com.gonzalocamera.padelcounter.mobile.ui.components.WinLossSparkline
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.AggregateStats
import com.gonzalocamera.padelcounter.shared.Winner

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val stats by viewModel.stats.collectAsState()
    val last7 by viewModel.winLossLast7.collectAsState()
    val expanded = currentWindowAdaptiveInfo()
        .windowSizeClass
        .windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    StatsContent(stats = stats, last7 = last7, expanded = expanded)
}

@Composable
internal fun StatsContent(
    stats: AggregateStats,
    last7: List<Winner> = emptyList(),
    expanded: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Estadísticas",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "${stats.totalMatches} partidos jugados",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "Victorias",
                value = if (stats.totalMatches == 0) "—" else "${stats.winPercentage ?: 0}%",
                accent = PadelTheme.colors.accentMine,
                modifier = Modifier.weight(1f),
                trailing = { WinLossSparkline(results = last7) },
            )
            StatTile(
                label = "Racha actual",
                value = (maxOf(stats.currentWinStreak, stats.currentLossStreak)).toString(),
                accent = if (stats.currentWinStreak >= stats.currentLossStreak)
                    PadelTheme.colors.accentMine else PadelTheme.colors.accentRival,
                modifier = Modifier.weight(1f),
                trailing = {
                    DotStreak(
                        streak = maxOf(stats.currentWinStreak, stats.currentLossStreak),
                        total = 6,
                        accent = if (stats.currentWinStreak >= stats.currentLossStreak)
                            PadelTheme.colors.accentMine else PadelTheme.colors.accentRival,
                    )
                },
            )
            if (expanded) {
                val setsTotal = (stats.totalSetsWon + stats.totalSetsLost).coerceAtLeast(1)
                val setsPct = (stats.totalSetsWon * 100) / setsTotal
                StatTile(
                    label = "Sets ganados",
                    value = if (stats.totalMatches == 0) "—" else "$setsPct%",
                    accent = PadelTheme.colors.accentMine,
                    modifier = Modifier.weight(1f),
                    trailing = {
                        StackedRatioBar(
                            myCount = stats.totalSetsWon,
                            oppCount = stats.totalSetsLost,
                        )
                    },
                )
            }
        }

        Column {
            SectionHeader("PARTIDOS")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LabeledNumber("Ganados", stats.totalWins, PadelTheme.colors.accentMine)
                LabeledNumber("Perdidos", stats.totalLosses, PadelTheme.colors.accentRival)
            }
            Spacer(modifier = Modifier.height(10.dp))
            StackedRatioBar(
                myCount = stats.totalWins,
                oppCount = stats.totalLosses,
            )
        }

        Column {
            SectionHeader("SETS")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LabeledNumber("Ganados", stats.totalSetsWon, PadelTheme.colors.accentMine)
                LabeledNumber("Perdidos", stats.totalSetsLost, PadelTheme.colors.accentRival)
            }
            Spacer(modifier = Modifier.height(10.dp))
            StackedRatioBar(
                myCount = stats.totalSetsWon,
                oppCount = stats.totalSetsLost,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LabeledNumber(label: String, value: Int, accent: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )
        Text(
            text = value.toString(),
            style = PadelTheme.sportType.setGameNumeral.copy(fontFeatureSettings = "tnum"),
            color = accent,
        )
    }
}

@Composable
private fun StackedRatioBar(myCount: Int, oppCount: Int) {
    val total = (myCount + oppCount).coerceAtLeast(1)
    val myFrac = myCount.toFloat() / total
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp)),
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
}
