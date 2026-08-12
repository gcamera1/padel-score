package com.gonzalocamera.padelcounter.shared

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El id del partido tiene que ser estable: el reloj reenvía el mismo partido cada vez que se
 * abre la app con un partido terminado sin resetear, y de ese id dependen las dos defensas
 * contra duplicados (el path del `DataItem` y el `INSERT OR IGNORE` del teléfono).
 */
class MatchIdTest {

    private val started = 1_722_816_660_000L
    private val sets = listOf(listOf(4, 6), listOf(6, 4), listOf(7, 5))

    @Test
    fun `same match always yields the same id`() {
        assertThat(matchId(started, sets)).isEqualTo(matchId(started, sets))
    }

    @Test
    fun `a different start time yields a different id`() {
        assertThat(matchId(started, sets)).isNotEqualTo(matchId(started + 1, sets))
    }

    @Test
    fun `a different score yields a different id`() {
        val other = listOf(listOf(6, 4), listOf(6, 4))

        assertThat(matchId(started, sets)).isNotEqualTo(matchId(started, other))
    }

    /**
     * El id viaja como último segmento de `/padel-score/match/{id}`, así que no puede traer
     * caracteres que rompan el path del `DataClient`.
     */
    @Test
    fun `id is safe to use as a DataClient path segment`() {
        val id = matchId(started, sets)

        assertThat(id).matches("[0-9-]+")
        assertThat(id).doesNotContain("/")
        assertThat(id).doesNotContain(" ")
    }

    @Test
    fun `super tie-break scores stay path safe`() {
        val id = matchId(started, listOf(listOf(6, 4), listOf(4, 6), listOf(10, 8)))

        assertThat(id).matches("[0-9-]+")
    }

    /** Defensivo: el marcador puede venir vacío si el partido se corta antes de cerrar un set. */
    @Test
    fun `empty score still produces a usable id`() {
        val id = matchId(started, emptyList())

        assertThat(id).isEqualTo("$started")
        assertThat(id).matches("[0-9-]+")
    }
}
