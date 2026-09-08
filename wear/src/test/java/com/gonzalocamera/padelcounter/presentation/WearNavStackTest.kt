package com.gonzalocamera.padelcounter.presentation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El "atrás" del reloj (botón físico y deslizamiento) tiene que desandar la misma pila que
 * los botones "Volver"/"Cancelar" de cada pantalla, y salir al menú del reloj solo en las
 * raíces. Cada test es uno de los recorridos reales de la app.
 */
class WearNavStackTest {

    @Test
    fun `la cancha es raiz - no hay a donde volver`() {
        val nav = WearNavStack(Screen.COUNTER)
        assertThat(nav.canGoBack).isFalse()
    }

    @Test
    fun `de ajustes se vuelve a la cancha`() {
        val nav = WearNavStack(Screen.COUNTER)
        nav.open(Screen.SETTINGS)
        assertThat(nav.canGoBack).isTrue()
        nav.back()
        assertThat(nav.current).isEqualTo(Screen.COUNTER)
        assertThat(nav.canGoBack).isFalse()
    }

    @Test
    fun `ajustes - probar contador vuelve a ajustes`() {
        val nav = WearNavStack(Screen.COUNTER)
        nav.open(Screen.SETTINGS)
        nav.open(Screen.STROKE_TEST)
        nav.back()
        assertThat(nav.current).isEqualTo(Screen.SETTINGS)
    }

    @Test
    fun `ajustes - tutorial - recorrido guiado desanda paso a paso`() {
        val nav = WearNavStack(Screen.COUNTER)
        nav.open(Screen.SETTINGS)
        nav.open(Screen.TUTORIAL)
        nav.open(Screen.WALKTHROUGH)

        nav.back()
        assertThat(nav.current).isEqualTo(Screen.TUTORIAL)
        nav.back()
        assertThat(nav.current).isEqualTo(Screen.SETTINGS)
        nav.back()
        assertThat(nav.current).isEqualTo(Screen.COUNTER)
        assertThat(nav.canGoBack).isFalse()
    }

    @Test
    fun `ajustes - nuevo partido cancelado vuelve a ajustes`() {
        val nav = WearNavStack(Screen.COUNTER)
        nav.open(Screen.SETTINGS)
        nav.open(Screen.NEW_MATCH)
        nav.back()
        assertThat(nav.current).isEqualTo(Screen.SETTINGS)
    }

    /** El mismo destino, otro origen: es lo que obliga a tener pila y no un mapa padre. */
    @Test
    fun `fin de partido - nuevo partido cancelado vuelve a fin de partido`() {
        val nav = WearNavStack(Screen.COUNTER)
        nav.openRoot(Screen.MATCH_FINISHED)
        nav.open(Screen.NEW_MATCH)
        nav.back()
        assertThat(nav.current).isEqualTo(Screen.MATCH_FINISHED)
    }

    @Test
    fun `la pantalla de fin de partido es raiz - el atras sale al menu del reloj`() {
        val nav = WearNavStack(Screen.COUNTER)
        nav.open(Screen.SETTINGS)
        nav.openRoot(Screen.MATCH_FINISHED)
        assertThat(nav.canGoBack).isFalse()
    }

    @Test
    fun `arrancar un partido limpia la pila - no se vuelve a la configuracion`() {
        val nav = WearNavStack(Screen.COUNTER)
        nav.open(Screen.SETTINGS)
        nav.open(Screen.NEW_MATCH)
        nav.openRoot(Screen.COUNTER)   // "Arrancar"
        assertThat(nav.current).isEqualTo(Screen.COUNTER)
        assertThat(nav.canGoBack).isFalse()
    }

    @Test
    fun `back en la raiz no cambia nada`() {
        val nav = WearNavStack(Screen.COUNTER)
        nav.back()
        assertThat(nav.current).isEqualTo(Screen.COUNTER)
    }
}
