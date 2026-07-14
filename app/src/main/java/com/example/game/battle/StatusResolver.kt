package com.example.game.battle

import com.example.game.model.*

internal class StatusResolver {
    lateinit var reducer: BattleReducer

    fun resolveTurnStart(state: BattleState, actorId: String): Triple<BattleState, List<BattleEvent>, List<String>> {
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
            val (applied, applyEvents, _) = reducer.applyOutcome(next, outcome)
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

    fun applyReflect(
        state: BattleState,
        outcome: ActionOutcome,
        targetId: String,
        damage: Int,
        combatantUpdates: MutableMap<String, CombatantState>
    ) {
        val reflectStatus = state.statusEffects[targetId]?.firstOrNull { it.statusType == StatusEffectType.REFLECT }
        if (reflectStatus == null || reflectStatus.value <= 0f || outcome.actorId == targetId) return
        val reflectAmount = (damage * reflectStatus.value).toInt().coerceAtLeast(1)
        val attacker = combatantUpdates[outcome.actorId] ?: state.heroes.find { it.id == outcome.actorId }
            ?: state.monsters.find { it.id == outcome.actorId } ?: return
        val reflected = if (attacker.shield >= reflectAmount) {
            attacker.copy(shield = attacker.shield - reflectAmount)
        } else {
            val rem = reflectAmount - attacker.shield
            attacker.copy(shield = 0, hp = (attacker.hp - rem).coerceAtLeast(0))
        }
        combatantUpdates[outcome.actorId] = reflected
    }

    fun inflictStatuses(
        state: BattleState,
        skill: Skill?,
        combo: ComboSkill?,
        targetId: String,
        statuses: List<String>,
        current: Map<String, List<BattleStatus>>
    ): Map<String, List<BattleStatus>> {
        var newStatusEffects = current
        statuses.forEach { sName ->
            val se = skill?.statusEffects?.find { it.type.name == sName }
                ?: combo?.statusEffects?.find { it.type.name == sName }
            if (se != null) {
                val existing = newStatusEffects[targetId] ?: emptyList()
                newStatusEffects = newStatusEffects + (targetId to (existing + BattleStatus(targetId, se.type, se.duration, se.value)))
            }
        }
        return newStatusEffects
    }

    fun cleanseStatuses(
        targetId: String,
        cleansed: Boolean,
        current: Map<String, List<BattleStatus>>
    ): Map<String, List<BattleStatus>> {
        return if (cleansed) current - targetId else current
    }

    fun applyBuffs(
        state: BattleState,
        skill: Skill?,
        combo: ComboSkill?,
        outcome: ActionOutcome,
        current: Map<String, List<BattleStatus>>
    ): Map<String, List<BattleStatus>> {
        var newStatusEffects = current
        val buffs = skill?.buffs ?: combo?.buffs
        buffs?.forEach { buff ->
            val targetsToBuff = if (buff.targetsParty) state.aliveHeroes.map { it.id } else outcome.targets
            targetsToBuff.forEach { id ->
                val existing = newStatusEffects[id] ?: emptyList()
                newStatusEffects = newStatusEffects + (id to (existing + BattleStatus(id, buff.type, buff.duration, buff.value)))
            }
        }
        return newStatusEffects
    }
}