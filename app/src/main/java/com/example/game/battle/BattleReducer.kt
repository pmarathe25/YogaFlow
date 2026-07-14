package com.example.game.battle

import com.example.game.model.*
import com.example.game.model.BattlePhase.*

internal data class SkillOutcomeResult(
    val outcome: ActionOutcome,
    val events: List<BattleEvent>
)

sealed class BattleCommand {
    data object AdvanceTurn : BattleCommand()
    data class UseSkill(val heroId: String, val skill: Skill, val targetIds: List<String>) : BattleCommand()
    data class UseUltimate(val heroId: String) : BattleCommand()
    data class UseCombo(val combo: ComboSkill, val participantIds: Set<String>) : BattleCommand()
    data class Defend(val heroId: String) : BattleCommand()
    data class MonsterAct(val monsterId: String) : BattleCommand()
}

class BattleReducer(private val rng: RandomProvider = DefaultRandomProvider) {
    private val damageCalc = DamageCalculator(rng)
    private val statusResolver = StatusResolver()
    private val monsterAI = MonsterAI(rng, damageCalc)
    private val targetResolver = TargetResolver()

    init {
        statusResolver.reducer = this
        monsterAI.reducer = this
    }

    fun startBattle(
        heroes: List<CombatantState>,
        monsters: List<CombatantState>
    ): BattleState {
        val readyHeroes = heroes.map {
            it.copy(
                hp = it.maxHp,
                shield = it.shield,
                gauge = 0,
                isDefeated = false
            )
        }
        val readyMonsters = monsters.map {
            it.copy(hp = it.maxHp, shield = 0, activePhase = -1, isDefeated = false, turnsSinceLastSpecial = 0, extraActionsThisRound = 0)
        }
        val queue = calculateTurnOrder(readyHeroes, readyMonsters, rng)
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
            is BattleCommand.MonsterAct -> monsterAI.monsterAct(state, command.monsterId)
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
            currentIndex += 1
            if (currentIndex >= queue.size) wrapped = true
        }

        if (wrapped) {
            state = state.copy(
                round = state.round + 1,
                monsters = state.monsters.map { it.copy(extraActionsThisRound = 0) }
            )
            queue = calculateTurnOrder(state.heroes, state.monsters, rng)
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

        val (afterStart, startEvents, startLogs) = statusResolver.resolveTurnStart(state, actor.id)
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

        val outcomeResult = computeSkillOutcome(hero, skill, state, targetIds, rng)
        val (applied, applyEvents, updatedOutcome) = applyOutcome(state, outcomeResult.outcome)
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
        val targets = resolveTargets(hero.ultimate!!, heroId, state)
        if (targets.isEmpty()) return TurnResult(state)

        val outcomeResult = computeSkillOutcome(hero, hero.ultimate!!, state, targets, rng)
        val (applied, applyEvents, updatedOutcome) = applyOutcome(state, outcomeResult.outcome)
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
        val requiredIds = combo.requiredHeroes.map(Int::toString).toSet()
        if (!participantIds.containsAll(requiredIds)) return TurnResult(state)
        val participants = participantIds.mapNotNull { id -> state.heroes.firstOrNull { it.id == id && !it.isDefeated } }
        if (participants.size != combo.requiredHeroes.size) return TurnResult(state)

        val casterId = state.currentActorId.takeIf { it in participantIds } ?: participants.first().id
        val partnerIds = requiredIds.filter { it != casterId }
        val outcomeResult = computeComboOutcome(combo, casterId, partnerIds, state)
        val (applied, applyEvents, updatedOutcome) = applyOutcome(state, outcomeResult.outcome)
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

    internal fun terminalResult(
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

    private fun TurnResult.prepend(events: List<BattleEvent>, logs: List<String>): TurnResult =
        copy(events = events + this.events, logMessages = logs + this.logMessages)

    private fun computeSkillOutcome(
        combatant: CombatantState,
        skill: Skill,
        state: BattleState,
        targets: List<String>,
        rng: RandomProvider = DefaultRandomProvider
    ): SkillOutcomeResult {
        var totalDamage = 0
        var totalHeal = 0
        var totalShield = 0
        val perTarget = mutableMapOf<String, TargetResult>()
        val breakdown = mutableListOf<DamageBreakdown>()
        val isCrit = rng.nextFloat() < BattleTuning.CRIT_CHANCE

        for (targetId in targets) {
            var tDmg = 0
            var tHeal = 0
            var tShield = 0
            val tStatuses = mutableListOf<String>()

            val damageComponents = if (skill.damageComponents.isEmpty() && skill.baseDamage > 0) {
                listOf(DamageComponent(DamageType.PHYSICAL))
            } else {
                skill.damageComponents
            }

            if (damageComponents.isNotEmpty()) {
                repeat(skill.hits.coerceAtLeast(1)) {
                    for (component in damageComponents) {
                        if (damageCalc.isComponentNullified(state, component)) continue

                        val target = state.heroes.find { it.id == targetId && !it.isDefeated }
                            ?: state.monsters.find { it.id == targetId && !it.isDefeated }
                        if (target == null) continue

                        val defenderElement = target.element
                        val skillBase = skill.baseDamage + skill.damagePerLevel * combatant.level
                        val atkBuff = damageCalc.computeBuffMultiplier(state, combatant.id, StatusEffectType.ATK_UP)
                        val spdBuff = damageCalc.computeBuffMultiplier(state, combatant.id, StatusEffectType.SPD_UP)
                        val dmgReduction = damageCalc.computeBuffMultiplier(state, targetId, StatusEffectType.DAMAGE_REDUCTION)

                        val result = damageCalc.computeDamage(
                            baseDamage = skillBase,
                            attackerAtk = combatant.attack,
                            attackerElement = combatant.element,
                            defenderElement = defenderElement,
                            damageComponent = component,
                            atkBuffMultiplier = atkBuff + (spdBuff * 0.1f),
                            isCrit = isCrit
                        )

                        val finalAmount = (result.amount * (1f - dmgReduction)).toInt().coerceAtLeast(1)

                        tDmg += finalAmount
                        breakdown.add(result.breakdown.copy(amount = finalAmount))
                    }
                }
            }

            skill.healScaling?.let { scaling ->
                if (scaling.isPercentage || scaling.baseHeal > 0) {
                    val heal = damageCalc.computeHeal(combatant.maxHp, scaling, combatant.level)
                    if (heal > 0) tHeal += heal
                }
            }

            skill.shieldScaling?.let { scaling ->
                val shield = damageCalc.computeShield(combatant.maxHp, scaling, combatant.level)
                if (shield > 0) tShield += shield
            }

            skill.statusEffects.forEach { se ->
                if (rng.nextFloat() < se.chance) {
                    tStatuses.add(se.type.name)
                }
            }

            perTarget[targetId] = TargetResult(
                damage = tDmg, heal = tHeal, shield = tShield,
                statuses = tStatuses, cleansed = skill.cleanse
            )

            totalDamage += tDmg
            totalHeal += tHeal
            totalShield += tShield
        }

        return SkillOutcomeResult(
            outcome = ActionOutcome(
                action = TurnAction.SKILL, actorId = combatant.id, skillUsed = skill, targets = targets,
                damageDealt = totalDamage, healingDone = totalHeal, shieldApplied = totalShield,
                wasCrit = isCrit,
                damageTypeBreakdown = breakdown, perTargetResult = perTarget
            ),
            events = emptyList()
        )
    }

    private fun computeComboOutcome(
        combo: ComboSkill,
        casterId: String,
        partnerIds: List<String>,
        state: BattleState
    ): SkillOutcomeResult {
        var totalDamage = 0
        var totalHeal = 0
        var totalShield = 0
        val appliedStatuses = mutableListOf<String>()
        val cleansedStatuses = mutableListOf<String>()
        val appliedBuffs = mutableListOf<String>()
        val revived = mutableListOf<String>()
        val breakdown = mutableListOf<DamageBreakdown>()
        val perTarget = mutableMapOf<String, TargetResult>()

        val participants = (listOf(casterId) + partnerIds).mapNotNull { id ->
            state.heroes.find { it.id == id && !it.isDefeated }
        }
        if (participants.isEmpty()) return SkillOutcomeResult(
            ActionOutcome(action = TurnAction.COMBO, actorId = casterId),
            emptyList()
        )

        val avgLevel = participants.map { it.level }.average().toInt()

        val targets = when (combo.targetType) {
            TargetType.SELF -> listOf(casterId)
            TargetType.SINGLE_ALLY -> listOf(state.aliveHeroes.firstOrNull()?.id ?: casterId)
            TargetType.SINGLE_ENEMY -> listOf(state.aliveMonsters.firstOrNull()?.id ?: "")
            TargetType.ALL_ALLIES -> state.aliveHeroes.map { it.id }
            TargetType.ALL_ENEMIES -> state.aliveMonsters.map { it.id }
            TargetType.ALL -> state.aliveHeroes.map { it.id } + state.aliveMonsters.map { it.id }
        }

        var comboHeal = 0
        combo.healScaling?.let { scaling ->
            val avgHero = participants.first()
            val heal = if (scaling.isPercentage) {
                (scaling.baseHeal * avgHero.maxHp / 100).coerceAtLeast(1)
            } else {
                scaling.baseHeal + scaling.healPerLevel * avgLevel
            }
            if (heal > 0) comboHeal = heal
        }

        var comboShield = 0
        combo.shieldScaling?.let { scaling ->
            val shield = if (scaling.isPercentage && scaling.percentage > 0) {
                (scaling.percentage * participants.first().maxHp).toInt()
            } else if (scaling.isPercentage) {
                (scaling.baseShield * participants.first().maxHp / 100).coerceAtLeast(1)
            } else {
                scaling.baseShield + scaling.shieldPerLevel * avgLevel
            }
            if (shield > 0) comboShield = shield
        }

        for (targetId in targets) {
            var tDmg = 0

            if (combo.damageComponents.isNotEmpty()) {
                for (component in combo.damageComponents) {
                    if (damageCalc.isComponentNullified(state, component)) continue
                    val dmg = combo.baseDamage + combo.damagePerLevel * avgLevel
                    tDmg += dmg
                    breakdown.add(DamageBreakdown(component.type, component.element, dmg))
                }
            }

            totalDamage += tDmg

            perTarget[targetId] = TargetResult(
                damage = tDmg, heal = 0, shield = 0,
                cleansed = combo.cleanse
            )

            if (combo.cleanse) {
                cleansedStatuses.add(targetId)
            }
        }

        totalHeal = comboHeal
        totalShield = comboShield

        state.aliveHeroes.forEach { h ->
            val existing = perTarget[h.id]
            val healAmt = if (comboHeal > 0) comboHeal else 0
            val shieldAmt = if (comboShield > 0) comboShield else 0
            perTarget[h.id] = existing?.let {
                it.copy(heal = it.heal + healAmt, shield = it.shield + shieldAmt,
                    cleansed = it.cleansed || combo.cleanse)
            } ?: TargetResult(heal = healAmt, shield = shieldAmt, cleansed = combo.cleanse)
        }

        combo.buffs.forEach { buff ->
            val targetsToBuff = state.aliveHeroes.map { it.id }
            targetsToBuff.forEach { id ->
                appliedBuffs.add(buff.type.name)
            }
        }

        if (combo.revive) {
            state.heroes.filter { it.isDefeated }.forEach { h ->
                revived.add(h.id)
            }
        }

        return SkillOutcomeResult(
            outcome = ActionOutcome(
                action = TurnAction.COMBO, actorId = casterId,
                skillUsed = null, targets = targets,
                damageDealt = totalDamage, healingDone = totalHeal, shieldApplied = totalShield,
                statusApplied = appliedStatuses, statusCleansed = cleansedStatuses,
                buffsApplied = appliedBuffs, revivals = revived,
                damageTypeBreakdown = breakdown, comboTriggered = combo,
                perTargetResult = perTarget
            ),
            events = emptyList()
        )
    }

    internal fun computeMonsterOutcome(
        state: BattleState,
        monster: CombatantState,
        skill: Skill,
        targets: List<String>,
        rng: RandomProvider = DefaultRandomProvider
    ): SkillOutcomeResult {
        var totalDamage = 0
        val perTarget = mutableMapOf<String, TargetResult>()
        val breakdown = mutableListOf<DamageBreakdown>()
        val isCrit = rng.nextFloat() < BattleTuning.CRIT_CHANCE

        for (targetId in targets) {
            val hero = state.heroes.find { it.id == targetId && !it.isDefeated } ?: continue
            var tDmg = 0
            val tStatuses = mutableListOf<String>()

            val damageComponents = if (skill.damageComponents.isEmpty() && skill.baseDamage > 0) {
                listOf(DamageComponent(DamageType.PHYSICAL))
            } else {
                skill.damageComponents
            }

            if (damageComponents.isNotEmpty()) {
                repeat(skill.hits.coerceAtLeast(1)) {
                    for (component in damageComponents) {
                        val dmgReduction = damageCalc.computeBuffMultiplier(state, hero.id, StatusEffectType.DAMAGE_REDUCTION)
                        val result = damageCalc.computeDamage(
                            baseDamage = skill.baseDamage,
                            attackerAtk = monster.attack,
                            attackerElement = monster.element,
                            defenderElement = hero.element,
                            damageComponent = component,
                            isCrit = isCrit
                        )
                        val finalAmount = (result.amount * (1f - dmgReduction)).toInt().coerceAtLeast(1)
                        tDmg += finalAmount
                        breakdown.add(DamageBreakdown(component.type, component.element, finalAmount))
                    }
                }
            }

            skill.statusEffects.forEach { se ->
                if (rng.nextFloat() < se.chance) {
                    tStatuses.add(se.type.name)
                }
            }

            perTarget[targetId] = TargetResult(damage = tDmg, statuses = tStatuses)
            totalDamage += tDmg
        }

        return SkillOutcomeResult(
            outcome = ActionOutcome(
                action = TurnAction.SKILL, actorId = monster.id, skillUsed = skill, targets = targets,
                damageDealt = totalDamage, wasCrit = isCrit,
                damageTypeBreakdown = breakdown, perTargetResult = perTarget
            ),
            events = emptyList()
        )
    }

    internal fun applyOutcome(
        state: BattleState,
        outcome: ActionOutcome
    ): Triple<BattleState, List<BattleEvent>, ActionOutcome> {
        val events = mutableListOf<BattleEvent>()
        val skill = outcome.skillUsed
        val combo = outcome.comboTriggered

        val combatantUpdates = mutableMapOf<String, CombatantState>()
        var newStatusEffects = state.statusEffects
        val updatedPerTarget = outcome.perTargetResult.toMutableMap()

        outcome.perTargetResult.forEach { (targetId, result) ->
            val target = combatantUpdates[targetId] ?: state.heroes.find { it.id == targetId }
                ?: state.monsters.find { it.id == targetId }
            var shieldDmg = 0

            if (result.damage > 0 && target != null) {
                var c = target
                val dmg = result.damage
                if (c.shield >= dmg) {
                    c = c.copy(shield = c.shield - dmg)
                    shieldDmg = dmg
                } else {
                    val remaining = dmg - c.shield
                    shieldDmg = c.shield
                    c = c.copy(shield = 0, hp = (c.hp - remaining).coerceAtLeast(0))
                }
                combatantUpdates[targetId] = c

                statusResolver.applyReflect(state, outcome, targetId, dmg, combatantUpdates)
            }

            if (shieldDmg > 0) {
                updatedPerTarget[targetId] = result.copy(shieldDamage = shieldDmg)
            }

            val healedTarget = combatantUpdates[targetId] ?: target
            if (result.heal > 0 && healedTarget != null && healedTarget.side == CombatSide.HERO) {
                combatantUpdates[targetId] = healedTarget.copy(hp = (healedTarget.hp + result.heal).coerceAtMost(healedTarget.maxHp))
            }

            val shieldedTarget = combatantUpdates[targetId] ?: target
            if (result.shield > 0 && shieldedTarget != null && shieldedTarget.side == CombatSide.HERO) {
                combatantUpdates[targetId] = shieldedTarget.copy(shield = shieldedTarget.shield + result.shield)
            }

            newStatusEffects = statusResolver.inflictStatuses(state, skill, combo, targetId, result.statuses, newStatusEffects)
            newStatusEffects = statusResolver.cleanseStatuses(targetId, result.cleansed, newStatusEffects)
        }

        newStatusEffects = statusResolver.applyBuffs(state, skill, combo, outcome, newStatusEffects)

        if (skill?.revive == true || combo?.revive == true) {
            val fallen = state.heroes.filter { it.isDefeated }
            fallen.forEach { h ->
                val scaling = skill?.healScaling ?: combo?.healScaling
                val healPct = scaling?.let { if (it.isPercentage) it.baseHeal else 50 } ?: 50
                val prev = combatantUpdates[h.id] ?: h
                combatantUpdates[h.id] = prev.copy(
                    isDefeated = false,
                    hp = (h.maxHp * healPct / 100).coerceAtLeast(1)
                )
            }
        }

        val newHeroes = state.heroes.map { h -> combatantUpdates[h.id] ?: h }
        val newMonsters = state.monsters.map { m -> combatantUpdates[m.id] ?: m }

        val finalHeroes = newHeroes.map { h ->
            if (h.hp <= 0 && !h.isDefeated) {
                events.add(BattleEvent.HeroDown(h.id))
                h.copy(isDefeated = true, hp = 0)
            } else {
                h
            }
        }
        val finalMonsters = newMonsters.map { m ->
            if (m.hp <= 0 && !m.isDefeated) {
                events.add(BattleEvent.MonsterDown(m.id, m.element))
                m.copy(isDefeated = true, hp = 0)
            } else {
                m
            }
        }

        val updatedOutcome = outcome.copy(perTargetResult = updatedPerTarget)

        val newState = state.copy(
            heroes = finalHeroes,
            monsters = finalMonsters,
            statusEffects = newStatusEffects
        )

        return Triple(newState, events, updatedOutcome)
    }

    private fun calculateTurnOrder(
        heroes: List<CombatantState>,
        monsters: List<CombatantState>,
        rng: RandomProvider = DefaultRandomProvider
    ): List<BattleActor> {
        val actors = mutableListOf<BattleActor>()
        heroes.filter { !it.isDefeated }.forEach { h ->
            actors.add(BattleActor(h.id, h.name, h.speed, true, h.element))
        }
        monsters.filter { !it.isDefeated }.forEach { m ->
            actors.add(BattleActor(m.id, m.name, m.speed, false, m.element))
        }
        val tiebreakers = actors.associate { it.id to rng.nextInt(Int.MAX_VALUE) }
        return actors.sortedWith(
            compareByDescending<BattleActor> { it.speed }
                .thenByDescending { it.isHero }
                .thenBy { tiebreakers[it.id] }
        )
    }

    fun resolveTargets(
        skill: Skill,
        casterId: String,
        state: BattleState
    ): List<String> = targetResolver.resolveTargets(skill, casterId, state)
}

internal fun BattleState.actorName(id: String): String =
    heroes.firstOrNull { it.id == id }?.name ?: monsters.firstOrNull { it.id == id }?.name ?: id

fun BattleState.withComboAvailability(): BattleState {
    val aliveHeroIds = aliveHeroes.map { it.id }.toSet()
    val combos = runCatching { com.example.game.persistence.DataLoader.combos }.getOrDefault(emptyList())
    return copy(isComboAvailable = combos.any { combo ->
        aliveHeroIds.containsAll(combo.requiredHeroes.map(Int::toString))
    })
}