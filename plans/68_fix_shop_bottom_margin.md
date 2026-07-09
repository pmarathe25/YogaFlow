# Plan 68: Fix Shop Bottom Margin

## Problem
The Shop page (`ShopScreen.kt`) has a large visible gap between the last item in the `LazyColumn` and the `NavigationBar` at the bottom. This gap comes from two sources:

1. **`contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)`** (line 57) — This tells Scaffold to reserve space for the system navigation bar (gesture handle area) at the bottom. On devices with gesture navigation, this adds ~32dp of transparent padding below the NavigationBar. Combined with the NavigationBar's own 80dp height, the user sees a large empty area below the content before reaching the NavigationBar, plus extra space below it.

2. **`Modifier.weight(1f)` on `LazyColumn`** (line 125) — The LazyColumn fills all remaining vertical space in the Column, which pushes items apart when there are few items, but more importantly it means the Scaffold's bottom `contentPadding` (NavigationBar height) is added as empty space between the last item and the NavigationBar.

The user wants:
- The Weapon/Armor/Accessory selector (`NavigationBar`) to be the **bottom-most item** on the page
- Nothing below the NavigationBar
- No large margin/padding at the bottom

## Fix

### 1. Remove `contentWindowInsets`
Remove the `contentWindowInsets` line entirely (line 57). The `Scaffold` default behavior adds bottom padding equal to the `bottomBar` height, which is sufficient. The system bar inset is not needed because the `NavigationBar` already occupies the bottom of the screen:

```kotlin
// Before:
Scaffold(
    bottomBar = { SlotNavigationBar(...) },
    modifier = Modifier.fillMaxSize().background(...),
    contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
) { padding -> ... }

// After:
Scaffold(
    bottomBar = { SlotNavigationBar(...) },
    modifier = Modifier.fillMaxSize().background(...)
) { padding -> ... }
```

Also remove the unused import for `WindowInsets` / `WindowInsetsSides` if they become unused.

### 2. Reduce LazyColumn bottom padding
The `padding(padding)` from Scaffold already adds the NavigationBar height as bottom padding. To minimize the gap between the last item and the NavigationBar, ensure no additional bottom padding is added:

```kotlin
Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp))
```

This is already correct — the `.padding(padding)` adds only the NavigationBar height at the bottom, and the horizontal padding doesn't affect vertical spacing.

If there's still a visible gap after removing `contentWindowInsets`, reduce the NavigationBar's own bottom padding by setting `tonalElevation = 0.dp` in `SlotNavigationBar` (this removes the shadow below it) and verify the NavigationBar `Modifier` has no extra padding.

### 3. Ensure NavigationBar is truly bottom-most
Verify that the `SlotNavigationBar` inside `Scaffold.bottomBar` renders with no extra container below it. The `NavigationBar` composable (line 196) should not have any outer wrapper adding bottom margins.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` — line 57: remove `contentWindowInsets = ...` line; clean up unused imports

## Dependencies
- Supersedes Plan 59 (which incorrectly added `contentWindowInsets`)
