package com.example.game.battle

import com.example.game.model.*
import com.example.game.model.BattlePhase.*
import com.example.game.viewmodel.BattleEngine

sealed class BattleCommand {
    data object AdvanceTurn : BattleCommand()
    data class UseSkill(val heroId: String, val skill: Skill, val targetIds: List<String>) : BattleCommand()
    data class UseUltimate(val heroId: String) : BattleCommand()
    data class UseCombo(val combo: ComboSkill, val participantIds: Set<String>) : BattleCommand()
    data class Defend(val heroId: String) : BattleCommand()
    data class MonsterAct(val monsterId: String) : BattleCommand()
}

class BattleReducer(private val rng: RandomProvider = DefaultRandomProvider) {
    fun startBattle(
        heroes: List<CombatantState>,
        monsters: List<CombatantState>
    ): BattleState {
        val readyHeroes = heroes.map {
            it.copy(
                hp = it.maxHp,
                shield = 0,
                gauge = 0,
                isDefeated = false
            )
        }
        val readyMonsters = monsters.map {
            it.copy(hp = it.maxHp, shield = 0, activePhase = -1, isDefeated = false, turnsSinceLastSpecial = 0, extraActionsThisRound = 0)
        }
        val queue = BattleEngine.calculateTurnOrder(readyHeroes, readyMonsters, rng)
        val first = queue.firstOrNull()
        val phase = when {
            first == null -> VICTORY
            first.isHero -> PLAYER_TURN
            else -> ENEMY_TURN
        }
        return BattleState(
            heroes = readyHeroes,
            monsters = readyMonsters,
            turnOrder = queue,
            currentTurnIndex = 0,
            currentActorId = first?.id.orEmpty(),
            phase = phase
        ).withComboAvailability()
    }

    fun reduce(state: BattleState, command: BattleCommand): TurnResult {
        return when (command) {
            BattleCommand.AdvanceTurn -> advanceTurn(state)
            is BattleCommand.UseSkill -> useSkill(state, command.heroId, command.skill, command.targetIds)
            is BattleCommand.UseUltimate -> useUltimate(state, command.heroId)
            is BattleCommand.UseCombo -> useCombo(state, command.combo, command.participantIds)
            is BattleCommand.Defend -> defend(state, command.heroId)
            is BattleCommand.MonsterAct -> monsterAct(state, command.monsterId)
        }
    }

    private fun advanceTurn(input: BattleState): TurnResult {
        terminalResult(input)?.let { return it }

        var state = input.copy(
            turnsTaken = input.turnsTaken + 1,
            pendingSkill = null,
            selectedTargets = emptyList()
        )
        terminalResult(state)?.let { return it }

        var queue = state.turnOrder.filter { actor -> state.isActorAlive(actor.id) }
        var currentIndex = queue.indexOfFirst { it.id == state.currentActorId }
        var wrapped = false

        if (queue.isEmpty()) {
            wrapped = true
        } else {
            if (currentIndex < 0) currentIndex = -1
            currentIndex += 1
            if (currentIndex >= queue.size) wrapped = true
        }

        if (wrapped) {
            state = state.copy(
                round = state.round + 1,
                monsters = state.monsters.map { it.copy(extraActionsThisRound = 0) }
            )
            queue = BattleEngine.calculateTurnOrder(state.heroes, state.monsters, rng)
            currentIndex = 0
        }

        val actor = queue.getOrNull(currentIndex)
            ?: return terminalResult(state.copy(turnOrder = queue, currentActorId = "", currentTurnIndex = 0))
                ?: TurnResult(state.copy(turnOrder = queue, phase = DEFEAT), defeat = true)

        state = state.copy(
            turnOrder = queue,
            currentTurnIndex = currentIndex,
            currentActorId = actor.id,
            phase = if (actor.isHero) PLAYER_TURN else ENEMY_TURN
        )

        val (afterStart, startEvents, startLogs) = resolveTurnStart(state, actor.id)
        state = afterStart.withComboAvailability()
        terminalResult(state, startEvents, startLogs)?.let { return it }

        val liveActor = state.turnOrder.getOrNull(state.currentTurnIndex)
        if (liveActor == null || !state.isActorAlive(liveActor.id)) {
            return advanceTurn(state).prepend(startEvents, startLogs)
        }

        return TurnResult(newState = state, logMessages = startLogs, events = startEvents)
    }

    private fun useSkill(
        state: BattleState,
        heroId: String,
        skill: Skill,
        targetIds: List<String>
    ): TurnResult {
        if (state.phase != PLAYER_TURN || state.currentActorId != heroId) return TurnResult(state)
        val hero = state.heroes.firstOrNull { it.id == heroId && !it.isDefeated } ?: return TurnResult(state)
        val cooldown = state.skillCooldowns[heroId]?.get(skill.id) ?: 0
        if (cooldown > 0 || targetIds.isEmpty()) return TurnResult(state)

        val outcomeResult = BattleEngine.computeSkillOutcome(hero, skill, state, targetIds, rng)
        val (applied, applyEvents, updatedOutcome) = BattleEngine.applyOutcome(state, outcomeResult.outcome)
        val withGauge = applied.copy(
            heroes = applied.heroes.map { h ->
                if (h.id == heroId) h.copy(gauge = (h.gauge + skill.ultimateGain).coerceAtMost(100)) else h
            }
        )
        val withCooldown = if (skill.cooldown > 0) {
            val actorCooldowns = withGauge.skillCooldowns[heroId].orEmpty() + (skill.id to skill.cooldown)
            withGauge.copy(skillCooldowns = withGauge.skillCooldowns + (heroId to actorCooldowns))
        } else {
            withGauge
        }
        val event = BattleEvent.SkillUsed(heroId, skill, targetIds, listOf(updatedOutcome))
        val next = withCooldown.copy(eventLog = withCooldown.eventLog + applyEvents + event).withComboAvailability()
        val logs = buildList {
            add("${hero.name} uses ${skill.name}.")
            if (updatedOutcome.damageDealt > 0) add("${updatedOutcome.damageDealt} damage.")
            if (updatedOutcome.healingDone > 0) add("${updatedOutcome.healingDone} HP restored.")
            if (updatedOutcome.shieldApplied > 0) add("${updatedOutcome.shieldApplied} shield gained.")
        }
        return terminalResult(next, applyEvents + event, logs) ?: TurnResult(next, logs, applyEvents + event)
    }

    private fun useUltimate(state: BattleState, heroId: String): TurnResult {
        if (state.phase != PLAYER_TURN || state.currentActorId != heroId) return TurnResult(state)
        val hero = state.heroes.firstOrNull { it.id == heroId && !it.isDefeated } ?: return TurnResult(state)
        if (hero.gauge < 100) return TurnResult(state)
        val targets = BattleEngine.resolveTargets(hero.ultimate!!, heroId, state)
        if (targets.isEmpty()) return TurnResult(state)

        val outcomeResult = BattleEngine.computeSkillOutcome(hero, hero.ultimate!!, state, targets, rng)
        val (applied, applyEvents, updatedOutcome) = BattleEngine.applyOutcome(state, outcomeResult.outcome)
        val next = applied.copy(
            heroes = applied.heroes.map { h -> if (h.id == heroId) h.copy(gauge = 0) else h },
            eventLog = applied.eventLog + applyEvents + BattleEvent.SkillUsed(heroId, hero.ultimate!!, targets, listOf(updatedOutcome))
        ).withComboAvailability()
        val events = applyEvents + BattleEvent.SkillUsed(heroId, hero.ultimate!!, targets, listOf(updatedOutcome))
        val logs = listOf("${hero.name} unleashes ${hero.ultimate!!.name}.")
        return terminalResult(next, events, logs) ?: TurnResult(next, logs, events)
    }

    private fun useCombo(
        state: BattleState,
        combo: ComboSkill,
        participantIds: Set<String>
    ): TurnResult {
        if (state.phase != PLAYER_TURN) return TurnResult(state)
        if (!participantIds.containsAll(combo.requiredHeroes)) return TurnResult(state)
        val participants = participantIds.mapNotNull { id -> state.heroes.firstOrNull { it.id == id && !it.isDefeated } }
        if (participants.size != combo.requiredHeroes.size) return TurnResult(state)

        val casterId = state.currentActorId.takeIf { it in participantIds } ?: participants.first().id
        val partnerIds = combo.requiredHeroes.filter { it != casterId }
        val outcomeResult = BattleEngine.computeComboOutcome(combo, casterId, partnerIds, state)
        val (applied, applyEvents, updatedOutcome) = BattleEngine.applyOutcome(state, outcomeResult.outcome)
        val withGauge = applied.copy(
            heroes = applied.heroes.map { h ->
                if (h.id in participantIds) h.copy(gauge = (h.gauge + 18).coerceAtMost(100)) else h
            }
        )
        val event = BattleEvent.ComboUsed(participantIds, combo, updatedOutcome.targets, updatedOutcome)
        val next = withGauge.copy(eventLog = withGauge.eventLog + applyEvents + event).withComboAvailability()
        val logs = listOf("Party links ${combo.name}.")
        return terminalResult(next, applyEvents + event, logs) ?: TurnResult(next, logs, applyEvents + event)
    }

    private fun defend(state: BattleState, heroId: String): TurnResult {
        if (state.phase != PLAYER_TURN || state.currentActorId != heroId) return TurnResult(state)
        val next = state.copy(
            heroes = state.heroes.map { h ->
                if (h.id == heroId && !h.isDefeated) {
                    h.copy(
                        shield = h.shield + (h.maxHp * 0.2f).toInt().coerceAtLeast(1),
                        gauge = (h.gauge + 10).coerceAtMost(100)
                    )
                } else h
            }
        )
        return TurnResult(next, listOf("${state.actorName(heroId)} defends."))
    }

    private fun monsterAct(input: BattleState, monsterId: String): TurnResult {
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
            val (preMonster, preEvents) = BattleEngine.checkPhaseTriggers(monster)
            state = state.withUpdatedMonster(monsterId) { preMonster }
            events += preEvents
            logs += preEvents.filterIsInstance<BattleEvent.PhaseTriggered>().map { "${preMonster.name} shifts the arena." }

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

            val target = BattleEngine.chooseMonsterTarget(state.aliveHeroes, activeMonster.aiBehavior?.targetStrategy ?: TargetStrategy.RANDOM, state, rng)
            if (target.isBlank()) break
            val targets = when (skill.targetType) {
                TargetType.ALL, TargetType.ALL_ENEMIES -> state.aliveHeroes.map { it.id }
                else -> listOf(target)
            }
            val outcomeResult = BattleEngine.computeMonsterOutcome(state, activeMonster, skill, targets, rng)
            val (applied, applyEvents, updatedOutcome) = BattleEngine.applyOutcome(state, outcomeResult.outcome)
            val turnEvent = BattleEvent.MonsterTurn(monsterId, skill, targets, updatedOutcome)
            state = applied.copy(eventLog = applied.eventLog + applyEvents + turnEvent)
            events += applyEvents + turnEvent
            logs += "${activeMonster.name} uses ${skill.name}."

            val postMonster = state.monsters.firstOrNull { it.id == monsterId && !it.isDefeated }
            if (postMonster != null) {
                val (triggeredMonster, postEvents) = BattleEngine.checkPhaseTriggers(postMonster)
                val queued = triggeredMonster.extraActionsThisRound.coerceAtMost(maxActions - actionsTaken)
                state = state.withUpdatedMonster(monsterId) { triggeredMonster.copy(extraActionsThisRound = 0) }
                events += postEvents
                logs += postEvents.filterIsInstance<BattleEvent.PhaseTriggered>().map { "${triggeredMonster.name} gathers momentum." }
                actionsRemaining += queued
            }

            terminalResult(state, events, logs)?.let { return it }
        }

        return TurnResult(state.withComboAvailability(), logs, events)
    }

    private fun resolveTurnStart(state: BattleState, actorId: String): Triple<BattleState, List<BattleEvent>, List<String>> {
        var next = state
        val events = mutableListOf<BattleEvent>()
        val logs = mutableListOf<String>()

        val actorCooldowns = next.skillCooldowns[actorId]
        if (actorCooldowns != null) {
            val ticked = actorCooldowns.mapValues { (_, turns) -> (turns - 1).coerceAtLeast(0) }.filterValues { it > 0 }
            next = next.copy(skillCooldowns = if (ticked.isEmpty()) next.skillCooldowns - actorId else next.skillCooldowns + (actorId to ticked))
        }

        val statuses = next.statusEffects[actorId].orEmpty()
        var burnDamage = 0
        statuses.forEach { status ->
            if (status.statusType == StatusEffectType.BURN) burnDamage += status.value.toInt().coerceAtLeast(1)
        }
        if (burnDamage > 0) {
            val outcome = ActionOutcome(
                action = TurnAction.SKILL,
                actorId = actorId,
                targets = listOf(actorId),
                perTargetResult = mapOf(actorId to TargetResult(damage = burnDamage)),
                damageDealt = burnDamage
            )
            val (applied, applyEvents, _) = BattleEngine.applyOutcome(next, outcome)
            next = applied
            events += applyEvents
            logs += "${next.actorName(actorId)} suffers burn damage."
        }

        val remainingStatuses = statuses.mapNotNull { status ->
            val remaining = status.remainingTurns - 1
            if (remaining > 0) status.copy(remainingTurns = remaining) else null
        }
        next = next.copy(statusEffects = if (remainingStatuses.isEmpty()) next.statusEffects - actorId else next.statusEffects + (actorId to remainingStatuses))
        return Triple(next, events, logs)
    }

    private fun terminalResult(
        state: BattleState,
        events: List<BattleEvent> = emptyList(),
        logs: List<String> = emptyList()
    ): TurnResult? {
        return when {
            state.aliveMonsters.isEmpty() -> {
                val event = BattleEvent.Victory(state.turnsTaken)
                TurnResult(state.copy(phase = VICTORY, eventLog = state.eventLog + event), logs + "Victory!", events + event, victory = true)
            }
            state.aliveHeroes.isEmpty() -> {
                val event = BattleEvent.Defeat(state.round)
                TurnResult(state.copy(phase = DEFEAT, eventLog = state.eventLog + event), logs + "Defeat.", events + event, defeat = true)
            }
            state.phase == VICTORY -> TurnResult(state, logs, events, victory = true)
            state.phase == DEFEAT -> TurnResult(state, logs, events, defeat = true)
            else -> null
        }
    }

    private fun BattleState.isActorAlive(id: String): Boolean =
        heroes.any { it.id == id && !it.isDefeated } || monsters.any { it.id == id && !it.isDefeated }

    private fun BattleState.actorName(id: String): String =
        heroes.firstOrNull { it.id == id }?.name ?: monsters.firstOrNull { it.id == id }?.name ?: id

    private fun TurnResult.prepend(events: List<BattleEvent>, logs: List<String>): TurnResult =
        copy(events = events + this.events, logMessages = logs + this.logMessages)
}

fun BattleState.withComboAvailability(): BattleState {
    val aliveHeroIds = aliveHeroes.map { it.id }.toSet()
    val combos = runCatching { com.example.game.persistence.DataLoader.combos }.getOrDefault(emptyList())
    return copy(isComboAvailable = combos.any { combo ->
        aliveHeroIds.containsAll(combo.requiredHeroes)
    })
}
