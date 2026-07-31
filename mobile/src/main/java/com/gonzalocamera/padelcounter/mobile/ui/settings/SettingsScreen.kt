package com.gonzalocamera.padelcounter.mobile.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.data.UserPreferences
import com.gonzalocamera.padelcounter.mobile.ui.components.CourtColorThumb
import com.gonzalocamera.padelcounter.mobile.ui.components.SectionHeader
import com.gonzalocamera.padelcounter.shared.CourtColorOption
import com.gonzalocamera.padelcounter.shared.PadelCategory
import com.gonzalocamera.padelcounter.shared.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BACKUP_MIME = "application/json"

private val backupDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private fun defaultBackupFileName(): String =
    "padel-historial-${backupDateFormat.format(Date())}.json"

private fun partidos(n: Int): String = if (n == 1) "1 partido" else "$n partidos"

internal fun exportMessage(matchCount: Int): String = when (matchCount) {
    0 -> "Se guardó un backup vacío"
    1 -> "Se exportó 1 partido"
    else -> "Se exportaron $matchCount partidos"
}

internal fun importMessage(imported: Int, skipped: Int): String = when {
    imported == 0 && skipped == 0 -> "El backup no tenía partidos"
    imported == 0 -> "Ya tenías todos los partidos del backup"
    else -> buildString {
        append(if (imported == 1) "Se importó 1 partido" else "Se importaron $imported partidos")
        if (skipped > 0) append(" · ${partidos(skipped)} ya ${if (skipped == 1) "estaba" else "estaban"}")
    }
}

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // SAF: el usuario elige dónde guardar / qué abrir, así que no hacen falta permisos
    // de almacenamiento y el archivo queda visible en la app Archivos del teléfono.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = viewModel.exportJson(System.currentTimeMillis())
            val message = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(result.json.toByteArray())
                    } ?: error("stream nulo")
                    exportMessage(result.matchCount)
                }.getOrElse { "No se pudo guardar el archivo" }
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?.toString(Charsets.UTF_8)
                }.getOrNull()
            }
            val message = when {
                text == null -> "No se pudo leer el archivo"
                else -> when (val result = viewModel.importJson(text)) {
                    is ImportResult.Invalid -> result.reason
                    is ImportResult.Success -> importMessage(result.imported, result.skipped)
                }
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    SettingsContent(
        prefs = prefs,
        onKeepScreenOnChange = viewModel::setKeepScreenOn,
        onCourtColorChange = viewModel::setCourtColor,
        onThemeChange = viewModel::setThemeMode,
        onCategoryChange = viewModel::setCategory,
        onOpenCalculator = onOpenCalculator,
        onExportHistory = { exportLauncher.launch(defaultBackupFileName()) },
        // Muchos gestores de archivos etiquetan el .json como octet-stream, así que
        // filtrar solo por application/json ocultaría backups válidos.
        onImportHistory = { importLauncher.launch(arrayOf(BACKUP_MIME, "*/*")) },
        onContactUs = { contactByEmail(context) },
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
    onExportHistory: () -> Unit = {},
    onImportHistory: () -> Unit = {},
    onContactUs: () -> Unit = {},
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
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onExportHistory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar historial")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onImportHistory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Importar historial")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "El backup se guarda como archivo .json. Al importar se agregan " +
                    "solo los partidos que falten: nunca se pisa lo que ya tenés.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        Column {
            SectionHeader("CONTACTO")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "¿Encontraste un error o se te ocurre algo para mejorar?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onContactUs,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contactanos")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
