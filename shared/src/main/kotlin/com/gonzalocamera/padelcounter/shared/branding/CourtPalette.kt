package com.gonzalocamera.padelcounter.shared.branding

import com.gonzalocamera.padelcounter.shared.CourtColorOption

object CourtPalette {
    const val Blue: Long = 0xFF1976D2
    const val Orange: Long = 0xFFF55600
    const val Green: Long = 0xFF2E7D32
    const val Purple: Long = 0xFF6A1B9A
}

fun CourtColorOption.hex(): Long = when (this) {
    CourtColorOption.BLUE -> CourtPalette.Blue
    CourtColorOption.ORANGE -> CourtPalette.Orange
    CourtColorOption.GREEN -> CourtPalette.Green
    CourtColorOption.PURPLE -> CourtPalette.Purple
}
