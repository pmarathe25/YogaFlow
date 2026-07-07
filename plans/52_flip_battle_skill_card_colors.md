# Plan 52: Flip Battle Skill Card Colors

## Problem

The battle skill card colors are the opposite of what was intended. Currently:

- **Background** (`bgColor`): Based on skill type (heal = pale green, damage = pale red, ultimate = pale yellow, etc.)
- **Inner border** (`borderColor`): Uses hero's element color (via `heroColor`)

The desired behavior is:
- **Background**: Hero's element color (to visually identify which hero the card belongs to)
- **Inner border**: Skill type color (to indicate the card's function)

## Current State

**`ActionTray.kt:428-439`**:

```kotlin
val bgColor = when {
    isOnCooldown                     -> Color(0xFFE0E0E0)
    isUltimate                       -> Color(0xFFFFF9C4)
    skill.healScaling != null        -> Color(0xFFF1F8E9)
    skill.damageComponents.isNotEmpty() -> Color(0xFFFFF1F0)
    else                             -> Color(0xFFE1F5FE)
}

val borderColor = when {
    isOnCooldown                     -> Color.Gray
    isUltimate                       -> Color(0xFFFFD700)
    else                             -> heroColor
}
```

## Fix

### 1. Swap bgColor and borderColor logic

**`ActionTray.kt`** — change `bgColor` to use `heroColor` (with appropriate alpha) and `borderColor` to use skill-type-based colors.

New `bgColor` (use hero's element color, toned down with alpha):
```kotlin
val bgColor = when {
    isOnCooldown -> Color(0xFFE0E0E0)
    isUltimate   -> Color(0xFFFFF9C4)            // keep gold bg for ultimates (they're special)
    else         -> heroColor.copy(alpha = 0.15f) // hero element color, very transparent
}
```

New `borderColor` (skill-type-based):
```kotlin
val borderColor = when {
    isOnCooldown                     -> Color.Gray
    isUltimate                       -> Color(0xFFFFD700)  // gold border for ultimates
    skill.healScaling != null        -> Color(0xFF689F38)  // green
    skill.damageComponents.isNotEmpty() -> Color(0xFFD32F2F) // red
    skill.shieldScaling != null      -> Color(0xFF0288D1)  // blue
    skill.buffs.isNotEmpty()         -> Color(0xFFFFA000)  // amber
    else                             -> Color(0xFF0288D1)  // blue fallback
}
```

### 2. Keep cooldown/ultimate visual treatment

Cards on cooldown still get grey background + grey border. Ultimate cards still get gold background + gold border (with pulsing glow). Only regular skill cards change.

### 3. Update border alpha

Keep the existing border alpha for non-ultimate cards:
```kotlin
borderColor.copy(alpha = 0.6f)
```

## Result

| Card state | Before (bg / border) | After (bg / border) |
|---|---|---|
| Regular skill | pale-type-color / hero-color | hero-color-tint / type-color |
| Heal skill | pale green / hero-color | hero-color-tint / green |
| Damage skill | pale red / hero-color | hero-color-tint / red |
| Shield skill | pale blue / hero-color | hero-color-tint / blue |
| On cooldown | grey / grey | grey / grey |
| Ultimate ready | pale yellow / gold | pale yellow / gold |

## Files to modify

| File | Changes |
|---|---|
| `ActionTray.kt` | Swap `bgColor` to use `heroColor.copy(alpha = 0.15f)` for regular skills; swap `borderColor` to use skill-type-based colors; keep cooldown and ultimate special cases unchanged |

## Dependencies

- None. Standalone visual fix.
