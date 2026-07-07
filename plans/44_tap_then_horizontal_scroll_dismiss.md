# Plan 44: Tap Then Horizontal Scroll Dismisses Popped Card

## Problem

When a skill card is tapped and popped up in the battle UI, horizontally scrolling the hand of cards does not dismiss the popped card back to the hand. The popped card remains floating while the hand scrolls underneath, creating a confusing state.

Additionally, if the user drags horizontally **on the popped card itself**, it should drag the card (for aiming the skill, `rawDragX`) rather than scrolling the entire hand.

## Current Behavior

- `poppedCardIndex` remains set when the user starts a horizontal scroll on the outer `Box`.
- The outer `draggable(Orientation.Horizontal)` scrolls the fan while the popped card stays in its lifted position.
- The popped card's `pointerInput` block handles horizontal movement as "aiming" (`rawDragX`), but the outer `draggable` may also consume it, causing both to fight.

## Fix

### 1. Dismiss popped card on horizontal scroll start

**`ActionTray.kt:188-196`** — in the outer `Box`'s `draggable` handler, dismiss any popped card when a horizontal drag begins:

```kotlin
val draggableState = rememberDraggableState { delta ->
    if (poppedCardIndex >= 0) {
        // Dismiss popped card on first horizontal scroll
        poppedCardIndex = -1
        dragActiveIndex = -1
        isPopped = false
        rawDragY = 0f
    }
    scrollOffset = (scrollOffset + delta).coerceIn(minScrollOffset, maxScrollOffset)
}
```

This clears the popped state immediately when the user initiates a horizontal scroll gesture.

### 2. Consume horizontal drag on popped card

**`ActionTray.kt:219-286`** — in the per-card `pointerInput` block, when a card is already popped (`isPopped == true`), consume horizontal drag events to prevent them from reaching the outer `draggable`:

Modify the `onVerticalDrag` handler so that after the card is popped, horizontal movement is consumed by the card's own pointer input (for `rawDragX` aiming) and not forwarded:

```kotlin
onVerticalDrag = { change: PointerInputChange, dragAmountY: Float ->
    if (poppedCardIndex >= 0 && poppedCardIndex != index) {
        // This card is not the popped one — ignore
        return@onVerticalDrag
    }
    rawDragY += dragAmountY
    if (!isPopped) {
        if (rawDragY < -popThresholdPx) {
            isPopped = true
            lastDragX = change.position.x
        } else {
            lastDragX = change.position.x
        }
    } else {
        // When popped, consume horizontal movement for aiming
        val currentX = change.position.x
        val dx = currentX - lastDragX
        rawDragX += dx
        lastDragX = currentX
        // Mark horizontal motion as consumed so outer draggable doesn't get it
        change.consume()
    }
}
```

The key addition is `change.consume()` in the horizontal-tracking branch, which prevents the pointer event from being propagated to the outer `draggable`.

### 3. Combine drag and tap into a single `pointerInput`

Alternatively, combine both `dragMod` and `tapMod` into a single `pointerInput` block using `awaitEachGesture` for full control over gesture disambiguation:

```kotlin
Modifier.pointerInput(index) {
    awaitEachGesture {
        // Wait for first pointer down
        val down = awaitFirstDown(requireUnconsumed = false)
        // If card is popped and we get horizontal drag -> dismiss
        // If card is popped and we get vertical drag -> aim/throw
        // If card is not popped and we get vertical drag -> pop
        // If card is not popped and we get horizontal drag -> let outer scroll handle it
    }
}
```

This is more complex but gives precise control. The simpler approach (steps 1-2 above) is preferred.

## Files to modify

| File | Changes |
|---|---|
| `ActionTray.kt` | Dismiss popped card when horizontal scroll starts in outer `draggable`; consume horizontal events on popped card to prevent outer scroll; add `change.consume()` in horizontal tracking branch |

## Dependencies

- None. Standalone gesture fix.
