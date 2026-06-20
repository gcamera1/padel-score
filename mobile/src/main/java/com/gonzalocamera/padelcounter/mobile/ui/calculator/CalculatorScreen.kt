package com.gonzalocamera.padelcounter.mobile.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.components.PadelTopAppBar
import com.gonzalocamera.padelcounter.mobile.ui.components.SectionHeader
import com.gonzalocamera.padelcounter.mobile.ui.components.StrokeVerdictBadge
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import com.gonzalocamera.padelcounter.shared.PadelCategory
import com.gonzalocamera.padelcounter.shared.PointIntensity
import com.gonzalocamera.padelcounter.shared.StrokeVerdict
import com.gonzalocamera.padelcounter.shared.estimateStrokes
import com.gonzalocamera.padelcounter.shared.verdict
import kotlin.math.roundToInt

private fun intensityLabel(i: PointIntensity): String = when (i) {
    PointIntensity.LOW -> "Baja"
    PointIntensity.MEDIUM -> "Media"
    PointIntensity.HIGH -> "Alta"
}

private fun intensityHint(i: PointIntensity): String = when (i) {
    PointIntensity.LOW -> "Games rápidos, se cerraban en pocos puntos. Casi sin ventajas."
    PointIntensity.MEDIUM -> "Ritmo de club: peloteo fluido, algún 40-40 de vez en cuando."
    PointIntensity.HIGH -> "Puntos largos y games eternos, con muchos deuces y ventajas."
}

private fun involvementHint(involvement: Float): String = when {
    involvement <= 0.15f -> "Te metieron en la heladera: la jugaban casi toda a tu compañero."
    involvement >= 0.35f -> "Te buscaron a vos todo el partido, o fuiste el protagonista."
    else -> "Repartido: 1 de cada 4 pelotas pasó por vos."
}

private fun categoryLabel(c: PadelCategory): String = when (c) {
    PadelCategory.SEPTIMA -> "7ma"
    PadelCategory.SEXTA -> "6ta"
    PadelCategory.QUINTA -> "5ta"
}

/** Copy de feedback por veredicto (estilo FDD, agnóstico de categoría). */
private fun verdictCopy(v: StrokeVerdict): String = when (v) {
    StrokeVerdict.FRIDGE -> "O el partido se liquidó rápido, o te encerraron en el fondo y le cargaron el juego a tu compañero. ¡A practicar la paciencia!"
    StrokeVerdict.NORMAL -> "Juego bien distribuido. Participación fluida, sin desgastarte de más. ¡Buen ritmo!"
    StrokeVerdict.HIGH_LOAD -> "Te buscaron un montón o los puntos se hicieron largos. Tocó ponerse el overol y laburar cada pelota."
    StrokeVerdict.MARATHON -> "Volumen altísimo. Puro desgaste trayendo todo desde el fondo. Poné el brazo en hielo."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var games by remember { mutableIntStateOf(18) }
    var intensity by remember { mutableStateOf(PointIntensity.MEDIUM) }
    var involvement by remember { mutableFloatStateOf(0.25f) }
    var category by remember { mutableStateOf(PadelCategory.SEXTA) }

    val estimate = estimateStrokes(games, intensity, involvement, category)
    val verdict = category.verdict(estimate.pgg)

    Scaffold(
        topBar = { PadelTopAppBar(title = "Calculadora de golpes", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Card de resultado ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        text = estimate.totalStrokes.toString(),
                        style = PadelTheme.sportType.scoreNumeral.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "golpes estimados · ${"%.1f".format(estimate.pgg)} PGG",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StrokeVerdictBadge(verdict)
                    Text(
                        text = verdictCopy(verdict),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // --- Games ---
            Column {
                SectionHeader("GAMES JUGADOS · $games")
                Slider(
                    value = games.toFloat(),
                    onValueChange = { games = it.roundToInt() },
                    valueRange = 12f..36f,
                    steps = 23,
                )
            }

            // --- Intensidad ---
            Column {
                SectionHeader("INTENSIDAD DE PUNTOS")
                Spacer(modifier = Modifier.height(8.dp))
                val options = PointIntensity.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, opt ->
                        SegmentedButton(
                            selected = intensity == opt,
                            onClick = { intensity = opt },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(intensityLabel(opt)) }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = intensityHint(intensity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            // --- Involucramiento ---
            Column {
                SectionHeader("TU INVOLUCRAMIENTO")
                Spacer(modifier = Modifier.height(8.dp))
                val options = listOf(0.15f, 0.25f, 0.35f)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, opt ->
                        SegmentedButton(
                            selected = involvement == opt,
                            onClick = { involvement = opt },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text("${(opt * 100).roundToInt()}%") }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = involvementHint(involvement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            // --- Categoría ---
            Column {
                SectionHeader("CATEGORÍA")
                Spacer(modifier = Modifier.height(8.dp))
                val options = PadelCategory.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, opt ->
                        SegmentedButton(
                            selected = category == opt,
                            onClick = { category = opt },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(categoryLabel(opt)) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
