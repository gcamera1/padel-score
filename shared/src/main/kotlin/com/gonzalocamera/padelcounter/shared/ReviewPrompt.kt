package com.gonzalocamera.padelcounter.shared

/**
 * Estado del pedido de calificación. Se persiste en las prefs del teléfono.
 *
 * - [NEVER_ASKED]: todavía no se mostró el modal.
 * - [SNOOZED]: el usuario eligió "Más tarde" — puede volver a aparecer.
 * - [DISMISSED]: eligió "No, gracias" — no vuelve a aparecer nunca.
 * - [RATED]: fue a la ficha de Play Store (por el modal o por Ajustes) — no vuelve a aparecer.
 */
enum class ReviewPromptStatus { NEVER_ASKED, SNOOZED, DISMISSED, RATED }

/**
 * Señal de uso que suma puntos hacia el pedido de calificación.
 *
 * Los pesos miden **cuánto valor vivió el usuario**, no cuánto tocó la app: que un
 * partido llegue solo desde el reloj vale más que cargarlo a mano, y compartir un
 * resultado vale más que todo lo demás (quien lo comparte ya está recomendando la app).
 * Abrir la app no puntúa: abrir no es valor.
 */
enum class ReviewSignal(val points: Int) {
    WEAR_MATCH_SYNCED(3),
    PHONE_MATCH_FINISHED(2),
    MANUAL_MATCH_ADDED(1),
    MATCH_SHARED(4),
    STATS_VIEWED(1),
}

data class ReviewPromptState(
    val status: ReviewPromptStatus = ReviewPromptStatus.NEVER_ASKED,
    val score: Int = 0,
    /** Primera vez que se inicializó el estado — no es la fecha de instalación real. */
    val firstSeenAt: Long = 0L,
    val lastPromptAt: Long = 0L,
    val scoreAtLastPrompt: Int = 0,
    val promptCount: Int = 0,
    /** Día (epoch/24h) en que STATS_VIEWED puntuó por última vez. */
    val lastStatsSignalDay: Long = 0L,
)

/**
 * Decide **si** corresponde pedir la calificación. El **cuándo** (en qué pantalla y con
 * cuánto delay) lo resuelve la UI: esta política solo mira el uso acumulado.
 *
 * Puro y sin dependencias de Android para que se pueda testear en la JVM.
 */
object ReviewPolicy {
    /** Puntos necesarios: ~2 partidos del reloj, o 1 compartido + una visita a stats. */
    const val SCORE_THRESHOLD = 5

    /** Días desde el primer arranque: evita pedirle nada a quien recién instaló. */
    const val MIN_DAYS_INSTALLED = 3

    /** El historial vacío no da contexto para calificar. */
    const val MIN_MATCHES = 2

    /** Espera mínima tras un "Más tarde". */
    const val SNOOZE_DAYS = 10

    /** Además de esperar, tiene que haber seguido usando la app. */
    const val SNOOZE_SCORE_DELTA = 4

    /** Techo duro: tres pedidos en la vida de la instalación y listo. */
    const val MAX_PROMPTS = 3

    const val DAY_MS = 24L * 60 * 60 * 1000

    fun shouldPrompt(state: ReviewPromptState, matchCount: Int, now: Long): Boolean {
        if (state.status == ReviewPromptStatus.RATED) return false
        if (state.status == ReviewPromptStatus.DISMISSED) return false
        if (state.promptCount >= MAX_PROMPTS) return false
        if (matchCount < MIN_MATCHES) return false
        if (state.score < SCORE_THRESHOLD) return false
        // firstSeenAt == 0 significa "todavía no inicializado": sin ancla temporal no se
        // puede saber si es un usuario nuevo, así que no se pide.
        if (state.firstSeenAt <= 0L) return false
        if (now - state.firstSeenAt < MIN_DAYS_INSTALLED * DAY_MS) return false
        if (state.status == ReviewPromptStatus.SNOOZED) {
            if (now - state.lastPromptAt < SNOOZE_DAYS * DAY_MS) return false
            if (state.score - state.scoreAtLastPrompt < SNOOZE_SCORE_DELTA) return false
        }
        return true
    }

    /**
     * Puntaje inicial de quien ya venía usando la app antes de que existiera esta función.
     * Un usuario con historial ya vivió el valor, así que arranca cerca del umbral en vez
     * de tener que volver a ganárselo; la guarda de [MIN_DAYS_INSTALLED] igual le da aire
     * de tres días antes del primer pedido.
     */
    fun seedScore(matchCount: Int): Int = minOf(matchCount, 3) * 2

    fun signalFor(origin: MatchOrigin): ReviewSignal = when (origin) {
        MatchOrigin.WEAR -> ReviewSignal.WEAR_MATCH_SYNCED
        MatchOrigin.MOBILE -> ReviewSignal.PHONE_MATCH_FINISHED
        MatchOrigin.MANUAL -> ReviewSignal.MANUAL_MATCH_ADDED
    }

    /** Día calendario aproximado, para topear STATS_VIEWED a una vez por día. */
    fun dayOf(now: Long): Long = now / DAY_MS

    /** "Más tarde": cuenta el pedido y exige tiempo + uso nuevo antes del siguiente. */
    fun snooze(state: ReviewPromptState, now: Long): ReviewPromptState = state.copy(
        status = ReviewPromptStatus.SNOOZED,
        lastPromptAt = now,
        scoreAtLastPrompt = state.score,
        promptCount = state.promptCount + 1,
    )

    /** "No, gracias": cierra el tema para siempre. */
    fun dismiss(state: ReviewPromptState, now: Long): ReviewPromptState = state.copy(
        status = ReviewPromptStatus.DISMISSED,
        lastPromptAt = now,
        promptCount = state.promptCount + 1,
    )

    /** "Calificar": se lo mandó a la ficha, no se le vuelve a pedir. */
    fun rated(state: ReviewPromptState, now: Long): ReviewPromptState = state.copy(
        status = ReviewPromptStatus.RATED,
        lastPromptAt = now,
        promptCount = state.promptCount + 1,
    )
}
