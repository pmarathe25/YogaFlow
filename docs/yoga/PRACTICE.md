# Yoga Practice

The core feature of YogaFlow — guided yoga sessions with pose demonstrations, voice guidance, and ambient music.

## Flows

Yoga flows are sequences of poses loaded from `assets/flows.json`. They are organized into three difficulty tracks:

| Track | Description |
|-------|-------------|
| Beginner Path | Flows for newcomers (e.g., Morning Energizer, Bedtime Wind-Down, Core Balance) |
| Intermediate Path | Standard flows (e.g., Sun Salutation, Warrior Strength, Restorative Yin) |
| Advanced Path | Challenging flows (e.g., Heart-Opening Vinyasa, Power Vinyasa, Advanced Balance) |

Each flow has:
- Name, description, difficulty rating
- Total duration in minutes
- Ordered list of poses with hold durations

## Poses

Each `YogaPose` model includes:

| Field | Description |
|-------|-------------|
| `id` | Unique identifier |
| `sanskritName` | Sanskrit name (e.g., "Adho Mukha Svanasana") |
| `englishName` | English name (e.g., "Downward-Facing Dog") |
| `description` | Brief pose description |
| `benefits` | List of health/wellness benefits |
| `instructions` | Step-by-step instructions for proper alignment |
| `voicePrompt` | Text spoken aloud by the voice guide |
| `holdDurationSec` | Default hold time (usually 30s) |

### Pose Visualization

Each pose is rendered as a Canvas-drawn skeleton (front + side views) using `YogaPoseVisual` composable. The `PoseSkeleton` system defines joints, limbs, and a head circle as bezier paths for poses including:

Prayer, Raised Arms, Forward Bend, Lunge, Plank, Eight-Limbed, Cobra, Downward Dog, Mountain, Warrior I/II, Triangle, Child's Pose, Butterfly, Spinal Twist, Savasana, and more.

## Session Player

The `YogaPlayerScreen` provides the active session experience:

- **Pose display**: Canvas skeleton (front + side) with Sanskrit + English name
- **Circular timer**: Arc-based countdown for the current pose's hold duration
- **Playback controls**: Play/pause, skip forward/backward, direct pose selection
- **Instructions**: "How to Hold It" card with key benefits
- **Voice guidance**: TTS reads pose instructions aloud (English or Sanskrit)
- **Ambient music**: Background tracks play during the session
- **Sound effects**: Wood-tap sound on pose completion

### Session Flow

1. User selects a flow from Dashboard → Flow Detail screen
2. Optional 3-2-1 countdown before starting
3. Pose-by-pose progression: timer counts down, voice announces pose
4. On completion: wood-tap sound, auto-advance to next pose
5. Flow ends → SessionCompleteScreen with summary (poses held, time, XP earned)
6. Session auto-logged to Room database

### Voice Guide

- English TTS via Android's built-in engine
- Sanskrit TTS with `sa-IN`/`hi-IN` locale fallback
- Live status banner shows loading/playing/error/idle state
- Voice prompts can be toggled on/off in Settings

### Ambient Music

- Multiple selectable tracks for background playback
- Runs as a foreground service with wake lock for background playback
- Can be muted; track selection with 15-second previews
- Music state persists across sessions
