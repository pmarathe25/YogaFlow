# Plan 38: Fix Attack Animations — Sprite Lunges Toward Actual Target Position

## Problem

Attack animations use hardcoded `offsetX`/`offsetY` values (e.g., `offsetX = 80f, offsetY = -10f` for heroes). These fixed offsets don't account for the actual screen positions of the attacker and target. As a result:

1. The direction of movement may not point toward the actual target (especially with varying hero count or screen sizes).
2. With multiple heroes, the offset is the same regardless of which hero attacks or where the target is, making the animation feel mechanical.
3. The hardcoded values were tuned for a specific screen layout and may look wrong on different devices.

## Fix

### 1. Compute direction vector from attacker to target

**`BattleAnimations.kt`** — in `rememberSpriteAnimations()`, instead of hardcoded `offsetX`/`offsetY`, calculate the direction from the attacker's position to the target's position.

The function already receives screen positions (`heroPositions: Map<String, Offset>`, `monsterPos: Offset`). Use these to compute the lunge direction.

For hero attacking monster:
```kotlin
val attackerPos = heroPositions[event.heroId] ?: return@launch
val targetPos = monsterPos
val dx = targetPos.x - attackerPos.x
val dy = targetPos.y - attackerPos.y
val distance = sqrt(dx * dx + dy * dy)
val normalizedDx = dx / distance
val normalizedDy = dy / distance

val lungeDistance = 80f  // pixels to lunge toward target

heroAnimStates[event.heroId] = SpriteAnimState(
    state = SpriteState.ATTACKING, stateTime = 0f,
    offsetX = normalizedDx * lungeDistance,
    offsetY = normalizedDy * lungeDistance
)
```

For monster attacking hero:
```kotlin
val targetPos = heroPositions[targetHeroId] ?: return@launch
val monsterPos = monsterPos
val dx = targetPos.x - monsterPos.x
val dy = targetPos.y - monsterPos.y
val distance = sqrt(dx * dx + dy * dy)
val normalizedDx = dx / distance
val normalizedDy = dy / distance

monsterAnimState.value = SpriteAnimState(
    state = SpriteState.ATTACKING, stateTime = 0f,
    offsetX = normalizedDx * lungeDistance,
    offsetY = normalizedDy * lungeDistance
)
```

For the hit reaction on the target, use the inverse direction (target recoils away from attacker):
```kotlin
val recoilDistance = 25f
event.targets.forEach { targetHeroId ->
    heroAnimStates[targetHeroId] = SpriteAnimState(
        state = SpriteState.HIT, stateTime = 0f,
        offsetX = -normalizedDx * recoilDistance,
        offsetY = -normalizedDy * recoilDistance
    )
}
```

### 2. Use spring animation for smooth return

The existing spring-based `animateFloatAsState` in `CombatantSprite.kt` already handles smooth return to position when `SpriteAnimState` resets to IDLE with `offsetX = 0f, offsetY = 0f`. No changes needed there.

## Files to modify

| File | Changes |
|---|---|
| `BattleAnimations.kt` | Replace hardcoded offset values with direction-to-target calculation using `heroPositions` and `monsterPos`; compute lunge vector from actual positions; apply inverse vector for target hit recoil |

## Dependencies

- None. Standalone animation fix.
