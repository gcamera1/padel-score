package com.gonzalocamera.padelcounter.presentation

import com.google.common.truth.Truth.assertThat
import com.gonzalocamera.padelcounter.shared.PadelState
import org.junit.Test

/**
 * Distinguir "en juego" de "terminado sin cerrar" es lo que decide si la invitación a
 * calificar aparece. Tratarlos igual la dejó inalcanzable, porque el resumen del último
 * partido es exactamente donde queda el usuario cuando cierra la app.
 */
class MatchInProgressTest {

    @Test
    fun `sin saque elegido no hay partido en juego`() {
        assertThat(isMatchInProgress(PadelState(isServeSet = false))).isFalse()
    }

    @Test
    fun `con saque elegido y sin terminar hay partido en juego`() {
        val state = PadelState(isServeSet = true, myGames = 3, oppGames = 2, bestOf = 3)
        assertThat(isMatchInProgress(state)).isTrue()
    }

    @Test
    fun `un partido terminado sin cerrar NO esta en juego`() {
        val finished = PadelState(
            isServeSet = true,
            mySets = 1, oppSets = 0, bestOf = 1,
            setsHistory = listOf(listOf(6, 0)),
        )
        assertThat(isMatchInProgress(finished)).isFalse()
    }
}
