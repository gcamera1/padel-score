package com.gonzalocamera.padelcounter.mobile.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.gonzalocamera.padelcounter.mobile.ui.components.DonutChart
import com.gonzalocamera.padelcounter.mobile.ui.components.DonutSegment
import com.gonzalocamera.padelcounter.mobile.ui.components.DotStreak
import com.gonzalocamera.padelcounter.mobile.ui.components.GoldProgressBar
import com.gonzalocamera.padelcounter.mobile.ui.components.PremiumCard
import com.gonzalocamera.padelcounter.mobile.ui.components.SectionHeader
import com.gonzalocamera.padelcounter.mobile.ui.components.StatTile
import com.gonzalocamera.padelcounter.mobile.ui.components.StrokeVerdictBadge
import com.gonzalocamera.padelcounter.mobile.ui.components.WinLossSparkline
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.AggregateStats
import com.gonzalocamera.padelcounter.shared.StrokeAggregate
import com.gonzalocamera.padelcounter.shared.Winner

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val stats by viewModel.stats.collectAsState()
    val last7 by viewModel.winLossLast7.collectAsState()
    val strokeStats by viewModel.strokeStats.collectAsState()
    val expanded = currentWindowAdaptiveInfo()
        .windowSizeClass
        .windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    StatsContent(stats = stats, last7 = last7, strokeStats = strokeStats, expanded = expanded)
}

@Composable
internal fun StatsContent(
    stats: AggregateStats,
    last7: List<Winner> = emptyList(),
    strokeStats: StrokeAggregate = StrokeAggregate(0, 0, 0f, null, 0),
    expanded: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PadelPalette.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Estadísticas",
            style = MaterialTheme.typography.displayMedium,
            color = PadelTheme.colors.gold,
        )
        Text(
            text = "${stats.totalMatches} PARTIDOS JUGADOS",
            style = PadelTheme.sportType.sectionHeader,
            color = PadelTheme.colors.textFaint,
        )

        // --- Rendimiento donut (Victorias vs Derrotas) ---
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                SectionHeader("Rendimiento general")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DonutChart(
                        segments = listOf(
                            DonutSegment(stats.totalWins.toFloat(), PadelPalette.Gold),
                            DonutSegment(stats.totalLosses.toFloat(), PadelPalette.Wear),
                        ),
                        centerValue = stats.totalMatches.toString(),
                        centerCaption = "Partidos",
                        diameter = 132.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendRow("Ganados", stats.totalWins, PadelPalette.Gold)
                        LegendRow("Perdidos", stats.totalLosses, PadelPalette.Wear)
                        LegendRow(
                            "Win %",
                            stats.winPercentage ?: 0,
                            PadelPalette.GoldLight,
                            suffix = "%",
                        )
                    }
                }
            }
        }

        // --- Quick tiles ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "Victorias",
                value = if (stats.totalMatches == 0) "—" else "${stats.winPercentage ?: 0}%",
                accent = PadelTheme.colors.goldLight,
                modifier = Modifier.weight(1f),
                trailing = { WinLossSparkline(results = last7) },
            )
            StatTile(
                label = "Racha actual",
                value = (maxOf(stats.currentWinStreak, stats.currentLossStreak)).toString(),
                accent = if (stats.currentWinStreak >= stats.currentLossStreak)
                    PadelTheme.colors.goldLight else PadelPalette.Text,
                modifier = Modifier.weight(1f),
                trailing = {
                    DotStreak(
                        streak = maxOf(stats.currentWinStreak, stats.currentLossStreak),
                        total = 6,
                        accent = if (stats.currentWinStreak >= stats.currentLossStreak)
                            PadelTheme.colors.goldLight else PadelPalette.Text,
                    )
                },
            )
        }

        // --- Partidos / Sets breakdown ---
        RatioCard(
            title = "Partidos",
            myLabel = "Ganados",
            myCount = stats.totalWins,
            oppLabel = "Perdidos",
            oppCount = stats.totalLosses,
        )
        RatioCard(
            title = "Sets",
            myLabel = "Ganados",
            myCount = stats.totalSetsWon,
            oppLabel = "Perdidos",
            oppCount = stats.totalSetsLost,
        )

        // --- Golpes ---
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                SectionHeader("Golpes")
                Spacer(modifier = Modifier.height(12.dp))
                if (strokeStats.matchesWithData == 0) {
                    Text(
                        text = "Todavía no registraste partidos con golpes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PadelTheme.colors.textMuted,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = strokeStats.totalStrokes.toString(),
                                style = PadelTheme.sportType.setGameNumeral,
                                color = PadelTheme.colors.goldLight,
                            )
                            Text(
                                text = "GOLPES · ${"%.1f".format(strokeStats.avgPgg)} PGG",
                                style = PadelTheme.sportType.sectionHeader,
                                color = PadelTheme.colors.textFaint,
                            )
                        }
                        strokeStats.verdict?.let { StrokeVerdictBadge(it) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Partido más maratónico",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PadelTheme.colors.textMuted,
                        )
                        Text(
                            text = "${strokeStats.maxStrokesInMatch} golpes",
                            style = PadelTheme.sportType.matchCardScore,
                            color = PadelPalette.Text,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LegendRow(label: String, value: Int, color: Color, suffix: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column {
            Text(
                text = label.uppercase(),
                style = PadelTheme.sportType.sectionHeader,
                color = PadelTheme.colors.textFaint,
            )
            Text(
                text = "$value$suffix",
                style = PadelTheme.sportType.matchCardScore,
                color = PadelPalette.Text,
            )
        }
    }
}

@Composable
private fun RatioCard(
    title: String,
    myLabel: String,
    myCount: Int,
    oppLabel: String,
    oppCount: Int,
) {
    val total = (myCount + oppCount).coerceAtLeast(1)
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LabeledNumber(myLabel, myCount, PadelTheme.colors.goldLight)
                LabeledNumber(oppLabel, oppCount, PadelTheme.colors.textMuted)
            }
            Spacer(modifier = Modifier.height(12.dp))
            GoldProgressBar(
                fraction = myCount.toFloat() / total,
                color = PadelPalette.Gold,
            )
        }
    }
}

@Composable
private fun LabeledNumber(label: String, value: Int, accent: Color) {
    Column {
        Text(
            text = label.uppercase(),
            style = PadelTheme.sportType.sectionHeader,
            color = PadelTheme.colors.textFaint,
        )
        Text(
            text = value.toString(),
            style = PadelTheme.sportType.setGameNumeral,
            color = accent,
        )
    }
}
