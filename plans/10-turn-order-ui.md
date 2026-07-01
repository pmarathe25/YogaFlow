# Plan: Compress Turn Order UI & Add Turn Indicator Banner

## Issue Addressed
- **Issue 12**: The turn order UI can be more compressed — there's too much vertical spacing between names. Add a small banner at the top indicating whose turn it is. Animate the name of the active actor (bounce text slowly).

---

## Changes Required

### 1. `BattleScreen.kt` — `TurnOrderList` composable

**Current implementation** (lines 353-371):
```kotlin
@Composable
fun TurnOrderList(state: BattleState) {
    LazyColumn(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        items(state.turnOrder) { actor ->
            val isActive = actor.id == state.currentActorId
            val color = elementToColor(actor.element)
            
            Text(
                text = (if (isActive) "▶ " else "") + actor.name.uppercase(),
                color = if (isActive) color else Color.White.copy(alpha = 0.7f),
                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
```

**Desired changes:**

#### Reduce vertical spacing
- Remove `verticalArrangement = Arrangement.spacedBy(4.dp)` or reduce to `2.dp`
- Remove the `padding(4.dp)` on the `LazyColumn` modifier
- Reduce font size slightly if needed
- Make it more compact overall

```kotlin
LazyColumn(
    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    verticalArrangement = Arrangement.spacedBy(1.dp)
) {
    items(state.turnOrder) { actor ->
        val isActive = actor.id == state.currentActorId
        val color = elementToColor(actor.element)
        
        Text(
            text = actor.name.uppercase(),
            color = if (isActive) color else Color.White.copy(alpha = 0.6f),
            fontWeight = if (isActive) FontWeight.Black else FontWeight.Normal,
            fontSize = if (isActive) 12.sp else 10.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
        )
        // Use a dot or small indicator instead of the ▶ prefix
    }
}
```

### 2. Add a turn indicator banner at the top

Create a small banner in `BattleScreen.kt` that shows whose turn it is, positioned above the turn order list or at the top center of the screen:

```kotlin
@Composable
fun TurnIndicator(
    actorName: String?,
    actorElement: Element?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it }
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            color = if (actorElement != null) elementToColor(actorElement).copy(alpha = 0.7f)
                    else Color.Black.copy(alpha = 0.6f)
        ) {
            Text(
                text = "${actorName ?: "Unknown"}'s Turn",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
```

This is a simpler, smaller alternative to the full-screen `TurnBanner` that already exists in `BattleAnimations.kt`. Consider whether to keep both or replace `TurnBanner` with this compact version.

### 3. Animate the active actor's name (slow bounce)

In `TurnOrderList`, add a slow bouncing animation to the currently active actor's name:

```kotlin
val infiniteTransition = rememberInfiniteTransition()
val bounceOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = -3f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)
```

Apply the bounce to the active actor's text:

```kotlin
Text(
    text = actor.name.uppercase(),
    color = if (isActive) color else Color.White.copy(alpha = 0.6f),
    fontWeight = if (isActive) FontWeight.Black else FontWeight.Normal,
    fontSize = if (isActive) 12.sp else 10.sp,
    modifier = Modifier
        .padding(horizontal = 4.dp, vertical = 0.dp)
        .then(if (isActive) Modifier.graphicsLayer { translationY = bounceOffset } else Modifier)
)
```

Use a slow animation period (1000ms or longer) for a gentle bouncing effect.

### 4. Replace the full TurnBanner with the compact indicator

**Decision: Replace the existing `TurnBanner` in `BattleAnimations.kt` with the compact `TurnIndicator`.** The full-screen black overlay banner (36sp text, center-screen, 24dp padding) is too intrusive. The compact top-aligned `TurnIndicator` (13sp, 12dp corner radius, 6dp padding) conveys the same information with much less visual disruption.

Changes:
- Remove the `TurnBanner` usage from `BattleScreen.kt` (line 306: `TurnBanner(actorName = ..., visible = ..., modifier = Modifier.align(Alignment.Center))`).
- Add the new `TurnIndicator` composable in the `Row` header area alongside `TurnOrderList`, or as a top-center element above the monster area.
- Keep `TurnBanner.kt` file but delete its composable (or leave it if used elsewhere — verify with a grep).
- The turn indicator should remain visible for the **entire duration of the turn**, not just 1000ms. Since it's small and unobtrusive (top corner, 13sp), it serves as a persistent "whose turn is it" reference. Remove the `delay(1000)` + hide logic from the `LaunchedEffect` at line 99. The indicator shows when `currentActorId` changes and stays visible until the next actor's turn.
