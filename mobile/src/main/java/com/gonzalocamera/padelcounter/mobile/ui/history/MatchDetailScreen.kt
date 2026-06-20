package com.gonzalocamera.padelcounter.mobile.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.components.PadelTopAppBar
import com.gonzalocamera.padelcounter.mobile.ui.components.SectionHeader
import com.gonzalocamera.padelcounter.mobile.ui.components.SetsBars
import com.gonzalocamera.padelcounter.mobile.ui.components.StrokeVerdictBadge
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.PadelCategory
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.Winner
import com.gonzalocamera.padelcounter.shared.strokeStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: String,
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    var match by remember { mutableStateOf<Match?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val category by viewModel.category.collectAsState()

    LaunchedEffect(matchId) {
        match = viewModel.getMatchDetail(matchId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            PadelTopAppBar(
                title = "Detalle",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            match == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Partido no encontrado",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            else -> MatchDetailContent(
                match = match!!,
                category = category,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar partido") },
            text = { Text("¿Seguro? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMatch(matchId)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
internal fun MatchDetailContent(
    match: Match,
    category: PadelCategory = PadelCategory.SEXTA,
    modifier: Modifier = Modifier,
) {
    val win = match.winner == Winner.MY
    val accent = if (win) PadelTheme.colors.accentMine else PadelTheme.colors.accentRival
    val durationMinutes = ((match.finishedAt - match.startedAt) / 60_000).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (win) PadelTheme.colors.winSurface else PadelTheme.colors.lossSurface,
            ),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (win) "VICTORIA" else "DERROTA",
                    style = MaterialTheme.typography.displaySmall,
                    color = accent,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = match.setsScore.joinToString("  ") { "${it[0]}-${it[1]}" },
                    style = PadelTheme.sportType.scoreNumeral.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(56f, androidx.compose.ui.unit.TextUnitType.Sp),
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = buildString {
                        append(when (match.decider) {
                            Decider.TB7 -> "TB7"
                            Decider.SUPER10 -> "S10"
                        })
                        append(" · ")
                        append(formatDuration(durationMinutes))
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        Column {
            SectionHeader("POR SET")
            Spacer(modifier = Modifier.height(12.dp))
            SetsBars(setsScore = match.setsScore)
        }

        match.strokeStats(category)?.let { s ->
            Column {
                SectionHeader("GOLPES")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = s.totalStrokes.toString(),
                            style = PadelTheme.sportType.setGameNumeral.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "golpes · ${"%.1f".format(s.pgg)} PGG",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        )
                    }
                    StrokeVerdictBadge(s.verdict)
                }
                Spacer(modifier = Modifier.height(12.dp))
                s.perSet.forEach { set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Set ${set.setIndex + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        )
                        Text(
                            text = "${set.strokes} · ${"%.1f".format(set.pgg)} PGG",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        StrokeVerdictBadge(set.verdict)
                    }
                }
            }
        }

        Column {
            SectionHeader("DETALLES")
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow("Fecha", dateFormat.format(Date(match.finishedAt)))
            DetailRow("Duración", formatDuration(durationMinutes))
            DetailRow("Modo", when (match.scoringMode) {
                ScoringMode.DEUCE -> "Deuce / Ventaja"
                ScoringMode.GOLDEN_POINT -> "Punto de Oro"
                ScoringMode.STAR_POINT -> "Star Point"
            })
            DetailRow("Formato", when (match.bestOf) {
                1 -> "Al mejor de 1 set"
                3 -> "Al mejor de 3 sets"
                5 -> "Al mejor de 5 sets"
                else -> "Al mejor de ${match.bestOf} sets"
            })
            DetailRow("Tie-break", if (match.tieBreakUsed) "Sí" else "No")
            DetailRow("Origen", when (match.origin) {
                MatchOrigin.WEAR -> "Reloj"
                MatchOrigin.MOBILE -> "Móvil"
            })
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun formatDuration(minutes: Long): String =
    if (minutes < 60) "$minutes min"
    else "${minutes / 60}h ${minutes % 60}min"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InlineMatchDetailScaffold(
    match: Match,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    category: PadelCategory = PadelCategory.SEXTA,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            PadelTopAppBar(
                title = "Detalle",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        MatchDetailContent(
            match = match,
            category = category,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar partido") },
            text = { Text("¿Seguro? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            },
        )
    }
}
