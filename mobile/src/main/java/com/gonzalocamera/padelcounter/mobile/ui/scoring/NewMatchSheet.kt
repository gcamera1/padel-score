package com.gonzalocamera.padelcounter.mobile.ui.scoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.components.SectionHeader
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.ScoringMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMatchSheet(
    defaultDecider: Decider,
    defaultScoringMode: ScoringMode,
    defaultBestOf: Int,
    onDismiss: () -> Unit,
    onConfirm: (Decider, ScoringMode, Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedDecider by remember { mutableStateOf(defaultDecider) }
    var selectedMode by remember { mutableStateOf(defaultScoringMode) }
    var selectedBestOf by remember { mutableStateOf(defaultBestOf) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Nuevo partido",
                style = MaterialTheme.typography.headlineMedium,
            )

            Column {
                SectionHeader("DEFINICIÓN")
                Spacer(modifier = Modifier.height(8.dp))
                val deciderOptions = listOf(Decider.TB7 to "TB a 7", Decider.SUPER10 to "Súper TB 10")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    deciderOptions.forEachIndexed { i, (opt, label) ->
                        SegmentedButton(
                            selected = selectedDecider == opt,
                            onClick = { selectedDecider = opt },
                            shape = SegmentedButtonDefaults.itemShape(i, deciderOptions.size),
                        ) { Text(label) }
                    }
                }
            }

            Column {
                SectionHeader("MODO DE JUEGO")
                Spacer(modifier = Modifier.height(8.dp))
                val modeOptions = listOf(
                    ScoringMode.DEUCE to "Deuce",
                    ScoringMode.GOLDEN_POINT to "Pto. Oro",
                    ScoringMode.STAR_POINT to "Star Pt.",
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modeOptions.forEachIndexed { i, (opt, label) ->
                        SegmentedButton(
                            selected = selectedMode == opt,
                            onClick = { selectedMode = opt },
                            shape = SegmentedButtonDefaults.itemShape(i, modeOptions.size),
                        ) { Text(label) }
                    }
                }
            }

            Column {
                SectionHeader("SETS")
                Spacer(modifier = Modifier.height(8.dp))
                val bestOfOptions = listOf(1, 3, 5)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    bestOfOptions.forEachIndexed { i, n ->
                        SegmentedButton(
                            selected = selectedBestOf == n,
                            onClick = { selectedBestOf = n },
                            shape = SegmentedButtonDefaults.itemShape(i, bestOfOptions.size),
                        ) { Text("$n") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onConfirm(selectedDecider, selectedMode, selectedBestOf) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Arrancar partido")
            }
        }
    }
}
