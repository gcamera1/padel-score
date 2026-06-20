package com.gonzalocamera.padelcounter.shared

enum class Decider { TB7, SUPER10 }
enum class CourtColorOption { BLUE, ORANGE, GREEN, PURPLE }
enum class Winner { MY, OPP }
enum class MatchOrigin { WEAR, MOBILE }
enum class ScoringMode { DEUCE, GOLDEN_POINT, STAR_POINT }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class StrokeSensitivity { LOW, MEDIUM, HIGH }

/**
 * Categoría de juego — calibra los umbrales del veredicto de golpes (PGG individual).
 * `b1/b2/b3` son los bordes de banda: Heladera/Normal, Normal/Alto, Alto/Maratón.
 * `shotsPerPoint` = golpes promedio por punto entre los 4 jugadores (lo usa la calculadora).
 * 7ma y 6ta vienen del FDD validado; 5ta extrapolada sobre la misma escalera.
 */
enum class PadelCategory(
    val b1: Float,
    val b2: Float,
    val b3: Float,
    val shotsPerPoint: Float,
) {
    SEPTIMA(4.2f, 6.5f, 8.5f, 4.3f),
    SEXTA(6.5f, 11.0f, 14.5f, 6.25f),   // default
    QUINTA(11.0f, 15.5f, 19.0f, 7.5f);
}

/** Diagnóstico de volumen individual de golpes para un partido o set. */
enum class StrokeVerdict { FRIDGE, NORMAL, HIGH_LOAD, MARATHON }

/** Intensidad/duración de los puntos → puntos por game (para la calculadora). */
enum class PointIntensity(val pointsPerGame: Float) {
    LOW(4.5f),
    MEDIUM(5.5f),   // default
    HIGH(7.0f),
}
