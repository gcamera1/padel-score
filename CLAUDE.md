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
- `PadelLogic` — pure functions for scoring (`addPointToMy`, `addPointToOpp`, `subtractPointFrom*`, `pointsLabel`). Takes `PadelState` in, returns `PadelState` out.
- `Match` / `MatchSummary` / `AggregateStats` — domain models for completed matches
- `MatchCodec` — JSON serialization via kotlinx.serialization for watch↔phone sync
- `Enums` — `Decider`, `CourtColorOption`, `Winner`, `MatchOrigin`, `ScoringMode`

### `:wear` (Wear OS, API 30-34, Compose for Wear)
- Single-activity (`MainActivity.kt`) with three screens (COUNTER, SETTINGS, NEW_MATCH) navigated via `mutableStateOf` + `AnimatedVisibility`. No ViewModel, no DI.
- `PadelDataStore` — `PadelRepository` wrapping DataStore Preferences, exposes `PadelState` as `Flow`
- `sync/` — `WearSyncQueue` + `WearSyncSender` enqueue completed matches and push to phone via Wearable DataClient
- Gestures: tap to score, double-tap to subtract, swipe-left for settings
- `ScreenMetrics` adapts layout for round vs square screens (constraint: `fw² + fh² ≤ 1.0`)

### `:mobile` (Phone app, API 26+, Material3 Compose)
- Uses ViewModels + `ViewModelFactory` for DI (manual, no framework)
- Navigation Compose with bottom nav: Scoring, History, Stats, Settings
- `data/db/` — Room database (`PadelDatabase`, `MatchDao`, `MatchEntity`) for match history persistence
- `data/MobilePreferences` — DataStore for in-progress match state and user preferences
- `data/MobileRepository` — single repository coordinating Room + DataStore
- `sync/SyncBridgeListener` — receives match data from watch via Wearable DataClient
- `sync/SyncBridgeClient` — checks watch connectivity

### Data flow: Watch → Phone sync
Watch finishes match → `WearSyncQueue.enqueue()` → `WearSyncSender.trySendPending()` sends via `DataClient` → Phone's `SyncBridgeListener` receives → inserts into Room via `MobileRepository`.

## Key Domain Concepts

- **Scoring progression:** 0 → 15 → 30 → 40 → Game (indexed 0-4 in `myPointsIdx`/`oppPointsIdx`)
- **Scoring modes:** `DEUCE` (40-40 → advantage), `GOLDEN_POINT` (40-40 → next point wins), `STAR_POINT`
- **Tie-break:** Triggers at 6-6 games. `TB7` (first to 7) or `SUPER10` (first to 10), win by 2
- **Best-of:** Configurable match length (default: best of 3 sets)
- **State is immutable:** Always use `PadelState.copy()` for updates

## Conventions

- **Commit format:** Conventional Commits — `feat(wear): ...`, `fix(mobile): ...`, `refactor(shared): ...`
- **UI text:** Spanish for user-facing strings, English for code identifiers
- **Pure functions preferred** in `:shared` logic — no side effects
- **Versioning:** `PADEL_VERSION_CODE` and `PADEL_VERSION_NAME` in root `gradle.properties`, shared by both `:mobile` and `:wear`. Build enforces consistency via `checkVersionConsistency` task.
- **Haptic feedback (wear):** `TextHandleMove` for taps, `LongPress` for double-taps
