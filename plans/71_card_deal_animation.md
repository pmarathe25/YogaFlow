# Plan 71: Card Deal Animation When Turn Changes

## Problem
When the turn passes from one hero to the next, the skill cards in `ActionTray` simply swap in place (via `key(state.currentActorId)` at `BattleScene.kt:409`). There is no visual transition — the old hero's cards disappear instantly and the new hero's cards appear in the same position. The user wants a "card deal" animation where cards visibly slide into the hand from off-screen.

## Fix

### Approach: Animate card entry on turn change using `AnimatedVisibility` + slide-in
Wrap the `ActionTray` (or the `HandOfCards` content) with an `AnimatedVisibility` that triggers on `state.currentActorId` changes, using a slide-in-from-bottom + fade-in transition.

### Step 1: Add a unique key for the hand transition
The `key(state.currentActorId)` at `BattleScene.kt:409` already recomposes the entire `ActionTray` when the actor changes. We can leverage this to add an enter animation.

### Step 2: Add slide-in animation to `HandOfCards`
Wrap the card rendering inside `HandOfCards` with an animation that triggers when `allCards` changes. Since `allCards` is derived from `currentHero`, it changes whenever the hero changes.

**In `HandOfCards` (ActionTray.kt:132-357)**, add enter transitions to the card items:

```kotlin
// Around line 200, wrap the forEachIndexed content:
items.forEachIndexed { index, item ->
    val isNewHand = remember { mutableStateOf(true) }
    LaunchedEffect(allCards) {
        // Reset animation state when hand changes
        isNewHand.value = true
        delay(50 * index) // Stagger
        isNewHand.value = false
    }

    AnimatedVisibility(
        visible = !isNewHand.value,
        enter = slideInVertically(
            initialOffsetY = { it + 200 }, // slide up from below
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 300f
            )
        ) + fadeIn(animationSpec = tween(300))
    ) {
        // existing card rendering...
    }
}
```

However, wrapping individual cards in `AnimatedVisibility` may interfere with gesture detection (since the composable enters/exits the composition). A simpler approach:

### Alternative: Animate the entire `ActionTray` as a single unit
Use a slide transition on the `ActionTray` composable itself. Since it's already wrapped in a `key(state.currentActorId)`, Compose will treat it as a new composition on each turn change:

```kotlin
// In BattleScene.kt, around line 408:
if (currentHero != null && state.phase == PLAYER_TURN) {
    key(state.currentActorId) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(1f)
                .animateContentSize()
        ) {
            ActionTray(...)
        }
    }
}
```

But `animateContentSize` only animates size changes, not position.

### Recommended: Use `Modifier.graphicsLayer` + `LaunchedEffect` for slide-up
The cleanest approach: track whether the hand is entering with a boolean state, and animate `translationY` from below to 0:

```kotlin
// In BattleScene.kt, replace the key block (lines 408-431):
if (currentHero != null && state.phase == PLAYER_TURN) {
    key(state.currentActorId) {
        var showHand by remember { mutableStateOf(false) }
        val handOffset by animateFloatAsState(
            targetValue = if (showHand) 0f else 200f,
            animationSpec = spring(0.5f, 100f)
        )

        LaunchedEffect(Unit) {
            showHand = false
            delay(50) // Brief pause to reset position
            showHand = true
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(1f)
                .graphicsLayer { translationY = handOffset.dp.toPx() }
        ) {
            ActionTray(
                currentHero = currentHero,
                ...
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            )
        }
    }
}
```

This slides the entire ActionTray up from 200dp below its final position. The `animateFloatAsState` with `spring()` gives a bouncy card-deal feel.

### Step 3: Add per-card stagger (optional enhancement)
For a more polished effect where cards appear to be "dealt" one by one, add a fade-in delay inside `HandOfCards` when the hero changes:

```kotlin
// In HandOfCards, around line 201:
val handGeneration = remember { mutableIntStateOf(0) }

// Increment when currentHero changes
LaunchedEffect(currentHero.id) {
    handGeneration.intValue++
}

allCards.forEachIndexed { index, item ->
    val cardAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400, delayMillis = 100 + index * 80), // Stagger: 100ms + 80ms per card
        label = "cardAlpha"
    )

    // ... existing card rendering with alpha modifier:
    // cardMod.alpha(cardAlpha).then(...)
}
```

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/BattleScene.kt` — wrap `ActionTray` in a slide-up animation using `animateFloatAsState` + `graphicsLayer.translationY`
- (Optional) `app/src/main/java/com/example/game/ui/components/ActionTray.kt` — add per-card stagger alpha animation in `HandOfCards`

## Dependencies
- None
