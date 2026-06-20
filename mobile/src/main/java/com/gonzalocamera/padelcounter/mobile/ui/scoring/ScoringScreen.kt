package com.gonzalocamera.padelcounter.mobile.ui.scoring

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.components.BadgeTone
import com.gonzalocamera.padelcounter.mobile.ui.components.ContextBadge
import com.gonzalocamera.padelcounter.mobile.ui.components.PadelCourt
import com.gonzalocamera.padelcounter.mobile.ui.components.ScoreDisplay
import com.gonzalocamera.padelcounter.mobile.ui.components.ServeBall
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.Winner
import com.gonzalocamera.padelcounter.shared.branding.hex
import com.gonzalocamera.padelcounter.shared.isMatchFinished
import com.gonzalocamera.padelcounter.shared.isStarPointDecider
import com.gonzalocamera.padelcounter.shared.pointsLabel

private fun courtColorToColor(option: CourtColorOption): Color = Color(option.hex())

/**
 * Positions the serve ball in the corner matching the serving player's service box,
 * replicating the watch's mirrored-perspective layout. RIVAL is at the top (portrait)
 * or left (landscape); YO is at the bottom or right.
 */
private fun serveBallAlignment(myServe: Boolean, serveFromRight: Boolean, landscape: Boolean): Alignment =
    if (!landscape) {
        when {
            !myServe && serveFromRight -> Alignment.TopStart
            !myServe && !serveFromRight -> Alignment.TopEnd
            myServe && serveFromRight -> Alignment.BottomEnd
            else -> Alignment.BottomStart
        }
    } else {
        when {
            !myServe && serveFromRight -> Alignment.BottomStart
            !myServe && !serveFromRight -> Alignment.TopStart
            myServe && serveFromRight -> Alignment.TopEnd
            else -> Alignment.BottomEnd
        }
    }

private fun isMatchStart(state: PadelState): Boolean =
    state.mySets == 0 && state.oppSets == 0 &&
        state.myGames == 0 && state.oppGames == 0 &&
        state.myPointsIdx == 0 && state.oppPointsIdx == 0 &&
        !state.inTieBreak

@Composable
fun ScoringScreen(viewModel: ScoringViewModel) {
    val state by viewModel.state.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingWinner by remember { mutableStateOf<Winner?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScoringUiEvent.MatchSaved -> pendingWinner = null
                is ScoringUiEvent.ShowError -> errorMessage = event.message
            }
        }
    }

    ScoringScreenContent(
        state = state,
        onTapMy = { viewModel.addPointToMy() },
        onTapOpp = { viewModel.addPointToOpp() },
        onLongPressMy = { viewModel.subtractPointFromMy() },
        onLongPressOpp = { viewModel.subtractPointFromOpp() },
        onSetServe = { viewModel.setServe(it) },
        onStartNewMatch = { d, m, b -> viewModel.startNewMatch(d, m, b) },
        onFinalize = { winner ->
            pendingWinner = winner
            viewModel.finalizeMatch(winner)
        },
        onDiscard = { viewModel.discardMatch() },
    )

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("No se pudo guardar") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    errorMessage = null
                    pendingWinner?.let { viewModel.retryFinalize(it) }
                }) { Text("Reintentar") }
            },
            dismissButton = {
                TextButton(onClick = { errorMessage = null }) { Text("Cerrar") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ScoringScreenContent(
    state: PadelState,
    onTapMy: () -> Unit,
    onTapOpp: () -> Unit,
    onLongPressMy: () -> Unit,
    onLongPressOpp: () -> Unit,
    onSetServe: (Boolean) -> Unit,
    onStartNewMatch: (Decider, ScoringMode, Int) -> Unit,
    onFinalize: (Winner) -> Unit,
    onDiscard: () -> Unit,
) {
    var showNewMatchSheet by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (isMatchFinished(state) && !showFinishDialog) showFinishDialog = true
    }

    val hasActiveMatch = !isMatchStart(state) || state.isServeSet

    Box(modifier = Modifier.fillMaxSize()) {
        if (!state.isServeSet && isMatchStart(state)) {
            ServeSelectionScreen(
                onSelectMyServe = { onSetServe(true) },
                onSelectOppServe = { onSetServe(false) },
            )
        } else {
            CourtScreen(
                state = state,
                onTapMy = onTapMy,
                onTapOpp = onTapOpp,
                onLongPressMy = onLongPressMy,
                onLongPressOpp = onLongPressOpp,
            )
        }

        ExtendedFloatingActionButton(
            onClick = { showNewMatchSheet = true },
            expanded = !hasActiveMatch,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nuevo partido") },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    if (showNewMatchSheet) {
        NewMatchSheet(
            defaultDecider = state.decider,
            defaultScoringMode = state.scoringMode,
            defaultBestOf = state.bestOf,
            onDismiss = { showNewMatchSheet = false },
            onConfirm = { d, m, b ->
                onStartNewMatch(d, m, b)
                showNewMatchSheet = false
            },
        )
    }

    if (showFinishDialog) {
        val won = state.mySets > state.oppSets
        MatchEndSheet(
            state = state,
            won = won,
            onConfirm = {
                onFinalize(if (won) Winner.MY else Winner.OPP)
                showFinishDialog = false
            },
            onDiscard = {
                onDiscard()
                showFinishDialog = false
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ServeSelectionScreen(
    onSelectMyServe: () -> Unit,
    onSelectOppServe: () -> Unit,
) {
    val rival = PadelTheme.colors.accentRival
    val mine = PadelTheme.colors.accentMine
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(rival.copy(alpha = 0.18f))
                .combinedClickable(onClick = onSelectOppServe, onLongClick = {})
                .semantics {
                    role = Role.Button
                    contentDescription = "Saca rival"
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ServeBall(size = 48.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Saca rival",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "¿Quién saca?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(mine.copy(alpha = 0.26f))
                .combinedClickable(onClick = onSelectMyServe, onLongClick = {})
                .semantics {
                    role = Role.Button
                    contentDescription = "Saco yo"
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ServeBall(size = 48.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Saco yo",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CourtScreen(
    state: PadelState,
    onTapMy: () -> Unit,
    onTapOpp: () -> Unit,
    onLongPressMy: () -> Unit,
    onLongPressOpp: () -> Unit,
) {
    val courtColor = courtColorToColor(state.courtColor)
    val haptic = LocalHapticFeedback.current
    val motion = PadelTheme.motion
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var oppTaps by remember { mutableIntStateOf(0) }
    var myTaps by remember { mutableIntStateOf(0) }
    val oppFlash = remember { Animatable(0f) }
    val myFlash = remember { Animatable(0f) }
    LaunchedEffect(oppTaps) {
        if (oppTaps > 0) {
            oppFlash.snapTo(1f)
            oppFlash.animateTo(0f, tween(motion.durationFast, easing = motion.standard))
        }
    }
    LaunchedEffect(myTaps) {
        if (myTaps > 0) {
            myFlash.snapTo(1f)
            myFlash.animateTo(0f, tween(motion.durationFast, easing = motion.standard))
        }
    }

    val isAt4040 = state.myPointsIdx == 3 && state.oppPointsIdx == 3 && !state.inTieBreak
    val showGoldenPulse = isAt4040 && state.scoringMode != ScoringMode.STAR_POINT
    val pulse = if (showGoldenPulse) {
        val transition = rememberInfiniteTransition(label = "golden")
        transition.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.16f,
            animationSpec = infiniteRepeatable(
                animation = tween(motion.durationCelebration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "goldenAlpha",
        ).value
    } else 0f

    val rivalOverlay: @Composable () -> Unit = {
        if (showGoldenPulse && !state.myServe) {
            GoldenPulseOverlay(alpha = pulse)
        }
        if (oppTaps > 0) TapFlash(intensity = oppFlash.value)
    }
    val myOverlay: @Composable () -> Unit = {
        if (showGoldenPulse && state.myServe) {
            GoldenPulseOverlay(alpha = pulse)
        }
        if (myTaps > 0) TapFlash(intensity = myFlash.value)
    }
    val rivalScore: @Composable () -> Unit = {
        ScoreDisplay(
            label = "RIVAL",
            points = pointsLabel(state, isMe = false),
            sets = state.oppSets,
            games = state.oppGames,
            accent = PadelTheme.colors.accentRival,
        )
    }
    val myScore: @Composable () -> Unit = {
        ScoreDisplay(
            label = "YO",
            points = pointsLabel(state, isMe = true),
            sets = state.mySets,
            games = state.myGames,
            accent = PadelTheme.colors.accentMine,
        )
    }
    val oppMod = Modifier
        .combinedClickable(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                oppTaps++
                onTapOpp()
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPressOpp()
            },
        )
        .semantics {
            role = Role.Button
            contentDescription = "Sumar punto a rival; mantener para restar"
        }
    val myMod = Modifier
        .combinedClickable(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                myTaps++
                onTapMy()
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPressMy()
            },
        )
        .semantics {
            role = Role.Button
            contentDescription = "Sumar punto a yo; mantener para restar"
        }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        PadelCourt(courtColor = courtColor, modifier = Modifier.fillMaxSize())

        if (landscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .then(oppMod),
                    contentAlignment = Alignment.Center,
                ) { rivalOverlay(); rivalScore() }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .then(myMod),
                    contentAlignment = Alignment.Center,
                ) { myOverlay(); myScore() }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                CourtContextStrip(state = state)
            }

            if (state.isServeSet) {
                val align = serveBallAlignment(state.myServe, state.serveFromRight, landscape = true)
                val atTop = align == Alignment.TopStart || align == Alignment.TopEnd
                ServeBall(
                    size = 18.dp,
                    modifier = Modifier
                        .align(align)
                        .then(if (atTop) Modifier.statusBarsPadding() else Modifier.navigationBarsPadding())
                        .padding(horizontal = 28.dp, vertical = 16.dp),
                )
            }
        } else {
            // Portrait: anchor everything to the court geometry (mirrors PadelCourt).
            val netY = maxHeight / 2
            val serviceFromNet = maxHeight * 0.35f
            val serviceTopY = netY - serviceFromNet      // rival's service line
            val serviceBottomY = netY + serviceFromNet   // my service line

            // Full-height tappable halves (scoring + tap/golden overlays). Drawn first.
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(oppMod),
                ) { rivalOverlay() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(myMod),
                ) { myOverlay() }
            }

            // RIVAL score centered between its service line and the net.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(serviceFromNet)
                    .offset(y = serviceTopY),
                contentAlignment = Alignment.Center,
            ) { rivalScore() }

            // YO score centered between the net and its service line.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(serviceFromNet)
                    .offset(y = netY),
                contentAlignment = Alignment.Center,
            ) { myScore() }

            // Context badges centered on the net.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                CourtContextStrip(state = state)
            }

            // Serve ball sits just outside the serving player's service line, mirrored
            // (like the watch): YO on the right serves from screen-right, RIVAL mirrored.
            if (state.isServeSet) {
                val ballSize = 18.dp
                val ballOnRight = (state.myServe && state.serveFromRight) ||
                    (!state.myServe && !state.serveFromRight)
                val ballY = if (state.myServe) serviceBottomY + 2.dp
                else serviceTopY - 2.dp - ballSize
                ServeBall(
                    size = ballSize,
                    modifier = Modifier
                        .align(if (ballOnRight) Alignment.TopEnd else Alignment.TopStart)
                        .offset(y = ballY)
                        .padding(horizontal = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun CourtContextStrip(state: PadelState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.inTieBreak) {
            ContextBadge(text = "TIE-BREAK", tone = BadgeTone.TieBreak)
            Spacer(modifier = Modifier.size(8.dp))
        }
        if (isStarPointDecider(state)) {
            ContextBadge(text = "SP", tone = BadgeTone.Golden)
            Spacer(modifier = Modifier.size(8.dp))
        }
        ContextBadge(
            text = when (state.scoringMode) {
                ScoringMode.DEUCE -> "DEUCE"
                ScoringMode.GOLDEN_POINT -> "PUNTO DE ORO"
                ScoringMode.STAR_POINT -> "STAR POINT"
            },
            tone = BadgeTone.Mode,
        )
        Spacer(modifier = Modifier.size(8.dp))
        ContextBadge(
            text = when (state.decider) {
                Decider.TB7 -> "TB7"
                Decider.SUPER10 -> "S10"
            },
            tone = BadgeTone.Neutral,
        )
    }
}

@Composable
private fun GoldenPulseOverlay(alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        PadelTheme.colors.goldenGlow.copy(alpha = alpha),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

@Composable
private fun TapFlash(intensity: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = intensity * 0.08f)),
    )
}
