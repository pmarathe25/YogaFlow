# Plan 20: Fix Back Button Colors in Dark Mode

## Problem

The `ArrowBack` icons in `ShopScreen` and `PartyScreen` use the default icon tint, which is `LocalContentColor.current`. In dark mode, if the content color resolves to black (or a dark shade), the back button becomes invisible against the dark background.

Other screens (like `HubScreen` and `MonsterRoadSelection`) don't have this problem because they either:
- Use a dark background with light-colored icons explicitly
- Use text-based back buttons instead of icons

The JourneyScreen also has `onNavigateToBattle` actions that navigate to the battle hub, but those are buttons, not back arrows.

## Fix

Add the same explicit `tint = MaterialTheme.colorScheme.onBackground` that `JourneyScreen` and `PlayerScreens` use — this is the consistent pattern across screens that work in both themes.

### ShopScreen.kt line 46
```kotlin
// Before:
Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
// After:
Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
     tint = MaterialTheme.colorScheme.onBackground)
```

### PartyScreen.kt line 50
```kotlin
// Before:
Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
// After:
Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
     tint = MaterialTheme.colorScheme.onBackground)
```

`onBackground` always contrasts against `background` in both themes — white in dark mode, dark in light mode. This is the same approach `JourneyScreen.kt:79`, `PlayerScreens.kt:75`, `FlowDetailsScreen.kt`, and `HistoryScreen.kt` already use.

`MonsterRoadSelection.kt:173` uses `tint = Color.White` — that's correct for its dark `#0D1B2A` background and doesn't need changing.

## Files to modify

| File | Change |
|---|---|
| `ShopScreen.kt:45-47` | Add `tint = MaterialTheme.colorScheme.onBackground` |
| `PartyScreen.kt:49-51` | Add `tint = MaterialTheme.colorScheme.onBackground` |
