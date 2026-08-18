# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## graphify

This repo has a knowledge graph at `graphify-out/` with god nodes, community structure, and
cross-file relationships.

- ALWAYS read `graphify-out/GRAPH_REPORT.md` before reading any source files, running grep/glob
  searches, or answering codebase questions. The graph is your primary map of the codebase.
- IF `graphify-out/wiki/index.md` EXISTS, navigate it instead of reading raw files.
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`,
  `graphify path "<A>" "<B>"` or `graphify explain "<concept>"` over grep — these traverse the
  graph's EXTRACTED + INFERRED edges instead of scanning files.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Project Overview

Padel Score — a multi-module Android app for tracking padel tennis scores. Companion architecture: a Wear OS watch app (`:wear`) paired with a phone app (`:mobile`), sharing scoring logic via `:shared`. Both modules share `applicationId = com.gonzalocamera.padelcounter` and are published as a single Play Store listing.

## Build & Test Commands

```bash
# Full build
./gradlew build

# Tests by module
./gradlew :shared:test                                  # Scoring logic + codec (pure JVM, fast)
./gradlew :shared:test --tests "*PadelLogicTest"        # Scoring logic only
./gradlew :shared:test --tests "*MatchCodecTest"        # Serialization codec only
./gradlew :wear:test                                    # Wear module tests
./gradlew :mobile:test                                  # Mobile module tests

# Install on connected device/emulator
./gradlew :wear:installDebug                            # Install wear app
./gradlew :mobile:installDebug                          # Install mobile app

# Screenshot tests (Paparazzi — no emulator needed)
./gradlew :wear:recordPaparazziDebug --tests "*CounterScreenshot*"
./gradlew :mobile:recordPaparazziDebug --tests "*MobileScreenshot*"

# Release builds (keystore configured in ~/.gradle/gradle.properties, see docs/publishing-guide.md)
./gradlew :mobile:bundleRelease :wear:bundleRelease     # Both AABs
# Output: mobile/build/outputs/bundle/release/mobile-release.aab
#         wear/build/outputs/bundle/release/wear-release.aab
```

## Architecture

Three Gradle modules:

### `:shared` (pure Kotlin/JVM — no Android dependencies)
Scoring logic (`PadelState`/`PadelLogic`), domain models (`Match`/`MatchSummary`/`AggregateStats`),
serialization (`MatchCodec`), stroke detection (`StrokeDetector`), and stroke stats (`StrokeStats`).
- **Gotcha:** `Match.strokesPerSet: List<Int>?` — nullable for backward compatibility
- **Gotcha:** `StrokeSensitivity.thresholdMs2()` maps High/Medium/Low → threshold (non-obvious API)

### `:wear` (Wear OS, API 30-35, Compose for Wear)
Single-activity with screens navigated via `mutableStateOf` + `AnimatedVisibility`. **No ViewModel, no DI.**
- **Design:** `StrokeCounterService` is a foreground service (`foregroundServiceType="health"`); strokes accumulate in `StrokeCounter` (in-memory singleton, DataStore backup per game)
- **Constraint:** `ScreenMetrics` adapts layout for round vs square — `fw² + fh² ≤ 1.0`

### `:mobile` (Phone app, API 26+, Material3 Compose)
ViewModels + `ViewModelFactory` for DI (**manual, no framework**). Navigation Compose with bottom nav.
Room database for history, DataStore for preferences, Wearable DataClient for watch sync.
- **Gotcha:** Room migration 3→4 added `strokesPerSetJson` (nullable)
- **Gotcha:** `MatchArchive` needs `encodeDefaults = true` — without it kotlinx omits `version`,
  breaking the compatibility check once `ARCHIVE_VERSION` moves past 1
- **Design:** PGG + verdict are derived at read-time (never persisted) — switching `PadelCategory`
  re-diagnoses every past match
- **Design:** Manual matches use `origin = MANUAL`, `startedAt == finishedAt` (local noon) —
  detail screen hides "Duración" and "Modo" for these
- **Design:** the rating prompt is **score-based, not count-based**. `ReviewPolicy` (`:shared`, pure)
  decides *if*; `NavGraph` decides *when* — stats or match detail after a delay, never cold start.
  Points come from `MobileRepository.insertMatch` (weighted by `MatchOrigin`), sharing a match and
  visiting stats; importing a backup never scores. The modal deep-links to the Play Store listing and
  **not** to the in-app review API, which forbids asking the user anything before its card

### Data flow: Watch → Phone sync
Watch finishes match → `WearSyncQueue.enqueue()` → `WearSyncSender.trySendPending()` sends via `DataClient` → Phone's `SyncBridgeListener` receives → inserts into Room via `MobileRepository`. The payload carries `Match.strokesPerSet` (per-set stroke count, `null` when the feature is off or no sensor); the phone **persists that raw data** (`strokesPerSetJson`) and derives metrics (PGG, per-category verdict) at read-time via `StrokeStats` — the verdict is never persisted, so switching category re-diagnoses every past match.

## Key Domain Concepts

- **Scoring progression:** 0 → 15 → 30 → 40 → Game (indexed 0-4 in `myPointsIdx`/`oppPointsIdx`)
- **Scoring modes:** `DEUCE` (40-40 → advantage), `GOLDEN_POINT` (40-40 → next point wins), `STAR_POINT` (allows two AD/Deuce cycles; once both advantages are spent — `deuceCount >= 2` — the next point at 40-40 decides the game)
- **"SP" indicator:** golden badge shown on wear and mobile only on the Star Point deciding point (`isStarPointDecider` in `:shared`), warning that the next point closes the game
- **Tie-break:** Triggers at 6-6 games. `TB7` (first to 7) or `SUPER10` (first to 10), win by 2
- **Best-of:** Configurable match length (default: best of 3 sets)
- **State is immutable:** Always use `PadelState.copy()` for updates
- **Stroke counting (wear):** on-device peak detection (`StrokeDetector`: magnitude `√(x²+y²+z²)` over threshold + ~350ms debounce). The watch only counts and groups per set (raw data); all interpretation is deferred to the phone. Sensitivity is user-adjustable (High/Medium/Low → threshold), calibrated via the "Probar contador" test mode. Requires wearing the watch on the paddle-hand wrist
- **Stroke stats (mobile):** key metric is **PGG** (strokes per game = strokes ÷ games, normalizes for match length). PGG maps to a `StrokeVerdict` (🧊 Fridge / ⚖️ Normal / 🔨 High load / 🦸 Marathon) whose thresholds depend on `PadelCategory` (SEPTIMA/SEXTA/QUINTA, chosen in Settings, default SEXTA). Computed per set and per match in `StrokeStats` (`:shared`), derived at read-time — never persisted

## Conventions

- **Commit format:** Conventional Commits — `feat(wear): ...`, `fix(mobile): ...`, `refactor(shared): ...`
- **UI text:** Spanish for user-facing strings, English for code identifiers
- **Pure functions preferred** in `:shared` logic — no side effects
- **Versioning:** `PADEL_VERSION_CODE` and `PADEL_VERSION_NAME` in root `gradle.properties`, shared by both `:mobile` and `:wear`. Build enforces consistency via `checkVersionConsistency` task.
- **Haptic feedback (wear):** `TextHandleMove` for taps, `LongPress` for double-taps
- **Wear text buttons:** never use Wear Material's `Button`/`OutlinedButton` with a text label —
  they are **circular icon buttons** (`size(52.dp)` fixed, no content padding), so with a large
  system font the label wraps and gets clipped by the pill's rounded edge. Play rejected the app
  twice over this (WO-V1). Use the `WideTextButton` helper (`Chip`/`OutlinedChip`), which grows in
  height and reserves padding. The two 36dp buttons in `StrokeTestScreen` are the only legitimate
  `Button` uses: those really are icon buttons.
- **Wear long text:** center it (`TextAlign.Center`) and keep each `ScalingLazyColumn` item to ≤3
  lines at the largest font. On a round display the available width at distance `y` from center is
  `√(R²−y²)`: left-aligned lines all start at the same `x` and lose their first letter in the
  curve, and an item taller than the screen renders at scale 1.0 with its extreme lines in the
  corners. Worst case to verify against: **192dp · font_scale 1.24 · bold** (see
  `docs/publishing-guide.md` §8).
