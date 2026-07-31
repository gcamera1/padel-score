package com.gonzalocamera.padelcounter.mobile.ui.history

import com.gonzalocamera.padelcounter.mobile.ui.components.display
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.PadelCategory
import com.gonzalocamera.padelcounter.shared.Winner
import com.gonzalocamera.padelcounter.shared.strokeStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val shareDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))

/**
 * Arma el texto del partido para compartir por WhatsApp y afines.
 *
 * Usa el markup de WhatsApp (`*negrita*`, `_cursiva_`), que otras apps muestran
 * como texto plano sin romperse. El bloque de golpes solo aparece si el partido
 * trae datos del reloj — un partido manual o sin sensor lo omite entero.
 */
internal fun Match.toShareText(category: PadelCategory): String = buildString {
    val won = winner == Winner.MY
    val scoreLine = setsScore.joinToString(" ") { "${it[0]}-${it[1]}" }

    append("🎾 *")
    append(if (won) "VICTORIA" else "DERROTA")
    append("* ")
    append(scoreLine)
    append("\n\n")

    setsScore.forEachIndexed { index, set ->
        // Oro para el set ganado, plata para el perdido: marca el resultado sin
        // que una derrota se lea como un reproche.
        append(if (set[0] > set[1]) "🥇" else "🥈")
        append(" Set ${index + 1}: *${set[0]}-${set[1]}*\n")
    }

    append("\n📅 ")
    append(shareDateFormat.format(Date(finishedAt)))
    append(" · ")
    append(bestOfLabel(bestOf))

    strokeStats(category)?.let { stats ->
        val (emoji, label) = stats.verdict.display()
        append("\n\n💪 *${stats.totalStrokes} golpes* · ")
        append(formatPgg(stats.pgg))
        append(" PGG\n")
        append("$emoji $label")
    }

    append("\n\n_Simple Padel Score_")
}

private fun bestOfLabel(bestOf: Int): String = when (bestOf) {
    1 -> "Al mejor de 1 set"
    else -> "Al mejor de $bestOf sets"
}

/** Locale fijo: en es-AR "%.1f" usa coma y en el chat queda raro junto a "PGG". */
private fun formatPgg(pgg: Float): String = String.format(Locale.US, "%.1f", pgg)
