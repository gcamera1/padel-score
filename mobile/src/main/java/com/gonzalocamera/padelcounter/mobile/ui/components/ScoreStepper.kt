package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

/**
 * Selector numérico compacto (− valor +) para cargar games a mano.
 *
 * Se usa en vez de un campo de texto: no abre teclado y hace imposible una entrada
 * inválida. [contentDescriptionPrefix] etiqueta ambos botones (ej: "Mis games del set 1").
 */
@Composable
fun ScoreStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    contentDescriptionPrefix: String,
    modifier: Modifier = Modifier,
    range: IntRange = 0..7,
) {
    val canDecrease = value > range.first
    val canIncrease = value < range.last

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PadelPalette.Gray),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = { onValueChange(value - 1) },
            enabled = canDecrease,
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "$contentDescriptionPrefix, quitar uno",
                tint = if (canDecrease) PadelTheme.colors.gold else PadelTheme.colors.textFaint,
            )
        }

        Text(
            text = value.toString(),
            // tnum: el ancho del dígito no cambia, así el botón "+" no se mueve al tocarlo.
            style = PadelTheme.sportType.setGameNumeral.copy(fontFeatureSettings = "tnum"),
            color = if (value > 0) PadelPalette.Text else PadelTheme.colors.textFaint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )

        IconButton(
            onClick = { onValueChange(value + 1) },
            enabled = canIncrease,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "$contentDescriptionPrefix, sumar uno",
                tint = if (canIncrease) PadelTheme.colors.gold else PadelTheme.colors.textFaint,
            )
        }
    }
}
