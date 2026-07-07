# Plan 42: Encounters Ahead Horizontal Scroll

## Problem

The "Encounters Ahead" preview on the dashboard (`JourneyScreen.kt`) uses a `Row` with `Arrangement.spacedBy(16.dp)` showing only the first 6 monsters (`DataLoader.monsters.take(6)`). It is not scrollable and always starts with Bhaya (the first monster), even if the player has already progressed much further.

The full monster road view (`MonsterRoadSelection`) uses vertical scroll, which makes navigation feel unnatural for a horizontal list of encounters.

## Fix

### 1. Make the dashboard preview scroll horizontally

**`JourneyScreen.kt:199-219`** — replace the `Row` with a `LazyRow` so the user can scroll through all monsters, not just the first 6:

Before:
```kotlin
val previewMonsters = remember { DataLoader.monsters.take(6) }
...
Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    ...
) {
    previewMonsters.forEach { monster -> MonsterPreviewCircle(...) }
}
```

After:
```kotlin
LazyRow(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    ...
) {
    items(DataLoader.monsters) { monster ->
        MonsterPreviewCircle(
            monster = monster,
            ...
        )
    }
}
```

### 2. Scroll to furthest unlocked monster by default

**`JourneyScreen.kt`** — add a `LazyListState` and use `LaunchedEffect` to scroll to the furthest defeated/unlocked monster on first composition:

```kotlin
val listState = rememberLazyListState()

LaunchedEffect(Unit) {
    val defeatedIds = gameSaveData.defeatedMonsterIds
    val furthestIdx = DataLoader.monsters.indexOfLast { m ->
        m.id.lowercase() in defeatedIds.map { it.lowercase() }
    }
    val targetIdx = (furthestIdx + 1).coerceAtMost(DataLoader.monsters.lastIndex)
    if (targetIdx > 0) {
        listState.animateScrollToItem(targetIdx)
    }
}
```

This finds the last defeated monster and scrolls to the next one (the next challenge). If none are defeated, it stays at index 0 (Bhaya).

### 3. Full monster road view

The full-screen `MonsterRoadSelection` currently uses vertical scroll. Redesigning this to horizontal is a larger effort and should be handled separately. For now, only the preview row on the dashboard is changed.

## Files to modify

| File | Changes |
|---|---|
| `JourneyScreen.kt` | Replace `Row` with `LazyRow` showing all monsters; add `LazyListState`; add `LaunchedEffect` to scroll to furthest unlocked monster |

## Dependencies

- None. Standalone UI fix for the preview row only.
