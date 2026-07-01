# Plan: Bug Fixes — Navigation & Layout

**Dependencies:** None — fully independent.
**Files touched:**
- `/home/pranav/Code/YogaFlow/app/src/main/java/com/example/MainActivity.kt` (line 121)
- `/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/ui/components/MonsterRoadSelection.kt` (lines 90-109)
- `/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/ui/components/HubScreen.kt` (all)
- `/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/ui/components/GameApp.kt` (all)
- `/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/ui/components/BattleScreen.kt` (lines 397-398 and 229-426)

---

## 1.1 Hide bottom nav bar during battle

**File:** `MainActivity.kt` — find `showBottomBar` (around line 121)

**Change:** Add `currentRoute != Screen.ZenBattle.route` to the `showBottomBar` condition.

**Before (line ~121):**
```kotlin
val showBottomBar = !isInPlayerFlow && currentRoute != Screen.SessionComplete.route
```

**After:**
```kotlin
val showBottomBar = !isInPlayerFlow
    && currentRoute != Screen.SessionComplete.route
    && currentRoute != Screen.ZenBattle.route
```

**Import check:** `Screen` is already imported from `com.example.navigation.Screen`.

**Verification:** Navigate to ZenBattle → bottom bar (Dashboard/Journey/History) should not appear.

---

## 1.2 MonsterRoadSelection: move back button from RIGHT to LEFT

**File:** `MonsterRoadSelection.kt` (path: `/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/ui/components/MonsterRoadSelection.kt`)

**Before (lines ~90-109):**
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        "The Path of Zen",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        color = Color(0xFF1B5E20)
    )
    IconButton(
        onClick = onDismiss,
        modifier = Modifier.background(Color.White.copy(alpha = 0.8f), CircleShape)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}
```

**After:**
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    IconButton(
        onClick = onDismiss,
        modifier = Modifier.background(Color.White.copy(alpha = 0.8f), CircleShape)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text(
        "The Path of Zen",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        color = Color(0xFF1B5E20)
    )
    Spacer(modifier = Modifier.weight(1f))
}
```

This matches the pattern used by FlowDetailsScreen, HistoryScreen, JourneyScreen, ShopScreen, and PartyScreen — all place the ArrowBack as the FIRST child in the top Row.

---

## 1.3 MonsterRoadSelection back button navigates out of GameApp

**Current behavior:** `onDismiss = { viewModel.navigateBack() }` sets screen to `GameScreen.HUB`. But we're already on HUB — so the button does nothing visible.

**Desired behavior:** Pressing the back arrow on the hub exits GameApp entirely (pops the NavHost back stack, returning to the Journey/ExpandedDashboard screen).

### Step 1: Add `onExitHub` parameter to HubScreen

**File:** `HubScreen.kt` (`/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/ui/components/HubScreen.kt`)

**Current content (all 48 lines):**
```kotlin
package com.example.game.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.game.viewmodel.GameViewModel
import com.example.game.viewmodel.GameScreen

@Composable
fun HubScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val saveData by viewModel.saveData.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    MonsterRoadSelection(
        defeatedMonsterIds = saveData.defeatedMonsterIds,
        onSelectMonster = { monsterId -> viewModel.startBattle(monsterId) },
        onDismiss = { viewModel.navigateBack() }
    )
    
    // Error snackbar
    errorMessage?.let { msg ->
        androidx.compose.material3.Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                androidx.compose.material3.TextButton(onClick = { viewModel.clearError() }) {
                    androidx.compose.material3.Text("Dismiss")
                }
            }
        ) { androidx.compose.material3.Text(msg) }
    }
}
```

**After:**
```kotlin
package com.example.game.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.game.viewmodel.GameViewModel
import com.example.game.viewmodel.GameScreen

@Composable
fun HubScreen(
    viewModel: GameViewModel,
    onExitHub: () -> Unit,       // NEW — callback to pop GameApp from NavHost
    modifier: Modifier = Modifier
) {
    val saveData by viewModel.saveData.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    MonsterRoadSelection(
        defeatedMonsterIds = saveData.defeatedMonsterIds,
        onSelectMonster = { monsterId -> viewModel.startBattle(monsterId) },
        onDismiss = onExitHub    // CHANGED: was viewModel.navigateBack()
    )
    
    // Error snackbar
    errorMessage?.let { msg ->
        androidx.compose.material3.Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                androidx.compose.material3.TextButton(onClick = { viewModel.clearError() }) {
                    androidx.compose.material3.Text("Dismiss")
                }
            }
        ) { androidx.compose.material3.Text(msg) }
    }
}
```

### Step 2: Add `onExitHub` parameter to GameApp

**File:** `GameApp.kt` (`/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/ui/components/GameApp.kt`)

**Current content:**
```kotlin
package com.example.game.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.game.viewmodel.GameViewModel
import com.example.game.viewmodel.GameScreen

@Composable
fun GameApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    BackHandler(enabled = currentScreen != GameScreen.HUB) {
        viewModel.navigateBack()
    }

    AnimatedContent(
        targetState = currentScreen,
        modifier = modifier
    ) { screen ->
        when (screen) {
            GameScreen.HUB -> HubScreen(viewModel = viewModel)
            GameScreen.BATTLE -> BattleScreen(viewModel = viewModel)
            GameScreen.PARTY -> PartyScreen(viewModel = viewModel)
            GameScreen.EQUIPMENT -> PartyScreen(viewModel = viewModel)
            GameScreen.TROPHIES -> TrophyScreen(viewModel = viewModel)
            GameScreen.SHOP -> ShopScreen(viewModel = viewModel)
            GameScreen.BATTLE_RESULT -> BattleResultScreen(viewModel = viewModel)
            GameScreen.SETTINGS -> HubScreen(viewModel = viewModel)
        }
    }
}
```

**After:**
```kotlin
package com.example.game.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.game.viewmodel.GameViewModel
import com.example.game.viewmodel.GameScreen

@Composable
fun GameApp(
    viewModel: GameViewModel,
    onExitHub: () -> Unit,       // NEW
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    BackHandler(enabled = currentScreen != GameScreen.HUB) {
        viewModel.navigateBack()
    }

    AnimatedContent(
        targetState = currentScreen,
        modifier = modifier
    ) { screen ->
        when (screen) {
            GameScreen.HUB -> HubScreen(
                viewModel = viewModel,
                onExitHub = onExitHub     // NEW
            )
            GameScreen.BATTLE -> BattleScreen(viewModel = viewModel)
            GameScreen.PARTY -> PartyScreen(viewModel = viewModel)
            GameScreen.EQUIPMENT -> PartyScreen(viewModel = viewModel)
            GameScreen.TROPHIES -> TrophyScreen(viewModel = viewModel)
            GameScreen.SHOP -> ShopScreen(viewModel = viewModel)
            GameScreen.BATTLE_RESULT -> BattleResultScreen(viewModel = viewModel)
            GameScreen.SETTINGS -> HubScreen(
                viewModel = viewModel,
                onExitHub = onExitHub     // NEW
            )
        }
    }
}
```

### Step 3: Pass `onExitHub` from MainActivity

**File:** `MainActivity.kt` — find the ZenBattle composable destination (around line 321):
```kotlin
composable(Screen.ZenBattle.route) {
    BackHandler { navController.popBackStack() }
    GameApp(viewModel = gameViewModel)
}
```

**After:**
```kotlin
composable(Screen.ZenBattle.route) {
    BackHandler { navController.popBackStack() }
    GameApp(
        viewModel = gameViewModel,
        onExitHub = { navController.popBackStack() }
    )
}
```

**How it works:**
- On HUB: internal `BackHandler` is disabled (`currentScreen != GameScreen.HUB` is false), so the `BackHandler` in MainActivity's composable fires → `navController.popBackStack()` exits GameApp
- On other game screens: internal `BackHandler` fires → `viewModel.navigateBack()` returns to HUB

The back arrow button on the HUB now also calls `onExitHub` which pops the NavHost stack.

---

## 1.4 Action panel overlay hides heroes

**File:** `BattleScreen.kt` (`/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/ui/components/BattleScreen.kt`)

**Symptom:** The `ActionPanel` has `height(400.dp)` (line 103 of BattleActions.kt), but the `Spacer` at bottom is only `100.dp` (line 398 of BattleScreen.kt). So 300dp of the action panel overlaps with the hero area.

**Fix:** Remove the `Spacer(height = 100.dp)` and instead add `.padding(bottom = 400.dp)` to the inner `Column` that holds monsters + heroes.

### Exact changes in BattleScreen.kt

**A) Line 229-230 — add padding to the Column:**
Before: `Column(modifier = Modifier.fillMaxSize())`
After: `Column(modifier = Modifier.fillMaxSize().padding(bottom = 400.dp))`

**B) Lines 397-398 — remove the Spacer:**
Find and delete:
```kotlin
                // Bottom Spacer to make room for ActionPanel overlay
                Spacer(modifier = Modifier.height(100.dp))
```

### Complete resulting layout structure (after both changes):
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 400.dp)) {  // CHANGED
            // Top Bar: Turn Order & Log (unchanged)
            // Monster Area — weight(0.55f) (unchanged)
            // Hero Area — weight(0.45f) (unchanged)
            // NO MORE Spacer(100.dp)                                       // DELETED
        }

        // Action Panel Overlay (unchanged)
        val currentHero = state.aliveHeroes.find { it.heroId == state.currentActorId }
        if (currentHero != null && state.phase == BattlePhase.PLAYER_TURN) {
            key(state.currentActorId) {
                ActionPanel(
                    // ... unchanged
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                )
            }
        }
    }
    // ... rest of file unchanged (effects, turn banner, black overlay, log modal)
}
```

**Alternative (better for responsive layouts):** If you want the padding to auto-match the ActionPanel height, define a constant:
```kotlin
private val ACTION_PANEL_HEIGHT = 400.dp
```
and use it both here and in `BattleActions.kt`. But the inlined 400.dp is fine for now since both files use the same constant value.

**Verification:** Heroes should be fully visible with no occlusion from the action panel.
