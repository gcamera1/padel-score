# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- `PadelState` — immutable data class representing match state
- `PadelLogic` — pure functions for scoring (`addPointToMy`, `addPointToOpp`, `subtractPointFrom*`, `pointsLabel`, `isStarPointDecider`). Takes `PadelState` in, returns `PadelState` out.
- `Match` / `MatchSummary` / `AggregateStats` — domain models for completed matches
  (`Match.strokesPerSet: List<Int>?` — per-set stroke count, nullable/backward-compatible)
- `MatchCodec` — JSON serialization via kotlinx.serialization for watch↔phone sync
- `StrokeDetector` — pure stroke detection by acceleration peaks (threshold + debounce),
  testable without Android; `StrokeSensitivity.thresholdMs2()` maps High/Medium/Low → threshold
- `StrokeStats` — pure interpretation of stroke data (phone side): `Match.strokeStats(category)`
  derives PGG (strokes ÷ games) per set and total; `strokeAggregate(matches, category)` aggregates
  history; `PadelCategory.verdict(pgg)` maps PGG → `StrokeVerdict` calibrated by category
- `Enums` — `Decider`, `CourtColorOption`, `Winner`, `MatchOrigin`, `ScoringMode`,
  `StrokeSensitivity`, `PadelCategory`, `StrokeVerdict`

### `:wear` (Wear OS, API 30-34, Compose for Wear)
- Single-activity (`MainActivity.kt`) with screens (COUNTER, SETTINGS, NEW_MATCH, TUTORIAL, WALKTHROUGH, MATCH_FINISHED, STROKE_TEST) navigated via `mutableStateOf` + `AnimatedVisibility`. No ViewModel, no DI.
- `PadelDataStore` — `PadelRepository` wrapping DataStore Preferences, exposes `PadelState` as `Flow`
- `sync/` — `WearSyncQueue` + `WearSyncSender` enqueue completed matches and push to phone via Wearable DataClient
- `StrokeCounterService` — foreground service (`foregroundServiceType="health"`) that samples the accelerometer during a match and feeds `StrokeDetector` (`:shared`); strokes accumulate per set in `StrokeCounter` (in-memory singleton, DataStore backup per game). Lifecycle tied to match start/finish; injected into `Match.strokesPerSet` on finish. Test mode (`StrokeTestScreen`) registers the sensor directly from the composable (no service)
- Gestures: tap to score, double-tap to subtract, swipe-left for settings
- `ScreenMetrics` adapts layout for round vs square screens (constraint: `fw² + fh² ≤ 1.0`)

### `:mobile` (Phone app, API 26+, Material3 Compose)
- Uses ViewModels + `ViewModelFactory` for DI (manual, no framework)
- Navigation Compose with bottom nav: Scoring, History, Stats, Settings
- `data/db/` — Room database (`PadelDatabase`, `MatchDao`, `MatchEntity`) for match history persistence.
  `MatchEntity.strokesPerSetJson` (nullable) persists the per-set stroke count from the watch (migration 3→4)
- `data/MobilePreferences` — DataStore for in-progress match state and user preferences (incl. `category`)
- `data/MobileRepository` — single repository coordinating Room + DataStore
- `sync/SyncBridgeListener` — receives match data from watch via Wearable DataClient
- `sync/SyncBridgeClient` — checks watch connectivity
- **Stroke stats:** phone interprets the watch's raw data via `StrokeStats` (`:shared`). PGG + verdict
  are derived at read-time (not persisted) using the `PadelCategory` chosen in Settings, and shown in a
  "GOLPES" section of the match detail (per set + total) and aggregated in the Stats screen

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
