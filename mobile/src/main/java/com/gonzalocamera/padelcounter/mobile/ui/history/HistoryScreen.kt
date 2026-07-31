package com.gonzalocamera.padelcounter.mobile.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.components.EmptyState
import com.gonzalocamera.padelcounter.mobile.ui.components.MatchCard
import com.gonzalocamera.padelcounter.mobile.ui.components.SectionHeader
import com.gonzalocamera.padelcounter.mobile.ui.components.ServeBall
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val monthFormat = SimpleDateFormat("MMM yyyy", Locale("es"))

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    @Suppress("UNUSED_PARAMETER") onMatchClick: (String) -> Unit = {},
    onPlayMatch: () -> Unit,
) {
    val matches by viewModel.matches.collectAsState()
    val aggregate by viewModel.aggregateLite.collectAsState()
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()
    val scope = rememberCoroutineScope()
    var selectedMatchId by remember { mutableStateOf<String?>(null) }
    var showManualSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
            selectedMatchId = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                HistoryUiEvent.ManualMatchSaved ->
                    snackbarHostState.showSnackbar("Partido guardado")
                is HistoryUiEvent.ShowError ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                Box(modifier = Modifier.fillMaxSize()) {
                    HistoryScreenContent(
                        matches = matches,
                        totalMatches = aggregate.totalMatches,
                        winPct = aggregate.winPct,
                        onMatchClick = { matchId ->
                            selectedMatchId = matchId
                            scope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                            }
                        },
                        onPlayMatch = onPlayMatch,
                        onAddManualMatch = { showManualSheet = true },
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 88.dp),
                    ) { data ->
                        // El default de M3 es un panel claro que rompe el negro-oro.
                        Snackbar(
                            snackbarData = data,
                            containerColor = PadelPalette.Gray,
                            contentColor = PadelPalette.Text,
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                val matchId = selectedMatchId
                if (matchId != null) {
                    InlineMatchDetailPane(
                        matchId = matchId,
                        viewModel = viewModel,
                        onBack = {
                            scope.launch {
                                navigator.navigateBack()
                                selectedMatchId = null
                            }
                        },
                    )
                } else {
                    EmptyDetailHint(
                        showBackToList = navigator.canNavigateBack(),
                        onBackToList = {
                            scope.launch {
                                navigator.navigateBack()
                                selectedMatchId = null
                            }
                        },
                    )
                }
            }
        },
    )

    // Fuera del scaffold: el sheet vive en su propia ventana y no debe quedar dentro
    // de un AnimatedPane que puede desmontarse durante una transición de panel.
    if (showManualSheet) {
        ManualMatchSheet(
            onDismiss = { showManualSheet = false },
            onConfirm = { draft ->
                viewModel.saveManualMatch(draft)
                showManualSheet = false
            },
        )
    }
}

@Composable
private fun EmptyDetailHint(
    showBackToList: Boolean,
    onBackToList: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PadelPalette.Background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ServeBall(size = 44.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Elegí un partido",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tocá un partido del historial para ver su detalle.",
            style = MaterialTheme.typography.bodyMedium,
            color = PadelTheme.colors.textMuted,
            textAlign = TextAlign.Center,
        )
        if (showBackToList) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBackToList) {
                Text("Volver al historial")
            }
        }
    }
}

@Composable
private fun InlineMatchDetailPane(
    matchId: String,
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    var match by remember { mutableStateOf<Match?>(null) }
    val category by viewModel.category.collectAsState()
    LaunchedEffect(matchId) { match = viewModel.getMatchDetail(matchId) }
    match?.let {
        InlineMatchDetailScaffold(
            match = it,
            onBack = onBack,
            onDelete = {
                viewModel.deleteMatch(matchId)
                onBack()
            },
            category = category,
        )
    }
}

@Composable
internal fun HistoryScreenContent(
    matches: List<MatchSummary>,
    totalMatches: Int,
    winPct: Int,
    onMatchClick: (String) -> Unit,
    onPlayMatch: () -> Unit,
    onAddManualMatch: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PadelPalette.Background),
    ) {
        if (matches.isEmpty()) {
            EmptyState(
                title = "Aún no jugaste ningún partido",
                subtitle = "Arrancá uno y te lo guardo acá.",
                ctaLabel = "Jugar un partido",
                onCta = onPlayMatch,
                decoration = {
                    Column(modifier = Modifier.alpha(0.32f)) {
                        ServeBall(size = 56.dp)
                    }
                },
            )
        } else {
            HistoryList(
                matches = matches,
                totalMatches = totalMatches,
                winPct = winPct,
                onMatchClick = onMatchClick,
            )
        }

        FloatingActionButton(
            onClick = onAddManualMatch,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Cargar partido manualmente")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryList(
    matches: List<MatchSummary>,
    totalMatches: Int,
    winPct: Int,
    onMatchClick: (String) -> Unit,
) {
    val grouped = matches.groupedByPeriod()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Historial",
                style = MaterialTheme.typography.displayMedium,
                color = PadelTheme.colors.gold,
            )
            Text(
                text = "$totalMatches ${if (totalMatches == 1) "PARTIDO" else "PARTIDOS"} · $winPct% V",
                style = PadelTheme.sportType.sectionHeader,
                color = PadelTheme.colors.textFaint,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
        }

        grouped.forEach { (period, list) ->
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PadelPalette.Background)
                        .padding(vertical = 8.dp),
                ) {
                    SectionHeader(period)
                }
            }
            items(items = list, key = { it.id }) { match ->
                MatchCard(summary = match, onClick = { onMatchClick(match.id) })
            }
        }

        // Colchón para que el FAB no tape la última card: 56 (FAB) + 16 (margen) + 16.
        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}

private fun List<MatchSummary>.groupedByPeriod(): Map<String, List<MatchSummary>> {
    val now = Calendar.getInstance()
    val startOfToday = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfWeek = (startOfToday.clone() as Calendar).apply {
        val offset = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY).let { if (it < 0) it + 7 else it }
        add(Calendar.DAY_OF_YEAR, -offset)
    }
    val startOfMonth = (startOfToday.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }

    val result = linkedMapOf<String, MutableList<MatchSummary>>()
    for (match in this.sortedByDescending { it.finishedAt }) {
        val time = match.finishedAt
        val key = when {
            time >= startOfToday.timeInMillis -> "HOY"
            time >= startOfWeek.timeInMillis -> "ESTA SEMANA"
            time >= startOfMonth.timeInMillis -> "ESTE MES"
            else -> monthFormat.format(Date(time)).uppercase(Locale("es"))
        }
        result.getOrPut(key) { mutableListOf() }.add(match)
    }
    return result
}
