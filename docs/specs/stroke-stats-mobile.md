# SPEC — Estadísticas de Golpes (Mobile)

**PRD:** [`docs/prd/stroke-stats-mobile.md`](../prd/stroke-stats-mobile.md)
**Módulos:** `:shared`, `:mobile`
**Estado:** Final — listo para implementar
**Fecha:** 2026-06-20

---

## 1. Resumen técnico

El reloj ya manda `Match.strokesPerSet`. El celular hoy lo **decodifica pero lo descarta** al persistir (`MatchEntity` no tiene columna). Este feature:

1. **Cierra el hueco de persistencia**: columna nullable en `MatchEntity` + migración Room 3→4 + mapeo en `toEntity()`/`toMatch()`.
2. **Agrega lógica pura en `:shared`**: `PadelCategory` (umbrales por categoría), `StrokeVerdict`, y el cálculo `Match.strokeStats(category)` (PGG por set y total + veredictos) y `strokeAggregate(...)` para el histórico.
3. **Agrega un setting** de categoría en `UserPreferences`.
4. **Pinta dos secciones "GOLPES"**: en el detalle del partido (`MatchDetailScreen`) y en `StatsScreen`.

**Decisión clave:** el veredicto se **deriva en lectura** (no se persiste). Solo se persiste el dato crudo (`strokesPerSet`) + la categoría elegida. Cambiar de categoría recalcula todo sin tocar la base.

---

## 2. Mapa de cambios

### `:shared` (puro)
| Archivo | Cambio |
|---|---|
| `Enums.kt` | + `enum class PadelCategory` (con umbrales) y `enum class StrokeVerdict` |
| `StrokeStats.kt` *(nuevo)* | `PadelCategory.verdict(pgg)`, `data class SetStrokeStat` / `StrokeStats` / `StrokeAggregate`, `Match.strokeStats(category)`, `strokeAggregate(matches, category)` |

### `:mobile`
| Archivo | Cambio |
|---|---|
| `data/db/MatchEntity.kt` | + columna `strokesPerSetJson: String?`; mapearla en `toEntity()`/`toMatch()` |
| `data/db/PadelDatabase.kt` | version 3→4 + `MIGRATION_3_4` (`ADD COLUMN`) |
| `data/MobilePreferences.kt` | + `category` en `UserPreferences` + key + read/write |
| `ui/settings/SettingsScreen.kt` | + selector de categoría (`SingleChoiceSegmentedButtonRow`, igual que el de tema) |
| `ui/history/MatchDetailScreen.kt` | + sección "GOLPES" en `MatchDetailContent` (recibe `category`) |
| `ui/history/HistoryViewModel.kt` | exponer `category` (desde `userPreferences`) para el detalle |
| `ui/stats/StatsScreen.kt` | + sección "GOLPES" (3 tiles) |
| `ui/stats/StatsViewModel.kt` | + flow `strokeAggregate` combinando historial + categoría |
| `ui/components/` | + `StrokeVerdictBadge` (badge emoji+copy), reutilizable en detalle y stats |

> Sin cambios en `:wear`.

---

## 3. `:shared` — lógica pura

### 3.1 `Enums.kt`
```kotlin
/** Categoría de juego — calibra los umbrales del veredicto de golpes (PGG). */
enum class PadelCategory(val b1: Float, val b2: Float, val b3: Float) {
    SEPTIMA(2.0f, 6.5f, 10.0f),
    SEXTA(6.5f, 11.0f, 14.5f),   // default — única 100% anclada en data real
    QUINTA(11.0f, 15.5f, 19.0f);
}

/** Diagnóstico de volumen individual de golpes. */
enum class StrokeVerdict { FRIDGE, NORMAL, HIGH_LOAD, MARATHON }
```
> Los `b1/b2/b3` son los bordes de banda (Heladera/Normal, Normal/Alto, Alto/Maratón). Las etiquetas en español ("7ma", "🧊 Heladera", etc.) viven en `:mobile` (convención: copy en UI, lógica en `:shared`).

### 3.2 `StrokeStats.kt` (nuevo)
```kotlin
package com.gonzalocamera.padelcounter.shared

/** Banda de veredicto para un PGG dado, según los umbrales de la categoría.
 *  Bordes (6ta): <6.5 Fridge · 6.5–11.0 Normal · 11.1–14.5 HighLoad · >14.5 Marathon. */
fun PadelCategory.verdict(pgg: Float): StrokeVerdict = when {
    pgg < b1  -> StrokeVerdict.FRIDGE
    pgg <= b2 -> StrokeVerdict.NORMAL
    pgg <= b3 -> StrokeVerdict.HIGH_LOAD
    else      -> StrokeVerdict.MARATHON
}

data class SetStrokeStat(
    val setIndex: Int,
    val strokes: Int,
    val games: Int,
    val pgg: Float,
    val verdict: StrokeVerdict,
)

data class StrokeStats(
    val perSet: List<SetStrokeStat>,
    val totalStrokes: Int,
    val totalGames: Int,
    val pgg: Float,
    val verdict: StrokeVerdict,
)

data class StrokeAggregate(
    val matchesWithData: Int,
    val totalStrokes: Int,
    val avgPgg: Float,
    val verdict: StrokeVerdict?,      // null si no hay datos
    val maxStrokesInMatch: Int,
)

private fun pggOf(strokes: Int, games: Int): Float =
    if (games > 0) strokes.toFloat() / games else 0f

/** Métricas de golpes del partido. `null` si no hay dato (`strokesPerSet == null`). */
fun Match.strokeStats(category: PadelCategory): StrokeStats? {
    val strokes = strokesPerSet ?: return null
    val perSet = strokes.mapIndexed { i, s ->
        val games = setsScore.getOrNull(i)?.let { it[0] + it[1] } ?: 0
        val pgg = pggOf(s, games)
        SetStrokeStat(i, s, games, pgg, category.verdict(pgg))
    }
    val totalStrokes = strokes.sum()
    val totalGames = setsScore.sumOf { it[0] + it[1] }
    val pgg = pggOf(totalStrokes, totalGames)
    return StrokeStats(perSet, totalStrokes, totalGames, pgg, category.verdict(pgg))
}

/** Agregado histórico sobre los partidos que tienen dato de golpes. */
fun strokeAggregate(matches: List<Match>, category: PadelCategory): StrokeAggregate {
    val withData = matches.filter { it.strokesPerSet != null }
    if (withData.isEmpty()) return StrokeAggregate(0, 0, 0f, null, 0)
    val totalStrokes = withData.sumOf { it.strokesPerSet!!.sum() }
    val totalGames = withData.sumOf { m -> m.setsScore.sumOf { it[0] + it[1] } }
    val avgPgg = pggOf(totalStrokes, totalGames)
    val maxInMatch = withData.maxOf { it.strokesPerSet!!.sum() }
    return StrokeAggregate(withData.size, totalStrokes, avgPgg, category.verdict(avgPgg), maxInMatch)
}
```
> `avgPgg` se calcula ponderado (suma de golpes ÷ suma de games de todos los partidos con dato), más robusto que promediar PGGs de partidos de distinto largo.

---

## 4. `:mobile` — persistencia

### 4.1 `MatchEntity.kt`
```kotlin
@Entity(tableName = "matches")
data class MatchEntity(
    // ... campos actuales ...
    val bestOf: Int = 3,
    val strokesPerSetJson: String? = null,   // ← nuevo, JSON de List<Int>, nullable
)

fun Match.toEntity(): MatchEntity = MatchEntity(
    // ... actuales ...
    bestOf = bestOf,
    strokesPerSetJson = strokesPerSet?.let { Json.encodeToString(it) },
)

fun MatchEntity.toMatch(): Match = Match(
    // ... actuales ...
    bestOf = bestOf,
    strokesPerSet = strokesPerSetJson?.let {
        runCatching { Json.decodeFromString<List<Int>>(it) }.getOrNull()
    },
)
```

### 4.2 `PadelDatabase.kt`
```kotlin
@Database(entities = [MatchEntity::class], version = 4, exportSchema = false)
// ...
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE matches ADD COLUMN strokesPerSetJson TEXT")  // nullable
    }
}
// .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
```

### 4.3 `MobilePreferences.kt`
- `UserPreferences` + `val category: PadelCategory = PadelCategory.SEXTA`.
- Nueva key `val CATEGORY = stringPreferencesKey("category")`.
- En `userPreferences`: `category = runCatching { PadelCategory.valueOf(prefs[Keys.CATEGORY] ?: "") }.getOrDefault(PadelCategory.SEXTA)`.
- En `savePreferences`: `p[Keys.CATEGORY] = prefs.category.name`.

---

## 5. `:mobile` — UI

### 5.1 `StrokeVerdictBadge.kt` (nuevo componente)
Mapea `StrokeVerdict` → emoji + copy (español), pinta un chip/badge con color de acento.
```kotlin
private fun StrokeVerdict.display(): Pair<String, String> = when (this) {
    StrokeVerdict.FRIDGE    -> "🧊" to "Heladera"
    StrokeVerdict.NORMAL    -> "⚖️" to "Normal"
    StrokeVerdict.HIGH_LOAD -> "🔨" to "Alto desgaste"
    StrokeVerdict.MARATHON  -> "🦸" to "Maratón"
}

private fun PadelCategory.label(): String = when (this) {
    PadelCategory.SEPTIMA -> "7ma"
    PadelCategory.SEXTA   -> "6ta"
    PadelCategory.QUINTA  -> "5ta"
}
```
Composable `StrokeVerdictBadge(verdict)`. (`label()` se usa en el selector de Ajustes.)

### 5.2 `SettingsScreen.kt` — selector de categoría
Reusar el patrón del selector de tema (`SingleChoiceSegmentedButtonRow` + `SegmentedButton`), bajo un `SectionHeader("CATEGORÍA")`. Wirea `viewModel::setCategory` (nuevo método en `SettingsViewModel` que actualiza `UserPreferences.category`).

### 5.3 `MatchDetailScreen.kt` — sección "GOLPES"
- `MatchDetailContent` pasa a recibir `category: PadelCategory` (default `SEXTA`). `MatchDetailScreen` lo toma del `viewModel` (ver 5.5); `InlineMatchDetailScaffold` también debe propagarlo.
- Tras la sección "DETALLES" (o entre POR SET y DETALLES), agregar:
```kotlin
match.strokeStats(category)?.let { s ->
    Column {
        SectionHeader("GOLPES")
        Spacer(Modifier.height(12.dp))
        // Total protagonista
        Text("${s.totalStrokes}", style = ...numeral...)
        Text("golpes en el partido", style = labelMedium)
        // Veredicto del partido
        StrokeVerdictBadge(s.verdict)   // + "PGG ${"%.1f".format(s.pgg)}"
        Spacer(...)
        // Por set
        s.perSet.forEach { set ->
            Row(SpaceBetween) {
                Text("Set ${set.setIndex + 1}")
                Text("${set.strokes} golpes · ${set.games} games · ${"%.1f".format(set.pgg)} PGG")
                StrokeVerdictBadge(set.verdict)
            }
        }
    }
}
```
> Si `strokeStats == null` (sin dato), no se renderiza nada (el `?.let` lo cubre). Opcional: mostrar texto tenue "Sin datos de golpes".

### 5.4 `StatsScreen.kt` — sección "GOLPES"
Nueva `Column` con `SectionHeader("GOLPES")`. Si `aggregate.matchesWithData == 0` → `EmptyState`/texto tenue ("Todavía no registraste partidos con golpes"). Si hay dato, 3 tiles (reusar `StatTile` / `LabeledNumber`):
- **Golpes acumulados** = `aggregate.totalStrokes`.
- **Intensidad típica** = `"%.1f".format(aggregate.avgPgg)` PGG + `StrokeVerdictBadge(aggregate.verdict!!)`.
- **Más maratónico** = `aggregate.maxStrokesInMatch` golpes.

### 5.5 ViewModels
- **`StatsViewModel`**: nuevo `StateFlow<StrokeAggregate>` combinando `repository.matchHistory` con `repository.userPreferences` (para la categoría):
```kotlin
val strokeStats: StateFlow<StrokeAggregate> =
    combine(repository.matchHistory, repository.userPreferences) { matches, prefs ->
        strokeAggregate(matches, prefs.category)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StrokeAggregate(0,0,0f,null,0))
```
  (Requiere exponer `matchHistory` en la interfaz `MatchRepository`, o derivar de `matchSummaries` — preferible exponer la categoría y reusar `aggregateStats`. Usar `matchHistory` que ya existe en `MobileRepository`.)
- **`HistoryViewModel`**: exponer `category` (último valor de `userPreferences.category`) para pasárselo a `MatchDetailContent`. Puede ser un `StateFlow<PadelCategory>` o leerse junto al `getMatchDetail`.

---

## 6. Plan de tests

### `:shared` (unitarios, sin emulador) — `StrokeStatsTest` (nuevo)
- `verdict()` respeta los bordes en 6ta: 6.4→Fridge, 6.5→Normal, 11.0→Normal, 11.1→HighLoad, 14.5→HighLoad, 14.6→Marathon.
- `verdict()` cambia con la categoría: PGG 7.0 → Normal en 6ta pero HighLoad en 7ma.
- `Match.strokeStats`: PGG total = total golpes / total games; por-set alinea índices.
- `strokeStats` con `strokesPerSet = null` → `null`.
- División por cero (games 0) → PGG 0, sin crash.
- `strokeAggregate`: filtra `null`, suma correcta, `maxStrokesInMatch` correcto; lista vacía → `matchesWithData == 0`, `verdict == null`.

### `:mobile` (unitarios)
- `MatchEntity` round-trip: `Match(strokesPerSet=[42,38]).toEntity().toMatch()` preserva la lista; `null` se preserva como `null`.
- (Si hay tests de migración) `MIGRATION_3_4` agrega la columna y conserva filas.

### Screenshot (Paparazzi, opcional)
- `MatchDetailContent` con sección GOLPES (un set Heladera, otro Maratón).
- `StatsContent` con y sin datos de golpes.

---

## 7. Orden de implementación (bloques)

1. **Bloque 1 — `:shared`**: `PadelCategory` + `StrokeVerdict` en `Enums.kt`, `StrokeStats.kt` (verdict + cálculos). + `StrokeStatsTest`. *(Verde sin Android.)*
2. **Bloque 2 — persistencia `:mobile`**: columna en `MatchEntity` + mapeo + `MIGRATION_3_4` + version 4. + test de round-trip. *(Cierra el hueco: el dato del reloj ya se guarda.)*
3. **Bloque 3 — categoría**: `UserPreferences.category` + key + selector en `SettingsScreen` + `SettingsViewModel.setCategory`.
4. **Bloque 4 — UI de golpes**: `StrokeVerdictBadge`, sección GOLPES en `MatchDetailScreen` (+ propagar `category`), sección GOLPES en `StatsScreen` (+ `StrokeAggregate` en `StatsViewModel`).

> Tras el Bloque 2 ya podés jugar mañana y el dato queda guardado aunque la UI todavía no lo muestre. Los bloques 3–4 le dan la cara visible.

## 8. Riesgos técnicos

| Riesgo | Mitigación |
|---|---|
| Migración Room mal hecha rompe la base | `ADD COLUMN` nullable, sin `DROP`; probar upgrade desde v3 |
| `strokesPerSet.size` ≠ `setsScore.size` (desalineación) | `getOrNull` por índice; games faltantes → 0, sin crash |
| Umbrales de 7ma/5ta extrapolados (no validados en cancha) | Valores centralizados en `PadelCategory`; ajuste trivial |
| Mostrar métricas en partidos sin dato | `strokeStats` devuelve `null` + filtro en agregado + empty-state |
| PGG distorsionado por sets con tie-break (13 games) | Aceptado en v1; el TB cuenta como games reales, error menor |
