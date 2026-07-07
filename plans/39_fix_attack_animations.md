# Plan 38: Fix Attack Animations — Sprite Lunges Toward Actual Target Position

## Problem

Attack animations partially compute direction-to-target, but the vertical component is zeroed out (`offsetY = 0f`), making the lunge purely horizontal. Since heroes and monsters are vertically separated on screen, the lunge should move diagonally toward the actual target position for a natural look.

Hit reactions also still use hardcoded offsets instead of the inverse lunge direction.

## Current State

Looking at `BattleAnimations.kt`:

- **Hero attacking monster** (lines 73-94): Computes `dx`, `dy`, `distance`, `normalizedDx` from positions, but sets `offsetY = 0f` (line 84). Monster hit reaction uses hardcoded `offsetX = -25f, offsetY = 5f` (line 91).
- **Monster attacking hero** (lines 104-129): Same horizontal-only lunge (`offsetY = 0f`, line 115). Hero hit reaction uses hardcoded `offsetX = -15f, offsetY = 10f` (line 122).
- **Non-damaging skills** (lines 96-101): Hardcoded `offsetX = 0f, offsetY = -30f` (vertical hop).

## Remaining Changes

### 1. Use full 2D direction for attacker lunge

**Line 84** — replace `offsetY = 0f` with computed vertical component:
```kotlin
offsetX = normalizedDx * lungeDistance,
offsetY = normalizedDy * lungeDistance   // was 0f
```

**Line 115** — same change for monster attacks:
```kotlin
offsetX = normalizedDx * lungeDistance,
offsetY = normalizedDy * lungeDistance   // was 0f
```

### 2. Use inverse direction for target hit recoil

**Lines 89-92** — monster hit reaction uses computed inverse direction instead of hardcoded `-25f, 5f`:
```kotlin
monsterAnimState.value = SpriteAnimState(
    state = SpriteState.HIT, stateTime = 0f,
    offsetX = -normalizedDx * 25f,
    offsetY = -normalizedDy * 25f       // recoil away from attacker
)
```

**Lines 119-123** — hero hit reaction uses computed inverse direction instead of hardcoded `-15f, 10f`:
```kotlin
event.targets.forEach { targetHeroId ->
    heroAnimStates[targetHeroId] = SpriteAnimState(
        state = SpriteState.HIT, stateTime = 0f,
        offsetX = -normalizedDx * 25f,
        offsetY = -normalizedDy * 25f   // recoil away from monster
    )
}
```

### 3. Non-damaging skills (optional)

The hardcoded vertical hop (`offsetX = 0f, offsetY = -30f`) for heals/buffs at lines 96-101 can remain as-is, since there's no target to lunge toward. Alternatively, if a hero element is available, it could lunge toward the target hero (self or ally). This is low priority.

## Files to modify

| File | Changes |
|---|---|
| `BattleAnimations.kt` | Use `normalizedDy * lungeDistance` for Y offset on lines 84, 115; use computed inverse direction for hit recoil on lines 89-92, 119-123 |

## Dependencies

- None. Completes the partially-implemented change.
