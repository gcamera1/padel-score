package com.gonzalocamera.padelcounter.mobile.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.components.ScoreStepper
import com.gonzalocamera.padelcounter.mobile.ui.components.SectionHeader
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

private val manualDateFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es"))

private const val SET_LABEL_WIDTH_DP = 48

/** Un partido ya jugado: cargar una fecha futura solo puede ser un error de tipeo. */
@OptIn(ExperimentalMaterial3Api::class)
private object NoFutureDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= todayUtcMidnight()

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}

/**
 * Hoja de carga manual de un partido ya jugado. Sigue el mismo patrón que
 * `NewMatchSheet`: wrapper con estado + content stateless (este último es el que
 * se snapshotea, por eso recibe la fecha ya formateada y la validación resuelta).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManualMatchSheet(
    onDismiss: () -> Unit,
    onConfirm: (ManualMatchDraft) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember {
        mutableStateOf(ManualMatchDraft(dateMillis = utcDateMillisToLocalNoon(todayUtcMidnight())))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ManualMatchSheetContent(
            bestOf = draft.bestOf,
            sets = draft.sets,
            dateLabel = manualDateFormat.format(Date(draft.dateMillis)),
            canSave = draft.isValid,
            onBestOfChange = { draft = draft.withBestOf(it) },
            onSetScoreChange = { index, mine, theirs -> draft = draft.withSet(index, mine, theirs) },
            onPickDate = { showDatePicker = true },
            onSave = { onConfirm(draft) },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            // El picker razona en medianoche UTC; el draft guarda mediodía local.
            initialSelectedDateMillis = localMillisToUtcMidnight(draft.dateMillis),
            selectableDates = NoFutureDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            draft = draft.copy(dateMillis = utcDateMillisToLocalNoon(it))
                        }
                        showDatePicker = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManualMatchSheetContent(
    bestOf: Int,
    sets: List<Pair<Int, Int>>,
    dateLabel: String,
    canSave: Boolean,
    onBestOfChange: (Int) -> Unit,
    onSetScoreChange: (index: Int, mine: Int, theirs: Int) -> Unit,
    onPickDate: () -> Unit,
    onSave: () -> Unit,
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
            text = "Cargar partido",
            style = MaterialTheme.typography.headlineLarge,
            color = PadelTheme.colors.gold,
        )

        Column {
            SectionHeader("SETS")
            Spacer(modifier = Modifier.height(8.dp))
            val bestOfOptions = listOf(1, 3, 5)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                bestOfOptions.forEachIndexed { i, n ->
                    SegmentedButton(
                        selected = bestOf == n,
                        onClick = { onBestOfChange(n) },
                        shape = SegmentedButtonDefaults.itemShape(i, bestOfOptions.size),
                    ) { Text("$n") }
                }
            }
        }

        Column {
            SectionHeader("RESULTADO")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(SET_LABEL_WIDTH_DP.dp))
                Text(
                    text = "YO",
                    style = PadelTheme.sportType.sectionHeader,
                    color = PadelTheme.colors.gold,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RIVAL",
                    style = PadelTheme.sportType.sectionHeader,
                    color = PadelTheme.colors.textMuted,
                    modifier = Modifier.weight(1f),
                )
            }

            sets.forEachIndexed { index, (mine, theirs) ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SET ${index + 1}",
                        style = PadelTheme.sportType.sectionHeader,
                        color = PadelTheme.colors.textFaint,
                        modifier = Modifier.width(SET_LABEL_WIDTH_DP.dp),
                    )
                    ScoreStepper(
                        value = mine,
                        onValueChange = { onSetScoreChange(index, it, theirs) },
                        contentDescriptionPrefix = "Mis games del set ${index + 1}",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ScoreStepper(
                        value = theirs,
                        onValueChange = { onSetScoreChange(index, mine, it) },
                        contentDescriptionPrefix = "Games del rival en el set ${index + 1}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Los sets en 0-0 no se guardan.",
                style = MaterialTheme.typography.bodySmall,
                color = PadelTheme.colors.textFaint,
            )
        }

        Column {
            SectionHeader("FECHA")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPickDate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(dateLabel)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Guardar partido")
        }
    }
}
