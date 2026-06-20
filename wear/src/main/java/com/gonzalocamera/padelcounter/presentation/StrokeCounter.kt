package com.gonzalocamera.padelcounter.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Acumulado de golpes del partido en curso, agrupado por set, mantenido en memoria.
 *
 * Vive como singleton porque el [StrokeCounterService] (que escribe) y el Activity (que
 * lee al finalizar el partido) corren en el mismo proceso: leer de memoria evita races y
 * binding. El DataStore (ver [PadelRepository.writeStrokeBackup]) es solo respaldo para
 * sobrevivir a la muerte del proceso.
 */
object StrokeCounter {

    private val _perSet = MutableStateFlow<List<Int>>(emptyList())
    val perSet: StateFlow<List<Int>> = _perSet

    /** Suma 1 golpe al set [setIdx] (0-based), creciendo la lista con ceros si hace falta. */
    fun recordStroke(setIdx: Int) {
        if (setIdx < 0) return
        _perSet.update { current ->
            val list = current.toMutableList()
            while (list.size <= setIdx) list.add(0)
            list[setIdx] = list[setIdx] + 1
            list
        }
    }

    fun snapshot(): List<Int> = _perSet.value

    fun restore(list: List<Int>) {
        _perSet.value = list
    }

    fun reset() {
        _perSet.value = emptyList()
    }
}
