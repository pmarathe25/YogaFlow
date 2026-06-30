# YogaFlow

A guided yoga practice app for Android with pose visualizations, voice guidance, ambient music, progress tracking, and an optional turn-based battle minigame.

## Features

### Yoga Practice
- **Guided flows**: Browse yoga sequences organized by difficulty (Beginner/Intermediate/Advanced)
- **Pose visualizations**: Canvas-drawn front + side skeleton views for proper alignment
- **Voice guidance**: TTS reads pose instructions aloud (English or Sanskrit)
- **Ambient music**: Background tracks with foreground service playback
- **Session timer**: Circular arc timer for each pose's hold duration
- **Progress tracking**: Karma XP, 10 yoga levels, achievements, practice history
- **Practice calendar**: Monthly view of completed sessions with detail expansion
- **Reminders**: Per-flow scheduled notifications with alarm integration

### Zen Battle (Minigame)
- Turn-based battles with 5 yoga-virtue heroes vs. 16 inner-demon monsters
- Speed-based turn queue with elemental damage and combo team attacks
- Equipment system (generic, class-specific, unique items with set bonuses)
- Canvas-drawn silhouettes and parallax backgrounds
- Badge and trophy progression tied to yoga practice

## Tech Stack

- **Language**: Kotlin 2.2.10, Jetpack Compose (BOM 2025.01.00), Material 3
- **Architecture**: ViewModel + StateFlow, AndroidViewModel
- **Database**: Room (yoga data) + SharedPreferences/Gson (game save)
- **Audio**: ZenSoundSynthesizer, SoundPool, Android TTS
- **Navigation**: Jetpack Navigation Compose
- **Build**: Gradle with Kotlin DSL, AGP 9.1.1

## Getting Started

1. Open in Android Studio
2. Remove `signingConfig = signingConfigs.getByName("debugConfig")` from `app/build.gradle.kts`
3. Build and run on emulator or physical device

## Documentation

Developer docs are in `docs/`:

- `docs/YOGA/` — Yoga practice features
- `docs/ZEN_BATTLE/` — Battle minigame
- `docs/ARCHITECTURE.md` — Full app structure
