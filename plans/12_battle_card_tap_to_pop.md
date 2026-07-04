# Plan: Tap Skill Card to Pop It Out of Hand (Instead of Full-Screen Overlay)

## Problem

Tapping a skill/combo card in battle opens a full-screen modal overlay with a semi-transparent black backdrop and an enlarged card. This obscures the battle entirely and requires an extra dismiss step. The user wants the tap to behave exactly like dragging the card partway out of the hand — lifting it above the other cards, floating just below the play area, ready to be dragged further to play or tapped again to dismiss.

## Current Behavior

1. **Tap a card** → `inspectedCardItem` is set → entire screen covered by `Box` with `Color.Black.copy(alpha = 0.5f)` → enlarged `SkillCard`/`ComboCard` centered
2. **Tap the backdrop** → `inspectedCardItem = null` → overlay dismissed
3. **Drag a card** → card follows finger up, pops out at 30dp, plays at 200dp

## Desired Behavior

1. **Tap a card** → the card springs up above the hand to a fixed "popped" position just below the play area, with the same visual transforms as a drag-pop (scale 1.15x, rotationZ 0, elevated zIndex, spring animation)
2. **Tap the same card again** → it springs back into the hand
3. **Tap a different card** → the first card springs back, the new card pops up
4. **Drag the popped card** → behaves exactly as though the card had been dragged from the hand originally: dragging it further up into the play area (past the 200dp threshold) executes the skill; releasing it below the play area (before the threshold) makes it spring back to the hand
5. **Drag a different card** while one is popped → the popped card returns, new card drags normally

## Implementation

### Step 1: Remove the full-screen overlay from BattleScene.kt

**File:** `app/src/main/java/com/example/game/ui/components/BattleScene.kt`

- **Line 71:** Remove `var inspectedCardItem by remember { mutableStateOf<Any?>(null) }`
- **Line 415:** Remove the `onCardTap = { item -> inspectedCardItem = item }` argument from the `ActionTray` call
- **Lines 471-497:** Remove the entire `if (inspectedCardItem != null) { ... }` overlay block

### Step 2: Remove the `onCardTap` callback from ActionTray

**File:** `app/src/main/java/com/example/game/ui/components/ActionTray.kt`

- **Line 58:** Remove `onCardTap: ((Any) -> Unit)? = null` parameter
- **Line 107:** Remove the `onCardTap` parameter from the `HandOfCards` call
- **Line 141:** Remove `onCardTap: ((Any) -> Unit)? = null` parameter from `HandOfCards`

### Step 3: Add popped card state to HandOfCards

**File:** `app/src/main/java/com/example/game/ui/components/ActionTray.kt`

After line 151 (`var scrollOffset`), add:

```kotlin
var poppedCardIndex by remember { mutableIntStateOf(-1) }
```

This tracks which card (by index) is currently popped via tap. `-1` = none.

Define a pop position constant. After line 150 (`val thresholdPx`), add:

```kotlin
val tapPopPositionPx = with(density) { 130.dp.toPx() }
```

This is how far above its normal position the card floats when tapped. ~130dp is enough to sit clearly above the hand arc while remaining below the 200dp play threshold, so the user can drag the remaining ~70dp to execute the skill.

### Step 4: Replace the tap modifier to set popped state directly

**File:** `app/src/main/java/com/example/game/ui/components/ActionTray.kt`, lines 257-261

Replace the current `tapMod`:

```kotlin
// Before:
val tapMod = if (item is com.example.game.model.Skill || item is ComboSkill) {
    Modifier.pointerInput(index) {
        detectTapGestures { onCardTap?.invoke(item) }
    }
} else Modifier

// After:
val tapMod = if (item is com.example.game.model.Skill || item is ComboSkill) {
    Modifier.pointerInput(index) {
        detectTapGestures {
            if (poppedCardIndex == index) {
                // Card already popped — tap again to dismiss
                poppedCardIndex = -1
                dragActiveIndex = -1
                isPopped = false
                rawDragY = 0f
            } else {
                // Pop this card to the fixed position
                poppedCardIndex = index
                dragActiveIndex = index
                isPopped = true
                rawDragY = -tapPopPositionPx
                rawDragX = 0f
            }
        }
    }
} else Modifier
```

### Step 5: Handle drag-end from popped state

**File:** `app/src/main/java/com/example/game/ui/components/ActionTray.kt`, lines 230-247

The existing `onDragEnd` handler already checks `if (rawDragY < -thresholdPx)` to decide whether to execute. Since `rawDragY` already has the pop offset (`-tapPopPositionPx`), the user only needs to drag an additional `thresholdPx - tapPopPositionPx` (~70dp) upward to trigger execution. On release without enough drag, the card springs back to its normal position (all state resets).

The `onDragEnd` handler also already resets `dragActiveIndex = -1`, `isPopped = false`, and calls `onCardDragEnd?.invoke()`. The `poppedCardIndex` should also be reset here:

```kotlin
onDragEnd = {
    if (rawDragY < -thresholdPx) {
        rawDragY = 0f
        when (item) {
            is com.example.game.model.Skill -> { /* execute skill */ }
            is ComboSkill -> onComboSelect(item.id)
        }
    }
    dragActiveIndex = -1
    poppedCardIndex = -1    // ← add this
    isPopped = false
    onCardDragEnd?.invoke()
},
```

Also add `poppedCardIndex = -1` in the `onDragCancel` handler (line 248-253).

### Step 6: Handle drag start while a card is popped

**File:** `app/src/main/java/com/example/game/ui/components/ActionTray.kt`, lines 207-213

The current `onDragStart` unconditionally resets `rawDragY = 0f` and `isPopped = false`. When a card was already popped via tap and the user begins dragging it, these resets would yank the card back to the hand. The handler must preserve the popped card's position.

Replace the `onDragStart` handler:

```kotlin
onDragStart = { startPos ->
    if (dragActiveIndex == index && isPopped) {
        // This card was popped via tap and is now being dragged.
        // Preserve its popped position — the drag continues from here.
        // Don't reset rawDragY, isPopped, or poppedCardIndex.
        poppedCardIndex = -1    // tap-pop is done; drag takes over
        lastDragX = startPos.x
    } else {
        // Normal drag start (or a different card was popped)
        poppedCardIndex = -1
        rawDragX = 0f
        rawDragY = 0f
        isPopped = false
        lastDragX = startPos.x
    }
    dragActiveIndex = index
    onCardDragStart?.invoke(cardColor)
},
```

This way:
- **Dragging the popped card**: `rawDragY` stays at `-tapPopPositionPx` (~-130dp), the card remains elevated. `onVerticalDrag` adds the new drag amount on top. If total passes -200dp, release executes the skill.
- **Dragging a different card**: The popped card's state is reset silently and the new card drags normally.

## Visual Behavior Summary

| Action | What happens |
|--------|-------------|
| Tap a card that's in the hand | Card springs up ~130dp above its hand position, straightens (rotationZ=0), scales 1.15x, zIndex 999; other cards remain in hand |
| Tap the same card again | Card springs back to its hand position (rotation, scale, zIndex restored) |
| Tap a different card | First card returns to hand, new card pops up |
| Drag a different card | Popped card returns silently, new card drags normally |
| Drag the popped card up | Existing drag handler takes over; if total Y displacement exceeds 200dp on release, skill executes |
| Release popped card without enough drag | Card springs back to hand (existing spring animation) |

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/java/com/example/game/ui/components/BattleScene.kt` | Remove `inspectedCardItem` state (line 71), remove `onCardTap` callback (line 415), remove overlay block (lines 471-497) |
| `app/src/main/java/com/example/game/ui/components/ActionTray.kt` | Remove `onCardTap` parameter (lines 58, 107, 141); add `poppedCardIndex` state and `tapPopPositionPx` constant; rewrite `tapMod` to set popped state directly; add `poppedCardIndex = -1` resets in `onDragStart`, `onDragEnd`, `onDragCancel` |
