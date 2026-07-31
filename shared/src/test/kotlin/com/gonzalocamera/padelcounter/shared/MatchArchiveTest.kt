package com.gonzalocamera.padelcounter.shared

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MatchArchiveTest {

    private fun match(id: String, strokesPerSet: List<Int>? = null) = Match(
        id = id,
        startedAt = 1700000000000L,
        finishedAt = 1700003600000L,
        setsScore = listOf(listOf(6, 4), listOf(6, 3)),
        tieBreakUsed = false,
        decider = Decider.TB7,
        scoringMode = ScoringMode.DEUCE,
        winner = Winner.MY,
        origin = MatchOrigin.WEAR,
        strokesPerSet = strokesPerSet,
    )

    @Test
    fun `round-trip preserves every match`() {
        val archive = MatchArchive(
            exportedAt = 1700000000000L,
            matches = listOf(
                match("a"),
                match("b", strokesPerSet = listOf(42, 38)),
                match("c").copy(origin = MatchOrigin.MANUAL),
            ),
        )

        val decoded = decodeArchive(encodeArchive(archive))

        assertThat(decoded).isEqualTo(archive)
        assertThat(decoded.matches).hasSize(3)
        assertThat(decoded.matches[1].strokesPerSet).containsExactly(42, 38).inOrder()
        assertThat(decoded.matches[2].origin).isEqualTo(MatchOrigin.MANUAL)
    }

    @Test
    fun `round-trip of an empty history`() {
        val decoded = decodeArchive(encodeArchive(MatchArchive(exportedAt = 1L, matches = emptyList())))

        assertThat(decoded.matches).isEmpty()
        assertThat(decoded.version).isEqualTo(ARCHIVE_VERSION)
    }

    @Test
    fun `stamps the current archive version`() {
        val json = encodeArchive(MatchArchive(exportedAt = 1L, matches = emptyList()))

        assertThat(json).contains("\"version\": $ARCHIVE_VERSION")
    }

    @Test
    fun `blank file is rejected`() {
        val error = runCatching { decodeArchive("   ") }.exceptionOrNull()

        assertThat(error).isInstanceOf(ArchiveDecodeException::class.java)
        assertThat(error).hasMessageThat().contains("vacío")
    }

    @Test
    fun `unrelated json is rejected`() {
        val error = runCatching { decodeArchive("""{"hola":"mundo"}""") }.exceptionOrNull()

        assertThat(error).isInstanceOf(ArchiveDecodeException::class.java)
        assertThat(error).hasMessageThat().contains("no es un historial")
    }

    @Test
    fun `garbage is rejected without crashing`() {
        val error = runCatching { decodeArchive("no soy json {{{") }.exceptionOrNull()

        assertThat(error).isInstanceOf(ArchiveDecodeException::class.java)
    }

    @Test
    fun `a newer archive version is rejected with a clear reason`() {
        val json = """{"version":99,"exportedAt":1,"matches":[]}"""

        val error = runCatching { decodeArchive(json) }.exceptionOrNull()

        assertThat(error).isInstanceOf(ArchiveDecodeException::class.java)
        assertThat(error).hasMessageThat().contains("versión más nueva")
    }

    @Test
    fun `unknown fields are ignored so an older app can read a newer file`() {
        val json = """{"version":1,"exportedAt":1,"matches":[],"campoNuevo":"algo"}"""

        assertThat(decodeArchive(json).matches).isEmpty()
    }
}
