# Plan 87: Fix Drag Snap-to-Center on Pop

## Problem
Vertically dragging a skill card past the pop threshold causes the card to snap to a horizontally centered position (as if the tap-to-pop behavior is triggering). The card should stay under the user's finger, following `rawDragY` continuously.

## Root Cause
The snap-to-center is not caused by the drag gesture itself (which correctly tracks `rawDragY += dragAmount.y`), but by an interaction between the `scrollOffset` update and the card's horizontal position calculation.

When `isPopped = false` and the user drags:
- The parent `draggable` (or unified handler) updates `scrollOffset` with horizontal delta
- The card's `rawDragX` accumulates horizontal delta from the drag handler
- Card position: `translationX = tx.dp.toPx() + dx` where `tx = relativeIndex * 85f` and `relativeIndex = index - centerIndex + (scrollOffset / 150f)`
- Since `dx = 0` when `!isPopped`, only `tx` affects X position
- If the drag has a horizontal component, `scrollOffset` changes → `tx` changes → card moves horizontally

When `isPopped = true`:
- The card consumes events → no more `scrollOffset` updates
- `dx = rawDragX` (which accumulated some X during pre-pop drag)
- `tx` is now based on the `scrollOffset` at pop time
- The card position at pop time = `tx_final.dp.toPx() + rawDragX`

The "snap to center" happens because `scrollOffset` was drifting toward 0 during the pop (if the drag had a slight horizontal component toward center), and `rawDragX` also accumulated the same component, potentially canceling out and making the card appear to snap to center.

With Plan 86 (unified handler), the horizontal delta is ONLY applied to `scrollOffset` (not to `rawDragX`) when `!isPopped`:

```kotlin
if (!isPopped) {
    scrollOffset = (scrollOffset + dragAmount.x).coerceIn(...)
}
if (poppedCardIndex < 0 || poppedCardIndex == dragActiveIndex) {
    rawDragY += dragAmount.y
    if (!isPopped) rawDragX += dragAmount.x  // ← this line accumulates X even before pop
```

Actually, `rawDragX` IS accumulated even before pop in the unified handler. This is needed so that when `isPopped` becomes true and consumption starts, `rawDragX` already contains the X position offset from the start of the gesture. But `scrollOffset` is ALSO updated with the same X delta. So the card sees its position change via both `tx` (from `scrollOffset`) and `dx` (from `rawDragX`).

This double-accumulation is the problem. When `isPopped` becomes true, `tx` stops changing (no more scrollOffset updates) but `dx = rawDragX` already contains the total X delta since drag start. So the card's X position = `tx_initial + deltaX` (from scrollOffset) + `deltaX` (from rawDragX) = `tx_initial + 2*deltaX`. The card appears to JUMP by `deltaX`.

## Fix

### Option A: Don't accumulate `rawDragX` until after pop (Preferred)

In the unified handler, only accumulate `rawDragX` AFTER `isPopped` becomes true. Before pop, the X delta only goes to `scrollOffset`. After pop, X delta goes to `rawDragX` (for free movement).

```kotlin
onDrag = { change, dragAmount ->
    if (dragActiveIndex >= 0) {
        // Pre-pop: X → scrollOffset only, Y → rawDragY for threshold tracking
        if (!isPopped) {
            scrollOffset = (scrollOffset + dragAmount.x).coerceIn(minScrollOffset, maxScrollOffset)
            rawDragY += dragAmount.y
            rawDragX = 0f  // Keep at 0 — not visually applied yet
        } else {
            // Post-pop: both axes → rawDragX/Y for free movement
            rawDragX += dragAmount.x
            rawDragY += dragAmount.y
        }
        // Pop threshold check
        if (!isPopped && rawDragY < -popThresholdPx
            && abs(rawDragY) > abs(rawDragX) * 1.5f) {
            isPopped = true
        }
    }
    if (isPopped) change.consume()
}
```

### Option B: Invert scrollOffset accumulation after pop

When `isPopped` becomes true, subtract the accumulated X back from `scrollOffset` so the card doesn't double-jump:

```kotlin
var prePopScrollOffset by remember { mutableStateOf(0f) }
...
if (!isPopped && rawDragY < -popThresholdPx ...) {
    isPopped = true
    prePopScrollOffset = scrollOffset
}
```

This approach is more fragile. Option A is cleaner.

## File to Modify
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt`

## Verification
- Drag a card upward → card follows finger, no snap to center
- Card pops at threshold but stays at finger position (rawDragY)
- After pop, drag horizontally → card moves horizontally from its popped position
- After pop, release → card either returns or fires based on threshold
- Tap a card → card snaps to center pop position (tapPopPositionPx) — this is correct tap behavior
- Drag a card at the edge of the hand → card pops and stays at edge position

## Dependencies
- Requires Plan 86 (unified gesture handler) to be applied first
