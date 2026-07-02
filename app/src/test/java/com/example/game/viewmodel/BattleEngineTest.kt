package com.example.game.viewmodel

import com.example.game.model.*
import org.junit.Assert.*
import org.junit.Test

class BattleEngineTest {

    private val dummySkill = Skill(
        id = "dummy", name = "Dummy", description = "",
        targetType = TargetType.SINGLE_ENEMY
    )

    private val dummyUltimate = Skill(
        id = "ult", name = "Ultimate", description = "",
        targetType = TargetType.SINGLE_ENEMY, baseDamage = 300
    )

    private fun makeHeroInstance(
        id: String = "test_hero",
        name: String = "Test Hero",
        element: Element = Element.NEUTRAL,
        baseHp: Int = 500, baseAtk: Int = 100, baseSpd: Int = 80,
        level: Int = 1
    ): HeroInstance {
        val hero = Hero(
            id = id, name = name, description = "",
            element = element, role = HeroRole.DPS,
            baseHp = baseHp, baseAtk = baseAtk, baseSpd = baseSpd,
            unlockYogaLevel = 1,
            skills = listOf(dummySkill), ultimate = dummyUltimate
        )
        return hero.createInstance(level)
    }

    private fun makeMonsterInstance(
        id: String = "test_monster",
        name: String = "Test Monster",
        element: Element = Element.NEUTRAL,
        baseHp: Int = 1000, baseAtk: Int = 50, baseSpd: Int = 50,
        phases: List<MonsterPhase> = listOf(MonsterPhase(1f, emptyList()))
    ): MonsterInstance {
        val monster = Monster(
            id = id, name = name, englishName = name,
            element = element, baseHp = baseHp, baseAtk = baseAtk, baseSpd = baseSpd,
            specialAttack = dummySkill, mechanicDescription = "",
            aiBehavior = AIBehavior(), difficultyTier = DifficultyTier.EASY,
            phases = phases, isBoss = false
        )
        return monster.createInstance()
    }

    @Test
    fun `computeDamage_basic_formula`() {
        val result = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL
        )
        assertEquals(200, result.amount)
    }

    @Test
    fun `computeDamage_minimumIsOne`() {
        val result = BattleEngine.computeDamage(
            baseDamage = 0, attackerAtk = 0,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL
        )
        assertTrue(result.amount >= 1)
    }

    @Test
    fun `computeDamage_weaknessOnePointFiveX`() {
        val neutral = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.FIRE, defenderElement = Element.NEUTRAL
        )
        val weak = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.FIRE, defenderElement = Element.AIR
        )
        assertEquals(1.5f, weak.amount.toFloat() / neutral.amount.toFloat(), 0.01f)
    }

    @Test
    fun `computeDamage_resistanceHalvesDamage`() {
        val neutral = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.FIRE, defenderElement = Element.NEUTRAL
        )
        val resisted = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.FIRE, defenderElement = Element.WATER
        )
        assertEquals(0.5f, resisted.amount.toFloat() / neutral.amount.toFloat(), 0.01f)
    }

    @Test
    fun `computeDamage_allElementPairingsNeverNegative`() {
        for (atkEl in Element.values()) {
            for (defEl in Element.values()) {
                val result = BattleEngine.computeDamage(
                    baseDamage = 100, attackerAtk = 100,
                    attackerElement = atkEl, defenderElement = defEl
                )
                assertTrue("$atkEl -> $defEl produced ${result.amount}", result.amount >= 1)
            }
        }
    }

    @Test
    fun `computeDamage_critMultipliedByOnePointFive`() {
        val nonCrit = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL,
            isCrit = false
        )
        val crit = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL,
            isCrit = true
        )
        assertTrue(crit.isCrit)
        assertFalse(nonCrit.isCrit)
        assertEquals(300, crit.amount)
        assertEquals(200, nonCrit.amount)
    }

    @Test
    fun `computeDamage_dodge_returnsZeroDamage`() {
        val result = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL,
            isDodged = true
        )
        assertTrue(result.isDodged)
        assertEquals(0, result.amount)
    }

    @Test
    fun `computeDamage_atkBuffMultiplierIncreasesDamage`() {
        val noBuff = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL,
            atkBuffMultiplier = 0f
        )
        val withBuff = BattleEngine.computeDamage(
            baseDamage = 100, attackerAtk = 100,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL,
            atkBuffMultiplier = 0.5f
        )
        assertEquals(200, noBuff.amount)
        assertEquals(300, withBuff.amount)
    }

    @Test
    fun `computeHeal_scalesWithMaxHp`() {
        val scaling = HealScaling(baseHeal = 60, healPerLevel = 0, isPercentage = true)
        val low = BattleEngine.computeHeal(100, scaling)
        val high = BattleEngine.computeHeal(1000, scaling)
        assertTrue(high > low)
    }

    @Test
    fun `computeHeal_flatHeal_usesBaseAndLevel`() {
        val scaling = HealScaling(baseHeal = 50, healPerLevel = 10, isPercentage = false)
        val result = BattleEngine.computeHeal(0, scaling, level = 5)
        assertEquals(100, result)
    }

    @Test
    fun `computeShield_scalesWithMaxHp`() {
        val scaling = ShieldScaling(baseShield = 0, shieldPerLevel = 0, isPercentage = true, percentage = 0.5f)
        val low = BattleEngine.computeShield(100, scaling)
        val high = BattleEngine.computeShield(1000, scaling)
        assertTrue(high > low)
    }

    @Test
    fun `computeShield_flatShield_usesBaseAndLevel`() {
        val scaling = ShieldScaling(baseShield = 30, shieldPerLevel = 5, isPercentage = false)
        val result = BattleEngine.computeShield(0, scaling, level = 3)
        assertEquals(45, result)
    }

    @Test
    fun `applyOutcome_shieldAbsorbsBeforeHP`() {
        val hero = makeHeroInstance(id = "Shanti", baseHp = 500).apply { shield = 100 }
        val state = BattleState(heroes = listOf(hero), monsters = listOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(damage = 60))
        )
        val (newState, _) = BattleEngine.applyOutcome(state, outcome)
        val updatedHero = newState.heroes.first()
        assertEquals(hero.maxHp, updatedHero.currentHp)
        assertEquals(40, updatedHero.shield)
    }

    @Test
    fun `applyOutcome_overkill_damagesHPAfterShieldDepleted`() {
        val hero = makeHeroInstance(id = "Shanti", baseHp = 500).apply { shield = 50 }
        val state = BattleState(heroes = listOf(hero), monsters = listOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(damage = 100))
        )
        val (newState, _) = BattleEngine.applyOutcome(state, outcome)
        val updatedHero = newState.heroes.first()
        assertEquals(0, updatedHero.shield)
        assertEquals(hero.maxHp - 50, updatedHero.currentHp)
    }

    @Test
    fun `applyOutcome_death_whenHPReachesZero`() {
        val hero = makeHeroInstance(id = "Shanti", baseHp = 500).apply { currentHp = 30 }
        val state = BattleState(heroes = listOf(hero), monsters = listOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(damage = 100))
        )
        val (newState, events) = BattleEngine.applyOutcome(state, outcome)
        val updatedHero = newState.heroes.first()
        assertEquals(0, updatedHero.currentHp)
        assertTrue(updatedHero.isDead)
        assertTrue(events.any { it is BattleEvent.HeroDown })
    }

    @Test
    fun `applyOutcome_heal_clampedToMaxHp`() {
        val hero = makeHeroInstance(id = "Shanti", baseHp = 500).apply { currentHp = maxHp - 10 }
        val state = BattleState(heroes = listOf(hero), monsters = listOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(heal = 1000))
        )
        val (newState, _) = BattleEngine.applyOutcome(state, outcome)
        val updatedHero = newState.heroes.first()
        assertEquals(hero.maxHp, updatedHero.currentHp)
    }

    @Test
    fun `applyOutcome_statusEffectApplied`() {
        val skill = Skill(
            id = "test_skill", name = "Test", description = "",
            targetType = TargetType.SINGLE_ENEMY,
            statusEffects = listOf(StatusEffectInfliction(StatusEffectType.ATK_DOWN, 1f, 3))
        )
        val hero = makeHeroInstance(id = "Shanti")
        val state = BattleState(heroes = listOf(hero), monsters = listOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti", skillUsed = skill,
            perTargetResult = mapOf("Shanti" to TargetResult(statuses = listOf("ATK_DOWN")))
        )
        val (newState, _) = BattleEngine.applyOutcome(state, outcome)
        assertTrue(newState.statusEffects.containsKey("Shanti"))
        assertEquals(StatusEffectType.ATK_DOWN, newState.statusEffects["Shanti"]?.first()?.statusType)
    }

    @Test
    fun `applyOutcome_cleanse_removesDebuffs`() {
        val hero = makeHeroInstance(id = "Shanti")
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(),
            statusEffects = mapOf("Shanti" to listOf(
                BattleStatus("Shanti", StatusEffectType.ATK_DOWN, 3, 0.2f)
            ))
        )
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(cleansed = true))
        )
        val (newState, _) = BattleEngine.applyOutcome(state, outcome)
        assertFalse(newState.statusEffects.containsKey("Shanti"))
    }

    @Test
    fun `applyOutcome_revive_bringsBackDeadHero`() {
        val reviveSkill = Skill(
            id = "test_revive", name = "Revive", description = "",
            targetType = TargetType.SINGLE_ALLY,
            healScaling = HealScaling(baseHeal = 50, healPerLevel = 0, isPercentage = true),
            revive = true
        )
        val hero = makeHeroInstance(id = "Shanti", baseHp = 500).apply { currentHp = 0; isDead = true }
        val state = BattleState(heroes = listOf(hero), monsters = listOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Maitri", skillUsed = reviveSkill,
            perTargetResult = mapOf("Shanti" to TargetResult(heal = 300))
        )
        val (newState, _) = BattleEngine.applyOutcome(state, outcome)
        val revived = newState.heroes.first()
        assertFalse(revived.isDead)
        assertTrue(revived.currentHp > 0)
    }

    @Test
    fun `calculateTurnOrder_higherSpdFirst`() {
        val fast = makeHeroInstance(id = "Shanti", baseSpd = 200)
        val slow = makeHeroInstance(id = "Virya", baseSpd = 10)
        val order = BattleEngine.calculateTurnOrder(listOf(fast, slow), emptyList())
        assertEquals("Shanti", order[0].id)
        assertEquals("Virya", order[1].id)
    }

    @Test
    fun `calculateTurnOrder_variance_preventsDeterministicTies`() {
        val a = makeHeroInstance(id = "Shanti", baseSpd = 100)
        val b = makeHeroInstance(id = "Virya", baseSpd = 100)
        val orders = (1..50).map {
            BattleEngine.calculateTurnOrder(listOf(a, b), emptyList())
        }
        val allAFirst = orders.all { it[0].id == "Shanti" }
        assertFalse(allAFirst)
    }

    @Test
    fun `calculateTurnOrder_intermixHeroesAndMonsters`() {
        val hero = makeHeroInstance(id = "Hero", baseSpd = 80)
        val monster = makeMonsterInstance(id = "Monster", baseSpd = 120)
        val order = BattleEngine.calculateTurnOrder(listOf(hero), listOf(monster))
        assertFalse(order[0].isHero)
        assertTrue(order[1].isHero)
    }

    @Test
    fun `resolveTargets_singleAlly_returnsCaster`() {
        val skill = Skill(id = "test", name = "Test", description = "",
            targetType = TargetType.SINGLE_ALLY)
        val hero = makeHeroInstance(id = "Shanti")
        val state = BattleState(heroes = listOf(hero), monsters = listOf())
        val targets = BattleEngine.resolveTargets(skill, "Shanti", state)
        assertEquals(listOf("Shanti"), targets)
    }

    @Test
    fun `resolveTargets_singleEnemy_returnsFirstLivingMonster`() {
        val skill = Skill(id = "test", name = "Test", description = "",
            targetType = TargetType.SINGLE_ENEMY)
        val hero = makeHeroInstance(id = "Shanti")
        val monster = makeMonsterInstance(id = "Bhaya")
        val state = BattleState(heroes = listOf(hero), monsters = listOf(monster))
        val targets = BattleEngine.resolveTargets(skill, "Shanti", state)
        assertEquals(1, targets.size)
        assertEquals("Bhaya", targets[0])
    }

    @Test
    fun `resolveTargets_allAllies_returnsAllLiving`() {
        val skill = Skill(id = "test", name = "Test", description = "",
            targetType = TargetType.ALL_ALLIES)
        val alive1 = makeHeroInstance(id = "Shanti")
        val alive2 = makeHeroInstance(id = "Virya")
        val alive3 = makeHeroInstance(id = "Santosha")
        val dead1 = makeHeroInstance(id = "DeadHero").apply { currentHp = 0; isDead = true }
        val state = BattleState(
            heroes = listOf(alive1, alive2, alive3, dead1),
            monsters = listOf()
        )
        val targets = BattleEngine.resolveTargets(skill, "Shanti", state)
        assertEquals(3, targets.size)
        assertTrue(targets.containsAll(listOf("Shanti", "Virya", "Santosha")))
        assertFalse(targets.contains("DeadHero"))
    }

    @Test
    fun `resolveTargets_allEnemies_returnsAllMonsters`() {
        val skill = Skill(id = "test", name = "Test", description = "",
            targetType = TargetType.ALL_ENEMIES)
        val hero = makeHeroInstance(id = "Shanti")
        val monster = makeMonsterInstance(id = "Bhaya")
        val state = BattleState(heroes = listOf(hero), monsters = listOf(monster))
        val targets = BattleEngine.resolveTargets(skill, "Shanti", state)
        assertEquals(1, targets.size)
        assertEquals("Bhaya", targets[0])
    }

    @Test
    fun `comboSkill_twoHero_outcome`() {
        val combo = ComboSkill(
            id = "test_combo", name = "Test Combo", description = "",
            requiredHeroes = setOf("HeroA", "HeroB"),
            targetType = TargetType.SINGLE_ENEMY,
            damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
            baseDamage = 200,
            comboType = ComboType.TWO_HERO
        )
        val h1 = makeHeroInstance(id = "HeroA", baseHp = 500, level = 5)
        val h2 = makeHeroInstance(id = "HeroB", baseHp = 500, level = 5)
        val monster = makeMonsterInstance(id = "Enemy")
        val state = BattleState(heroes = listOf(h1, h2), monsters = listOf(monster))
        val result = BattleEngine.computeComboOutcome(
            combo = combo,
            casterId = "HeroA",
            partnerIds = listOf("HeroB"),
            state = state
        )
        assertNotNull(result.outcome)
        assertTrue(result.outcome.damageDealt > 0)
    }

    @Test
    fun `comboSkill_threeHero_outcome`() {
        val combo = ComboSkill(
            id = "test_combo3", name = "Test Combo 3", description = "",
            requiredHeroes = setOf("HeroA", "HeroB", "HeroC"),
            targetType = TargetType.SINGLE_ENEMY,
            damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
            baseDamage = 300,
            comboType = ComboType.THREE_HERO
        )
        val h1 = makeHeroInstance(id = "HeroA", level = 5)
        val h2 = makeHeroInstance(id = "HeroB", level = 5)
        val h3 = makeHeroInstance(id = "HeroC", level = 5)
        val monster = makeMonsterInstance(id = "Enemy")
        val state = BattleState(heroes = listOf(h1, h2, h3), monsters = listOf(monster))
        val result = BattleEngine.computeComboOutcome(
            combo = combo,
            casterId = "HeroA",
            partnerIds = listOf("HeroB", "HeroC"),
            state = state
        )
        assertNotNull(result.outcome)
    }

    @Test
    fun `comboSkill_healing_restoresHP`() {
        val combo = ComboSkill(
            id = "heal_combo", name = "Heal Combo", description = "",
            requiredHeroes = setOf("HeroA", "HeroB"),
            targetType = TargetType.ALL_ALLIES,
            healScaling = HealScaling(baseHeal = 30, healPerLevel = 0, isPercentage = true),
            comboType = ComboType.TWO_HERO
        )
        val h1 = makeHeroInstance(id = "HeroA", baseHp = 1000, level = 5)
        val h2 = makeHeroInstance(id = "HeroB", baseHp = 1000, level = 5)
        val preHp = h1.currentHp
        h1.currentHp = (h1.currentHp - 200).coerceAtLeast(1)
        val damagedHp = h1.currentHp
        val state = BattleState(
            heroes = listOf(h1, h2),
            monsters = listOf()
        )
        val result = BattleEngine.computeComboOutcome(
            combo = combo,
            casterId = "HeroA",
            partnerIds = listOf("HeroB"),
            state = state
        )
        val (newState, _) = BattleEngine.applyOutcome(state, result.outcome)
        val updatedHero = newState.heroes.first { it.heroId == "HeroA" }
        assertTrue(updatedHero.currentHp > damagedHp)
    }

    @Test
    fun `monsterPhase_gainShield_addsShield`() {
        val phases = listOf(
            MonsterPhase(0.5f, listOf(
                PhaseTrigger(PhaseTriggerType.GAIN_SHIELD, value = 0.3f)
            ))
        )
        val monster = makeMonsterInstance(
            id = "PhaseMonster", baseHp = 1000,
            phases = phases
        ).apply {
            currentHp = (maxHp * 0.45f).toInt()
            shield = 0
        }
        val preShield = monster.shield
        val (updatedMonster, _) = BattleEngine.checkPhaseTriggers(monster)
        assertTrue(updatedMonster.shield > preShield)
    }

    @Test
    fun `chooseMonsterTarget_lowestHP_targetsLowest`() {
        val heroes = listOf(
            makeHeroInstance(id = "Shanti").apply { currentHp = 100 },
            makeHeroInstance(id = "Virya").apply { currentHp = 50 },
            makeHeroInstance(id = "Santosha").apply { currentHp = 200 }
        )
        val target = BattleEngine.chooseMonsterTarget(heroes, TargetStrategy.LOWEST_HP)
        assertEquals("Virya", target)
    }

    @Test
    fun `chooseMonsterTarget_highestHP_targetsHighest`() {
        val heroes = listOf(
            makeHeroInstance(id = "Shanti").apply { currentHp = 100 },
            makeHeroInstance(id = "Virya").apply { currentHp = 50 },
            makeHeroInstance(id = "Santosha").apply { currentHp = 200 }
        )
        val target = BattleEngine.chooseMonsterTarget(heroes, TargetStrategy.HIGHEST_HP)
        assertEquals("Santosha", target)
    }

    @Test
    fun `chooseMonsterTarget_random_selectsLivingHero`() {
        val heroes = listOf(
            makeHeroInstance(id = "Shanti").apply { currentHp = 0; isDead = true },
            makeHeroInstance(id = "Virya").apply { currentHp = 100; isDead = false }
        )
        val targets = (1..20).map {
            BattleEngine.chooseMonsterTarget(heroes, TargetStrategy.RANDOM)
        }
        assertTrue(targets.all { it == "Virya" })
    }

    @Test
    fun `resolveTargets_excludesDeadHeroes`() {
        val skill = Skill(id = "test", name = "Test", description = "",
            targetType = TargetType.ALL_ALLIES)
        val alive = makeHeroInstance(id = "Shanti")
        val dead = makeHeroInstance(id = "Virya").apply { currentHp = 0; isDead = true }
        val state = BattleState(heroes = listOf(alive, dead), monsters = listOf())
        val targets = BattleEngine.resolveTargets(skill, "Shanti", state)
        assertEquals(listOf("Shanti"), targets)
        assertFalse(targets.contains("Virya"))
    }

    @Test
    fun `calculateTurnOrder_excludesDeadMonsters`() {
        val hero = makeHeroInstance(id = "Hero", baseSpd = 100)
        val aliveMonster = makeMonsterInstance(id = "AliveMonster", baseSpd = 200)
        val deadMonster = makeMonsterInstance(id = "DeadMonster", baseSpd = 999).apply {
            currentHp = 0; isDead = true
        }
        val order = BattleEngine.calculateTurnOrder(
            listOf(hero),
            listOf(aliveMonster, deadMonster)
        )
        assertEquals(2, order.size)
        assertTrue(order.any { it.id == "AliveMonster" })
        assertFalse(order.any { it.id == "DeadMonster" })
    }

    @Test
    fun `applyOutcome_zeroDamage_noDeathEvents`() {
        val hero = makeHeroInstance(id = "Shanti")
        val state = BattleState(heroes = listOf(hero), monsters = listOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = emptyMap()
        )
        val (newState, _) = BattleEngine.applyOutcome(state, outcome)
        assertEquals(hero.currentHp, newState.heroes.first().currentHp)
    }
}
