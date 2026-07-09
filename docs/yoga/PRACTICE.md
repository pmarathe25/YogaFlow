# Yoga Practice

The core feature of YogaFlow — guided yoga sessions with pose demonstrations, voice guidance, and ambient music.

## Data Sources

| Data | Location |
|------|----------|
| Flows | `app/src/main/assets/flows.json` |
| Poses | `app/src/main/assets/poses.json` |
| Flow loading logic | `model/FlowLoader.kt` |
| Level definitions | `model/LevelDefinitions.kt` |
| XP calculation | `model/XpCalculator.kt` |

## Key Components

- **FlowLoader** — Loads poses and flows from JSON assets with caching
- **YogaPoseVisual / PoseVisualizer** — Canvas-drawn skeleton (front + side views)
- **YogaPlayerScreen** — Active session: pose display, circular timer, playback controls, instructions, voice guidance, ambient music
- **SessionViewModel** — Manages active session state (timer, pose navigation, playback)

## Session Flow

1. User selects a flow from Dashboard → Flow Detail screen
2. Optional 3-2-1 countdown before starting
3. Pose-by-pose progression: timer counts down, voice announces pose
4. On completion: wood-tap sound, auto-advance to next pose
5. Flow ends → SessionCompleteScreen with summary (poses held, time, XP earned)
6. Session auto-logged to Room database

## Voice Guide

- English TTS via Android's built-in engine
- Sanskrit TTS with `sa-IN`/`hi-IN` locale fallback
- Live status banner shows loading/playing/error/idle state
- Toggle in Settings

## Ambient Music

- Multiple selectable tracks for background playback
- Runs as a foreground service with wake lock for background playback
- Can be muted; track selection with 15-second previews
- Music state persists across sessions

## Settings

See `SettingsScreen.kt` and `SettingsViewModel` for all user preferences (theme, screen awake, audio, voice language, reminders).