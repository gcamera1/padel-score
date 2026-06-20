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
 * Anclada en data real de 6ta; 7ma y 5ta extrapoladas sobre la misma escalera.
 */
enum class PadelCategory(val b1: Float, val b2: Float, val b3: Float) {
    SEPTIMA(2.0f, 6.5f, 10.0f),
    SEXTA(6.5f, 11.0f, 14.5f),   // default
    QUINTA(11.0f, 15.5f, 19.0f);
}

/** Diagnóstico de volumen individual de golpes para un partido o set. */
enum class StrokeVerdict { FRIDGE, NORMAL, HIGH_LOAD, MARATHON }
