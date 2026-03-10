package com.gonzalocamera.padelcounter.presentation

import org.junit.Test
import com.google.common.truth.Truth.assertThat

class PadelLogicTest {

    // ============= NORMAL GAME (sin tie-break) =============

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
    fun `addPointToMy wins game from 40-less with goldenPoint=false`() {
        val state = PadelState(
            myPointsIdx = 3,
            oppPointsIdx = 1,
            goldenPoint = false
        )
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `addPointToMy wins game from 40-less with goldenPoint=true`() {
        val state = PadelState(
            myPointsIdx = 3,
            oppPointsIdx = 1,
            goldenPoint = true
        )
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `addPointToMy at 40-40 goes to AD with goldenPoint=false`() {
        val state = PadelState(
            myPointsIdx = 3,
            oppPointsIdx = 3,
            goldenPoint = false
        )
        val result = addPointToMy(state)
        assertThat(result.myPointsIdx).isEqualTo(4) // AD
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("AD")
    }

    @Test
    fun `addPointToMy at 40-40 wins game with goldenPoint=true`() {
        val state = PadelState(
            myPointsIdx = 3,
            oppPointsIdx = 3,
            goldenPoint = true
        )
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `addPointToMy wins game from AD`() {
        val state = PadelState(
            myPointsIdx = 4, // AD
            oppPointsIdx = 3,
            goldenPoint = false
        )
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `addPointToMy from oppAD reverts to Deuce`() {
        val state = PadelState(
            myPointsIdx = 3,
            oppPointsIdx = 4, // AD opp
            goldenPoint = false
        )
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

    // ============= SET WIN LOGIC =============

    @Test
    fun `win set at 6-0`() {
        val state = PadelState(
            myGames = 6,
            oppGames = 0,
            myPointsIdx = 3,
            oppPointsIdx = 0,
            goldenPoint = false
        )
        val result = addPointToMy(state)
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
        assertThat(result.oppGames).isEqualTo(0)
    }

    @Test
    fun `win set at 6-4`() {
        val state = PadelState(
            myGames = 6,
            oppGames = 4,
            myPointsIdx = 3,
            oppPointsIdx = 0,
            goldenPoint = false
        )
        val result = addPointToMy(state)
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
        assertThat(result.oppGames).isEqualTo(0)
    }

    @Test
    fun `win set when reaching 7-5 from 6-5`() {
        val state = PadelState(
            myGames = 6,
            oppGames = 5,
            myPointsIdx = 3,
            oppPointsIdx = 0,
            goldenPoint = false
        )
        val result = addPointToMy(state)
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
        assertThat(result.oppGames).isEqualTo(0)
        assertThat(result.myPointsIdx).isEqualTo(0)
        assertThat(result.oppPointsIdx).isEqualTo(0)
    }

    @Test
    fun `after set reset, next point starts new game (0 to 15)`() {
        // Simulamos que terminó un set y quedó todo reseteado (como debería)
        val afterSet = PadelState(
            mySets = 1,
            myGames = 0,
            oppGames = 0,
            myPointsIdx = 0,
            oppPointsIdx = 0,
            goldenPoint = false
        )
        val result = addPointToMy(afterSet)
        assertThat(result.mySets).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(0)
        assertThat(result.oppGames).isEqualTo(0)
        assertThat(result.myPointsIdx).isEqualTo(1)
        assertThat(pointsLabel(result, isMe = true)).isEqualTo("15")
    }

    // ============= TIE-BREAK =============

    @Test
    fun `enter tiebreak when reaching 6-6 in games`() {
        val state = PadelState(
            myGames = 5,
            oppGames = 6,
            myPointsIdx = 3,
            oppPointsIdx = 0,
            goldenPoint = false
        )
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
        val state = PadelState(
            inTieBreak = true,
            myTbPoints = 0,
            oppTbPoints = 0,
            decider = Decider.TB7
        )
        val result = addPointToMy(state)
        assertThat(result.myTbPoints).isEqualTo(1)
        assertThat(result.inTieBreak).isTrue()
    }

    @Test
    fun `win tiebreak at 7-5 (TB7)`() {
        val state = PadelState(
            inTieBreak = true,
            myTbPoints = 6,
            oppTbPoints = 4,
            myGames = 6,
            oppGames = 6,
            decider = Decider.TB7
        )
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
        val state = PadelState(
            inTieBreak = true,
            myTbPoints = 9,
            oppTbPoints = 7,
            myGames = 6,
            oppGames = 6,
            decider = Decider.SUPER10
        )
        val result = addPointToMy(state)
        assertThat(result.myTbPoints).isEqualTo(0)
        assertThat(result.inTieBreak).isFalse()
        assertThat(result.mySets).isEqualTo(1)
    }

    @Test
    fun `no tiebreak win at 7-6 with TB7`() {
        val state = PadelState(
            inTieBreak = true,
            myTbPoints = 6,
            oppTbPoints = 6,
            decider = Decider.TB7
        )
        val result = addPointToMy(state)
        assertThat(result.inTieBreak).isTrue()
        assertThat(result.myTbPoints).isEqualTo(7)
    }

    @Test
    fun `subtract tiebreak point`() {
        val state = PadelState(
            inTieBreak = true,
            myTbPoints = 5,
            oppTbPoints = 3
        )
        val result = subtractPointFromMy(state)
        assertThat(result.myTbPoints).isEqualTo(4)
    }

    @Test
    fun `subtract tiebreak point stays at 0`() {
        val state = PadelState(
            inTieBreak = true,
            myTbPoints = 0
        )
        val result = subtractPointFromMy(state)
        assertThat(result.myTbPoints).isEqualTo(0)
    }

    // ============= POINTS LABEL =============

    @Test
    fun `pointsLabel returns correct values`() {
        val state0 = PadelState(myPointsIdx = 0)
        val state1 = PadelState(myPointsIdx = 1)
        val state2 = PadelState(myPointsIdx = 2)
        val state3 = PadelState(myPointsIdx = 3)
        val state4 = PadelState(myPointsIdx = 4)

        assertThat(pointsLabel(state0, isMe = true)).isEqualTo("0")
        assertThat(pointsLabel(state1, isMe = true)).isEqualTo("15")
        assertThat(pointsLabel(state2, isMe = true)).isEqualTo("30")
        assertThat(pointsLabel(state3, isMe = true)).isEqualTo("40")
        assertThat(pointsLabel(state4, isMe = true)).isEqualTo("AD")
    }

    @Test
    fun `pointsLabel in tiebreak`() {
        val state = PadelState(inTieBreak = true, myTbPoints = 5)
        assertThat(pointsLabel(state, isMe = true)).isEqualTo("5")
    }

    // ============= COMPLEX SCENARIOS =============

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
        var state = PadelState(
            myPointsIdx = 3,
            oppPointsIdx = 3,
            goldenPoint = false
        )
        state = addPointToMy(state) // -> AD me
        assertThat(state.myPointsIdx).isEqualTo(4)

        state = addPointToOpp(state) // -> vuelvo a Deuce
        assertThat(state.myPointsIdx).isEqualTo(3)
        assertThat(state.oppPointsIdx).isEqualTo(3)

        state = addPointToMy(state) // -> AD me again
        assertThat(state.myPointsIdx).isEqualTo(4)

        state = addPointToMy(state) // -> Win game
        assertThat(state.myGames).isEqualTo(1)
    }

    @Test
    fun `opponent wins from opp AD`() {
        var state = PadelState(
            myPointsIdx = 3,
            oppPointsIdx = 4, // opp AD
            goldenPoint = false
        )
        state = addPointToOpp(state) // opp wins
        assertThat(state.oppGames).isEqualTo(1)
        assertThat(state.myPointsIdx).isEqualTo(0)
        assertThat(state.oppPointsIdx).isEqualTo(0)
    }

    // ── Subtract game tests ──

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
        assertThat(result.myPointsIdx).isEqualTo(0) // 15 -> 0
    }

    @Test
    fun `subtract game in tie-break when both tb points are zero`() {
        val state = PadelState(
            myGames = 6, oppGames = 6,
            inTieBreak = true, myTbPoints = 0, oppTbPoints = 0
        )
        val result = subtractPointFromMy(state)
        assertThat(result.myGames).isEqualTo(5)
        assertThat(result.inTieBreak).isFalse()
    }

    @Test
    fun `subtract tb point does not remove game when tb points not both zero`() {
        val state = PadelState(
            myGames = 6, oppGames = 6,
            inTieBreak = true, myTbPoints = 2, oppTbPoints = 3
        )
        val result = subtractPointFromMy(state)
        assertThat(result.myTbPoints).isEqualTo(1)
        assertThat(result.myGames).isEqualTo(6)
        assertThat(result.inTieBreak).isTrue()
    }

    // ============= SERVE TRACKING =============

    @Test
    fun `serve side toggles each point`() {
        var state = PadelState(myServe = true, serveFromRight = true)
        state = addPointToMy(state) // 15-0
        assertThat(state.serveFromRight).isFalse()
        state = addPointToMy(state) // 30-0
        assertThat(state.serveFromRight).isTrue()
        state = addPointToMy(state) // 40-0
        assertThat(state.serveFromRight).isFalse()
    }

    @Test
    fun `server toggles on game win`() {
        val state = PadelState(myServe = true, myPointsIdx = 3, oppPointsIdx = 0)
        val result = addPointToMy(state)
        assertThat(result.myGames).isEqualTo(1)
        assertThat(result.myServe).isFalse() // opponent serves next game
        assertThat(result.serveFromRight).isTrue() // reset for new game
    }

    @Test
    fun `serve alternates correctly through multiple games`() {
        var state = PadelState(myServe = true, isServeSet = true)

        // Win game 1 (I serve)
        repeat(4) { state = addPointToMy(state) }
        assertThat(state.myGames).isEqualTo(1)
        assertThat(state.myServe).isFalse()

        // Win game 2 (opponent serves)
        repeat(4) { state = addPointToMy(state) }
        assertThat(state.myGames).isEqualTo(2)
        assertThat(state.myServe).isTrue() // back to me
    }

    @Test
    fun `serve side toggles in deuce game`() {
        var state = PadelState(
            myServe = true, serveFromRight = true,
            myPointsIdx = 3, oppPointsIdx = 3,
            goldenPoint = false
        )
        // At 40-40 (deuce), 6 points played, serveFromRight should be true
        state = addPointToMy(state) // AD me - 7th point
        assertThat(state.serveFromRight).isFalse()
        state = addPointToOpp(state) // back to deuce - 8th point
        assertThat(state.serveFromRight).isTrue()
    }

    @Test
    fun `tiebreak serve pattern - first server gets 1 then alternating 2`() {
        var state = PadelState(
            inTieBreak = true, myGames = 6, oppGames = 6,
            myServe = true, tieBreakStartedByMe = true,
            decider = Decider.TB7
        )

        // Point 1: I serve (starter), from right
        assertThat(state.myServe).isTrue()
        assertThat(state.serveFromRight).isTrue()

        state = addPointToMy(state) // total=1
        // Points 2-3: opponent serves
        assertThat(state.myServe).isFalse()
        assertThat(state.serveFromRight).isFalse() // from left

        state = addPointToMy(state) // total=2
        assertThat(state.myServe).isFalse() // still opponent
        assertThat(state.serveFromRight).isTrue()

        state = addPointToMy(state) // total=3
        // Points 4-5: I serve
        assertThat(state.myServe).isTrue()
        assertThat(state.serveFromRight).isFalse()

        state = addPointToMy(state) // total=4
        assertThat(state.myServe).isTrue()
        assertThat(state.serveFromRight).isTrue()
    }

    @Test
    fun `tiebreak entry saves who starts`() {
        // I serve game 12 (myGames=5→6), entering 6-6
        val state = PadelState(
            myGames = 5, oppGames = 6,
            myPointsIdx = 3, oppPointsIdx = 0,
            myServe = true
        )
        val result = addPointToMy(state)
        assertThat(result.inTieBreak).isTrue()
        // After winning game: serve toggles to opponent
        assertThat(result.myServe).isFalse()
        assertThat(result.tieBreakStartedByMe).isFalse()
    }

    @Test
    fun `after tiebreak win, opposite of TB starter serves next set`() {
        val state = PadelState(
            inTieBreak = true,
            myTbPoints = 6, oppTbPoints = 4,
            myGames = 6, oppGames = 6,
            tieBreakStartedByMe = false, // opponent started TB
            decider = Decider.TB7
        )
        val result = addPointToMy(state)
        assertThat(result.inTieBreak).isFalse()
        assertThat(result.mySets).isEqualTo(1)
        // Opposite of TB starter → I serve next set
        assertThat(result.myServe).isTrue()
        assertThat(result.serveFromRight).isTrue()
    }

    @Test
    fun `subtract game toggles server back`() {
        val state = PadelState(
            myGames = 3, oppGames = 2,
            myPointsIdx = 0, oppPointsIdx = 0,
            myServe = true
        )
        val result = subtractPointFromMy(state)
        assertThat(result.myGames).isEqualTo(2)
        assertThat(result.myServe).isFalse() // toggled back
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
    fun `computeTbServe pattern is correct`() {
        // Point 0: starter serves from right
        assertThat(computeTbServe(0, startedByMe = true)).isEqualTo(Pair(true, true))
        // Point 1: other serves from left
        assertThat(computeTbServe(1, startedByMe = true)).isEqualTo(Pair(false, false))
        // Point 2: other serves from right
        assertThat(computeTbServe(2, startedByMe = true)).isEqualTo(Pair(false, true))
        // Point 3: starter serves from left
        assertThat(computeTbServe(3, startedByMe = true)).isEqualTo(Pair(true, false))
        // Point 4: starter serves from right
        assertThat(computeTbServe(4, startedByMe = true)).isEqualTo(Pair(true, true))
        // Point 5: other serves from left
        assertThat(computeTbServe(5, startedByMe = true)).isEqualTo(Pair(false, false))
        // Point 6: other serves from right
        assertThat(computeTbServe(6, startedByMe = true)).isEqualTo(Pair(false, true))
    }
}
