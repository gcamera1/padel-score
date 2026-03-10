package com.gonzalocamera.padelcounter.presentation

/**
 * Scoring:
 * - Game normal: 0/15/30/40 + (AD si goldenPoint=false)
 * - Golden point: en 40-40 el próximo punto gana el game (sin AD)
 * - Tie-break: se activa SOLO cuando games llegan a 6-6, y se juega a 7 o 10 según [Decider]
 *
 * Regla pedida: la única manera de tocar el “puntaje del otro” es si el otro estaba en AD
 * y yo hago punto (vuelve a Deuce = 40-40).
 */

private fun tbTarget(decider: Decider): Int = when (decider) {
    Decider.TB7 -> 7
    Decider.SUPER10 -> 10
}

/**
 * Compute who serves and from which side in a tie-break.
 * Pattern: first server gets 1 serve, then alternating 2 serves each.
 * Side: even total → right (deuce), odd total → left (ad).
 */
internal fun computeTbServe(totalTbPoints: Int, startedByMe: Boolean): Pair<Boolean, Boolean> {
    val serverIsStarter = ((totalTbPoints + 1) / 2) % 2 == 0
    val myServe = if (serverIsStarter) startedByMe else !startedByMe
    val serveFromRight = totalTbPoints % 2 == 0
    return Pair(myServe, serveFromRight)
}

fun pointsLabel(state: PadelState, isMe: Boolean): String {
    return if (state.inTieBreak) {
        val p = if (isMe) state.myTbPoints else state.oppTbPoints
        p.toString()
    } else {
        val idx = if (isMe) state.myPointsIdx else state.oppPointsIdx
        when (idx) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            3 -> "40"
            else -> "AD"
        }
    }
}

fun addPointToMy(state: PadelState): PadelState =
    if (state.inTieBreak) addTbPoint(state, me = true) else addNormalPoint(state, me = true)

fun addPointToOpp(state: PadelState): PadelState =
    if (state.inTieBreak) addTbPoint(state, me = false) else addNormalPoint(state, me = false)

fun subtractPointFromMy(state: PadelState): PadelState {
    return if (state.inTieBreak) {
        if (state.myTbPoints == 0 && state.oppTbPoints == 0) {
            subtractGame(state, me = true)
        } else {
            val newMyTb = (state.myTbPoints - 1).coerceAtLeast(0)
            val (myServe, fromRight) = computeTbServe(
                newMyTb + state.oppTbPoints, state.tieBreakStartedByMe
            )
            state.copy(myTbPoints = newMyTb, myServe = myServe, serveFromRight = fromRight)
        }
    } else {
        if (state.myPointsIdx == 0 && state.oppPointsIdx == 0) {
            subtractGame(state, me = true)
        } else {
            state.copy(
                myPointsIdx = decIdx(state.myPointsIdx),
                serveFromRight = !state.serveFromRight
            )
        }
    }
}

fun subtractPointFromOpp(state: PadelState): PadelState {
    return if (state.inTieBreak) {
        if (state.myTbPoints == 0 && state.oppTbPoints == 0) {
            subtractGame(state, me = false)
        } else {
            val newOppTb = (state.oppTbPoints - 1).coerceAtLeast(0)
            val (myServe, fromRight) = computeTbServe(
                state.myTbPoints + newOppTb, state.tieBreakStartedByMe
            )
            state.copy(oppTbPoints = newOppTb, myServe = myServe, serveFromRight = fromRight)
        }
    } else {
        if (state.myPointsIdx == 0 && state.oppPointsIdx == 0) {
            subtractGame(state, me = false)
        } else {
            state.copy(
                oppPointsIdx = decIdx(state.oppPointsIdx),
                serveFromRight = !state.serveFromRight
            )
        }
    }
}

private fun subtractGame(state: PadelState, me: Boolean): PadelState {
    val games = if (me) state.myGames else state.oppGames
    if (games <= 0) return state
    // Si estamos en tie-break (6-6) y descuento un game, salimos del tie-break
    val newState = if (state.inTieBreak) {
        state.copy(inTieBreak = false, myTbPoints = 0, oppTbPoints = 0)
    } else {
        state
    }
    val s = if (me) newState.copy(myGames = games - 1) else newState.copy(oppGames = games - 1)
    return s.copy(myServe = !state.myServe, serveFromRight = true)
}

private fun decIdx(idx: Int): Int = when {
    idx <= 0 -> 0
    idx == 4 -> 3 // AD -> 40
    else -> idx - 1
}

private fun addNormalPoint(state: PadelState, me: Boolean): PadelState {
    val myIdx = if (me) state.myPointsIdx else state.oppPointsIdx
    val oppIdx = if (me) state.oppPointsIdx else state.myPointsIdx
    val flipServe = !state.serveFromRight

    // Golden point (sin AD)
    if (state.goldenPoint) {
        if (myIdx >= 3 && oppIdx < 3) return winGame(state, me)
        if (myIdx >= 3 && oppIdx >= 3) return winGame(state, me)
        val nextMy = (myIdx + 1).coerceAtMost(3)
        val s = if (me) state.copy(myPointsIdx = nextMy) else state.copy(oppPointsIdx = nextMy)
        return s.copy(serveFromRight = flipServe)
    }

    // Con AD
    return when {
        myIdx == 3 && oppIdx < 3 -> winGame(state, me)

        myIdx == 3 && oppIdx == 3 -> {
            val s = if (me) state.copy(myPointsIdx = 4) else state.copy(oppPointsIdx = 4)
            s.copy(serveFromRight = flipServe)
        }

        myIdx == 4 -> winGame(state, me)

        oppIdx == 4 -> state.copy(myPointsIdx = 3, oppPointsIdx = 3, serveFromRight = flipServe)

        else -> {
            val nextMy = (myIdx + 1).coerceAtMost(3)
            val s = if (me) state.copy(myPointsIdx = nextMy) else state.copy(oppPointsIdx = nextMy)
            s.copy(serveFromRight = flipServe)
        }
    }
}

private fun addTbPoint(state: PadelState, me: Boolean): PadelState {
    val target = tbTarget(state.decider)

    val my = if (me) state.myTbPoints + 1 else state.myTbPoints
    val opp = if (!me) state.oppTbPoints + 1 else state.oppTbPoints

    val updated = if (me) state.copy(myTbPoints = my) else state.copy(oppTbPoints = opp)

    val iWin = if (me) (my >= target && my - opp >= 2) else (opp >= target && opp - my >= 2)
    if (iWin) return winTieBreakAndSet(updated, me)

    val (myServe, fromRight) = computeTbServe(my + opp, state.tieBreakStartedByMe)
    return updated.copy(myServe = myServe, serveFromRight = fromRight)
}

private fun winGame(state: PadelState, me: Boolean): PadelState {
    val newMyGames = if (me) state.myGames + 1 else state.myGames
    val newOppGames = if (!me) state.oppGames + 1 else state.oppGames

    // Reset puntos del game + toggle server + reset serve side
    var s = state.copy(
        myGames = newMyGames,
        oppGames = newOppGames,
        myPointsIdx = 0,
        oppPointsIdx = 0,
        myServe = !state.myServe,
        serveFromRight = true
    )

    // Entrar a tie-break SOLO en 6-6
    if (!s.inTieBreak && s.myGames == 6 && s.oppGames == 6) {
        s = s.copy(
            inTieBreak = true,
            myTbPoints = 0,
            oppTbPoints = 0,
            tieBreakStartedByMe = s.myServe
        )
        return s
    }

    return checkSetWin(s)
}

private fun winTieBreakAndSet(state: PadelState, me: Boolean): PadelState {
    // Venimos de 6-6. El que gana queda 7-6.
    // Next set: the player who didn't start the TB serves first.
    val s = if (me) {
        state.copy(myGames = 7, oppGames = 6)
    } else {
        state.copy(myGames = 6, oppGames = 7)
    }.copy(
        inTieBreak = false,
        myTbPoints = 0,
        oppTbPoints = 0,
        myPointsIdx = 0,
        oppPointsIdx = 0,
        myServe = !state.tieBreakStartedByMe,
        serveFromRight = true
    )

    return winSet(s, me)
}

private fun checkSetWin(state: PadelState): PadelState {
    val my = state.myGames
    val opp = state.oppGames

    val myWon = (my >= 6 && my - opp >= 2) || (my == 7 && (opp == 5 || opp == 6))
    val oppWon = (opp >= 6 && opp - my >= 2) || (opp == 7 && (my == 5 || my == 6))

    return when {
        myWon -> winSet(state, me = true)
        oppWon -> winSet(state, me = false)
        else -> state
    }
}

private fun winSet(state: PadelState, me: Boolean): PadelState {
    val newMySets = if (me) state.mySets + 1 else state.mySets
    val newOppSets = if (!me) state.oppSets + 1 else state.oppSets

    return state.copy(
        mySets = newMySets,
        oppSets = newOppSets,
        myGames = 0,
        oppGames = 0,
        myPointsIdx = 0,
        oppPointsIdx = 0,
        myTbPoints = 0,
        oppTbPoints = 0,
        inTieBreak = false
    )
}