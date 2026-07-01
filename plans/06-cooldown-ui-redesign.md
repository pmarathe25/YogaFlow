# Plan: Redesign Cooldown UI on Skill Cards

## Issue Addressed
- **Issue 6**: The UI for cooldowns is ugly. Move cooldowns to the top-left corner of the cards, simplify the display. Show as a number in a circle. When on cooldown, change the number color to light red. Cooldown of 1 turn should not be displayed.

---

## Current Behavior

In `BattleActions.kt`, the cooldown badge is rendered at `TopEnd` of the skill icon box (lines 521-539):

```kotlin
Box(contentAlignment = Alignment.Center) {
    Text(getSkillIcon(skill), ...)
    
    if (baseCooldown > 0 || isOnCooldown) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    if (isOnCooldown) Color.Red.copy(alpha = 0.8f)
                    else Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isOnCooldown) "$cooldownRemaining" else "CD:$baseCooldown",
                ...
            )
        }
    }
}
```

Problems:
- Positioned at `TopEnd` (top-right) of the icon, not top-left of the card
- Uses a rounded rectangle background instead of a circle
- Shows "CD:1" when cooldown is 1 (undesired — should hide)
- Always shows the base cooldown even when not on cooldown (shows "CD:3" etc.)
- Color is the same regardless of cooldown state (red for active, black for inactive)

---

## Changes Required

### 1. `BattleActions.kt` — `SkillCard` composable

#### Position: Move to top-left corner of the card

Move the cooldown indicator out of the `Box` wrapping the icon, and place it at the card level's top-left corner. The best approach is to wrap the entire card content in a `Box` and position the cooldown badge in the top-left:

```kotlin
Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
    // ... existing card content (icon, name, description, etc.)
    
    // Cooldown badge (top-left, not on icon)
    if (showCooldown) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(24.dp)
                .background(
                    if (isOnCooldown) Color(0xFFFFCDD2)  // light red
                    else Color.Black.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$cooldownRemaining",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOnCooldown) Color(0xFFE53935)  // light red text
                        else Color.DarkGray
            )
        }
    }
}
```

#### Logic: Hide cooldown of 1 turn

```kotlin
val showCooldown = baseCooldown > 1 && cooldownRemaining >= 0
```

- If `baseCooldown <= 1`, never show the cooldown badge.
- If `baseCooldown > 1` and `cooldownRemaining == 0`, show "0" (or the base cooldown number).
- If `baseCooldown > 1` and `cooldownRemaining > 0`, show the remaining turns.

Wait, re-reading the requirement: "When a skill hasn't been used, the cooldown should display as a number in a circle" — so show the base cooldown when not on cooldown. "When the skill is on cooldown, change the number color to a light red." "Also, a cooldown of 1 turn does not need to be displayed."

So:
- `cooldown == 1` → never show badge
- `cooldown > 1`, not on cooldown → show base cooldown number in a circle (dark text, subtle background)
- `cooldown > 1`, on cooldown → show remaining turns in a circle with light red text on light red background

```kotlin
val showCooldown = baseCooldown > 1
val displayText = if (isOnCooldown) "$cooldownRemaining" else "$baseCooldown"
```

#### Visual: Standard cooldown icon

Use a circle shape with the number centered inside. The icon itself can be:
- A circular outline with the number inside
- A filled circle (subtle gray when not active, light red when on cooldown)

Use `CircleShape` instead of `RoundedCornerShape(4.dp)`.

### 2. `BattleActions.kt` — Ensure card greying still works

The existing behavior greys out the card via `.alpha(if (isOnCooldown) 0.8f else 1f)` and changes the background to `Color(0xFFE0E0E0)`. Keep this behavior as-is.

### 3. Summary of visual states

| State | Badge visible? | Position | Shape | Text | Text Color | BG Color |
|-------|---------------|----------|-------|------|-----------|---------|
| cooldown=0 or 1 | No | — | — | — | — | — |
| cooldown>1, not active | Yes | Top-left | Circle | Base CD number | Dark gray | Subtle gray |
| cooldown>1, active cooldown | Yes | Top-left | Circle | Remaining turns | Light red (#E53935) | Light red bg (#FFCDD2) |
