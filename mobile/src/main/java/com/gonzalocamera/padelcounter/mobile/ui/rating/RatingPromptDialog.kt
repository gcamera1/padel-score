package com.gonzalocamera.padelcounter.mobile.ui.rating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gonzalocamera.padelcounter.mobile.ui.components.PremiumCard
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

/**
 * Invitación a calificar la app. Es un [Dialog] con contenido propio en vez de un
 * `AlertDialog`: el diálogo de Material sale gris y rompería el lenguaje negro mate +
 * dorado del resto de la app (mismo criterio que `MatchEndSheet`).
 *
 * Cerrar con "atrás" o tocando afuera equivale a **"Más tarde"**, nunca a "No, gracias":
 * un descarte accidental no debería silenciar el pedido para siempre.
 */
@Composable
fun RatingPromptDialog(
    onRate: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit,
) {
    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        RatingPromptContent(onRate = onRate, onLater = onLater, onNever = onNever)
    }
}

/** Extraído del [Dialog] porque Paparazzi no captura ventanas de diálogo. */
@Composable
internal fun RatingPromptContent(
    onRate: () -> Unit = {},
    onLater: () -> Unit = {},
    onNever: () -> Unit = {},
) {
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        featured = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = PadelTheme.colors.gold,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "¿Nos dejás una calificación?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Una calificación en Google Play ayuda a que más jugadores la " +
                    "encuentren y que podamos mejorarles la experiencia.",
                style = MaterialTheme.typography.bodyMedium,
                color = PadelPalette.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRate,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Calificar") }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onLater, modifier = Modifier.weight(1f)) {
                    Text("Más tarde")
                }
                TextButton(onClick = onNever, modifier = Modifier.weight(1f)) {
                    Text("No, gracias", color = PadelPalette.TextFaint)
                }
            }
        }
    }
}
