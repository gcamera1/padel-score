package com.gonzalocamera.padelcounter.mobile.ui.history

import android.content.Context
import android.content.Intent
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.PadelCategory

/**
 * Abre la hoja de compartir del sistema con el resultado del partido.
 *
 * Chooser genérico en vez de un intent directo a WhatsApp: no necesita declarar
 * `<queries>` en el manifest, cubre también WhatsApp Business (otro package) y
 * no falla si la app no está instalada.
 */
internal fun shareMatch(context: Context, match: Match, category: PadelCategory) {
    val text = match.toShareText(category)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartir resultado"))
}
