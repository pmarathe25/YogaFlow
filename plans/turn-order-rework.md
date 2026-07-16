# Turn Order Rework — Implementation Plan

## Summary
Remove the SPD (speed) mechanic from data models, combat logic, and UI. Replace the speed-sorted turn order with a fixed sequence: all monsters act first each round, then the player selects heroes one at a time in any order. After all alive heroes act, the round advances (monsters go again).

---

## Prerequisites

No build-breaking cyclic dependencies exist. The changes can be applied in the order below.

**Build verification**: After each phase, run `./gradlew assembleDebug` to catch compile errors early.

---

## Phase 1 — Remove SPD from Data Models (lowest risk, pure removal)

### 1.1 `app/src/main/java/com/example/game/model/Skill.kt`
- **Line 31–33**: Remove the `ActionSpeed` enum entirely:
  - Delete lines 31–33.
- **Reason**: This enum is already unused in combat logic but imported in BattleState.kt.

### 1.2 `app/src/main/java/com/example/game/model/Hero.kt`
- **Line 11**: Delete `val baseSpd: Int`.
- **Lines 35–36**: Delete the `spdPercent` computation (the entire val that filters `SPD_PERCENT` and sums).
- **Line 42**: Delete `val spdMult = (1f + spdPercent) * (1f + allStatsPercent)`.
  - Change line ~41 to compute `atkMult` and `hpMult` using the same formula but remove the `spdMult` line.
  - Also update the `atkMult` and `hpMult` formulas — they currently multiply `(1f + allStatsPercent)`. Since `allStatsPercent` was also used for SPD, decide: either keep `ALL_STATS_PERCENT` affecting ATK/HP only (by renaming intent), or remove `ALL_STATS_PERCENT` references from these multipliers. **Recommendation**: Keep `ALL_STATS_PERCENT` for ATK/HP only (change nothing in multipliers for now, just remove `spdMult`).
- **Line 47**: Delete `val finalSpd = (baseSpd * spdMult * levelMult).toInt()`.
- **Lines 49–62**: In the `CombatantState(...)` constructor call at the `return`, remove:
  - `speed = finalSpd,` (line 57)
  - Replace with `speed = 0` (or remove the parameter if `CombatantState` no longer has it — see Phase 1.3).

### 1.3 `app/src/main/java/com/example/game/model/BattleState.kt`
- **Line 5**: Delete `import com.example.game.model.ActionSpeed`.
- **Line 31**: Remove `val speed: Int,` from `CombatantState`.
- **Line 176**: Remove `val speed: Int,` from `BattleActor`.
- **Lines 173–179**: `BattleActor` becomes:
  ```kotlin
  data class BattleActor(
      val id: String,
      val name: String,
      val isHero: Boolean,
      val element: Element = Element.NEUTRAL
  )
  ```

### 1.4 `app/src/main/java/com/example/game/model/Monster.kt`
- **Line 10**: Delete `val baseSpd: Int`.
- **Line 56–72**: In `toCombatantState()`:
  - **Line 64**: Delete `speed = baseSpd,`.

### 1.5 `app/src/main/java/com/example/game/model/Equipment.kt`
- **Enum `EquipmentEffectType`** (lines 100–131): Remove these entries from the enum:
  - `SPD_PERCENT` (line 103)
  - `SPD_ON_ULTIMATE` (line 128)
  - `SPD_ON_SKILL` (line 129)
  - `ALL_STATS_PERCENT` (line 118) — **optional**: Keep but rename intent to "ATK & HP" in description. **Recommendation**: Remove `ALL_STATS_PERCENT` since it no longer makes sense without SPD.
- **`bonusDescription`** property (lines 42–76):
  - Delete the `when` branches for `SPD_PERCENT` (lines 47), `ALL_STATS_PERCENT` (line 62), `SPD_ON_ULTIMATE` (line 72), `SPD_ON_SKILL` (line 73).

### 1.6 `app/src/main/java/com/example/game/model/StatusEffect.kt`
- **Lines 8–9**: Remove `SPD_UP,` and `SPD_DOWN,` from `StatusEffectType` enum.

### 1.7 `app/src/main/java/com/example/game/battle/DamageCalculator.kt`
- **Lines 284–285**: Remove the `spdBuff` variable.
- **Line 294**: Change `atkBuffMultiplier = atkBuff + (spdBuff * 0.1f)` to `atkBuffMultiplier = atkBuff`.
- **No changes to `computeBuffMultiplier`** — it's generic and may still be used for `ATK_UP` etc.
- **No changes to `computeDamage`** — it already accepts `atkBuffMultiplier: Float = 0f`.

### 1.8 `app/src/main/java/com/example/game/battle/BattleReducer.kt`
- Remove SPD from `calculateTurnOrder` — this function will be completely replaced in Phase 2. For now:
  - **Lines 626–644**: Modify `calculateTurnOrder` to **not sort by speed**. Either make it identity-ordered (heroes then monsters) or simply remove the `speed` field from `BattleActor` constructor calls (this is a transitional step; final version comes in Phase 2).
  - **Lines 633, 636**: Remove `.speed` from `BattleActor(...)` constructor calls.
  - **Lines 639–643**: Remove sorting logic; replace with simple concatenation (heroes first for now — Phase 2 will change order).

### 1.9 UI: Status Icons and Stat Display — Remove SPD references

#### `app/src/main/java/com/example/game/ui/components/BattleHUD.kt`
- **Lines 185–186**: Remove `SPD_UP` and `SPD_DOWN` cases from the `StatusIcon` `when` block.

#### `app/src/main/java/com/example/game/ui/components/PartyScreen.kt`
- **Line 125**: In `HeroStats(spd: Int)`, rename to `...`. **Remove `spd`**:
  ```kotlin
  private data class HeroStats(val maxHp: Int, val atk: Int)
  ```
- **Line 132**: Delete `spd = (hero.baseSpd * mult).toInt()`.
- **Line 298**: Delete the `StatBar("SPD", stats.spd, 25, Color.Cyan, ...)` line entirely.
- **Lines 601–603**: In `LevelUpDialog`:
  - Delete `StatComparisonRow("SPD", currentStats.spd, nextStats.spd, Color.Cyan)` (line 603).

#### `app/src/main/java/com/example/game/ui/components/MonsterRoadSelection.kt`
- **Line 772**: Delete `StatChip("SPD", "${monster.baseSpd}", Color(0xFF2196F3))`.

### 1.10 `app/src/main/java/com/example/game/battle/MonsterAI.kt`
- **Line 139**: In `checkPhaseTriggers` `SUMMON_ADD` block, remove `speed = 50,` from `CombatantState(...)`.
- **Line 156**: Remove `.speed` from `BattleActor(...)` constructor.

---

## Phase 2 — New Turn Order: Monsters First, Player-Selectable Heroes

### 2.1 Add `selectedHeroId` and `heroesActedThisRound` to `BattleState`

**File**: `app/src/main/java/com/example/game/model/BattleState.kt`

Add two new fields to `BattleState` (after line 139, before `) {`):

```kotlin
    val selectedHeroId: String = "",           // which hero's cards are currently showing
    val heroesActedThisRound: Set<String> = emptySet(),  // heroes who have acted this round
```

Update the `advanceRound()` function (line 155) to also reset `heroesActedThisRound`:
```kotlin
    return copy(round = round + 1, statusEffects = newStatusEffects, heroesActedThisRound = emptySet())
```

### 2.2 Rewrite `calculateTurnOrder` in `BattleReducer`

**File**: `app/src/main/java/com/example/game/battle/BattleReducer.kt`, lines 626–644.

Replace the speed-based sorting with a fixed order:
```kotlin
private fun calculateTurnOrder(
    heroes: List<CombatantState>,
    monsters: List<CombatantState>,
    rng: RandomProvider = DefaultRandomProvider
): List<BattleActor> {
    val actors = mutableListOf<BattleActor>()
    // Monsters first
    monsters.filter { !it.isDefeated }.forEach { m ->
        actors.add(BattleActor(m.id, m.name, false, m.element))
    }
    // Heroes second
    heroes.filter { !it.isDefeated }.forEach { h ->
        actors.add(BattleActor(h.id, h.name, true, h.element))
    }
    return actors
}
```
- Remove the `rng` parameter usage (it's no longer needed). Keep `rng` in the function signature until callers are updated.
- Remove tiebreaker logic.

### 2.3 Modify `startBattle` in `BattleReducer`

**File**: `app/src/main/java/com/example/game/battle/BattleReducer.kt`, lines 31–61.

Change the phase calculation — monsters always go first:
```kotlin
fun startBattle(
    heroes: List<CombatantState>,
    monsters: List<CombatantState>
): BattleState {
    val readyHeroes = heroes.map { ... }  // unchanged
    val readyMonsters = monsters.map { ... }  // unchanged
    val queue = calculateTurnOrder(readyHeroes, readyMonsters, rng)
    val first = queue.firstOrNull()
    val phase = when {
        first == null -> VICTORY
        else -> ENEMY_TURN  // Always monsters first
    }
    return BattleState(
        heroes = readyHeroes,
        monsters = readyMonsters,
        turnOrder = queue,
        currentTurnIndex = 0,
        currentActorId = first?.id.orEmpty(),
        phase = phase,
        heroesActedThisRound = emptySet(),
        selectedHeroId = ""
    ).withComboAvailability()
}
```

### 2.4 Modify `advanceTurn` in `BattleReducer`

**File**: `app/src/main/java/com/example/game/battle/BattleReducer.kt`, lines 74–125.

The key changes in `advanceTurn`:

1. **When wrapping to next round**: Reset `heroesActedThisRound` and set `selectedHeroId = ""`.
2. **When entering PLAYER_TURN**: Don't set `currentActorId` to a specific hero. Instead set it to empty string and let the player pick.
3. **Turn order is now just a visual display**: The `turnOrder` list shows all combatants with monsters first. The `currentTurnIndex` still increments but its meaning changes: during ENEMY_TURN phases, it indexes through monsters. During PLAYER_TURN phases, it indexes through ALL heroes (or stays at first hero index).

The revised logic:

```
advanceTurn(input):
    // terminal check (unchanged)
    // increment turnsTaken (unchanged)
    
    // Check if all heroes have acted this round
    if phase == PLAYER_TURN && heroesActedThisRound contains all aliveHeroIds:
        // All heroes done -> advance to next round (monsters turn)
        wrapped = true
    
    if wrapped:
        increment round, reset monsters' extraActionsThisRound, reset heroesActedThisRound
        recalculate turnOrder (new calculateTurnOrder)
        currentTurnIndex = 0
        selectedHeroId = ""
    
    val nextActor = turnOrder[currentTurnIndex]
    
    if nextActor.isHero:
        // In new system, during PLAYER_TURN the player selects any hero
        // currentActorId is "" until a hero is selected
        phase = PLAYER_TURN
        currentActorId = ""  // Player picks
        selectedHeroId = ""
    else:
        phase = ENEMY_TURN
        currentActorId = nextActor.id
    
    // ... rest unchanged
```

**Important**: The `advanceTurn` function is complex and interleaves status tick resolution. The exact implementation must carefully maintain all existing behavior (status tick, combatant death cleanup, combo availability) while changing only the turn progression logic.

### 2.5 Modify `useSkill` and `useUltimate` in `BattleReducer`

**File**: `app/src/main/java/com/example/game/battle/BattleReducer.kt`, lines 127–178.

**`useSkill`** and **`useUltimate`** currently check `state.currentActorId != heroId` to validate. Change this validation:
- Remove the `state.currentActorId != heroId` check.
- Add check: `heroId in state.heroesActedThisRound` => reject (hero already acted).
- Add check: `heroId` must be alive hero.
- After skill execution, mark the hero as acted:
```kotlin
val withActed = applied.copy(
    heroesActedThisRound = applied.heroesActedThisRound + heroId,
    selectedHeroId = ""  // deselect after action
)
```
- Also apply to `defend` and `useCombo`.

### 2.6 Grant `advanceTurn` the logic to auto-advance when all heroes acted

After `useSkill`/`useUltimate`/`defend`/`useCombo` complete, if the resulting state has all alive heroes in `heroesActedThisRound`, auto-call `advanceTurn` (recursively or via a loop) to move to enemy phase. Alternatively, this auto-advance can be handled by `BattleOrchestrator` (Phase 3).

---

## Phase 3 — Update BattleOrchestrator

**File**: `app/src/main/java/com/example/game/viewmodel/BattleOrchestrator.kt`

### 3.1 `onIntroComplete()` (lines 57–72)

Currently checks if first actor is hero/monster and transitions accordingly. With new system, it should always transition to `ENEMY_TURN`:
```kotlin
fun onIntroComplete() {
    val state = _battleState.value ?: return
    val firstMonster = state.monsters.firstOrNull { !it.isDefeated }
    val newState = state.copy(phase = ENEMY_TURN, currentActorId = firstMonster?.id ?: "")
    _battleState.value = newState
    if (firstMonster != null) {
        viewModelScope.launch {
            delay(1200)
            executeMonsterTurnLoop(firstMonster.id)
        }
    }
}
```

### 3.2 `advanceToNextTurn()` (lines 193–216)

After auto-advancing from one hero's action, check if all heroes have acted. If so, advance to next round (enemy turn). If not, stay in `PLAYER_TURN` with empty `currentActorId`.

Update the logic:
```kotlin
private suspend fun advanceToNextTurn() {
    val state = _battleState.value ?: return
    
    // If all alive heroes have acted, advance round
    val aliveHeroIds = state.aliveHeroes.map { it.id }.toSet()
    if (state.phase == PLAYER_TURN && aliveHeroIds.all { it in state.heroesActedThisRound }) {
        // Advance to next round
        val result = turnManager.advanceTurn(state)
        // ... handle result
    } else if (state.phase == PLAYER_TURN) {
        // Stay in PLAYER_TURN, wait for next hero selection
        _battleState.value = state.copy(currentActorId = "", selectedHeroId = "")
        _isProcessingTurn.value = false  // Already false from caller
        return  // Don't call advanceTurn yet
    } else {
        // Non-player-turn: normal advance
        val result = turnManager.advanceTurn(state)
        // ... handle result
    }
}
```

### 3.3 `executeSkill()` (lines 98–141), `executeUltimate()` (lines 143–160), `skipTurn()` (lines 74–91), `executeCombo()` (lines 162–179)

In each method, replace `state.currentActorId` checks:
- Remove `if (state.currentActorId != heroId) return` validation from `executeSkill` and `executeUltimate`.
- Add check: `if (heroId in state.heroesActedThisRound) return` — hero already acted.
- The heroId parameter is now the hero the player selected via tapping (see Phase 5).

After the skill resolves, `advanceToNextTurn` should handle whether to stay in PLAYER_TURN or move to ENEMY_TURN.

---

## Phase 4 — Update TurnManager

**File**: `app/src/main/java/com/example/game/battle/TurnManager.kt`

Minimal changes needed — `TurnManager` is a thin wrapper. No API changes required since `BattleReducer` does the heavy lifting. Verify that:
- `advanceTurn` returns a valid `AdvanceTurnResult` with the new state's phase correctly set.
- `executeSkill`/`executeUltimate`/`defend`/`executeCombo` correctly propagate the `heroesActedThisRound` update.

If any of those functions return a `TurnResult` without the new field, update `TurnResult` to carry `heroesActedThisRound` or just rely on `newState.heroesActedThisRound`.

---

## Phase 5 — Update Battle UI (Hero Selection & Action Tray)

### 5.1 `app/src/main/java/com/example/game/ui/components/BattleScene.kt`

#### Hero Tap Handlers (lines 317–382)

Currently hero sprites in the hero zone are clickable only during targeting (when `isTargeting` is true). Modify to make them **also tappable during PLAYER_TURN when no hero is selected**:

```kotlin
val isHeroSelectable = state.phase == PLAYER_TURN && 
    state.currentActorId.isEmpty() &&   // No specific hero's "turn" — player picks
    !state.heroesActedThisRound.contains(hero.id) &&  // Not yet acted
    !hero.isDefeated

val heroClickable = if (isTargeting && canTarget) {
    Modifier.clickable { ... }  // existing targeting logic
} else if (isHeroSelectable) {
    Modifier.clickable {
        viewModel.selectHero(hero.id)  // New method — see below
    }
} else Modifier
```

#### Visual Greying of Acted Heroes

Add visual feedback for heroes who have already acted. In the hero rendering block (around line 347), apply:
```kotlin
val hasActed = hero.id in state.heroesActedThisRound
val heroAlpha by animateFloatAsState(
    targetValue = if (hasActed) 0.4f else 1f,
    animationSpec = tween(300)
)
// Apply alpha modifier to the hero column
modifier = Modifier.graphicsLayer { alpha = heroAlpha }
```

#### Action Tray Display (lines 423–463)

Currently shows `if (currentHero != null && state.phase == PLAYER_TURN)`. Change to:
```kotlin
if (state.selectedHeroId.isNotEmpty() && state.phase == PLAYER_TURN) {
    val selectedHero = state.heroes.find { it.id == state.selectedHeroId }
    if (selectedHero != null) {
        ActionTray(
            currentHero = selectedHero,
            ...
        )
    }
}
```

#### Turn Order Sidebar (lines 573–596, `TurnOrderList`)

Keep the turn order sidebar but update the "has acted" logic. Currently uses `turnOrder` index comparison. Change to use `heroesActedThisRound`:
```kotlin
val hasActed = if (actor.isHero) {
    actor.id in state.heroesActedThisRound
} else {
    // Monsters act once per round via turn index
    actorIndex < state.currentTurnIndex
}
```

Also, emphasize monsters-first order: the `turnOrder` list already has monsters first. The `TurnOrderList` just renders what it's given.

#### Turn Banner (lines 510–515)

When no specific actor is selected (`currentActorId` is empty during PLAYER_TURN), show "CHOOSE A HERO" instead of an actor name.

### 5.2 `app/src/main/java/com/example/game/ui/components/ActionTray.kt`

#### `actedHeroIds` calculation (lines 161–166)

Currently computes `actedHeroIds` from `turnOrder` and `currentTurnIndex`. Change to accept `heroesActedThisRound: Set<String>` as a parameter instead:
```kotlin
@Composable
fun ActionTray(
    currentHero: CombatantState,
    turnOrder: List<BattleActor>,
    currentTurnIndex: Int,
    heroesActedThisRound: Set<String>,   // NEW param
    skillCooldowns: Map<String, Int>,
    ...
)
```

And remove the `actedHeroIds` computed variable (line 161–166), replacing all references to `actedHeroIds` with `heroesActedThisRound.mapNotNull { it.toIntOrNull() }.toSet()`.

#### `HandOfCards` — same change (lines 161–166)

Pass `heroesActedThisRound` through and use it for the `actedHeroIds` derivation.

### 5.3 Add `selectHero()` method to `GameViewModel`

**File**: `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt`

```kotlin
fun selectHero(heroId: String) {
    val state = _battleState.value ?: return
    if (state.phase != PLAYER_TURN) return
    if (heroId in state.heroesActedThisRound) return
    _battleState.value = state.copy(selectedHeroId = heroId, currentActorId = heroId)
}

fun deselectHero() {
    val state = _battleState.value ?: return
    _battleState.value = state.copy(selectedHeroId = "", currentActorId = "")
}
```

---

## Phase 6 — Update JSON Data Files

### 6.1 `app/src/main/assets/game/heroes.json`
Remove the `"baseSpd"` field from every hero entry. Also remove `"speedWeight"` from every skill entry (the `ActionSpeed` enum is unused).

### 6.2 `app/src/main/assets/game/monsters.json`
Remove the `"baseSpd"` field from every monster entry.

### 6.3 `app/src/main/assets/game/equipment.json`
Either:
- Remove all entries that have `"type": "SPD_PERCENT"`, `"type": "SPD_ON_ULTIMATE"`, `"type": "SPD_ON_SKILL"`, `"type": "ALL_STATS_PERCENT"` in their effects arrays.
- Or leave the entries but the Kotlin enum will be out of date (Gson will ignore unknown values if configured correctly — verify Gson deserialization handles missing enum values gracefully). **Recommendation**: Remove from JSON to keep data clean.

---

## Phase 7 — Update Tests

### 7.1 `app/src/test/java/com/example/game/battle/BattleReducerTest.kt`
- Remove `speed` parameter from `makeCombatant` helper (line 52) and all call sites.
- Rewrite `startBattle produces correct turn order` test (line 77): verify monsters first, then heroes.
- Remove assertions checking `state.turnOrder[0].speed >= state.turnOrder[1].speed` (line 84).
- Rewrite `advanceTurn switches from PLAYER_TURN to ENEMY_TURN` test (line 96): verify ENEMY_TURN starts first.
- Fix all test call sites that pass `speed = ...` to `makeCombatant`.
- Add new tests:
  - Test that heroes can act in any order.
  - Test that `heroesActedThisRound` is populated correctly.
  - Test that advanceTurn auto-progresses after all heroes act.
  - Test that acted heroes cannot act again.

### 7.2 `app/src/test/java/com/example/game/battle/TurnManagerTest.kt`
- Remove `baseSpd` parameter from `makeHero` (line 39) and `makeMonster` (line 54).
- Change `speed = baseSpd` to not set speed (or set 0 if field retained).
- Rewrite tests to match new turn order expectations.

---

## Phase 8 — Cleanup and Verification

### 8.1 Remove `BattleActor.speed` entirely
Ensure no remaining references to `speed` on `BattleActor` or `CombatantState` anywhere in the codebase (run global search for `.speed` in Kotlin files under `com/example/game/`).

### 8.2 Remove all `EquipmentEffectType` references to SPD/ALL_STATS
Any code that matches on these enum values (in `Hero.kt`, equipment generation, etc.) must be removed.

### 8.3 Remove all `StatusEffectType.SPD_UP` / `SPD_DOWN` references
Ensure no code references these status types (skills, data, StatusResolver, etc.).

### 8.4 Lint and Build
```bash
./gradlew assembleDebug
./gradlew lint
./gradlew test
```

### 8.5 Manual verification checklist
1. Start a battle — intro animation plays.
2. Monsters take their turns first (all monsters act).
3. Player sees all unacted heroes highlighted; acted heroes are dimmed.
4. Tapping an unacted hero shows their action cards (ActionTray).
5. Selecting a skill for that hero executes it, hero becomes dimmed.
6. After all heroes act, monsters go again automatically.
7. Victory/defeat conditions still work.
8. Turn order sidebar shows monsters first, then heroes, with correct act/fade status.
9. Party screen no longer shows SPD stat bar.
10. Monster road selection no longer shows SPD chip.
11. Equipment descriptions no longer reference speed.

---

## Dependency Order Summary

| Step | Phase | Can be done independently |
|------|-------|--------------------------|
| Remove `ActionSpeed` enum | 1.1 | Yes |
| Remove `baseSpd` from Hero | 1.2 | After 1.3 (CombatantState must be ready) |
| Remove `speed` from CombatantState & BattleActor | 1.3 | Must be FIRST — blocks 1.2, 1.4, 1.8, 5.x |
| Remove `baseSpd` from Monster | 1.4 | After 1.3 |
| Remove SPD from Equipment.kt | 1.5 | Independent |
| Remove SPD from StatusEffect.kt | 1.6 | Independent |
| Clean DamageCalculator | 1.7 | After 1.6 |
| Clean BattleReducer speed refs | 1.8 | After 1.3 |
| UI SPD removal (BattleHUD, PartyScreen, MonsterRoad) | 1.9 | After 1.3 |
| MonsterAI speed removal | 1.10 | After 1.3 |
| New BattleState fields | 2.1 | Independent |
| Rewrite calculateTurnOrder | 2.2 | After 2.1 |
| Modify startBattle | 2.3 | After 2.2 |
| Modify advanceTurn | 2.4 | After 2.2 |
| Modify useSkill/useUltimate/defend/combo | 2.5 | After 2.1 |
| Orchestrator updates | 3.x | After Phase 2 |
| TurnManager updates | 4 | After Phase 2 |
| Battle UI updates | 5.x | After Phase 3 |
| JSON data updates | 6 | After Phase 1 (can be parallel) |
| Test updates | 7 | After Phase 3 (or alongside) |

**Recommended execution order**: 1.3 → 1.1 → 1.2 → 1.4 → 1.5 → 1.6 → 1.7 → 1.8 → 1.9 → 1.10 → 2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 2.6 → 3.x → 4 → 5.x → 6 → 7 → 8.
