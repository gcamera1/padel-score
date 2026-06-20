# SPEC — Calculadora de Golpes (Mobile)

**PRD:** [`docs/prd/stroke-calculator-mobile.md`](../prd/stroke-calculator-mobile.md)
**Módulos:** `:shared`, `:mobile`
**Estado:** Final — listo para implementar
**Fecha:** 2026-06-20

---

## 1. Resumen técnico

Pantalla nueva, **volátil y sin estado persistente**: solo `remember` local para los 4 controles y un cálculo puro de `:shared`. No usa ViewModel, repositorio, Room ni DataStore. Reutiliza `PadelCategory.verdict()` y `StrokeVerdictBadge` ya existentes. Se accede desde un botón en Ajustes vía una ruta nueva de Navigation Compose.

---

## 2. Mapa de cambios

### `:shared` (puro)
| Archivo | Cambio |
|---|---|
| `Enums.kt` | + `enum class PointIntensity(val pointsPerGame: Float)`; `PadelCategory` + propiedad `shotsPerPoint`; **actualizar umbrales de `SEPTIMA`** a `(4.2, 6.5, 8.5)` |
| `StrokeStats.kt` | + `StrokeEstimate` + `estimateStrokes(games, intensity, involvement, category)` |

> ⚠️ Cambiar `SEPTIMA` afecta también la etapa 2 (los partidos reales en 7ma se re-diagnostican al leer, sin migración).

### `:mobile`
| Archivo | Cambio |
|---|---|
| `ui/calculator/CalculatorScreen.kt` *(nuevo)* | Pantalla del simulador (controles + salida reactiva) |
| `ui/navigation/NavGraph.kt` | + ruta `composable("calculator")` + `onOpenCalculator` en el `composable("settings")` |
| `ui/settings/SettingsScreen.kt` | + botón "Calculadora de golpes" (callback `onOpenCalculator`) |

> Sin cambios en ViewModels, `ViewModelFactory`, Room, DataStore ni `:wear`.

---

## 3. `:shared` — lógica pura

### 3.1 `Enums.kt`
```kotlin
/** Intensidad/duración de los puntos → puntos por game (para el simulador). */
enum class PointIntensity(val pointsPerGame: Float) {
    LOW(4.5f),
    MEDIUM(5.5f),   // default
    HIGH(7.0f),
}

// PadelCategory: + propiedad shotsPerPoint (golpes/punto entre los 4 jugadores, para la calculadora)
// y umbrales de SEPTIMA actualizados al FDD:
enum class PadelCategory(val b1: Float, val b2: Float, val b3: Float, val shotsPerPoint: Float) {
    SEPTIMA(4.2f, 6.5f, 8.5f, 4.3f),    // ← umbrales actualizados (eran 2.0/6.5/10.0)
    SEXTA(6.5f, 11.0f, 14.5f, 6.25f),
    QUINTA(11.0f, 15.5f, 19.0f, 7.5f);  // shotsPerPoint extrapolado (FDD no lo da)
}
```

### 3.2 `StrokeStats.kt` (extender)
```kotlin
data class StrokeEstimate(
    val totalStrokes: Int,
    val pgg: Float,
)

/**
 * Estimación manual de golpes (calculadora). Pura.
 * @param involvement fracción 0..1 (ej. 0.25 = 25%).
 *
 * Nota: el PGG no depende de [games] (se cancela); games solo escala el total.
 */
fun estimateStrokes(
    games: Int,
    intensity: PointIntensity,
    involvement: Float,
    category: PadelCategory,
): StrokeEstimate {
    val totalPoints = games * intensity.pointsPerGame
    val totalShots = totalPoints * category.shotsPerPoint
    val yourStrokes = (totalShots * involvement).roundToInt()
    val pgg = if (games > 0) yourStrokes.toFloat() / games else 0f
    return StrokeEstimate(yourStrokes, pgg)
}
```
> `roundToInt()` ya se importa en `:shared` (lo usa `AggregateStats`). El veredicto se obtiene aparte con `category.verdict(estimate.pgg)` — no se duplica lógica.

---

## 4. `:mobile` — UI

### 4.1 `CalculatorScreen.kt` (nuevo)
Composable stateless con estado local. Estructura:
```kotlin
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var games by remember { mutableIntStateOf(18) }
    var intensity by remember { mutableStateOf(PointIntensity.MEDIUM) }
    var involvement by remember { mutableFloatStateOf(0.25f) }
    var category by remember { mutableStateOf(PadelCategory.SEXTA) }

    val estimate = estimateStrokes(games, intensity, involvement, category)
    val verdict = category.verdict(estimate.pgg)
    // Scaffold con PadelTopAppBar(title = "Calculadora", onBack = onBack)
    // Column scrollable:
    //   - Card resultado: estimate.totalStrokes (grande) + "golpes estimados"
    //       + "PGG ${"%.1f".format(estimate.pgg)}" + StrokeVerdictBadge(verdict)
    //       + verdictCopy(verdict)  ← texto largo de feedback
    //   - "Games totales (N)" + Slider(value=games, 12f..36f, steps=23, onValueChange→round)
    //   - "Intensidad de puntos" + SingleChoiceSegmentedButtonRow (Baja/Media/Alta)
    //   - "Tu involucramiento" + SingleChoiceSegmentedButtonRow (15%/25%/35%)
    //   - "Categoría" + SingleChoiceSegmentedButtonRow (7ma/6ta/5ta)
}

/** Copy de feedback por veredicto (calculadora, estilo FDD, agnóstico de categoría). */
private fun verdictCopy(v: StrokeVerdict): String = when (v) {
    StrokeVerdict.FRIDGE -> "O el partido se liquidó rápido, o te encerraron en el fondo y le cargaron el juego a tu compañero. ¡A practicar la paciencia!"
    StrokeVerdict.NORMAL -> "Juego bien distribuido. Participación fluida, sin desgastarte de más. ¡Buen ritmo!"
    StrokeVerdict.HIGH_LOAD -> "Te buscaron un montón o los puntos se hicieron largos. Tocó ponerse el overol y laburar cada pelota."
    StrokeVerdict.MARATHON -> "Volumen altísimo. Puro desgaste trayendo todo desde el fondo. Poné el brazo en hielo."
}
```
- Reusa `PadelTopAppBar`, `SectionHeader`, `StrokeVerdictBadge`, `SingleChoiceSegmentedButtonRow`/`SegmentedButton` (ya usados en Settings), patrón de Material3.
- Labels en español: intensidad `Baja/Media/Alta`, involucramiento `15%/25%/35%`, categoría `7ma/6ta/5ta` (helper `categoryLabel`, igual que en Settings — extraer a un sitio común o duplicar el `when`).
- El `Slider` de games: `value = games.toFloat()`, `valueRange = 12f..36f`, `steps = 23` (24 valores enteros), `onValueChange = { games = it.roundToInt() }`.

### 4.2 `NavGraph.kt`
- Nueva ruta:
```kotlin
composable("calculator") {
    CalculatorScreen(onBack = { navController.popBackStack() })
}
```
- En `composable("settings")`, pasar el callback:
```kotlin
SettingsScreen(
    viewModel = vm,
    onOpenCalculator = { navController.navigate("calculator") },
)
```
> La ruta `"calculator"` no está en `navItems`, así que `showNavChrome` queda `false` y la pantalla se muestra sin la bottom nav (como `match_detail`). Correcto.

### 4.3 `SettingsScreen.kt`
- `SettingsScreen(viewModel, onOpenCalculator: () -> Unit = {})` propaga a `SettingsContent`.
- En `SettingsContent`, nueva sección al final (antes del `Spacer` de cierre):
```kotlin
Column {
    SectionHeader("HERRAMIENTAS")
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onOpenCalculator, modifier = Modifier.fillMaxWidth()) {
        Text("Calculadora de golpes")
    }
}
```

---

## 5. Plan de tests

### `:shared` — extender `StrokeStatsTest` (o `StrokeEstimateTest` nuevo)
- `estimateStrokes(18, MEDIUM, 0.25f, SEXTA)` → totalStrokes ≈ 155, pgg ≈ 8.6 (within 0.1).
- PGG **independiente de games**: `estimateStrokes(12, …, SEXTA).pgg == estimateStrokes(36, …, SEXTA).pgg`.
- Intensidad sube el PGG: `LOW.pgg < MEDIUM.pgg < HIGH.pgg` (mismo games/involucramiento/categoría).
- Involucramiento sube el total y el PGG proporcionalmente.
- `shotsPerPoint` por categoría: con mismos inputs, `SEPTIMA.totalStrokes < SEXTA.totalStrokes < QUINTA.totalStrokes`.
- Veredicto coherente: `SEXTA.verdict(estimateStrokes(18, MEDIUM, 0.25f, SEXTA).pgg) == NORMAL`.
- **Umbrales 7ma actualizados:** `SEPTIMA.verdict(4.1f) == FRIDGE`, `verdict(6.5f) == NORMAL`, `verdict(8.5f) == HIGH_LOAD`, `verdict(8.6f) == MARATHON`.
- `games = 0` → pgg 0, sin crash (defensivo; la UI nunca manda < 12).

### Screenshot (Paparazzi, opcional)
- `CalculatorScreen` con defaults (extraer un `CalculatorContent` stateless para snapshotear sin navegación).

---

## 6. Orden de implementación (bloques)

1. **Bloque 1 — `:shared`**: `PointIntensity` + `estimateStrokes` + `AVG_SHOTS_PER_POINT`. + tests. *(Verde sin Android.)*
2. **Bloque 2 — UI + navegación**: `CalculatorScreen`, ruta en `NavGraph`, botón en `SettingsScreen`.

> Feature chica y aislada; dos bloques alcanzan.

## 7. Riesgos técnicos

| Riesgo | Mitigación |
|---|---|
| `GOLPES_POR_PUNTO = 6.5` no representa todas las categorías | Constante central, fácil de ajustar; la categoría ya reinterpreta el PGG vía umbrales |
| Usuario confundido porque games no cambia el veredicto | Es por diseño (documentado); el total sí cambia, dando feedback visible |
| Slider con `steps` mal calculado | 24 valores enteros (12..36) → `steps = 23`; cubrir en prueba manual |
