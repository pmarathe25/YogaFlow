package com.example.game.battle

import com.example.game.model.*
import com.example.game.model.BattlePhase.*

internal fun BattleState.isMonsterUntargetable(monsterId: String): Boolean {
    val monster = monsters.firstOrNull { it.id == monsterId } ?: return false
    if (monster.activePhase < 0) return false
    val phase = monster.phases.getOrNull(monster.activePhase)
    return phase?.triggers?.any { it.type == PhaseTriggerType.BECOME_UNTARGETABLE } == true
}

internal class MonsterAI(
    private val rng: RandomProvider,
    private val damageCalc: DamageCalculator
) {
    lateinit var reducer: BattleReducer

    fun monsterAct(input: BattleState, monsterId: String): TurnResult {
        if (input.phase != ENEMY_TURN || input.currentActorId != monsterId) return TurnResult(input)
        var state = input
        val logs = mutableListOf<String>()
        val events = mutableListOf<BattleEvent>()
        var actionsRemaining = 1
        var actionsTaken = 0
        val maxActions = 3

        while (actionsRemaining > 0 && actionsTaken < maxActions) {
            actionsRemaining--
            actionsTaken++
            val monster = state.monsters.firstOrNull { it.id == monsterId && !it.isDefeated } ?: break
            val (preState, preEvents) = checkPhaseTriggers(state, monsterId)
            state = preState
            events += preEvents
            logs += preEvents.filterIsInstance<BattleEvent.PhaseTriggered>().map { "${monster.name} shifts the arena." }

            val activeMonster = state.monsters.firstOrNull { it.id == monsterId && !it.isDefeated } ?: break
            val useSpecial = rng.nextFloat() < (activeMonster.aiBehavior?.specialChance ?: 0.3f) || activeMonster.turnsSinceLastSpecial >= 3
            val skill = if (useSpecial && activeMonster.specialAttack != null) activeMonster.specialAttack else Skill(
                id = "${monsterId}_attack",
                name = "Strike",
                description = "A direct attack.",
                targetType = TargetType.SINGLE_ENEMY,
                damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)),
                baseDamage = activeMonster.attack,
                ultimateGain = 0
            )
            state = state.withUpdatedMonster(monsterId) {
                it.copy(turnsSinceLastSpecial = if (useSpecial) 0 else it.turnsSinceLastSpecial + 1)
            }

            val target = chooseMonsterTarget(state.aliveHeroes, activeMonster.aiBehavior?.targetStrategy ?: TargetStrategy.RANDOM, state)
            if (target.isBlank()) break
            val targets = when (skill.targetType) {
                TargetType.ALL, TargetType.ALL_ENEMIES -> state.aliveHeroes.map { it.id }
                else -> listOf(target)
            }
            val outcomeResult = reducer.computeMonsterOutcome(state, activeMonster, skill, targets, rng)
            val (applied, applyEvents, updatedOutcome) = reducer.applyOutcome(state, outcomeResult.outcome)
            val turnEvent = BattleEvent.MonsterTurn(monsterId, skill, targets, updatedOutcome, activeMonster.element)
            state = applied.copy(eventLog = applied.eventLog + applyEvents + turnEvent)
            events += applyEvents + turnEvent
            logs += "${activeMonster.name} uses ${skill.name}."

            val postMonster = state.monsters.firstOrNull { it.id == monsterId && !it.isDefeated }
            if (postMonster != null) {
                val (afterTriggerState, postEvents) = checkPhaseTriggers(state, monsterId)
                val triggeredMonster = afterTriggerState.monsters.firstOrNull { it.id == monsterId } ?: postMonster
                val queued = triggeredMonster.extraActionsThisRound.coerceAtMost(maxActions - actionsTaken)
                state = afterTriggerState.withUpdatedMonster(monsterId) { it.copy(extraActionsThisRound = 0) }
                events += postEvents
                logs += postEvents.filterIsInstance<BattleEvent.PhaseTriggered>().map { "${triggeredMonster.name} gathers momentum." }
                actionsRemaining += queued
            }

            reducer.terminalResult(state, events, logs)?.let { return it }
        }

        return TurnResult(state.withComboAvailability(), logs, events)
    }

    fun chooseMonsterTarget(
        combatants: List<CombatantState>,
        strategy: TargetStrategy,
        state: BattleState? = null
    ): String {
        val alive = combatants.filter { !it.isDefeated }
        if (alive.isEmpty()) return ""
        return when (strategy) {
            TargetStrategy.RANDOM, TargetStrategy.RANDOM_HERO -> {
                val idx = rng.nextInt(alive.size)
                alive[idx].id
            }
            TargetStrategy.LOWEST_HP -> alive.minByOrNull { it.hp }?.id ?: ""
            TargetStrategy.HIGHEST_HP -> alive.maxByOrNull { it.hp }?.id ?: ""
            TargetStrategy.MOST_BUFFS -> {
                val s = state ?: return alive.maxByOrNull { it.hp }?.id ?: ""
                alive.maxByOrNull { hero ->
                    s.getStatusesForTarget(hero.id).size
                }?.id ?: ""
            }
        }
    }

    fun checkPhaseTriggers(
        state: BattleState,
        monsterId: String
    ): Pair<BattleState, List<BattleEvent>> {
        val events = mutableListOf<BattleEvent>()
        var monster = state.monsters.firstOrNull { it.id == monsterId && !it.isDefeated }
            ?: return Pair(state, emptyList())
        var statusEffects = state.statusEffects
        val addedMonsters = mutableListOf<CombatantState>()
        for (i in monster.phases.indices) {
            val phase = monster.phases[i]
            if (monster.hpPercent <= phase.hpThreshold && monster.activePhase < i) {
                monster = monster.copy(activePhase = i)
                phase.triggers.forEach { trigger ->
                    events.add(BattleEvent.PhaseTriggered(monster.id, i, trigger))
                    when (trigger.type) {
                        PhaseTriggerType.EXTRA_ACTION -> monster = monster.copy(extraActionsThisRound = monster.extraActionsThisRound + 1)
                        PhaseTriggerType.DOUBLE_ACTIONS -> monster = monster.copy(extraActionsThisRound = 2)
                        PhaseTriggerType.GAIN_SHIELD -> monster = monster.copy(shield = monster.shield + (monster.maxHp * trigger.value).toInt())
                        PhaseTriggerType.REFLECT_DAMAGE -> {
                            val existing = statusEffects[monster.id] ?: emptyList()
                            statusEffects = statusEffects + (monster.id to (existing + BattleStatus(monster.id, StatusEffectType.REFLECT, 999, trigger.value)))
                        }
                        PhaseTriggerType.BECOME_UNTARGETABLE -> {}
                        PhaseTriggerType.SUMMON_ADD -> {
                            val summonId = trigger.summonMonsterId ?: "summon_${monster.id}"
                            val summon = CombatantState(
                                id = summonId,
                                side = CombatSide.MONSTER,
                                name = summonId,
                                element = monster.element,
                                maxHp = 200,
                                hp = 200,
                                attack = 30,
                                speed = 50,
                                level = 1,
                                phases = listOf(MonsterPhase(1f, emptyList())),
                                aiBehavior = AIBehavior(specialChance = 0f)
                            )
                            addedMonsters.add(summon)
                        }
                        else -> {}
                    }
                }
            }
        }
        var newState = state.withUpdatedMonster(monsterId) { monster }
        newState = newState.copy(statusEffects = statusEffects)
        if (addedMonsters.isNotEmpty()) {
            newState = newState.copy(
                monsters = newState.monsters + addedMonsters,
                turnOrder = newState.turnOrder + addedMonsters.map { BattleActor(it.id, it.name, it.speed, false, it.element) }
            )
        }
        return Pair(newState, events)
    }
}