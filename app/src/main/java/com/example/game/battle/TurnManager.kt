package com.example.game.battle

import com.example.game.model.*
import com.example.game.viewmodel.BattleEngine

data class TurnResult(
    val newState: BattleState,
    val logMessages: List<String> = emptyList(),
    val events: List<BattleEvent> = emptyList(),
    val victory: Boolean = false,
    val defeat: Boolean = false
)

data class AdvanceTurnResult(
    val newState: BattleState,
    val logMessages: List<String> = emptyList(),
    val events: List<BattleEvent> = emptyList(),
    val victory: Boolean = false,
    val defeat: Boolean = false
)

class TurnManager(private val rng: RandomProvider = DefaultRandomProvider) {

    fun startBattle(
        heroes: List<HeroInstance>,
        monsters: List<MonsterInstance>
    ): BattleState {
        val turnOrder = BattleEngine.calculateTurnOrder(heroes, monsters, rng)
        val firstActor = turnOrder.firstOrNull()
        val currentActorId = firstActor?.id ?: ""
        val phase = when {
            firstActor == null -> BattlePhase.VICTORY
            firstActor.isHero -> BattlePhase.PLAYER_TURN
            else -> BattlePhase.ENEMY_TURN
        }
        return BattleState(
            heroes = heroes,
            monsters = monsters,
            turnOrder = turnOrder,
            currentTurnIndex = 0,
            currentActorId = currentActorId,
            phase = phase
        )
    }

    fun advanceTurn(state: BattleState): AdvanceTurnResult {
        val logMessages = mutableListOf<String>()
        val events = mutableListOf<BattleEvent>()

        var currentState = state.copy(turnsTaken = state.turnsTaken + 1)

        val oldIndex = currentState.currentTurnIndex
        currentState = currentState.copy(currentTurnIndex = (oldIndex + 1) % currentState.turnOrder.size)
        val wrapped = currentState.currentTurnIndex <= oldIndex &&
                currentState.currentTurnIndex == 0 &&
                oldIndex + 1 >= currentState.turnOrder.size

        if (wrapped || currentState.turnOrder.isEmpty()) {
            currentState = currentState.advanceRound()
            currentState = currentState.copy(
                monsters = currentState.monsters.map { it.copy(extraActionsThisRound = 0) }
            )
            val newOrder = BattleEngine.calculateTurnOrder(currentState.heroes, currentState.monsters, rng)
            currentState = currentState.copy(turnOrder = newOrder, currentTurnIndex = 0)
        }

        if (currentState.aliveMonsters.isEmpty()) {
            events.add(BattleEvent.Victory(currentState.turnsTaken))
            logMessages.add("Victory!")
            return AdvanceTurnResult(
                newState = currentState.copy(phase = BattlePhase.VICTORY),
                logMessages = logMessages,
                events = events,
                victory = true
            )
        }

        if (currentState.aliveHeroes.isEmpty()) {
            events.add(BattleEvent.Defeat(currentState.round))
            logMessages.add("Defeat...")
            return AdvanceTurnResult(
                newState = currentState.copy(phase = BattlePhase.DEFEAT),
                logMessages = logMessages,
                events = events,
                defeat = true
            )
        }

        val nextActor = currentState.turnOrder[currentState.currentTurnIndex]
        currentState = currentState.copy(currentActorId = nextActor.id)

        val actorCds = currentState.skillCooldowns[nextActor.id]
        if (actorCds != null) {
            val updatedCds = actorCds.mapValues { (_, turns) ->
                (turns - 1).coerceAtLeast(0)
            }.filterValues { it > 0 }
            val newCooldowns = if (updatedCds.isEmpty()) {
                currentState.skillCooldowns - nextActor.id
            } else {
                currentState.skillCooldowns + (nextActor.id to updatedCds)
            }
            currentState = currentState.copy(skillCooldowns = newCooldowns)
        }

        val phase = if (nextActor.isHero) BattlePhase.PLAYER_TURN else BattlePhase.ENEMY_TURN
        currentState = currentState.copy(phase = phase)

        return AdvanceTurnResult(
            newState = currentState,
            logMessages = logMessages,
            events = events
        )
    }

    fun executeSkill(
        state: BattleState,
        heroId: String,
        skill: Skill,
        targets: List<String>
    ): TurnResult {
        val hero = state.heroes.find { it.heroId == heroId && !it.isDead }
            ?: return TurnResult(newState = state)

        val cooldowns = state.skillCooldowns[heroId] ?: emptyMap()
        if ((cooldowns[skill.id] ?: 0) > 0) return TurnResult(newState = state)

        val outcomeResult = BattleEngine.computeSkillOutcome(hero, skill, state, targets, rng)
        val (postApplyState, applyEvents) = BattleEngine.applyOutcome(state, outcomeResult.outcome)

        val logMessages = mutableListOf<String>()
        logMessages.add("${hero.name} uses ${skill.name}!")

        if (outcomeResult.outcome.healingDone > 0) {
            logMessages.add("${hero.name} heals for ${outcomeResult.outcome.healingDone} HP!")
        }
        if (outcomeResult.outcome.shieldApplied > 0) {
            logMessages.add("${hero.name} applies ${outcomeResult.outcome.shieldApplied} shield!")
        }

        val newStateWithGauge = postApplyState.copy(
            heroes = postApplyState.heroes.map { h ->
                if (h.heroId == heroId) {
                    h.copy(ultimateGauge = (h.ultimateGauge + skill.ultimateGain).coerceAtMost(100))
                } else h
            }
        )

        val newCooldowns = if (skill.cooldown > 0) {
            val existing = newStateWithGauge.skillCooldowns[heroId] ?: emptyMap()
            newStateWithGauge.skillCooldowns + (heroId to (existing + (skill.id to skill.cooldown)))
        } else {
            newStateWithGauge.skillCooldowns
        }

        val allEvents = applyEvents + BattleEvent.SkillUsed(heroId, skill, targets, listOf(outcomeResult.outcome))

        return TurnResult(
            newState = newStateWithGauge.copy(
                skillCooldowns = newCooldowns,
                eventLog = newStateWithGauge.eventLog + allEvents
            ),
            logMessages = logMessages,
            events = allEvents
        )
    }

    fun executeUltimate(
        state: BattleState,
        heroId: String
    ): TurnResult {
        val hero = state.heroes.find { it.heroId == heroId && !it.isDead }
            ?: return TurnResult(newState = state)
        if (hero.ultimateGauge < 100) return TurnResult(newState = state)

        val skill = hero.ultimate
        val targets = BattleEngine.resolveTargets(skill, heroId, state)
        if (targets.isEmpty()) return TurnResult(newState = state)

        val outcomeResult = BattleEngine.computeSkillOutcome(hero, skill, state, targets, rng)
        val (postApplyState, applyEvents) = BattleEngine.applyOutcome(state, outcomeResult.outcome)

        val logMessages = mutableListOf<String>()
        logMessages.add("${hero.name} unleashes ${skill.name}!")

        val newStateWithGauge = postApplyState.copy(
            heroes = postApplyState.heroes.map { h ->
                if (h.heroId == heroId) {
                    h.copy(ultimateGauge = 0)
                } else h
            }
        )

        val allEvents = applyEvents + BattleEvent.SkillUsed(heroId, skill, targets, listOf(outcomeResult.outcome))

        return TurnResult(
            newState = newStateWithGauge.copy(
                eventLog = newStateWithGauge.eventLog + allEvents
            ),
            logMessages = logMessages,
            events = allEvents
        )
    }

    fun executeCombo(
        state: BattleState,
        combo: ComboSkill,
        participantIds: Set<String>
    ): TurnResult {
        val participants = participantIds.mapNotNull { id ->
            state.heroes.find { it.heroId == id && !it.isDead }
        }
        if (participants.size != combo.requiredHeroes.size) return TurnResult(newState = state)
        if (participants.any { it.ultimateGauge < 100 }) return TurnResult(newState = state)

        val casterId = participants.first().heroId
        val partnerIds = participants.drop(1).map { it.heroId }

        val outcomeResult = BattleEngine.computeComboOutcome(combo, casterId, partnerIds, state)
        val (postApplyState, applyEvents) = BattleEngine.applyOutcome(state, outcomeResult.outcome)

        val logMessages = mutableListOf<String>()
        logMessages.add("Party unleashes ${combo.name}!")

        val newStateWithGauge = postApplyState.copy(
            heroes = postApplyState.heroes.map { h ->
                if (h.heroId in participantIds) {
                    h.copy(ultimateGauge = (h.ultimateGauge + 20).coerceAtMost(100))
                } else h
            }
        )

        val allEvents = applyEvents + BattleEvent.ComboUsed(participantIds, combo, outcomeResult.outcome.targets, outcomeResult.outcome)

        return TurnResult(
            newState = newStateWithGauge.copy(
                eventLog = newStateWithGauge.eventLog + allEvents
            ),
            logMessages = logMessages,
            events = allEvents
        )
    }

    fun executeMonsterTurn(
        state: BattleState,
        monsterId: String
    ): TurnResult {
        val logMessages = mutableListOf<String>()
        val allEvents = mutableListOf<BattleEvent>()
        var currentState = state

        val initialMonster = currentState.monsters.find { it.monsterId == monsterId && !it.isDead }
            ?: return TurnResult(newState = currentState)

        val (preMonster, preEvents) = BattleEngine.checkPhaseTriggers(initialMonster)
        currentState = currentState.withUpdatedMonster(initialMonster.monsterId) { preMonster }
        allEvents.addAll(preEvents)
        preEvents.filterIsInstance<BattleEvent.PhaseTriggered>().forEach {
            logMessages.add("${preMonster.name} triggers: ${it.trigger.type.name}!")
        }

        val currentMonster = currentState.monsters.find { it.monsterId == monsterId && !it.isDead }
            ?: return TurnResult(newState = currentState, logMessages = logMessages, events = allEvents)

        val useSpecial = rng.nextFloat() < currentMonster.aiBehavior.specialChance ||
                currentMonster.turnsSinceLastSpecial >= 3
        val (skill, updatedMonster) = if (useSpecial) {
            Pair(currentMonster.specialAttack, currentMonster.copy(turnsSinceLastSpecial = 0))
        } else {
            Pair(
                Skill("monster_attack", "Attack", "Basic attack.", TargetType.SINGLE_ENEMY,
                    damageComponents = listOf(DamageComponent(DamageType.PHYSICAL)), baseDamage = currentMonster.atk),
                currentMonster.copy(turnsSinceLastSpecial = currentMonster.turnsSinceLastSpecial + 1)
            )
        }
        currentState = currentState.withUpdatedMonster(monsterId) { updatedMonster }

        val targetId = BattleEngine.chooseMonsterTarget(
            currentState.aliveHeroes,
            updatedMonster.aiBehavior.targetStrategy,
            currentState,
            rng
        )
        if (targetId.isEmpty()) {
            return TurnResult(newState = currentState, logMessages = logMessages, events = allEvents)
        }

        val targets = if (skill.targetType == TargetType.ALL_ENEMIES || skill.targetType == TargetType.ALL) {
            currentState.aliveHeroes.map { it.heroId }
        } else {
            listOf(targetId)
        }
        if (targets.isEmpty()) {
            return TurnResult(newState = currentState, logMessages = logMessages, events = allEvents)
        }

        val outcomeResult = BattleEngine.computeMonsterOutcome(currentState, updatedMonster, skill, targets, rng)
        val (afterActionState, applyEvents) = BattleEngine.applyOutcome(currentState, outcomeResult.outcome)
        currentState = afterActionState
        allEvents.addAll(applyEvents)

        logMessages.add("${updatedMonster.name} uses ${skill.name}!")

        val postMonster = currentState.monsters.find { it.monsterId == monsterId && !it.isDead }
        if (postMonster != null) {
            val (postUpdatedMonster, postEvents) = BattleEngine.checkPhaseTriggers(postMonster)
            currentState = currentState.withUpdatedMonster(monsterId) { postUpdatedMonster }
            allEvents.addAll(postEvents)
            postEvents.filterIsInstance<BattleEvent.PhaseTriggered>().forEach {
                logMessages.add("${postUpdatedMonster.name} triggers: ${it.trigger.type.name}!")
            }

            if (postUpdatedMonster.extraActionsThisRound > 0) {
                currentState = currentState.withUpdatedMonster(monsterId) {
                    it.copy(extraActionsThisRound = it.extraActionsThisRound - 1)
                }
                val extraResult = executeMonsterTurn(currentState, monsterId)
                currentState = extraResult.newState
                logMessages.addAll(extraResult.logMessages)
                allEvents.addAll(extraResult.events)
            }
        }

        val monsterTurnEvent = BattleEvent.MonsterTurn(monsterId, skill, targets, outcomeResult.outcome)
        allEvents.add(monsterTurnEvent)

        return TurnResult(
            newState = currentState.copy(eventLog = currentState.eventLog + allEvents),
            logMessages = logMessages,
            events = allEvents
        )
    }
}
