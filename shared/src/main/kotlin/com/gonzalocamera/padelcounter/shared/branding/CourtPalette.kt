package com.gonzalocamera.padelcounter.shared.branding

import com.gonzalocamera.padelcounter.shared.CourtColorOption

object CourtPalette {
    const val Blue: Long = 0xFF1976D2
    const val Orange: Long = 0xFFF55600
    const val Green: Long = 0xFF2E7D32
    const val Purple: Long = 0xFF6A1B9A

    /**
     * La cancha se pinta con alpha ~0.27 sobre el fondo, así que un negro puro se funde con
     * el fondo oscuro y deja a la vista solo las líneas. Es el efecto buscado en el reloj
     * (OLED) y el motivo por el que este valor es #000 y no un gris oscuro.
     */
    const val Black: Long = 0xFF000000
}

fun CourtColorOption.hex(): Long = when (this) {
    CourtColorOption.BLUE -> CourtPalette.Blue
    CourtColorOption.ORANGE -> CourtPalette.Orange
    CourtColorOption.GREEN -> CourtPalette.Green
    CourtColorOption.PURPLE -> CourtPalette.Purple
    CourtColorOption.BLACK -> CourtPalette.Black
}
