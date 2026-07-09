# Plan 84: Fix Shop Page Bottom Margin (Final)

## Problem
The Shop page still has a large bottom margin below the `SlotNavigationBar` buttons. Plan 77 removed the nested `Scaffold`, but the margin persists because:

1. **Parent padding**: `MainActivity.kt:137` applies `Modifier.fillMaxSize().padding(paddingValues)` to `YogaNavHost`. When on the ZenBattle route, `showBottomBar = false`, so the parent `Scaffold` has no bottom bar — but `paddingValues` still includes system window bottom insets (navigation bar area).

2. **Double consumption**: The parent's `padding(paddingValues)` adds bottom padding. Then `SlotNavigationBar`'s inner `NavigationBar` composable ALSO adds its own bottom padding (Material 3 automatically consumes `WindowInsets.navigationBars`). The result: bottom inset padding (48dp on 3-button nav) + NavigationBar padding (another 48dp) = ~96dp of empty space below the buttons.

## Fix
In `MainActivity.kt`, remove the bottom system window inset padding from `paddingValues` when the ZenBattle route is active. This way, `NavigationBar`'s automatic window inset consumption is the ONLY source of bottom padding, and it only applies once.

```kotlin
// MainActivity.kt
Scaffold(...) { paddingValues ->
    val adjustedPadding = if (currentRoute == Screen.ZenBattle.route) {
        PaddingValues(top = paddingValues.top, bottom = 0.dp)
    } else {
        paddingValues
    }
    YogaNavHost(
        ...
        modifier = Modifier.fillMaxSize().padding(adjustedPadding)
    )
}
```

This affects ALL ZenBattle screens (Battle, Party, Shop, etc.), which is correct:
- **Shop**: SlotNavigationBar's NavigationBar adds its own bottom inset → correct single padding
- **Battle**: ActionTray doesn't use NavigationBar → but it's outside the padded area anyway (overlaid via zIndex), so bottom insets matter less. The `NavigationBar` in `SlotNavigationBar` for equip dialogs in Party screen will handle their own insets.
- **Party**: Uses its own layout, no NavigationBar dependency

Additionally, add `windowInsets` override to the `SlotNavigationBar` in `ShopScreen.kt` as a safety net:

```kotlin
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    windowInsets = WindowInsets(0, 0, 0, 0)
) {
```

This ensures no double-padding even if the parent's bottom padding isn't fully removed.

## Files to Modify
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/game/ui/components/ShopScreen.kt`

## Changes

### Change 1: MainActivity.kt lines 128-138
**Before:**
```kotlin
) { paddingValues ->
    YogaNavHost(
        navController = navController,
        viewModel = viewModel,
        gameViewModel = gameViewModel,
        isCountdownActive = isCountdownActive,
        isCompleted = isCompleted,
        modifier = Modifier.fillMaxSize().padding(paddingValues)
    )
}
```

**After:**
```kotlin
) { paddingValues ->
    val adjustedPadding = if (currentRoute == Screen.ZenBattle.route) {
        PaddingValues(top = paddingValues.top, bottom = 0.dp)
    } else {
        paddingValues
    }
    YogaNavHost(
        navController = navController,
        viewModel = viewModel,
        gameViewModel = gameViewModel,
        isCountdownActive = isCountdownActive,
        isCompleted = isCompleted,
        modifier = Modifier.fillMaxSize().padding(adjustedPadding)
    )
}
```

Add import:
```kotlin
import androidx.compose.foundation.layout.PaddingValues
```
(Check if already imported.)

### Change 2: ShopScreen.kt line 193
**Before:**
```kotlin
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp
) {
```

**After:**
```kotlin
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    windowInsets = WindowInsets(0, 0, 0, 0)
) {
```

## Verification
- Open Shop → SlotNavigationBar buttons sit at the very bottom of the screen (with only the `NavigationBar` internal padding for system nav bar)
- No gap between the buttons and the bottom of the screen (on gesture navigation)
- No double spacing below the bar (on 3-button navigation)
- Battle screen ActionTray still shows at bottom (check Plan 80 for `align(BottomCenter)`)
- Other ZenBattle screens (Party, Battle) unaffected or improved
- Main navigation tabs (Dashboard, History, Settings, ZenBattle) show correctly

## Dependencies
- Plan 80 may also need `windowInsetsBottom` for the Battle ActionTray to handle bottom insets after parent padding removal
