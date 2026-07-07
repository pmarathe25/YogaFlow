# Plan 36: Fix Sluggish Card Dragging in Battle

## Problem

Skill card dragging (especially horizontal movement after popping) feels sluggish and lags behind the finger. The `displayDragX` and `displayDragY` values are smoothed through `animateFloatAsState` with relatively soft spring parameters (`dampingRatio = 0.65f, stiffness = 300f`), causing noticeable latency between touch input and card position.

## Root cause

**`ActionTray.kt:164-175`**:

```kotlin
// target values are set from raw drag position
val targetY = if (dragActiveIndex >= 0) rawDragY else 0f
val targetX = if (dragActiveIndex >= 0 && isPopped) rawDragX else 0f

// but actual display position is a smoothed animation
val displayDragY by animateFloatAsState(
    targetValue = targetY,
    animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
)
val displayDragX by animateFloatAsState(
    targetValue = targetX,
    animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
)
```

The `animateFloatAsState` with `spring(0.65f, 300f)` acts as a low-pass filter. The card never snaps to the current finger position — it always trails behind. This is intentional for a "smooth" feel during the pop animation but creates perceptible lag during active dragging.

## Fix

Replace the animated positions with direct values during active drag. Only use animation for the snap-back (when drag ends and values return to 0).

```kotlin
// Replace lines 164-175
val isDragged = dragActiveIndex >= 0

// Use raw values during drag; animate only the snap-back
val displayDragY by animateFloatAsState(
    targetValue = if (isDragged) rawDragY else 0f,
    animationSpec = if (isDragged)
        tween(0)                         // instant during drag
    else
        spring(dampingRatio = 0.5f, stiffness = 500f)  // snappier snap-back
)
val displayDragX by animateFloatAsState(
    targetValue = if (isDragged && isPopped) rawDragX else 0f,
    animationSpec = if (isDragged)
        tween(0)
    else
        spring(dampingRatio = 0.5f, stiffness = 500f)
)
```

This means:
- **During drag**: Card position = raw finger position (zero latency).
- **On release**: Card snaps back to idle with a slightly stiffer spring (500 vs 300) and less damping (0.5 vs 0.65) for a more responsive feel.

### Verify the modifier chain

Also check that `graphicsLayer` modifier at lines 203-211 uses `displayDragY` and `displayDragX`. If not already using the display values, update:

```kotlin
.graphicsLayer {
    val dy = if (isDragged) displayDragY else 0f
    val dx = if (isDragged && isPopped) displayDragX else 0f
    translationX = tx.dp.toPx() + dx
    translationY = ty.dp.toPx() + dy - (if (item is ComboSkill) 20f else 0f)
    rotationZ = if (isDragged) 0f else rotation
    if (isDragged) { scaleX = 1.15f; scaleY = 1.15f }
}
```

The `graphicsLayer` translations use `dp.toPx()` for the fan position (tx, ty) and raw pixel values for drag offsets (displayDragX/Y which are already in px from the gesture detector). This mix should be correct — `rawDragX/Y` come from `detectVerticalDragGestures` which provides pixel values.

## Files to modify

| File | Changes |
|---|---|
| `ActionTray.kt` | Change `animateFloatAsState` specs to use instant `tween(0)` during drag and snappier `spring(0.5, 500)` for snap-back |

## Dependencies

- None. Standalone feel fix.
