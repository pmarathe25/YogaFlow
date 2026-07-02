# Plan: Fix Hero HP Persisting Across Battles

> **⚠️ Battle Refactor Impact (Plan 00):** This plan modifies `GameViewModel.startBattle()` to deep-copy heroes. After the refactor, `startBattle()` in the ViewModel will call `turnManager.startBattle(battleHeroes, monsters)`. The deep-copy still happens in the ViewModel *before* passing to TurnManager. The `onBattleWon()` and defeat handler changes are pure ViewMethod methods that survive the refactor untouched.

## Issue Addressed
- **Issue 13**: Hero HP persists across battles — if a hero loses HP in one battle, they start with that same amount in the next battle. Stats (HP, attack, shields, etc.) should only be valid for the duration of the battle.

---

## Root Cause

In `GameViewModel.kt`, the `startBattle()` method creates the BattleState by referencing party heroes directly:

```kotlin
fun startBattle(monsterId: String) {
    ...
    val state = BattleState(
        heroes = _party.value.toMutableList(),  // <-- shallow copy!
        monsters = mutableListOf(monsterInstance)
    )
    ...
}
```

`_party.value.toMutableList()` creates a **new list** but the **same `HeroInstance` objects**. Since `HeroInstance` is a data class with mutable `var` fields (`currentHp`, `shield`, `ultimateGauge`, `isDead`), any modifications made during the battle directly mutate the same objects in `_party.value`.

The battle "resets" in `onBattleWon()` and defeat handling (in `advanceTurn`):
```kotlin
_party.value.forEach { hero ->
    hero.currentHp = hero.maxHp
    hero.shield = 0
    hero.ultimateGauge = 0
    hero.isDead = false
}
```

But this reset is fragile. If a crash occurs, or if the battle ends without triggering the reset, or if `_party.value` is read before the reset (e.g., by another composable), the stale HP values will be visible.

Furthermore, the `emitBattleState` method uses `state.snapshot()` which creates copies for the `_battleState` flow, but this doesn't protect `_party.value` which holds the original references.

---

## Changes Required

### 1. `GameViewModel.kt` — `startBattle()`

Deep-copy heroes when creating the BattleState. This ensures battle mutations never affect the party:

```kotlin
fun startBattle(monsterId: String) {
    val monster = DataLoader.getMonster(monsterId)
    if (_party.value.isEmpty()) {
        _error.value = "No heroes in party!"
        return
    }
    _currentMonster.value = monster
    val monsterInstance = monster.createInstance()
    
    // Deep copy heroes so battle mutations don't affect party state
    val battleHeroes = _party.value.map { hero ->
        hero.copy(
            equippedItems = hero.equippedItems.toMutableList()
        )
    }.toMutableList()
    
    val state = BattleState(
        heroes = battleHeroes,
        monsters = mutableListOf(monsterInstance)
    )
    calculateTurnOrder(state)
    state.currentActorId = state.turnOrder.firstOrNull()?.id ?: ""
    _battleState.value = state
    _battleLog.value = emptyList()
    _currentScreen.value = GameScreen.BATTLE
    addBattleLog("Battle begins! ${monster.englishName} appears!")
    
    val firstActor = state.turnOrder.firstOrNull()
    if (firstActor != null) {
        if (firstActor.isHero) {
            state.phase = PLAYER_TURN
            updateComboAvailability(state)
        } else {
            state.phase = ENEMY_TURN
            executeMonsterTurn(state, firstActor)
        }
    }
    emitBattleState(state)
}
```

The key change: `_party.value.map { hero -> hero.copy(equippedItems = hero.equippedItems.toMutableList()) }` creates independent copies that won't be affected by battle mutations.

### 2. `GameViewModel.kt` — `onBattleWon()` and defeat handler

The reset in these handlers should still run to ensure `_party.value` heroes are restored to full health (even though they were never mutated now). However, since we're deep-copying, the reset is technically unnecessary for correctness. Keep it as a safety measure.

Consider also resetting before `startBattle` instead of after battle ends, to ensure the party always starts fresh:

```kotlin
fun startBattle(monsterId: String) {
    // Reset party to full health before battle
    _party.value.forEach { hero ->
        hero.currentHp = hero.maxHp
        hero.shield = 0
        hero.ultimateGauge = 0
        hero.isDead = false
    }
    ...
}
```

This is simpler and guarantees fresh stats regardless of any prior state.

### 3. Verify `HeroInstance.copy()` behavior

`HeroInstance` is a data class with a `copy()` method. Since all fields are in the constructor, `copy()` creates a true independent instance for value-type fields (`Int`, `Boolean`, etc.). The only mutable reference field is `equippedItems: MutableList<String>`, which is why we explicitly deep-copy it.

### 4. Verify `MonsterInstance` creation

Monsters are created fresh each battle via `monster.createInstance()`, so they don't have a persistence issue.

### 5. Test scenarios

After the fix, verify:
- Heroes start each battle with full HP, 0 shield, 0 ultimate gauge
- Hero level-ups and equipment changes persist correctly (they use `_saveData.value.party` which is separate)
- Party screen shows correct HP between battles
- Equipment is still correctly carried into battle (via `equippedItems` copy)
