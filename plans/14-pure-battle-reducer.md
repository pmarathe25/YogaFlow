# Plan 14: Consolidate BattleEngine into BattleReducer

## Summary
Merge all stateless calculation functions from `BattleEngine.kt` directly into `BattleReducer`. The reducer becomes the single pure function that takes `(BattleState, BattleCommand) -> TurnResult`. Remove `BattleEngine.kt` entirely.

## Changes

### 1. Move all `BattleEngine` functions into `BattleReducer`
- Inline `BattleEngine.computeDamage()` as a private function inside `BattleReducer`.
- Inline `BattleEngine.computeHeal()`, `computeShield()`.
- Inline `BattleEngine.computeSkillOutcome()`, `computeComboOutcome()`, `computeMonsterOutcome()`.
- Inline `BattleEngine.applyOutcome()`.
- Inline `BattleEngine.calculateTurnOrder()`.
- Inline `BattleEngine.resolveTargets()`.
- Inline `BattleEngine.checkPhaseTriggers()`.
- Inline `BattleEngine.chooseMonsterTarget()`.
- Inline `BattleEngine.computeBuffMultiplier()`.
- Inline `BattleEngine.isComponentNullified()`.
- Inline `elementChart` and `getElementMultiplier()`.
- Remove the `BattleEngine` object import from `BattleReducer`.

### 2. Update `BattleReducer` to use `CombatantState` fields
- Wherever `HeroInstance`/`MonsterInstance` methods were called (e.g., `hero.atk`, `monster.specialAttack`), use the equivalent `CombatantState` fields.
- The `applyOutcome` function already outputs events — ensure all paths produce `BattleEvent` values.
- Ensure deterministic RNG is passed through where needed.

### 3. Remove `BattleEngine.kt`
- Delete the file entirely.
- Move `DamageResult` and `SkillOutcomeResult` data classes into `BattleReducer.kt` or remove if not needed externally.

### 4. Update `TurnManager.kt`
- No changes needed — `TurnManager` already delegates to `BattleReducer`.
- Verify all delegation methods still match.

### 5. Update `GameViewModel.kt`
- Remove any direct `BattleEngine` calls (there should be none left — `BattleEngine.resolveTargets()` is used in `executeSkill()` for auto-targeting).
- Move auto-targeting logic into `BattleReducer.resolveTargets()` and call it through `TurnManager`.

### 6. Update test imports
- `BattleEngineTest.kt` will be rewritten in Plan 18, but for now remove imports from deleted file.

## Verification
- `./gradlew assembleDebug` must compile.
- Run `./gradlew testDebugUnitTest` — existing tests that call `BattleEngine` directly will fail (expected; they will be fixed in Plan 18).
