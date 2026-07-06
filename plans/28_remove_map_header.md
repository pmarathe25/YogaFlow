# Plan 28: Remove Top Bar from Path of Zen Map

## Problem

`MonsterRoadSelection` draws a `MapHeader` at the top of the screen with a back button and "The Path of Zen" title. This duplicates information already on the Journey/portal page and the system back button already handles navigation. The header consumes 56dp of vertical space that the map could use.

## Changes

### 1. Remove `MapHeader` composable and its call

**`MonsterRoadSelection.kt:88`** — delete the `MapHeader(onBack = onBack)` line.

**`MonsterRoadSelection.kt:157-183`** — delete the entire `MapHeader` composable function.

### 2. Remove the `onBack` parameter

Since the only consumer was `MapHeader`, the parameter is no longer needed:

```diff
 fun MonsterRoadSelection(
     monsters: List<Monster>,
     defeatedIds: Set<String>,
-    onMonsterSelected: (Monster) -> Unit,
-    onBack: () -> Unit
+    onMonsterSelected: (Monster) -> Unit
 )
```

### 3. Remove top padding from scrollable area

**`MonsterRoadSelection.kt:93`** — change the padding that used to accommodate the header:

```diff
 BoxWithConstraints(
     modifier = Modifier
         .fillMaxSize()
-        .padding(top = 56.dp)
         .verticalScroll(scrollState)
 )
```

### 4. Update callers

**`JourneyScreen.kt`** (in `ExpandedDashboardScreen`, the `showMonsterRoad` branch):

```diff
 MonsterRoadSelection(
     monsters = DataLoader.monsters,
     defeatedIds = gameSaveData.defeatedMonsterIds,
-    onMonsterSelected = { monster ->
+    onMonsterSelected = { monster ->
         gameViewModel.startBattle(monster.id)
         onNavigateToBattle()
         showMonsterRoad = false
     },
-    onBack = { showMonsterRoad = false }
 )
```

The map is no longer toggled off by a GUI back button. The system back button pops back to the Journey portal page (or Dashboard, depending on the nav stack). The `showMonsterRoad` state is reset when `onMonsterSelected` fires (navigating to battle).

**`HubScreen.kt`** (if still present):

```diff
 MonsterRoadSelection(
     monsters = DataLoader.monsters,
     defeatedIds = saveData.defeatedMonsterIds,
     onMonsterSelected = { onNavigateToBattle(it.id) },
-    onBack = onExitHub
 )
```

System back button handles navigation out of the Hub. The existing `BackHandler` in `YogaNavHost.kt:154-155` already calls `navController.popBackStack()` when the system back is pressed on the ZenBattle route.

### 5. (Optional) Clean up unused imports

After removing `MapHeader`, check `MonsterRoadSelection.kt` for any imports that are now unused:
- `Icons.AutoMirrored.filled.ArrowBack` — only used in `MapHeader`
- `Icons` (entirely, if no other icon usage)

Only remove if no other code in the file uses them.

## Files to modify

| File | Changes |
|---|---|
| `MonsterRoadSelection.kt` | Remove `MapHeader` composable and its call; remove `onBack` param; remove `padding(top = 56.dp)` from scroll area |
| `JourneyScreen.kt` | Remove `onBack` argument from `MonsterRoadSelection` call |
| `HubScreen.kt` | Remove `onBack` argument from `MonsterRoadSelection` call |

## Dependencies

- None. Standalone change.
