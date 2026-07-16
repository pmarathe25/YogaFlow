package com.example.game.battle

import com.example.game.model.*
import org.junit.Assert.*
import org.junit.Test

class TurnManagerTest {

    private val fixedRng = object : RandomProvider {
        override fun nextFloat(): Float = 0.5f
        override fun nextInt(until: Int): Int = 0
    }

    private val turnManager = TurnManager(fixedRng)

    private fun enterPlayerTurn(state: BattleState): BattleState {
        var s = state
        while (s.phase == BattlePhase.ENEMY_TURN) {
            s = turnManager.executeMonsterTurn(s, s.currentActorId).newState
            s = turnManager.advanceTurn(s).newState
        }
        return s
    }

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
        level: Int = 1,
        isDefeated: Boolean = false
    ): CombatantState = CombatantState(
        id = id, side = CombatSide.HERO, name = name, element = Element.NEUTRAL,
        maxHp = baseHp, hp = hp, attack = baseAtk, level = level,
        skills = listOf(dummySkill), ultimate = dummyUltimate, isDefeated = isDefeated
    )

    private fun makeMonster(
        id: String = "Monster",
        name: String = "Monster",
        baseHp: Int = 1000,
        hp: Int = baseHp,
        baseAtk: Int = 50,
        phases: List<MonsterPhase> = listOf(MonsterPhase(1f, emptyList())),
        isDefeated: Boolean = false
    ): CombatantState = CombatantState(
        id = id, side = CombatSide.MONSTER, name = name, element = Element.NEUTRAL,
        maxHp = baseHp, hp = hp, attack = baseAtk, level = 1,
        phases = phases, aiBehavior = AIBehavior(specialChance = 0f),
        specialAttack = dummySkill, isDefeated = isDefeated
    )

    @Test
    fun `startBattle returns ordered turn queue with first actor active`() {
        val hero1 = makeHero(id = "H1")
        val hero2 = makeHero(id = "H2")
        val monster = makeMonster(id = "M1")
        var state = turnManager.startBattle(listOf(hero1, hero2), listOf(monster))
        assertEquals(3, state.turnOrder.size)
        assertEquals("M1", state.currentActorId)
        assertEquals(BattlePhase.ENEMY_TURN, state.phase)
    }

    @Test
    fun `advanceTurn propagates victory when all monsters defeated`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1", hp = 0, isDefeated = true)
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = listOf(BattleActor("H1", "Hero", true)),
            currentTurnIndex = 0, currentActorId = "H1", phase = BattlePhase.PLAYER_TURN
        )
        val result = turnManager.advanceTurn(state)
        assertTrue(result.victory)
        assertEquals(BattlePhase.VICTORY, result.newState.phase)
    }

    @Test
    fun `advanceTurn propagates defeat when all heroes defeated`() {
        val hero = makeHero(id = "H1", hp = 0, isDefeated = true)
        val monster = makeMonster(id = "M1")
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = listOf(BattleActor("M1", "Monster", false)),
            currentTurnIndex = 0, currentActorId = "M1", phase = BattlePhase.ENEMY_TURN
        )
        val result = turnManager.advanceTurn(state)
        assertTrue(result.defeat)
        assertEquals(BattlePhase.DEFEAT, result.newState.phase)
    }

    @Test
    fun `executeSkill facade reduces target HP and returns TurnResult`() {
        val hero = makeHero(id = "H1", baseAtk = 100)
        val monster = makeMonster(id = "M1", baseHp = 1000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.executeSkill(state, "H1", damageSkill, listOf("M1"))
        val updatedMonster = result.newState.monsters.find { it.id == "M1" }
        assertNotNull(updatedMonster)
        assertTrue(updatedMonster!!.hp < 1000)
    }

    @Test
    fun `executeUltimate facade resets gauge and deals damage`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1", baseHp = 2000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "H1") it.copy(gauge = 100) else it }
        )
        val result = turnManager.executeUltimate(state, "H1")
        val updatedHero = result.newState.heroes.find { it.id == "H1" }
        assertEquals(0, updatedHero!!.gauge)
        val updatedMonster = result.newState.monsters.find { it.id == "M1" }
        assertTrue(updatedMonster!!.hp < 2000)
    }

    @Test
    fun `defend facade adds shield and gauge`() {
        val hero = makeHero(id = "H1", baseHp = 500, hp = 500)
        val monster = makeMonster(id = "M1")
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.defend(state, "H1")
        val updatedHero = result.newState.heroes.first { it.id == "H1" }
        assertTrue(updatedHero.shield > 0)
        assertTrue(updatedHero.gauge > 0)
    }

    @Test
    fun `executeMonsterTurn facade damages hero`() {
        val hero = makeHero(id = "H1", baseHp = 500)
        val monster = makeMonster(id = "M1", baseAtk = 100)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeMonsterTurn(state, "M1")
        val updatedHero = result.newState.heroes.find { it.id == "H1" }
        assertTrue(updatedHero!!.hp < 500)
    }

    @Test
    fun `executeCombo facade applies damage to targets`() {
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
        var state = turnManager.startBattle(listOf(h1, h2), listOf(m1, m2))
        state = enterPlayerTurn(state)
        val result = turnManager.executeCombo(state, combo, setOf("1", "2"))
        assertTrue(result.newState.monsters.all { it.hp < 1000 })
    }

    @Test
    fun `resolveTargets facade returns expected target list`() {
        val hero = makeHero(id = "H1")
        val monster = makeMonster(id = "M1")
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        val targets = turnManager.resolveTargets(damageSkill, "H1", state)
        assertEquals(listOf("M1"), targets)
    }
}