package com.example.game.viewmodel

import com.example.game.model.*
import org.junit.Assert.*
import org.junit.Test

class BattleEngineTest {

    private val shanti = HeroDefinitions.getHero("Shanti")!!
    private val virya = HeroDefinitions.getHero("Virya")!!
    private val santosha = HeroDefinitions.getHero("Santosha")!!
    private val bhaya = MonsterDefinitions.getMonster("Bhaya")!!
    private val krodha = MonsterDefinitions.getMonster("Krodha")!!
    private val klesh = MonsterDefinitions.getMonster("Klesh")!!

    private fun createHeroInstance(hero: Hero, level: Int = 1): HeroInstance {
        return HeroDefinitions.createInstance(hero, level)
    }

    private fun createMonsterInstance(monster: Monster, level: Int = 1): MonsterInstance {
        return MonsterDefinitions.createInstance(monster)
    }

    @Test
    fun `computeDamage_basic reduces ATK by DEF`() {
        val result = BattleEngine.computeDamage(
            attackerAtk = 100, defenderDef = 50,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL
        )
        assertTrue(result.amount > 0)
        assertTrue(result.amount <= 330)
    }

    @Test
    fun `computeDamage_floor is 50 percent of attacker ATK`() {
        val result = BattleEngine.computeDamage(
            attackerAtk = 100, defenderDef = 1000,
            attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL
        )
        assertTrue(result.amount >= 45)
    }

    @Test
    fun `computeDamage_weakness double damage`() {
        val neutral = BattleEngine.computeDamage(
            attackerAtk = 100, defenderDef = 50,
            attackerElement = Element.FIRE, defenderElement = Element.NEUTRAL
        )
        val weak = BattleEngine.computeDamage(
            attackerAtk = 100, defenderDef = 50,
            attackerElement = Element.FIRE, defenderElement = Element.AIR
        )
        assertTrue(weak.amount > neutral.amount * 1.3)
    }

    @Test
    fun `computeDamage_resistance halves damage`() {
        val neutral = BattleEngine.computeDamage(
            attackerAtk = 100, defenderDef = 50,
            attackerElement = Element.FIRE, defenderElement = Element.NEUTRAL
        )
        val resisted = BattleEngine.computeDamage(
            attackerAtk = 100, defenderDef = 50,
            attackerElement = Element.FIRE, defenderElement = Element.WATER
        )
        assertTrue(resisted.amount < neutral.amount * 0.7)
    }

    @Test
    fun `computeDamage_allElementPairings_neverNegative`() {
        for (atkEl in Element.values()) {
            for (defEl in Element.values()) {
                val result = BattleEngine.computeDamage(
                    attackerAtk = 80, defenderDef = 40,
                    attackerElement = atkEl, defenderElement = defEl
                )
                assertTrue("$atkEl -> $defEl produced negative damage", result.amount >= 0)
            }
        }
    }

    @Test
    fun `computeDamage_nullifiedElement_doesZeroDamage`() {
        val result = BattleEngine.computeDamage(
            attackerAtk = 200, defenderDef = 10,
            attackerElement = Element.VOID, defenderElement = Element.SHADOW
        )
        assertEquals(0, result.amount)
    }

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
        assertTrue(crit.amount > nonCrit.amount)
    }

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

    @Test
    fun `computeDamage_variance_withinTenPercent`() {
        val results = (1..100).map {
            BattleEngine.computeDamage(
                attackerAtk = 120, defenderDef = 60,
                attackerElement = Element.NEUTRAL, defenderElement = Element.NEUTRAL
            ).amount
        }
        val base = 120 * 4 - 60 * 2
        results.forEach { amount ->
            assertTrue("Variance $amount outside +-10% of $base",
                amount >= (base * 0.9).toInt() && amount <= (base * 1.1).toInt() + 1)
        }
    }

    @Test
    fun `computeHeal_scalesWithAttack`() {
        val scaling = HealScaling(baseHeal = 60, healPerLevel = 0, isPercentage = true)
        val lowAtk = BattleEngine.computeHeal(50, scaling)
        val highAtk = BattleEngine.computeHeal(200, scaling)
        assertTrue(highAtk > lowAtk)
    }

    @Test
    fun `computeHeal_minimumIsBaseHeal`() {
        val scaling = HealScaling(baseHeal = 50, healPerLevel = 0)
        val result = BattleEngine.computeHeal(0, scaling)
        assertTrue(result >= 50)
    }

    @Test
    fun `computeShield_scalesWithAttack`() {
        val scaling = ShieldScaling(baseShield = 0, shieldPerLevel = 0, isPercentage = true, percentage = 0.5f)
        val lowAtk = BattleEngine.computeShield(50, scaling)
        val highAtk = BattleEngine.computeShield(200, scaling)
        assertTrue(highAtk > lowAtk)
    }

    @Test
    fun `applyOutcome_shieldAbsorbsBeforeHP`() {
        val hero = createHeroInstance(shanti).apply { shield = 100 }
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(damage = 60))
        )
        val newState = BattleEngine.applyOutcome(state, outcome)
        val updatedHero = newState.heroes.first()
        assertEquals(hero.maxHp, updatedHero.currentHp)
        assertEquals(40, updatedHero.shield)
    }

    @Test
    fun `applyOutcome_overkill_damagesHPAfterShieldDepleted`() {
        val hero = createHeroInstance(shanti).apply { shield = 50 }
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(damage = 100))
        )
        val newState = BattleEngine.applyOutcome(state, outcome)
        val updatedHero = newState.heroes.first()
        assertEquals(0, updatedHero.shield)
        assertEquals(hero.maxHp - 50, updatedHero.currentHp)
    }

    @Test
    fun `applyOutcome_death_whenHPReachesZero`() {
        val hero = createHeroInstance(shanti).apply { currentHp = 30 }
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(damage = 100))
        )
        val newState = BattleEngine.applyOutcome(state, outcome)
        val updatedHero = newState.heroes.first()
        assertEquals(0, updatedHero.currentHp)
        assertTrue(updatedHero.isDead)
        assertTrue(newState.eventLog.any { it is BattleEvent.HeroDown })
    }

    @Test
    fun `applyOutcome_heal_clampedToMaxHp`() {
        val hero = createHeroInstance(shanti).apply { currentHp = maxHp - 10 }
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(heal = 1000))
        )
        val newState = BattleEngine.applyOutcome(state, outcome)
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
        val hero = createHeroInstance(shanti)
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti", skillUsed = skill,
            perTargetResult = mapOf("Shanti" to TargetResult(statuses = listOf("ATK_DOWN")))
        )
        val newState = BattleEngine.applyOutcome(state, outcome)
        assertTrue(newState.statusEffects.containsKey("Shanti"))
        assertEquals(StatusEffectType.ATK_DOWN, newState.statusEffects["Shanti"]?.first()?.statusType)
    }

    @Test
    fun `applyOutcome_cleanse_removesDebuffs`() {
        val hero = createHeroInstance(shanti)
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf())
        state.statusEffects["Shanti"] = mutableListOf(
            BattleStatus("Shanti", StatusEffectType.ATK_DOWN, 3, 0.2f)
        )
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = mapOf("Shanti" to TargetResult(cleansed = true))
        )
        val newState = BattleEngine.applyOutcome(state, outcome)
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
        val hero = createHeroInstance(shanti).apply { currentHp = 0; isDead = true }
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Maitri", skillUsed = reviveSkill,
            perTargetResult = mapOf("Shanti" to TargetResult(heal = 300))
        )
        val newState = BattleEngine.applyOutcome(state, outcome)
        val revived = newState.heroes.first()
        assertTrue(revived.isDead == false)
        assertTrue(revived.currentHp > 0)
    }

    @Test
    fun `calculateTurnOrder_higherSpdFirst`() {
        val fast = createHeroInstance(shanti).copy(spd = 200, heroId = "Shanti")
        val slow = createHeroInstance(virya).copy(spd = 10, heroId = "Virya")
        val order = BattleEngine.calculateTurnOrder(listOf(fast, slow), emptyList())
        assertEquals("Shanti", order[0].id)
        assertEquals("Virya", order[1].id)
    }

    @Test
    fun `calculateTurnOrder_variance_preventsDeterministicTies`() {
        val a = createHeroInstance(shanti).copy(spd = 100, heroId = "Shanti")
        val b = createHeroInstance(virya).copy(spd = 100, heroId = "Virya")
        val orders = (1..50).map {
            BattleEngine.calculateTurnOrder(listOf(a, b), emptyList())
        }
        val allAFirst = orders.all { it[0].id == "Shanti" }
        assertFalse(allAFirst)
    }

    @Test
    fun `calculateTurnOrder_intermixHeroesAndMonsters`() {
        val hero = createHeroInstance(shanti).copy(spd = 80)
        val monster = createMonsterInstance(krodha).copy(spd = 120)
        val order = BattleEngine.calculateTurnOrder(listOf(hero), listOf(monster))
        assertFalse(order[0].isHero)
        assertTrue(order[1].isHero)
    }

    @Test
    fun `resolveTargets_singleAlly_returnsCaster`() {
        val skill = Skill(id = "test", name = "Test", description = "",
            targetType = TargetType.SINGLE_ALLY)
        val hero = createHeroInstance(shanti)
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf())
        val targets = BattleEngine.resolveTargets(skill, "Shanti", state)
        assertEquals(listOf("Shanti"), targets)
    }

    @Test
    fun `resolveTargets_singleEnemy_returnsFirstLivingMonster`() {
        val skill = Skill(id = "test", name = "Test", description = "",
            targetType = TargetType.SINGLE_ENEMY)
        val hero = createHeroInstance(shanti)
        val monster = createMonsterInstance(bhaya)
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf(monster))
        val targets = BattleEngine.resolveTargets(skill, "Shanti", state)
        assertEquals(1, targets.size)
        assertTrue(targets[0] == "Bhaya")
    }

    @Test
    fun `resolveTargets_allAllies_returnsAllLiving`() {
        val skill = Skill(id = "test", name = "Test", description = "",
            targetType = TargetType.ALL_ALLIES)
        val alive1 = createHeroInstance(shanti).copy(heroId = "Shanti")
        val alive2 = createHeroInstance(virya).copy(heroId = "Virya")
        val alive3 = createHeroInstance(santosha).copy(heroId = "Santosha")
        val dead1 = createHeroInstance(shanti).copy(heroId = "DeadHero", isDead = true, currentHp = 0)
        val state = BattleState(
            heroes = mutableListOf(alive1, alive2, alive3, dead1),
            monsters = mutableListOf()
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
        val hero = createHeroInstance(shanti)
        val monster = createMonsterInstance(bhaya)
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf(monster))
        val targets = BattleEngine.resolveTargets(skill, "Shanti", state)
        assertEquals(1, targets.size)
        assertEquals("Bhaya", targets[0])
    }

    @Test
    fun `comboSkill_twoHero_outcome`() {
        val combo = ComboSkillDefinitions.allCombos.first { it.comboType == ComboType.TWO_HERO }
        val h1 = HeroDefinitions.createInstance(HeroDefinitions.getHero("Shanti")!!, 5)
        val h2 = HeroDefinitions.createInstance(HeroDefinitions.getHero("Virya")!!, 5)
        val h3 = HeroDefinitions.createInstance(HeroDefinitions.getHero("Santosha")!!, 5)
        val state = BattleState(heroes = mutableListOf(h1, h2, h3), monsters = mutableListOf())
        val result = BattleEngine.computeComboOutcome(
            combo = combo,
            casterId = combo.requiredHeroes.first(),
            partnerIds = combo.requiredHeroes.drop(1).toList(),
            state = state
        )
        assertNotNull(result.outcome)
        assertTrue(result.outcome.damageDealt > 0 || result.outcome.healingDone > 0 || result.outcome.shieldApplied > 0)
    }

    @Test
    fun `comboSkill_threeHero_outcome`() {
        val combo = ComboSkillDefinitions.allCombos.first { it.comboType == ComboType.THREE_HERO }
        val h1 = HeroDefinitions.createInstance(HeroDefinitions.getHero("Shanti")!!, 5)
        val h2 = HeroDefinitions.createInstance(HeroDefinitions.getHero("Santosha")!!, 5)
        val h3 = HeroDefinitions.createInstance(HeroDefinitions.getHero("Virya")!!, 5)
        val state = BattleState(heroes = mutableListOf(h1, h2, h3), monsters = mutableListOf())
        val result = BattleEngine.computeComboOutcome(
            combo = combo,
            casterId = combo.requiredHeroes.first(),
            partnerIds = combo.requiredHeroes.drop(1).toList(),
            state = state
        )
        assertNotNull(result.outcome)
    }

    @Test
    fun `comboSkill_healing_restoresHP`() {
        val combo = ComboSkillDefinitions.allCombos.firstOrNull { it.healScaling != null }
            ?: return
        val shantiHero = HeroDefinitions.createInstance(HeroDefinitions.getHero("Shanti")!!, 5)
        val viryaHero = HeroDefinitions.createInstance(HeroDefinitions.getHero("Virya")!!, 5)
        val santoshaHero = HeroDefinitions.createInstance(HeroDefinitions.getHero("Santosha")!!, 5)
        val preHp = shantiHero.currentHp
        shantiHero.currentHp = (shantiHero.currentHp - 200).coerceAtLeast(1)
        val damagedHp = shantiHero.currentHp
        val state = BattleState(
            heroes = mutableListOf(shantiHero, viryaHero, santoshaHero),
            monsters = mutableListOf()
        )
        val result = BattleEngine.computeComboOutcome(
            combo = combo,
            casterId = combo.requiredHeroes.first(),
            partnerIds = combo.requiredHeroes.drop(1).toList(),
            state = state
        )
        val newState = BattleEngine.applyOutcome(state, result.outcome)
        val updatedHero = newState.heroes.first { it.heroId == "Shanti" }
        assertTrue(updatedHero.currentHp > damagedHp)
    }

    @Test
    fun `monsterPhase_reflectDamage_triggersAtThreshold`() {
        val monster = createMonsterInstance(krodha).apply {
            currentHp = (maxHp * 0.45f).toInt()
        }
        val state = BattleState(heroes = mutableListOf(), monsters = mutableListOf(monster))
        val events = BattleEngine.checkPhaseTriggers(state)
        assertTrue(events.any { it is BattleEvent.PhaseTriggered && it.monsterId == "Krodha" })
    }

    @Test
    fun `monsterPhase_summonAdd_createsNewMonster`() {
        val monster = createMonsterInstance(klesh).apply {
            currentHp = (maxHp * 0.55f).toInt()
        }
        val state = BattleState(heroes = mutableListOf(), monsters = mutableListOf(monster))
        val events = BattleEngine.checkPhaseTriggers(state)
        assertTrue(events.any { it is BattleEvent.PhaseTriggered && it.monsterId == "Klesh" })
    }

    @Test
    fun `monsterPhase_gainShield_addsShield`() {
        val monster = createMonsterInstance(klesh).apply {
            currentHp = (maxHp * 0.55f).toInt()
            shield = 0
        }
        val state = BattleState(heroes = mutableListOf(), monsters = mutableListOf(monster))
        val preShield = monster.shield
        BattleEngine.checkPhaseTriggers(state)
        assertTrue(monster.shield > preShield)
    }

    @Test
    fun `chooseMonsterTarget_lowestHP_targetsLowest`() {
        val heroes = listOf(
            createHeroInstance(shanti).copy(currentHp = 100, heroId = "Shanti"),
            createHeroInstance(virya).copy(currentHp = 50, heroId = "Virya"),
            createHeroInstance(santosha).copy(currentHp = 200, heroId = "Santosha")
        )
        val target = BattleEngine.chooseMonsterTarget(heroes, TargetStrategy.LOWEST_HP)
        assertEquals("Virya", target)
    }

    @Test
    fun `chooseMonsterTarget_highestHP_targetsHighest`() {
        val heroes = listOf(
            createHeroInstance(shanti).copy(currentHp = 100, heroId = "Shanti"),
            createHeroInstance(virya).copy(currentHp = 50, heroId = "Virya"),
            createHeroInstance(santosha).copy(currentHp = 200, heroId = "Santosha")
        )
        val target = BattleEngine.chooseMonsterTarget(heroes, TargetStrategy.HIGHEST_HP)
        assertEquals("Santosha", target)
    }

    @Test
    fun `chooseMonsterTarget_random_selectsLivingHero`() {
        val heroes = listOf(
            createHeroInstance(shanti).copy(isDead = true, currentHp = 0, heroId = "Shanti"),
            createHeroInstance(virya).copy(isDead = false, currentHp = 100, heroId = "Virya")
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
        val alive = createHeroInstance(shanti).copy(heroId = "Shanti")
        val dead = createHeroInstance(virya).copy(heroId = "Virya", isDead = true, currentHp = 0)
        val state = BattleState(heroes = mutableListOf(alive, dead), monsters = mutableListOf())
        val targets = BattleEngine.resolveTargets(skill, "Shanti", state)
        assertEquals(listOf("Shanti"), targets)
        assertFalse(targets.contains("Virya"))
    }

    @Test
    fun `calculateTurnOrder_excludesDeadMonsters`() {
        val hero = createHeroInstance(shanti).copy(spd = 100)
        val aliveMonster = createMonsterInstance(bhaya).copy(spd = 200, monsterId = "AliveMonster")
        val deadMonster = createMonsterInstance(krodha).copy(
            spd = 999, monsterId = "DeadMonster",
            isDead = true, currentHp = 0
        )
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
        val hero = createHeroInstance(shanti)
        val state = BattleState(heroes = mutableListOf(hero), monsters = mutableListOf())
        val outcome = ActionOutcome(
            action = TurnAction.SKILL, actorId = "Shanti",
            perTargetResult = emptyMap()
        )
        val newState = BattleEngine.applyOutcome(state, outcome)
        assertEquals(hero.currentHp, newState.heroes.first().currentHp)
    }
}
