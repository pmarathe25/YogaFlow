# Plan: Remove Top Margin from Shop Screen

**File:** `app/src/main/java/com/example/game/ui/components/ShopScreen.kt:39`

## Change

Replace the Column modifier to remove `statusBarsPadding()` and change the 16dp uniform padding to horizontal+bottom only:

```kotlin
// Before:
Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp))

// After:
Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
```

## Why

The `statusBarsPadding()` pushes content below the system status bar, which creates unnecessary empty space at the top of the shop screen. The header (back button, title, gold display) doesn't need to be pushed below the status bar since the background already extends edge-to-edge.

## Affected files

| File | Change |
|------|--------|
| `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` | Modify Column modifier on line 39 |
