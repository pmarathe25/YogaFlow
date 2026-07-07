# Plan 47: Monster Defeat Animations

## Problem

When a monster is defeated, the `MonsterDown` event immediately sets `alpha = 0f` and `offsetY = 30f`. The `CombatantSprite` fades alpha over 400ms (via `tween(400)`), but there is no visual drama — no shrinking, dissolving, spiraling, or other defeat animation. The monster simply disappears, and after a delay the victory screen appears.

## Current State

**`BattleAnimations.kt:134-137`**:
```kotlin
is BattleEvent.MonsterDown -> {
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.DYING, stateTime = 0f,
        alpha = 0f, offsetY = 30f      // snap to invisible + shift down
    )
    delay(1200)
}
```

**`CombatantSprite.kt:47-50`** — the only smooth transition:
```kotlin
val smoothAlpha by animateFloatAsState(
    targetValue = animState.alpha,
    animationSpec = tween(400)
)
```

No scale, rotation, or particle effects are used.

## Fix

### 1. Animate defeat with scale + alpha over time

**`BattleAnimations.kt:134-137`** — instead of setting final values instantly, animate through multiple states over the 1200ms delay:

```kotlin
is BattleEvent.MonsterDown -> {
    // Phase 1: Shrink and spin (0-600ms)
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.DYING, stateTime = 0f,
        alpha = 1f, scale = 0.3f            // target: shrink to 30%
    )
    delay(600)

    // Phase 2: Fade out with downward drift (600-1000ms)
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.DYING, stateTime = 0f,
        alpha = 0f, scale = 0.1f, offsetY = 40f
    )
    delay(400)

    // Phase 3: Stay dead
    delay(200)
}
```

### 2. Add rotation/tumble via `CombatantSprite`

**`CombatantSprite.kt`** — add support for rendering rotation in the `SpriteAnimState`:

Add `rotation: Float = 0f` to `SpriteAnimState` in `BattleCanvas.kt`:
```kotlin
data class SpriteAnimState(
    val state: SpriteState = SpriteState.IDLE,
    val stateTime: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,       // NEW: rotation in degrees
    val alpha: Float = 1f
)
```

In `CombatantSprite.kt`, add rotation animation:
```kotlin
val smoothRotation by animateFloatAsState(
    targetValue = animState.rotation,
    animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
)
```

Apply rotation in the canvas draw:
```kotlin
drawContext.transform.rotate(smoothRotation, Offset(drawCx, drawCy))
// ... existing draw calls ...
drawContext.transform.restore()
```

### 3. Spiral defeat animation

Update the `MonsterDown` handler to use a spiraling effect:

```kotlin
is BattleEvent.MonsterDown -> {
    // Spiral out — spin while shrinking and rising slightly
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.DYING, stateTime = 0f,
        alpha = 1f, scale = 1f, rotation = 0f, offsetY = 0f
    )
    delay(100)
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.DYING, stateTime = 0f,
        alpha = 0.8f, scale = 0.7f, rotation = 180f, offsetY = -10f
    )
    delay(200)
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.DYING, stateTime = 0f,
        alpha = 0.5f, scale = 0.4f, rotation = 360f, offsetY = -5f
    )
    delay(200)
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.DYING, stateTime = 0f,
        alpha = 0f, scale = 0.1f, rotation = 540f, offsetY = 20f
    )
    delay(300)
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.DEAD, stateTime = 0f,
        alpha = 0f, scale = 0f
    )
    delay(400)
}
```

### 4. Add particle burst on defeat

**`BattleEffectsLayer.kt`** — add a `MonsterDown` event handler that triggers a particle burst at the monster's position:

```kotlin
is BattleEvent.MonsterDown -> {
    val monsterElement = lastEvent.monster?.element ?: Element.NEUTRAL
    val emitter = emitterConfigForElement(monsterElement).copy(
        force = 15f,        // stronger burst for death
        count = 40          // more particles
    )
    pool.emitBurst(emitter, monsterPosition, 40)
}
```

Pass the monster's element through the `BattleEvent.MonsterDown` data class if not already there.

### 5. Coordinate timing with victory screen

**`GameViewModel.kt:309-325`** — the `advanceToNextTurn()` already waits 2500ms before transitioning to `BATTLE_RESULT`. The defeat animation plays during this window. Ensure the total animation time (~1200ms) fits within the 2500ms delay so the animation completes before the victory screen appears.

## Files to modify

| File | Changes |
|---|---|
| `BattleCanvas.kt` | Add `rotation: Float` field to `SpriteAnimState` |
| `CombatantSprite.kt` | Add `smoothRotation` animation and canvas rotation rendering |
| `BattleAnimations.kt` | Replace instant alpha=0 with staged spiral defeat animation (shrink + rotate + fade over phases) |
| `BattleEffectsLayer.kt` | Add particle burst on `MonsterDown` event |
| `BattleEvent.kt` (if needed) | Ensure `MonsterDown` carries monster element data |

## Dependencies

- A `MonsterDown` event that includes the monster's element. Check `BattleEvent.MonsterDown` — if it doesn't carry the monster reference, add it.
