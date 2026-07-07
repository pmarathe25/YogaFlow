# Plan 37: Battle Sound Effects

## Problem

The battle screen has no sound effects. Attack hits, heals, ultimate activations, and monster actions are all silent. This makes the battle feel flat and unresponsive. The rest of the app already has audio infrastructure (`ZenSoundSynthesizer` for SFX, `AmbientMusicService` for music), but battle is entirely mute.

## Fix

### 1. Create `BattleSoundManager`

**New file** — similar pattern to `ZenSoundSynthesizer.kt` but more extensive. Synthesizes short PCM-based sound effects using Android's `SoundPool`:

```kotlin
class BattleSoundManager(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    // Sound IDs
    private var hitSoundId: Int = 0
    private var healSoundId: Int = 0
    private var whooshSoundId: Int = 0
    private var ultimateSoundId: Int = 0
    private var clickSoundId: Int = 0

    init {
        hitSoundId = loadSynthesizedSound(context) { generateHitPcm() }
        healSoundId = loadSynthesizedSound(context) { generateHealPcm() }
        whooshSoundId = loadSynthesizedSound(context) { generateWhooshPcm() }
        ultimateSoundId = loadSynthesizedSound(context) { generateUltimatePcm() }
        clickSoundId = loadSynthesizedSound(context) { generateClickPcm() }
    }

    fun playHit()    { soundPool.play(hitSoundId, 0.7f, 0.7f, 1, 0, 1f) }
    fun playHeal()   { soundPool.play(healSoundId, 0.5f, 0.5f, 1, 0, 1f) }
    fun playWhoosh() { soundPool.play(whooshSoundId, 0.3f, 0.3f, 1, 0, 1f) }
    fun playUltimate() { soundPool.play(ultimateSoundId, 0.8f, 0.8f, 1, 0, 1f) }
    fun playClick()  { soundPool.play(clickSoundId, 0.4f, 0.4f, 1, 0, 1f) }

    fun release() { soundPool.release() }
}
```

Each `generate*Pcm()` function synthesizes a short waveform:
- **Hit**: short burst of noise with exponential decay (~100ms)
- **Heal**: rising sine tone (~200ms)
- **Whoosh**: filtered white noise sweep (~150ms)
- **Ultimate**: sustained tone with harmonic stack (~500ms)
- **Click**: very short high-frequency tick (~30ms)

### 2. Wire into battle composable

**`BattleScene.kt`** — create the `BattleSoundManager` instance with `LocalContext.current` and pass it (or expose it) to the layers that need it.

Option A: Use `CompositionLocal`:
```kotlin
val LocalBattleSoundManager = staticCompositionLocalOf<BattleSoundManager> {
    error("No BattleSoundManager provided")
}
```

Provide it at the top of `BattleScreen`:
```kotlin
val soundManager = remember { BattleSoundManager(context) }
CompositionLocalProvider(LocalBattleSoundManager provides soundManager) {
    // existing battle layout
}
DisposableEffect(Unit) {
    onDispose { soundManager.release() }
}
```

Option B: Pass as parameter through the composable tree (simpler, less refactoring).

### 3. Trigger sounds on battle events

**`BattleEffectsLayer.kt`** — in the LaunchedEffect that watches `state.eventLog`, add sound triggers:

```kotlin
LaunchedEffect(state.eventLog.size) {
    val lastEvent = state.eventLog.lastOrNull() ?: return@LaunchedEffect
    when (lastEvent) {
        is BattleEvent.SkillUsed -> {
            when {
                lastEvent.skill.ultimateGain == 0 -> soundManager.playUltimate()
                lastEvent.skill.healScaling != null -> soundManager.playHeal()
                lastEvent.skill.damageComponents.isNotEmpty() -> {
                    soundManager.playWhoosh()  // attack swoosh on use
                    delay(300)                 // wait for impact
                    soundManager.playHit()     // impact sound
                }
                else -> soundManager.playWhoosh()
            }
        }
        is BattleEvent.MonsterTurn -> {
            soundManager.playWhoosh()  // monster attacks
            delay(300)
            soundManager.playHit()
        }
        else -> {}
    }
}
```

Alternatively, wire sounds into `rememberSpriteAnimations()` in `BattleAnimations.kt` so sounds play in sync with animation timings.

### 4. Add settings toggle (optional future)

In a follow-up, a "SFX Volume" or "Sound Effects" toggle could be added to settings. For now, sounds always play.

## Files to modify

| File | Changes |
|---|---|
| `BattleSoundManager.kt` (new) | SoundPool-based SFX manager with synthesized hit/heal/whoosh/ultimate/click sounds |
| `BattleScene.kt` | Create and wire `BattleSoundManager` instance |
| `BattleEffectsLayer.kt` or `BattleAnimations.kt` | Trigger sounds on battle events |

## Dependencies

- None. Uses existing `SoundPool` infrastructure (same pattern as `ZenSoundSynthesizer`).
