# Plan 65: Fix Card Dragging — All Sub-Issues

## Problem
The battle card drag system has six distinct bugs, all introduced or left unfixed by the combination of Plans 55, 56, and 63:

### Bug A: Direct-drag jitter (card vibrates horizontally before settling)
When a card is dragged directly out of the hand (NOT popped first by tapping), it vibrates/jitters horizontally for a few moments before eventually settling. After settling, vertical drag works fine but horizontal drag has a scaling issue (see Bug G).

Root causes:
- Plan 55's `Animatable` snapback system (`snapBackY`, `snapBackX`) reads values in `graphicsLayer` (lines 212-213), adding per-frame overhead. During drag the `Animatable` may briefly oscillate before settling.
- `onDragStart` resets all pop state (`poppedCardIndex = -1`, `rawDragX = 0f`, etc.) on every direct drag start (lines 248-254). If the drag starts with slight horizontal jitter (finger vs touch sensor), the state resets repeatedly.
- The card transitions from fan position (`translationX = tx.dp.toPx()` at the fan angle) to pop position abruptly when `isPopped` flips mid-drag, causing a visual jerk.

### Bug B: Horizontal drag on popped card prevents subsequent vertical drag
When the user horizontally drags a popped card, the outer `draggableState` (line 187-189) redirects the delta to `rawDragX` but does NOT set `dragActiveIndex = poppedCardIndex`. When the user then tries to drag vertically, `onDragStart` (line 230) checks `if (dragActiveIndex == index && isPopped)` which is false (since `dragActiveIndex` was never set), so it falls into the `else` branch (lines 248-254) which resets `isPopped = false`, `rawDragY = 0f`, `rawDragX = 0f` — dismissing the pop entirely.

### Bug C: Popped card not centred — appears on the right
Plan 63's centering formula in `graphicsLayer` (lines 214-216):
```kotlin
translationX = (parentWidthPx - cardWidthPx) / 2f + dx
```
This assumes the card's default position is at screen-left (0,0). But the parent `Box` uses `Alignment.BottomCenter` (line 199), which already centres all children by default. The centering formula then adds `(parentWidthPx - cardWidthPx) / 2f` which pushes the card an extra half-screen to the right, placing it at the ¾ point instead of centre.

### Bug D: Can't scroll the hand when a card is popped
Plan 56's redirect in `draggableState` (lines 187-189) sends ALL horizontal drag to `rawDragX` whenever `poppedCardIndex >= 0`. This means the user cannot scroll the card fan at all while any card is popped — even if they drag outside the popped card area or on a different card.

### Bug E: X position snaps when pop threshold is crossed while dragging
In `onVerticalDrag` (lines 263-268), `rawDragX` only accumulates after `isPopped` becomes true. While dragging vertically downward, before crossing the pop threshold, no X deltas are recorded. When the threshold is crossed and `isPopped` flips to true, `rawDragX` is still 0, so the card's horizontal position snaps to centre (its `translationX = dx` value at dx=0). The finger's horizontal drift during the downward drag is lost.

### Bug F: Tapping to pop then dragging immediately dismisses the pop
When the user taps a card (via `detectTapGestures`), it sets `poppedCardIndex = index`, `isPopped = true`, `rawDragY = -tapPopPositionPx`. When the user then starts vertical dragging, `onDragStart` fires. The branch `poppedCardIndex >= 0 && poppedCardIndex != index` is false (they match). In the else branch, `dragActiveIndex == index && isPopped` is true (both are set by the tap), so it sets `poppedCardIndex = -1` — dismissing the pop. The user must then drag further to re-pop the card.

### Bug G: Horizontal drag on popped card moves at ~0.5x speed
When a card is popped (either by tap or by direct drag) and then dragged horizontally, the card's horizontal movement is approximately half the speed of the finger. For example, moving the finger from one side of the screen to the other only moves the card to the middle.

Root cause analysis:
- `detectVerticalDragGestures` (line 229) fires `onVerticalDrag` with both `dragAmountY` (vertical delta filtered from the gesture) and `change.position` (raw pointer position relative to the element). The horizontal delta is computed as `change.position.x - lastDragX` (line 268).
- The outer `draggable` (line 186) with `Orientation.Horizontal` competes for the same pointer events. Although the inner handler calls `change.consume()` (line 271), the outer `draggable` may still intercept the initial horizontal portion of the drag before the inner handler fully claims the gesture. The outer `draggableState` also adds to `rawDragX` (line 188-189), but with different delta values (the `draggable` reports the horizontal component of pointer movement, which may differ from `change.position.x - lastDragX` due to coordinate-space / filtering differences).
- However, since the outer redirect only fires when `poppedCardIndex >= 0` (line 187), and the inner only fires when `poppedCardIndex < 0 || poppedCardIndex == index` (line 261), both can fire for the same event. Both accumulate into `rawDragX`, but with different delta values — the outer's raw horizontal delta and the inner's `change.position.x - lastDragX`. If one is ~0.5x and the other is ~0.5x, the behavior would appear ~1x. But if only one fires (inner claims the gesture and outer is suppressed), the card moves at half speed, suggesting the inner's delta computation is effectively halved.
- **Likely cause**: `detectVerticalDragGestures` processes the pointer event in a way that partially filters out horizontal movement. The `PointerInputChange`'s `position` may be relative to the element's original frame (pre-graphicsLayer transform), but the outer `draggable` uses a different coordinate basis. The fix should switch to a custom gesture detector or use `detectDragGestures` (non-oriented) for the popped card to get unfiltered deltas.

## Fixes

### Fix A: Remove `Animatable` snapback, revert to `animateFloatAsState`
Remove `snapBackY`, `snapBackX`, and the `LaunchedEffect(isDragged)`. The pre-Plan-55 approach of `animateFloatAsState` with `snap()` during drag and `spring()` on release is simpler and correct.

Replace `Animatable` declarations (lines 169-177):
```kotlin
// REMOVE these:
val snapBackY = remember { Animatable(0f) }
val snapBackX = remember { Animatable(0f) }
LaunchedEffect(isDragged) {
    if (!isDragged) {
        snapBackY.snapTo(0f)
        snapBackX.snapTo(0f)
    }
}
```

Replace `graphicsLayer` reads (lines 212-213):
```kotlin
// BEFORE:
val dy = if (isDragged) rawDragY else snapBackY.value
val dx = if (isDragged && isPopped) rawDragX else snapBackX.value

// AFTER:
val dy = if (isDragged) rawDragY else 0f
val dx = if (isDragged && isPopped) rawDragX else 0f
```

The `animateFloatAsState` animation at lines 169-180 already handles smooth spring-back by switching from `snap()` (during drag) to `spring()` (after release), so raw reads in `graphicsLayer` are fine.

### Fix B: Set `dragActiveIndex` in the outer `draggable` redirect
In `draggableState` (lines 186-192), when redirecting horizontal deltas to `rawDragX`, also set `dragActiveIndex`:

```kotlin
val draggableState = rememberDraggableState { delta ->
    if (poppedCardIndex >= 0) {
        rawDragX += delta
        dragActiveIndex = poppedCardIndex  // ADD THIS
        return@rememberDraggableState
    }
    scrollOffset = (scrollOffset + delta).coerceIn(minScrollOffset, maxScrollOffset)
}
```

This ensures that when vertical drag starts on a card that was previously only dragged horizontally, `dragActiveIndex` is already set, so the `onDragStart` check `dragActiveIndex == index && isPopped` passes correctly and doesn't dismiss the pop.

### Fix C: Remove the erroneous centering offset
In `graphicsLayer` (lines 214-219), change to:

```kotlin
if (isDragged && isPopped) {
    translationX = dx  // default position is already centred by Alignment.BottomCenter
} else {
    translationX = tx.dp.toPx() + dx
}
```

Remove `parentWidthPx` and its `onSizeChanged` (line 165, line 183) since they are no longer needed.

### Fix D: Only redirect to `rawDragX` when the pointer started on the popped card
The outer `draggable` currently cannot tell which card the user started dragging on. The simplest fix: don't redirect in `draggableState` at all — instead, keep scrolling the hand always, but check in `onVerticalDrag` whether the card is popped and handle horizontal drift there.

Better approach: When the user drags horizontally on a popped card, `detectVerticalDragGestures` still fires (there's always some vertical component). The `change.consume()` at line 271 prevents the outer `draggable` from ever seeing these events. The only remaining case is a PURELY horizontal drag on a popped card. For that case, keep the redirect but only when `isPopped && dragActiveIndex == poppedCardIndex` (indicating the user is intentionally dragging the popped card, not scrolling elsewhere):

```kotlin
val draggableState = rememberDraggableState { delta ->
    if (poppedCardIndex >= 0 && dragActiveIndex == poppedCardIndex) {
        rawDragX += delta
        return@rememberDraggableState
    }
    scrollOffset = (scrollOffset + delta).coerceIn(minScrollOffset, maxScrollOffset)
}
```

With Fix B also in place, `dragActiveIndex` is set to `poppedCardIndex` when the outer draggable receives horizontal drag on a popped card. The condition `dragActiveIndex == poppedCardIndex` ensures only the popped card's drag is redirected; scrolling elsewhere in the hand while a card is popped still works.

### Fix E: Accumulate X deltas before pop threshold crossing
Move `rawDragX += change.position.x - lastDragX` outside the `if (isPopped)` block so it always runs during drag, even before the card is popped:

```kotlin
onVerticalDrag = { change: PointerInputChange, dragAmountY: Float ->
    if (poppedCardIndex < 0 || poppedCardIndex == index) {
        rawDragY += dragAmountY
        rawDragX += change.position.x - lastDragX  // always accumulate
        if (!isPopped && rawDragY < -popThresholdPx) {
            isPopped = true
        }
        lastDragX = change.position.x
        change.consume()
    }
}
```

This ensures `rawDragX` already contains the full horizontal drift accumulated throughout the drag, so when `isPopped` flips to true, the card's X position is already at the correct offset — no snap.

### Fix F: Don't dismiss pop in `onDragStart` when same card is already popped
Currently when the user taps to pop a card (which sets `poppedCardIndex`, `isPopped`, `rawDragY`) and then drags vertically, `onDragStart` fires and immediately dismisses the pop because `dragActiveIndex == index && isPopped` is true. Fix: skip the pop-dismiss when the drag starts on the already-popped card. Only reset `poppedCardIndex` when a DIFFERENT card starts dragging:

```kotlin
onDragStart = { startPos ->
    if (poppedCardIndex >= 0 && poppedCardIndex != index) {
        // Different card started dragging — dismiss current pop
        poppedCardIndex = -1
        dragActiveIndex = -1
        isPopped = false
        rawDragY = 0f
        rawDragX = 0f
    } else {
        val usable = if (item is com.example.game.model.Skill) {
            val s = item
            val isUlt = s.ultimateGain == 0
            if (isUlt) currentHero.gauge >= 100
            else (skillCooldowns[s.id] ?: 0) <= 0
        } else true
        if (usable) {
            if (poppedCardIndex == index && isPopped) {
                // Already popped by tap — just set dragActiveIndex, keep state
                lastDragX = startPos.x
            } else {
                // Fresh drag on a non-popped card
                poppedCardIndex = -1
                rawDragX = 0f
                rawDragY = 0f
                isPopped = false
                lastDragX = startPos.x
            }
            dragActiveIndex = index
            onCardDragStart?.invoke(cardColor)
        }
    }
}
```

The key change: replace `if (dragActiveIndex == index && isPopped)` with `if (poppedCardIndex == index && isPopped)`. When a tap already popped this card (`poppedCardIndex == index`, `isPopped = true`), just start dragging without resetting.

### Fix G: Replace `detectVerticalDragGestures` with unfiltered `detectDragGestures`
The `detectVerticalDragGestures` (line 229) is designed to report vertical drag components only. While we can read `change.position.x` from its callback, the gesture detector may still filter or transform the raw pointer deltas (some implementations only report the vertical portion of the drag delta and the raw `position` may come from a coordinate space affected by gesture filtering).

Replace the vertical-drag-specific handler with a general `detectDragGestures` that reports unfiltered deltas:

```kotlin
val dragMod = if (item is com.example.game.model.Skill || item is ComboSkill) {
    val cardColor = getCardColor(item)
    Modifier.pointerInput(index) {
        detectDragGestures(
            onDragStart = { startPos ->
                // Same logic as Fix F above
                if (poppedCardIndex >= 0 && poppedCardIndex != index) {
                    poppedCardIndex = -1
                    dragActiveIndex = -1
                    isPopped = false
                    rawDragY = 0f
                    rawDragX = 0f
                } else {
                    val usable = if (item is com.example.game.model.Skill) {
                        val s = item
                        val isUlt = s.ultimateGain == 0
                        if (isUlt) currentHero.gauge >= 100
                        else (skillCooldowns[s.id] ?: 0) <= 0
                    } else true
                    if (usable) {
                        if (poppedCardIndex == index && isPopped) {
                            lastDragX = startPos.x
                        } else {
                            poppedCardIndex = -1
                            rawDragX = 0f
                            rawDragY = 0f
                            isPopped = false
                            lastDragX = startPos.x
                        }
                        dragActiveIndex = index
                        onCardDragStart?.invoke(cardColor)
                    }
                }
            },
            onDrag = { change: PointerInputChange, dragAmount: Offset ->
                // Unfiltered delta — dragAmount.x and dragAmount.y are both accurate
                if (poppedCardIndex < 0 || poppedCardIndex == index) {
                    rawDragY += dragAmount.y
                    rawDragX += dragAmount.x  // unfiltered horizontal delta
                    if (!isPopped && rawDragY < -popThresholdPx) {
                        isPopped = true
                    }
                    change.consume()
                }
            },
            onDragEnd = {
                if (rawDragY < -thresholdPx) {
                    rawDragY = 0f
                    when (item) {
                        is com.example.game.model.Skill -> {
                            val skill = item
                            val isUlt = skill.ultimateGain == 0
                            val canUse = if (isUlt) currentHero.gauge >= 100
                                else (skillCooldowns[skill.id] ?: 0) <= 0
                            if (canUse) onSkill(skill)
                        }
                        is ComboSkill -> onComboSelect(item.id)
                    }
                }
                dragActiveIndex = -1
                poppedCardIndex = -1
                isPopped = false
                onCardDragEnd?.invoke()
            },
            onDragCancel = {
                dragActiveIndex = -1
                poppedCardIndex = -1
                isPopped = false
                onCardDragEnd?.invoke()
            }
        )
    }
} else Modifier
```

Key changes from the original:
- `detectVerticalDragGestures` → `detectDragGestures` (unfiltered orientation)
- `onVerticalDrag` → `onDrag` with `dragAmount: Offset` (gives `dragAmount.x` and `dragAmount.y` directly)
- `rawDragY += dragAmountY` → `rawDragY += dragAmount.y`
- `rawDragX += change.position.x - lastDragX` → `rawDragX += dragAmount.x` (unfiltered, 1:1 with finger movement)
- Remove `lastDragX` tracking entirely (no longer needed since `dragAmount.x` is the direct delta)

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt`
  - Lines ~169-177: Remove `snapBackY`, `snapBackX`, `LaunchedEffect(isDragged)`
  - Lines ~212-213: Read `0f` directly instead of `snapBackY/snapBackX.value`
  - Lines ~214-216: Remove centering formula, use `translationX = dx`
  - Line ~165, ~183: Remove `parentWidthPx` and `onSizeChanged`
  - Line ~187-192: Add `dragActiveIndex = poppedCardIndex` in draggableState; add `dragActiveIndex == poppedCardIndex` guard
  - Lines ~260-271: Replace `detectVerticalDragGestures` with `detectDragGestures` (Fix E + Fix G)
  - Lines ~230-258: Fix `onDragStart`/`onDragEnd`/`onDragCancel` to match new API shape, keep pop state when same card (Fix F)
  - Remove `lastDragX` state variable (line 163) — no longer needed

## Dependencies
- Supersedes and corrects Plans 55, 56, 63
