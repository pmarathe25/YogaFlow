# YogaFlow — Agent Quick Reference

## Project Overview
Android app (Kotlin + Jetpack Compose) with two domains: **Yoga Practice** (primary) and **Zen Battle** (turn-based minigame).

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires env vars: KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD)
./gradlew assembleRelease

# Run unit tests (Robolectric + Roborazzi)
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run Roborazzi screenshot tests
./gradlew recordRoborazziDebug   # record new golden images
./gradlew verifyRoborazziDebug   # verify against goldens

# Lint
./gradlew lint

# Clean
./gradlew clean
```

## Key Gradle Config (app/build.gradle.kts)
- **compileSdk**: 36 (Android 16, minorApiLevel 1)
- **minSdk**: 24, **targetSdk**: 36
- **Kotlin**: 2.2.10, **Compose BOM**: 2024.09.00, **AGP**: 9.2.1
- **KSP** for Room compiler + Moshi codegen
- **Secrets plugin** reads `.env` / `.env.example`
- **Signing**: debug uses `debug.keystore` (password: `android`); release reads from env vars

## Architecture Highlights
- **Package**: `com.example` (namespace + applicationId: `com.aistudio.guidedyoga.ymvjkl`)
- **Entry point**: `MainActivity.kt` → `NavHost` with 4-tab bottom nav
- **Navigation**: Sealed class `Screen` in `navigation/Screen.kt`
- **ViewModels**: `YogaViewModel` (delegates to Session/Stats/Settings/Reminder) + independent `GameViewModel`
- **Database**: Room (`YogaSession`, `FavoriteFlow`, `ReminderEntity`)
- **Game save**: SharedPreferences + Gson (`game/persistence/GameSaveManager.kt`)

## Important Conventions
- **Game code isolated** under `com.example.game.*` — do not mix with yoga practice code
- **Frosted glass UI**: `FrostedGlassBackground` composable (theme-aware gradient orbs)
- **Canvas pose visuals**: `PoseVisualizer.kt` draws stick-figure skeletons (front/side)
- **Background audio**: `AmbientMusicService` (foreground service with wake lock)
- **StateFlow + snapshot pattern** for battle state to trigger recomposition

## Common Tasks

### Add a new yoga flow
Edit `model/FlowLoader.kt` (flows defined as data) or add JSON asset + update loader.

### Add a new game hero/monster/equipment
See `docs/zen_battle/` — models in `game/model/`, data in `game/persistence/DataLoader.kt`.

### Modify theme
`ui/theme/Color.kt`, `Type.kt`, `Theme.kt` — Material 3 + custom frosted glass.

### Update Room schema
1. Edit entity in `db/`
2. Increment `version` in `YogaDatabase.kt`
3. Add migration if needed (see `YogaDatabase.kt`)

### Run single test
```bash
./gradlew test --tests "com.example.viewmodel.YogaViewModelTest"
```

## Known Quirks
- `.env` required for release signing and any API keys (see `secrets` block)
- Debug build requires `debug.keystore` in project root (auto-generated if missing)
- Roborazzi goldens stored in `app/src/test/resources/...`
- `kotlin.compiler.execution.strategy=in-process` in gradle.properties to avoid daemon issues
- Some deps commented out in `app/build.gradle.kts` (camera, permissions, datastore) — uncomment if needed

## Documentation
- `docs/ARCHITECTURE.md` — full structure
- `docs/yoga/` — practice features (PRACTICE.md, PROGRESSION.md, SETTINGS.md)
- `docs/zen_battle/` — minigame (OVERVIEW.md, BATTLE.md, HEROES.md, MONSTERS.md, EQUIPMENT.md, REWARDS.md)