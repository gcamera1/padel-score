package com.gonzalocamera.padelcounter.presentation

import android.app.Activity
import androidx.compose.ui.text.style.TextAlign
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.gonzalocamera.padelcounter.R
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.PadelState
import com.gonzalocamera.padelcounter.shared.ScoringMode
import com.gonzalocamera.padelcounter.shared.addPointToMy
import com.gonzalocamera.padelcounter.shared.addPointToOpp
import com.gonzalocamera.padelcounter.shared.branding.hex
import com.gonzalocamera.padelcounter.shared.subtractPointFromMy
import com.gonzalocamera.padelcounter.shared.subtractPointFromOpp
import com.gonzalocamera.padelcounter.shared.pointsLabel
import com.gonzalocamera.padelcounter.shared.isStarPointDecider
import com.gonzalocamera.padelcounter.shared.isMatchFinished
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.Winner
import com.gonzalocamera.padelcounter.sync.WearSyncQueue
import com.gonzalocamera.padelcounter.sync.WearSyncSender
import com.gonzalocamera.padelcounter.shared.StrokeSensitivity
import com.gonzalocamera.padelcounter.shared.StrokeDetector
import com.gonzalocamera.padelcounter.shared.thresholdMs2
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import kotlin.math.sqrt
import java.util.UUID

/**
 * Layout metrics calculated from screen size and shape (round vs square).
 * Ensures the court rectangle fits inside the circular display on round watches.
 *
 * Round safe area math: for a circle of diameter D, a centered rectangle (W×H)
 * fits when (W/D)² + (H/D)² ≤ 1.0
 */
internal data class ScreenMetrics(
    val isSmall: Boolean,
    val isRound: Boolean,
    val courtWidthFraction: Float,
    val courtHeightFraction: Float,
    val bigScore: androidx.compose.ui.unit.TextUnit,
    val smallSize: androidx.compose.ui.unit.TextUnit,
    val courtRadius: Dp,
    val courtPadding: Dp,
    val courtHorizontalPadding: Dp,
    val pointsYOffset: Dp,
    val gamesXOffset: Dp,
    val hintEndPadding: Dp
)

@Composable
internal fun rememberScreenMetrics(): ScreenMetrics {
    val config = LocalConfiguration.current
    val minDp = minOf(config.screenWidthDp, config.screenHeightDp)
    val isSmall = minDp <= 200
    val isRound = config.isScreenRound

    return remember(minDp, isRound) {
        when {
            isSmall && isRound -> ScreenMetrics(
                isSmall = true, isRound = true,
                courtWidthFraction = 0.68f, courtHeightFraction = 0.72f,
                bigScore = 34.sp, smallSize = 11.sp,
                courtRadius = 16.dp, courtPadding = 0.dp,
                courtHorizontalPadding = 4.dp,
                pointsYOffset = (-3).dp, gamesXOffset = (-4).dp,
                hintEndPadding = 14.dp
            )
            isSmall && !isRound -> ScreenMetrics(
                isSmall = true, isRound = false,
                courtWidthFraction = 0.88f, courtHeightFraction = 0.82f,
                bigScore = 38.sp, smallSize = 12.sp,
                courtRadius = 14.dp, courtPadding = 0.dp,
                courtHorizontalPadding = 6.dp,
                pointsYOffset = (-4).dp, gamesXOffset = 6.dp,
                hintEndPadding = 1.dp
            )
            !isSmall && isRound -> ScreenMetrics(
                isSmall = false, isRound = true,
                courtWidthFraction = 0.62f, courtHeightFraction = 0.72f,
                bigScore = 42.sp, smallSize = 13.sp,
                courtRadius = 18.dp, courtPadding = 1.dp,
                courtHorizontalPadding = 8.dp,
                pointsYOffset = (-5).dp, gamesXOffset = (-4).dp,
                hintEndPadding = 16.dp
            )
            else -> ScreenMetrics( // large square
                isSmall = false, isRound = false,
                courtWidthFraction = 0.60f, courtHeightFraction = 0.78f,
                bigScore = 54.sp, smallSize = 15.sp,
                courtRadius = 18.dp, courtPadding = 1.dp,
                courtHorizontalPadding = 10.dp,
                pointsYOffset = (-6).dp, gamesXOffset = 2.dp,
                hintEndPadding = 1.dp
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PadelApp() } }
    }
}

private enum class Screen { COUNTER, SETTINGS, NEW_MATCH, TUTORIAL, WALKTHROUGH, MATCH_FINISHED, STROKE_TEST }

@Composable
private fun PadelApp() {
    val context = LocalContext.current
    val repo = remember { PadelRepository(context) }
    val syncQueue = remember { WearSyncQueue(context) }
    val syncSender = remember { WearSyncSender(context, syncQueue) }
    val scope = rememberCoroutineScope()
    val state by repo.stateFlow.collectAsState(initial = PadelState())
    val matchStartedAt by repo.matchStartedAt.collectAsState(initial = null)
    val hasSeenWalkthrough by repo.hasSeenWalkthrough.collectAsState(initial = true)

    LaunchedEffect(state.keepScreenOn) {
        val act = context as? Activity ?: return@LaunchedEffect
        if (state.keepScreenOn) act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    var screen by remember { mutableStateOf(Screen.COUNTER) }
    var previousState by remember { mutableStateOf<PadelState?>(null) }
    var matchSynced by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        syncSender.trySendPending()
    }

    // Contador de golpes: arranca el service cuando el partido está en juego y el feature está ON.
    val matchActive = state.isServeSet && !isMatchFinished(state)
    LaunchedEffect(matchActive, state.strokeCountingEnabled) {
        val intent = Intent(context, StrokeCounterService::class.java)
        if (matchActive && state.strokeCountingEnabled) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }

    LaunchedEffect(state.mySets, state.oppSets) {
        if (isMatchFinished(state) && screen == Screen.COUNTER && !matchSynced) {
            matchSynced = true
            context.stopService(Intent(context, StrokeCounterService::class.java))
            val strokes = if (state.strokeCountingEnabled) {
                StrokeCounter.snapshot().takeIf { it.isNotEmpty() }
            } else null
            val match = Match(
                id = UUID.randomUUID().toString(),
                startedAt = matchStartedAt ?: System.currentTimeMillis(),
                finishedAt = System.currentTimeMillis(),
                setsScore = state.setsHistory,
                tieBreakUsed = state.setsHistory.any { it[0] == 7 || it[1] == 7 },
                decider = state.decider,
                goldenPoint = (state.scoringMode == ScoringMode.GOLDEN_POINT),
                scoringMode = state.scoringMode,
                winner = if (state.mySets > state.oppSets) Winner.MY else Winner.OPP,
                origin = MatchOrigin.WEAR,
                bestOf = state.bestOf,
                strokesPerSet = strokes
            )
            syncQueue.enqueue(match)
            syncSender.trySendPending()
            StrokeCounter.reset()
            repo.clearStrokeBackup()
            screen = Screen.MATCH_FINISHED
        }
    }

    // Walkthrough en primer inicio
    if (!hasSeenWalkthrough) {
        WalkthroughScreen(onFinish = {
            scope.launch { repo.setHasSeenWalkthrough() }
        })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = screen == Screen.COUNTER,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CounterScreen(
                state = state,
                onSave = { newState ->
                    previousState = state
                    scope.launch { repo.save(newState) }
                },
                onUndo = {
                    previousState?.let { prev ->
                        val current = state
                        previousState = current
                        scope.launch { repo.save(prev) }
                    }
                },
                onOpenSettings = { screen = Screen.SETTINGS }
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.SETTINGS,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsScreen(
                state = state,
                onToggleKeepOn = { scope.launch { repo.setKeepScreenOn(it) } },
                onCourtColorChange = { scope.launch { repo.setCourtColor(it) } },
                onToggleStrokeCounting = { scope.launch { repo.setStrokeCountingEnabled(it) } },
                onStrokeSensitivityChange = { scope.launch { repo.setStrokeSensitivity(it) } },
                onTestCounter = { screen = Screen.STROKE_TEST },
                onNewMatch = { screen = Screen.NEW_MATCH },
                onTutorial = { screen = Screen.TUTORIAL },
                onBack = { screen = Screen.COUNTER }
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.STROKE_TEST,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            StrokeTestScreen(
                state = state,
                onBack = { screen = Screen.SETTINGS }
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.NEW_MATCH,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            NewMatchScreen(
                initialScoringMode = state.scoringMode,
                initialDecider = state.decider,
                initialBestOf = state.bestOf,
                onConfirm = { decider, scoringMode, bestOf ->
                    scope.launch {
                        repo.resetMatchWithConfig(
                            decider = decider,
                            scoringMode = scoringMode,
                            courtColor = state.courtColor,
                            bestOf = bestOf
                        )
                    }
                    screen = Screen.COUNTER
                },
                onCancel = { screen = Screen.SETTINGS }
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.TUTORIAL,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            TutorialScreen(
                onBack = { screen = Screen.SETTINGS },
                onWalkthrough = { screen = Screen.WALKTHROUGH }
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.WALKTHROUGH,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            WalkthroughScreen(onFinish = { screen = Screen.TUTORIAL })
        }

        AnimatedVisibility(
            visible = screen == Screen.MATCH_FINISHED,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            MatchFinishedScreen(
                state = state,
                onNewMatch = {
                    matchSynced = false
                    scope.launch {
                        repo.resetMatchWithConfig(
                            decider = state.decider,
                            scoringMode = state.scoringMode,
                            courtColor = state.courtColor,
                            bestOf = state.bestOf
                        )
                    }
                    screen = Screen.COUNTER
                },
                onDismiss = { screen = Screen.COUNTER }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CounterScreen(
    state: PadelState,
    onSave: (PadelState) -> Unit,
    onUndo: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val metrics = rememberScreenMetrics()
    // Evita capturar un estado viejo en los handlers de tap (en Wear puede recomposear mas lento)
    val latestState by rememberUpdatedState(state)

    val myGreen = Color(0xFF00C853)
    val oppRed = Color(0xFFFF5252)

    // Swipe para ir a Ajustes
    var dragAccum by remember { mutableStateOf(0f) }
    val swipeThresholdPx = 110f

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(metrics.courtPadding)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                        onDragEnd = {
                            if (abs(dragAccum) >= swipeThresholdPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenSettings()
                            }
                            dragAccum = 0f
                        }
                    )
                }
        ) {
            val courtModifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(metrics.courtWidthFraction)
                .fillMaxHeight(metrics.courtHeightFraction)
                .clip(RoundedCornerShape(metrics.courtRadius))

            val needsServeSelection = !state.isServeSet && isMatchStart(state)

            Box(modifier = courtModifier) {
                // Fondo cancha
                CourtBackgroundVertical(
                    courtColor = courtColorToColor(state.courtColor),
                    isSmall = metrics.isSmall,
                    modifier = Modifier.fillMaxSize()
                )

                // Sets a la izquierda, DENTRO de la cancha (oculto durante seleccion de saque)
                if (!needsServeSelection) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.oppSets.toString(),
                            color = oppRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = metrics.smallSize,
                            maxLines = 1
                        )
                        Text(
                            text = "-",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = metrics.smallSize,
                            maxLines = 1
                        )
                        Text(
                            text = state.mySets.toString(),
                            color = myGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = metrics.smallSize,
                            maxLines = 1
                        )
                    }
                }

                // Pelota central durante seleccion de quien saca
                if (needsServeSelection) {
                    Image(
                        painter = painterResource(id = R.drawable.padelball),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(with(LocalDensity.current) { metrics.bigScore.toDp() })
                    )
                }

                // Indicador "SP": punto definitorio del game en modo Star Point
                if (!needsServeSelection && isStarPointDecider(state)) {
                    Text(
                        text = "SP",
                        color = Color(0xFF1A0E00),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = metrics.smallSize * 0.72f,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFC9A46C).copy(alpha = 0.92f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // Contenido principal (puntos + games)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = metrics.courtHorizontalPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rival (arriba)
                    TapZone(
                        modifier = Modifier.weight(1f),
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val s = latestState
                            if (!s.isServeSet && isMatchStart(s)) {
                                onSave(s.copy(isServeSet = true, myServe = false, serveFromRight = true))
                            } else {
                                onSave(addPointToOpp(s))
                            }
                        },
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(subtractPointFromOpp(latestState))
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUndo()
                        },
                        highlightColor = oppRed.copy(alpha = 0.18f)
                    ) {
                        if (!needsServeSelection) {
                            ScoreLine(
                                games = state.oppGames,
                                pointsText = pointsLabel(state, isMe = false),
                                pointsColor = oppRed,
                                bigScore = metrics.bigScore,
                                smallSize = metrics.smallSize,
                                pointsYOffset = metrics.pointsYOffset,
                                gamesXOffset = metrics.gamesXOffset
                            )
                        }
                    }

                    // Vos (abajo)
                    TapZone(
                        modifier = Modifier.weight(1f),
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val s = latestState
                            if (!s.isServeSet && isMatchStart(s)) {
                                onSave(s.copy(isServeSet = true, myServe = true, serveFromRight = true))
                            } else {
                                onSave(addPointToMy(s))
                            }
                        },
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(subtractPointFromMy(latestState))
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUndo()
                        },
                        highlightColor = myGreen.copy(alpha = 0.18f)
                    ) {
                        if (!needsServeSelection) {
                            ScoreLine(
                                games = state.myGames,
                                pointsText = pointsLabel(state, isMe = true),
                                pointsColor = myGreen,
                                bigScore = metrics.bigScore,
                                smallSize = metrics.smallSize,
                                gamesXOffset = metrics.gamesXOffset
                            )
                        }
                    }
                }

                // Serve indicator (padel ball)
                if (state.isServeSet) {
                    val ballSize = if (metrics.isSmall) 7.dp else 9.dp
                    val hPad = if (metrics.isSmall) 12.dp else 16.dp
                    val vPad = if (metrics.isSmall) 6.dp else 8.dp

                    // Opponent serves from right = viewer's top-left (mirrored perspective)
                    val alignment: Alignment = when {
                        !state.myServe && state.serveFromRight -> Alignment.TopStart
                        !state.myServe && !state.serveFromRight -> Alignment.TopEnd
                        state.myServe && state.serveFromRight -> Alignment.BottomEnd
                        else -> Alignment.BottomStart
                    }

                    Image(
                        painter = painterResource(id = R.drawable.padelball),
                        contentDescription = null,
                        modifier = Modifier
                            .align(alignment)
                            .padding(horizontal = hPad, vertical = vPad)
                            .size(ballSize)
                    )
                }
            }
            // Hint visual para indicar swipe a Ajustes (fuera del clip de la cancha)
            SwipeToSettingsHint(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = metrics.hintEndPadding)
            )
        }
    }
}



@Composable
private fun SwipeToSettingsHint(
    modifier: Modifier = Modifier,
) {
    // Pequeno indicador: puntitos + chevron. Fondo blanco para visibilidad sobre pantallas oscuras.
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Tres puntitos verticales
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.75f))
                )
            }
        }

        // Chevron hacia la izquierda (swipe hacia la izquierda para abrir)
        Text(
            text = "‹",
            color = Color.Black.copy(alpha = 0.80f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScoreLine(
    games: Int,
    pointsText: String,
    pointsColor: Color,
    bigScore: androidx.compose.ui.unit.TextUnit,
    smallSize: androidx.compose.ui.unit.TextUnit,
    pointsYOffset: Dp = 0.dp,
    gamesXOffset: Dp = 0.dp
) {
    // Box para que los puntos queden centrados y los games a la derecha sin mover el centro.
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = pointsText,
                fontWeight = FontWeight.ExtraBold,
                color = pointsColor,
                fontSize = bigScore,
                maxLines = 1,
                modifier = Modifier.offset(y = pointsYOffset)
            )
        }

        // Games a la derecha (sin ceros a la izquierda)
        Text(
            text = "G$games",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = gamesXOffset),
            fontWeight = FontWeight.SemiBold,
            fontSize = smallSize,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}

@Composable
private fun TapZone(
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit = {},
    highlightColor: Color,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Brillo/flash de feedback tactil
        if (pressed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(highlightColor)
            )
        }
        content()
    }
}

/**
 * Cancha vertical "mas real":
 * - Net al centro.
 * - Linea de saque a 3m del net (sobre 10m de media cancha) => 3/10 del semialto.
 * Importante: NO achicar otra vez adentro, porque eso reintroduce el "padding"/borde negro.
 */
@Composable
private fun CourtBackgroundVertical(
    courtColor: Color,
    isSmall: Boolean,
    modifier: Modifier = Modifier
) {
    // Importante: NO achicar otra vez adentro, porque eso reintroduce el "padding"/borde negro.
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(color = courtColor.copy(alpha = 0.26f))

        val line = Color.White.copy(alpha = 0.52f)
        val thick = if (isSmall) 3.5f else 4f
        val thin = if (isSmall) 1.8f else 2f

        // Borde
        drawLine(line, Offset(0f, 0f), Offset(w, 0f), strokeWidth = thick)
        drawLine(line, Offset(0f, h), Offset(w, h), strokeWidth = thick)
        drawLine(line, Offset(0f, 0f), Offset(0f, h), strokeWidth = thick)
        drawLine(line, Offset(w, 0f), Offset(w, h), strokeWidth = thick)

        // Red
        val netY = h / 2f
        drawLine(line, Offset(0f, netY), Offset(w, netY), strokeWidth = thick)

        // Lineas de saque: mas lejos de la red (visual). Ajuste a 7/10 del semialto.
        val half = h / 2f
        val serviceFromNet = half * (7f / 10f)
        val serviceTopY = netY - serviceFromNet
        val serviceBottomY = netY + serviceFromNet

        // Linea central (solo entre lineas de saque)
        drawLine(line, Offset(w / 2f, serviceTopY), Offset(w / 2f, serviceBottomY), strokeWidth = thin)

        drawLine(line, Offset(0f, serviceTopY), Offset(w, serviceTopY), strokeWidth = thin)
        drawLine(line, Offset(0f, serviceBottomY), Offset(w, serviceBottomY), strokeWidth = thin)
    }
}

internal fun isMatchStart(state: PadelState): Boolean =
    state.mySets == 0 && state.oppSets == 0 &&
    state.myGames == 0 && state.oppGames == 0 &&
    state.myPointsIdx == 0 && state.oppPointsIdx == 0 &&
    !state.inTieBreak

internal fun courtColorToColor(opt: CourtColorOption): Color = Color(opt.hex())

@Composable
private fun SettingsScreen(
    state: PadelState,
    onToggleKeepOn: (Boolean) -> Unit,
    onCourtColorChange: (CourtColorOption) -> Unit,
    onToggleStrokeCounting: (Boolean) -> Unit,
    onStrokeSensitivityChange: (StrokeSensitivity) -> Unit,
    onTestCounter: () -> Unit,
    onNewMatch: () -> Unit,
    onTutorial: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val haptic = LocalHapticFeedback.current
    var swipeDragAccum by remember { mutableStateOf(0f) }
    val swipeThresholdPx = 110f

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeDragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount -> swipeDragAccum += dragAmount },
                        onDragEnd = {
                            if (swipeDragAccum >= swipeThresholdPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBack()
                            }
                            swipeDragAccum = 0f
                        }
                    )
                },
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("Ajustes", fontWeight = FontWeight.Bold) }

            item {
                Button(onClick = onNewMatch, modifier = Modifier.fillMaxWidth()) { Text("Nuevo partido…") }
            }

            item { Text("Color de cancha") }

            item {
                val haptic = LocalHapticFeedback.current

                val choices = listOf(
                    Triple(CourtColorOption.GREEN, "Verde", courtColorToColor(CourtColorOption.GREEN)),
                    Triple(CourtColorOption.ORANGE, "Naranja", courtColorToColor(CourtColorOption.ORANGE)),
                    Triple(CourtColorOption.PURPLE, "Violeta", courtColorToColor(CourtColorOption.PURPLE)),
                    Triple(CourtColorOption.BLUE, "Azul", courtColorToColor(CourtColorOption.BLUE))
                )

                fun idxFor(opt: CourtColorOption): Int = when (opt) {
                    CourtColorOption.GREEN -> 0
                    CourtColorOption.ORANGE -> 1
                    CourtColorOption.PURPLE -> 2
                    CourtColorOption.BLUE -> 3
                }

                var dragAccum by remember { mutableStateOf(0f) }
                val swipeThresholdPx = 80f

                val currentIdx = idxFor(state.courtColor)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(state.courtColor) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccum = 0f },
                                onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                                onDragEnd = {
                                    if (abs(dragAccum) >= swipeThresholdPx) {
                                        val dir = if (dragAccum > 0f) -1 else 1
                                        val nextIdx = (currentIdx + dir + choices.size) % choices.size
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCourtColorChange(choices[nextIdx].first)
                                    }
                                    dragAccum = 0f
                                }
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val (_, label, dotColor) = choices[currentIdx]

                    AnimatedContent(
                        targetState = currentIdx,
                        transitionSpec = {
                            val direction = if (targetState > initialState) {
                                slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                            } else {
                                slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
                            }
                            direction togetherWith slideOutHorizontally(targetOffsetX = { if (targetState > initialState) -it else it }) + fadeOut()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { idx ->
                        val (_, lbl, clr) = choices[idx]
                        Chip(
                            onClick = {
                                val nextIdx = (currentIdx + 1) % choices.size
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCourtColorChange(choices[nextIdx].first)
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(clr)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(lbl)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        choices.indices.forEach { i ->
                            val alpha = if (i == currentIdx) 0.95f else 0.35f
                            Box(
                                modifier = Modifier
                                    .size(if (i == currentIdx) 6.dp else 5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = alpha))
                            )
                        }
                    }
                }
            }

            item { Text("Pantalla siempre encendida", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            item {
                ToggleChip(
                    checked = state.keepScreenOn,
                    onCheckedChange = onToggleKeepOn,
                    label = { Text(if (state.keepScreenOn) "Activado" else "Desactivado") },
                    toggleControl = { Switch(checked = state.keepScreenOn) }
                )
            }

            item { Text("Contador de golpes", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            item {
                ToggleChip(
                    checked = state.strokeCountingEnabled,
                    onCheckedChange = onToggleStrokeCounting,
                    label = { Text(if (state.strokeCountingEnabled) "Activado" else "Desactivado") },
                    toggleControl = { Switch(checked = state.strokeCountingEnabled) }
                )
            }

            if (state.strokeCountingEnabled) {
                item { Text("Sensibilidad del sensor", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                item {
                    SensitivitySelector(
                        current = state.strokeSensitivity,
                        onChange = onStrokeSensitivityChange
                    )
                }
                item {
                    OutlinedButton(onClick = onTestCounter, modifier = Modifier.fillMaxWidth()) { Text("Probar contador") }
                }
            }

            item {
                OutlinedButton(onClick = onTutorial, modifier = Modifier.fillMaxWidth()) { Text("Tutorial") }
            }
            item {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
            }
            item {
                Text(
                    text = "v1.0.0",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WalkthroughScreen(onFinish: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var step by remember { mutableStateOf(0) }

    data class WalkthroughStep(
        val title: String,
        val description: String,
        val showBall: Boolean = false
    )

    val steps = listOf(
        WalkthroughStep(
            title = "Simple Padel Score",
            description = "Tu marcador de pádel\nen la muñeca",
            showBall = true
        ),
        WalkthroughStep(
            title = "Elegí quién saca",
            description = "Tocá arriba si saca el rival\nTocá abajo si sacás vos"
        ),
        WalkthroughStep(
            title = "Anotá puntos",
            description = "Un toque = sumar punto\nDoble toque = restar punto"
        ),
        WalkthroughStep(
            title = "Deshacer",
            description = "Mantené presionado para\nvolver al estado anterior"
        ),
        WalkthroughStep(
            title = "Navegación",
            description = "Deslizá a la izquierda\npara abrir Ajustes"
        ),
        WalkthroughStep(
            title = "Contador de golpes",
            description = "Contamos tus golpes en el partido.\nUsá el reloj en la muñeca\nde la paleta"
        ),
        WalkthroughStep(
            title = "¡Listo!",
            description = "Ya podés empezar\na anotar tu partido"
        )
    )
    val totalSteps = steps.size

    val current = steps[step]

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(step) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (step < totalSteps - 1) step++ else onFinish()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                }
            ) { currentStep ->
                val s = steps[currentStep]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.weight(1f))

                    // Titulo
                    Text(
                        text = s.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // Pelota en el paso de bienvenida
                    if (s.showBall) {
                        Image(
                            painter = painterResource(id = R.drawable.padelball),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Descripcion
                    Text(
                        text = s.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.weight(1f))

                    // Indicador de progreso (dots)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalSteps) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == currentStep) 6.dp else 4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (i == currentStep) Color.White
                                        else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Texto de accion
                    Text(
                        text = if (currentStep < totalSteps - 1) "Tocá para continuar" else "Tocá para empezar",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TutorialScreen(onBack: () -> Unit, onWalkthrough: () -> Unit) {
    val listState = rememberScalingLazyListState()
    val haptic = LocalHapticFeedback.current
    var swipeDragAccum by remember { mutableStateOf(0f) }
    val swipeThresholdPx = 110f

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeDragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount -> swipeDragAccum += dragAmount },
                        onDragEnd = {
                            if (swipeDragAccum >= swipeThresholdPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBack()
                            }
                            swipeDragAccum = 0f
                        }
                    )
                },
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Text("Tutorial", fontWeight = FontWeight.Bold) }

            item { Text("1. Al iniciar, tocá arriba o abajo para elegir quién saca.", fontSize = 12.sp) }
            item { Text("2. Un toque suma un punto al lado tocado (arriba = rival, abajo = vos).", fontSize = 12.sp) }
            item { Text("3. Doble toque resta un punto de ese lado.", fontSize = 12.sp) }
            item { Text("4. Si ambos están en 0 puntos, el doble toque resta un game del lado que se toque.", fontSize = 12.sp) }
            item { Text("5. Mantené presionado para deshacer la última acción.", fontSize = 12.sp) }
            item { Text("6. La pelotita indica quién saca y de qué lado.", fontSize = 12.sp) }
            item { Text("7. Deslizá a la izquierda para Ajustes, a la derecha para volver.", fontSize = 12.sp) }

            item {
                Button(onClick = onWalkthrough, modifier = Modifier.fillMaxWidth()) { Text("Recorrido guiado") }
            }
            item {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
            }
        }
    }
}

@Composable
private fun NewMatchScreen(
    initialScoringMode: ScoringMode,
    initialDecider: Decider,
    initialBestOf: Int = 3,
    onConfirm: (decider: Decider, scoringMode: ScoringMode, bestOf: Int) -> Unit,
    onCancel: () -> Unit
) {
    var scoringMode by remember { mutableStateOf(initialScoringMode) }
    var decider by remember { mutableStateOf(initialDecider) }
    var bestOf by remember { mutableStateOf(initialBestOf) }

    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("Nuevo partido", fontWeight = FontWeight.Bold) }

            item { Text("Sets") }
            listOf(1, 3, 5).forEach { option ->
                item {
                    Chip(
                        onClick = { bestOf = option },
                        label = {
                            Text(when (option) {
                                1 -> "Al mejor de 1 set"
                                3 -> "Al mejor de 3 sets"
                                else -> "Al mejor de 5 sets"
                            })
                        },
                        secondaryLabel = { Text(if (bestOf == option) "Seleccionado" else "") }
                    )
                }
            }

            item { Text("Desempate del partido") }
            item {
                Chip(
                    onClick = { decider = Decider.TB7 },
                    label = { Text("Tie-break (a 7)") },
                    secondaryLabel = { Text(if (decider == Decider.TB7) "Seleccionado" else "") }
                )
            }
            item {
                Chip(
                    onClick = { decider = Decider.SUPER10 },
                    label = { Text("Super Tie-break (a 10)") },
                    secondaryLabel = { Text(if (decider == Decider.SUPER10) "Seleccionado" else "") }
                )
            }

            item { Text("Modo de juego") }
            item {
                Chip(
                    onClick = { scoringMode = ScoringMode.DEUCE },
                    label = { Text("Deuce/Ventaja") },
                    secondaryLabel = { Text(if (scoringMode == ScoringMode.DEUCE) "Seleccionado" else "") }
                )
            }
            item {
                Chip(
                    onClick = { scoringMode = ScoringMode.GOLDEN_POINT },
                    label = { Text("Punto de Oro") },
                    secondaryLabel = { Text(if (scoringMode == ScoringMode.GOLDEN_POINT) "Seleccionado" else "") }
                )
            }
            item {
                Chip(
                    onClick = { scoringMode = ScoringMode.STAR_POINT },
                    label = { Text("Star Point") },
                    secondaryLabel = { Text(if (scoringMode == ScoringMode.STAR_POINT) "Seleccionado" else "") }
                )
            }

            item {
                Button(onClick = { onConfirm(decider, scoringMode, bestOf) }, modifier = Modifier.fillMaxWidth()) { Text("Arrancar") }
            }
            item {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun MatchFinishedScreen(
    state: PadelState,
    onNewMatch: () -> Unit,
    onDismiss: () -> Unit
) {
    val winnerText = if (state.mySets > state.oppSets) "Ganaste!" else "Perdiste"

    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = winnerText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Sets: ${state.mySets} - ${state.oppSets}",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            if (state.setsHistory.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = state.setsHistory.joinToString("  ") { "${it[0]}-${it[1]}" },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNewMatch, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Nuevo partido")
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Seguir viendo")
            }
        }
    }
}

@Composable
private fun SensitivitySelector(
    current: StrokeSensitivity,
    onChange: (StrokeSensitivity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val choices = listOf(
        StrokeSensitivity.HIGH to "Alto",
        StrokeSensitivity.MEDIUM to "Medio",
        StrokeSensitivity.LOW to "Bajo"
    )
    val currentIdx = choices.indexOfFirst { it.first == current }.coerceAtLeast(0)

    var dragAccum by remember { mutableStateOf(0f) }
    val swipeThresholdPx = 80f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(current) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (abs(dragAccum) >= swipeThresholdPx) {
                            val dir = if (dragAccum > 0f) -1 else 1
                            val nextIdx = (currentIdx + dir + choices.size) % choices.size
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onChange(choices[nextIdx].first)
                        }
                        dragAccum = 0f
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = currentIdx,
            transitionSpec = {
                val direction = if (targetState > initialState) {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                } else {
                    slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
                }
                direction togetherWith slideOutHorizontally(targetOffsetX = { if (targetState > initialState) -it else it }) + fadeOut()
            },
            modifier = Modifier.fillMaxWidth()
        ) { idx ->
            val (_, lbl) = choices[idx]
            Chip(
                onClick = {
                    val nextIdx = (currentIdx + 1) % choices.size
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onChange(choices[nextIdx].first)
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(lbl)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            choices.indices.forEach { i ->
                val alpha = if (i == currentIdx) 0.95f else 0.35f
                Box(
                    modifier = Modifier
                        .size(if (i == currentIdx) 6.dp else 5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
private fun StrokeTestScreen(
    state: PadelState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val metrics = rememberScreenMetrics()
    var count by remember { mutableStateOf(0) }

    // Acelerómetro en tiempo real (sin batching) con la sensibilidad actual.
    // Se re-registra si cambia la sensibilidad (recrea el detector con el nuevo umbral).
    DisposableEffect(state.strokeSensitivity) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val detector = StrokeDetector(state.strokeSensitivity.thresholdMs2())
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = e.values[0]
                val y = e.values[1]
                val z = e.values[2]
                val m = sqrt(x * x + y * y + z * z)
                if (detector.onSample(m, e.timestamp / 1_000_000L)) count++
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) { /* no-op */ }
        }
        if (accel != null) sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    Scaffold(timeText = { TimeText() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(metrics.courtPadding)
                .pointerInput(Unit) {
                    var drag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onHorizontalDrag = { _, amount -> drag += amount },
                        onDragEnd = {
                            if (drag >= 110f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBack()
                            }
                            drag = 0f
                        }
                    )
                }
        ) {
            // Cancha centrada con las mismas fracciones/clip que el partido (CounterScreen).
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(metrics.courtWidthFraction)
                    .fillMaxHeight(metrics.courtHeightFraction)
                    .clip(RoundedCornerShape(metrics.courtRadius))
            ) {
                CourtBackgroundVertical(
                    courtColor = courtColorToColor(state.courtColor),
                    isSmall = metrics.isSmall,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = count.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = metrics.bigScore,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (metrics.isSmall) 8.dp else 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        count = 0
                    },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) { Text("↺", fontSize = 18.sp) }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBack()
                    },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) { Text("✕", fontSize = 18.sp) }
            }
        }
    }
}
