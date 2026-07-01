# Plan: Code Simplification and Quality Improvement

## Issues Addressed

- **God ViewModel**: `GameViewModel` (750 lines) mixes battle, party, shop, equipment, and economy logic.
- **Sub-ViewModel Leak**: `YogaViewModel` manually instantiates 4 sub-ViewModels (`StatsViewModel`, `SettingsViewModel`, `ReminderViewModel`, `SessionViewModel`) without DI or `ViewModelProvider` — their `onCleared()` is never called.
- **Missing `onCleared()`**: `YogaViewModel` has no cleanup.
- **Duplicate NavHost**: `YogaNavHost.kt` exists but is unused — the inline NavHost in `MainActivity.kt` (335 lines) is what actually renders. Two copies to maintain.
- **Dead Code**: `DataLoader.getHeroForLevel()` is never called. `BattleState.currentCombo` and `pendingAction` are never read (only defined and snapshotted). `pendingSkill` is actively used and must stay.
- **Large Component**: `BattleScreen` (546 lines) is difficult to maintain.

---

## Changes Required

### 1. `YogaViewModel.kt` — Fix sub-ViewModel lifecycle

**Current (lines 23-26):**
```kotlin
val statsViewModel = StatsViewModel(application)
val settingsViewModel = SettingsViewModel(application)
val reminderViewModel = ReminderViewModel(application)
val sessionViewModel = SessionViewModel(application)
```

These are instantiated as plain objects, not through AndroidX `ViewModelProvider`. They will never have their `onCleared()` called, causing resource leaks (especially `SessionViewModel` which manages wake locks, audio, and timers at lines 432-440).

#### Fix: Three options, in order of preference

**Option A (Recommended — full DI retrofit):** Use `ViewModelProvider` or a manual `ViewModelStore`:
```kotlin
private val statsViewModel: StatsViewModel by viewModels()
private val settingsViewModel: SettingsViewModel by viewModels()
// SessionViewModel needs special handling since it's shared
```

However, `YogaViewModel` extends `AndroidViewModel`, and `by viewModels()` works in `ComponentActivity`/`Fragment`, not within a ViewModel directly. A better approach:

**Option B (Delegate to manager classes):** Fold the thin sub-ViewModels into `YogaViewModel` directly (or into existing managers), avoiding sub-ViewModel instantiation entirely:

| Sub-ViewModel | Lines | Action |
|---------------|-------|--------|
| `StatsViewModel` (138 lines) | Fold into `StatsManager` (already exists in `db/`) or into `YogaViewModel` directly. Its logic is purely reactive (Room DB flows). |
| `SettingsViewModel` (70 lines) | Fold into existing `SettingsManager` or `YogaViewModel`. Thin wrapper. |
| `ReminderViewModel` (118 lines) | Fold into existing `ReminderManager` or `YogaViewModel`. Thin wrapper. |
| `SessionViewModel` (441 lines) | **Cannot fold** — manages active session lifecycle (wake lock, audio, countdown, zen sound). Extract into a dedicated `SessionController` manager class with explicit `cleanup()` method called from `YogaViewModel.onCleared()`. |

#### Add `onCleared()` to YogaViewModel:
```kotlin
override fun onCleared() {
    super.onCleared()
    sessionViewModel.cleanup()  // or sessionController.cleanup()
}
```

### 2. `GameViewModel.kt` — Extract PartyManager and ShopManager

Current: 750 lines mixing 5 concerns.

Extract into two new files under `game/manager/`:

#### `PartyManager.kt`
Move methods:
- `restoreParty()` (lines 131-142)
- `getUnlockedHeroes()` (lines 144-146)
- `getAvailableHeroes()` (lines 148-150)
- `levelUpHero()` (lines 697-720)
- `purchaseHero()` (lines 724-739)
- `getHeroLevelUpCost()` (lines 692-695)
- `getEquippedItems()` (lines 678-681)
- `equipItem()` (lines 649-666)
- `unequipItem()` (lines 668-676)

PartyManager takes `_party` and `_saveData` state flows as constructor params:
```kotlin
class PartyManager(
    private val party: MutableStateFlow<List<HeroInstance>>,
    private val saveData: MutableStateFlow<GameSaveData>,
    private val saveGame: () -> Unit
)
```

#### `ShopManager.kt`
Move methods:
- `purchaseItem()` (lines 633-647)
- `getAvailableGold()` (lines 685-688)

```kotlin
class ShopManager(
    private val saveData: MutableStateFlow<GameSaveData>,
    private val saveGame: () -> Unit
)
```

#### `GameViewModel.kt` after extraction
- Delegates to `PartyManager` and `ShopManager`
- Keeps battle-specific methods (~450 lines remaining): `startBattle`, `executeSkill`, `executeUltimate`, `executeCombo`, `advanceTurn`, `executeMonsterTurn`, `onBattleWon`, `syncWithMainApp`, etc.
- Creates managers in `init {}` block:
```kotlin
private val partyManager = PartyManager(_party, _saveData, ::saveGame)
private val shopManager = ShopManager(_saveData, ::saveGame)
```

### 3. `BattleState.kt` — Remove unused properties

Remove (lines 105-106):
```kotlin
var currentCombo: ComboSkill? = null,   // UNUSED — never read
var pendingAction: TurnAction? = null,   // UNUSED — never read
```

**Keep `pendingSkill`** (line 107) — it IS actively used in `GameViewModel.kt:211,231,241` and `BattleScreen.kt:66,263,342`.

Also remove from `snapshot()`:
```kotlin
currentCombo = currentCombo,
pendingAction = pendingAction,
```

### 4. `DataLoader.kt` — Remove dead code

Remove function (lines 44-46):
```kotlin
fun getHeroForLevel(level: Int): Hero =
    heroes.filter { level >= it.unlockYogaLevel }.maxByOrNull { it.unlockYogaLevel }
        ?: heroes.first()
```

Usage confirmed: defined but never called anywhere in the codebase.

### 5. `YogaNavHost.kt` — Resolve duplicate

**Current state:** Two copies of the NavHost exist:
- Inline in `MainActivity.kt` `YogaAppContent` composable (lines 203-332) — the one actually used
- `navigation/YogaNavHost.kt` — defined at line 21 but never imported or called

**Decision: Delete `YogaNavHost.kt`** (159 lines of dead code). It's a stale extraction that diverged from the inline version. If NavHost extraction is desired later, re-extract cleanly from the active copy.

### 6. `BattleScreen.kt` — Decompose large composable

Current: 546 lines. Extract:

| Extracted Component | Estimated Lines | Contents |
|-------------------|-----------------|----------|
| `BattleStartAnimation` | ~50 | Black fade → monster reveal → heroes slide in → "BATTLE BEGINS!" |
| `BattleTargetingOverlay` | ~60 | TargetCircle rendering, target selection state |
| `BattleFlashEffect` | ~40 | Flash overlay timing and color logic |
| `BattleStatusBar` | ~50 | Top bar: turn indicator + turn order list |

No structural changes — just break up the single composable into smaller named composables in the same file or a new `battle/` subdirectory.

---

## Summary of changes

| File | Change | Impact |
|------|--------|--------|
| `viewmodel/YogaViewModel.kt` | Fold 3 thin sub-VMs, extract SessionController, add `onCleared()` | Fixes memory leak |
| `viewmodel/SessionViewModel.kt` | Extract SessionController, keep UI state | Enables cleanup |
| `viewmodel/StatsViewModel.kt` | Fold into StatsManager or YogaViewModel | Delete file |
| `viewmodel/SettingsViewModel.kt` | Fold into SettingsManager or YogaViewModel | Delete file |
| `viewmodel/ReminderViewModel.kt` | Fold into ReminderManager or YogaViewModel | Delete file |
| `game/viewmodel/GameViewModel.kt` | Extract PartyManager + ShopManager | 750→450 lines |
| `game/manager/PartyManager.kt` | New file | 150 lines |
| `game/manager/ShopManager.kt` | New file | 40 lines |
| `game/model/BattleState.kt` | Remove `currentCombo`, `pendingAction` | 201→195 lines |
| `game/persistence/DataLoader.kt` | Remove `getHeroForLevel()` | 66→60 lines |
| `navigation/YogaNavHost.kt` | Delete file | 159 lines removed |
| `game/ui/components/BattleScreen.kt` | Extract 4 sub-composables | 546→~350 lines |

---

## ⚠️ Important corrections to Gemini analysis

- `pendingSkill` is actively used at `GameViewModel.kt:211,231,241` and `BattleScreen.kt:66,263,342`. **Do NOT remove.**
- `SessionViewModel` (441 lines) is **not** a thin wrapper — it manages wake locks, audio, countdown timers, and zen sound synthesis during active sessions. Folding it requires creating a proper `SessionController` with explicit lifecycle.
- `StatsManager` already exists in `db/StatsManager.kt`. The request to "create" it is stale.
- `SettingsManager` already exists in `db/SettingsManager.kt`.
- `ReminderManager` already exists in `db/ReminderManager.kt`.
- `VaginaViyogaHost` already exists in `navigation/YogaNavHost.kt` (the file exists but is dead code).
