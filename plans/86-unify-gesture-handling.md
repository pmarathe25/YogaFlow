# Plan 86: Unify Gesture Handling — Single Parent-Level Handler

## Problem
Scrolling the hand of cards is completely broken. The two-layer gesture architecture (child `detectDragGestures` per card + parent `draggable` for scrolling) doesn't work because `detectDragGestures` internally consumes pointer events — the parent's `draggable` modifier never receives horizontal scroll events when the user drags on a card.

Since cards overlap heavily (150dp wide, spaced at 85dp intervals), there are effectively no gaps to grab for scrolling. The user cannot scroll the hand at all.

## Root Cause
`detectDragGestures` in Compose internally consumes pointer changes during drag tracking. Even when our `onDrag` callback doesn't call `change.consume()`, the underlying gesture infrastructure has already consumed the event. The parent's `draggable(Orientation.Horizontal)` never sees drag events that start on a card.

The hand layout has cards closely stacked (fan layout with `translationX = relativeIndex * 85f` where cards are 150dp wide). Nearly the entire hand surface is covered by card gesture handlers, leaving no scrollable area.

## Fix
Replace the two-layer gesture architecture with a **single unified gesture handler** at the parent Box level. This handler:
1. Detects ALL drags on the hand area
2. Calculates which card the drag started on (from the X position)
3. Routes horizontal delta → `scrollOffset` (scrolls the hand)
4. Routes vertical delta → card's `rawDragY` (pops the card)
5. Manages pop/release/threshold logic centrally

Per-card `detectTapGestures` is kept for tap-to-pop (short tap doesn't conflict with drag).

### How card detection works
The X position of card `i` (in dp) is approximately: `tx = (i - centerIndex) * 85f + (scrollOffset / 150f) * 85f`. Given a drag start X (relative to the inner Box), we find the closest card:

```
centerIndex = (cardCount - 1) / 2f
relativeX = (startX - boxCenterX) / density
approxIndex = centerIndex + relativeX / 85f - scrollOffset / 150f
cardIndex = approxIndex.roundToInt().coerceIn(0, cardCount - 1)
```

### State management
All per-card drag state is already at the `HandOfCards` level (not per-card): `dragActiveIndex`, `poppedCardIndex`, `rawDragY`, `rawDragX`, `isPopped`. This means the unified handler can directly control these without refactoring state ownership.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt`

## Changes

### 1. Remove per-card drag gesture handler (replace `dragModifier`)
Delete the per-card `dragModifier` (currently lines 224-281) that contains `detectDragGestures`. The `tapMod` is kept.

### 2. Remove parent `draggable` modifier
Replace the inner Box's `.draggable(state = draggableState, orientation = Orientation.Horizontal)` (line 190) with the new unified handler.

### 3. Add unified gesture handler
Add a single `pointerInput` to the inner Box (or outer Box) that handles both scrolling and card popping:

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)
        .pointerInput(cardCount, scrollOffset) {
            // Calculate card positions for hit-testing
            val centerIndex = (cardCount - 1) / 2f
            val density = this.density

            detectDragGestures(
                onDragStart = { startPos ->
                    // Determine which card is under the pointer
                    val boxCenterX = size.width / 2f
                    val relativeX = (startPos.x - boxCenterX) / density
                    val approxIndex = centerIndex + relativeX / 85f - (scrollOffset / 150f)
                    val index = approxIndex.roundToInt().coerceIn(0, cardCount - 1)

                    if (index in allCards.indices) {
                        if (poppedCardIndex >= 0 && poppedCardIndex != index) {
                            poppedCardIndex = -1; dragActiveIndex = -1; isPopped = false
                            rawDragY = 0f; rawDragX = 0f
                        } else {
                            val item = allCards[index]
                            val usable = if (item is Skill) {
                                val s = item; val isUlt = s.ultimateGain == 0
                                if (isUlt) currentHero.gauge >= 100
                                else (skillCooldowns[s.id] ?: 0) <= 0
                            } else if (item is ComboSkill) {
                                !item.requiredHeroes.any { it in actedHeroIds }
                            } else true
                            if (usable) {
                                if (poppedCardIndex == index && isPopped) { /* keep */ } else {
                                    poppedCardIndex = -1; rawDragX = 0f; rawDragY = 0f; isPopped = false
                                }
                                dragActiveIndex = index
                                onCardDragStart?.invoke(getCardColor(item))
                            }
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    if (dragActiveIndex >= 0) {
                        // Horizontal delta → scroll hand (unless popped)
                        if (!isPopped) {
                            scrollOffset = (scrollOffset + dragAmount.x)
                                .coerceIn(minScrollOffset, maxScrollOffset)
                        }
                        // Vertical delta → pop card (only the active card)
                        if (poppedCardIndex < 0 || poppedCardIndex == dragActiveIndex) {
                            rawDragY += dragAmount.y
                            if (!isPopped) rawDragX += dragAmount.x
                            // Pop only if vertical-dominant
                            if (!isPopped && rawDragY < -popThresholdPx
                                && abs(rawDragY) > abs(rawDragX) * 1.5f) {
                                isPopped = true
                                // After pop, consume events to prevent further scrolling
                            }
                        }
                    }
                    if (isPopped) change.consume()
                },
                onDragEnd = {
                    if (dragActiveIndex >= 0) {
                        val item = allCards.getOrNull(dragActiveIndex)
                        if (rawDragY < -thresholdPx) {
                            rawDragY = 0f
                            when (item) {
                                is Skill -> {
                                    val s = item; val isUlt = s.ultimateGain == 0
                                    val canUse = if (isUlt) currentHero.gauge >= 100
                                        else (skillCooldowns[s.id] ?: 0) <= 0
                                    if (canUse) onSkill(s)
                                }
                                is ComboSkill -> if (!item.requiredHeroes.any { it in actedHeroIds }) onComboSelect(item.id)
                            }
                        }
                    }
                    dragActiveIndex = -1; poppedCardIndex = -1; isPopped = false
                    rawDragY = 0f; rawDragX = 0f
                    onCardDragEnd?.invoke()
                },
                onDragCancel = {
                    dragActiveIndex = -1; poppedCardIndex = -1; isPopped = false
                    rawDragY = 0f; rawDragX = 0f
                    onCardDragEnd?.invoke()
                }
            )
        },
    contentAlignment = Alignment.BottomCenter
) {
    // Cards rendered without dragModifier or draggable
    allCards.forEachIndexed { index, item ->
        // ... card rendering unchanged, but without dragModifier
        // Keep tapMod for tap-to-pop
    }
}
```

### 4. Keep `tapMod` as-is
The per-card `tapMod` with `detectTapGestures` (lines 283-327) is unchanged. Tap doesn't conflict with the parent's drag handler because:
- A tap is a quick down-up without movement → `detectTapGestures` fires
- A drag starts as a down + move → `detectDragGestures` fires, and the tap gesture fails to detect a tap

### 5. Remove `rememberDraggableState`
Since the unified handler manages scrolling directly, the `rememberDraggableState` variable and `draggable` modifier are no longer needed. The `minScrollOffset`/`maxScrollOffset` calculation stays (used in the new handler).

## Verification
- Swipe horizontally on any card → hand scrolls left/right smoothly
- Swipe up sharply on a card → card pops up and follows finger
- After pop by drag, move finger in any direction → card follows (both axes)
- Tap a card → card pops to center position (tapPopPositionPx)
- After tap-pop, drag the card → card follows finger in all axes
- Scroll hand while no card is popped → scrolls normally
- After tap-pop, scroll hand by swiping on a DIFFERENT area → hand scrolls, popped card unaffected
- Release popped card past 200dp → skill fires
- Release popped card below 200dp → card returns to hand
- Cooldown cards are not draggable
- Combo cards work correctly

## Dependencies
- Plan 87 may adjust pop visual behavior
