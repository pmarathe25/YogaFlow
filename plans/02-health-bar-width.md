# Plan: Fix Hero Health Bar Width

## Issue Addressed
- **Issue 2**: The hero health bars are too wide. Previous width reductions had no effect, indicating a deeper layout issue.

---

## Root Cause Analysis

### How the width is currently set

In `BattleHUD.kt`, `FloatingHUD` applies width via:
```kotlin
Column(
    modifier = modifier.width(width.dp),
    ...
)
```

`HeroHUD` passes `width = 60`:
```kotlin
FloatingHUD(..., modifier, ..., width = 60)
```

In `BattleScreen.kt`, each hero Column is inside a `Row` with `weight(1f)`:
```kotlin
Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
    state.aliveHeroes.forEach { hero ->
        Column(modifier = Modifier.weight(1f)...) {
            HeroHUD(..., modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
        }
    }
}
```

**The problem**: `HeroHUD` receives `Modifier.fillMaxWidth().padding(bottom = 4.dp)` and passes it to `FloatingHUD`. Inside `FloatingHUD`, the modifier chain becomes:
```
Modifier.fillMaxWidth().padding(bottom = 4.dp).width(60.dp)
```

In Compose, `fillMaxWidth()` sets the width to the maximum available space. Then `width(60.dp)` overrides it to exactly 60dp. However, the `horizontalArrangement = Arrangement.SpaceEvenly` and `weight(1f)` on each column may interact unexpectedly, causing the Column to expand to fill the Row's space regardless of the inner `width(60.dp)` constraint.

The `.width(60.dp)` is applied to the **Column** itself, but the Column's content (HP bar Canvas, Text, etc.) uses `Modifier.fillMaxWidth()` within the Column. The Column's width should be constrained to 60dp, but if the Row's weight allocation forces a minimum width wider than 60dp, the constraint may not take effect as expected.

### Additional issue: MonsterHUD width

`MonsterHUD` passes `width = 100`, which may also be wider than needed but is less problematic since there's only one monster.

---

## Changes Required

### 1. `BattleHUD.kt` — `FloatingHUD` composable

Replace `modifier.width(width.dp)` with `Modifier.requiredWidth(width.dp)`, which has higher precedence and cannot be overridden by parent constraints:

```kotlin
Column(
    modifier = modifier.requiredWidth(width.dp),
    ...
)
```

Use `requiredWidth` only. If the build fails (API level check), fall back to `.width(width.dp).fillMaxWidth(false)`. `requiredWidth` is available since Compose 1.0.

### 2. `BattleScreen.kt` — Hero Row layout

Remove `Modifier.weight(1f)` from the hero Column to allow the inner `requiredWidth(60.dp)` to take full effect. Instead, use a simpler arrangement:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    state.aliveHeroes.forEach { hero ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(...)
        ) {
            HeroHUD(hero = hero, ..., modifier = Modifier)
            ...
        }
    }
}
```

Remove the `fillMaxWidth()` from the HeroHUD modifier in the call site, since `FloatingHUD` will now enforce a fixed width internally.

**⚠️ Consequence of removing `weight(1f)`:** Without `weight(1f)`, hero columns will collapse to their natural width (controlled by `requiredWidth(60.dp)`). This means they'll pack to the left of the Row. To keep them centered and evenly distributed, add fixed-width spacers between columns, or wrap each hero Column in a `Box` with `Modifier.weight(1f)` while removing weight from the Column itself:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    state.aliveHeroes.forEach { hero ->
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(...)) {
                HeroHUD(hero = hero, ..., modifier = Modifier)
                ...
            }
        }
    }
}
```

The `Box` with `weight(1f)` distributes space evenly, while the inner Column with `requiredWidth(60.dp)` constrains actual HUD width.

### 3. `BattleHUD.kt` — `HeroHUD` default width

Reduce the default hero HUD width further if needed (e.g., from 60 to 50dp), and verify visually.

### 4. Stylize health bars with vertical gradient (3D effect)

Currently the HP bar fill uses a single flat color via `drawRoundRect` (line 97-101 of `BattleHUD.kt`):
```kotlin
drawRoundRect(
    color = hpBarColor ?: hpColorFromPercent(animatedHpPercent),
    size = Size(size.width * animatedHpPercent, size.height),
    cornerRadius = CornerRadius(2f)
)
```

Replace the flat fill with a `Brush.verticalGradient` that simulates lighting from above:

```kotlin
// HP fill color
val baseColor = hpBarColor ?: hpColorFromPercent(animatedHpPercent)

// Fill with vertical gradient for 3D effect
drawRect(
    brush = Brush.verticalGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.85f),       // slightly transparent at top
            baseColor,                              // solid base color
            baseColor.copy(red = baseColor.red * 0.7f, green = baseColor.green * 0.7f, blue = baseColor.blue * 0.7f) // darker at bottom
        ),
        startY = 0f,
        endY = size.height
    ),
    size = Size(size.width * animatedHpPercent, size.height),
    // Use drawRoundRect with clipping or drawRect with cornerRadius parameter
)
```

Use `drawRoundRect` with a `Brush` parameter — Compose's `DrawScope` has an overload of `drawRoundRect` that accepts `brush`. If the overload isn't resolved, use `drawRoundRect` with `color` for the background outline only, and fill the bar with a separate `drawRect` using `Brush.verticalGradient`. Refactor to:

```kotlin
// Background
drawRoundRect(
    color = Color.Black.copy(alpha = 0.5f),
    cornerRadius = CornerRadius(3f)
)

// Fill with vertical gradient
val fillWidth = size.width * animatedHpPercent
if (fillWidth > 0f) {
    // Clip to rounded rect by using drawRoundRect with gradient brush
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.9f),         // lighter top (simulates light)
                baseColor,                               // mid
                baseColor.copy(                          // darker bottom (shadow)
                    red = (baseColor.red * 0.6f).coerceAtMost(1f),
                    green = (baseColor.green * 0.6f).coerceAtMost(1f),
                    blue = (baseColor.blue * 0.6f).coerceAtMost(1f)
                )
            )
        ),
        size = Size(fillWidth, size.height),
        cornerRadius = CornerRadius(3f)
    )
    
    // Subtle top-edge highlight for extra depth
    drawRoundRect(
        color = Color.White.copy(alpha = 0.15f),
        size = Size((fillWidth).coerceAtLeast(1f), size.height * 0.3f),
        cornerRadius = CornerRadius(1f)
    )
}
```

**Note:** Compose `DrawScope.drawRoundRect` has an overload that accepts `brush`. If not available directly, import `androidx.compose.ui.graphics.drawscope.drawRoundRect` or use `drawRoundRect(color = ...)` for the outline and fill with a `DrawScope` extension. The brush overload is available in Compose 1.2+.

**Additional polish:** Apply the same gradient treatment to the **shield bar** (line 78-83) — use a vertical gradient from light blue to darker blue:

```kotlin
val shieldPct = (shield.toFloat() / maxHp).coerceAtMost(1f)
val shieldWidth = size.width * shieldPct
if (shieldWidth > 0f) {
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF64B5F6),  // light blue top
                Color(0xFF1E88E5)   // darker blue bottom
            )
        ),
        size = Size(shieldWidth, size.height),
        cornerRadius = CornerRadius(1f)
    )
}
```

Similarly for the **ultimate gauge** bar — add a subtle gold vertical gradient.

### 5. Verify in `FloatingHUD`

Ensure all child composables (HP bar Canvas, shield bar, ultimate gauge, status icons) use `Modifier.fillMaxWidth()` within the Column so they correctly fill the constrained width.
