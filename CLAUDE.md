# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Padel Score — a Wear OS (Android) app for tracking padel tennis scores. Kotlin + Jetpack Compose, targeting 40mm+ watches (API 30-35).

## Build & Test Commands

```bash
./gradlew build                              # Build the project
./gradlew test                               # Run all unit tests
./gradlew test --tests "*PadelLogicTest"     # Run scoring logic tests only
./gradlew installDebug                       # Install on connected watch/emulator

# Screenshots (Paparazzi — no emulator needed)
./gradlew recordPaparazziDebug --tests "*CounterScreenshot*"
# Output: app/src/test/snapshots/images/

# Release build (requires keystore configured in ~/.gradle/gradle.properties)
./gradlew bundleRelease                      # Build AAB for Play Store
# Output: app/build/outputs/bundle/release/app-release.aab
```

## Architecture

Three-layer architecture, no ViewModel, no DI — direct instantiation suits this small Wear app.

**All source lives in** `app/src/main/java/com/gonzalocamera/padelcounter/presentation/`:

- **`MainActivity.kt`** — Single activity with all Composable UI. Navigation between three screens (COUNTER, SETTINGS, NEW_MATCH) via `mutableStateOf` + `AnimatedVisibility`. Gestures: tap to score, double-tap to subtract, swipe-left for settings.
- **`PadelLogic.kt`** — Pure functions for scoring logic (`addPointToMy`, `addPointToOpp`, `subtractPointFromMy`, `subtractPointFromOpp`, `pointsLabel`). No side effects — takes `PadelState` in, returns `PadelState` out. This is the most testable layer.
- **`PadelDataStore.kt`** — `PadelRepository` class wrapping AndroidX DataStore Preferences. Holds `PadelState` (immutable data class) and exposes it as a `Flow`. All persistence is suspend-based.
- **`theme/Theme.kt`** — Minimal Wear Material3 theme wrapper.

**Tests:** `app/src/test/.../PadelLogicTest.kt` — 45 tests using JUnit 4 + Google Truth covering normal scoring, deuce/advantage, golden point, tie-breaks (TB7 and SUPER10), and point subtraction.

## Key Domain Concepts

- **Scoring progression:** 0 → 15 → 30 → 40 → Game (indexed 0-4 in `myPointsIdx`/`oppPointsIdx`)
- **Golden Point:** At 40-40, next point wins (no deuce). Toggled per match.
- **Deuce/AD:** When golden point is off, 40-40 → advantage → win or back to deuce
- **Tie-break:** Triggers at 6-6 games. Two modes: TB7 (first to 7) or SUPER10 (first to 10)
- **State is immutable:** Always use `PadelState.copy()` for updates

## Conventions

- **Commit format:** Conventional Commits — `feat(wear): ...`, `fix: ...`, `refactor: ...`
- **UI text:** Spanish for user-facing strings, English for code identifiers
- **Pure functions preferred** in logic layer — no unnecessary dependencies or side effects
- **Responsive layout:** `ScreenMetrics` adapts court size for round vs square screens. Round screens use smaller fractions to fit within the inscribed circle (`fw² + fh² ≤ 1.0`)
- **Haptic feedback:** `TextHandleMove` for taps, `LongPress` for double-taps

## Enums

- `Decider`: `TB7` | `SUPER10` — tie-break format
- `CourtColorOption`: `BLUE` | `ORANGE` | `GREEN` | `PURPLE`
