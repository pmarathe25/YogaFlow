# Plan: Rewrite Battle Logic from Scratch

## Current Problems

1. **GameViewModel is 726 lines** — mixes battle orchestration, save management, equipment, economy, hero level-up, and shop logic. Battle flow code is intertwined with Android lifecycle (`viewModelScope`, `Application` context), making it impossible to unit test the turn logic.

2. **`BattleEngine` uses hardcoded `Random`** — `computeSkillOutcome`, `calculateTurnOrder`, and `computeMonsterOutcome` all call `Random.nextFloat()` directly. Tests cannot produce deterministic results for status effects or turn ordering.

3. **`BattleState` is a mutable class** — the entire battle mutates a single `BattleState` instance in place, then snapshots it for the UI. This makes reasoning about state transitions fragile and hides bugs.

4. **Turn flow is recursive and in the ViewModel** — `advanceTurn()` calls itself when the next actor is dead, and `executeMonsterTurn()` calls `advanceTurn()` which may call `executeMonsterTurn()` again. This recursive chain is the root cause of double-turn bugs.

5. **Dead code** — `BattleEngine.advanceTurn()` (56 lines) is never called. `GameViewModel.chooseMonsterTarget()` is never called. `GameViewModel.addHeroToParty()`/`removeHeroFromParty()` are stubs.

6. **No tests for turn flow** — the existing 28 tests only cover `BattleEngine` pure math functions. Zero tests cover turn ordering, victory/defeat detection, round advancement, or the hero/monster action cycle.

7. **Wrappers that duplicate engine logic** — `GameViewModel.computeSkillOutcome`, `computeComboOutcome`, `computeMonsterOutcome` are one-line wrappers around `BattleEngine` that add no value but create an extra layer to maintain.

## Proposed Architecture

```
app/src/main/java/com/example/game/
├── battle/
│   ├── BattleEngine.kt            # Pure stateless math (extracted, cleaned up)
│   ├── RandomProvider.kt          # Interface for injectable randomness
│   ├── TurnManager.kt             # Pure turn flow state machine
│   └── BattleState.kt             # Immutable data classes (no mutation in place)
├── model/                         # Keep as-is (data definitions)
├── persistence/                   # Keep as-is
├── viewmodel/
│   └── GameViewModel.kt           # Thin coordinator (much smaller)
└── ui/components/                 # Keep as-is (UI only)
```

### 1. New File: `battle/RandomProvider.kt`

```kotlin
fun interface RandomProvider {
    fun nextFloat(): Float
    fun nextInt(until: Int): Int
}

object DefaultRandomProvider : RandomProvider {
    override fun nextFloat(): Float = Random.nextFloat()
    override fun nextInt(until: Int): Int = Random.nextInt(until)
}
```

This is the single most important change for testability. Every function in `BattleEngine` that needs randomness will accept a `RandomProvider` parameter (defaulting to `DefaultRandomProvider`). Tests pass a deterministic provider that returns known values.

### 2. Refactored: `battle/BattleEngine.kt`

**What changes:**
- Remove the dead `advanceTurn()` method entirely
- Every function that uses `Random` gains a `RandomProvider` parameter
- `computeSkillOutcome` becomes a pure function: `(hero, skill, state, targets, rng) -> SkillOutcomeResult`
- `computeMonsterOutcome` similarly accepts `rng`
- `calculateTurnOrder` accepts `rng`
- `computeComboOutcome` already has no randomness — keep as-is
- Extract `applyOutcome` to work with immutable state: returns a new `BattleState` instead of mutating in place

**Breaking change to `applyOutcome`:**

```kotlin
// Before: mutates state in place, returns same reference
fun applyOutcome(state: BattleState, outcome: ActionOutcome): BattleState

// After: takes an immutable state, returns a new one
fun applyOutcome(state: BattleState, outcome: ActionOutcome): BattleState
```

This means `applyOutcome` cannot call `state.addEvent()` internally. Instead, it returns both the new state and a list of new events:

```kotlin
fun applyOutcome(state: BattleState, outcome: ActionOutcome): Pair<BattleState, List<BattleEvent>>
```

Or, better, include generated events inside `SkillOutcomeResult`:

```kotlin
data class SkillOutcomeResult(
    val outcome: ActionOutcome,
    val newState: BattleState,
    val newEvents: List<BattleEvent>
)
```

But wait — this mixes the "compute outcome" and "apply outcome" phases. Better to keep them separate for testing individual pieces. Let me think about this differently.

Actually, the cleanest approach:

```kotlin
// computeDamage, computeHeal, computeShield — unchanged, pure
// computeSkillOutcome — pure, no state mutation, takes RNG
// computeComboOutcome — pure, no state mutation
// computeMonsterOutcome — pure, no state mutation, takes RNG

// applyOutcome — takes BattleState + ActionOutcome, returns (BattleState, List<BattleEvent>)
```

The `BattleState` snapshot is the source of truth. When an action is performed:
1. Compute outcome (pure, using state snapshot)
2. Create new state by applying outcome to snapshot
3. Collect generated events for the UI/log

### 3. New File: `battle/TurnManager.kt`

This is the heart of the refactor — a pure state machine that replaces the 200+ lines of turn flow in `GameViewModel`:

```kotlin
data class TurnResult(
    val newState: BattleState,
    val logMessages: List<String>,
    val events: List<BattleEvent>,
    val victory: Boolean = false,
    val defeat: Boolean = false
)

class TurnManager(private val rng: RandomProvider = DefaultRandomProvider) {

    /**
     * Initialize battle: create turn order, set current actor, return initial state.
     */
    fun startBattle(
        heroes: List<HeroInstance>,
        monsters: List<MonsterInstance>
    ): BattleState

    /**
     * Advance to the next actor in turn order.
     * Handles round wrapping, turn re-calculation, status effect decay, and cooldowns.
     * Returns next state + any events/logging.
     */
    fun advanceTurn(state: BattleState): AdvanceTurnResult

    /**
     * Execute a hero skill. Pure — takes state, returns new state + side effects.
     */
    fun executeSkill(
        state: BattleState,
        heroId: String,
        skill: Skill,
        targets: List<String>
    ): TurnResult

    /**
     * Execute a hero ultimate. Same pattern as skill.
     */
    fun executeUltimate(
        state: BattleState,
        heroId: String
    ): TurnResult

    /**
     * Execute a combo skill.
     */
    fun executeCombo(
        state: BattleState,
        combo: ComboSkill,
        participantIds: Set<String>
    ): TurnResult

    /**
     * Execute a monster's turn (AI decision + skill resolution).
     */
    fun executeMonsterTurn(
        state: BattleState,
        monsterId: String
    ): TurnResult
}
```

**Key design decisions:**
- All methods return a result object, never mutate the input state
- `BattleState` becomes a data class (or at least has no public setter methods that mutate internal lists)
- Log messages are returned as data, not written to a ViewModel MutableStateFlow
- Victory/defeat flags let the ViewModel react (play animation, navigate to result screen)
- Timing/delays are NOT in this class — the ViewModel orchestrates delays between turns

**How turn advancement works (fixes the recursive bug):**

```
advanceTurn(state):
  1. Increment turnIndex, handle round wrapping
  2. If wrapped: advanceRound (status decay), recalculate turn order
  3. Check victory (all monsters dead) -> return defeat/victory flag
  4. Check defeat (all heroes dead) -> return defeat flag
  5. Get next actor
  6. Decrement cooldowns for current actor
  7. If next actor is dead -> advanceTurn again (tail recursion, clean)  
     OR handle by skipping (but without the bug — skip properly)
  8. Determine phase: PLAYER_TURN or ENEMY_TURN
  9. If ENEMY_TURN: compute monster AI decision + outcome in same call
  10. Return result
```

Actually, for monsters, we should NOT resolve their turn inside `advanceTurn`. Instead:

```
advanceTurn(state):
  ... step 1-7 above
  return AdvanceTurnResult(
    newState = ...,
    nextActorId = ...,
    phase = PLAYER_TURN or ENEMY_TURN
  )
```

Then the ViewModel (or a test) sees ENEMY_TURN and calls `executeMonsterTurn` separately. This keeps the flow explicit and testable.

**State data class refactoring:**

```kotlin
data class BattleState(
    val heroes: List<HeroInstance>,
    val monsters: List<MonsterInstance>,
    val turnOrder: List<BattleActor>,
    val currentTurnIndex: Int,
    val currentActorId: String,
    val phase: BattlePhase,
    val round: Int,
    val turnsTaken: Int,
    val statusEffects: Map<String, List<BattleStatus>>,
    val eventLog: List<BattleEvent>,
    val skillCooldowns: Map<String, Map<String, Int>>,
    val isComboAvailable: Boolean,
    val pendingSkill: Skill?,
    val showTargetSelection: Boolean
)
```

Key changes:
- `heroes` and `monsters` are `List<...>` (not `MutableList`)
- `statusEffects` and `skillCooldowns` use immutable collections
- No `addEvent()` method — events are generated and returned separately, then appended to create a new state
- No `snapshot()` needed — the data class is already immutable

### 4. Refactored: `viewmodel/GameViewModel.kt`

The ViewModel becomes a thin coordinator:

```kotlin
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val turnManager = TurnManager()
    // ... save management, navigation, equipment remain ...

    fun startBattle(monsterId: String) {
        // ...create hero/monster instances...
        val initialState = turnManager.startBattle(heroes, monsters)
        _battleState.value = initialState
        // ...emit battle log, navigate to battle screen...
        // Check if first turn is monster -> schedule monster execution
        if (initialState.phase == ENEMY_TURN) {
            scheduleMonsterTurn(initialState.currentActorId)
        }
    }

    fun executeSkill(heroId: String, skill: Skill, customTargets: List<String>?) {
        viewModelScope.launch {
            val state = _battleState.value ?: return@launch
            val result = turnManager.executeSkill(state, heroId, skill, resolvedTargets)
            _battleState.value = result.newState
            _battleLog.value = _battleLog.value + result.logMessages
            delay(1000)
            advanceToNextTurn()
        }
    }

    private suspend fun advanceToNextTurn() {
        val state = _battleState.value ?: return
        val result = turnManager.advanceTurn(state)
        _battleState.value = result.newState
        if (result.victory) { /* ... */ return }
        if (result.defeat) { /* ... */ return }
        if (result.newState.phase == ENEMY_TURN) {
            delay(1200)
            executeMonsterTurn(result.newState.currentActorId)
        }
    }

    private suspend fun executeMonsterTurn(monsterId: String) {
        val state = _battleState.value ?: return
        val result = turnManager.executeMonsterTurn(state, monsterId)
        // Handle multi-action monsters (extraActionsThisRound)
        _battleState.value = result.newState
        _battleLog.value = _battleLog.value + result.logMessages
        delay(1500)
        advanceToNextTurn()
    }
}
```

The ViewModel:
- No longer contains business logic for damage/heal/target computation
- No longer manually manipulates turn indices or cooldowns
- Only orchestrates delays, navigation, and persistence
- All pure logic is testable without Android dependencies

### 5. Updated Test Suite: `BattleEngineTest.kt`

**Keep all existing tests** (they test pure math). **Add new test classes:**

**`TurnManagerTest.kt`** — the core new test file:
```kotlin
class TurnManagerTest {
    private val rng = FixedRandomProvider(/* known values */)
    private val turnManager = TurnManager(rng)
    private val dummySkill: Skill = ...
    private val dummyUltimate: Skill = ...

    @Test
    fun `startBattle_createsTurnOrder_includesAllAliveActors`()
    @Test
    fun `startBattle_firstActorInTurnOrder_isCurrentActor`()
    @Test
    fun `startBattle_heroFirst_setsPhaseToPLAYER_TURN`()
    @Test
    fun `startBattle_monsterFirst_setsPhaseToENEMY_TURN`()
    @Test
    fun `advanceTurn_wrapsAround_advancesRoundAndDecaysStatuses`()
    @Test
    fun `advanceTurn_wrapsAround_recalculatesTurnOrder`()
    @Test
    fun `advanceTurn_allMonstersDead_setsPhaseToVictory`()
    @Test
    fun `advanceTurn_allHeroesDead_setsPhaseToDefeat`()
    @Test
    fun `advanceTurn_nextActorIsHero_setsPhaseToPLAYER_TURN`()
    @Test
    fun `advanceTurn_nextActorIsMonster_setsPhaseToENEMY_TURN`()
    @Test
    fun `advanceTurn_decrementsCooldowns_forCurrentActor`()
    @Test
    fun `advanceTurn_skipCount_advancesMultiplePositions`()  // combo skip
    @Test
    fun `advanceTurn_deadActorInOrder_skipsToNextAlive`()
    @Test
    fun `advanceTurn_deadActorDoesNotDoubleTurn`()  // regression test for Bug 1
    @Test
    fun `advanceTurn_comboSkipOnWrap_doesNotResetToIndexZero`()  // regression for Bug 2

    @Test
    fun `executeSkill_damageSkill_reducesTargetHP`()
    @Test
    fun `executeSkill_healSkill_restoresAllyHP`()
    @Test
    fun `executeSkill_shieldSkill_addsShield`()
    @Test
    fun `executeSkill_killTriggersHeroDownEvent`()
    @Test
    fun `executeSkill_monsterKillTriggersMonsterDownEvent`()
    @Test
    fun `executeSkill_withCooldown_setsCooldown`()
    @Test
    fun `executeSkill_skillOnCooldown_doesNothing`()
    @Test
    fun `executeUltimate_resetsUltimateGaugeToZero`()
    @Test
    fun `executeUltimate_whenGaugeNotFull_noOp`()

    @Test
    fun `executeCombo_multiTarget_appliesToAll`()
    @Test
    fun `executeCombo_participantsLoseUltimateGauge`()

    @Test
    fun `executeMonsterTurn_basicAttack_damagesHero`()
    @Test
    fun `executeMonsterTurn_specialAttack_usesSpecialSkill`()
    @Test
    fun `executeMonsterTurn_checksPhaseTriggers_beforeAndAfter`()  // regression for Bug 3
    @Test
    fun `executeMonsterTurn_extraAction_actsAgain`()
    @Test
    fun `executeMonsterTurn_killsHero_triggersHeroDown`()

    @Test
    fun `monster_phaseTrigger_gainShield_atThreshold`()
    @Test
    fun `monster_phaseTrigger_extraAction_grantsExtraTurn`()
}
```

## Migration Strategy

### Phase 1: Create pure foundation (no behavior change)
1. Create `battle/RandomProvider.kt` interface
2. Create `battle/TurnManager.kt` with the new immutable state machine
3. Refactor `BattleState` from mutable class to immutable data class
4. Add `rng` parameter to all `BattleEngine` functions that use randomness

### Phase 2: Update BattleEngine
5. Remove dead `BattleEngine.advanceTurn()`
6. Make `applyOutcome` return `Pair<BattleState, List<BattleEvent>>` instead of mutating
7. Ensure all functions accept `RandomProvider`

### Phase 3: Rewire GameViewModel
8. Replace all battle flow logic in `GameViewModel` with calls to `TurnManager`
9. Remove dead wrapper methods
10. Keep only: save management, equipment, economy, navigation, timing/delays

### Phase 4: Comprehensive testing
11. Add `TurnManagerTest` (30+ tests covering all turn flow paths)
12. Add deterministic RNG tests for `BattleEngine`
13. Verify all regression bugs from the existing plan have test coverage
14. Run `BattleEngineTest` to confirm no regressions in existing math tests

### Phase 5: Cleanup
15. Remove all dead code
16. Move `battle/` package to be co-located (keep model, merge appropriately)
17. Verify the app compiles and runs end-to-end

## Files to Create
- `app/src/main/java/com/example/game/battle/RandomProvider.kt`
- `app/src/main/java/com/example/game/battle/TurnManager.kt`

## Files to Modify
- `app/src/main/java/com/example/game/viewmodel/BattleEngine.kt`
- `app/src/main/java/com/example/game/model/BattleState.kt`
- `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt`
- `app/src/test/java/com/example/game/viewmodel/BattleEngineTest.kt`

## Files Created for Tests
- `app/src/test/java/com/example/game/battle/TurnManagerTest.kt`
