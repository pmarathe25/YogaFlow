# Plan 27: Move Settings to Bottom Navigation Bar

## Problem

The Settings button is in the top-right corner of the Dashboard header. This is inconsistent — other top-level pages (Journey, History) are accessed via the bottom navigation bar. Settings should join them there.

## Changes

### 1. Add Settings item to bottom bar

**`YogaBottomBar.kt`** — add a 4th `NavigationBarItem` after History, using `Icons.Default.Settings`:

```kotlin
NavigationBarItem(
    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
    label = { Text("Settings") },
    selected = currentRoute == Screen.Settings.route,
    onClick = {
        navController.navigate(Screen.Settings.route) {
            popUpTo(Screen.Dashboard.route)
            launchSingleTop = true
        }
    },
    modifier = Modifier.testTag("bottom_nav_settings")
)
```

Place this after the History item. The navigation pattern matches the other items: `popUpTo(Dashboard)` and `launchSingleTop = true`.

### 2. Remove settings icon from dashboard header

**`DashboardScreen.kt`** — remove the settings `IconButton` and its `onOpenSettings` callback:

- Remove the `Icons.Default.Settings` import (no longer used in this file)
- Remove the `onOpenSettings: () -> Unit` parameter from `YogaDashboardScreen`
- Remove the `IconButton` block at lines 100-110

The header Row becomes just the app icon + "Yoga Flow" title:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    // App icon + title (no settings button)
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ).padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            "Yoga Flow",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
```

### 3. Update caller in NavHost

**`YogaNavHost.kt:39-46`** — remove the `onOpenSettings` callback from `YogaDashboardScreen`:

```diff
 YogaDashboardScreen(
     viewModel = viewModel,
     onViewFlowDetails = { flow ->
         navController.navigate(Screen.FlowDetails.createRoute(flow.id))
     },
-    onOpenSettings = {
-        navController.navigate(Screen.Settings.route)
-    }
 )
```

## Files to modify

| File | Changes |
|---|---|
| `YogaBottomBar.kt` | Add Settings `NavigationBarItem` after History |
| `DashboardScreen.kt` | Remove `onOpenSettings` parameter; remove `Icons.Default.Settings` import; remove settings `IconButton` from header |
| `YogaNavHost.kt` | Remove `onOpenSettings` callback from `YogaDashboardScreen` call |

## Dependencies

- None. Independent of other plans.
