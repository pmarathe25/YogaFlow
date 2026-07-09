# Plan 72: Fix Shop Screen Top/Bottom Margins

## Problem
The Shop screen has excessive top and bottom margins. The `Scaffold` on `ShopScreen.kt:48-57` previously had `contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)` which was removed by Plan 68. Without an explicit `contentWindowInsets`, `Scaffold` defaults to `WindowInsets.systemBars`, which adds **both** top (status bar) and bottom (navigation bar) system insets as content padding. The `.padding(padding)` applied to the Column at line 58 includes these insets, creating:

- **Top margin**: ~24–48dp gap at the top (status bar height) between the screen edge and the header Row
- **Bottom margin**: navigation bar height added below the content

The user previously asked for the NavigationBar to be the bottom-most element with nothing below it. The default system bar insets break this.

## Fix
Set `contentWindowInsets` to `WindowInsets(0, 0, 0, 0)` to explicitly disable all system bar insets from being added as content padding. The Scaffold's `bottomBar` slot already handles the NavigationBar positioning correctly without needing extra insets.

**`ShopScreen.kt:48-57`**:
```kotlin
// Before:
Scaffold(
    bottomBar = {
        SlotNavigationBar(
            selectedSlot = selectedCategory,
            onSlotSelected = { selectedCategory = it }
        )
    },
    modifier = Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {

// After:
Scaffold(
    bottomBar = {
        SlotNavigationBar(
            selectedSlot = selectedCategory,
            onSlotSelected = { selectedCategory = it }
        )
    },
    modifier = Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
```

This ensures `padding` only contains the NavigationBar height (from `bottomBar` slot), with zero system bar contribution.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` — add `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to Scaffold

## Dependencies
- Supersedes the incomplete fix from Plan 68
