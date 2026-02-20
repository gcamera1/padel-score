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
        state.copy(myTbPoints = (state.myTbPoints - 1).coerceAtLeast(0))
    } else {
        state.copy(myPointsIdx = decIdx(state.myPointsIdx))
    }
}

fun subtractPointFromOpp(state: PadelState): PadelState {
    return if (state.inTieBreak) {
        state.copy(oppTbPoints = (state.oppTbPoints - 1).coerceAtLeast(0))
    } else {
        state.copy(oppPointsIdx = decIdx(state.oppPointsIdx))
    }
}

private fun decIdx(idx: Int): Int = when {
    idx <= 0 -> 0
    idx == 4 -> 3 // AD -> 40
    else -> idx - 1
}

private fun addNormalPoint(state: PadelState, me: Boolean): PadelState {
    val myIdx = if (me) state.myPointsIdx else state.oppPointsIdx
    val oppIdx = if (me) state.oppPointsIdx else state.myPointsIdx

    // Golden point (sin AD)
    if (state.goldenPoint) {
        // Si estoy en 40 y el otro < 40 -> gano game
        if (myIdx >= 3 && oppIdx < 3) return winGame(state, me)
        // Si es 40-40 -> próximo punto gana game
        if (myIdx >= 3 && oppIdx >= 3) return winGame(state, me)
        // Caso normal
        val nextMy = (myIdx + 1).coerceAtMost(3)
        return if (me) state.copy(myPointsIdx = nextMy) else state.copy(oppPointsIdx = nextMy)
    }

    // Con AD
    return when {
        // Gano game desde 40 si el otro < 40
        myIdx == 3 && oppIdx < 3 -> winGame(state, me)

        // Deuce -> AD mío
        myIdx == 3 && oppIdx == 3 -> {
            if (me) state.copy(myPointsIdx = 4) else state.copy(oppPointsIdx = 4)
        }

        // Ya estaba en AD y marco -> gano game
        myIdx == 4 -> winGame(state, me)

        // El otro estaba en AD y yo marco -> vuelvo a Deuce (40-40)
        oppIdx == 4 -> state.copy(myPointsIdx = 3, oppPointsIdx = 3)

        // Caso normal: escalo
        else -> {
            val nextMy = (myIdx + 1).coerceAtMost(3)
            if (me) state.copy(myPointsIdx = nextMy) else state.copy(oppPointsIdx = nextMy)
        }
    }
}

private fun addTbPoint(state: PadelState, me: Boolean): PadelState {
    val target = tbTarget(state.decider)

    val my = if (me) state.myTbPoints + 1 else state.myTbPoints
    val opp = if (!me) state.oppTbPoints + 1 else state.oppTbPoints

    val updated = if (me) state.copy(myTbPoints = my) else state.copy(oppTbPoints = opp)

    val iWin = if (me) (my >= target && my - opp >= 2) else (opp >= target && opp - my >= 2)
    return if (iWin) winTieBreakAndSet(updated, me) else updated
}

private fun winGame(state: PadelState, me: Boolean): PadelState {
    val newMyGames = if (me) state.myGames + 1 else state.myGames
    val newOppGames = if (!me) state.oppGames + 1 else state.oppGames

    // Reset puntos del game
    var s = state.copy(
        myGames = newMyGames,
        oppGames = newOppGames,
        myPointsIdx = 0,
        oppPointsIdx = 0
    )

    // Entrar a tie-break SOLO en 6-6
    if (!s.inTieBreak && s.myGames == 6 && s.oppGames == 6) {
        s = s.copy(inTieBreak = true, myTbPoints = 0, oppTbPoints = 0)
        return s
    }

    return checkSetWin(s)
}

private fun winTieBreakAndSet(state: PadelState, me: Boolean): PadelState {
    // Venimos de 6-6. El que gana queda 7-6.
    val s = if (me) {
        state.copy(myGames = 7, oppGames = 6)
    } else {
        state.copy(myGames = 6, oppGames = 7)
    }.copy(
        inTieBreak = false,
        myTbPoints = 0,
        oppTbPoints = 0,
        myPointsIdx = 0,
        oppPointsIdx = 0
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