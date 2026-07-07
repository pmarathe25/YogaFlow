# Plan 38: Fix Attack Animations — Sprite Lunges Toward Actual Target Position

## Problem

Attack animations use hardcoded `offsetX`/`offsetY` values (e.g., `offsetX = 80f, offsetY = -10f` for heroes). These fixed offsets don't account for the actual screen positions of the attacker and target. As a result:

1. The direction of movement may not point toward the actual target (especially with varying hero count or screen sizes).
2. The vertical component (`offsetY`) makes the movement look diagonal rather than a straightforward lunge.
3. With multiple heroes, the offset is the same regardless of which hero attacks, making the animation feel mechanical.

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

val lungeDistance = 60f  // pixels to lunge toward target

heroAnimStates[event.heroId] = SpriteAnimState(
    state = SpriteState.ATTACKING, stateTime = 0f,
    offsetX = normalizedDx * lungeDistance,
    offsetY = normalizedDy * lungeDistance
)
```

For monster attacking hero:
```kotlin
val targetPos = heroPositions[targetHeroId] ?: return@launch
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

### 2. Simplify to horizontal-only movement (optional)

If pure horizontal movement is preferred (as suggested by the request), set `offsetY = 0f` and only use the X component sign:

```kotlin
val directionX = if (dx > 0) 1f else -1f  // sign of horizontal direction
val lungeDistance = 80f

heroAnimStates[event.heroId] = SpriteAnimState(
    state = SpriteState.ATTACKING, stateTime = 0f,
    offsetX = directionX * lungeDistance,
    offsetY = 0f  // no vertical movement
)
```

This makes the lunge purely horizontal — toward the target but without the diagonal component.

### 3. Use spring animation for smooth return

The existing spring-based `animateFloatAsState` in `CombatantSprite.kt` already handles smooth return to position when `SpriteAnimState` resets to IDLE with `offsetX = 0f, offsetY = 0f`. No changes needed there.

## Files to modify

| File | Changes |
|---|---|
| `BattleAnimations.kt` | Replace hardcoded offset values with direction-to-target calculation using `heroPositions` and `monsterPos`; set `offsetY = 0f` for horizontal-only movement |

## Dependencies

- None. Standalone animation fix.
