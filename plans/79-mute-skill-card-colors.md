# Plan 79: Mute Skill Card Colors to Eggshell + Tint

## Problem
Skill card backgrounds use `heroColor.copy(alpha = 0.85f)` which produces a strong, saturated card color. For example, a fire hero gets a bright red card, water hero gets bright blue, etc. This is visually overwhelming — cards should feel paper-like with just a subtle hint of the hero's element color.

## Fix
Replace the saturated `heroColor.copy(alpha = 0.85f)` with an **eggshell base** (`#F5EEDC`) blended with a small fraction of the hero's element color using `Color.lerp`. The border color (`skillCardBorderColor`) and drag overlay color (`getCardColor`) remain as-is since they serve functional purposes (type identification, drag feedback).

- Base color: eggshell `Color(0xFFF5EEDC)` (warm off-white)
- Tint: 15% hero color, 85% eggshell
- Ultimate: keep `Color(0xFFFFF9C4)` (warm gold tint instead of full gold)
- Cooldown: keep `Color(0xFFE0E0E0)` (muted gray)

For example:
- Shanti (water/blue): card becomes pale blue-beige (#EAEEF5)
- Agni (fire/red): card becomes pale warm beige (#F5EAE8)
- Ultimate: pale gold (#FFF9C4)

Also mute the `bgColor` in `ComboCard` (if it exists) for consistency.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt`

## Changes

### ActionTray.kt — line ~460
Replace the `bgColor` calculation:

**Before:**
```kotlin
val bgColor = when {
    isOnCooldown -> Color(0xFFE0E0E0)
    isUltimate   -> Color(0xFFFFF9C4)
    else         -> heroColor.copy(alpha = 0.85f)
}
```

**After:**
```kotlin
val eggshell = Color(0xFFF5EEDC)

val bgColor = when {
    isOnCooldown -> Color(0xFFE0E0E0)
    isUltimate   -> Color(0xFFFFF9C4)
    else         -> Color.lerp(eggshell, heroColor, 0.15f)
}
```

Add the `lerp` import if not present:
```kotlin
import androidx.compose.ui.graphics.lerp
```

### ComboCard (if applicable)
Check if `ComboCard` has a similar `bgColor` that needs muting. Verify after writing.

## Verification
- Open battle for each hero element → cards show eggshell/off-white background with very subtle tint of the hero's element
- Ultimate cards still show warm gold tint
- On-cooldown cards show muted gray
- Text (skill name, description) remains readable against pale background
- Border colors unchanged (still show DAMAGE=red, HEAL=green, BUFF=blue)

## Dependencies
- None (isolated visual change)
