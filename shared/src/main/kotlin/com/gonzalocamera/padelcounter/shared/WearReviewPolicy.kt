package com.gonzalocamera.padelcounter.shared

/**
 * Estado del pedido de calificación **en el reloj**.
 *
 * Vive solo en el reloj y no se sincroniza con el teléfono: alguien que ya calificó desde el
 * teléfono puede recibir la invitación acá. Es el precio de que el reloj no dependa de la app
 * de teléfono, que es justamente el punto de esta funcionalidad.
 */
data class WearReviewState(
    val finishedMatches: Int = 0,
    val promptsShown: Int = 0,
    val rated: Boolean = false,
)

/**
 * Decide si corresponde invitar a calificar desde el reloj.
 *
 * Deliberadamente más simple que [ReviewPolicy], la del teléfono: allá hay varias señales que
 * puntúan (compartir, ver estadísticas, el origen del partido) y una ventana de snooze; acá la
 * única señal disponible es haber terminado partidos, y la pantalla no da para más.
 */
object WearReviewPolicy {

    /** Partidos terminados antes de la primera invitación. */
    const val MIN_MATCHES = 3

    /** Tope de invitaciones automáticas. Después solo queda la opción de Ajustes. */
    const val MAX_PROMPTS = 2

    /**
     * @param matchInProgress hay un partido **en juego**: saque elegido y todavía sin
     *   terminar. Un partido terminado sin cerrar NO cuenta: la pantalla de resumen es un
     *   buen momento para invitar, y como es donde queda el usuario después de su último
     *   partido, excluirla dejaba la invitación prácticamente inalcanzable.
     */
    fun shouldPrompt(state: WearReviewState, matchInProgress: Boolean): Boolean = when {
        state.rated -> false
        matchInProgress -> false
        state.promptsShown >= MAX_PROMPTS -> false
        else -> state.finishedMatches >= MIN_MATCHES
    }
}
