# Plan: Fix Gemini-Introduced Bugs from Refactoring

## Background

Gemini refactored the codebase to eliminate dead code, consolidate ViewModels into Managers, and remove duplicated NavHost. While the structural changes are good, several critical bugs were introduced in the battle system.

## Bugs Identified

### Bug 1: Double monster turns from dead-actor skip

**File:** `GameViewModel.kt:393-404`

**Root cause:** A new dead-actor skip was added in `advanceTurn()`:
```kotlin
if (!actorInstanceAlive) {
    advanceTurn(state)  // recursive call
    return@launch
}
```

When the next scheduled actor is dead, this calls `advanceTurn` recursively, which calls `state.currentTurnIndex = (state.currentTurnIndex + 1) % size`. This lands on the next actor in the order — which could be a monster that already acted this round, giving it a **double turn**.

**Example:** Turn order [HeroA, Monster]. Monster kills HeroA during its turn. When HeroA's turn comes next, the dead-actor skip advances to the Monster again, giving it an extra turn.

**Fix:** Remove the dead-actor skip entirely. The original code didn't have it — dead heroes' turns come up but the UI naturally handles it (the ActionPanel shows nothing since `currentActorId` has no alive hero). When all heroes are dead, the defeat check at lines 382-391 catches it.

```kotlin
// DELETE lines 393-404 entirely
```

### Bug 2: `currentTurnIndex = 0` on round wrap breaks combo skip

**File:** `GameViewModel.kt:368`

**Root cause:** On round wrap, `currentTurnIndex` is unconditionally set to 0:
```kotlin
if (wrapped) {
    state.advanceRound()
    state.monsters.forEach { it.extraActionsThisRound = 0 }
    val newOrder = BattleEngine.calculateTurnOrder(state.heroes, state.monsters)
    state.turnOrder.clear()
    state.turnOrder.addAll(newOrder)
    state.currentTurnIndex = 0  // BUG
}
```

This is correct for skipCount=1, but wrong for skipCount > 1 (combo skills). E.g., turn order [H1, M, H2, H3] at index 3 with skipCount=2: `(3+2)%4 = 1`, wrapped. New code sets index=0, skipping H2 who should go next.

**Fix:**
```kotlin
if (wrapped) {
    state.advanceRound()
    state.monsters.forEach { it.extraActionsThisRound = 0 }
    val newOrder = BattleEngine.calculateTurnOrder(state.heroes, state.monsters)
    state.turnOrder.clear()
    state.turnOrder.addAll(newOrder)
    // Keep currentTurnIndex — the % calculation already computed the correct position
}
```

### Bug 3: Missing post-phase trigger checks after monster action

**File:** `GameViewModel.kt:477-482`

**Root cause:** The original `executeMonsterTurn` checked `BattleEngine.checkPhaseTriggers()` after the monster's action AND after extra actions. The refactored code only checks preEvents (line 440) and removed the post-action check:

```kotlin
// REMOVED from refactored code:
val postEvents = BattleEngine.checkPhaseTriggers(state, monster)
postEvents.filterIsInstance<BattleEvent.PhaseTriggered>().forEach { event ->
    addBattleLog("${monster.name} triggers: ${event.trigger.type.name}!")
}
```

**Fix:** Add the post-action phase trigger check after `applyOutcome` and before the delay:

```kotlin
applyOutcome(state, outcome)
emitBattleState(state)

val postEvents = BattleEngine.checkPhaseTriggers(state, monster)
postEvents.filterIsInstance<BattleEvent.PhaseTriggered>().forEach {
    addBattleLog("${monster.name} triggers: ${it.trigger.type.name}!")
}

delay(1500)
```

### Bug 4: `BattleEngine.advanceTurn()` is dead code

**File:** `BattleEngine.kt:36-92`

**Root cause:** Gemini created a second `advanceTurn` method in `BattleEngine` but `GameViewModel.advanceTurn()` (lines 353-430) is the one actually called from all battle flow paths. The `BattleEngine.advanceTurn()` is never invoked.

**Fix:** Remove the `BattleEngine.advanceTurn()` method entirely (56 lines of dead code).

### Bug 5: `BattleScreen` flash `LaunchedEffect` missing `lastEventCount` guard

**File:** `BattleScreen.kt` `LaunchedEffect(state.eventLog.size)`

**Root cause:** The original code had:
```kotlin
val lastEventCount = remember { mutableIntStateOf(0) }
LaunchedEffect(eventLog.size) {
    if (eventLog.size <= lastEventCount.intValue) return@LaunchedEffect
    lastEventCount.intValue = eventLog.size
```

The refactored code removed the `lastEventCount` guard. While `LaunchedEffect` with a key should only re-run when the key changes, stale recompositions could replay flash effects. The guard was defensive for a reason.

**Fix:** Restore the `lastEventCount` guard.

### Bug 6: `BattleScreen` missing `MonsterDown`/`HeroDown` flash handling

**File:** `BattleScreen.kt` `LaunchedEffect(state.eventLog.size)`

**Root cause:** The refactored code removed the `MonsterDown` and `HeroDown` event handlers that were in the original's flash effect handler:

```kotlin
// REMOVED:
is BattleEvent.MonsterDown -> {
    monsterAnimState.value = SpriteAnimState(state = SpriteState.DYING, stateTime = 0f, alpha = 0f, offsetY = 30f)
}
is BattleEvent.HeroDown -> {
    heroAnimStates[event.heroId] = SpriteAnimState(state = SpriteState.DYING, stateTime = 0f, alpha = 0f, offsetY = 30f)
}
```

These may be handled by `rememberSpriteAnimations()` in `BattleAnimations.kt`, but need verification. If not handled there, restore these handlers.

### Bug 7: `GameViewModel.chooseMonsterTarget()` is dead code

**File:** `GameViewModel.kt:486-488`

**Root cause:** The refactored `executeMonsterTurn` calls `BattleEngine.chooseMonsterTarget()` directly (line 456), leaving the wrapper method `GameViewModel.chooseMonsterTarget()` defined but never called.

**Fix:** Remove the unused wrapper method.

---

## Summary of Changes

| # | File | Change |
|---|------|--------|
| 1 | `GameViewModel.kt:393-404` | Remove dead-actor skip (causes double monster turns) |
| 2 | `GameViewModel.kt:368` | Remove `currentTurnIndex = 0` on wrap |
| 3 | `GameViewModel.kt:~474` | Restore post-phase trigger checks after monster action |
| 4 | `BattleEngine.kt:36-92` | Remove dead `BattleEngine.advanceTurn()` |
| 5 | `BattleScreen.kt` | Restore `lastEventCount` guard in flash `LaunchedEffect` |
| 6 | `BattleScreen.kt` | Restore `MonsterDown`/`HeroDown` flash/sprite transitions |
| 7 | `GameViewModel.kt:486-488` | Remove dead `chooseMonsterTarget` wrapper |

## Verified Correct (no changes needed)

- **`BattleEngine.applyOutcome` new combo handling** (buffs, revive, statusEffects via `combo?` fields) — correctly handles combo effects that were previously duplicated in `GameViewModel.computeComboOutcome`.
- **`SpriteState`/`SpriteAnimState` move** from `BattleAnimations.kt` to `BattleCanvas.kt` — correct, no functional change.
- **`MainActivity.kt` → `YogaNavHost.kt` extraction** — correct, eliminates inline NavHost duplication.
- **`YogaViewModel` sub-ViewModel → Manager consolidation** — correct, fixes lifecycle leak.
- **`DataLoader.getHeroForLevel()` removal** — confirmed dead code.
- **`BattleState.currentCombo`/`pendingAction` removal** — confirmed dead code.
