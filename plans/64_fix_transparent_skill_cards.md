# Plan 64: Fix Transparent Skill Cards

## Problem
Battle skill cards (`SkillCard` in `ActionTray.kt:454`) use `heroColor.copy(alpha = 0.25f)` as the background color for non-cooldown, non-ultimate cards. The hero element color at 25% opacity is too transparent — the wooden table background and other UI elements show through, making the card content (icon, name, description) hard to read.

Plan 54 had already increased this from 0.15f to 0.25f but that was insufficient.

## Fix
Increase the background alpha to make skill cards nearly opaque while retaining a subtle tint of the hero's element color:

**`ActionTray.kt:454`**:
```kotlin
// Before:
else -> heroColor.copy(alpha = 0.25f)

// After:
else -> heroColor.copy(alpha = 0.85f)
```

This gives a strong but still slightly translucent background that clearly shows the hero element identity without washing out card text content.

For `ComboCard` (`ActionTray.kt:378`), the container color is `Color(0xFF4A148C).copy(alpha = 0.3f)` — increase this to `0.85f` as well for consistency:

```kotlin
// Before:
colors = CardDefaults.cardColors(containerColor = Color(0xFF4A148C).copy(alpha = 0.3f))

// After:
colors = CardDefaults.cardColors(containerColor = Color(0xFF4A148C).copy(alpha = 0.85f))
```

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt` — line 454: `0.25f` → `0.85f`; line 378: `0.3f` → `0.85f`

## Dependencies
- Reverts the ineffective alpha bump from Plan 54
