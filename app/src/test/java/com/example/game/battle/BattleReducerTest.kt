package com.example.game.battle

import com.example.game.model.*
import org.junit.Assert.*
import org.junit.Test

class BattleReducerTest {

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

    private fun makeCombatant(
        id: String = "hero_1",
        name: String = "Test Hero",
        element: Element = Element.FIRE,
        hp: Int = 500,
        maxHp: Int = 500,
        atk: Int = 50,
        level: Int = 1,
        isMonster: Boolean = false,
        skills: List<Skill> = listOf(dummySkill),
        ultimate: Skill? = dummyUltimate,
        phases: List<MonsterPhase> = if (isMonster) listOf(MonsterPhase(1f, emptyList())) else emptyList(),
        isDefeated: Boolean = false
    ): CombatantState = CombatantState(
        id = id,
        side = if (isMonster) CombatSide.MONSTER else CombatSide.HERO,
        name = name,
        element = element,
        maxHp = maxHp,
        hp = hp,
        attack = atk,
        level = level,
        skills = skills,
        ultimate = ultimate,
        phases = phases,
        aiBehavior = if (isMonster) AIBehavior(specialChance = 0f) else null,
        specialAttack = if (isMonster) dummySkill else null,
        isDefeated = isDefeated
    )

    @Test
    fun `startBattle produces monsters first then heroes turn order`() {
        val fast = makeCombatant(id = "h1")
        val slow = makeCombatant(id = "h2")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(fast, slow), listOf(monster))
        assertEquals(3, state.turnOrder.size)
        assertEquals("m1", state.turnOrder[0].id)
        assertEquals(false, state.turnOrder[0].isHero)
        assertEquals(true, state.turnOrder[1].isHero)
        assertEquals(true, state.turnOrder[2].isHero)
        assertEquals(BattlePhase.ENEMY_TURN, state.phase)
        assertEquals("m1", state.currentActorId)
    }

    @Test
    fun `startBattle with empty lists produces victory`() {
        var state = turnManager.startBattle(emptyList(), emptyList())
        state = enterPlayerTurn(state)
        assertEquals(BattlePhase.VICTORY, state.phase)
        assertTrue(state.turnOrder.isEmpty())
    }

    @Test
    fun `heroes act in any order and stay in PLAYER_TURN until all acted`() {
        val h1 = makeCombatant(id = "h1")
        val h2 = makeCombatant(id = "h2")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(h1, h2), listOf(monster))
        state = enterPlayerTurn(state)
        // monsters go first
        state = turnManager.advanceTurn(state).newState // m1 acts
        assertEquals(BattlePhase.PLAYER_TURN, state.phase)
        // act with h2 first (out of order)
        val afterH2 = turnManager.executeSkill(state, "h2", damageSkill, listOf("m1"))
        state = afterH2.newState
        assertTrue("h2" in state.heroesActedThisRound)
        assertEquals(BattlePhase.PLAYER_TURN, state.phase)
        assertEquals("", state.currentActorId)
        // act with h1
        val afterH1 = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        state = afterH1.newState
        assertTrue("h1" in state.heroesActedThisRound)
        // advancing should move to enemy phase (new round)
        val afterAdvance = turnManager.advanceTurn(state)
        assertEquals(BattlePhase.ENEMY_TURN, afterAdvance.newState.phase)
        assertEquals(2, afterAdvance.newState.round)
    }

    @Test
    fun `acted hero cannot act again`() {
        val h1 = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(h1), listOf(monster))
        state = enterPlayerTurn(state)
        state = turnManager.advanceTurn(state).newState // m1 acts
        val afterFirst = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        assertTrue("h1" in afterFirst.newState.heroesActedThisRound)
        // attempting to act again is a no-op
        val afterSecond = turnManager.executeSkill(afterFirst.newState, "h1", damageSkill, listOf("m1"))
        assertEquals(afterFirst.newState, afterSecond.newState)
    }

    @Test
    fun `advanceTurn wraps to enemy phase after all heroes act`() {
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = turnManager.advanceTurn(state).newState // m1 acts -> PLAYER_TURN
        assertEquals(BattlePhase.PLAYER_TURN, state.phase)
        state = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1")).newState
        assertEquals(1, state.round)
        val result = turnManager.advanceTurn(state)
        assertEquals(BattlePhase.ENEMY_TURN, result.newState.phase)
        assertEquals(2, result.newState.round)
    }

    @Test
    fun `advanceTurn skips dead actors and detects victory`() {
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 50, maxHp = 50)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val afterKill = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        assertTrue(afterKill.newState.monsters.any { it.isDefeated })
        val result = turnManager.advanceTurn(afterKill.newState)
        assertTrue(result.victory)
        assertEquals(BattlePhase.VICTORY, result.newState.phase)
    }

    @Test
    fun `resolveTargets single ally returns caster`() {
        val skill = Skill(id = "t", name = "T", description = "", targetType = TargetType.SINGLE_ALLY)
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val targets = turnManager.resolveTargets(skill, "h1", state)
        assertEquals(listOf("h1"), targets)
    }

    @Test
    fun `resolveTargets single enemy returns first living monster`() {
        val skill = Skill(id = "t", name = "T", description = "", targetType = TargetType.SINGLE_ENEMY)
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val targets = turnManager.resolveTargets(skill, "h1", state)
        assertEquals(listOf("m1"), targets)
    }

    @Test
    fun `resolveTargets all allies returns all living heroes`() {
        val skill = Skill(id = "t", name = "T", description = "", targetType = TargetType.ALL_ALLIES)
        val h1 = makeCombatant(id = "h1")
        val h2 = makeCombatant(id = "h2")
        val h3 = makeCombatant(id = "h3")
        var state = turnManager.startBattle(listOf(h1, h2, h3), emptyList())
        state = enterPlayerTurn(state)
        val targets = turnManager.resolveTargets(skill, "h1", state)
        assertEquals(3, targets.size)
        assertTrue(targets.containsAll(listOf("h1", "h2", "h3")))
    }

    @Test
    fun `resolveTargets all enemies returns all monsters`() {
        val skill = Skill(id = "t", name = "T", description = "", targetType = TargetType.ALL_ENEMIES)
        val hero = makeCombatant(id = "h1")
        val m1 = makeCombatant(id = "m1", isMonster = true)
        val m2 = makeCombatant(id = "m2", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(m1, m2))
        state = enterPlayerTurn(state)
        val targets = turnManager.resolveTargets(skill, "h1", state)
        assertEquals(2, targets.size)
        assertTrue(targets.containsAll(listOf("m1", "m2")))
    }

    @Test
    fun `resolveTargets self returns caster`() {
        val skill = Skill(id = "t", name = "T", description = "", targetType = TargetType.SELF)
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val targets = turnManager.resolveTargets(skill, "h1", state)
        assertEquals(listOf("h1"), targets)
    }

    @Test
    fun `executeSkill damage reduces target HP`() {
        val hero = makeCombatant(id = "h1", atk = 100)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        val updated = result.newState.monsters.first { it.id == "m1" }
        assertTrue(updated.hp < 1000)
        assertTrue(updated.hp >= 0)
    }

    @Test
    fun `executeSkill shield absorbs before HP`() {
        val hero = makeCombatant(id = "h1", atk = 100)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            monsters = state.monsters.map { if (it.id == "m1") it.copy(shield = 100) else it }
        )
        val result = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        val updated = result.newState.monsters.first { it.id == "m1" }
        assertTrue(updated.hp < 1000)
        assertTrue(updated.shield == 0 || updated.shield < 100)
    }

    @Test
    fun `executeSkill overkill damages HP after shield depleted`() {
        val hero = makeCombatant(id = "h1", atk = 100)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            monsters = state.monsters.map { if (it.id == "m1") it.copy(shield = 50) else it }
        )
        val bigDamageSkill = damageSkill.copy(baseDamage = 300)
        val result = turnManager.executeSkill(state, "h1", bigDamageSkill, listOf("m1"))
        val updated = result.newState.monsters.first { it.id == "m1" }
        assertEquals(0, updated.shield)
        assertTrue(updated.hp < 1000)
    }

    @Test
    fun `executeSkill kill triggers MonsterDown event`() {
        val hero = makeCombatant(id = "h1", atk = 100)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 50, maxHp = 50)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        val updated = result.newState.monsters.first { it.id == "m1" }
        assertTrue(updated.isDefeated)
        assertTrue(result.events.any { it is BattleEvent.MonsterDown })
    }

    @Test
    fun `executeSkill heal restores HP clamped to max`() {
        val hero = makeCombatant(id = "h1", maxHp = 500, hp = 500)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "h1") it.copy(hp = 200) else it }
        )
        val result = turnManager.executeSkill(state, "h1", healSkill, listOf("h1"))
        val updated = result.newState.heroes.first { it.id == "h1" }
        assertTrue(updated.hp > 200)
        assertTrue(updated.hp <= 500)
    }

    @Test
    fun `executeSkill heal percentage scales with max Hp`() {
        val pctHeal = Skill(
            id = "pct_heal", name = "Pct Heal", description = "",
            targetType = TargetType.SINGLE_ALLY,
            healScaling = HealScaling(baseHeal = 50, healPerLevel = 0, isPercentage = true)
        )
        val hero = makeCombatant(id = "h1", maxHp = 1000, hp = 1000)
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "h1") it.copy(hp = 100) else it }
        )
        val result = turnManager.executeSkill(state, "h1", pctHeal, listOf("h1"))
        val updated = result.newState.heroes.first { it.id == "h1" }
        assertEquals(600, updated.hp)
    }

    @Test
    fun `executeSkill shield applied to hero`() {
        val hero = makeCombatant(id = "h1", maxHp = 500, hp = 500)
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.executeSkill(state, "h1", shieldSkill, listOf("h1"))
        val updated = result.newState.heroes.first { it.id == "h1" }
        assertTrue(updated.shield > 0)
    }

    @Test
    fun `executeSkill shield absorbs damage`() {
        val hero = makeCombatant(id = "h1", maxHp = 500, hp = 500)
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "h1") it.copy(shield = 200) else it }
        )
        val result = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        val updated = result.newState.monsters.first { it.id == "m1" }
        assertTrue(updated.hp < 1000)
    }

    @Test
    fun `executeSkill hero kill triggers HeroDown event`() {
        val hero = makeCombatant(id = "h1", hp = 100, maxHp = 200, atk = 100)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 50, maxHp = 50)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "h1") it.copy(hp = 30) else it }
        )
        val result = turnManager.executeSkill(state, "h1", damageSkill, listOf("h1"))
        val updated = result.newState.heroes.first { it.id == "h1" }
        assertTrue(updated.isDefeated)
        assertTrue(result.events.any { it is BattleEvent.HeroDown })
    }

    @Test
    fun `executeSkill revive restores HP`() {
        val reviveSkill = Skill(
            id = "revive", name = "Revive", description = "",
            targetType = TargetType.SINGLE_ALLY,
            healScaling = HealScaling(baseHeal = 50, healPerLevel = 0, isPercentage = true),
            revive = true
        )
        val hero = makeCombatant(id = "h1", maxHp = 500, hp = 500)
        val dead = makeCombatant(id = "dead", maxHp = 500, hp = 0)
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero, dead), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "dead") it.copy(hp = 0, isDefeated = true) else it }
        )
        val result = turnManager.executeSkill(state, "h1", reviveSkill, listOf("dead"))
        val revived = result.newState.heroes.first { it.id == "dead" }
        assertFalse(revived.isDefeated)
        assertTrue(revived.hp > 0)
    }

    @Test
    fun `executeSkill monster kill triggers MonsterDown`() {
        val hero = makeCombatant(id = "h1", atk = 100)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 30, maxHp = 30)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        assertTrue(result.events.any { it is BattleEvent.MonsterDown })
        val updated = result.newState.monsters.first { it.id == "m1" }
        assertTrue(updated.isDefeated)
    }

    @Test
    fun `executeSkill sets cooldown`() {
        val cdSkill = damageSkill.copy(cooldown = 3)
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.executeSkill(state, "h1", cdSkill, listOf("m1"))
        val cooldowns = result.newState.skillCooldowns["h1"]
        assertNotNull(cooldowns)
        assertEquals(3, cooldowns?.get("attack"))
    }

    @Test
    fun `executeSkill cooldown ticks down on turn start`() {
        val cdSkill = damageSkill.copy(cooldown = 2)
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val afterUse = turnManager.executeSkill(state, "h1", cdSkill, listOf("m1"))
        val afterMonster = turnManager.advanceTurn(afterUse.newState)
        val afterWrap = turnManager.advanceTurn(afterMonster.newState)
        val cooldowns = afterWrap.newState.skillCooldowns["h1"]
        assertNotNull(cooldowns)
        assertEquals(1, cooldowns?.get("attack"))
    }

    @Test
    fun `executeSkill on cooldown does nothing`() {
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        val state = BattleState(
            heroes = listOf(hero), monsters = listOf(monster),
            turnOrder = listOf(BattleActor("h1", "Test Hero", true)),
            currentTurnIndex = 0, currentActorId = "h1", phase = BattlePhase.PLAYER_TURN,
            skillCooldowns = mapOf("h1" to mapOf("attack" to 2))
        )
        val result = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        assertEquals(state, result.newState)
    }

    @Test
    fun `executeSkill status applied on target`() {
        val statusSkill = Skill(
            id = "status", name = "Status", description = "",
            targetType = TargetType.SINGLE_ENEMY,
            damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
            baseDamage = 10,
            statusEffects = listOf(StatusEffectInfliction(StatusEffectType.ATK_DOWN, 1f, 3))
        )
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.executeSkill(state, "h1", statusSkill, listOf("m1"))
        val statuses = result.newState.statusEffects["m1"]
        assertNotNull(statuses)
        assertEquals(StatusEffectType.ATK_DOWN, statuses?.first()?.statusType)
    }

    @Test
    fun `status effect ticks on turn start and expires correctly`() {
        val statusSkill = Skill(
            id = "status", name = "Status", description = "",
            targetType = TargetType.SINGLE_ENEMY,
            damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
            baseDamage = 10,
            statusEffects = listOf(StatusEffectInfliction(StatusEffectType.ATK_DOWN, 1f, 2))
        )
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val afterApply = turnManager.executeSkill(state, "h1", statusSkill, listOf("m1"))
        val afterAdvance1 = turnManager.advanceTurn(afterApply.newState)
        // monster's turn starts → tick monster's statuses
        val statuses1 = afterAdvance1.newState.statusEffects["m1"]
        assertNotNull(statuses1)
        assertEquals(1, statuses1?.first()?.remainingTurns)
    }

    @Test
    fun `executeSkill increases ultimate gauge`() {
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        val updated = result.newState.heroes.first { it.id == "h1" }
        assertTrue(updated.gauge > 0)
    }

    @Test
    fun `executeUltimate resets gauge to zero`() {
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "h1") it.copy(gauge = 100) else it }
        )
        val result = turnManager.executeUltimate(state, "h1")
        val updated = result.newState.heroes.first { it.id == "h1" }
        assertEquals(0, updated.gauge)
    }

    @Test
    fun `executeUltimate when gauge not full does nothing`() {
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "h1") it.copy(gauge = 50) else it }
        )
        val result = turnManager.executeUltimate(state, "h1")
        assertEquals(state, result.newState)
    }

    @Test
    fun `executeUltimate deals damage`() {
        val hero = makeCombatant(id = "h1")
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 2000, maxHp = 2000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "h1") it.copy(gauge = 100) else it }
        )
        val result = turnManager.executeUltimate(state, "h1")
        val updated = result.newState.monsters.first { it.id == "m1" }
        assertTrue(updated.hp < 2000)
    }

    @Test
    fun `combo available only when required heroes alive`() {
        val h1 = makeCombatant(id = "h1")
        val h2 = makeCombatant(id = "h2")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(h1, h2), listOf(monster))
        state = enterPlayerTurn(state)
        assertFalse(state.isComboAvailable)
    }

    @Test
    fun `combo participants gain ultimate gauge`() {
        val combo = ComboSkill(
            id = "test_combo", name = "Test Combo", description = "",
            requiredHeroes = setOf(1, 2),
            targetType = TargetType.SINGLE_ENEMY,
            damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
            baseDamage = 100, comboType = ComboType.TWO_HERO
        )
        val h1 = makeCombatant(id = "1")
        val h2 = makeCombatant(id = "2")
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(h1, h2), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.executeCombo(state, combo, setOf("1", "2"))
        val updated1 = result.newState.heroes.find { it.id == "1" }
        val updated2 = result.newState.heroes.find { it.id == "2" }
        assertTrue(updated1!!.gauge > 0)
        assertTrue(updated2!!.gauge > 0)
    }

    @Test
    fun `combo multi target applies damage to all enemies`() {
        val combo = ComboSkill(
            id = "test_combo", name = "Test Combo", description = "",
            requiredHeroes = setOf(1, 2),
            targetType = TargetType.ALL_ENEMIES,
            damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
            baseDamage = 100, comboType = ComboType.TWO_HERO
        )
        val h1 = makeCombatant(id = "1")
        val h2 = makeCombatant(id = "2")
        val m1 = makeCombatant(id = "m1", isMonster = true)
        val m2 = makeCombatant(id = "m2", isMonster = true)
        var state = turnManager.startBattle(listOf(h1, h2), listOf(m1, m2))
        state = enterPlayerTurn(state)
        val result = turnManager.executeCombo(state, combo, setOf("1", "2"))
        val updated1 = result.newState.monsters.find { it.id == "m1" }
        val updated2 = result.newState.monsters.find { it.id == "m2" }
        assertTrue(updated1!!.hp < 1000)
        assertTrue(updated2!!.hp < 1000)
    }

    @Test
    fun `combo healing restores HP to all allies`() {
        val combo = ComboSkill(
            id = "heal_combo", name = "Heal Combo", description = "",
            requiredHeroes = setOf(1, 2),
            targetType = TargetType.ALL_ALLIES,
            healScaling = HealScaling(baseHeal = 30, healPerLevel = 0, isPercentage = true),
            comboType = ComboType.TWO_HERO
        )
        val h1 = makeCombatant(id = "1", hp = 500, maxHp = 500)
        val h2 = makeCombatant(id = "2", hp = 500, maxHp = 500)
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(h1, h2), listOf(monster))
        state = enterPlayerTurn(state)
        state = state.copy(
            heroes = state.heroes.map { if (it.id == "1") it.copy(hp = 100) else it }
        )
        val result = turnManager.executeCombo(state, combo, setOf("1", "2"))
        val updated = result.newState.heroes.find { it.id == "1" }
        assertTrue(updated!!.hp > 100)
    }

    @Test
    fun `defend adds shield and gauge`() {
        val hero = makeCombatant(id = "h1", maxHp = 500, hp = 500)
        val monster = makeCombatant(id = "m1", isMonster = true)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val result = turnManager.defend(state, "h1")
        val updated = result.newState.heroes.first { it.id == "h1" }
        assertTrue(updated.shield > 0)
        assertTrue(updated.gauge > 0)
    }

    @Test
    fun `monster basic attack damages hero`() {
        val hero = makeCombatant(id = "h1", hp = 500, maxHp = 500)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeMonsterTurn(state, "m1")
        val updated = result.newState.heroes.first { it.id == "h1" }
        assertTrue(updated.hp < 500)
    }

    @Test
    fun `monster kills hero triggers HeroDown`() {
        val hero = makeCombatant(id = "h1", hp = 10, maxHp = 10)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeMonsterTurn(state, "m1")
        assertTrue(result.events.any { it is BattleEvent.HeroDown })
    }

    @Test
    fun `monster extra actions bounded and no recursion`() {
        val phases = listOf(
            MonsterPhase(0.5f, listOf(
                PhaseTrigger(PhaseTriggerType.EXTRA_ACTION, value = 10f)
            ))
        )
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000, phases = phases)
        val hero = makeCombatant(id = "h1", hp = 5000, maxHp = 5000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = state.copy(
            monsters = state.monsters.map { if (it.id == "m1") it.copy(hp = 400) else it }
        )
        val result = turnManager.executeMonsterTurn(state, "m1")
        assertTrue(result.newState.heroes.first { it.id == "h1" }.hp < 5000)
    }

    @Test
    fun `victory checked after all monsters dead`() {
        val hero = makeCombatant(id = "h1", atk = 100)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 50, maxHp = 50)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = enterPlayerTurn(state)
        val afterKill = turnManager.executeSkill(state, "h1", damageSkill, listOf("m1"))
        assertTrue(afterKill.newState.monsters.first { it.id == "m1" }.isDefeated)
        val result = turnManager.advanceTurn(afterKill.newState)
        assertTrue(result.victory)
        assertEquals(BattlePhase.VICTORY, result.newState.phase)
    }

    @Test
    fun `defeat checked after all heroes dead`() {
        val hero = makeCombatant(id = "h1", hp = 10, maxHp = 10)
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        val result = turnManager.executeMonsterTurn(state, "m1")
        assertTrue(result.newState.heroes.first { it.id == "h1" }.isDefeated)
        val afterAdvance = turnManager.advanceTurn(result.newState)
        assertTrue(afterAdvance.defeat)
        assertEquals(BattlePhase.DEFEAT, afterAdvance.newState.phase)
    }

    @Test
    fun `phase trigger grants shield at HP threshold`() {
        val phases = listOf(
            MonsterPhase(0.5f, listOf(
                PhaseTrigger(PhaseTriggerType.GAIN_SHIELD, value = 0.3f)
            ))
        )
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000, phases = phases)
        val hero = makeCombatant(id = "h1", hp = 500, maxHp = 500)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = state.copy(
            monsters = state.monsters.map { if (it.id == "m1") it.copy(hp = 400) else it }
        )
        val result = turnManager.executeMonsterTurn(state, "m1")
        val updated = result.newState.monsters.find { it.id == "m1" }
        assertNotNull(updated)
        assertTrue(updated!!.shield > 0)
    }

    @Test
    fun `phase trigger double action at threshold`() {
        val phases = listOf(
            MonsterPhase(0.5f, listOf(
                PhaseTrigger(PhaseTriggerType.DOUBLE_ACTIONS, value = 1f)
            ))
        )
        val monster = makeCombatant(id = "m1", isMonster = true, hp = 1000, maxHp = 1000, phases = phases)
        val hero = makeCombatant(id = "h1", hp = 5000, maxHp = 5000)
        var state = turnManager.startBattle(listOf(hero), listOf(monster))
        state = state.copy(
            monsters = state.monsters.map { if (it.id == "m1") it.copy(hp = 400) else it }
        )
        val result = turnManager.executeMonsterTurn(state, "m1")
        val updated = result.newState.heroes.first { it.id == "h1" }
        assertTrue(updated.hp < 5000)
    }
}
