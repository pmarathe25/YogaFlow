# Plan: Dismiss Skill Card When Clicking Anywhere Except "Use" Button

## Issue Addressed
- **Issue 4**: When a skill card is selected, clicking anywhere except on the "Use" button should dismiss the card. Currently, clicking in the bottom half of the screen correctly dismisses the card, but clicking in the battle area (top half) has no effect.

---

## Root Cause Analysis

In `BattleActions.kt`, when a skill card is selected (`selectedCardId != null`), the overlay has two layers:

1. **Background dim** (lines 172-178):
```kotlin
if (!isUsingSkill) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { selectedCardId = null }
    )
}
```
This dim layer covers the **entire ActionPanel** (which is 400dp tall, per `Modifier.height(400.dp)`). But the dim layer only covers the ActionPanel area, not the full screen.

2. The **SkillCard** is centered within the ActionPanel.

Since the battle area (top half of the screen above the ActionPanel) is outside the ActionPanel's bounds, clicks there never reach the dim layer's click handler.

---

## Changes Required

### 1. `BattleActions.kt` — Extend the dismiss click area

**Decision: Lift `selectedCardId` state to `GameViewModel` (Option E from original analysis).** This is the cleanest approach because:
1. The fly-out animation (Section 3) also needs BattleScreen to know when a card is selected (to show the dim overlay behind the ActionPanel)
2. Dismiss-on-click-outside needs BattleScreen to intercept clicks
3. Having a single source of truth avoids synchronization issues between ActionPanel, BattleScreen, and animation state

All other options (A-D) introduce either fragile coordinate hacks or callback chains that break encapsulation. ViewModel state is the right architectural choice here.

Add to `GameViewModel`:
```kotlin
private val _selectedCardId = MutableStateFlow<String?>(null)
val selectedCardId: StateFlow<String?> = _selectedCardId.asStateFlow()

fun selectCard(cardId: String?) {
    _selectedCardId.value = cardId
}

fun dismissSelectedCard() {
    _selectedCardId.value = null
}
```

`ActionPanel` calls `viewModel.selectCard(id)` on tap and `viewModel.dismissSelectedCard()` on dismiss. `BattleScreen` observes `selectedCardId` and shows a full-screen dismiss overlay when non-null.

The BattleScreen overlay:
```kotlin
// Inside the main Box in BattleScreen, between the battle Column and ActionPanel
val selectedCardId by viewModel.selectedCardId.collectAsState()
if (selectedCardId != null && !isTargeting && !isUsingSkill) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { viewModel.dismissSelectedCard() }
    )
}
```

This ensures clicks anywhere (battle area, monster, heroes) dismiss the card. The overlay sits above the hero/monster Column but below the ActionPanel in Z-order.

### 2. Implement the overlay in `BattleScreen.kt`

This is described in the dismiss decision above. The overlay Box with `clickable { viewModel.dismissSelectedCard() }` sits in the `main Box` between the battle Column and ActionPanel, covering the full screen.

### 3. Card Selection Fly-Out Animation

When a skill card is tapped in the hand fan, it should animate from its fan position to the center overlay rather than appearing instantly.

#### Current state

The fan cards sit in `HandOfCards` (lines 285-343) with per-index positions:

| Property | Formula |
|----------|---------|
| `translationX` | `relativeIndex * 85f` converted to px |
| `translationY` | `(relativeIndex² * 10f)` converted to px |
| `rotationZ` | `relativeIndex * 12f` |
| `scale` | 1.0 |

The overlay card (lines 166-217) appears instantly centered at 1.2x scale with no entry animation.

#### Decision: Fan-position-aware fly-out (Option A)

Capture the card's fan position on selection and animate from those values to target (0,0,0, 1.2x). This looks better than a generic rise-from-below because the card visually "comes out of" the hand fan.

In `ActionPanel`, add origin tracking:
```kotlin
// State to track animation origin (from fan position)
data class AnimOrigin(val tx: Float, val ty: Float, val rot: Float)
var animOrigin by remember { mutableStateOf<AnimOrigin?>(null) }
var isAnimatingIn by remember { mutableStateOf(false) }
```

When `selectedCardId` changes, compute the origin from the same formula used in `HandOfCards`:
```kotlin
LaunchedEffect(selectedCardId) {
    if (selectedCardId != null) {
        // Recompute fan position using the same formulas as HandOfCards
        val allCards: List<Any> = buildList {
            currentHero.skills.forEach { add(it) }
            if (currentHero.ultimateGauge >= 100) add(currentHero.ultimate)
            availableCombos.forEach { add(it) }
        }
        val index = allCards.indexOfFirst { 
            (it is Skill && it.id == selectedCardId) || 
            (it is ComboSkill && it.id == selectedCardId) 
        }
        if (index >= 0) {
            val centerIndex = (allCards.size - 1) / 2f
            // scrollOffset is local to HandOfCards — if can't access it, assume 0
            val relativeIndex = index - centerIndex
            animOrigin = AnimOrigin(
                tx = relativeIndex * 85f,
                ty = (relativeIndex * relativeIndex * 10f),
                rot = relativeIndex * 12f
            )
        }
        isAnimatingIn = true
    } else {
        animOrigin = null
        isAnimatingIn = false
    }
}
```

Pass a `scrollOffset` parameter from `HandOfCards` up to `ActionPanel` (since `scrollOffset` is currently private to `HandOfCards`). Or export it — simplest is to add `scrollOffset` as a parameter to `ActionPanel` that `HandOfCards` updates.

**⚠️ Coordinate space caveat:** The fan positions are relative to `HandOfCards`'s `BottomCenter`-aligned `Box` (280dp tall), while the overlay is `Center`-aligned in `ActionPanel`'s outer Box (400dp tall). The Y offsets won't be exact.

**Decision: Use a Y-bias to compensate for the alignment difference.** The vertical mismatch between `BottomCenter` (fan) and `Center` (overlay) is roughly `(400 - 280) / 2 = 60dp`. Use `translationY = lerp(60.dp.toPx() + tyPx, 0f, progress)` to offset this. The fan cards' own Y offset (`relativeIndex² * 10f`) maxes out at ~40dp for edge cards — small enough that the approximation is acceptable.

```kotlin
// In the overlay SkillCard's graphicsLayer:
val entryProgress = animateFloatAsState(
    targetValue = if (isAnimatingIn) 1f else 0f,
    animationSpec = tween(300, easing = FastOutSlowInEasing)
)

graphicsLayer {
    val t = entryProgress.value
    val origin = animOrigin ?: AnimOrigin(0f, 0f, 0f)
    
    // Adjust for coordinate space: fan is BottomCenter, overlay is Center
    val yBias = 60.dp.toPx() // vertical offset between the two alignments
    
    // Lerp from fan position to center (0,0)
    translationX = lerp(origin.tx.dp.toPx(), 0f, t)
    translationY = lerp(origin.ty.dp.toPx() + yBias, 0f, t)
    rotationZ = lerp(origin.rot, 0f, t)
    scaleX = lerp(1f, 1.2f, t)
    scaleY = lerp(1f, 1.2f, t)
}
```

#### Hide the fan card when it flies out

When the selected card appears in the overlay, the corresponding card in `HandOfCards` should disappear (not just dim). In `HandOfCards`, set its alpha to 0f when selected:
```kotlin
alpha = if (selectedCardId == item.id) 0f 
        else if (selectedCardId != null && !isSelected) 0.3f 
        else 1f
```

#### Animation on dismiss

When the card is dismissed (tapped outside), reverse the animation — animate from center back to the fan position. Track the "dismissing" state:
```kotlin
var isDismissing by remember { mutableStateOf(false) }

fun dismissCard() {
    isDismissing = true
    // After animation completes, clear selectedCardId:
    delay(300)
    selectedCardId = null
    isAnimatingIn = false
    isDismissing = false
}
```

Use the same `entryProgress` but animated toward 0 when dismissing. This creates a smooth return-to-hand effect.

#### Extra polish (optional): Scale the fan arc when a card is selected

When a card flies out, the remaining cards in the fan could spread wider to fill the gap. This is a nice-to-have — add it only if the basic fly-out + dismiss works cleanly. To implement: recalculate the arc using the same `relativeIndex`/`centerIndex` formula but exclude the selected card from `allCards`.

#### Summary of changes

| File | Line(s) | Change |
|------|---------|--------|
| `BattleActions.kt` — `ActionPanel` | ~57 | Add `animOrigin` + `isAnimatingIn` + `isDismissing` states |
| `BattleActions.kt` — `ActionPanel` | ~155 | `LaunchedEffect(selectedCardId)` to compute origin |
| `BattleActions.kt` — `ActionPanel` | ~196-215 | Replace static overlay positioning with animated `graphicsLayer` |
| `BattleActions.kt` — `ActionPanel` | ~175 | `selectedCardId = null` → call `dismissCard()` instead |
| `BattleActions.kt` — `HandOfCards` | ~313 | Set selected card alpha to 0f |
| `BattleActions.kt` — `HandOfCards` | ~47 | Expose `scrollOffset` to `ActionPanel` |

### 4. Exclusion

When the user clicks the "Use" button on the card, the click should NOT dismiss — it should execute the skill. This is already handled since the "Use" button is a separate clickable element inside the SkillCard, and Compose's event propagation will not reach the background overlay if the overlay is behind the card in Z-order.
