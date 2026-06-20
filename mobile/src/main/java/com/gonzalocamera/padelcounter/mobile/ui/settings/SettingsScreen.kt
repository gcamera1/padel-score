package com.gonzalocamera.padelcounter.mobile.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.data.UserPreferences
import com.gonzalocamera.padelcounter.mobile.ui.components.CourtColorThumb
import com.gonzalocamera.padelcounter.mobile.ui.components.SectionHeader
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.PadelCategory
import com.gonzalocamera.padelcounter.shared.ThemeMode

private fun courtColorLabel(option: CourtColorOption): String = when (option) {
    CourtColorOption.BLUE -> "Azul"
    CourtColorOption.ORANGE -> "Naranja"
    CourtColorOption.GREEN -> "Verde"
    CourtColorOption.PURPLE -> "Violeta"
}

private fun categoryLabel(category: PadelCategory): String = when (category) {
    PadelCategory.SEPTIMA -> "7ma"
    PadelCategory.SEXTA -> "6ta"
    PadelCategory.QUINTA -> "5ta"
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenCalculator: () -> Unit = {},
) {
    val prefs by viewModel.preferences.collectAsState()
    SettingsContent(
        prefs = prefs,
        onKeepScreenOnChange = viewModel::setKeepScreenOn,
        onCourtColorChange = viewModel::setCourtColor,
        onThemeChange = viewModel::setThemeMode,
        onCategoryChange = viewModel::setCategory,
        onOpenCalculator = onOpenCalculator,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    prefs: UserPreferences,
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    onCourtColorChange: (CourtColorOption) -> Unit = {},
    onThemeChange: (ThemeMode) -> Unit = {},
    onCategoryChange: (PadelCategory) -> Unit = {},
    onOpenCalculator: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Column {
            SectionHeader("PANTALLA")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pantalla siempre encendida",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Mantiene la pantalla activa durante el partido",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Switch(checked = prefs.keepScreenOn, onCheckedChange = onKeepScreenOnChange)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tema", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            val themeOptions = listOf(
                ThemeMode.SYSTEM to "Auto",
                ThemeMode.LIGHT to "Claro",
                ThemeMode.DARK to "Oscuro",
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = prefs.themeMode == mode,
                        onClick = { onThemeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, themeOptions.size),
                    ) { Text(label) }
                }
            }
        }

        Column {
            SectionHeader("CANCHA")
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CourtColorOption.entries.forEach { option ->
                    CourtColorThumb(
                        option = option,
                        label = courtColorLabel(option),
                        selected = prefs.courtColor == option,
                        onClick = { onCourtColorChange(option) },
                    )
                }
            }
        }

        Column {
            SectionHeader("CATEGORÍA")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Calibra el diagnóstico de golpes según tu nivel de juego",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            val categoryOptions = PadelCategory.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                categoryOptions.forEachIndexed { i, category ->
                    SegmentedButton(
                        selected = prefs.category == category,
                        onClick = { onCategoryChange(category) },
                        shape = SegmentedButtonDefaults.itemShape(i, categoryOptions.size),
                    ) { Text(categoryLabel(category)) }
                }
            }
        }

        Column {
            SectionHeader("HERRAMIENTAS")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenCalculator,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Calculadora de golpes")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
