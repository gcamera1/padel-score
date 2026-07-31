package com.gonzalocamera.padelcounter.mobile.ui.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupMessagesTest {

    @Test
    fun `export message agrees in number`() {
        assertThat(exportMessage(0)).isEqualTo("Se guardó un backup vacío")
        assertThat(exportMessage(1)).isEqualTo("Se exportó 1 partido")
        assertThat(exportMessage(7)).isEqualTo("Se exportaron 7 partidos")
    }

    @Test
    fun `import message agrees in number`() {
        assertThat(importMessage(imported = 1, skipped = 0)).isEqualTo("Se importó 1 partido")
        assertThat(importMessage(imported = 5, skipped = 0)).isEqualTo("Se importaron 5 partidos")
    }

    @Test
    fun `import message reports the skipped ones`() {
        assertThat(importMessage(imported = 3, skipped = 1))
            .isEqualTo("Se importaron 3 partidos · 1 partido ya estaba")
        assertThat(importMessage(imported = 3, skipped = 2))
            .isEqualTo("Se importaron 3 partidos · 2 partidos ya estaban")
    }

    @Test
    fun `nothing new to import`() {
        assertThat(importMessage(imported = 0, skipped = 4))
            .isEqualTo("Ya tenías todos los partidos del backup")
    }

    @Test
    fun `empty backup`() {
        assertThat(importMessage(imported = 0, skipped = 0))
            .isEqualTo("El backup no tenía partidos")
    }
}
