package com.gonzalocamera.padelcounter.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Dispositivo donde terminó abriéndose la ficha de Google Play. */
enum class RateTarget { WATCH, PHONE, NONE }

/**
 * Abre la ficha de la app en Google Play para calificarla.
 *
 * Primero en el propio reloj, que es donde el usuario tocó. Si ahí no hay Play Store —algún
 * Wear OS sin servicios de Google— cae al teléfono vinculado con el mismo mecanismo que
 * "Instalar app en el teléfono". No se usa la in-app review de Play: su API prohíbe pedirle
 * nada al usuario antes de mostrar su propia tarjeta, y acá el pedido es justamente el punto
 * (es la misma razón por la que el modal del teléfono deep-linkea a la ficha).
 */
suspend fun openRatingListing(context: Context): RateTarget {
    val intent = Intent(Intent.ACTION_VIEW)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setData(Uri.parse("market://details?id=${context.packageName}"))
    return try {
        context.startActivity(intent)
        RateTarget.WATCH
    } catch (_: ActivityNotFoundException) {
        if (CompanionDetector.openListingOnPhone(context)) RateTarget.PHONE else RateTarget.NONE
    }
}

/**
 * Avisa cuando la ficha NO se abrió en la muñeca. Sin esto el botón parece roto: el usuario
 * toca, Play arranca en el teléfono que tiene en el bolsillo y en el reloj no pasa nada.
 */
fun announceRating(context: Context, target: RateTarget) {
    val message = when (target) {
        RateTarget.WATCH -> return
        RateTarget.PHONE -> "Seguí en el teléfono"
        RateTarget.NONE -> "No pude abrir Google Play"
    }
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
