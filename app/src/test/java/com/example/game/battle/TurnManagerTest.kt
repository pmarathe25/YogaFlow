package com.example.game.battle

import com.example.game.model.*
import org.junit.Assert.*
import org.junit.Test

class TurnManagerTest {

    private val fixedRng = object : RandomProvider {
        private var floatCalls = 0
        override fun nextFloat(): Float {
            floatCalls++
            return 0.5f
        }
        override fun nextInt(until: Int): Int = 0
    }

    private val turnManager = TurnManager(fixedRng)

    private val dummySkill = Skill(
        id = "dummy", name = "Dummy", description = "",
        targetType = TargetType.SINGLE_ENEMY
    )

    private val damageSkill = Skill(
        id = "attack", name = "Attack", description = "",
        targetType = TargetType.SINGLE_ENEMY,
        damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
        baseDamage = 100, ultimateGain = 20, cooldown = 0
    )

    private val healSkill = Skill(
        id = "heal", name = "Heal", description = "",
        targetType = TargetType.SINGLE_ALLY,
        healScaling = HealScaling(baseHeal = 30, healPerLevel = 0, isPercentage = true)
    )

    private val shieldSkill = Skill(
        id = "shield", name = "Shield", description = "",
        targetType = TargetType.SELF,
        shieldScaling = ShieldScaling(baseShield = 0, shieldPerLevel = 0, isPercentage = true, percentage = 0.3f)
    )

    private val dummyUltimate = Skill(
        id = "ult", name = "Ultimate", description = "",
        targetType = TargetType.SINGLE_ENEMY, baseDamage = 300
    )

    private fun makeHero(
        id: String = "Hero",
        name: String = "Hero",
        baseHp: Int = 500,
        hp: Int = baseHp,
        baseAtk: Int = 100,
        baseSpd: Int = 100,
        level: Int = 1,
        isDefeated: Boolean = false
    ): CombatantState = CombatantState(
        id = id,
        side = CombatSide.HERO,
        name = name,
        element = Element.NEUTRAL,
        maxHp = baseHp,
        hp = hp,
        attack = baseAtk,
        speed = baseSpd,
        level = level,
        skills = listOf(dummySkill),
        ultimate = dummyUltimate,
        isDefeated = isDefeated
    )

    private fun makeMonster(
        id: String = "Monster",
        name: String = "Monster",
        baseHp: Int = 1000,
        hp: Int = baseHp,
        baseAtk: Int = 50,
        baseSpd: Int = 50,
        phases: List<MonsterPhase> = listOf(MonsterPhase(1f, emptyList())),
        isDefeated: Boolean = false
    ): CombatantState = CombatantState(
        id = id,
        side = CombatSide.MONSTER,
        name = name,
        element = Element.NEUTRAL,
        maxHp = baseHp,
        hp = hp,
        attack = baseAtk,
        speed = baseSpd,
        level = 1,
        phases = phases,
        aiBehavior = AIBehavior(specialChance = 0f),
        specialAttack = dummySkill,
        isDefeated = isDefeated
    )

    @Test
    fun `startBattle_createsTurnOrder_includesAllAliveActors`() {
        val hero1 = makeHero(id = "H1")
        val hero2 = makeHero(id = "H2")
        val monster = makeMonster(id = "M1")
        val state = turnManager.startBattle(listOf(hero1, hero2), listOf(monster))
        assertEquals(3, state.turnOrder.size)
        assertTrue(state.turnOrder.any { it.id == "H1" })
        assertTrue(state.turnOrder.any { it.id == "H2" })
        assertTrue(state.turnOrder.any { it.id == "M1" })
    }

    @Test
    fun `startBattle_firstActorInTurnOrder_isCurrentActor`() {
        val hero = makeHero(id = "H1", baseSpd = 200)
        val monster = makeMonster(id = "M1", baseSpd = 10)
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        assertEquals("H1", state.currentActorId)
    }

    @Test
    fun `startBattle_heroFirst_setsPhaseToPLAYER_TURN`() {
        val hero = makeHero(id = "H1", baseSpd = 200)
        val monster = makeMonster(id = "M1", baseSpd = 10)
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        assertEquals(BattlePhase.PLAYER_TURN, state.phase)
    }

    @Test
    fun `startBattle_monsterFirst_setsPhaseToENEMY_TURN`() {
        val hero = makeHero(id = "H1", baseSpd = 10)
        val monster = makeMonster(id = "M1", baseSpd = 200)
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        assertEquals(BattlePhase.ENEMY_TURN, state.phase)
    }

    @Test
    fun `advanceTurn_allMonstersDead_setsPhaseToVictory`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1", hp = 0, isDefeated = true)
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = listOf(BattleActor("H1", "Hero", 100, true)),
            currentTurnIndex = 0, currentActorId = "H1", phase = BattlePhase.PLAYER_TURN
        )
        val result = turnManager.advanceTurn(state)
        assertTrue(result.victory)
        assertEquals(BattlePhase.VICTORY, result.newState.phase)
    }

    @Test
    fun `advanceTurn_allHeroesDead_setsPhaseToDefeat`() {
        val hero = makeHero(id = "H1", hp = 0, isDefeated = true)
        val monster = makeMonster(id = "M1")
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = listOf(BattleActor("M1", "Monster", 100, false)),
            currentTurnIndex = 0, currentActorId = "M1", phase = BattlePhase.ENEMY_TURN
        )
        val result = turnManager.advanceTurn(state)
        assertTrue(result.defeat)
        assertEquals(BattlePhase.DEFEAT, result.newState.phase)
    }

    @Test
    fun `advanceTurn_nextActorIsHero_setsPhaseToPLAYER_TURN`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        val order = listOf(BattleActor("H1", "Hero", 100, true), BattleActor("M1", "Monster", 50, false))
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = order, currentTurnIndex = 0,
            currentActorId = "H1", phase = BattlePhase.PLAYER_TURN
        )
        val result = turnManager.advanceTurn(state)
        assertEquals(BattlePhase.ENEMY_TURN, result.newState.phase)
        assertEquals("M1", result.newState.currentActorId)
    }

    @Test
    fun `advanceTurn_nextActorIsMonster_setsPhaseToENEMY_TURN`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        val order = listOf(BattleActor("M1", "Monster", 50, false), BattleActor("H1", "Hero", 100, true))
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = order, currentTurnIndex = 0,
            currentActorId = "M1", phase = BattlePhase.ENEMY_TURN
        )
        val result = turnManager.advanceTurn(state)
        assertEquals(BattlePhase.PLAYER_TURN, result.newState.phase)
        assertEquals("H1", result.newState.currentActorId)
    }

    @Test
    fun `advanceTurn_wrapsAround_advancesRoundAndDecaysStatuses`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        val order = listOf(BattleActor("H1", "Hero", 100, true))
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = order, currentTurnIndex = 0,
            currentActorId = "H1", phase = BattlePhase.PLAYER_TURN,
            statusEffects = mapOf("H1" to listOf(BattleStatus("H1", StatusEffectType.ATK_UP, 2, 0.3f)))
        )
        val result = turnManager.advanceTurn(state)
        assertEquals(2, result.newState.round)
        val remainingStatus = result.newState.statusEffects["H1"]
        assertNotNull(remainingStatus)
        assertEquals(1, remainingStatus?.first()?.remainingTurns)
    }

    @Test
    fun `advanceTurn_decrementsCooldowns_forNextActor`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        val order = listOf(BattleActor("H1", "Hero", 100, true), BattleActor("M1", "Monster", 50, false))
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = order, currentTurnIndex = 0,
            currentActorId = "H1", phase = BattlePhase.PLAYER_TURN,
            skillCooldowns = mapOf("M1" to mapOf("dummy" to 2))
        )
        val result = turnManager.advanceTurn(state)
        val cooldowns = result.newState.skillCooldowns["M1"]
        assertNotNull(cooldowns)
        assertEquals(1, cooldowns?.get("dummy"))
    }

    @Test
    fun `advanceTurn_incrementsTurnsTaken`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        val order = listOf(BattleActor("H1", "Hero", 100, true), BattleActor("M1", "Monster", 50, false))
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = order, currentTurnIndex = 0,
            currentActorId = "H1", phase = BattlePhase.PLAYER_TURN,
            turnsTaken = 5
        )
        val result = turnManager.advanceTurn(state)
        assertEquals(6, result.newState.turnsTaken)
    }

    @Test
    fun `executeSkill_damageSkill_reducesTargetHP`() {
        val hero = makeHero(id = "H1", baseAtk = 100)
        val monster = makeMonster(id = "M1", baseHp = 1000)
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeSkill(state, "H1", damageSkill, listOf("M1"))
        val updatedMonster = result.newState.monsters.find { it.id == "M1" }
        assertNotNull(updatedMonster)
        assertTrue(updatedMonster!!.hp < 1000)
    }

    @Test
    fun `executeSkill_healSkill_restoresAllyHP`() {
        val hero = makeHero(id = "H1", baseHp = 500)
        val monster = makeMonster(id = "M1")
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "H1") it.copy(hp = 200) else it }
        )
        val result = turnManager.executeSkill(state, "H1", healSkill, listOf("H1"))
        val updatedHero = result.newState.heroes.find { it.id == "H1" }
        assertNotNull(updatedHero)
        assertTrue(updatedHero!!.hp > 200)
    }

    @Test
    fun `executeSkill_shieldSkill_addsShield`() {
        val hero = makeHero(id = "H1", baseHp = 500)
        val monster = makeMonster(id = "M1")
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeSkill(state, "H1", shieldSkill, listOf("H1"))
        val updatedHero = result.newState.heroes.find { it.id == "H1" }
        assertNotNull(updatedHero)
        assertTrue(updatedHero!!.shield > 0)
    }

    @Test
    fun `executeSkill_monsterKillTriggersMonsterDown`() {
        val hero = makeHero(id = "H1", baseAtk = 10000)
        val monster = makeMonster(id = "M1", baseHp = 10)
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeSkill(state, "H1", damageSkill, listOf("M1"))
        assertTrue(result.events.any { it is BattleEvent.MonsterDown })
        val updatedMonster = result.newState.monsters.find { it.id == "M1" }
        assertTrue(updatedMonster!!.isDefeated)
    }

    @Test
    fun `executeSkill_withCooldown_setsCooldown`() {
        val skillWithCd = damageSkill.copy(cooldown = 3)
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeSkill(state, "H1", skillWithCd, listOf("M1"))
        val cooldowns = result.newState.skillCooldowns["H1"]
        assertNotNull(cooldowns)
        assertEquals(3, cooldowns?.get("attack"))
    }

    @Test
    fun `executeSkill_skillOnCooldown_doesNothing`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = listOf(BattleActor("H1", "Hero", 100, true)),
            currentTurnIndex = 0, currentActorId = "H1", phase = BattlePhase.PLAYER_TURN,
            skillCooldowns = mapOf("H1" to mapOf("attack" to 2))
        )
        val result = turnManager.executeSkill(state, "H1", damageSkill, listOf("M1"))
        assertEquals(state, result.newState)
    }

    @Test
    fun `executeSkill_increasesUltimateGauge`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeSkill(state, "H1", damageSkill, listOf("M1"))
        val updatedHero = result.newState.heroes.find { it.id == "H1" }
        assertTrue(updatedHero!!.gauge > 0)
    }

    @Test
    fun `executeUltimate_resetsUltimateGaugeToZero`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "H1") it.copy(gauge = 100) else it }
        )
        val result = turnManager.executeUltimate(state, "H1")
        val updatedHero = result.newState.heroes.find { it.id == "H1" }
        assertEquals(0, updatedHero!!.gauge)
    }

    @Test
    fun `executeUltimate_whenGaugeNotFull_noOp`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "H1") it.copy(gauge = 50) else it }
        )
        val result = turnManager.executeUltimate(state, "H1")
        assertEquals(state, result.newState)
    }

    @Test
    fun `executeUltimate_dealsDamage`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1", baseHp = 2000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "H1") it.copy(gauge = 100) else it }
        )
        val result = turnManager.executeUltimate(state, "H1")
        val updatedMonster = result.newState.monsters.find { it.id == "M1" }
        assertTrue(updatedMonster!!.hp < 2000)
    }

    @Test
    fun `executeMonsterTurn_basicAttack_damagesHero`() {
        val hero = makeHero(id = "H1", baseHp = 500, baseSpd = 10)
        val monster = makeMonster(id = "M1", baseAtk = 100, baseSpd = 200)
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeMonsterTurn(state, "M1")
        val updatedHero = result.newState.heroes.find { it.id == "H1" }
        assertNotNull(updatedHero)
        assertTrue(updatedHero!!.hp < 500)
    }

    @Test
    fun `executeMonsterTurn_killsHero_triggersHeroDown`() {
        val hero = makeHero(id = "H1", baseHp = 10, baseSpd = 10)
        val monster = makeMonster(id = "M1", baseAtk = 1000, baseSpd = 200)
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeMonsterTurn(state, "M1")
        assertTrue(result.events.any { it is BattleEvent.HeroDown })
    }

    @Test
    fun `executeMonsterTurn_usesSpecialAttack_whenChanceSufficient`() {
        val specialSkill = Skill(
            id = "special", name = "Special", description = "",
            targetType = TargetType.SINGLE_ENEMY,
            damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
            baseDamage = 500
        )
        val monster = CombatantState(
            id = "M1", side = CombatSide.MONSTER, name = "Monster",
            element = Element.NEUTRAL, maxHp = 1000, hp = 1000,
            attack = 50, speed = 200,
            specialAttack = specialSkill,
            aiBehavior = AIBehavior(specialChance = 1f),
            phases = listOf(MonsterPhase(1f, emptyList()))
        )
        val hero = makeHero(id = "H1", baseHp = 2000, baseSpd = 10)
        val state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeMonsterTurn(state, "M1")
        val updatedHero = result.newState.heroes.find { it.id == "H1" }
        assertNotNull(updatedHero)
        assertTrue(updatedHero!!.hp < 2000)
    }

    @Test
    fun `monster_phaseTrigger_gainShield_atThreshold`() {
        val phases = listOf(
            MonsterPhase(0.5f, listOf(
                PhaseTrigger(PhaseTriggerType.GAIN_SHIELD, value = 0.3f)
            ))
        )
        val monster = makeMonster(id = "M1", baseHp = 1000, phases = phases, baseSpd = 200)
        val hero = makeHero(id = "H1", baseHp = 500, baseSpd = 10)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = state.copy(
            monsters = state.monsters.map { if (it.id == "M1") it.copy(hp = 400) else it }
        )
        val result = turnManager.executeMonsterTurn(state, "M1")
        val updatedMonster = result.newState.monsters.find { it.id == "M1" }
        assertNotNull(updatedMonster)
        assertTrue(updatedMonster!!.shield > 0)
    }

    @Test
    fun `monster_phaseTrigger_extraAction_grantsExtraTurn`() {
        val phases = listOf(
            MonsterPhase(0.5f, listOf(
                PhaseTrigger(PhaseTriggerType.EXTRA_ACTION, value = 1f)
            ))
        )
        val monster = makeMonster(id = "M1", baseHp = 1000, phases = phases)
        val hero = makeHero(id = "H1", baseHp = 2000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = state.copy(
            monsters = state.monsters.map { if (it.id == "M1") it.copy(hp = 400) else it }
        )
        val result = turnManager.executeMonsterTurn(state, "M1")
        assertTrue(result.newState.monsters.any { it.id == "M1" && !it.isDefeated })
    }

    @Test
    fun `executeCombo_multiTarget_appliesToAll`() {
        val combo = ComboSkill(
            id = "test_combo", name = "Test Combo", description = "",
            requiredHeroes = setOf(1, 2),
            targetType = TargetType.ALL_ENEMIES,
            damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
            baseDamage = 100, comboType = ComboType.TWO_HERO
        )
        val h1 = makeHero(id = "1")
        val h2 = makeHero(id = "2")
        val m1 = makeMonster(id = "M1")
        val m2 = makeMonster(id = "M2")
        val state = turnManager.startBattle(listOf(h1, h2), listOf(m1, m2))
        val result = turnManager.executeCombo(state, combo, setOf("1", "2"))
        val updatedM1 = result.newState.monsters.find { it.id == "M1" }
        val updatedM2 = result.newState.monsters.find { it.id == "M2" }
        assertTrue(updatedM1!!.hp < 1000)
        assertTrue(updatedM2!!.hp < 1000)
    }

    @Test
    fun `executeCombo_participantsGainUltimateGauge`() {
        val combo = ComboSkill(
            id = "test_combo", name = "Test Combo", description = "",
            requiredHeroes = setOf(1, 2),
            targetType = TargetType.SINGLE_ENEMY,
            damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
            baseDamage = 100, comboType = ComboType.TWO_HERO
        )
        val h1 = makeHero(id = "1")
        val h2 = makeHero(id = "2")
        val monster = makeMonster(id = "M1")
        val state = turnManager.startBattle(listOf(h1, h2), listOf(monster))
        val result = turnManager.executeCombo(state, combo, setOf("1", "2"))
        val updatedH1 = result.newState.heroes.find { it.id == "1" }
        val updatedH2 = result.newState.heroes.find { it.id == "2" }
        assertTrue(updatedH1!!.gauge > 0)
        assertTrue(updatedH2!!.gauge > 0)
    }
}
