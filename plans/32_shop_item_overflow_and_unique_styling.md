# Plan 32: Fix Shop Item Overflow and Unique Item Styling

## Problem

Three issues in the shop item cards:

1. **Name overflow** — Long item names (e.g. "Phoenix Feather Robe") push the tier badge off-screen or squeeze it because the name `Text` has no width constraint or overflow handling.

2. **Unique item border on wrong layer** — The colored `drawBehind` border is applied to the inner `Row` (inside `GlassCard`), but `GlassCard` has its own 1dp border via `Modifier.border`. The hero color appears on an inner nested layer, not the visible outer edge.

3. **Unique items lack distinct text color and visual pop** — Name text color is already set via `getThemeColor()` (gold for unique), but there's no special treatment beyond that. No shimmer/shine effect to distinguish them.

## Changes

### 1. Fix name overflow

**`ShopScreen.kt:244-249`** — add `maxLines` and `overflow` to the item name, and constrain it with `weight(1f)` so the tier badge stays visible:

```diff
 Row(
     verticalAlignment = Alignment.CenterVertically,
     horizontalArrangement = Arrangement.spacedBy(8.dp)
 ) {
     if (levelLocked) {
         Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(14.dp))
     }
-    Text(
-        item.name,
-        style = MaterialTheme.typography.titleSmall,
-        fontWeight = FontWeight.Bold,
-        color = item.getThemeColor()
-    )
+    Text(
+        item.name,
+        style = MaterialTheme.typography.titleSmall,
+        fontWeight = FontWeight.Bold,
+        color = item.getThemeColor(),
+        maxLines = 1,
+        overflow = TextOverflow.Ellipsis,
+        modifier = Modifier.weight(1f)
+    )
     Surface(
         shape = RoundedCornerShape(4.dp),
         color = item.getThemeColor().copy(alpha = 0.15f)
```

### 2. Fix unique item border to be outer

Move the `drawBehind` border from the inner `Row` to the `GlassCard` itself. `GlassCard` uses `Modifier.border()` (Components.kt line 88-115). Replace the inner Row's conditional drawBehind with a conditional modifier on the entire card.

**Remove** from the inner Row (lines 220-229):
```diff
 Row(
     modifier = Modifier.fillMaxWidth().padding(12.dp)
-        .then(
-            if (item.tier == EquipmentTier.UNIQUE && item.heroId != null)
-                Modifier.drawBehind {
-                    drawRoundRect(
-                        color = item.getThemeColor().copy(alpha = 0.8f),
-                        cornerRadius = CornerRadius(16.dp.toPx()),
-                        style = Stroke(width = 2.dp.toPx())
-                    )
-                }
-            else Modifier
-        ),
     verticalAlignment = Alignment.CenterVertically
 )
```

**Add** to the `GlassCard` modifier (around line 207-217). Since `GlassCard` sets its own `border` parameter, override it for unique items:

```diff
 GlassCard(
     modifier = Modifier
         .fillMaxWidth()
         .padding(vertical = 4.dp)
         .alpha(if (levelLocked) 0.6f else 1f)
         .clickable { onShowDetail() }
+        .then(
+            if (item.tier == EquipmentTier.UNIQUE)
+                Modifier.border(
+                    width = 2.dp,
+                    color = item.getThemeColor().copy(alpha = 0.8f),
+                    shape = RoundedCornerShape(16.dp)
+                )
+            else Modifier
+        ),
     containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
     elevation = 2.dp,
     useDefaultPadding = false
 )
```

### 3. Unique item text color and shimmer

**Text color** — `item.getThemeColor()` already returns `Color(0xFFFFD700)` (gold) for `UNIQUE` tier. Verify `Equipment.kt:86-88` returns gold for `EquipmentTier.UNIQUE` as the fallback. No change needed if the hero-specific override is already producing a gold-ish hue.

**Shimmer effect** — add an animated shimmer/highlight overlay to unique item cards. Add a `shimmerTransition` inside the `ShopItemCard` composable:

```kotlin
// Inside ShopItemCard, before the GlassCard
val shimmerTransition = rememberInfiniteTransition()
val shimmerAlpha by shimmerTransition.animateFloat(
    initialValue = 0f, targetValue = 0.5f,
    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
)

// On the GlassCard or a parent Box
val isUnique = item.tier == EquipmentTier.UNIQUE

GlassCard(
    modifier = Modifier
        ...
        .then(
            if (isUnique) Modifier.drawBehind {
                val shimmerWidth = size.width * 0.4f
                val shimmerX = (size.width * (shimmerAlpha * 2f)) % (size.width * 1.5f) - shimmerWidth
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerX, 0f),
                        end = Offset(shimmerX + shimmerWidth, size.height)
                    )
                )
            } else Modifier
        ),
    ...
)
```

This draws a white-gleam diagonal sweep across unique item cards.

### 4. Unique item text color for name

Ensure unique item names use a rich gold color, not just generic tier color. In `Equipment.kt:83-95`, `getThemeColor()` returns hero-specific colors for hero-linked uniques. For non-hero uniques (if any), it falls back to `tier.getThemeColor()` which is `Color(0xFFFFD700)`. This is correct — gold text for unique items.

## Files to modify

| File | Changes |
|---|---|
| `ShopScreen.kt` | Add `maxLines=1, overflow=Ellipsis, weight(1f)` to item name; move unique border from inner Row to GlassCard; add shimmer overlay for unique items |
| `Components.kt` | (Possibly) expose `border` parameter override in `GlassCard` if not already supported |

## Dependencies

- None. Standalone styling fix.
