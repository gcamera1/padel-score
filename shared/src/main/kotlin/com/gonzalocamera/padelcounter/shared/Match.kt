package com.gonzalocamera.padelcounter.shared

import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long,
    val setsScore: List<List<Int>>,
    val tieBreakUsed: Boolean,
    val decider: Decider,
    val goldenPoint: Boolean = false,
    val scoringMode: ScoringMode = ScoringMode.DEUCE,
    val winner: Winner,
    val origin: MatchOrigin,
    val bestOf: Int = 3,
    val strokesPerSet: List<Int>? = null
)

/**
 * Id **estable** derivado del contenido del partido, para que reenviar el mismo partido
 * produzca siempre el mismo id.
 *
 * Con `UUID.randomUUID()` cada reenvío generaba un id nuevo, y eso desarmaba las dos
 * defensas contra duplicados: el `DataClient` publica en `/padel-score/match/{id}`, así que
 * un id nuevo es un `DataItem` nuevo, y el `INSERT OR IGNORE` del teléfono solo protege
 * contra reimportar el **mismo** id.
 *
 * `startedAt` ya es único por sí solo (se escribe con la hora del reloj al empezar cada
 * partido); el marcador se agrega para que el id sea legible al depurar. El resultado solo
 * usa dígitos y guiones, que son válidos como segmento de path del `DataClient`.
 */
fun matchId(startedAt: Long, setsScore: List<List<Int>>): String =
    if (setsScore.isEmpty()) "$startedAt"
    else "$startedAt-" + setsScore.joinToString("-") { "${it[0]}${it[1]}" }
