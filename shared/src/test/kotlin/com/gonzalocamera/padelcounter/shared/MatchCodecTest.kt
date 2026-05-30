package com.gonzalocamera.padelcounter.shared

import org.junit.Test
import com.google.common.truth.Truth.assertThat

class MatchCodecTest {

    @Test
    fun `round-trip 6-0 6-0 won by MY`() {
        val match = Match(
            id = "550e8400-e29b-41d4-a716-446655440000",
            startedAt = 1700000000000L,
            finishedAt = 1700001800000L,
            setsScore = listOf(listOf(6, 0), listOf(6, 0)),
            tieBreakUsed = false,
            decider = Decider.TB7,
            scoringMode = ScoringMode.DEUCE,
            winner = Winner.MY,
            origin = MatchOrigin.MOBILE
        )
        val decoded = decodeMatch(encodeMatch(match))
        assertThat(decoded).isEqualTo(match)
    }

    @Test
    fun `round-trip 6-4 4-6 7-6 with tie-break`() {
        val match = Match(
            id = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            startedAt = 1700000000000L,
            finishedAt = 1700007200000L,
            setsScore = listOf(listOf(6, 4), listOf(4, 6), listOf(7, 6)),
            tieBreakUsed = true,
            decider = Decider.TB7,
            scoringMode = ScoringMode.DEUCE,
            winner = Winner.MY,
            origin = MatchOrigin.WEAR
        )
        val decoded = decodeMatch(encodeMatch(match))
        assertThat(decoded).isEqualTo(match)
    }

    @Test
    fun `round-trip with golden point`() {
        val match = Match(
            id = "golden-point-match-id",
            startedAt = 1700000000000L,
            finishedAt = 1700003600000L,
            setsScore = listOf(listOf(6, 3), listOf(6, 2)),
            tieBreakUsed = false,
            decider = Decider.TB7,
            goldenPoint = true,
            scoringMode = ScoringMode.GOLDEN_POINT,
            winner = Winner.MY,
            origin = MatchOrigin.MOBILE
        )
        val decoded = decodeMatch(encodeMatch(match))
        assertThat(decoded).isEqualTo(match)
    }

    @Test
    fun `round-trip with SUPER10`() {
        val match = Match(
            id = "super10-match-id",
            startedAt = 1700000000000L,
            finishedAt = 1700005400000L,
            setsScore = listOf(listOf(4, 6), listOf(6, 3), listOf(7, 6)),
            tieBreakUsed = true,
            decider = Decider.SUPER10,
            scoringMode = ScoringMode.DEUCE,
            winner = Winner.OPP,
            origin = MatchOrigin.WEAR
        )
        val decoded = decodeMatch(encodeMatch(match))
        assertThat(decoded).isEqualTo(match)
    }

    @Test
    fun `round-trip short duration match`() {
        val match = Match(
            id = "short-match",
            startedAt = 1700000000000L,
            finishedAt = 1700000060000L,
            setsScore = listOf(listOf(6, 0), listOf(6, 0)),
            tieBreakUsed = false,
            decider = Decider.TB7,
            goldenPoint = true,
            scoringMode = ScoringMode.GOLDEN_POINT,
            winner = Winner.MY,
            origin = MatchOrigin.MOBILE
        )
        val decoded = decodeMatch(encodeMatch(match))
        assertThat(decoded).isEqualTo(match)
    }

    @Test
    fun `round-trip long duration match`() {
        val match = Match(
            id = "long-match",
            startedAt = 1700000000000L,
            finishedAt = 1700014400000L,
            setsScore = listOf(listOf(6, 7), listOf(7, 6), listOf(7, 6)),
            tieBreakUsed = true,
            decider = Decider.SUPER10,
            scoringMode = ScoringMode.DEUCE,
            winner = Winner.OPP,
            origin = MatchOrigin.WEAR
        )
        val decoded = decodeMatch(encodeMatch(match))
        assertThat(decoded).isEqualTo(match)
    }

    @Test(expected = MatchDecodeException::class)
    fun `decodeMatch throws on empty payload`() {
        decodeMatch(ByteArray(0))
    }

    @Test(expected = MatchDecodeException::class)
    fun `decodeMatch throws on random bytes`() {
        decodeMatch(byteArrayOf(0x00, 0x01, 0x02, 0xFF.toByte()))
    }

    @Test(expected = MatchDecodeException::class)
    fun `decodeMatch throws on invalid JSON`() {
        decodeMatch("{\"id\": 123}".toByteArray())
    }

    @Test(expected = MatchDecodeException::class)
    fun `decodeMatch throws on missing fields`() {
        decodeMatch("{\"id\": \"abc\"}".toByteArray())
    }

    @Test
    fun `round-trip with bestOf field`() {
        val match = Match(
            id = "bestof-test",
            startedAt = 1700000000000L,
            finishedAt = 1700003600000L,
            setsScore = listOf(listOf(6, 3)),
            tieBreakUsed = false,
            decider = Decider.TB7,
            scoringMode = ScoringMode.DEUCE,
            winner = Winner.MY,
            origin = MatchOrigin.MOBILE,
            bestOf = 1
        )
        val decoded = decodeMatch(encodeMatch(match))
        assertThat(decoded).isEqualTo(match)
    }

    @Test
    fun `decode old match without bestOf defaults to 3`() {
        val json = """{"id":"old","startedAt":0,"finishedAt":0,"setsScore":[[6,0],[6,0]],"tieBreakUsed":false,"decider":"TB7","goldenPoint":false,"winner":"MY","origin":"MOBILE"}"""
        val decoded = decodeMatch(json.toByteArray())
        assertThat(decoded.bestOf).isEqualTo(3)
    }

    @Test
    fun `decode old match with goldenPoint true migrates to GOLDEN_POINT`() {
        val json = """{"id":"old-golden","startedAt":0,"finishedAt":0,"setsScore":[[6,3],[6,2]],"tieBreakUsed":false,"decider":"TB7","goldenPoint":true,"winner":"MY","origin":"MOBILE"}"""
        val decoded = decodeMatch(json.toByteArray())
        assertThat(decoded.scoringMode).isEqualTo(ScoringMode.GOLDEN_POINT)
    }

    @Test
    fun `decode old match with goldenPoint false stays DEUCE`() {
        val json = """{"id":"old-deuce","startedAt":0,"finishedAt":0,"setsScore":[[6,3],[6,2]],"tieBreakUsed":false,"decider":"TB7","goldenPoint":false,"winner":"MY","origin":"MOBILE"}"""
        val decoded = decodeMatch(json.toByteArray())
        assertThat(decoded.scoringMode).isEqualTo(ScoringMode.DEUCE)
    }

    @Test
    fun `round-trip with STAR_POINT`() {
        val match = Match(
            id = "star-point-match",
            startedAt = 1700000000000L,
            finishedAt = 1700003600000L,
            setsScore = listOf(listOf(6, 4), listOf(6, 3)),
            tieBreakUsed = false,
            decider = Decider.TB7,
            scoringMode = ScoringMode.STAR_POINT,
            winner = Winner.MY,
            origin = MatchOrigin.MOBILE
        )
        val decoded = decodeMatch(encodeMatch(match))
        assertThat(decoded).isEqualTo(match)
    }

    @Test
    fun `new JSON with scoringMode present ignores goldenPoint for migration`() {
        val json = """{"id":"new","startedAt":0,"finishedAt":0,"setsScore":[[6,3]],"tieBreakUsed":false,"decider":"TB7","goldenPoint":true,"scoringMode":"STAR_POINT","winner":"MY","origin":"MOBILE"}"""
        val decoded = decodeMatch(json.toByteArray())
        assertThat(decoded.scoringMode).isEqualTo(ScoringMode.STAR_POINT)
    }
}
