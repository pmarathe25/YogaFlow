# Plan 24: Loosen Hand Scroll Limits

## Problem

Plan 14's scroll limits are too tight. They clamp `scrollOffset` so that each extreme card's **edge** stays on screen, but the user wants to be able to scroll each extreme card all the way to the **center** of the screen.

Recall the fan layout (`ActionTray.kt:183-187`):

```
relativeIndex = index - centerIndex + (scrollOffset / 150f)
rotation = relativeIndex * 12f
tx = relativeIndex * 85f       (dp from center)
ty = relativeIndex² * 10f       (dp drop)
```

Current limits (edge-visible):
- **Max** (scroll right): card 0's left edge touches screen left edge
- **Min** (scroll left): card N-1's right edge touches screen right edge

For 5 cards on a 360dp screen this gives about ±115px of range — not enough to bring either extreme card near center.

## Fix

Change the limit bounds so each extreme card can reach `relativeIndex ≈ 0` (center position with 0° rotation).

### New formula

For card 0 to be centered: `relativeIndex_0 = 0`
→ `0 - centerIndex + scrollOffset / 150f = 0`
→ `scrollOffset = centerIndex × 150`

For card N-1 to be centered: `relativeIndex_{N-1} = 0`
→ `(N-1) - centerIndex + scrollOffset / 150f = 0`
→ `scrollOffset = -(N-1 - centerIndex) × 150`

```
maxScrollOffset = centerIndex × 150f
minScrollOffset = -(cardCount - 1 - centerIndex) × 150f
```

These depend only on card count, not screen width. No `BoxWithConstraints` needed.

### Worked ranges

| Cards | centerIndex | minOffset | maxOffset | Range | vs old (360dp) |
|---|---|---|---|---|---|
| 5 | 2.0 | -300 | +300 | 600px | ~5.2× looser |
| 4 | 1.5 | -225 | +225 | 450px | — |
| 3 | 1.0 | -150 | +150 | 300px | ~3.4× looser |
| 2 | 0.5 | -75 | +75 | 150px | — |
| 1 | 0.0 | 0 | 0 | 0px | no scrolling |

### Implementation

Replace the runtime bounds computation with a simple `remember(cardCount)`:

```kotlin
// ActionTray.kt, after line 149:
val (minScrollOffset, maxScrollOffset) = remember(cardCount) {
    val center = (cardCount - 1) / 2f
    val maxOff = center * 150f
    val minOff = -(cardCount - 1 - center) * 150f
    minOff to maxOff
}
```

Then clamp the drag delta:

```kotlin
val draggableState = rememberDraggableState { delta ->
    scrollOffset = (scrollOffset + delta).coerceIn(minScrollOffset, maxScrollOffset)
}
```

### Remove BoxWithConstraints

Since the bounds no longer depend on screen width, revert to a plain `Box` instead of `BoxWithConstraints` (if it was changed). The old `Box` on line 175 can stay as-is.

## Files to modify

| File | Changes |
|---|---|
| `ActionTray.kt:149` | Add `remember(cardCount)` computed bounds |
| `ActionTray.kt:172` | Clamp `scrollOffset` with `coerceIn(minScrollOffset, maxScrollOffset)` |

## Dependencies

Replaces the scroll-limit logic from Plan 14.
