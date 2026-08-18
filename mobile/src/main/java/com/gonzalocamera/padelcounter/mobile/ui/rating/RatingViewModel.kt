package com.gonzalocamera.padelcounter.mobile.ui.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonzalocamera.padelcounter.mobile.data.MatchRepository
import com.gonzalocamera.padelcounter.shared.ReviewPolicy
import com.gonzalocamera.padelcounter.shared.ReviewPromptState
import com.gonzalocamera.padelcounter.shared.ReviewSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Dueño del pedido de calificación. Vive a nivel del `NavGraph` (una sola instancia por
 * Activity) para que el modal se renderice en un único lugar y nunca se apilen dos.
 *
 * El **si** corresponde pedir lo decide [ReviewPolicy]; acá está el **cuándo**: los
 * "momentos de valor" que la navegación reporta ([onStatsViewed], [onMatchDetailViewed]).
 * Nunca se evalúa en el arranque en frío: ahí el usuario todavía no vio nada.
 */
class RatingViewModel(private val repository: MatchRepository) : ViewModel() {

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedReviewPrompt(System.currentTimeMillis())
        }
    }

    /** Momento de valor: vio sus estadísticas. Suma un punto (máx. 1 por día). */
    fun onStatsViewed() = evaluate(ReviewSignal.STATS_VIEWED)

    /** Momento de valor: se quedó mirando el detalle de un partido. */
    fun onMatchDetailViewed() = evaluate(signal = null)

    /**
     * Compartió un resultado — la señal más fuerte, pero **no** abre el modal: el usuario
     * se está yendo al selector de compartir y pisarlo con un diálogo sería intrusivo.
     * Los puntos quedan para el próximo momento de valor.
     */
    fun onMatchShared() {
        viewModelScope.launch {
            repository.recordReviewSignal(ReviewSignal.MATCH_SHARED, System.currentTimeMillis())
        }
    }

    /** "Calificar" — la UI abre la ficha de Play Store; acá se cierra el tema. */
    fun onRate() = close { state, now -> ReviewPolicy.rated(state, now) }

    /** "Más tarde" — vuelve a aparecer con tiempo y uso nuevo. */
    fun onLater() = close { state, now -> ReviewPolicy.snooze(state, now) }

    /** "No, gracias" — no vuelve a aparecer. */
    fun onNever() = close { state, now -> ReviewPolicy.dismiss(state, now) }

    private fun evaluate(signal: ReviewSignal?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (signal != null) repository.recordReviewSignal(signal, now)
            val state = repository.reviewPromptState.first()
            val matchCount = repository.matchSummaries.first().size
            if (ReviewPolicy.shouldPrompt(state, matchCount, now)) _visible.value = true
        }
    }

    private fun close(transform: (ReviewPromptState, Long) -> ReviewPromptState) {
        _visible.value = false
        viewModelScope.launch {
            val state = repository.reviewPromptState.first()
            repository.saveReviewPromptState(transform(state, System.currentTimeMillis()))
        }
    }
}
