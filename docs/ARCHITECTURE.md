# Architecture

YogaFlow is a Kotlin/Jetpack Compose Android app with two feature domains: **Yoga Practice** (primary) and **Zen Battle** (minigame).

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2025.01.00), Material 3 |
| Architecture | ViewModel + StateFlow, AndroidViewModel |
| Database | Room (yoga sessions, reminders, favorites) |
| Persistence | SharedPreferences + Gson (game save data) |
| Navigation | Jetpack Navigation Compose |
| Audio | ZenSoundSynthesizer, SoundPool, Android TTS |
| Build | Gradle with Kotlin DSL, AGP 9.1.1 |

## Module Structure

```
app/src/main/java/com/example/
├── api/                      # API integrations
├── audio/                    # Sound synthesis, ambient music, TTS voice guidance
├── db/                       # Room database, entities, DAOs, repository
├── model/                    # Data models (YogaPose, YogaFlow, LevelDef, XpCalculator)
├── navigation/               # Screen sealed class with routes
├── viewmodel/                # YogaViewModel + sub-VMs (Session, Stats, Settings, Reminder)
├── ui/
│   ├── theme/                # Compose theme (colors, typography, frosted glass)
│   ├── components/           # Shared composables (GlassCard, YogaPoseVisual, etc.)
│   └── screens/              # Screen-level composables (Dashboard, Player, History, etc.)
├── game/                     # Zen Battle minigame (see docs/zen_battle/)
├── MainActivity.kt           # Entry point, NavHost with bottom navigation
└── SettingsScreen.kt         # App settings (theme, audio, reminders)
```

## Navigation

```kotlin
sealed class Screen(val route: String) {
    Dashboard          // "dashboard"          — Bottom nav: main hub
    FlowDetails        // "flow_details/{id}"  — Flow detail + pose list
    Player             // "player"             — Active session player
    SessionComplete    // "session_complete"   — Post-session summary
    ExpandedDashboard  // "expanded_dashboard" — Bottom nav: journey/levels
    History            // "history"            — Bottom nav: practice history
    Settings           // "settings"           — Settings from multiple screens
    ZenGarden          // "zen_garden"         — Bottom nav: Zen Battle
}
```

4-tab bottom nav: **Dashboard**, **Journey** (level badge), **History**, **Zen Battle**.

## ViewModel Hierarchy

```
YogaViewModel (top-level, delegates to sub-VMs)
├── SessionViewModel      — Active yoga session (playback, timer, pose navigation)
├── StatsViewModel        — Statistics, levels, XP, achievements
├── SettingsViewModel     — User preferences (theme, audio, voice)
├── ReminderViewModel     — Per-flow practice reminders
└── RpgViewModel          — Legacy bridge to old Zen Garden (to be replaced)

GameViewModel (independent, for Zen Battle)
    — Party management, battle engine, equipment, save/load
```

## Database (Room)

| Entity | Table | Purpose |
|--------|-------|---------|
| `YogaSession` | `yoga_sessions` | Completed practice sessions |
| `FavoriteFlow` | `favorite_flows` | User-starred flows |
| `ReminderEntity` | `reminders` | Scheduled practice reminders |
| `GardenItemEntity` | `garden_items` | Legacy Zen Garden decorations |

## Data Flow

```
Yoga Practice:
  User browses flows → selects flow → FlowDetails → Start
    → Countdown → Player (pose-by-pose with timer + voice)
    → Session complete → logged to Room, XP calculated
    → Stats updated (levels, achievements, calendar)

Zen Battle (see docs/zen_battle/OVERVIEW.md):
  Yoga practice earns Karma XP and Sparks
    → Level heroes, buy equipment
    → Fight monsters → earn badges/trophies
```

## Key Design Decisions

- **Frosted glass UI**: Custom `FrostedGlassBackground` composable with purple/blue radial gradient orbs, adapts to light/dark theme
- **Canvas pose visuals**: Stick-figure skeleton drawn with bezier curves per pose (front + side views)
- **State management**: `StateFlow` with snapshot pattern for battle state to trigger Compose recomposition
- **Background audio**: Foreground service with wake lock for uninterrupted music playback
- **All game code** under `com.example.game.*` to maintain separation from yoga practice code
