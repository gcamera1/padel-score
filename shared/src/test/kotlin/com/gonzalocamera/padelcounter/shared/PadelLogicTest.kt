package com.gonzalocamera.padelcounter.shared

import org.junit.Test
import com.google.common.truth.Truth.assertThat

class PadelLogicTest {

    @Test
    fun `addPointToMy increments from 0 to 15`() {
        val state = PadelState()
        val result = addPointToMy(state)
        assertThat(result.myPointsIdx).isEqualTo(1)
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("15")
    }

    @Test
    fun `addPointToMy increments from 15 to 30`() {
        val state = PadelState(myPointsIdx = 1)
        val result = addPointToMy(state)
        assertThat(result.myPointsIdx).isEqualTo(2)
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("30")
    }

    @Test
    fun `addPointToMy increments from 30 to 40`() {
        val state = PadelState(myPointsIdx = 2)
        val result = addPointToMy(state)
        assertThat(result.myPointsIdx).isEqualTo(3)
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("40")
    }

    @Test
    fun `addPointToMy wins game from 40-less with DEUCE mode`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 1, scoringMode = ScoringMode.DEUCE)
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `addPointToMy wins game from 40-less with GOLDEN_POINT mode`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 1, scoringMode = ScoringMode.GOLDEN_POINT)
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `addPointToMy at 40-40 goes to AD with DEUCE mode`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 3, scoringMode = ScoringMode.DEUCE)
        val result = addPointToMy(state)
        assertThat(result.myPointsIdx).isEqualTo(4)
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("AD")
    }

    @Test
    fun `addPointToMy at 40-40 wins game with GOLDEN_POINT mode`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 3, scoringMode = ScoringMode.GOLDEN_POINT)
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `addPointToMy wins game from AD`() {
        val state = PadelState(myPointsIdx = 4, oppPointsIdx = 3, scoringMode = ScoringMode.DEUCE)
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `addPointToMy from oppAD reverts to Deuce`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 4, scoringMode = ScoringMode.DEUCE)
        val result = addPointToMy(state)
        assertThat(result.myPointsIdx).isEqualTo(3)
        assertThat(result.oppPointsIdx).isEqualTo(3)
    }

    @Test
    fun `subtractPointFromMy decreases correctly`() {
        val state = PadelState(myPointsIdx = 2)
        val result = subtractPointFromMy(state)
        assertThat(result.myPointsIdx).isEqualTo(1)
    }

    @Test
    fun `subtractPointFromMy from 0 stays at 0`() {
        val state = PadelState(myPointsIdx = 0)
        val result = subtractPointFromMy(state)
        assertThat(result.myPointsIdx).isEqualTo(0)
    }

    @Test
    fun `subtractPointFromMy from AD goes to 40`() {
        val state = PadelState(myPointsIdx = 4)
        val result = subtractPointFromMy(state)
        assertThat(result.myPointsIdx).isEqualTo(3)
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("40")
    }

    @Test
    fun `win set at 6-0`() {
        val state = PadelState(myGames = 6, oppGames = 0, myPointsIdx = 3, oppPointsIdx = 0, scoringMode = ScoringMode.DEUCE)
        val result = addPointToMy(state)
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
        assertThat(result.oppGames).isEqualTo(0)
    }

    @Test
    fun `win set at 6-4`() {
        val state = PadelState(myGames = 6, oppGames = 4, myPointsIdx = 3, oppPointsIdx = 0, scoringMode = ScoringMode.DEUCE)
        val result = addPointToMy(state)
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
        assertThat(result.oppGames).isEqualTo(0)
    }

    @Test
    fun `win set when reaching 7-5 from 6-5`() {
        val state = PadelState(myGames = 6, oppGames = 5, myPointsIdx = 3, oppPointsIdx = 0, scoringMode = ScoringMode.DEUCE)
        val result = addPointToMy(state)
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
        assertThat(result.oppGames).isEqualTo(0)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `after set reset, next point starts new game`() {
        val afterSet = PadelState(mySets = 1, myGames = 0, oppGames = 0, myPointsIdx = 0, oppPointsIdx = 0, scoringMode = ScoringMode.DEUCE)
        val result = addPointToMy(afterSet)
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
        assertThat(result.myPointsIdx).isEqualTo(1)
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("15")
    }

    @Test
    fun `enter tiebreak when reaching 6-6 in games`() {
        val state = PadelState(myGames = 5, oppGames = 6, myPointsIdx = 3, oppPointsIdx = 0, scoringMode = ScoringMode.DEUCE)
        val result = addPointToMy(state)
        assertThat(result.inTieBreak).isTrue()
        assertThat(result.myTbPoints).isEqualTo(0)
        assertThat(result.oppTbPoints).isEqualTo(0)
        assertThat(result.mySets).isEqualTo(0)
        assertThat(result.myGames).isEqualTo(6)
        assertThat(result.oppGames).isEqualTo(6)
    }

    @Test
    fun `tiebreak scoring increments correctly`() {
        val state = PadelState(inTieBreak = true, myTbPoints = 0, oppTbPoints = 0, decider = Decider.TB7)
        val result = addPointToMy(state)
        assertThat(result.myTbPoints).isEqualTo(1)
        assertThat(result.inTieBreak).isTrue()
    }

    @Test
    fun `win tiebreak at 7-5 (TB7)`() {
        val state = PadelState(inTieBreak = true, myTbPoints = 6, oppTbPoints = 4, myGames = 6, oppGames = 6, decider = Decider.TB7)
        val result = addPointToMy(state)
        assertThat(result.myTbPoints).isEqualTo(0)
        assertThat(result.oppTbPoints).isEqualTo(0)
        assertThat(result.inTieBreak).isFalse()
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
        assertThat(result.oppGames).isEqualTo(0)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `win tiebreak at 10-8 (SUPER10)`() {
        val state = PadelState(inTieBreak = true, myTbPoints = 9, oppTbPoints = 7, myGames = 6, oppGames = 6, decider = Decider.SUPER10)
        val result = addPointToMy(state)
        assertThat(result.myTbPoints).isEqualTo(0)
        assertThat(result.inTieBreak).isFalse()
        assertThat(result.mySets).isEqualTo(1)
    }

    @Test
    fun `no tiebreak win at 7-6 with TB7`() {
        val state = PadelState(inTieBreak = true, myTbPoints = 6, oppTbPoints = 6, decider = Decider.TB7)
        val result = addPointToMy(state)
        assertThat(result.inTieBreak).isTrue()
        assertThat(result.myTbPoints).isEqualTo(7)
    }

    @Test
    fun `subtract tiebreak point`() {
        val state = PadelState(inTieBreak = true, myTbPoints = 5, oppTbPoints = 3)
        val result = subtractPointFromMy(state)
        assertThat(result.myTbPoints).isEqualTo(4)
    }

    @Test
    fun `subtract tiebreak point stays at 0`() {
        val state = PadelState(inTieBreak = true, myTbPoints = 0)
        val result = subtractPointFromMy(state)
        assertThat(result.myTbPoints).isEqualTo(0)
    }

    @Test
    fun `pointsLabel returns correct values`() {
        assertThat(pointsLabel(PadelState(myPointsIdx = 0), isMe = true)).isEqualTo("0")
        assertThat(pointsLabel(PadelState(myPointsIdx = 1), isMe = true)).isEqualTo("15")
        assertThat(pointsLabel(PadelState(myPointsIdx = 2), isMe = true)).isEqualTo("30")
        assertThat(pointsLabel(PadelState(myPointsIdx = 3), isMe = true)).isEqualTo("40")
        assertThat(pointsLabel(PadelState(myPointsIdx = 4), isMe = true)).isEqualTo("AD")
    }

    @Test
    fun `pointsLabel in tiebreak`() {
        val state = PadelState(inTieBreak = true, myTbPoints = 5)
        assertThat(pointsLabel(state, isMe = true)).isEqualTo("5")
    }

    @Test
    fun `full game sequence 0-15-30-40-win`() {
        var state = PadelState()
        state = addPointToMy(state)
        assertThat(state.myPointsIdx).isEqualTo(1)
        state = addPointToMy(state)
        assertThat(state.myPointsIdx).isEqualTo(2)
        state = addPointToMy(state)
        assertThat(state.myPointsIdx).isEqualTo(3)
        state = addPointToMy(state)
        assertThat(state.myGames).isEqualTo(1)
        assertThat(state.myPointsIdx).isEqualTo(0)
    }

    @Test
    fun `deuce then AD then back to deuce then win`() {
        var state = PadelState(myPointsIdx = 3, oppPointsIdx = 3, scoringMode = ScoringMode.DEUCE)
        state = addPointToMy(state)
        assertThat(state.myPointsIdx).isEqualTo(4)
        state = addPointToOpp(state)
        assertThat(state.myPointsIdx).isEqualTo(3)
        assertThat(state.oppPointsIdx).isEqualTo(3)
        state = addPointToMy(state)
        assertThat(state.myPointsIdx).isEqualTo(4)
        state = addPointToMy(state)
        assertThat(state.myGames).isEqualTo(1)
    }

    @Test
    fun `opponent wins from opp AD`() {
        var state = PadelState(myPointsIdx = 3, oppPointsIdx = 4, scoringMode = ScoringMode.DEUCE)
        state = addPointToOpp(state)
        assertThat(state.oppGames).isEqualTo(1)
        assertThat(state.myPointsIdx).isEqualTo(0)
        assertThat(state.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `subtract game from my when both points are zero`() {
        val state = PadelState(myGames = 3, oppGames = 2, myPointsIdx = 0, oppPointsIdx = 0)
        val result = subtractPointFromMy(state)
        assertThat(result.myGames).isEqualTo(2)
        assertThat(result.oppGames).isEqualTo(2)
    }

    @Test
    fun `subtract game from opp when both points are zero`() {
        val state = PadelState(myGames = 3, oppGames = 2, myPointsIdx = 0, oppPointsIdx = 0)
        val result = subtractPointFromOpp(state)
        assertThat(result.oppGames).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(3)
    }

    @Test
    fun `subtract game does nothing when games already zero`() {
        val state = PadelState(myGames = 0, oppGames = 0, myPointsIdx = 0, oppPointsIdx = 0)
        val result = subtractPointFromMy(state)
        assertThat(result.myGames).isEqualTo(0)
    }

    @Test
    fun `subtract does not remove game when points are not both zero`() {
        val state = PadelState(myGames = 3, oppGames = 2, myPointsIdx = 1, oppPointsIdx = 0)
        val result = subtractPointFromMy(state)
        assertThat(result.myGames).isEqualTo(3)
        assertThat(result.myPointsIdx).isEqualTo(0)
    }

    @Test
    fun `subtract game in tie-break when both tb points are zero`() {
        val state = PadelState(myGames = 6, oppGames = 6, inTieBreak = true, myTbPoints = 0, oppTbPoints = 0)
        val result = subtractPointFromMy(state)
        assertThat(result.myGames).isEqualTo(5)
        assertThat(result.inTieBreak).isFalse()
    }

    @Test
    fun `subtract tb point does not remove game when tb points not both zero`() {
        val state = PadelState(myGames = 6, oppGames = 6, inTieBreak = true, myTbPoints = 2, oppTbPoints = 3)
        val result = subtractPointFromMy(state)
        assertThat(result.myTbPoints).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(6)
        assertThat(result.inTieBreak).isTrue()
    }

    @Test
    fun `serve side toggles each point`() {
        var state = PadelState(myServe = true, serveFromRight = true)
        state = addPointToMy(state)
        assertThat(state.serveFromRight).isFalse()
        state = addPointToMy(state)
        assertThat(state.serveFromRight).isTrue()
        state = addPointToMy(state)
        assertThat(state.serveFromRight).isFalse()
    }

    @Test
    fun `server toggles on game win`() {
        val state = PadelState(myServe = true, myPointsIdx = 3, oppPointsIdx = 0)
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myServe).isFalse()
        assertThat(result.serveFromRight).isTrue()
    }

    @Test
    fun `serve alternates correctly through multiple games`() {
        var state = PadelState(myServe = true, isServeSet = true)
        repeat(4) { state = addPointToMy(state) }
        assertThat(state.myGames).isEqualTo(1)
        assertThat(state.myServe).isFalse()
        repeat(4) { state = addPointToMy(state) }
        assertThat(state.myGames).isEqualTo(2)
        assertThat(state.myServe).isTrue()
    }

    @Test
    fun `serve side toggles in deuce game`() {
        var state = PadelState(myServe = true, serveFromRight = true, myPointsIdx = 3, oppPointsIdx = 3, scoringMode = ScoringMode.DEUCE)
        state = addPointToMy(state)
        assertThat(state.serveFromRight).isFalse()
        state = addPointToOpp(state)
        assertThat(state.serveFromRight).isTrue()
    }

    @Test
    fun `tiebreak serve pattern - first server gets 1 then alternating 2`() {
        var state = PadelState(inTieBreak = true, myGames = 6, oppGames = 6, myServe = true, tieBreakStartedByMe = true, decider = Decider.TB7)
        assertThat(state.myServe).isTrue()
        assertThat(state.serveFromRight).isTrue()

        state = addPointToMy(state)
        assertThat(state.myServe).isFalse()
        assertThat(state.serveFromRight).isFalse()

        state = addPointToMy(state)
        assertThat(state.myServe).isFalse()
        assertThat(state.serveFromRight).isTrue()

        state = addPointToMy(state)
        assertThat(state.myServe).isTrue()
        assertThat(state.serveFromRight).isFalse()

        state = addPointToMy(state)
        assertThat(state.myServe).isTrue()
        assertThat(state.serveFromRight).isTrue()
    }

    @Test
    fun `tiebreak entry saves who starts`() {
        val state = PadelState(myGames = 5, oppGames = 6, myPointsIdx = 3, oppPointsIdx = 0, myServe = true)
        val result = addPointToMy(state)
        assertThat(result.inTieBreak).isTrue()
        assertThat(result.myServe).isFalse()
        assertThat(result.tieBreakStartedByMe).isFalse()
    }

    @Test
    fun `after tiebreak win, opposite of TB starter serves next set`() {
        val state = PadelState(inTieBreak = true, myTbPoints = 6, oppTbPoints = 4, myGames = 6, oppGames = 6, tieBreakStartedByMe = false, decider = Decider.TB7)
        val result = addPointToMy(state)
        assertThat(result.inTieBreak).isFalse()
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myServe).isTrue()
        assertThat(result.serveFromRight).isTrue()
    }

    @Test
    fun `subtract game toggles server back`() {
        val state = PadelState(myGames = 3, oppGames = 2, myPointsIdx = 0, oppPointsIdx = 0, myServe = true)
        val result = subtractPointFromMy(state)
        assertThat(result.myGames).isEqualTo(2)
        assertThat(result.myServe).isFalse()
        assertThat(result.serveFromRight).isTrue()
    }

    @Test
    fun `subtract point toggles serve side back`() {
        val state = PadelState(myPointsIdx = 2, serveFromRight = false)
        val result = subtractPointFromMy(state)
        assertThat(result.myPointsIdx).isEqualTo(1)
        assertThat(result.serveFromRight).isTrue()
    }

    @Test
    fun `isMatchFinished false at 0-0 bestOf 3`() {
        assertThat(isMatchFinished(PadelState(bestOf = 3))).isFalse()
    }

    @Test
    fun `isMatchFinished true at 2-0 bestOf 3`() {
        assertThat(isMatchFinished(PadelState(mySets = 2, oppSets = 0, bestOf = 3))).isTrue()
    }

    @Test
    fun `isMatchFinished true at 1-2 bestOf 3`() {
        assertThat(isMatchFinished(PadelState(mySets = 1, oppSets = 2, bestOf = 3))).isTrue()
    }

    @Test
    fun `isMatchFinished false at 1-0 bestOf 3`() {
        assertThat(isMatchFinished(PadelState(mySets = 1, oppSets = 0, bestOf = 3))).isFalse()
    }

    @Test
    fun `isMatchFinished true at 1-0 bestOf 1`() {
        assertThat(isMatchFinished(PadelState(mySets = 1, oppSets = 0, bestOf = 1))).isTrue()
    }

    @Test
    fun `isMatchFinished true at 3-0 bestOf 5`() {
        assertThat(isMatchFinished(PadelState(mySets = 3, oppSets = 0, bestOf = 5))).isTrue()
    }

    @Test
    fun `isMatchFinished true at 2-3 bestOf 5`() {
        assertThat(isMatchFinished(PadelState(mySets = 2, oppSets = 3, bestOf = 5))).isTrue()
    }

    @Test
    fun `isMatchFinished false at 2-1 bestOf 5`() {
        assertThat(isMatchFinished(PadelState(mySets = 2, oppSets = 1, bestOf = 5))).isFalse()
    }

    @Test
    fun `computeTbServe pattern is correct`() {
        assertThat(computeTbServe(0, startedByMe = true)).isEqualTo(Pair(true, true))
        assertThat(computeTbServe(1, startedByMe = true)).isEqualTo(Pair(false, false))
        assertThat(computeTbServe(2, startedByMe = true)).isEqualTo(Pair(false, true))
        assertThat(computeTbServe(3, startedByMe = true)).isEqualTo(Pair(true, false))
        assertThat(computeTbServe(4, startedByMe = true)).isEqualTo(Pair(true, true))
        assertThat(computeTbServe(5, startedByMe = true)).isEqualTo(Pair(false, false))
        assertThat(computeTbServe(6, startedByMe = true)).isEqualTo(Pair(false, true))
    }

    @Test
    fun `starPoint at 40-40 goes to AD when deuceCount is 0`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 3, scoringMode = ScoringMode.STAR_POINT, deuceCount = 0)
        val result = addPointToMy(state)
        assertThat(result.myPointsIdx).isEqualTo(4)
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("AD")
    }

    @Test
    fun `starPoint AD won wins game normally`() {
        val state = PadelState(myPointsIdx = 4, oppPointsIdx = 3, scoringMode = ScoringMode.STAR_POINT, deuceCount = 0)
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
    }

    @Test
    fun `starPoint AD lost increments deuceCount`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 4, scoringMode = ScoringMode.STAR_POINT, deuceCount = 0)
        val result = addPointToMy(state)
        assertThat(result.myPointsIdx).isEqualTo(3)
        assertThat(result.oppPointsIdx).isEqualTo(3)
        assertThat(result.deuceCount).isEqualTo(1)
    }

    @Test
    fun `starPoint at 40-40 goes to AD when deuceCount is 1`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 3, scoringMode = ScoringMode.STAR_POINT, deuceCount = 1)
        val result = addPointToMy(state)
        assertThat(result.myPointsIdx).isEqualTo(4)
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("AD")
    }

    @Test
    fun `starPoint at 40-40 with deuceCount 2 wins game as golden point`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 3, scoringMode = ScoringMode.STAR_POINT, deuceCount = 2)
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `starPoint deuceCount resets on game win`() {
        val state = PadelState(myPointsIdx = 4, oppPointsIdx = 3, scoringMode = ScoringMode.STAR_POINT, deuceCount = 1)
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.deuceCount).isEqualTo(0)
    }

    @Test
    fun `starPoint full cycle - 2 AD-deuce cycles then golden`() {
        var state = PadelState(myPointsIdx = 3, oppPointsIdx = 3, scoringMode = ScoringMode.STAR_POINT)
        // First deuce: go to AD
        state = addPointToMy(state)
        assertThat(state.myPointsIdx).isEqualTo(4)
        // AD lost: back to deuce, deuceCount=1
        state = addPointToOpp(state)
        assertThat(state.deuceCount).isEqualTo(1)
        assertThat(state.myPointsIdx).isEqualTo(3)
        // Second deuce: go to AD again
        state = addPointToOpp(state)
        assertThat(state.oppPointsIdx).isEqualTo(4)
        // AD lost: back to deuce, deuceCount=2
        state = addPointToMy(state)
        assertThat(state.deuceCount).isEqualTo(2)
        assertThat(state.myPointsIdx).isEqualTo(3)
        // Third deuce: golden point kicks in
        state = addPointToMy(state)
        assertThat(state.myGames).isEqualTo(1)
        assertThat(state.myPointsIdx).isEqualTo(0)
        assertThat(state.deuceCount).isEqualTo(0)
    }

    @Test
    fun `deuce mode does not increment deuceCount on AD loss`() {
        val state = PadelState(myPointsIdx = 3, oppPointsIdx = 4, scoringMode = ScoringMode.DEUCE, deuceCount = 0)
        val result = addPointToMy(state)
        assertThat(result.deuceCount).isEqualTo(0)
    }

    // ── setsHistory ──

    @Test
    fun `setsHistory is empty at match start`() {
        assertThat(PadelState().setsHistory).isEmpty()
    }

    @Test
    fun `setsHistory records set score on set win 6-0`() {
        val state = PadelState(myGames = 5, oppGames = 0, myPointsIdx = 3, oppPointsIdx = 0)
        val result = addPointToMy(state)
        assertThat(result.setsHistory).isEqualTo(listOf(listOf(6, 0)))
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
    }

    @Test
    fun `setsHistory records set score on set win 6-4`() {
        val state = PadelState(myGames = 5, oppGames = 4, myPointsIdx = 3, oppPointsIdx = 0)
        val result = addPointToMy(state)
        assertThat(result.setsHistory).isEqualTo(listOf(listOf(6, 4)))
    }

    @Test
    fun `setsHistory records 7-5 set correctly`() {
        val state = PadelState(myGames = 6, oppGames = 5, myPointsIdx = 3, oppPointsIdx = 0)
        val result = addPointToMy(state)
        assertThat(result.setsHistory).isEqualTo(listOf(listOf(7, 5)))
    }

    @Test
    fun `setsHistory records tiebreak set as 7-6`() {
        val state = PadelState(
            myGames = 6, oppGames = 6,
            inTieBreak = true, myTbPoints = 6, oppTbPoints = 5,
            tieBreakStartedByMe = true
        )
        val result = addPointToMy(state)
        assertThat(result.setsHistory).isEqualTo(listOf(listOf(7, 6)))
    }

    @Test
    fun `setsHistory accumulates across multiple sets`() {
        val afterFirstSet = PadelState(
            mySets = 1, setsHistory = listOf(listOf(6, 3)),
            myGames = 5, oppGames = 4, myPointsIdx = 3, oppPointsIdx = 0
        )
        val result = addPointToMy(afterFirstSet)
        assertThat(result.setsHistory).isEqualTo(listOf(listOf(6, 3), listOf(6, 4)))
        assertThat(result.mySets).isEqualTo(2)
    }

    @Test
    fun `setsHistory records opponent set win`() {
        val state = PadelState(myGames = 2, oppGames = 5, myPointsIdx = 0, oppPointsIdx = 3)
        val result = addPointToOpp(state)
        assertThat(result.setsHistory).isEqualTo(listOf(listOf(2, 6)))
        assertThat(result.oppSets).isEqualTo(1)
    }
}
