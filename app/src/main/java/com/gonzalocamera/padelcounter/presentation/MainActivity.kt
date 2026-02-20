package com.gonzalocamera.padelcounter.presentation

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.background

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
private fun CounterScreen(
    state: PadelState,
    onSave: (PadelState) -> Unit,
    onOpenSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val config = LocalConfiguration.current
    // Evita capturar un estado viejo en los handlers de tap (en Wear puede recomposear más lento)
    val latestState by rememberUpdatedState(state)

    // Responsive: priorizar 40mm
    val minDp = minOf(config.screenWidthDp, config.screenHeightDp)
    val isSmall = minDp <= 200 // priorizar 40mm

    // Cero padding en 40mm para evitar “borde negro” y que la cancha ocupe el máximo
    val padding = if (isSmall) 0.dp else 1.dp
    val bigScore = if (isSmall) 38.sp else 54.sp
    val smallSize = if (isSmall) 12.sp else 15.sp

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
                .padding(padding)
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
            // Caja de cancha: que ocupe casi toda el área útil (prioridad 40mm)
            // (el “borde negro” venía de achicar demasiado la caja y además volver a achicar adentro)
            val courtW = if (isSmall) 0.88f else 0.60f
            val courtH = if (isSmall) 0.82f else 0.78f
            val courtRadius = if (isSmall) 14.dp else 18.dp

            val courtModifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(courtW)
                .fillMaxHeight(courtH)
                .clip(RoundedCornerShape(courtRadius))

            Box(modifier = courtModifier) {
                // Fondo cancha
                CourtBackgroundVertical(
                    courtColor = courtColorToColor(state.courtColor),
                    isSmall = isSmall,
                    modifier = Modifier.fillMaxSize()
                )

                // Sets a la izquierda, pero DENTRO de la cancha.
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
                        fontSize = smallSize,
                        maxLines = 1
                    )
                    Text(
                        text = "-",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = smallSize,
                        maxLines = 1
                    )
                    Text(
                        text = state.mySets.toString(),
                        color = myGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = smallSize,
                        maxLines = 1
                    )
                }

                // Contenido principal (puntos + games)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rival (arriba)
                    TapZone(
                        modifier = Modifier.weight(1f),
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSave(addPointToOpp(latestState))
                        },
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(subtractPointFromOpp(latestState))
                        },
                        highlightColor = oppRed.copy(alpha = 0.18f)
                    ) {
                        ScoreLine(
                            games = state.oppGames,
                            pointsText = pointsLabel(state, isMe = false),
                            pointsColor = oppRed,
                            bigScore = bigScore,
                            smallSize = smallSize,
                            // Subimos un poco el score de Rival para que quede centrado en su mitad sin el título
                            pointsYOffset = if (isSmall) (-4).dp else (-6).dp
                        )
                    }

                    // Vos (abajo)
                    TapZone(
                        modifier = Modifier.weight(1f),
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSave(addPointToMy(latestState))
                        },
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(subtractPointFromMy(latestState))
                        },
                        highlightColor = myGreen.copy(alpha = 0.18f)
                    ) {
                        ScoreLine(
                            games = state.myGames,
                            pointsText = pointsLabel(state, isMe = true),
                            pointsColor = myGreen,
                            bigScore = bigScore,
                            smallSize = smallSize
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ScoreLine(
    games: Int,
    pointsText: String,
    pointsColor: Color,
    bigScore: androidx.compose.ui.unit.TextUnit,
    smallSize: androidx.compose.ui.unit.TextUnit,
    pointsYOffset: Dp = 0.dp
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
                .padding(end = 2.dp),
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
 * Cancha vertical “más real”:
 * - Net al centro.
 * - Línea de saque a 3m del net (sobre 10m de media cancha) => 3/10 del semialto.
 * Importante: NO achicar otra vez adentro, porque eso reintroduce el “padding”/borde negro.
 */
@Composable
private fun CourtBackgroundVertical(
    courtColor: Color,
    isSmall: Boolean,
    modifier: Modifier = Modifier
) {
    // Importante: NO achicar otra vez adentro, porque eso reintroduce el “padding”/borde negro.
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

private fun courtColorToColor(opt: CourtColorOption): Color = when (opt) {
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

            item { Text("Pantalla siempre encendida") }
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