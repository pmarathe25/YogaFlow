# Plan 40: Fix Hero Details Skill Colors

## Problem

The `HeroDetailSkillCard` on the hero details page uses skill-function-based colors for the indicator bar and text (heal = green, damage = red per element, shield = blue, etc.). This is inconsistent with the battle UI direction where skill cards use the hero's element color (see Plan 36).

All skill cards for a given hero should use that hero's element color to visually tie them together, regardless of the skill's function.

## Fix

### 1. Replace `typeColor` with `heroColor`

**`PartyScreen.kt:445-461`** — change `HeroDetailSkillCard` to use `heroColor` for all skills instead of the skill-type-based `typeColor`:

Before:
```kotlin
val typeColor = when {
    isUltimate -> heroColor
    skill.healScaling != null -> Color(0xFF66BB6A)
    skill.shieldScaling != null -> Color(0xFF42A5F5)
    skill.damageComponents.any { it.element == Element.FIRE } -> Color(0xFFE53935)
    skill.damageComponents.any { it.element == Element.WATER } -> Color(0xFF1E88E5)
    ...
    else -> MaterialTheme.colorScheme.onSurface
}
```

After:
```kotlin
val typeColor = heroColor   // all skills use hero's element color
```

### 2. Adjust color usage throughout the card

Since `typeColor` is now `heroColor` (same for all skills of a hero), ensure the color is still visually useful:
- The colored indicator bar (line 489-490) will use `heroColor` — good, this identifies the hero.
- The skill name text (line 499) uses `typeColor` — fine, it's now hero-colored.
- The type label background (line 504) uses `typeColor.copy(alpha = 0.15f)` — fine.
- The damage text (line 526) uses `typeColor` — still readable since hero colors are generally vibrant.

### 3. Keep the `typeLabel` text

The `typeLabel` (line 463-473) should remain — it still identifies whether the skill is "Heal", "Shield", "Buff", "Physical", etc. Only the color changes.

## Files to modify

| File | Changes |
|---|---|
| `PartyScreen.kt` | Change `typeColor` in `HeroDetailSkillCard` to always use `heroColor`; remove skill-function-based color branching |

## Dependencies

- None. Standalone visual fix.
