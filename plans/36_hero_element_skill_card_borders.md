# Plan 36: Hero Element-Colored Skill Card Borders in Battle

## Problem

Battle skill cards use a skill-type-based inner border color (heal = green, damage = red, buff = blue, etc.). This makes all heroes' cards look the same regardless of their element. The cards should visually communicate which hero they belong to by using the hero's element color for the inner border (e.g., blue for Water-element Shanti, red for Fire-element heroes).

The outer border/glow for ultimates can remain gold (to signify ultimate status), but the inner card border should use the hero's element color for all non-ultimate cards.

## Fix

### 1. Pass hero element color to `SkillCard`

**`ActionTray.kt`** — `SkillCard` currently doesn't receive the hero's element or color. The call site in `HandOfCards` (around line 306) has access to `currentHero`. Add a `heroColor: Color` parameter to `SkillCard`:

```kotlin
@Composable
private fun SkillCard(
    item: Any,
    heroColor: Color,               // new parameter
    currentHero: Hero,
    skillCooldowns: Map<String, Int>,
    ...
)
```

At the call site:
```kotlin
SkillCard(
    item = item,
    heroColor = elementToColor(currentHero.element),
    currentHero = currentHero,
    ...
)
```

### 2. Replace skill-type-based border with hero element color

**`ActionTray.kt:412-418`** — change `borderColor` to use `heroColor` for all non-cooldown, non-ultimate cards:

```kotlin
val borderColor = when {
    isOnCooldown              -> Color.Gray
    isUltimate                -> Color(0xFFFFD700)  // keep gold for ultimates
    else                      -> heroColor
}
```

Keep the existing alpha: `.copy(alpha = 0.6f)`.

### 3. Keep skill-type-based background color

The background color (`bgColor`) can remain skill-type-based (heal green, damage red, etc.) to still convey the skill's function at a glance. Only the inner border changes to reflect the hero's element.

## Files to modify

| File | Changes |
|---|---|
| `ActionTray.kt` | Add `heroColor` param to `SkillCard`; pass `elementToColor(currentHero.element)` at call site; replace skill-type-based `borderColor` with `heroColor` |

## Dependencies

- None. Standalone visual change.
