# Plan 82: Fix Crash with Hero-Targeting Skills

## Problem
The app crashes when using skills that can target heroes (SINGLE_ALLY, ALL_ALLIES, SELF). The crash likely occurs during the transition from skill execution to the next turn, or due to stale state references after the battle state updates.

## Investigation
Tracing the code path for a SINGLE_ALLY skill:
1. User selects skill → `executeSkill(heroId, skill)` sets `pendingSkill`
2. UI enters targeting mode → user taps a hero
3. `executeSkill(heroId, skill, listOf(targetHeroId))` is called with `customTargets`
4. Since `customTargets != null`, the auto-target block is skipped
5. Launches coroutine, calls `turnManager.executeSkill(...)`, delays 1300ms, calls `advanceToNextTurn()`

Potential crash points:
- State update race: `_battleState.value` changes during the launched coroutine. Captured `state` on line 240 is stale when used on line 271: `turnManager.executeSkill(_battleState.value ?: return@launch, ...)`. This re-reads `_battleState.value`, which should be fine if it wasn't modified between the initial read and the coroutine launch.
- `currentHero.id` captured in lambda (BattleScene.kt:270, 349) — if the hero was defeated between composition and execution, `currentHero` is still non-null but stale. This is safe for the `executeSkill` call because the function re-validates via `state.heroes.find { it.id == heroId && !it.isDefeated }`.
- `advanceToNextTurn()` might reference invalid state if the turn order changes unexpectedly.
- In `ActionTray.kt`, the `onSkill` callback uses `currentHero` captured from composition scope. If `currentHero` changes between compositions, lambda captures stale reference.

The most likely cause is a **null pointer or index-out-of-bounds** in the turn manager when computing outcomes for ally targets. For instance, `chooseMonsterTarget` uses `!!` which crashes on empty monster lists.

## Fix
### BattleScene.kt — Guard targeting callbacks
Add null-safety checks in the `onSkill` callback to prevent stale `currentHero` from causing issues:

Before (line 457-459):
```kotlin
onSkill = { skill ->
    viewModel.executeSkill(currentHero.id, skill)
},
```

After:
```kotlin
onSkill = { skill ->
    val id = currentHero?.id ?: return@...
    viewModel.executeSkill(id, skill)
},
```

Also for hero targeting (line 349):
```kotlin
viewModel.executeSkill(currentHero?.id ?: return@clickable, skill, listOf(hero.id))
```

### GameViewModel.kt — Validate hero in coroutine
Re-read `_battleState.value` at the start of the coroutine and re-validate:

Before (lines 267-278):
```kotlin
viewModelScope.launch {
    _isProcessingTurn.value = true
    _battleState.value = state.copy(pendingSkill = null)
    val result = turnManager.executeSkill(_battleState.value ?: return@launch, heroId, skill, targets)
```

After:
```kotlin
viewModelScope.launch {
    _isProcessingTurn.value = true
    val currentState = _battleState.value ?: run { _isProcessingTurn.value = false; return@launch }
    _battleState.value = currentState.copy(pendingSkill = null)
    val heroStillAlive = currentState.heroes.find { it.id == heroId && !it.isDefeated } ?: run {
        _isProcessingTurn.value = false; return@launch
    }
    val result = turnManager.executeSkill(currentState, heroId, skill, targets)
```

### BattleReducer.kt — Remove !! operators
Replace `!!` with safe calls + `continue` in `chooseMonsterTarget` and similar AI functions.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/BattleScene.kt`
- `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt`
- `app/src/main/java/com/example/game/battle/BattleReducer.kt`

## Specific Changes

### Change 1: BattleScene.kt line 457-459
Guard `onSkill` lambda.

### Change 2: BattleScene.kt line 346-351
Guard targeting clickable modifier (already has `?.` but add additional safety).

### Change 3: GameViewModel.kt lines 267-271
Re-read state inside coroutine and re-validate.

### Change 4: BattleReducer.kt `chooseMonsterTarget`
Replace `alive.maxByOrNull { it.hp }!!.id` with `alive.maxByOrNull { it.hp }?.id ?: return emptyList()`.

## Verification
- Use every hero's SELF skill → no crash
- Use a SINGLE_ALLY skill (e.g., Shanti "Pranayama Breath") → targets ally, no crash
- Use an ALL_ALLIES skill (e.g., Maitri "Gentle Breeze") → affects all allies, no crash
- Cycle through multiple turns with targeting → no crash
- Kill all monsters first, then try a SINGLE_ALLY skill → works correctly
- Kill all allies, then observe behavior → no crash (game should handle gracefully)

## Dependencies
- None
