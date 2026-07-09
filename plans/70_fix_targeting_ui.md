# Plan 70: Fix Targeting UI — Move Prompt, Shorten Text, Restore Highlighting

## Problem
Three issues with the target selection UI during battle:

### 1. "Select a target" prompt blocks hero sprites
The prompt text "Select a target by clicking them" is rendered inside `ActionTray` (ActionTray.kt:112-128) with `Modifier.align(Alignment.TopCenter)`. Since the ActionTray sits at the bottom of the BattleScene (BattleScene.kt:408-431), the prompt appears just above the skill cards — right at the same vertical level as the hero sprites in the hero zone (BattleScene.kt:316-394). The dark semi-transparent background box (`Color.Black.copy(alpha = 0.7f)`) overlaps the hero sprites, making them harder to see and tap.

### 2. Text is unnecessarily verbose
"Select a target by clicking them" is ~30 characters. This can be shortened to "Select a target" (~15 chars) — the "by clicking them" part is obvious from the interaction context (the user just tapped a skill card, so they know they need to click).

### 3. Valid-target highlighting is broken
The `isTargeted` glow is only applied when the user has already clicked a valid target (`selectedTargets.contains(id)`), which means the glow only appears **after** the target is selected — not before. The valid-target glow (`CombatantSprite.kt:144` renders a pulsing border when `isTargeted = true`) should be active for all valid targets during targeting mode, not just the one that was clicked. This helps the user see which targets are clickable.

## Fix

### Fix 1: Move prompt to the bottom center of the screen, above ActionTray
Move the targeting prompt from `ActionTray` to `BattleScene`, positioned at the bottom center of the hero zone (above the ActionTray but below the hero sprites). This keeps it visible without overlapping the heroes.

**In `BattleScene.kt`**, add the prompt after the hero zone Row (after line 394) as a separate Box aligned to BottomCenter:

```kotlin
// After hero zone Row (line ~394), inside the Column at line 239:
if (isTargeting) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 4.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Select a target",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(16.dp))
            TextButton(onClick = {
                viewModel.cancelAction()
                selectedTargets.clear()
            }) {
                Text("CANCEL", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
    }
}
```

**Remove the old prompt** from `ActionTray.kt` (lines 112-128).

### Fix 2: Shorten text
Already done in Fix 1 — changed from "Select a target by clicking them" to "Select a target".

### Fix 3: Highlight all valid targets during targeting mode
In `BattleScene.kt`, change the `isTargeted` computation for both monsters and heroes to reflect whether the entity is a **valid clickable target** during targeting mode, not just whether it has been clicked:

**For monster (lines 264-267):**
```kotlin
val canTarget = state.pendingSkill?.let {
    it.targetType == TargetType.SINGLE_ENEMY || it.targetType == TargetType.ALL_ENEMIES || it.targetType == TargetType.ALL
} ?: false
val isTargeted = isTargeting && canTarget
```

**For heroes (lines 335-343):**
```kotlin
val canTarget = state.pendingSkill?.let { skill ->
    when (skill.targetType) {
        TargetType.SINGLE_ALLY -> hero.id != state.currentActorId
        TargetType.ALL_ALLIES, TargetType.ALL -> true
        TargetType.SELF -> hero.id == state.currentActorId
        else -> false
    }
} ?: false
val isTargeted = isTargeting && canTarget
```

This replaces the current `selectedTargets.contains(...)` logic. The `selectedTargets` list was only ever used for this glow effect and is set/reset in click handlers — it no longer needs to drive the glow state.

Remove the `selectedTargets` state variable (line 107) if it becomes unused.

### Fix 4: Ensure monsters still show the scale-up on valid target
The monster already has a scale-up effect on `isTargeted` (lines 304-308):
```kotlin
if (isTargeted) { scaleX = 1.65f; scaleY = 1.65f }
```
With Fix 3, this will now fire for all valid targets during targeting, not just after click — exactly the desired behavior.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/BattleScene.kt` — remove `selectedTargets` state; change `isTargeted` computation for both monsters and heroes to check `isTargeting && canTarget`; add new targeting prompt Box after hero zone
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt` — remove targeting prompt Box (lines 112-128)

## Dependencies
- None
