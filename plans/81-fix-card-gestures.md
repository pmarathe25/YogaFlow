# Plan 81: Fix Horizontal Scrolling and Card Drag Gesture Handling

## Problem
Three gesture-related issues after switching from `detectDragGestures` to `detectVerticalDragGestures`:

1. **Horizontal scrolling on cards doesn't work** — `detectVerticalDragGestures` consumes all pointer events (`change.consume()`), preventing the parent `draggable(Orientation.Horizontal)` from receiving horizontal scroll gestures on cards. The user can only scroll by dragging gaps between cards.

2. **Popped card can only move vertically, not horizontally** — `detectVerticalDragGestures` only reports Y deltas, so `rawDragX` never accumulates. The parent `draggable` would provide X deltas via the `rememberDraggableState` callback, but it never fires because the card's gesture handler consumed the event.

3. **Can't scroll hand when a card is popped** — `rememberDraggableState` returns early when `poppedCardIndex >= 0 && dragActiveIndex == poppedCardIndex`, intercepting ALL horizontal scrolls regardless of whether they started on the popped card or the background.

## Root Cause
The gesture architecture has two layers:
- **Outer**: `draggable(state, Orientation.Horizontal)` on the inner Box (line 194) — handles horizontal scrolling
- **Inner**: `detectVerticalDragGestures` per card (line 231) — handles vertical pop gestures

In Compose, inner `pointerInput` modifiers consume events before outer ones. Since `detectVerticalDragGestures` calls `change.consume()` on every drag frame, the outer `draggable` never sees any drag event that starts on a card. This completely breaks horizontal scrolling on cards.

Additionally, the `rememberDraggableState` callback condition `if (poppedCardIndex >= 0 && dragActiveIndex == poppedCardIndex)` gates ALL horizontal drags — if a card is popped, even dragging the background won't scroll because `dragActiveIndex` still equals the popped card index.

## Fix
Use **`detectDragGestures`** (reports both X and Y) per card, but apply the following rules:
- **Don't consume** the pointer change when `!isPopped` → parent `draggable` receives horizontal events for scrolling
- **Do consume** when `isPopped` → popped card moves freely in both axes, parent doesn't interfere
- **Vertical-dominance check** before popping — `abs(rawDragY) > abs(rawDragX) * 1.5`. This prevents accidental pop during horizontal scrolls.
- **Pop-via-drag ≠ snap-to-center**: When dragging upward past the threshold, `isPopped = true` but the card stays under the finger at `rawDragY` (continuous tracking). It does NOT snap to the center pop position (`tapPopPositionPx`). Snap-to-center is tap-only behavior.
- **Track `rawDragX`** from the card's own gesture, not from `rememberDraggableState` — this ensures horizontal movement works for popped cards regardless of the parent

For the `rememberDraggableState`:
- Remove the `if (poppedCardIndex >= 0 ... rawDragX += delta)` early return — let horizontal scroll pass through when a card is popped
- The popped card's X movement is now handled entirely by the card's `detectDragGestures` 

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt`

## Changes

### 1. Replace `detectVerticalDragGestures` with `detectDragGestures` (lines 231-295)
Replace the per-card gesture handler:

**Before:**
```kotlin
Modifier.pointerInput(index) {
    detectVerticalDragGestures(
        onDragStart = { startPos -> ... },
        onVerticalDrag = { change, dragAmount ->
            if (poppedCardIndex < 0 || poppedCardIndex == index) {
                rawDragY += dragAmount
                if (!isPopped && rawDragY < -popThresholdPx) { isPopped = true }
                change.consume()
            }
        },
        onDragEnd = { ... },
        onDragCancel = { ... }
    )
}
```

**After:**
```kotlin
Modifier.pointerInput(index) {
    detectDragGestures(
        onDragStart = { startPos ->
            if (poppedCardIndex >= 0 && poppedCardIndex != index) {
                poppedCardIndex = -1; dragActiveIndex = -1; isPopped = false; rawDragY = 0f; rawDragX = 0f
            } else {
                val usable = if (item is com.example.game.model.Skill) {
                    val s = item; val isUlt = s.ultimateGain == 0
                    if (isUlt) currentHero.gauge >= 100 else (skillCooldowns[s.id] ?: 0) <= 0
                } else if (item is ComboSkill) {
                    !item.requiredHeroes.any { it in actedHeroIds }
                } else true
                if (usable) {
                    if (poppedCardIndex == index && isPopped) { /* keep state */ } else {
                        poppedCardIndex = -1; rawDragX = 0f; rawDragY = 0f; isPopped = false
                    }
                    dragActiveIndex = index; onCardDragStart?.invoke(cardColor)
                }
            }
        },
        onDrag = { change, dragAmount ->
            if (poppedCardIndex < 0 || poppedCardIndex == index) {
                rawDragY += dragAmount.y
                rawDragX += dragAmount.x
                // Pop only if gesture is vertical-dominant
                if (!isPopped && rawDragY < -popThresholdPx 
                    && abs(rawDragY) > abs(rawDragX) * 1.5f) {
                    isPopped = true
                }
                // Consume only when popped to prevent parent scroll interference
                if (isPopped) {
                    change.consume()
                }
            }
        },
        onDragEnd = {
            if (rawDragY < -thresholdPx) {
                rawDragY = 0f
                when (item) {
                    is com.example.game.model.Skill -> {
                        val s = item; val isUlt = s.ultimateGain == 0
                        val canUse = if (isUlt) currentHero.gauge >= 100
                            else (skillCooldowns[s.id] ?: 0) <= 0
                        if (canUse) onSkill(s)
                    }
                    is ComboSkill -> if (!item.requiredHeroes.any { it in actedHeroIds }) onComboSelect(item.id)
                }
            }
            dragActiveIndex = -1; poppedCardIndex = -1; isPopped = false; onCardDragEnd?.invoke()
        },
        onDragCancel = {
            dragActiveIndex = -1; poppedCardIndex = -1; isPopped = false; onCardDragEnd?.invoke()
        }
    )
}
```

### 2. Fix `rememberDraggableState` (lines 181-188)
Remove the early return that intercepts horizontal scrolls when a card is popped:

**Before:**
```kotlin
val draggableState = rememberDraggableState { delta ->
    if (poppedCardIndex >= 0 && dragActiveIndex == poppedCardIndex) {
        rawDragX += delta
        dragActiveIndex = poppedCardIndex
        return@rememberDraggableState
    }
    scrollOffset = (scrollOffset + delta).coerceIn(minScrollOffset, maxScrollOffset)
}
```

**After:**
```kotlin
val draggableState = rememberDraggableState { delta ->
    scrollOffset = (scrollOffset + delta).coerceIn(minScrollOffset, maxScrollOffset)
}
```

## How It Works
### Horizontal scroll on cards (not popped)
1. User drags horizontally on a card
2. Card's `detectDragGestures.onDrag` fires: `rawDragY += dragAmount.y`, `rawDragX += dragAmount.x`
3. `isPopped` is false → `change.consume()` is NOT called
4. Parent `draggable(Orientation.Horizontal)` also fires: `scrollOffset += delta.x` → hand scrolls
5. `rawDragX` is visually ignored when `!isPopped` (see line 216: `val dx = if (isDragged && isPopped) rawDragX else 0f`)

### Pop by vertical drag
1. User drags sharply upward on a card
2. `rawDragY` becomes negative and large; `rawDragX` stays small
3. `abs(rawDragY) > abs(rawDragX) * 1.5f` → passes vertical-dominance check
4. `rawDragY < -popThresholdPx` → passes threshold check
5. `isPopped = true` → card pops up

### Dragging popped card horizontally
1. `isPopped` is true → `change.consume()` is called
2. Parent `draggable` does NOT fire → hand doesn't scroll
3. `rawDragX += dragAmount.x` → card moves horizontally
4. `rawDragY += dragAmount.y` → card moves vertically
5. Both dimentsions are visually applied (line 215-216: `val dx = if (isDragged && isPopped) rawDragX else 0f`)

### Scrolling hand while another card is popped
1. User swipes on background (not on the popped card)
2. Card's `detectDragGestures` doesn't fire (swipe not on that card)
3. Parent `draggable` fires → `scrollOffset` updates → hand scrolls
4. `rememberDraggableState` no longer has the early return

## Verification
- Swipe horizontally on a card → hand scrolls left/right
- Swipe up sharply on a card → card pops up
- Tap a card → card pops up (via `detectTapGestures`, unchanged)
- Drag popped card in any direction (horizontal, vertical, diagonal) → card follows finger
- Scroll hand while a different card is popped → hand scrolls normally
- Release popped card past threshold → skill fires
- Release below threshold → card returns to hand

## Dependencies
- None
