# Plan 85: Fix Shop Bottom Padding (Overcorrection)

## Problem
Plan 84 overcorrected the Shop bottom margin issue. Removing the parent's bottom padding AND setting `windowInsets = WindowInsets(0,0,0,0)` on the `NavigationBar` stripped ALL bottom safe-area padding. The SlotNavigationBar buttons now sit UNDER the system navigation bar (back/home/multitasking buttons).

## Root Cause
Two changes in Plan 84 caused this:
1. `MainActivity.kt`: Parent bottom padding removed for ZenBattle route → `PaddingValues(bottom = 0)`
2. `ShopScreen.kt`: `NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0))` → NavigationBar no longer pads for system nav bar

Combined, there is zero bottom padding anywhere → buttons overlap system nav bar.

## Fix
**Remove change #2 only.** Keep the parent bottom padding removal (change #1) — it correctly prevents double-padding with the Battle screen's own `windowInsetsPadding`. Restore the `NavigationBar`'s default `windowInsets`, so it correctly consumes system navigation bar insets ONCE.

The `NavigationBar` will add ~48dp of bottom padding (on 3-button nav) or ~16dp (on gesture nav), which is the correct amount — exactly what's needed to avoid the system nav bar.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/ShopScreen.kt`

## Changes

### ShopScreen.kt — line 196
Remove `windowInsets = WindowInsets(0, 0, 0, 0)` from `NavigationBar`:

**Before:**
```kotlin
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    windowInsets = WindowInsets(0, 0, 0, 0)
) {
```

**After:**
```kotlin
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp
) {
```

## Verification
- Open Shop → SlotNavigationBar buttons sit above the system navigation bar
- No gap between buttons and system nav bar (just the NavigationBar's built-in padding)
- All three tabs (Weapon, Armor, Accessory) visible and tappable
- Content (item list) scrolls properly, not overlapped by system nav bar
- On gesture navigation: buttons sit just above the gesture hint area
- On 3-button navigation: buttons sit just above the nav bar buttons

## Dependencies
- Relies on parent bottom padding being removed (Plan 84 change #1) — verify this is in place in MainActivity.kt
