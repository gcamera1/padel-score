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
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.gonzalocamera.padelcounter.BuildConfig
import com.gonzalocamera.padelcounter.R
import com.gonzalocamera.padelcounter.presentation.theme.PadelCounterTheme
import com.gonzalocamera.padelcounter.presentation.theme.WearBrand
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
import com.gonzalocamera.padelcounter.shared.isGoldenPointDecider
import com.gonzalocamera.padelcounter.shared.starPointAdvantageLevel
import com.gonzalocamera.padelcounter.shared.isMatchFinished
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.matchId
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

/**
 * Padding lateral seguro para pantallas de texto en un reloj redondo.
 *
 * En pantalla redonda el ancho disponible depende de la altura: a una distancia `y` del
 * centro el semi-ancho es √(R²−y²). Un padding chico y fijo (8-10dp) alcanza con la fuente
 * por defecto, pero con la fuente del sistema en Largest el texto crece, se acerca al borde
 * curvo y se corta — Play rechazó la v1.0.0 por esto (WO-V1).
 *
 * El 12% del ancho deja margen suficiente y, combinado con `ScalingLazyColumn`, cubre
 * también las líneas que scrollean hacia los extremos.
 */
@Composable
internal fun roundSafeSidePadding(): Dp {
    val config = LocalConfiguration.current
    return if (config.isScreenRound) (config.screenWidthDp * 0.12f).dp else 12.dp
}

/**
 * `contentPadding` para las listas de texto del reloj.
 *
 * El `top` es mayor que el `bottom` a propósito: en la posición inicial del scroll, el
 * primer item quedaría por debajo del `TimeText` del Scaffold y recortado contra el borde
 * superior. Una vez que el usuario scrollea, `Modifier.scrollAway` esconde el reloj.
 */
@Composable
internal fun roundSafeContentPadding(sideFraction: Float = 0.12f): PaddingValues {
    val config = LocalConfiguration.current
    val side = if (config.isScreenRound) (config.screenWidthDp * sideFraction).dp else 12.dp
    return PaddingValues(start = side, end = side, top = 40.dp, bottom = 30.dp)
}

/**
 * Tamaño de la pelota del paso de bienvenida del walkthrough.
 *
 * Se reduce a medida que crece la fuente del sistema: a 48dp fijos, con la fuente en
 * Largest el contenido empujaba el "Tocá para continuar" fuera de la vista inicial en los
 * relojes chicos. Play ya rechazó una versión por texto que no entraba, así que ante la
 * duda gana el texto.
 */
@Composable
internal fun walkthroughBallSize(): Dp {
    val fontScale = LocalConfiguration.current.fontScale
    return when {
        fontScale >= 1.25f -> 0.dp     // se oculta: el texto tiene prioridad
        fontScale >= 1.1f -> 34.dp
        else -> 48.dp
    }
}

/**
 * Botón de ancho completo con una etiqueta de texto.
 *
 * Implementado con `Chip` y NO con `Button`: en Wear Material, `Button` es un botón **circular
 * para iconos** — aplica `size(52.dp)` fijo y no reserva padding interno. Usado a lo ancho con
 * texto, con la fuente del sistema en grande la etiqueta envuelve, se pega al borde redondeado
 * de la píldora y pierde la primera y la última letra. Play rechazó por esto dos veces (WO-V1):
 * se vio en "Recorrido guiado" (Tutorial) y en "Instalar en el teléfono" (fin de partido).
 *
 * `Chip` sí está pensado para texto: `defaultMinSize(52.dp).height(IntrinsicSize.Min)` lo hace
 * crecer en alto cuando la etiqueta necesita más líneas, y `ChipDefaults.ContentPadding` deja
 * 14dp a cada lado para que el texto nunca toque el borde.
 *
 * No usar `Button`/`OutlinedButton` con texto: son para iconos.
 */
@Composable
internal fun WideTextButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = true,
    modifier: Modifier = Modifier
) {
    val label: @Composable RowScope.() -> Unit = {
        Text(text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    if (primary) {
        Chip(
            label = label,
            onClick = onClick,
            colors = ChipDefaults.primaryChipColors(),
            modifier = modifier.fillMaxWidth()
        )
    } else {
        OutlinedChip(
            label = label,
            onClick = onClick,
            modifier = modifier.fillMaxWidth()
        )
    }
}

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
        setContent { PadelCounterTheme { PadelApp() } }
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
    val hasSeenWalkthrough by repo.hasSeenWalkthrough.collectAsState(initial = true)

    LaunchedEffect(state.keepScreenOn) {
        val act = context as? Activity ?: return@LaunchedEffect
        if (state.keepScreenOn) act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    var screen by remember { mutableStateOf(Screen.COUNTER) }
    var previousState by remember { mutableStateOf<PadelState?>(null) }
    var matchSynced by remember { mutableStateOf(false) }

    // Estado del vínculo con la app de teléfono (companion) + avisos de instalación.
    var companionStatus by remember { mutableStateOf(CompanionStatus.UNKNOWN) }
    var showCompanionPrompt by remember { mutableStateOf(false) }
    var showMatchEndCompanionHint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        syncSender.trySendPending()
    }

    // Aviso al arrancar (máx 3 veces): si hay teléfono sin la app, o no hay teléfono.
    LaunchedEffect(hasSeenWalkthrough) {
        if (!hasSeenWalkthrough) return@LaunchedEffect
        val status = CompanionDetector.detect(context)
        companionStatus = status
        val shows = status == CompanionStatus.PHONE_NO_APP || status == CompanionStatus.NO_PHONE
        if (shows && repo.startupCompanionPromptCount.first() < 3) {
            repo.incrementStartupCompanionPromptCount()
            showCompanionPrompt = true
        }
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
            // Este efecto se vuelve a disparar en cada arranque en frío mientras haya un
            // partido terminado sin resetear. Los timestamps quedan persistidos para que la
            // duración no crezca en cada reapertura, y `firstTime` corta el reenvío: el
            // partido se encola una sola vez. Si el teléfono no estaba, la cola lo conserva
            // y el `trySendPending()` del arranque lo despacha cuando vuelva.
            val timestamps = repo.markMatchFinished()
            if (timestamps.firstTime) {
                val match = Match(
                    id = matchId(timestamps.startedAt, state.setsHistory),
                    startedAt = timestamps.startedAt,
                    finishedAt = timestamps.finishedAt,
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
            }
            StrokeCounter.reset()
            repo.clearStrokeBackup()
            // Aviso companion al finalizar (máx 3): si no está la app de teléfono.
            val status = CompanionDetector.detect(context)
            companionStatus = status
            val hint = status == CompanionStatus.PHONE_NO_APP || status == CompanionStatus.NO_PHONE
            showMatchEndCompanionHint = if (hint && repo.matchEndCompanionPromptCount.first() < 3) {
                repo.incrementMatchEndCompanionPromptCount()
                true
            } else false
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
                onInstallPhoneApp = { scope.launch { CompanionDetector.openInstallOnPhone(context) } },
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
                showCompanionHint = showMatchEndCompanionHint,
                onInstallPhoneApp = { scope.launch { CompanionDetector.openInstallOnPhone(context) } },
                onPlayAgain = {
                    matchSynced = false
                    showMatchEndCompanionHint = false
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
                onNewMatch = {
                    matchSynced = false
                    showMatchEndCompanionHint = false
                    screen = Screen.NEW_MATCH
                }
            )
        }

        // Overlay de aviso "instalá la app de teléfono" al arrancar (solo sobre COUNTER).
        AnimatedVisibility(
            visible = showCompanionPrompt && screen == Screen.COUNTER,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CompanionPromptScreen(
                status = companionStatus,
                onInstall = {
                    scope.launch { CompanionDetector.openInstallOnPhone(context) }
                    showCompanionPrompt = false
                },
                onDismiss = { showCompanionPrompt = false }
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

                // Indicador central del estado del game:
                // Star Point → "DE1"/"DE2" en cada ventaja y "SP" en el punto definitorio;
                // Punto de Oro → "PO" en el 40-40.
                val deciderBadge = when {
                    isStarPointDecider(state) -> "SP"
                    isGoldenPointDecider(state) -> "PO"
                    else -> starPointAdvantageLevel(state)?.let { "DE$it" }
                }
                if (!needsServeSelection && deciderBadge != null) {
                    Text(
                        text = deciderBadge,
                        color = WearBrand.OnGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = metrics.smallSize * 0.72f,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(999.dp))
                            .background(WearBrand.Gold)
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
            .background(WearBrand.Gold.copy(alpha = 0.92f))
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
                        .background(WearBrand.OnGold.copy(alpha = 0.85f))
                )
            }
        }

        // Chevron hacia la izquierda (swipe hacia la izquierda para abrir)
        Text(
            text = "‹",
            color = WearBrand.OnGold.copy(alpha = 0.9f),
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
    onInstallPhoneApp: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val haptic = LocalHapticFeedback.current
    var swipeDragAccum by remember { mutableStateOf(0f) }
    val swipeThresholdPx = 110f

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
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
            item { Text("Ajustes", fontWeight = FontWeight.Bold, color = WearBrand.Gold) }

            item { WideTextButton("Nuevo partido…", onNewMatch) }

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
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        choices.indices.forEach { i ->
                            val active = i == currentIdx
                            Box(
                                modifier = Modifier
                                    .size(if (active) 6.dp else 5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (active) WearBrand.Gold else WearBrand.TextFaint.copy(alpha = 0.5f))
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
                item { WideTextButton("Probar contador", onTestCounter, primary = false) }
            }

            item { WideTextButton("Tutorial", onTutorial, primary = false) }
            item { WideTextButton("Instalar app en el teléfono", onInstallPhoneApp, primary = false) }
            item { WideTextButton("Volver", onBack, primary = false) }
            item {
                // Del BuildConfig, no hardcodeado: estaba fijo en "v1.0.0" y con el bump
                // a 1.1.0 la pantalla de Ajustes mostraba una versión equivocada.
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    fontSize = 10.sp,
                    color = WearBrand.Gold.copy(alpha = 0.5f),
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

    // Sin saltos de línea hardcodeados: con el tamaño de fuente grande del sistema,
    // un `\n` fuerza líneas que ya no caben y el texto se corta contra el borde
    // curvo (WO-V1). Se deja que el wrap lo resuelva el ancho disponible.
    val steps = listOf(
        WalkthroughStep(
            title = "Simple Padel Score",
            description = "Tu marcador de pádel en la muñeca",
            showBall = true
        ),
        WalkthroughStep(
            title = "Elegí quién saca",
            description = "Tocá arriba si saca el rival. Tocá abajo si sacás vos"
        ),
        WalkthroughStep(
            title = "Anotá puntos",
            description = "Un toque suma punto. Doble toque resta punto"
        ),
        WalkthroughStep(
            title = "Deshacer",
            description = "Mantené presionado para volver al estado anterior"
        ),
        WalkthroughStep(
            title = "Navegación",
            description = "Deslizá a la izquierda para abrir Ajustes"
        ),
        // Descripciones cortas a propósito: en un reloj de 192dp (el mínimo que pide
        // WO-V16) con la fuente del sistema en Largest, un texto de 4 líneas empuja el
        // "Tocá para continuar" fuera de la vista inicial.
        WalkthroughStep(
            title = "Contador de golpes",
            description = "Usá el reloj en la muñeca de la paleta"
        ),
        WalkthroughStep(
            title = "También en tu teléfono",
            description = "Instalá la app en el celu para ver tu historial"
        ),
        WalkthroughStep(
            title = "¡Listo!",
            description = "Ya podés empezar a anotar tu partido"
        )
    )
    val totalSteps = steps.size

    val current = steps[step]

    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    // Cada paso arranca desde arriba: si el anterior quedó scrolleado (fuente grande),
    // el nuevo no debe heredar ese offset.
    LaunchedEffect(step) { listState.scrollToItem(0) }


    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
        // WO-V8: toda vista desplazable tiene que mostrar su barra de desplazamiento.
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
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
            // Con la fuente del sistema en Largest el contenido no entra en la pantalla.
            // `ScalingLazyColumn` lo hace desplazable y escala los items contra el borde
            // curvo en vez de recortarlos — el motivo del rechazo de Play (WO-V1).
            //
            // La lista vive FUERA del `AnimatedContent` a propósito: durante la transición
            // habría dos listas montadas a la vez compartiendo el mismo `listState`, que
            // es un estado de una sola lista. Se anima el bloque de texto, no la lista.
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = roundSafeContentPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                item {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                                (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                        }
                    ) { currentStep ->
                        val s = steps[currentStep]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = s.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = WearBrand.Gold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // La pelota se achica —y con la fuente en Largest se oculta—
                            // porque sus 48dp empujaban el "Tocá para continuar" fuera de la
                            // vista inicial. El texto tiene prioridad sobre el adorno (WO-V1).
                            val ballSize = walkthroughBallSize()
                            if (s.showBall && ballSize > 0.dp) {
                                Image(
                                    painter = painterResource(id = R.drawable.padelball),
                                    contentDescription = null,
                                    modifier = Modifier.size(ballSize)
                                )
                            }

                            Text(
                                text = s.description,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Indicador de progreso (dots)
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalSteps) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == step) 6.dp else 4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (i == step) WearBrand.Gold
                                        else WearBrand.TextFaint.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }

                // Texto de accion
                item {
                    Text(
                        text = if (step < totalSteps - 1) "Tocá para continuar" else "Tocá para empezar",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Pasos del tutorial, deliberadamente cortos.
 *
 * Cada paso es un item de `ScalingLazyColumn`: mientras entre en 3 líneas con la fuente del
 * sistema en grande, la lista puede escalarlo contra el borde curvo en vez de recortarlo. Los
 * pasos 2, 4 y 7 se acortaron por eso — antes ocupaban 5 y 6 líneas.
 */
private val TUTORIAL_STEPS = listOf(
    "1. Al iniciar, tocá arriba o abajo para elegir quién saca.",
    "2. Un toque suma un punto: arriba el rival, abajo vos.",
    "3. Doble toque resta un punto de ese lado.",
    "4. Con ambos en 0 puntos, el doble toque resta un game.",
    "5. Mantené presionado para deshacer.",
    "6. La pelotita indica quién saca y de qué lado.",
    "7. Deslizá: a la izquierda abrís Ajustes, a la derecha volvés."
)

@Composable
private fun TutorialScreen(onBack: () -> Unit, onWalkthrough: () -> Unit) {
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val haptic = LocalHapticFeedback.current
    var swipeDragAccum by remember { mutableStateOf(0f) }
    val swipeThresholdPx = 110f

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = roundSafeContentPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Text("Tutorial", fontWeight = FontWeight.Bold, color = WearBrand.Gold) }

            // Texto CENTRADO, no alineado a la izquierda. Con la fuente del sistema en grande
            // cada paso ocupa 4-6 líneas: es un item de `ScalingLazyColumn` más alto que la
            // pantalla, así que se dibuja a escala 1.0 y sus líneas de arriba y abajo caen en
            // la curva del bisel. Alineadas a la izquierda todas arrancan en el mismo x y ahí
            // perdían la primera letra ("saca." se leía "aca.", "rival" se leía "ival").
            // Centradas, cada línea se angosta hacia el centro justo donde el círculo se
            // angosta. Es también lo que recomienda la guía de diseño de Wear OS.
            TUTORIAL_STEPS.forEach { step ->
                item {
                    Text(
                        text = step,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { WideTextButton("Recorrido guiado", onWalkthrough) }
            item { WideTextButton("Volver", onBack, primary = false) }
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

    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("Nuevo partido", fontWeight = FontWeight.Bold, color = WearBrand.Gold) }

            item { Text("Modo de juego") }
            item {
                Chip(
                    onClick = { scoringMode = ScoringMode.DEUCE },
                    label = { Text("Deuce/Ventaja", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    secondaryLabel = { Text(if (scoringMode == ScoringMode.DEUCE) "Seleccionado" else "", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    colors = if (scoringMode == ScoringMode.DEUCE) ChipDefaults.primaryChipColors()
                             else ChipDefaults.secondaryChipColors()
                )
            }
            item {
                Chip(
                    onClick = { scoringMode = ScoringMode.GOLDEN_POINT },
                    label = { Text("Punto de Oro", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    secondaryLabel = { Text(if (scoringMode == ScoringMode.GOLDEN_POINT) "Seleccionado" else "", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    colors = if (scoringMode == ScoringMode.GOLDEN_POINT) ChipDefaults.primaryChipColors()
                             else ChipDefaults.secondaryChipColors()
                )
            }
            item {
                Chip(
                    onClick = { scoringMode = ScoringMode.STAR_POINT },
                    label = { Text("Star Point", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    secondaryLabel = { Text(if (scoringMode == ScoringMode.STAR_POINT) "Seleccionado" else "", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    colors = if (scoringMode == ScoringMode.STAR_POINT) ChipDefaults.primaryChipColors()
                             else ChipDefaults.secondaryChipColors()
                )
            }

            item { Text("Sets (al mejor de)") }
            item {
                val haptic = LocalHapticFeedback.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 3, 5).forEach { option ->
                        val selected = bestOf == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) WearBrand.Gold else WearBrand.Card)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    bestOf = option
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.toString(),
                                color = if (selected) WearBrand.OnGold else WearBrand.Text,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item { Text("Tie break (a)") }
            item {
                val haptic = LocalHapticFeedback.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(Decider.TB7 to "7", Decider.SUPER10 to "10").forEach { (option, label) ->
                        val selected = decider == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) WearBrand.Gold else WearBrand.Card)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    decider = option
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) WearBrand.OnGold else WearBrand.Text,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item { WideTextButton("Arrancar", { onConfirm(decider, scoringMode, bestOf) }) }
            item { WideTextButton("Cancelar", onCancel, primary = false) }
        }
    }
}

@Composable
private fun MatchFinishedScreen(
    state: PadelState,
    showCompanionHint: Boolean,
    onInstallPhoneApp: () -> Unit,
    onPlayAgain: () -> Unit,
    onNewMatch: () -> Unit
) {
    val won = state.mySets > state.oppSets
    val winnerText = if (won) "Ganaste!" else "Perdiste"
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
        // WO-V8: con `positionIndicator = { }` la lista no mostraba barra de
        // desplazamiento, y Play rechazó la v1.0.0 justamente por eso.
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = roundSafeContentPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            item {
                Text(
                    text = winnerText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (won) WearBrand.GoldLight else WearBrand.Text
                )
            }
            item {
                Text(
                    text = "Sets: ${state.mySets} - ${state.oppSets}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            if (state.setsHistory.isNotEmpty()) {
                item {
                    Text(
                        text = state.setsHistory.joinToString("  ") { "${it[0]}-${it[1]}" },
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            if (showCompanionHint) {
                item {
                    // Texto corto a propósito: el original tenía 75 caracteres y con la fuente
                    // del sistema en grande ocupaba 5 líneas, así que las de abajo caían en la
                    // curva del bisel y perdían la primera letra (WO-V1).
                    Text(
                        text = "Guardá el historial en tu teléfono",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                item { WideTextButton("Instalar en el teléfono", onInstallPhoneApp, primary = false) }
            }
            item {
                Chip(
                    onClick = onPlayAgain,
                    label = { Text("Jugar de nuevo", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    secondaryLabel = { Text("Misma configuración", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = onNewMatch,
                    label = { Text("Nuevo partido", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    secondaryLabel = { Text("Cambiar opciones", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CompanionPromptScreen(
    status: CompanionStatus,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasPhone = status == CompanionStatus.PHONE_NO_APP
    // Un solo texto, y corto, haciendo de título: con la fuente del sistema en grande el
    // presupuesto vertical de un reloj de 192dp son ~122dp, y un título de dos líneas más un
    // mensaje de dos líneas ya no dejaba lugar para el botón — quedaba cortado al ras del
    // borde inferior en la vista inicial.
    val message = if (hasPhone) {
        "Instalá la app en tu teléfono"
    } else {
        "Vinculá un teléfono con la app"
    }

    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
        // WO-V8: ésta es la pantalla que Google capturó como evidencia de "falta la
        // barra de desplazamiento" — tenía `positionIndicator = { }`.
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = roundSafeContentPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
            item {
                Text(
                    text = message,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = WearBrand.Gold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }
            if (hasPhone) {
                item { WideTextButton("Instalar en el teléfono", onInstall) }
            }
            item { WideTextButton(if (hasPhone) "Ahora no" else "Entendido", onDismiss, primary = false) }
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
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            choices.indices.forEach { i ->
                val active = i == currentIdx
                Box(
                    modifier = Modifier
                        .size(if (active) 6.dp else 5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (active) WearBrand.Gold else WearBrand.TextFaint.copy(alpha = 0.5f))
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
