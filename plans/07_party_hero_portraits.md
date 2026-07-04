# Plan: Hero Portraits in Party Screen

## Problem

The party hero list currently shows a colored circle with the first letter of the hero's name (e.g., "S" for Shanti). This is generic and doesn't convey the hero's identity.

## Solution

Reuse the existing procedural silhouette drawing from `BattleCanvas.kt:drawSilhouette()` to render each hero's unique portrait in the party list.

## Implementation

**File:** `app/src/main/java/com/example/game/ui/components/PartyScreen.kt:124-136`

Replace the current `Box` with Circle+initial with a composable that renders the hero's silhouette. Create a new `HeroPortrait` composable (either in PartyScreen.kt or a shared file) that wraps the Canvas drawing logic:

```kotlin
@Composable
fun HeroPortrait(
    heroId: String,
    elementColor: Color,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = size.width / 2f
        val cy = size.height * 0.6f
        val s = size.minDimension * 0.2f
        drawSilhouette(cx.toPx(), cy.toPx(), s.toPx(), heroId, elementColor)
    }
}
```

Update `HeroListItem` to use it:

```kotlin
Box(
    modifier = Modifier
        .size(56.dp)
        .clip(CircleShape)
        .background(heroColor.copy(alpha = 0.2f)),
    contentAlignment = Alignment.Center
) {
    if (!isUnlocked) {
        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray)
    } else {
        HeroPortrait(hero.id, heroColor, 48.dp)
    }
}
```

The `drawSilhouette()` function is currently `internal fun` in `BattleCanvas.kt`. It needs to be made `public` (remove `internal`) or the new `HeroPortrait` composable should live in the same package.

**Alternative** (cleaner): Create a standalone `HeroPortrait` composable in a new file like `app/src/main/java/com/example/game/ui/components/HeroPortrait.kt` that wraps the Canvas + drawSilhouette call:

```kotlin
package com.example.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun HeroPortrait(
    heroId: String,
    elementColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.6f
        val s = size.minDimension * 0.2f
        drawSilhouette(cx, cy, s, heroId, elementColor)
    }
}
```

This requires `drawSilhouette` to be accessible (make it `public` by removing `internal` from its declaration in `BattleCanvas.kt:435`).

## Also update HeroDetailsDialog

**File:** `PartyScreen.kt:186-192`

Replace the hero initial circle in the HeroDetailsDialog header with the same portrait rendering:

```kotlin
Box(
    modifier = Modifier.size(64.dp).background(heroColor.copy(alpha = 0.1f), CircleShape),
    contentAlignment = Alignment.Center
) {
    HeroPortrait(hero.id, heroColor, 56.dp)
}
```

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/java/com/example/game/ui/components/PartyScreen.kt` | Replace initial circle with `HeroPortrait` composable in both `HeroListItem` and `HeroDetailsDialog` |
| `app/src/main/java/com/example/game/ui/components/HeroPortrait.kt` | **NEW** — standalone composable wrapping `drawSilhouette` |
| `app/src/main/java/com/example/game/ui/components/BattleCanvas.kt` | Change `drawSilhouette` visibility from `internal` to `public` |
