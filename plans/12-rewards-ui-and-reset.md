# Plan: Flow Completion Rewards, Gold Fix, and Unified Reset

## Issues Addressed
- **Issue A**: SessionCompleteScreen shows hardcoded values ("12 Postures Held", "360s") instead of actual session rewards (XP, gold, sparks, level progress).
- **Issue B**: Gold is currently tied to battle "karma XP" rewards. But battles should not provide XP/gold/sparks — those come ONLY from yoga sessions. Gold should be derived from yoga session XP (e.g., 1 gold per 10 XP).
- **Issue C**: "Reset All Stats" only clears Room database yoga sessions — it does not reset game save data (gold, heroes, inventory, etc.).
- **Issue D**: No single source of truth for default game state. Resetting requires clearing multiple stores (Room DB, SharedPreferences). A single JSON file should define the default state so restoring = loading that file.

---

## Part 1: SessionCompleteScreen Rewards

### Current behavior

`SessionCompleteScreen` (PlayerScreens.kt:343-472) receives only `viewModel: YogaViewModel` and reads `flow.name`. It displays hardcoded "12 Postures Held" and "360s Practice Time". No XP, gold, sparks, or level info is shown.

The screen is rendered from `YogaNavHost.kt:88`:
```kotlin
isCompleted -> SessionCompleteScreen(
    viewModel = viewModel,
    onDone = { ... }
)
```

The `YogaViewModel` has access to `StatsManager` which tracks `totalXp`, `currentLevel`, `totalSparks`, `levelProgress`. The `SessionManager` tracks `_flow` and session duration.

### Changes Required

#### 1. `PlayerScreens.kt` — Rewrite `SessionCompleteScreen`

Add reward display after the flow name card. Needs access to StatsManager data:

```kotlin
@Composable
fun SessionCompleteScreen(
    viewModel: YogaViewModel,
    onDone: () -> Unit
) {
    val flow by viewModel.flow.collectAsState()
    val statsManager = viewModel.statsManager  // expose if not already public

    val totalXp by statsManager.totalXp.collectAsState()
    val currentLevel by statsManager.currentLevel.collectAsState()
    val levelName by statsManager.currentLevelName.collectAsState()
    val levelProgress by statsManager.levelProgress.collectAsState()
    val totalSparks by statsManager.totalSparks.collectAsState()

    // Calculate session XP from flow duration
    val sessionXp = remember(flow) {
        XpCalculator.calculateSessionXp(flow.totalDurationMinutes, flow.id)
    }

    // Gold is derived from total yoga XP: 1 gold per 10 XP
    val gold = totalXp / 10

    Column(...) {
        // ... existing check icon + "Namaste" + "Your practice is complete." ...

        // Flow name card (existing)
        GlassCard(...) { ... }

        // NEW: Rewards row
        Spacer(modifier = Modifier.height(16.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rewards", fontWeight = FontWeight.Bold, ...)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // XP earned this session
                    RewardItem(value = "+$sessionXp", label = "XP Earned", color = Color(0xFF7C4DFF))
                    // Total XP
                    RewardItem(value = "$totalXp", label = "Total XP", color = Color(0xFF448AFF))
                    // Level
                    RewardItem(value = "$currentLevel", label = levelName, color = Color(0xFFFFA000))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Sparks (unique practice days)
                    RewardItem(value = "$totalSparks", label = "Zen Sparks", color = Color(0xFF00BCD4))
                    // Gold (derived from total yoga XP)
                    RewardItem(value = "$gold", label = "Gold", color = Color(0xFFFFD600))
                }
            }
        }

        // Level progress bar
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = "Level ${currentLevel} — ${levelName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = levelProgress,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // ... existing "Return to Dashboard" button ...
    }
}
```

Create a small helper composable:
```kotlin
@Composable
private fun RewardItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
```

#### 2. `YogaViewModel.kt` — Expose StatsManager

`StatsManager` is currently a private field. Add a public accessor:
```kotlin
val statsManager: StatsManager get() = _statsManager
```

---

## Part 2: Gold Fix — Remove Battle Rewards, Gold from Yoga Only

### Current behavior

In `GameViewModel.onBattleWon()` (line ~560):
```kotlin
val karmaReward = 50 + (monster.difficultyTier.ordinal * 25)
data.copy(totalKarmaXp = data.totalKarmaXp + karmaReward)
```

Battles award "karma XP" which is then used as gold: `gold = totalKarmaXp - totalGoldSpent`. This is wrong — battles should give zero rewards.

### What needs to change

1. **Remove all reward logic from `onBattleWon()`** — battles award nothing. The function should still reset hero stats and save progress (defeated monster IDs, battle count), but not increment any currency.

2. **Gold should be derived from yoga session XP only.** Formula: `gold = totalYogaXp / 10` (1 gold per 10 XP). This is a simple derived value computed on-the-fly, not stored separately.

3. **Remove the `totalKarmaXp` field** from `GameSaveData` — it's no longer needed. Gold spending uses `totalGoldSpent` to track lifetime expenditure, and gold is computed from yoga XP minus spending.

### Changes Required

#### 1. `GameViewModel.kt` — `onBattleWon()`

Remove the karma reward:
```kotlin
private fun onBattleWon() {
    // No rewards from battles — XP/gold/sparks come only from yoga sessions
    val monster = _currentMonster.value ?: return
    val data = _saveData.value
    
    // Reset hero stats for next battle
    _party.value.forEach { hero ->
        hero.currentHp = hero.maxHp
        hero.shield = 0
        hero.ultimateGauge = 0
        hero.isDead = false
    }
    
    _saveData.value = data.copy(
        totalBattlesWon = data.totalBattlesWon + 1,
        defeatedMonsterIds = data.defeatedMonsterIds + monster.id,
        lastPlayedTimestamp = System.currentTimeMillis()
    )
    saveGame()
}
```

#### 2. `GameViewModel.kt` — `getAvailableGold()`

Gold is derived from yoga total XP (synced from main app). Add a `totalYogaXp` field to `GameSaveData` that is updated during `syncWithMainApp()`:

```kotlin
// In syncWithMainApp(), after computing xpSum:
updated = updated.copy(totalYogaXp = xpSum)
```

Then:
```kotlin
fun getAvailableGold(): Int {
    val data = _saveData.value
    return (data.totalYogaXp / 10) - data.totalGoldSpent
}
```

#### 3. `GameSaveManager.kt` — Add `totalYogaXp` field

```kotlin
data class GameSaveData(
    // ... existing fields ...
    val totalYogaXp: Int = 0,     // NEW: synced from yoga sessions
    val totalGoldSpent: Int = 0,
    // REMOVE: totalKarmaXp
    val defeatedMonsterIds: Set<String> = emptySet()
)
```

Remove `totalKarmaXp` and `KEY_TOTAL_KARMA_XP` entirely from `GameSaveManager`. Add `KEY_TOTAL_YOGA_XP`.

Update `toSaveMap()`, `loadGame()`, and `saveGame()` accordingly.

#### 4. Existing gold spenders — keep using `totalGoldSpent`

`levelUpHero()` and `purchaseItem()` already track spending via `totalGoldSpent`. Update their `availableGold` checks to use the new formula:
```kotlin
// Before
val availableGold = data.totalKarmaXp - data.totalGoldSpent
// After
val availableGold = (data.totalYogaXp / 10) - data.totalGoldSpent
```

#### 5. `BattleResultScreen.kt` — Remove reward display

The battle result screen currently shows "Karma XP +X" or similar rewards. Remove any reward text from victory/defeat screens. The battle result should only show stats (turns taken, damage dealt) and a "Return to Hub" button.

#### 6. `PartyScreen.kt` — Fix gold display

Update the gold display in PartyScreen to use the new formula. Currently (line ~162):
```kotlin
val gold = saveData.totalKarmaXp - saveData.totalGoldSpent
```
Change to:
```kotlin
val gold = (saveData.totalYogaXp / 10) - saveData.totalGoldSpent
```

---

## Part 3: Reset Stats — Include Game Save

### Current behavior

"Reset All Stats" in `SettingsScreen.kt:132` calls:
```kotlin
viewModel.clearAllCompletedSessions()
```

Which delegates to `StatsManager.clearAllCompletedSessions()`:
```kotlin
fun clearAllCompletedSessions() {
    scope.launch { repository.clearSessions() }
}
```

This only deletes Room database yoga sessions. Game save data (heroes, gold, inventory, conquered monsters) is untouched.

### Changes Required

#### 1. `SettingsScreen.kt` — Expand reset to include game save

```kotlin
viewModel.clearAllCompletedSessions()
viewModel.resetGameProgress()
showResetConfirmation = false
```

#### 2. `YogaViewModel.kt` — Add `resetGameProgress()`

YogaViewModel needs a reference to GameViewModel (or GameSaveManager) to reset game save:

```kotlin
fun resetGameProgress() {
    gameViewModel.resetAllProgress()
}
```

If GameViewModel is not directly accessible from YogaViewModel, pass it through SettingsScreen as a parameter (both are created in the navigation host / activity with `viewModel()`).

#### 3. `GameViewModel.kt` — `resetAllProgress()`

```kotlin
fun resetAllProgress() {
    _saveData.value = GameSaveData()  // full default
    _party.value = emptyList()
    _battleState.value = null
    saveGame()
    // Re-sync from main app — will set totalYogaXp based on sessions
    // (which were just cleared, so it will be 0)
    viewModelScope.launch { syncWithMainApp() }
}
```

#### 4. Update dialog text

In the reset confirmation dialog:
```kotlin
text = "Are you sure you want to permanently erase all completed sessions, XP, level history, battle progress, gold, and heroes? This action cannot be undone."
```

---

## Part 4: Single File Default State

### Current behavior

Default state is scattered:
- Room DB: empty by default (no sessions)
- SharedPreferences `game_save`: defaults are defined in `GameSaveData` constructor defaults (`totalYogaXp = 0`, `totalGoldSpent = 0`, etc.)
- No single file defines the "new game" state

### Changes Required

#### 1. Create `assets/game/default_save.json`

A JSON file containing the default `GameSaveData`:

```json
{
  "battleState": null,
  "party": [],
  "unlockedHeroIds": [],
  "sparks": 0,
  "yogaLevel": 1,
  "earnedTrophyIds": [],
  "totalBattlesWon": 0,
  "consumables": {},
  "inventory": [],
  "equippedSkins": {},
  "unlockedSkinIds": [],
  "totalPlayTimeMs": 0,
  "highestComboHits": 0,
  "fastestBattleTurns": 2147483647,
  "lastPlayedTimestamp": 0,
  "lastSyncedMainSparks": 0,
  "totalYogaXp": 0,
  "totalGoldSpent": 0,
  "defeatedMonsterIds": []
}
```

#### 2. `GameSaveManager.kt` — Add `resetToDefault()` from JSON asset

```kotlin
fun resetToDefault(context: Context) {
    try {
        val json = context.assets.open("game/default_save.json")
            .bufferedReader().use { it.readText() }
        val defaultData = gson.fromJson(json, GameSaveData::class.java)
        saveGame(defaultData)
    } catch (e: Exception) {
        clearSave()  // fallback to SharedPreferences clear
    }
}
```

#### 3. `GameViewModel.kt` — Use `resetToDefault()` in reset flow

```kotlin
fun resetAllProgress() {
    val app = getApplication<Application>()
    saveManager.resetToDefault(app)
    _party.value = emptyList()
    _battleState.value = null
    viewModelScope.launch { syncWithMainApp() }
}
```

#### 4. First-launch initialization (optional)

On first launch, load from `default_save.json` instead of constructor defaults:
```kotlin
fun loadGame(): GameSaveData {
    if (prefs.all.isEmpty()) {
        return loadDefaultSave()
    }
    // ... existing load logic ...
}
```

---

## Summary of All Changes

| File | Change |
|------|--------|
| `PlayerScreens.kt` | Rewrite `SessionCompleteScreen` to show XP, level, sparks, gold (from yoga XP / 10) |
| `YogaViewModel.kt` | Expose `StatsManager` for `SessionCompleteScreen`; add `resetGameProgress()` |
| `GameViewModel.kt` | Remove karma rewards from `onBattleWon()` — battles give nothing |
| `GameViewModel.kt` | `getAvailableGold()` uses `totalYogaXp / 10 - totalGoldSpent` |
| `GameViewModel.kt` | Add `resetAllProgress()` that reloads default JSON |
| `GameSaveManager.kt` | Replace `totalKarmaXp` with `totalYogaXp` in `GameSaveData` |
| `GameSaveManager.kt` | Add `resetToDefault(context)` reading from assets JSON |
| `GameSaveManager.kt` | Add `loadDefaultSave()` for first-launch initialization |
| `SettingsScreen.kt` | Call `resetGameProgress()` alongside `clearAllCompletedSessions()` |
| `SettingsScreen.kt` | Update dialog text to mention battle progress |
| `BattleResultScreen.kt` | Remove any reward display (battles give no rewards) |
| `PartyScreen.kt` | Update gold display formula |
| `assets/game/default_save.json` | **New file** — single source of truth for default game state |
