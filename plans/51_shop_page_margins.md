# Plan 51: Remove Extra Shop Page Margins

## Problem

The Shop page has multiple layers of padding that create unnecessary whitespace around the edges:

1. **Column** (line 63): `Modifier.padding(start = 16.dp, end = 16.dp)` — 16dp horizontal padding
2. **Scaffold content lambda** (line 62): `.padding(padding)` — scaffold insets padding
3. **LazyColumn** (line 135): `Modifier.padding(horizontal = 12.dp)` — additional 12dp horizontal padding
4. **ShopItemCard** (line 270): `Modifier.padding(vertical = 4.dp)` — vertical card spacing

The combination of 16dp + 12dp = 28dp of horizontal padding on each side is excessive and the outer `Box` + `Scaffold` nesting adds unnecessary containers.

## Fix

### 1. Remove outer `Box` and simplify layout

**`ShopScreen.kt:51-62`** — the outer `Box` wrapping `Scaffold` with only a background is unnecessary. Apply `Modifier.background()` directly to the `Scaffold` or the content `Column`:

Before:
```kotlin
Box(
    modifier = Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
) {
    Scaffold(
        bottomBar = { SlotNavigationBar(...) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize()
            .padding(start = 16.dp, end = 16.dp)
            .padding(padding)) {
```

After:
```kotlin
Scaffold(
    bottomBar = { SlotNavigationBar(...) },
    modifier = Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
) { padding ->
    Column(modifier = Modifier.fillMaxSize()
        .padding(horizontal = 16.dp)
        .padding(padding)) {
```

### 2. Remove LazyColumn horizontal padding

**`ShopScreen.kt:135`** — the additional 12dp horizontal padding on the `LazyColumn` is redundant since the parent `Column` already has 16dp:

Before:
```kotlin
LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
```

After:
```kotlin
LazyColumn(modifier = Modifier.weight(1f))
```

### 3. Reduce/remove top margin before tier filter

**`ShopScreen.kt:105`** — `Spacer(Modifier.height(8.dp))` and **line 124** `Spacer(Modifier.height(12.dp))` can be reduced to 4dp each for tighter spacing:

```kotlin
Spacer(Modifier.height(4.dp))
...
Spacer(Modifier.height(8.dp))
```

### 4. Close the dangling brace

With the outer `Box` removed, the closing brace at line 190 (`}`) and the extra indentation at line 189 (`}`) need to be cleaned up to ensure the `Scaffold` content block closes properly.

## Files to modify

| File | Changes |
|---|---|
| `ShopScreen.kt` | Remove outer `Box`; move background to `Scaffold`; remove `LazyColumn` horizontal padding; reduce spacer heights |

## Dependencies

- None. Standalone layout cleanup.
