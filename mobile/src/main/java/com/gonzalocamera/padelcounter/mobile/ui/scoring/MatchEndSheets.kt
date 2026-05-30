package com.gonzalocamera.padelcounter.mobile.ui.scoring

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.PadelState

/**
 * Full-screen end-of-match sheet, shared by win and loss outcomes. The only difference
 * between them is the headline text and the accent/glow color.
 */
@Composable
fun MatchEndSheet(
    state: PadelState,
    won: Boolean,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
) {
    val accent = if (won) PadelTheme.colors.accentMine else PadelTheme.colors.accentRival
    val glow = if (won) PadelTheme.colors.goldenGlow else PadelTheme.colors.accentRival

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        var shimmerProgress by remember { mutableStateOf(0f) }
        val animated by animateFloatAsState(
            targetValue = shimmerProgress,
            animationSpec = tween(
                durationMillis = PadelTheme.motion.durationCelebration,
                easing = LinearEasing,
            ),
            label = "shimmer",
        )
        LaunchedEffect(Unit) { shimmerProgress = 1f }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glow.copy(alpha = 0.18f * (1f - animated * 0.5f)),
                                Color.Transparent,
                            ),
                            radius = 900f,
                        ),
                    ),
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (won) "VICTORIA" else "DERROTA",
                    style = MaterialTheme.typography.displayLarge,
                    color = accent,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.setsHistory.ifEmpty { listOf(listOf(state.mySets, state.oppSets)) }
                        .joinToString("  ") { "${it[0]}-${it[1]}" },
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar partido") }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
                    Text("Descartar")
                }
            }
        }
    }
}
