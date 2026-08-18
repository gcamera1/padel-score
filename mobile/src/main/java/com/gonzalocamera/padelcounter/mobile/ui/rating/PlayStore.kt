package com.gonzalocamera.padelcounter.mobile.ui.rating

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

private const val PLAY_STORE_PACKAGE = "com.android.vending"

/**
 * Abre la ficha de la app en Play Store para que el usuario deje su calificación.
 *
 * Es un link a la ficha, **no** la tarjeta de in-app review de Play: esa API prohíbe
 * preguntarle nada al usuario antes de mostrarla, y el modal de tres botones
 * (`RatingPromptDialog`) es justamente una pregunta previa. Mandarlo a la ficha sí está
 * permitido y además nos deja controlar el "no volver a mostrar".
 */
internal fun openPlayStoreListing(context: Context) {
    val pkg = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
        // Sin paquete explícito, en Samsung `market://` abre un selector entre Galaxy Store
        // y Play Store: una decisión extra en el peor momento, y en Galaxy Store esta ficha
        // no existe. Verificado en un SM-S938U1. Con `setPackage` va derecho a Play; si Play
        // no está instalado tira ActivityNotFoundException y cae al fallback web de abajo.
        setPackage(PLAY_STORE_PACKAGE)
        // Flags recomendados por Google para abrir una ficha: la ficha no queda en
        // nuestro back stack, así que al volver el usuario cae donde estaba.
        addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
        )
    }
    try {
        context.startActivity(market)
        return
    } catch (e: ActivityNotFoundException) {
        // Sin Play Store instalado (emuladores, ROMs sin GMS) queda el navegador.
    }
    try {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg"),
            ),
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No encontramos Play Store en este dispositivo", Toast.LENGTH_LONG)
            .show()
    }
}
