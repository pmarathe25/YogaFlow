# Plan 29: Refine Path of Zen Page Layout

## Problem

The new Path of Zen portal page (Plan 26) has several rough edges:
- Title "Path of Zen" is below the spiral, centered — wastes vertical space
- Subtitle "Walk the path of enlightenment" is redundant
- Level card is above the spiral, breaking the visual hierarchy
- Trophies button navigates to the newer `TrophyScreen` (battle trophies only) instead of the older `TrophyModal` (which also shows yoga achievements)
- Preview circles show no distinction between locked/unlocked encounters

## Changes

### 1. Move title to top row, remove subtitle

**`JourneyScreen.kt`** — replace the sparks/gold-only top row with a row containing the title (left) and sparks/gold (right). Remove the "Path of Zen" title block and subtitle that currently sit below the spiral.

Before (lines 121-138 + 176-186):
```
Row: [sparks] [gold]   (right-aligned)
...
Spacer
LevelCardCompact
Spacer
Spiral Canvas
Spacer
"Path of Zen" title center
"Walk the path..." subtitle center
```

After:
```
Row: "Path of Zen" title (left)  [⚡ N] [💰 N] (right)
Spacer
Spiral Canvas
Spacer
LevelCardCompact (under spiral)
```

Replace the top Row:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        "Path of Zen",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = Color(0xFFE8F5E9)
    )
    Spacer(Modifier.weight(1f))
    Text("⚡", fontSize = 16.sp)
    Spacer(Modifier.width(4.dp))
    Text("$sparks",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.width(12.dp))
    Icon(Icons.Default.MonetizationOn, contentDescription = null,
        tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(4.dp))
    Text("$gold",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = Color(0xFFFFD700))
}
```

Remove the old title + subtitle block (currently lines 176-186). Move `LevelCardCompact` to after the spiral (lines 142-148 move to after line 172).

### 2. Move level card under spiral

The order becomes:
1. Title row (Path of Zen + sparks/gold)
2. Spacer
3. Spiral Canvas
4. Spacer
5. LevelCardCompact
6. Spacer
7. "Encounters ahead:" + preview row
8. "ENTER THE PATH" button
9. Nav buttons

### 3. Replace TrophyScreen with TrophyModal

**`JourneyScreen.kt`** — instead of wiring the Trophies nav button to `onNavigateToTrophies` (which navigates to a separate route), show `TrophyModal` as a dialog overlay:

Add state:
```kotlin
var showTrophies by remember { mutableStateOf(false) }
```

Add after the level dialog:
```kotlin
if (showTrophies) {
    val achievements by viewModel.achievements.collectAsState()
    TrophyModal(
        achievements = achievements,
        earnedTrophyIds = gameSaveData.earnedTrophyIds,
        onDismiss = { showTrophies = false }
    )
}
```

Change the Trophies nav button onClick:
```diff
 NavCard(
     title = "Trophies", icon = "🏆",
     subtitle = "Achievements",
-    onClick = onNavigateToTrophies,
+    onClick = { showTrophies = true },
     modifier = Modifier.weight(1f)
 )
```

Import `TrophyModal`:
```kotlin
import com.example.game.ui.components.TrophyModal
```

### 4. Locked encounter previews

**`JourneyScreen.kt`** — update the monster preview row to show locked state. Calculate lock status based on defeated IDs:

```kotlin
val normDefeated = remember(gameSaveData.defeatedMonsterIds) {
    gameSaveData.defeatedMonsterIds.map { it.lowercase() }.toSet()
}
```

For each monster in the preview, determine if it's locked:
```kotlin
previewMonsters.forEachIndexed { index, monster ->
    // First monster is always unlocked; others are unlocked if the previous is defeated
    val isUnlocked = index == 0 || normDefeated.contains(
        previewMonsters[index - 1].id.lowercase()
    )
    val isDefeated = normDefeated.contains(monster.id.lowercase())
    MonsterPreviewCircle(
        monster = monster,
        size = 52.dp,
        isLocked = !isUnlocked,
        isDefeated = isDefeated
    )
}
```

Update `MonsterPreviewCircle` signature and body:

```diff
 @Composable
-private fun MonsterPreviewCircle(monster: Monster, size: Dp = 44.dp) {
+private fun MonsterPreviewCircle(monster: Monster, size: Dp = 44.dp, isLocked: Boolean = false, isDefeated: Boolean = false) {
     val elColor = elementToColor(monster.element)
     Box(
         modifier = Modifier.size(size),
         contentAlignment = Alignment.Center
     ) {
         Canvas(modifier = Modifier.fillMaxSize()) {
             val c = this.size.width / 2f
+            val alpha = if (isLocked) 0.3f else if (isDefeated) 0.5f else 1f
-            drawCircle(elColor.copy(alpha = 0.2f), c, Offset(c, c))
-            drawCircle(elColor.copy(alpha = 0.5f), c * 0.6f, Offset(c, c), style = Stroke(width = 2f))
-            drawMonsterShape(c * 0.5f, c * 0.6f, c * 0.7f, monster.name, elColor)
+            drawCircle(elColor.copy(alpha = 0.2f * alpha), c, Offset(c, c))
+            drawCircle(elColor.copy(alpha = 0.5f * alpha), c * 0.6f, Offset(c, c), style = Stroke(width = 2f))
+            drawMonsterShape(c * 0.5f, c * 0.6f, c * 0.7f, monster.name,
+                tint = if (isDefeated) Color.Gray else elColor.copy(alpha = alpha))
+            // Lock icon
+            if (isLocked) {
+                drawCircle(Color(0xFF37474F).copy(alpha = 0.6f), c * 0.7f, Offset(c, c))
+                drawCircle(Color(0xFF455A64).copy(alpha = 0.3f), c * 0.8f, Offset(c, c))
+                // Simple lock shape
+                val lockPaint = android.graphics.Paint().apply {
+                    color = android.graphics.Color.WHITE
+                    textSize = c * 0.9f
+                    textAlign = android.graphics.Paint.Align.CENTER
+                    isAntiAlias = true
+                }
+                drawContext.canvas.nativeCanvas.drawText("🔒", c, c + c * 0.35f, lockPaint)
+            }
         }
     }
 }
```

> Note: The emoji `"🔒"` is drawn via native canvas. Alternative: use `drawCircle` + `drawRect` to draw a lock shape procedurally if emoji rendering is inconsistent.

## Files to modify

| File | Changes |
|---|---|
| `JourneyScreen.kt` | Move title to top row with sparks/gold; remove subtitle; move level card below spiral; add `showTrophies` state + `TrophyModal`; add locked/defeated state to preview monsters; update `MonsterPreviewCircle` with `isLocked`/`isDefeated` params |

## Dependencies

- Builds on Plan 26 (the new portal page layout). Apply after Plan 26.
