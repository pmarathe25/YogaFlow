# Plan: Extract BattleEngine from GameViewModel

**Dependencies:** None — pure Kotlin refactor, independent of other plans.
**Files touched:** `GameViewModel.kt` (modify — remove logic), `BattleEngine.kt` (create new), potentially `BattleState.kt` (minor — move related types if needed)
**Priority:** High

---

## Goal

Extract all pure battle logic functions from the 1050-line `GameViewModel` into a stand-alone `BattleEngine` object with **zero Android dependencies** (no `Context`, no `Application`, no `ViewModel`). This makes the logic testable with plain JUnit and significantly reduces `GameViewModel` complexity.

---

## What to extract

The following methods in `GameViewModel` are **pure functions** (they take inputs, return outputs, no side effects):

| Method | Inputs | Returns | Lines (approx) |
|--------|--------|---------|----------------|
| `computeDamage` | ATK, DEF, element chart, variance | `DamageResult` | ~40 |
| `computeSkillOutcome` | hero, skill, targets, state | `ActionOutcome` | ~30 |
| `computeComboOutcome` | combo, heroes, state | `ActionOutcome` | ~30 |
| `computeMonsterOutcome` | monster, target, state | `ActionOutcome` | ~30 |
| `computeHeal` | healer ATK, scaling | Int | ~15 |
| `computeShield` | caster ATK, scaling | Int | ~15 |
| `applyOutcome` | state, outcome | `BattleState` (modified) | ~80 |
| `calculateTurnOrder` | heroes, monsters | `List<BattleActor>` | ~20 |
| `resolveTargets` | skill, casterId, state | `List<String>` | ~25 |
| `checkPhaseTriggers` | state | `List<BattleEvent>` | ~40 |
| `chooseMonsterTarget` | monsters, heroes, strategy | String | ~20 |
| `computeDamageBreakdown` | damage, type, element | `DamageBreakdown` | ~10 |

**Total: ~355 lines** extracted from the ViewModel.

---

## New file: `BattleEngine.kt`

**Path:** `app/src/main/java/com/example/game/viewmodel/BattleEngine.kt`

### Exposed API

```kotlin
object BattleEngine {

    data class DamageResult(
        val amount: Int,
        val isCrit: Boolean,
        val isDodged: Boolean,
        val breakdown: DamageBreakdown
    )

    data class SkillOutcomeResult(
        val outcome: ActionOutcome,
        val events: List<BattleEvent>
    )

    /** Compute raw damage from attacker ATK to defender DEF, including elemental multipliers. */
    fun computeDamage(
        attackerAtk: Int,
        defenderDef: Int,
        attackerElement: Element,
        defenderElement: Element,
        damageComponent: DamageComponent? = null,
        baseDamage: Int = 0,
        isCrit: Boolean = false,
        variance: Float = 0.1f
    ): DamageResult

    /** Compute full outcome of a hero using a skill (supports damage/heal/shield/status/cleanse/revive). */
    fun computeSkillOutcome(
        hero: HeroInstance,
        skill: Skill,
        state: BattleState,
        targets: List<String>
    ): SkillOutcomeResult

    /** Compute full outcome of a combo skill. */
    fun computeComboOutcome(
        combo: ComboSkill,
        casterId: String,
        partnerIds: List<String>,
        state: BattleState
    ): SkillOutcomeResult

    /** Compute full outcome of a monster's turn. */
    fun computeMonsterOutcome(
        monster: MonsterInstance,
        targetId: String,
        state: BattleState
    ): SkillOutcomeResult

    /** Compute healing amount from ATK + scaling. */
    fun computeHeal(
        casterAtk: Int,
        healScaling: HealScaling
    ): Int

    /** Compute shield amount from ATK + scaling. */
    fun computeShield(
        casterAtk: Int,
        shieldScaling: ShieldScaling
    ): Int

    /** Apply an ActionOutcome to the battle state (modifies shields, HP, statuses, deaths). Returns new state. */
    fun applyOutcome(
        state: BattleState,
        outcome: ActionOutcome
    ): BattleState

    /** Sort actors by SPD (highest first) with random ±5% variance. */
    fun calculateTurnOrder(
        heroes: List<HeroInstance>,
        monsters: List<MonsterInstance>
    ): List<BattleActor>

    /** Resolve target IDs for a skill based on its TargetType. */
    fun resolveTargets(
        skill: Skill,
        casterId: String,
        state: BattleState
    ): List<String>

    /** Check and trigger monster phase transitions. */
    fun checkPhaseTriggers(
        state: BattleState
    ): List<BattleEvent>

    /** AI: choose which hero to target. */
    fun chooseMonsterTarget(
        heroes: List<HeroInstance>,
        strategy: TargetStrategy
    ): String

    /** Build a DamageBreakdown object. */
    fun computeDamageBreakdown(
        damage: Int,
        type: DamageType,
        element: Element
    ): DamageBreakdown
}
```

### Key behaviors to preserve exactly

#### computeDamage formula
```
baseDamage = (attackerAtk * 4) - (defenderDef * 2)
baseDamage = max(baseDamage, attackerAtk * 0.5)  // floor at 50% ATK
after elemental multiplier (see chart below)
after variance: random ±10%
crit: multiply by 1.5, always hits (bypasses dodge)
dodge: 5% base chance * speed ratio
```

#### Elemental multiplier chart (preserve existing values)
| Attacker \ Defender | FIRE | WATER | AIR | EARTH | LIGHT | DARK | SHADOW | ELECTRIC | VOID | NEUTRAL |
|---|---|---|---|---|---|---|---|---|---|---|
| FIRE | 1.0 | 0.5 | 2.0 | 0.5 | 1.0 | 1.0 | 1.0 | 1.5 | 1.0 | 1.0 |
| WATER | 2.0 | 1.0 | 0.5 | 0.5 | 1.0 | 1.0 | 1.0 | 1.5 | 1.0 | 1.0 |
| AIR | 0.5 | 2.0 | 1.0 | 0.5 | 1.0 | 1.0 | 1.0 | 1.5 | 1.0 | 1.0 |
| EARTH | 2.0 | 2.0 | 2.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.5 | 1.0 | 1.0 |
| LIGHT | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 2.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| DARK | 1.0 | 1.0 | 1.0 | 1.0 | 2.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| SHADOW | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 2.0 | 1.0 |
| ELECTRIC | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 2.0 | 1.0 |
| VOID | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 0.5 | 0.5 | 1.0 | 1.0 |
| NEUTRAL | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |

#### applyOutcome order of operations
1. For each target result:
   a. Shield absorbs damage first (`newShield = max(0, shield - damage)`, `overflow = max(0, damage - shield)`)
   b. Overflow damage reduces HP
   c. Apply healing (HP clamped to maxHP, can exceed if overheal allowed)
   d. Apply shielding (adds to shield value)
   e. Inflict status effects (with remaining turns)
   f. Apply buffs/debuffs
   g. Cleanse (remove all debuffs from target)
   h. Revive (if HP was 0, set HP to revive amount)
   i. Check death (HP <= 0 → mark alive = false)
2. Track total damage dealt, total healing done, total shield applied in `ActionOutcome`
3. Collect `HeroDown` / `MonsterDown` events for any deaths

#### calculateTurnOrder
```
1. Flatten heroes + monsters into BattleActor list (with type discriminator)
2. Sort by SPD descending
3. Add random variance of ±5% per actor
4. Return sorted list
```

#### checkPhaseTriggers
For each monster with phases:
1. Check current HP % against phase thresholds
2. If a new phase should trigger, generate appropriate `BattleEvent` (REFLECT_DAMAGE, SUMMON_ADD, GAIN_SHIELD, DOUBLE_ACTIONS, etc.)
3. Return list of triggered events (can be multiple)

---

## Refactoring GameViewModel

After extraction, `GameViewModel` methods become thin wrappers:

```kotlin
// BEFORE (in GameViewModel):
fun computeSkillOutcome(...): ActionOutcome {
    // 30 lines of logic
}

// AFTER (in GameViewModel):
fun computeSkillOutcome(...): ActionOutcome {
    return BattleEngine.computeSkillOutcome(hero, skill, state, targets).outcome
}
```

**Methods to refactor in GameViewModel:**
- `computeSkillOutcome` → delegate to `BattleEngine.computeSkillOutcome`
- `executeSkill` — keep orchestration (state mutation, event emission), extract damage computation
- `executeUltimate` — same pattern
- `executeCombo` — same pattern, delegate outcome computation to `BattleEngine`
- `advanceTurn` — keep turn cycling logic, delegate `calculateTurnOrder`
- `applyOutcome` → delegate to `BattleEngine.applyOutcome` (returns new state, ViewModel sets it)
- `checkPhaseTriggers` → delegate to `BattleEngine.checkPhaseTriggers`
- `chooseMonsterTarget` → delegate to `BattleEngine.chooseMonsterTarget`
- `resolveTargets` → delegate to `BattleEngine.resolveTargets`
- `computeMonsterOutcome` → delegate to `BattleEngine.computeMonsterOutcome`
- `computeDamage`, `computeHeal`, `computeShield`, `computeDamageBreakdown` → delete from VM

**State management that STAYS in GameViewModel:**
- `_battleState` / `emitBattleState` / `state.snapshot()`
- Turn cycling logic (`advanceTurn` orchestration)
- Event emission (`_battleEvents`)
- Combo gauge management
- Cooldown tracking
- Battle start/end logic
- Equipment/party/shop logic (unrelated to battle)
- All `StateFlow` management

---

## Moving types (optional)

`ActionOutcome`, `TargetResult`, `DamageBreakdown`, `BattleEvent`, and `BattleActor` are defined in `BattleState.kt`. They can stay there since `BattleState` already represents the output of the battle engine. No need to move them.

---

## Imports needed in BattleEngine.kt

```kotlin
import com.example.game.model.*
import com.example.game.model.BattleState.*
import com.example.game.model.Skill.*
import com.example.game.model.StatusEffect.*
import kotlin.math.*
import kotlin.random.Random
```

---

## Verification

1. Build must pass: `./gradlew :app:assembleDebug`
2. Play a full battle — all damage, healing, shielding, status, combo, and phase trigger behavior must match existing behavior exactly
3. No behavioral changes — this is a pure refactoring
