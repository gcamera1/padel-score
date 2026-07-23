package com.gonzalocamera.padelcounter.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de marca "Premier Padel" (negro mate + oro metálico), espejo de
 * `PadelPalette` del módulo :mobile (ver DESIGN.md). Se mantienen locales a
 * :wear para no acoplar los módulos; los hex son la fuente de verdad compartida
 * a nivel visual.
 */
object WearBrand {
    val Gold = Color(0xFFC5A85A)       // acento de marca, bordes, selección
    val GoldLight = Color(0xFFE5C453)  // números destacados, victorias
    val GoldDark = Color(0xFF8A6F30)   // sombras/gradientes de oro
    val OnGold = Color(0xFF14110A)     // texto/íconos sobre oro sólido

    val Background = Color(0xFF0B0C0D) // fondo
    val Card = Color(0xFF151719)       // superficies / chips
    val Gray = Color(0xFF222528)       // chips inertes, divisores
    val Text = Color(0xFFE2E5E8)       // texto primario
    val TextMuted = Color(0xFF9CA3AF)  // texto secundario
    val TextFaint = Color(0xFF6B7280)  // captions, inactivos

    val Live = Color(0xFFDC2626)       // rojo de marca ("EN VIVO" / error)
}
