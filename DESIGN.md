# Padel Score — Sistema de Diseño (App Mobile)

Guía de diseño del módulo `:mobile`. Estética **"Premier Padel": negro mate + oro
metálico**, dark-only, inspirada en la herramienta oficial de un circuito
profesional. Este documento describe los tokens, componentes y pantallas tal como
están implementados en Compose.

> Alcance: solo `:mobile`. El módulo `:wear` mantiene su propio diseño.

---

## 1. Principios

- **Dark-only premium.** Fondo negro mate, sin tema claro. El oro es el único
  acento de marca; el rojo se reserva para "EN VIVO".
- **El oro comunica lo destacado.** Victorias, valores clave y selecciones activas
  van en oro; lo neutro/perdido va en blanco o gris.
- **Tarjetas contenidas.** Todo bloque de información vive dentro de una card con
  borde dorado fino (`PremiumCard`). Nada "suelto" sobre el fondo.
- **Números en monoespaciada.** Marcadores y estadísticas usan JetBrains Mono para
  alineación tabular y carácter deportivo.

---

## 2. Color

Fuente única de verdad: `ui/theme/PadelColors.kt` → `object PadelPalette`.

| Token | Hex | Uso |
|---|---|---|
| `Gold` | `#C5A85A` | Acento de marca, bordes, selecciones, valores clave |
| `GoldLight` | `#E5C453` | Números destacados, victorias, hover |
| `GoldDark` | `#8A6F30` | Sombras/gradientes de oro |
| `Background` | `#0B0C0D` | Fondo de la app |
| `BackgroundDeep` | `#070809` | Fondo detrás de cards / borde del gradiente |
| `Card` | `#151719` | Superficie de tarjetas y sheets |
| `Gray` | `#222528` | Chips, divisores, botones inertes |
| `Text` | `#E2E5E8` | Texto primario |
| `TextMuted` | `#9CA3AF` | Texto secundario |
| `TextFaint` | `#6B7280` | Etiquetas, captions, iconos inactivos |
| `Live` | `#DC2626` | Indicador "EN VIVO" (único uso del rojo) |
| `DonutTrack` | `#1E2225` | Riel del donut |
| `Wear` | `#444A50` | Segmento gris ("perdidos" / "desgaste") |
| `Normal` | `#FFFFFF` | Segmento blanco en gráficos |

**Mapeo de dominio** (para no romper la semántica existente `accentMine/accentRival`):
`accentMine` → `GoldLight` (yo / victoria), `accentRival` → `Text` blanco (rival /
derrota). Las líneas de cancha se dibujan en oro al 28%.

**Esquema Material3** (`ui/theme/Theme.kt`, `darkColorScheme`): `primary = Gold`,
`onPrimary = #14110A`, `secondaryContainer = Gold` (rellena el segmented button /
chip activo con oro sólido), `background = Background`, `surface = Card`,
`surfaceVariant = Gray`, `error = Live`, `outline = Gold @25%`.

---

## 3. Tipografía

Google Fonts descargables (`ui/theme/Typography.kt`). Si el proveedor no está
disponible, Compose cae al sans del sistema automáticamente.

| Familia | Rol |
|---|---|
| **Outfit** (Medium→ExtraBold) | Display / títulos / botones |
| **Inter** (Normal→Bold) | Cuerpo / UI / etiquetas |
| **JetBrains Mono** (Medium→ExtraBold) | Marcadores y toda cifra |

### Escala Material3 (`PadelTypography`)

| Estilo | Familia | Peso | Tamaño/Línea | Tracking |
|---|---|---|---|---|
| `displayLarge` | Outfit | ExtraBold | 48/52 | -0.5 |
| `displayMedium` | Outfit | ExtraBold | 36/40 | -0.5 |
| `displaySmall` | Outfit | Bold | 26/30 | 0 |
| `headlineLarge` | Outfit | Bold | 24/28 | 0 |
| `headlineMedium` | Outfit | Bold | 20/24 | 0 |
| `titleLarge` | Outfit | Bold | 18/24 | 0 |
| `titleMedium` | Inter | SemiBold | 15/20 | 0.1 |
| `titleSmall` | Inter | SemiBold | 13/18 | 0.1 |
| `bodyLarge` | Inter | Normal | 15/22 | 0 |
| `bodyMedium` | Inter | Normal | 13/19 | 0 |
| `bodySmall` | Inter | Normal | 12/16 | 0 |
| `labelLarge` | Inter | Bold | 12 | 1.0 |
| `labelMedium` | Inter | SemiBold | 11 | 0.6 |
| `labelSmall` | Inter | SemiBold | 10 | 0.6 |

### Tipografía deportiva (`PadelSportTypography`)

| Token | Familia | Peso | Tamaño | Uso |
|---|---|---|---|---|
| `scoreNumeral` | JetBrains Mono | ExtraBold | 56 | Puntos en vivo, score hero |
| `setGameNumeral` | JetBrains Mono | Bold | 30 | Sets/games, valores de stats |
| `matchCardScore` | JetBrains Mono | Bold | 18 | Score en cards de historial / por set |
| `scoreLabel` | Inter | Bold | 10 (track 1.2) | "SETS", "GAMES", "YO"/"RIVAL" |
| `sectionHeader` | JetBrains Mono | Bold | 11 (track 1.4) | Eyebrows de sección, badges |

Todo texto numérico usa `fontFeatureSettings = "tnum"` (cifras tabulares).

---

## 4. Forma, espaciado y motion

**Shapes** (`ui/theme/Shapes.kt`): `extraSmall 6` · `small 12` · `medium 16`
(cards) · `large 20` · `extraLarge 28`. Pills/badges = `RoundedCornerShape(999)`.

**Spacing** (`ui/theme/Spacing.kt`): `xxs 4 · xs 8 · sm 12 · md 16 · lg 24 · xl 32 · xxl 48`.

**Motion** (`ui/theme/Motion.kt`): fast 120ms · medium 220ms · slow 400ms ·
celebration 700ms. Easings: `emphasized`, `standard`, `bounceOut`. El cambio de
puntaje entra con `scaleIn(0.78)+fadeIn` y sale con `scaleOut(1.12)+fadeOut`.

---

## 5. Componentes firma

### `PremiumCard` (`ui/components/PremiumCard.kt`)
La superficie base de todo el rediseño: fondo con gradiente `Card → Background`,
borde dorado fino (`hairline` = oro 20%, o `featured` = oro sólido) y esquinas
`medium` (16dp). Parámetro `grid = true` superpone una grilla dorada sutil (14dp,
5% alpha) usada en las cards hero.

### `DonutChart` (`ui/components/DonutChart.kt`)
Donut segmentado con arcos redondeados sobre un riel, y numeral + caption
centrados. Usado en Estadísticas (Victorias vs Derrotas).

### `GoldProgressBar` (`ui/components/GoldProgressBar.kt`)
Barra redondeada fina (riel `Background` + relleno de color) para ratios.

### `MatchCard` (`ui/components/MatchCard.kt`)
`PremiumCard` clickable con chip **WIN** (oro) / **LOSS** (gris), score en mono
(oro si victoria), fecha y pill de origen (MÓVIL/RELOJ).

### `StatTile` (`ui/components/StatTile.kt`)
`PremiumCard` con valor en `setGameNumeral`, label en `sectionHeader` y slot
`trailing` (sparkline, dots, badge).

### `ContextBadge` / `StrokeVerdictBadge`
Pills full-round con borde. Tonos: TieBreak (oro sólido), Golden (oro translúcido),
Mode/Neutral (gris). Los veredictos de golpes (🧊 Heladera · ⚖️ Normal · 🔨 Alto
desgaste · 🦸 Maratón) usan tonos oscuros con borde tenue armonizados al oro.

### `SectionHeader`
Eyebrow en mono uppercase (`sectionHeader`), color `TextMuted`.

---

## 6. Navegación

Bottom nav (`ui/navigation/NavGraph.kt`) con 4 tabs y hairline dorado superior;
ícono/label activos en oro, inactivos en `TextFaint`. En pantallas anchas cambia a
`NavigationRail`.

| Tab | Ruta | Ícono | Pantalla |
|---|---|---|---|
| Marcador | `scoring` | SportsScore | Marcador en vivo (inicio) |
| Historial | `history` | History | Lista de partidos |
| Estadísticas | `stats` | Leaderboard | Stats agregadas |
| Ajustes | `settings` | Settings | Preferencias |

Rutas sin nav: `match_detail/{id}`, `calculator`. Transiciones fade 220ms.

---

## 7. Pantallas

### Marcador (`ui/scoring/ScoringScreen.kt`)
Cancha a pantalla completa (`PadelCourt`, color elegible, líneas doradas al 28%).
Mitad superior = RIVAL, inferior = YO ("YO" en oro). Puntos en `scoreNumeral` mono.
Tap = suma (háptica), long-press = resta. Badges de contexto centrados en la red
(TIE-BREAK oro, SP, modo, decisor). En 40-40 (no Star Point), pulso radial dorado.
Pelota de saque dorada en el box de servicio. FAB "Nuevo partido" en oro.

**Selección de saque:** dos mitades negras divididas por franja "¿QUIÉN SACA?" en
oro; "Saco yo" con glow radial dorado, "Saca rival" neutro.

### Historial (`ui/history/HistoryScreen.kt`)
Título display en oro + subtítulo mono. Lista de `MatchCard` agrupada por período
(HOY / ESTA SEMANA / ESTE MES / mes-año) con sticky headers sobre fondo sólido.
Layout list-detail adaptativo en pantallas anchas. Estado vacío con CTA.

### Detalle de partido (`ui/history/MatchDetailScreen.kt`)
- **Card hero** (`featured` + `grid`): VICTORIA (oro) / DERROTA (gris), score en
  `scoreNumeral`, meta (decisor · duración).
- **Box "Por set"**: cabecera con **golpes totales + PGG + veredicto general**;
  tabla por set con resultado (oro si ganado) a la izquierda y, a la derecha, badge
  de intensidad + `golpes · PGG`. Divisores finos entre sets. Sin datos de golpes,
  muestra solo los resultados.
- **Box "Configuración"**: Fecha, Duración, Modo, Formato, Tie-break, Origen.

### Estadísticas (`ui/stats/StatsScreen.kt`)
Título en oro. **Card "Rendimiento general"** con `DonutChart` (Ganados oro /
Perdidos gris, total al centro) + leyenda. Tiles rápidos (Victorias % con
sparkline, Racha con dots). Cards "Partidos" y "Sets" con `GoldProgressBar`. Card
"Golpes" con total + PGG + veredicto + partido más maratónico.

### Ajustes (`ui/settings/SettingsScreen.kt`)
Sin toggle de tema (dark-only). Secciones: Pantalla (switch dorado), Cancha
(selector de color con mini-canchas), Categoría (segmented oro: 7ma/6ta/5ta),
Herramientas (botón outlined a la Calculadora).

### Calculadora de golpes (`ui/calculator/CalculatorScreen.kt`)
Card de resultado `featured` + `grid`: golpes estimados en `scoreNumeral` oro +
veredicto + copy. Controles: slider de games, segmented de intensidad,
involucramiento y categoría.

### Sheets
- **Nuevo partido** (`NewMatchSheet` → `NewMatchSheetContent`): título oro,
  segmented buttons en oro sólido (Definición, Modo, Sets), botón "Arrancar".
- **Fin de partido** (`MatchEndSheet`): full-screen, VICTORIA/DERROTA en display,
  glow radial dorado con shimmer, score y acciones Guardar/Descartar.

---

## 8. Testing visual

Snapshots con **Paparazzi** en `mobile/src/test/.../MobileScreenshotTest.kt`
(18 tests). Regrabar: `./gradlew :mobile:recordPaparazziDebug`. Verificar:
`./gradlew :mobile:verifyPaparazziDebug`.

> Nota: Paparazzi no descarga Google Fonts, así que en los snapshots la tipografía
> cae al sans del sistema; colores, layout y composición sí son fieles. Las fuentes
> reales (Outfit / JetBrains Mono) se ven en dispositivo.
