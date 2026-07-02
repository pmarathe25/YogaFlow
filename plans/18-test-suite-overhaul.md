# Plan 18: Test Suite Overhaul

## Summary
Rewrite all minigame tests to match the new immutable `CombatantState` / `GameProgress` models and the consolidated `BattleReducer`. Remove tests for deleted types. Add reducer unit tests covering the full battle rules spec.

## Test Changes

### 1. `BattleEngineTest.kt` → `BattleReducerTest.kt`
Replace the 548-line `BattleEngineTest` with focused `BattleReducer` tests:
- **Queue creation**: `startBattle` produces correct turn order from `CombatantState` list.
- **Empty queue**: safe handling when all combatants are defeated at start.
- **Turn advancement**: phase transitions, round wrapping, dead-actor skip.
- **Target resolution**: single enemy, single ally, all allies, all enemies, self.
- **Damage/H kill**: HP reduction, shield absorption, overkill, death events.
- **Healing**: HP restore clamped to max, heal from percentage/flat scaling.
- **Shield**: shield applied, shield absorbs damage, shield break sparks.
- **Death/revive**: hero down event, revive restores HP, monster down event.
- **Cooldowns**: cooldown set on skill use, ticked on turn start.
- **Status timing**: status applied, ticks on turn start, expires correctly.
- **Ultimate gauge**: gain from skills/spend on ultimate/gain from combo participation.
- **Combo availability**: available only when all required heroes alive.
- **Combo targeting**: applies damage/heal/shield to correct targets.
- **Defend**: shield + gauge gain.
- **Monster extra actions**: bounded, no recursion, phase triggers queue additional actions.
- **Victory/defeat**: checked after every action and before turn advancement.
- **Phase triggers**: shield gain, extra action, double action at HP thresholds.

### 2. `TurnManagerTest.kt` — Update for CombatantState
- Change all `makeHero()`/`makeMonster()` helpers to produce `CombatantState` instead of `HeroInstance`/`MonsterInstance`.
- Update test expectations for immutable state (no mutation of hero/monster after construction).
- Keep existing test coverage for `startBattle`, `advanceTurn`, `executeSkill`, `executeUltimate`, `executeMonsterTurn`, `executeCombo` — but now routing through `TurnManager` → `BattleReducer` with `CombatantState`.

### 3. `GameSaveManagerTest.kt` — Update for GameProgress
- Change all `GameSaveData` references to `GameProgress`.
- Update test data builders to use `CombatantState`-like data (simplified party format inline with `GameProgress.party`).
- Add round-trip test for versioned JSON blob (save → load → verify fields).
- Add test for default save loading from `assets/game/default_save.json`.
- Add migration test for legacy prefs format (if kept).

### 4. Remove obsolete tests
- Remove any tests that directly test `BattleEngine` static methods (they are now private within `BattleReducer`).

### 5. Add new test file: `GameProgressSerializationTest.kt`
- Test JSON serialization/deserialization of `GameProgress` to/from Gson.
- Test version field is respected.
- Test default values on missing fields.

## Running Tests
```bash
./gradlew testDebugUnitTest
```
All tests should pass. After implementation, verify manual battle and map on at least one phone-sized and one tablet/desktop preview viewport.

## Verification
- `./gradlew testDebugUnitTest` passes cleanly.
- All reducer unit tests (queue, targets, damage, healing, shields, death, revive, cooldowns, statuses, combos, ultimates, defend, monster actions, victory/defeat) are present and passing.
- Manual verification on phone and tablet viewports.
