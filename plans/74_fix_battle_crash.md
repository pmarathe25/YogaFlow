# Plan 74: Fix Battle Crash on Hero Turn

## Problem
The app crashes in battle when it becomes a hero's turn. The crash likely originates from the card deal animation introduced by Plan 71 (BattleScene.kt:429-474).

### Potential Cause 1: `handOffset.dp.toPx()` during initial composition
The card deal animation code (BattleScene.kt:432-449):
```kotlin
var showHand by remember { mutableStateOf(false) }
val handOffset by animateFloatAsState(
    targetValue = if (showHand) 0f else 200f,
    animationSpec = spring(0.5f, 100f)
)

LaunchedEffect(Unit) {
    showHand = false
    delay(50)
    showHand = true
}

Box(
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .zIndex(1f)
        .graphicsLayer { translationY = handOffset.dp.toPx() }
) {
    ActionTray(...)
}
```

On initial composition, `showHand = false` → `targetValue = 200f` → `handOffset = 200f` → `translationY = 200.dp.toPx()`.

The `handOffset` is a raw `Float` from `animateFloatAsState` (unitless). Using `.dp.toPx()` on it treats the float as a Dp value. On high-density screens, `200.dp.toPx()` could be 400–600px. The `graphicsLayer` translates the ActionTray this far down, potentially pushing it below the visible area. While this shouldn't crash by itself, combined with the `key(state.currentActorId)` recomposition, the large offscreen translation might cause rendering issues.

### Potential Cause 2: `key(state.currentActorId)` inside Column with `Modifier.align`
The `Modifier.align(Alignment.BottomCenter)` at line 445 is used inside a Column (parent at line 239). In a `Column` scope, `Modifier.align()` only supports horizontal alignment. `Alignment.BottomCenter` is a two-dimensional alignment. The Compose runtime may crash or behave unexpectedly when a `ColumnScope.align` receives a full `Alignment` instead of `Alignment.Horizontal`.

### Potential Cause 3: Animation + gesture conflict
The card deal animation uses `animateFloatAsState` which causes recompositions. Simultaneously, the `HandOfCards` inside `ActionTray` uses `detectDragGestures` (from Plan 65's Fix G). If the animation triggers recomposition while a pointer gesture is active, Compose's pointer input system might encounter an inconsistent state.

### Potential Cause 4: `handGeneration` increment resets card keys
The `HandOfCards` composable (ActionTray.kt:174-177) increments `handGeneration` on every `currentHero.id` change:
```kotlin
val handGeneration = remember { mutableIntStateOf(0) }
LaunchedEffect(currentHero.id) {
    handGeneration.intValue++
}
```

This number is used in `key(handGeneration.intValue * 1000 + index)` at line 203. Each turn change increments `handGeneration`, making ALL card keys new. This forces every card to recompose from scratch with the `cardAlpha` fade-in animation (lines 212-216). The combination of the outer key at BattleScene level AND the inner key at HandOfCards level might create conflicting composition state.

## Fix

### Fix A: Guard `handOffset` with sane bounds
Ensure the animation doesn't produce negative or excessively large offset values. Cap the animation target:

```kotlin
val handOffset by animateFloatAsState(
    targetValue = if (showHand) 0f else 200f,
    animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f)
)

Box(
    modifier = Modifier
        .fillMaxWidth()
        .zIndex(1f)
        .graphicsLayer {
            translationY = if (showHand) 0f else 200f.dp.toPx()
        }
) {
    ActionTray(...)
}
```

Wait — this loses the animation. Better approach: animate a fraction that gets multiplied by the dp value:

```kotlin
val slideFraction by animateFloatAsState(
    targetValue = if (showHand) 0f else 1f,
    animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f)
)

graphicsLayer {
    translationY = slideFraction * 200.dp.toPx()
}
```

This keeps the float in 0..1 range and multiplies by the pixel value once, avoiding the `handOffset.dp.toPx()` reinterpretation.

### Fix B: Replace `Modifier.align(Alignment.BottomCenter)` with `Modifier.align(Alignment.CenterHorizontally)`
In `BattleScene.kt:445`, change the alignment to be Column-compatible:
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .zIndex(1f)
        .graphicsLayer { translationY = slideFraction * 200.dp.toPx() }
) {
    ActionTray(...)
}
```

Remove `Modifier.align(Alignment.BottomCenter)` — the Box using `fillMaxWidth()` already takes full width, and within a `Column` the child's vertical position is determined by the Column's arrangement, not by `Modifier.align`.

### Fix C: Remove the `handGeneration` inside `HandOfCards`
The outer `key(state.currentActorId)` at BattleScene level already forces recomposition on turn change. The inner `key(handGeneration.intValue * 1000 + index)` is redundant and may cause double-recomposition issues. Remove or simplify it:

```kotlin
allCards.forEachIndexed { index, item ->
    // Remove: key(handGeneration.intValue * 1000 + index)
    val centerIndex = ...
    ...
```

If the per-card fade-in animation on turn change is desired, keep the `cardAlpha` animate but remove the `handGeneration` key wrapper.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/BattleScene.kt` — fix the card deal animation: use `slideFraction` 0..1 multiplied by `200.dp.toPx()`; remove `Modifier.align(Alignment.BottomCenter)` override
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt` — remove or simplify `handGeneration` key wrapper inside `HandOfCards`

## Dependencies
- None
