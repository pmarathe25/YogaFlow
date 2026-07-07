# Plan 35: Grey Out Unusable Ultimate Cards + Block Drag on Greyed Cards

## Problem

Two usability issues with battle skill cards:

1. **Ultimate card not visually greyed when gauge < 100** — The `SkillCard` has a separate not-ready ultimate branch (`Color(0xFFEEEEEE)` background, `Color.Gray` border) but it doesn't use the full cooldown visual treatment (reduced alpha, grey text, etc.). The card looks similar to a ready card.

2. **Greyed cards are still draggable** — Cards on cooldown or with an unready ultimate can still be dragged up and then snap back. The drag gesture itself isn't blocked — only the execution is checked in `onDragEnd`.

## Changes

### 1. Reuse `isOnCooldown` code path for unready ultimates

**`ActionTray.kt:400`** — change the definition of `isOnCooldown` so it also covers unready ultimates:

Before:
```kotlin
val isOnCooldown = cooldownRemaining > 0
```

After:
```kotlin
val isOnCooldown = cooldownRemaining > 0 || (isUltimate && !ultReady)
```

This means unready ultimates automatically get all the cooldown visual treatment:
- `Color(0xFFE0E0E0)` background
- `Color.Gray` border
- `.alpha(0.8f)` card alpha + `.alpha(0.5f)` icon alpha
- `Color.Gray` name text + `Color.LightGray` description text
- Cooldown badge (greyed out)

**`ActionTray.kt:404-418`** — simplify `bgColor` and `borderColor` to remove the separate not-ready ultimate branch:

```kotlin
val bgColor = when {
    isOnCooldown                    -> Color(0xFFE0E0E0)
    isUltimate                      -> Color(0xFFFFF9C4)   // only reached when ready
    skill.healScaling != null       -> Color(0xFFF1F8E9)
    skill.damageComponents.isNotEmpty() -> Color(0xFFFFF1F0)
    else                            -> Color(0xFFE1F5FE)
}

val borderColor = when {
    isOnCooldown                    -> Color.Gray
    isUltimate                      -> Color(0xFFFFD700)   // only reached when ready
    skill.healScaling != null       -> Color(0xFF689F38)
    skill.damageComponents.isNotEmpty() -> Color(0xFFD32F2F)
    else                            -> Color(0xFF0288D1)
}
```

Remove the separate alpha and text color overrides for unready ultimates — the cooldown styling handles them.

### 2. Block drag gesture on greyed cards

**`ActionTray.kt:216-273`** — in the `pointerInput` block for `detectVerticalDragGestures`, check if the card is usable before starting the drag:

```kotlin
val skill = item
if (skill is com.example.game.model.Skill) {
    val isUlt = skill.ultimateGain == 0
    val isUsable = if (isUlt) currentHero.gauge >= 100
                   else (skillCooldowns[skill.id] ?: 0) <= 0
    if (!isUsable) return@detectVerticalDragGestures  // block drag start
}
```

For the tap gesture (lines 276-293), add the same check:

```kotlin
detectTapGestures(
    onTap = {
        val skill = item
        if (skill is com.example.game.model.Skill) {
            val isUlt = skill.ultimateGain == 0
            val isUsable = if (isUlt) currentHero.gauge >= 100
                           else (skillCooldowns[skill.id] ?: 0) <= 0
            if (!isUsable) return@detectTapGestures
        }
        // existing pop logic...
    }
)
```

This prevents the card from visually moving/jiggling when the user tries to drag a greyed-out card.

## Files to modify

| File | Changes |
|---|---|
| `ActionTray.kt` | Change `isOnCooldown` to include unready ultimates; simplify color branches; add usability check at drag start and tap to block gesture on greyed cards |

## Dependencies

- None. Standalone fix.
