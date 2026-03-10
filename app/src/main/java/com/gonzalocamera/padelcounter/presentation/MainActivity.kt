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

private enum class Screen { COUNTER, SETTINGS, NEW_MATCH }

@Composable
private fun PadelApp() {
    val context = LocalContext.current
    val repo = remember { PadelRepository(context) }
    val scope = rememberCoroutineScope()
    val state by repo.stateFlow.collectAsState(initial = PadelState())

    LaunchedEffect(state.keepScreenOn) {
        val act = context as? Activity ?: return@LaunchedEffect
        if (state.keepScreenOn) act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    var screen by remember { mutableStateOf(Screen.COUNTER) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = screen == Screen.COUNTER,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CounterScreen(
                state = state,
                onSave = { scope.launch { repo.save(it) } },
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
                onNewMatch = { screen = Screen.NEW_MATCH },
                onBack = { screen = Screen.COUNTER }
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.NEW_MATCH,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            NewMatchScreen(
                initialGolden = state.goldenPoint,
                initialDecider = state.decider,
                onConfirm = { decider, golden ->
                    scope.launch {
                        repo.resetMatchWithConfig(
                            decider = decider,
                            goldenPoint = golden,
                            courtColor = state.courtColor
                        )
                    }
                    screen = Screen.COUNTER
                },
                onCancel = { screen = Screen.SETTINGS }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CounterScreen(
    state: PadelState,
    onSave: (PadelState) -> Unit,
    onOpenSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val metrics = rememberScreenMetrics()
    // Evita capturar un estado viejo en los handlers de tap (en Wear puede recomposear más lento)
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

                // Sets a la izquierda, DENTRO de la cancha (oculto durante selección de saque)
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

                // Pelota central durante selección de quién saca
                if (needsServeSelection) {
                    Image(
                        painter = painterResource(id = R.drawable.padelball),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(with(LocalDensity.current) { metrics.bigScore.toDp() })
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
    // Pequeño indicador: puntitos + chevron. Fondo blanco para visibilidad sobre pantallas oscuras.
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
                            // Mantiene el brillo mientras el dedo está apoyado
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Brillo/flash de feedback táctil
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
 * Cancha vertical "más real":
 * - Net al centro.
 * - Línea de saque a 3m del net (sobre 10m de media cancha) => 3/10 del semialto.
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

        // Líneas de saque: más lejos de la red (visual). Ajuste a 7/10 del semialto.
        val half = h / 2f
        val serviceFromNet = half * (7f / 10f)
        val serviceTopY = netY - serviceFromNet
        val serviceBottomY = netY + serviceFromNet

        // Línea central (solo entre líneas de saque)
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

internal fun courtColorToColor(opt: CourtColorOption): Color = when (opt) {
    CourtColorOption.BLUE -> Color(0xFF1976D2)
    CourtColorOption.ORANGE -> Color(0xFFF55600)
    CourtColorOption.GREEN -> Color(0xFF2E7D32)
    CourtColorOption.PURPLE -> Color(0xFF6A1B9A)
}

@Composable
private fun SettingsScreen(
    state: PadelState,
    onToggleKeepOn: (Boolean) -> Unit,
    onCourtColorChange: (CourtColorOption) -> Unit,
    onNewMatch: () -> Unit,
    onBack: () -> Unit
) {
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
            item { Text("Ajustes", fontWeight = FontWeight.Bold) }

            item { Text("Pantalla siempre encendida", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            item {
                ToggleChip(
                    checked = state.keepScreenOn,
                    onCheckedChange = onToggleKeepOn,
                    label = { Text(if (state.keepScreenOn) "Activado" else "Desactivado") },
                    toggleControl = { Switch(checked = state.keepScreenOn) }
                )
            }


            item { Text("Color de cancha") }

            item {
                val haptic = LocalHapticFeedback.current

                val choices = listOf(
                    Triple(CourtColorOption.GREEN, "Verde", Color(0xFF2E7D32)),
                    Triple(CourtColorOption.ORANGE, "Naranja", Color(0xFFF55600)),
                    Triple(CourtColorOption.PURPLE, "Violeta", Color(0xFF6A1B9A)),
                    Triple(CourtColorOption.BLUE, "Azul", Color(0xFF1976D2))
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
            item {
                Button(onClick = onNewMatch, modifier = Modifier.fillMaxWidth()) { Text("Nuevo partido…") }
            }
            item {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
            }
        }
    }
}

@Composable
private fun NewMatchScreen(
    initialGolden: Boolean,
    initialDecider: Decider,
    onConfirm: (decider: Decider, golden: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var golden by remember { mutableStateOf(initialGolden) }
    var decider by remember { mutableStateOf(initialDecider) }

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

            item { Text("Punto de oro") }
            item {
                ToggleChip(
                    checked = golden,
                    onCheckedChange = { golden = it },
                    label = { Text(if (golden) "Activado" else "Desactivado") },
                    toggleControl = { Switch(checked = golden) }
                )
            }

            item {
                Button(onClick = { onConfirm(decider, golden) }, modifier = Modifier.fillMaxWidth()) { Text("Arrancar") }
            }
            item {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
        }
    }
}