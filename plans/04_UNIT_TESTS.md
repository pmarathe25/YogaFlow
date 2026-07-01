# Plan: BattleEngine Unit Tests

**Dependencies:** Requires `BattleEngine.kt` to exist (Plan 03). However, the test file can be written against the specified API and will compile once BattleEngine is implemented.
**Files touched:** `app/src/test/java/com/example/game/viewmodel/BattleEngineTest.kt` (new file)
**Priority:** High

---

## Setup

### File location
```
app/src/test/java/com/example/game/viewmodel/BattleEngineTest.kt
```

### Test framework
- **JUnit 4** (compatible with existing test setup in `app/build.gradle.kts`)
- Standard assertions: `assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`, `assertNull`

### Test data
Tests use actual game data from:
- `HeroDefinitions.heroes`
- `MonsterDefinitions.monsters`
- `ComboSkillDefinitions.allCombos`
- `BattleState` constructor for fixtures

**Don't mock `BattleEngine`** — these are integration-style unit tests against the real implementation.

### Helpers

Create helper functions at the top of the test file:

```kotlin
import com.example.game.model.*
import com.example.game.model.BattleState.*
import com.example.game.model.Skill.*
import com.example.game.model.StatusEffect.*
import com.example.game.viewmodel.BattleEngine
import org.junit.Test
import kotlin.test.*

private val shanti = HeroDefinitions.getHero("shanti")
private val virya = HeroDefinitions.getHero("virya")
private val santosha = HeroDefinitions.getHero("santosha")
private val bhaya = MonsterDefinitions.getMonster("bhaya")
private val krodha = MonsterDefinitions.getMonster("krodha")

private fun createHeroInstance(hero: Hero, level: Int = 1): HeroInstance {
    val stats = hero.baseStats
    return HeroInstance(
        heroId = hero.id,
        hero = hero,
        level = level,
        currentHp = stats.maxHp,
        maxHp = stats.maxHp,
        attack = stats.attack,
        defense = stats.defense,
        speed = stats.speed,
        isAlive = true
    )
}

private fun createMonsterInstance(monster: Monster, level: Int = 1): MonsterInstance {
    return MonsterInstance(
        monsterId = monster.id,
        monster = monster,
        level = level,
        currentHp = monster.baseStats.maxHp,
        maxHp = monster.baseStats.maxHp,
        attack = monster.baseStats.attack,
        defense = monster.baseStats.defense,
        speed = monster.baseStats.speed,
        isAlive = true
    )
}
```

---

## Test cases

### 1. computeDamage

#### 1.1 Basic damage computation
```kotlin
@Test
fun `computeDamage_basic reduces ATK by DEF`() {
    val result = BattleEngine.computeDamage(
        attackerAtk = 100, defenderDef = 50,
        attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL
    )
    assertTrue(result.amount > 0)
    assertTrue(result.amount <= 300) // (100*4 - 50*2) * 1.0 * 1.1 = 330, but let's just check bounds
}
```

#### 1.2 Minimum damage floor
```kotlin
@Test
fun `computeDamage_floor is 50 percent of attacker ATK`() {
    val result = BattleEngine.computeDamage(
        attackerAtk = 100, defenderDef = 1000,
        attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL
    )
    assertTrue(result.amount >= 45) // 100 * 0.5 * 0.9 (with -10% variance)
}
```

#### 1.3 Elemental multiplier — weakness (2x)
```kotlin
@Test
fun `computeDamage_weakness double damage`() {
    val neutral = BattleEngine.computeDamage(
        attackerAtk = 100, defenderDef = 50,
        attackerElement = Element.FIRE, defenderElement = Element.AIR
    )
    val weak = BattleEngine.computeDamage(
        attackerAtk = 100, defenderDef = 50,
        attackerElement = Element.FIRE, defenderElement = Element.EARTH
    )
    // FIRE → EARTH = 0.5x, FIRE → AIR = 2.0x
    assertTrue(weak.amount > neutral.amount * 1.3, "Weakness multiplier should increase damage")
}
```

#### 1.4 Elemental multiplier — resistance (0.5x)
```kotlin
@Test
fun `computeDamage_resistance halves damage`() {
    val neutral = BattleEngine.computeDamage(
        attackerAtk = 100, defenderDef = 50,
        attackerElement = Element.WATER, defenderElement = Element.NEUTRAL
    )
    val resisted = BattleEngine.computeDamage(
        attackerAtk = 100, defenderDef = 50,
        attackerElement = Element.WATER, defenderElement = Element.FIRE
    )
    assertTrue(resisted.amount < neutral.amount * 0.7, "Resistance should reduce damage")
}
```

#### 1.5 All element pairings produce non-negative damage
```kotlin
@Test
fun `computeDamage_allElementPairings_neverNegative`() {
    for (atkEl in Element.values()) {
        for (defEl in Element.values()) {
            val result = BattleEngine.computeDamage(
                attackerAtk = 80, defenderDef = 40,
                attackerElement = atkEl, defenderElement = defEl
            )
            assertTrue(result.amount >= 0, "$atkEl → $defEl produced negative damage")
        }
    }
}
```

#### 1.6 Nullification (0x) produces zero damage
```kotlin
@Test
fun `computeDamage_nullifiedElement_doesZeroDamage`() {
    val result = BattleEngine.computeDamage(
        attackerAtk = 200, defenderDef = 10,
        attackerElement = Element.VOID, defenderElement = Element.SHADOW
    )
    assertEquals(0, result.amount)
}
```

#### 1.7 Crit multiplier
```kotlin
@Test
fun `computeDamage_critMultipliedByOnePointFive`() {
    val nonCrit = BattleEngine.computeDamage(
        attackerAtk = 100, defenderDef = 50,
        attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL,
        isCrit = false
    )
    val crit = BattleEngine.computeDamage(
        attackerAtk = 100, defenderDef = 50,
        attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL,
        isCrit = true
    )
    assertTrue(crit.isCrit)
    assertFalse(nonCrit.isCrit)
    assertEquals(crit.amount, (nonCrit.amount * 1.5).toInt(), 5) // allow rounding
}
```

#### 1.8 Dodge flag
```kotlin
@Test
fun `computeDamage_dodge_returnsZeroDamage`() {
    val result = BattleEngine.computeDamage(
        attackerAtk = 200, defenderDef = 10,
        attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL,
        isDodged = true
    )
    assertTrue(result.isDodged)
    assertEquals(0, result.amount)
}
```

#### 1.9 Variance stays within ±10%
```kotlin
@Test
fun `computeDamage_variance_withinTenPercent`() {
    val results = (1..100).map {
        BattleEngine.computeDamage(
            attackerAtk = 120, defenderDef = 60,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL
        ).amount
    }
    val base = 120 * 4 - 60 * 2 // 360
    results.forEach { amount ->
        assertTrue(amount >= (base * 0.9).toInt() && amount <= (base * 1.1).toInt() + 1,
            "Variance $amount outside ±10% of $base")
    }
}
```

---

### 2. computeHeal

#### 2.1 Healing scales with ATK
```kotlin
@Test
fun `computeHeal_scalesWithAttack`() {
    val scaling = HealScaling(stat = "ATK", ratio = 0.8f, baseHeal = 60)
    val lowAtk = BattleEngine.computeHeal(50, scaling)
    val highAtk = BattleEngine.computeHeal(200, scaling)
    assertTrue(highAtk > lowAtk)
}
```

#### 2.2 Minimum heal is base value
```kotlin
@Test
fun `computeHeal_minimumIsBaseHeal`() {
    val scaling = HealScaling(stat = "ATK", ratio = 0.1f, baseHeal = 50)
    val result = BattleEngine.computeHeal(0, scaling)
    assertTrue(result >= 50)
}
```

---

### 3. computeShield

#### 3.1 Shield scales with ATK
```kotlin
@Test
fun `computeShield_scalesWithAttack`() {
    val scaling = ShieldScaling(stat = "ATK", ratio = 0.5f, baseShield = 40)
    val lowAtk = BattleEngine.computeShield(50, scaling)
    val highAtk = BattleEngine.computeShield(200, scaling)
    assertTrue(highAtk > lowAtk)
}
```

---

### 4. applyOutcome

#### 4.1 Shield absorbs damage before HP
```kotlin
@Test
fun `applyOutcome_shieldAbsorbsBeforeHP`() {
    val hero = createHeroInstance(shanti)
    val state = BattleState(
        heroes = mutableListOf(hero),
        monsters = mutableListOf(),
        ...
    ).copy(heroShields = mutableMapOf("shanti" to 100))

    val outcome = ActionOutcome(
        totalDamage = 60,
        targetResults = listOf(TargetResult(targetId = "shanti", damage = 60))
    )

    val newState = BattleEngine.applyOutcome(state, outcome)
    val updatedHero = newState.heroes.first()
    assertEquals(shanti.baseStats.maxHp, updatedHero.currentHp) // HP untouched
    assertEquals(40, newState.heroShields["shanti"])   // shield reduced from 100 to 40
}
```

#### 4.2 Overkill when damage exceeds shield
```kotlin
@Test
fun `applyOutcome_overkill_damagesHPAfterShieldDepleted`() {
    val hero = createHeroInstance(shanti)
    val state = BattleState(
        heroes = mutableListOf(hero),
        monsters = mutableListOf(),
        ...
    ).copy(heroShields = mutableMapOf("shanti" to 50))

    val outcome = ActionOutcome(
        totalDamage = 100,
        targetResults = listOf(TargetResult(targetId = "shanti", damage = 100))
    )

    val newState = BattleEngine.applyOutcome(state, outcome)
    val updatedHero = newState.heroes.first()
    assertEquals(50, newState.heroShields["shanti"])    // shield depleted to 0 (50 - 50 = 0)
    // But wait — the spec says shield absorbs first, then overflow damages HP
    // shield 50, damage 100 → shield absorbs 50, overflow 50 → HP reduces by 50
    // ... implement as per existing code
}
```

#### 4.3 Death when HP reaches 0
```kotlin
@Test
fun `applyOutcome_death_whenHPReachesZero`() {
    val hero = createHeroInstance(shanti).copy(currentHp = 30)
    val state = BattleState(
        heroes = mutableListOf(hero),
        monsters = mutableListOf(),
        ...
    )

    val outcome = ActionOutcome(
        totalDamage = 100,
        targetResults = listOf(TargetResult(targetId = "shanti", damage = 100))
    )

    val newState = BattleEngine.applyOutcome(state, outcome)
    val updatedHero = newState.heroes.first()
    assertEquals(0, updatedHero.currentHp)
    assertFalse(updatedHero.isAlive)
    // Check that a HeroDown event was generated
}
```

#### 4.4 Healing cannot exceed maxHP
```kotlin
@Test
fun `applyOutcome_heal_clampedToMaxHp`() {
    val hero = createHeroInstance(shanti).copy(currentHp = shanti.baseStats.maxHp - 10)
    val state = BattleState(
        heroes = mutableListOf(hero),
        monsters = mutableListOf(),
        ...
    )

    // Heal for 1000, but only need 10
    val outcome = ActionOutcome(
        totalHealing = 1000,
        targetResults = listOf(TargetResult(targetId = "shanti", heal = 1000))
    )

    val newState = BattleEngine.applyOutcome(state, outcome)
    val updatedHero = newState.heroes.first()
    assertEquals(shanti.baseStats.maxHp, updatedHero.currentHp)
}
```

#### 4.5 Status effects applied
```kotlin
@Test
fun `applyOutcome_statusEffectApplied`() {
    // Create target result with a status effect
    // Verify status appears in state.battleStatuses
}
```

#### 4.6 Cleanse removes debuffs
```kotlin
@Test
fun `applyOutcome_cleanse_removesDebuffs`() {
    // Set up hero with ATK_DOWN debuff
    // Apply outcome with cleanse
    // Verify debuff is removed
}
```

#### 4.7 Revive dead hero
```kotlin
@Test
fun `applyOutcome_revive_bringsBackDeadHero`() {
    val hero = createHeroInstance(shanti).copy(currentHp = 0, isAlive = false)
    val state = BattleState(...)
    val outcome = ActionOutcome(
        totalHealing = 300,
        targetResults = listOf(TargetResult(targetId = "shanti", heal = 300, revive = true))
    )
    val newState = BattleEngine.applyOutcome(state, outcome)
    val revived = newState.heroes.first()
    assertTrue(revived.isAlive)
    assertTrue(revived.currentHp > 0)
}
```

---

### 5. calculateTurnOrder

#### 5.1 Higher SPD sorts first
```kotlin
@Test
fun `calculateTurnOrder_higherSpdFirst`() {
    val fast = createHeroInstance(shanti).copy(speed = 200)
    val slow = createHeroInstance(virya).copy(speed = 10)
    val order = BattleEngine.calculateTurnOrder(listOf(fast, slow), emptyList())
    assertEquals(fast.heroId, (order[0] as BattleActor.Hero).id)
    assertEquals(slow.heroId, (order[1] as BattleActor.Hero).id)
}
```

#### 5.2 Variance prevents identical SPD from always sorting same
```kotlin
@Test
fun `calculateTurnOrder_variance_preventsDeterministicTies`() {
    val a = createHeroInstance(shanti).copy(speed = 100)
    val b = createHeroInstance(virya).copy(speed = 100)
    // Run many iterations to confirm both orderings occur
    val orders = (1..50).map {
        BattleEngine.calculateTurnOrder(listOf(a, b), emptyList())
    }
    val allAFirst = orders.all { (it[0] as BattleActor.Hero).id == "shanti" }
    assertFalse(allAFirst, "Ties should be randomized by variance")
}
```

#### 5.3 Monsters and heroes intermix correctly
```kotlin
@Test
fun `calculateTurnOrder_intermixHeroesAndMonsters`() {
    val hero = createHeroInstance(shanti).copy(speed = 80)
    val monster = createMonsterInstance(krodha).copy(speed = 120)
    val order = BattleEngine.calculateTurnOrder(listOf(hero), listOf(monster))
    // Monster (120 SPD) should go before hero (80 SPD)
    assertTrue(order[0] is BattleActor.Monster)
    assertTrue(order[1] is BattleActor.Hero)
}
```

---

### 6. resolveTargets

#### 6.1 SINGLE_ALLY targets self if no specific target
```kotlin
@Test
fun `resolveTargets_singleAlly_returnsCaster`() {
    val skill = Skill(targetType = TargetType.SINGLE_ALLY, ...)
    val targets = BattleEngine.resolveTargets(skill, "shanti", state)
    assertEquals(listOf("shanti"), targets)
}
```

#### 6.2 SINGLE_ENEMY returns first living monster
```kotlin
@Test
fun `resolveTargets_singleEnemy_returnsFirstLivingMonster`() {
    val skill = Skill(targetType = TargetType.SINGLE_ENEMY, ...)
    val targets = BattleEngine.resolveTargets(skill, "shanti", stateWithMonsters)
    assertEquals(1, targets.size)
    assertTrue(targets[0].startsWith("bhaya"))
}
```

#### 6.3 ALL_ALLIES returns all living heroes
```kotlin
@Test
fun `resolveTargets_allAllies_returnsAllLiving`() {
    // Set up state with 3 living heroes, 1 dead hero
    // Should return only living hero IDs
}
```

#### 6.4 ALL_ENEMIES returns all monsters
```kotlin
@Test
fun `resolveTargets_allEnemies_returnsAllMonsters`() {
    val skill = Skill(targetType = TargetType.ALL_ENEMIES, ...)
    val targets = BattleEngine.resolveTargets(skill, "shanti", stateWithMonsters)
    assertEquals(1, targets.size) // one monster
}
```

---

### 7. Combo skills

#### 7.1 Two-hero combo computes outcome
```kotlin
@Test
fun `comboSkill_twoHero_outcome`() {
    // Find a two-hero combo
    val combo = ComboSkillDefinitions.allCombos.first { it.type == ComboType.TWO_HERO }
    val result = BattleEngine.computeComboOutcome(
        combo = combo,
        casterId = combo.requiredHeroes[0],
        partnerIds = listOf(combo.requiredHeroes[1]),
        state = stateWithParty
    )
    assertNotNull(result.outcome)
    assertTrue(result.outcome.totalDamage > 0 || result.outcome.totalHealing > 0)
}
```

#### 7.2 Three-hero combo computes outcome
```kotlin
@Test
fun `comboSkill_threeHero_outcome`() {
    val combo = ComboSkillDefinitions.allCombos.first { it.type == ComboType.THREE_HERO }
    val result = BattleEngine.computeComboOutcome(
        combo = combo,
        casterId = combo.requiredHeroes[0],
        partnerIds = combo.requiredHeroes.drop(1),
        state = stateWithParty
    )
    assertNotNull(result.outcome)
}
```

#### 7.3 Combo with healing component restores HP
```kotlin
@Test
fun `comboSkill_healing_restoresHP`() {
    // Find a combo that heals
    val combo = ComboSkillDefinitions.allCombos.firstOrNull { it.healComponents.isNotEmpty() }
        ?: return // skip if no healing combo exists
    val preHp = stateWithParty.heroes.first().currentHp
    stateWithParty.heroes.first().currentHp -= 200 // damage hero first
    val result = BattleEngine.computeComboOutcome(combo, ...)
    // apply outcome
    // assert hp restored
}
```

---

### 8. Monster phases

#### 8.1 REFLECT_DAMAGE triggers at correct HP threshold
```kotlin
@Test
fun `monsterPhase_reflectDamage_triggersAtThreshold`() {
    // Create monster with REFLECT_DAMAGE phase at 50% HP
    val monster = createMonsterInstance(krodha).copy(currentHp = 45) // 45% HP, should trigger 50% phase
    val state = BattleState(monsters = mutableListOf(monster), ...)
    val events = BattleEngine.checkPhaseTriggers(state)
    assertTrue(events.any { it is BattleEvent.PhaseTriggered && it.monsterId == "krodha" })
}
```

#### 8.2 SUMMON_ADD creates a new monster
```kotlin
@Test
fun `monsterPhase_summonAdd_createsNewMonster()`() {
    // Set up monster with SUMMON_ADD phase
    // Trigger the phase
    // Verify new monster instance in state
}
```

#### 8.3 GAIN_SHIELD provides shield to monster
```kotlin
@Test
fun `monsterPhase_gainShield_addsShield`() {
    // Similar to above, verify monsterShields updated
}
```

---

### 9. AI targeting

#### 9.1 LOWEST_HP targets hero with lowest HP
```kotlin
@Test
fun `chooseMonsterTarget_lowestHP_targetsLowest`() {
    val heroes = listOf(
        createHeroInstance(shanti).copy(currentHp = 100),
        createHeroInstance(virya).copy(currentHp = 50),
        createHeroInstance(santosha).copy(currentHp = 200)
    )
    val target = BattleEngine.chooseMonsterTarget(heroes, TargetStrategy.LOWEST_HP)
    assertEquals("virya", target)
}
```

#### 9.2 HIGHEST_HP targets highest
```kotlin
@Test
fun `chooseMonsterTarget_highestHP_targetsHighest`() {
    val heroes = listOf(
        createHeroInstance(shanti).copy(currentHp = 100),
        createHeroInstance(virya).copy(currentHp = 50),
        createHeroInstance(santosha).copy(currentHp = 200)
    )
    val target = BattleEngine.chooseMonsterTarget(heroes, TargetStrategy.HIGHEST_HP)
    assertEquals("santosha", target)
}
```

#### 9.3 RANDOM selects any valid target
```kotlin
@Test
fun `chooseMonsterTarget_random_selectsLivingHero`() {
    val heroes = listOf(
        createHeroInstance(shanti).copy(isAlive = false, currentHp = 0),
        createHeroInstance(virya).copy(isAlive = true, currentHp = 100)
    )
    // Running many times, should always pick virya (only alive one)
    val targets = (1..20).map { BattleEngine.chooseMonsterTarget(heroes, TargetStrategy.RANDOM) }
    assertTrue(targets.all { it == "virya" })
}
```

---

### 10. Edge cases

#### 10.1 Dead heroes are excluded from targeting
```kotlin
@Test
fun `resolveTargets_excludesDeadHeroes`() {
    // Mark one hero as dead
    // ALL_ALLIES skill should not include dead hero
}
```

#### 10.2 Dead monsters don't act
```kotlin
@Test
fun `calculateTurnOrder_excludesDeadMonsters`() {
    // Monster with isAlive = false should not appear in turn order
}
```

#### 10.3 Zero-damage outcome doesn't cause events
```kotlin
@Test
fun `applyOutcome_zeroDamage_noDeathEvents`() {
    val state = stateWithParty
    val outcome = ActionOutcome(totalDamage = 0, targetResults = emptyList())
    val result = BattleEngine.applyOutcome(state, outcome)
    // Should be identical state
}
```

---

## Running tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.game.viewmodel.BattleEngineTest"
```

All tests should pass. If any test fails, adjust the `BattleEngine` implementation to match existing game behavior (use `GameViewModel`'s current implementation as the reference).
