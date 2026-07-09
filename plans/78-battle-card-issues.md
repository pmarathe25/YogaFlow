# Plan 78: Fix Battle Card Drag, Centering, and Crash Issues

## Problem
Three distinct issues reported after Plan 65 (card deal animation) changes:

### A. Card pops on drag start (regression from `detectDragGestures`)
When the user tries to scroll the hand horizontally by swiping a card, `detectDragGestures` captures ALL drag deltas including the vertical component. `rawDragY` accumulates and quickly crosses `popThresholdPx = 30.dp.toPx()`, causing the card to pop up immediately even on horizontal scrolls.

Previously, `detectVerticalDragGestures` was used, which only reports vertical deltas — horizontal scrolls passed through to `rememberDraggableState` without affecting `rawDragY`.

### B. ActionTray vertically centered instead of bottom-aligned
The `Box` wrapping `ActionTray` at line 444-471 uses `Modifier.zIndex(1f)` and `graphicsLayer { translationY = ... }` but relies on `Modifier.align(Alignment.BottomCenter)` inside a `Column` parent (indirectly through the BattleScene column). If the parent is a `Column`, `align` only works if the `Box` has `Modifier.weight(1f)` or the Column uses `verticalArrangement = SpaceBetween`. The card deal slide animation (`graphicsLayer translationY`) may also position the hand incorrectly if `slideFraction` doesn't reach 0.

Additionally, the `ActionTray` at line 469 has its own `Modifier.align(Alignment.BottomCenter)` inside a `Box`, which in turn is inside the animated `Box`. This double `align` may be problematic.

### C. Crash on turn change to second hero
`key(state.currentActorId)` at line 431 causes the entire block to be removed and re-added when the turn changes (since `state.currentActorId` changes to the next hero's ID). This restarts `remember { mutableStateOf(false) }`, triggers `LaunchedEffect(Unit)` with `delay(50)`, and re-creates all composables. If any state is stale or if `currentHero` is briefly null during the transition, a crash occurs.

The crash likely happens because:
1. `currentHero` is derived from `state.currentActorId` and may be null briefly
2. The `key` swap destroys UI while animation is in progress
3. `ActionTray` receives invalid data mid-transition

## Fix

### Fix A: Switch back to `detectVerticalDragGestures`
In `ActionTray.kt`, revert the card-level pointer input from `detectDragGestures` to `detectVerticalDragGestures`. The `onVerticalDrag` callback provides only the vertical delta, preventing accidental pop from horizontal scrolls.

However, we need to keep `rawDragX` tracking for the horizontal drag-after-pop scenario (Plan 65 Fix E). Modify the `rememberDraggableState` approach:
- When a card is popped (`poppedCardIndex >= 0`), the horizontal scroll gesture `rememberDraggableState` accumulates `rawDragX` correctly
- The `detectVerticalDragGestures` only handles vertical deltas for pop and release
- No more double-counting or cross-talk

### Fix B: Ensure bottom alignment
Remove the inner `Modifier.align(Alignment.BottomCenter)` from `ActionTray` (line 469). The parent `Box` already positions its content — use `Modifier.align(Alignment.BottomCenter)` on the outer `Box` (the animated one) instead, or set `contentAlignment = Alignment.BottomCenter` on it.

Change the animated `Box` to:
```kotlin
Box(
    modifier = Modifier.fillMaxWidth().zIndex(1f)
        .graphicsLayer { translationY = slideFraction * 200.dp.toPx() },
    contentAlignment = Alignment.BottomCenter
) {
    ActionTray(..., modifier = Modifier.fillMaxWidth())
}
```

### Fix C: Stabilize the animation block
Replace `key(state.currentActorId)` with stable identity or add null-safety:

Option 1 (preferred): Remove the `key` block entirely. The animation state (`showHand`, `slideFraction`) is scoped to the `Box` via `remember` and will reset naturally on recomposition. If we want a smoother transition, use `AnimatedContent` instead.

Option 2: Guard with `currentHero != null` check before rendering the block:
```kotlin
if (currentHero != null && state.phase == PLAYER_TURN) {
    val stableId = currentHero.id
    key(stableId) { ... }
}
```

Option 3: Move `key` inside the Box so the `currentHero` reference is stable:
```kotlin
if (currentHero != null && state.phase == PLAYER_TURN) {
    Box(...) {
        key(state.currentActorId) { ... }
    }
}
```

**Recommended:** Guard with `currentHero != null` and remove the `key` block to let Compose handle recomposition naturally. The `remember` + `LaunchedEffect` pattern already handles the animation lifecycle.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt` (Fix A)
- `app/src/main/java/com/example/game/ui/components/BattleScene.kt` (Fix B, Fix C)

## Changes

### ActionTray.kt — Fix A
Replace lines 231-297:
```kotlin
// Replace detectDragGestures with detectVerticalDragGestures
Modifier.pointerInput(index) {
    detectVerticalDragGestures(
        onVerticalDragStart = { startPos ->
            if (poppedCardIndex >= 0 && poppedCardIndex != index) {
                poppedCardIndex = -1; dragActiveIndex = -1; isPopped = false; rawDragY = 0f; rawDragX = 0f
            } else {
                val usable = if (item is Skill) {
                    val s = item; val isUlt = s.ultimateGain == 0
                    if (isUlt) currentHero.gauge >= 100 else (skillCooldowns[s.id] ?: 0) <= 0
                } else if (item is ComboSkill) {
                    !item.requiredHeroes.any { it in actedHeroIds }
                } else true
                if (usable) {
                    if (poppedCardIndex == index && isPopped) { /* already popped */ } else {
                        poppedCardIndex = -1; rawDragX = 0f; rawDragY = 0f; isPopped = false
                    }
                    dragActiveIndex = index; onCardDragStart?.invoke(cardColor)
                }
            }
        },
        onVerticalDrag = { change, dragAmount ->
            if (poppedCardIndex < 0 || poppedCardIndex == index) {
                rawDragY += dragAmount
                if (!isPopped && rawDragY < -popThresholdPx) isPopped = true
                change.consume()
            }
        },
        onDragEnd = {
            if (rawDragY < -thresholdPx) {
                rawDragY = 0f
                when (item) {
                    is Skill -> {
                        val s = item; val isUlt = s.ultimateGain == 0
                        val canUse = if (isUlt) currentHero.gauge >= 100 else (skillCooldowns[s.id] ?: 0) <= 0
                        if (canUse) onSkill(s)
                    }
                    is ComboSkill -> if (!item.requiredHeroes.any { it in actedHeroIds }) onComboSelect(item.id)
                }
            }
            dragActiveIndex = -1; poppedCardIndex = -1; isPopped = false; onCardDragEnd?.invoke()
        },
        onVerticalDragCancel = {
            dragActiveIndex = -1; poppedCardIndex = -1; isPopped = false; onCardDragEnd?.invoke()
        }
    )
}
```

Also remove the `rememberDraggableState` block accumulating X deltas (lines 181-188) if it now only serves the horizontal scroll — the `rememberDraggableState` with `onDragEnd` is needed but the X-delta capture when popped should remain for the drag-after-pop case.

### BattleScene.kt — Fix B
Line 469: Remove `Modifier.align(Alignment.BottomCenter)` from `ActionTray`:
```kotlin
ActionTray(
    ...
    modifier = Modifier.fillMaxWidth()
)
```

Line 444-448: Set `contentAlignment = Alignment.BottomCenter` on the animated `Box`:
```kotlin
Box(
    contentAlignment = Alignment.BottomCenter,
    modifier = Modifier.fillMaxWidth().zIndex(1f)
        .graphicsLayer { translationY = slideFraction * 200.dp.toPx() }
)
```

### BattleScene.kt — Fix C
Line 431: Remove `key(state.currentActorId) { ... }` wrapper, keeping only the null check:
```kotlin
if (currentHero != null && state.phase == PLAYER_TURN) {
    var showHand by remember { mutableStateOf(false) }
    val slideFraction by animateFloatAsState(...)
    LaunchedEffect(Unit) { showHand = false; delay(50); showHand = true }
    Box(...) { ActionTray(...) }
}
```

## Verification
- Open battle → scroll hand left/right → cards do NOT pop up
- Drag a card upward → pop works as expected (threshold ~30dp)
- Tap a card → it pops up; drag after pop moves card X
- ActionTray aligned to bottom of screen, not floating
- Cycle through all heroes' turns → no crash on transition
- Card deal slide-up animation plays correctly on each turn

## Dependencies
- None
