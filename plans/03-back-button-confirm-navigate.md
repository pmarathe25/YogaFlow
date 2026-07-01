# Plan: Back Button Confirmation Dialog & Navigation Fixes

## Issues Addressed
- **Issue 3**: Pressing back from the battle screen should prompt with a confirmation dialog instead of directly exiting an in-progress battle.
- **Issue 7**: Pressing "back" from the Shop/Party screen launches the "Path Of Zen" page instead of going to the "Your Journey" dashboard.

---

## Issue 3: Battle Exit Confirmation

### Current behavior
In `GameApp.kt`, `BackHandler` calls `viewModel.navigateBack()` which unconditionally sets screen to `HUB`:

```kotlin
BackHandler(enabled = currentScreen != GameScreen.HUB) {
    viewModel.navigateBack()
}
```

```kotlin
fun navigateBack() {
    _currentScreen.value = GameScreen.HUB
}
```

### Changes Required

#### 1. `GameViewModel.kt`
- Add a `showExitBattleDialog` StateFlow (or similar) that the BattleScreen can observe.
- Modify the back handler logic to check if the current screen is `BATTLE` and battle is in progress (`phase` is `PLAYER_TURN` or `ENEMY_TURN`). If so, set the dialog flag instead of navigating away.
- If battle is in a terminal phase (VICTORY, DEFEAT), allow immediate exit.

#### 2. `GameApp.kt` or `BattleScreen.kt`
- Show an `AlertDialog` when the exit confirmation flag is set:
  - Title: "Exit Battle?"
  - Message: "Are you sure you want to forfeit the current battle?"
  - Confirm button: "Forfeit" — navigates to HUB (and maybe marks as defeat)
  - Cancel button: "Continue Fighting" — dismisses dialog

#### 3. BattleScreen.kt
- When the user confirms exit, optionally trigger a defeat/forfeit flow before navigating to HUB.

### Implementation approach

**Decision: Handle entirely in `BattleScreen.kt`** using a local `showExitDialog` state. This avoids polluting the ViewModel with UI-only dialog state. No change needed to `GameApp.kt`'s `BackHandler` — the inner `BackHandler` in `BattleScreen` takes priority (innermost handler wins in Compose) and prevents propagation to the outer one.

```kotlin
// In BattleScreen.kt
var showExitDialog by remember { mutableStateOf(false) }

BackHandler(enabled = state.phase == PLAYER_TURN || state.phase == ENEMY_TURN) {
    showExitDialog = true
}

if (showExitDialog) {
    AlertDialog(
        onDismissRequest = { showExitDialog = false },
        title = { Text("Exit Battle?") },
        text = { Text("Are you sure you want to forfeit the current battle?") },
        confirmButton = { TextButton(onClick = { viewModel.navigateBack(); showExitDialog = false }) { Text("Forfeit") } },
        dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Continue Fighting") } }
    )
}
```

For terminal phases (VICTORY, DEFEAT), the `BackHandler` is disabled by the `enabled` check — the outer handler in `GameApp.kt` will fire and navigate to HUB normally.

---

## Issue 7: Back from Shop/Party Navigation

### Current behavior
`navigateBack()` always sets screen to `HUB`. `HubScreen` contains `MonsterRoadSelection` (the "Path of Zen" page). So pressing back from Shop or Party goes directly to the Path of Zen.

### Desired behavior
Pressing back from Shop/Party should go to a "Your Journey" dashboard view instead of directly to the Path of Zen.

### Changes Required

#### 1. `HubScreen.kt` — Add dashboard state
Add a sub-navigation state within HubScreen to distinguish between "Your Journey" (dashboard) and "Path of Zen" (monster road):

```kotlin
enum class HubView { DASHBOARD, PATH_OF_ZEN }

@Composable
fun HubScreen(...) {
    var currentHubView by remember { mutableStateOf(HubView.DASHBOARD) }
    
    when (currentHubView) {
        HubView.DASHBOARD -> YourJourneyDashboard(
            onNavigateToPath = { currentHubView = HubView.PATH_OF_ZEN },
            // other nav actions
        )
        HubView.PATH_OF_ZEN -> MonsterRoadSelection(
            defeatedMonsterIds = ...,
            onSelectMonster = { ... },
            onDismiss = { currentHubView = HubView.DASHBOARD }
        )
    }
}
```

#### 2. Create "Your Journey" Dashboard composable
Create a new file `DashboardScreen.kt` under `ui/components/` with the dashboard composable:

- Welcome / player info (Yoga Level, Sparks, Gold)
- Navigation buttons:
  - "Path of Zen" → goes to MonsterRoadSelection
  - "Heroes" → navigates to PARTY screen
  - "Shop" → navigates to SHOP screen
  - "Trophies" → navigates to TROPHIES screen

#### 3. `GameViewModel.kt`
- `navigateBack()` should still set screen to `HUB`. The Hub itself will decide whether to show dashboard or path based on its own internal state.
- `startBattle()` should navigate to the Path of Zen first (set `currentHubView = PATH_OF_ZEN`) or directly to BATTLE (current behavior is fine).

#### 4. `MonsterRoadSelection.kt`
The `onDismiss` callback should navigate back to the dashboard (change `currentHubView` to `DASHBOARD`), not exit the Hub entirely.
