# Plan 45: Fix Battle Card Dragging Sluggishness

## Problem

Dragging a skill card upward in battle feels sluggish and laggy — the card lags behind where the finger is. This is caused by `animateFloatAsState` wrapping the drag offset values (`rawDragY`, `rawDragX`) even during active drag. Though `tween(0)` is used (which should be instant), the animation pipeline still introduces ~1 frame of latency on every drag event, making the motion feel delayed.

## Root Cause

**`ActionTray.kt:166-180`** — the chain is:

```
pointerInput → rawDragY (mutableStateOf) → animateFloatAsState → displayDragY → graphicsLayer { translationY = displayDragY }
```

`animateFloatAsState` with `tween(0)` still runs the animation pipeline per frame:
1. Pointer event arrives → `rawDragY` updates (mutable state)
2. Animation frame → `animateFloatAsState` starts animation toward new target
3. Next frame → animation resolves → `displayDragY` updates
4. Recomposition → `graphicsLayer` reads new value → draws

Step 2-3 adds ~16ms of latency per frame. During fast finger movement, this accumulates as visible lag.

## Fix

### Option A: Use `snap()` instead of `tween(0)`

**`ActionTray.kt:168-171`** — change the drag animation spec from `tween(0)` to `snap()`:

Before:
```kotlin
val displayDragY by animateFloatAsState(
    targetValue = if (isDragged) rawDragY else 0f,
    animationSpec = if (isDragged)
        tween(0)
    else
        spring(dampingRatio = 0.5f, stiffness = 500f)
)
```

After:
```kotlin
val displayDragY by animateFloatAsState(
    targetValue = if (isDragged) rawDragY else 0f,
    animationSpec = if (isDragged)
        snap()     // truly instant — no frame delay
    else
        spring(dampingRatio = 0.5f, stiffness = 500f)
)
```

Same for `displayDragX` (lines 176-179).

### Option B: Bypass animation entirely during drag (more performant)

Read `rawDragY`/`rawDragX` directly in `graphicsLayer` during drag and only animate on release:

```kotlin
// During drag, read raw values directly
val dy = if (isDragged) rawDragY else displayDragY
val dx = if (isDragged && isPopped) rawDragX else displayDragX

translationX = tx.dp.toPx() + dx
translationY = ty.dp.toPx() + dy - (...)
```

Where `displayDragY`/`displayDragX` are `animateFloatAsState` values that only animate the spring-back on release:

```kotlin
val displayDragY by animateFloatAsState(
    targetValue = 0f,   // always 0 — the rawDragY resets on dragEnd
    animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)
)
```

But this is more complex because on `onDragEnd`, `rawDragY` resets to 0 and the spring animation should take over. Option A (snap) is the simplest fix.

### Option C: Use `Animatable` with `snapTo()`

```kotlin
val dragOffsetY = remember { Animatable(0f) }

// In pointerInput onVerticalDrag:
dragOffsetY.snapTo(rawDragY)

// In graphicsLayer:
translationY = dragOffsetY.value
```

This avoids the `animateFloatAsState` recomposition overhead entirely.

## Recommendation

Implement **Option A** (`snap()`) as it's the minimal change and resolves the latency issue. If further improvement is needed, refactor to Option C.

## Files to modify

| File | Changes |
|---|---|
| `ActionTray.kt` | Replace `tween(0)` with `snap()` in drag animation specs for both `displayDragY` and `displayDragX` |

## Dependencies

- None. Standalone fix.
