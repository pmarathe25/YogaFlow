# Plan 77: Fix Shop Page Bottom Margin

## Problem
The Shop page has excess bottom margin below the `SlotNavigationBar` (weapon/armor/accessory selector). The root cause is **nested Scaffold padding conflict**:

1. `MainActivity.kt` wraps `YogaNavHost` with `Modifier.fillMaxSize().padding(paddingValues)` where `paddingValues` includes system window bottom insets (navigation bar height on 3-button nav, or gesture hint area).
2. `ShopScreen.kt` has its **own** `Scaffold` with `contentWindowInsets = WindowInsets(0,0,0,0)` — which tries to opt out of insets, but the parent already applied bottom padding.
3. `NavigationBar` (Material 3) automatically adds its own bottom padding for system navigation bar.
4. Result: parent bottom inset padding + `NavigationBar` default padding = double spacing below the bar.

The ZenBattle route hides the parent's `YogaBottomBar` but does **not** remove the system window inset padding from the parent Scaffold's `paddingValues`. Even with gesture navigation (where `paddingValues.bottom` might be 0), the `NavigationBar` composable still adds its own padding.

## Fix
Remove the nested `Scaffold` from `ShopScreen.kt`. Replace with a `Column` layout:
- Weighted column for scrollable content
- `SlotNavigationBar` at the bottom (no additional Scaffold wrapping)

This eliminates the double-padding problem entirely.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ShopScreen.kt`

## Changes

Replace the current structure:
```kotlin
Scaffold(
    containerColor = Color.Transparent,
    topBar = { ... },
    bottomBar = { SlotNavigationBar(...) },
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        ...
    }
}
```

With:
```kotlin
Column(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
    // Top bar area
    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
        // Header
        // Tier filter row
        // Buff description area (if applicable)
        // Item list LazyColumn (weight(1f) or fillMaxSize)
    }
    // Bottom bar
    SlotNavigationBar(
        selectedSlot = selectedSlot,
        onSlotSelected = { selectedSlot = it },
        inventorySlots = inventorySlots
    )
}
```

Key details:
- The `backButton` / title row remains at the top inside the weighted column
- The `LazyColumn` for items gets `weight(1f)` so it fills remaining space
- The `SlotNavigationBar` is rendered at the natural bottom of the Column with no additional padding
- `SlotNavigationBar` internal `NavigationBar` will still apply its own bottom system bar padding (Material 3 default), which is correct — it should extend into the system navigation bar area

## Unresolved
If `SlotNavigationBar` still looks too low (extends past safe area), we may need to adjust `MainActivity.kt` to NOT apply `Modifier.padding(paddingValues)` for the ZenBattle route specifically. This is a separate concern and should be tested before adding.

## Verification
- Navigate to Shop → SlotNavigationBar is flush with bottom of screen (no gap)
- Items scroll properly above the bar
- Hero item select dialog opens correctly on slot tap
- Other ZenBattle screens (Battle, Party, etc.) unaffected

## Dependencies
- None
