# Plan 46: Element-Based Diverse Sound Effects

## Problem

Currently all damaging skills play the same generic `whoosh` + `hit` sound effects, regardless of the skill's element. A Water-element attack like "Rippling Current" sounds identical to an Earth-element attack. The sounds should reflect the element of the ability to make combat feel more immersive.

## Current State

`BattleSoundManager` (already implemented) has 5 generic synthesized sounds:
- `playWhoosh()` — broad-band noise sweep (used for all attack initiations)
- `playHit()` — noise burst with decay (used for all impacts)
- `playHeal()` — rising sine tone
- `playUltimate()` — harmonic stack
- `playClick()` — short high tick

`BattleEffectsLayer.kt:160-176` plays these sounds based solely on whether the skill heals, damages, or is an ultimate — no element differentiation.

## Fix

### 1. Add element-specific sounds to `BattleSoundManager`

**`BattleSoundManager.kt`** — add synthesized PCM generation for each element, creating distinct tonal qualities:

```kotlin
// Element-specific sound IDs
private var fireSoundId: Int = 0
private var waterSoundId: Int = 0
private var earthSoundId: Int = 0
private var airSoundId: Int = 0
private var lightSoundId: Int = 0
private var darkSoundId: Int = 0

init {
    // ... existing sounds ...
    fireSoundId = loadSynthesizedSound(context) { generateFirePcm() }
    waterSoundId = loadSynthesizedSound(context) { generateWaterPcm() }
    earthSoundId = loadSynthesizedSound(context) { generateEarthPcm() }
    airSoundId = loadSynthesizedSound(context) { generateAirPcm() }
    lightSoundId = loadSynthesizedSound(context) { generateLightPcm() }
    darkSoundId = loadSynthesizedSound(context) { generateDarkPcm() }
}
```

Sound design for each element:
- **Fire** (`generateFirePcm`): Crackling noise burst with low rumble — noise filtered at ~200-2000 Hz with rapid amplitude modulation
- **Water** (`generateWaterPcm`): Splashing/chiming tone — sine wave with rapid frequency modulation (500 → 1200 Hz → 600 Hz sweep) with reverb-like echoes
- **Earth** (`generateEarthPcm`): Low, heavy thud — low-frequency sine (80 Hz) with slow attack and long decay, harmonics at 160/240 Hz
- **Air** (`generateAirPcm`): Whooshing wind — white noise with bandpass sweep (low → high → low) with gentle fade
- **Light** (`generateLightPcm`): Bright, shimmering chime — high harmonics of 440 Hz with fast attack, slow decay, and tremolo
- **Dark** (`generateDarkPcm`): Deep, ominous rumble — sub-bass sine (60 Hz) with slow attack, very long decay, and slight distortion

Add playback functions:
```kotlin
fun playElementSound(element: Element) {
    val soundId = when (element) {
        Element.FIRE -> fireSoundId
        Element.WATER -> waterSoundId
        Element.EARTH -> earthSoundId
        Element.AIR -> airSoundId
        Element.LIGHT -> lightSoundId
        Element.DARK -> darkSoundId
        Element.SHADOW -> darkSoundId       // reuse dark for shadow
        Element.ELECTRIC -> lightSoundId     // reuse light for electric
        else -> whooshSoundId                // fallback
    }
    soundPool.play(soundId, 0.5f, 0.5f, 1, 0, 1f)
}
```

### 2. Wire element sounds into battle effects

**`BattleEffectsLayer.kt:160-176`** — modify the sound effect block to use the skill's element for damage sounds:

```kotlin
when {
    event.skill.ultimateGain == 0 && event.skill.damageComponents.isNotEmpty() -> {
        soundManager.playUltimate()
    }
    event.skill.healScaling != null -> {
        soundManager.playHeal()
    }
    event.skill.damageComponents.isNotEmpty() -> {
        // Determine primary element from damage components
        val primaryElement = event.skill.damageComponents
            .firstNotNullOfOrNull { it.element } ?: Element.NEUTRAL
        soundManager.playElementSound(primaryElement)
        scope.launch {
            delay(300)
            soundManager.playHit()
        }
    }
    else -> soundManager.playWhoosh()
}
```

For `MonsterTurn` (line 247), determine the element from the monster's element:
```kotlin
soundManager.playElementSound(monster.element)
scope.launch {
    delay(300)
    soundManager.playHit()
}
```

### 3. Keep generic sounds as fallback

The generic `playWhoosh()` and `playHit()` remain as fallbacks for neutral elements or non-damaging actions.

## Files to modify

| File | Changes |
|---|---|
| `BattleSoundManager.kt` | Add 6 element-specific synthesized PCM sounds; add `playElementSound(element: Element)` function |
| `BattleEffectsLayer.kt` | Pass skill element to `playElementSound()` instead of `playWhoosh()`; pass monster element for monster turns |

## Dependencies

- None. Extends existing `BattleSoundManager`.
