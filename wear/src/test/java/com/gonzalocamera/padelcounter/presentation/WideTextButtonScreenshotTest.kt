package com.gonzalocamera.padelcounter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenRound
import com.gonzalocamera.padelcounter.presentation.theme.PadelCounterTheme
import org.junit.Rule
import org.junit.Test

/**
 * WO-V1: el texto de los botones no se corta con la fuente del sistema en grande.
 *
 * Google rechazó la app dos veces por texto cortado con fuente grande. La segunda vez la causa
 * fueron los botones de ancho completo hechos con `Button`/`OutlinedButton` de Wear Material:
 * son botones **circulares para iconos** (`size(52.dp)` fijo, sin padding interno), así que con
 * la fuente en Largest la etiqueta envolvía y perdía la primera y la última letra
 * ("Recorrido guiado" se leía "ecorrido / uiado").
 *
 * `WideTextButton` los reemplazó por `Chip`/`OutlinedChip`, que crecen en alto y reservan
 * padding. Este test fija ese comportamiento: se renderizan las etiquetas más largas de la app
 * en el reloj más chico que soportamos (198dp) con la fuente al máximo que escala Wear OS (1.3).
 *
 * Si alguien vuelve a usar `Button` con texto, las capturas cambian y el test falla.
 *
 * A diferencia de las pantallas completas, esto SÍ se puede cubrir en Paparazzi: los botones no
 * viven dentro de un `ScalingLazyColumn`, que es lo que renderiza vacío.
 */
class WideTextButtonScreenshotTest {

    /** Galaxy Watch 40mm renderizado a 2x — 198dp, el más chico que soportamos. */
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 792, screenHeight = 792,
            density = Density.XXXHIGH,
            screenRound = ScreenRound.ROUND
        ).copy(fontScale = 1.3f),
        maxPercentDifference = 0.1
    )

    @Test
    fun instalarAppEnElTelefono_largestFont() =
        snapshotButton("Instalar app en el teléfono", primary = false)

    @Test
    fun recorridoGuiado_largestFont() =
        snapshotButton("Recorrido guiado", primary = true)

    @Test
    fun probarContador_largestFont() =
        snapshotButton("Probar contador", primary = false)

    /**
     * Un botón por captura: si se apilan varios, el que sobra queda cortado por el alto de la
     * pantalla y la captura deja de distinguir "el texto no entra en el botón" de "no entra
     * otro botón más".
     */
    private fun snapshotButton(text: String, primary: Boolean) = paparazzi.snapshot {
        PadelCounterTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
            ) {
                WideTextButton(text, onClick = {}, primary = primary)
            }
        }
    }
}
