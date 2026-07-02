package com.example.game.viewmodel

import com.example.game.battle.DefaultRandomProvider
import com.example.game.battle.RandomProvider
import com.example.game.model.*
import kotlin.math.*

object BattleEngine {

    data class DamageResult(
        val amount: Int,
        val isCrit: Boolean,
        val isDodged: Boolean,
        val breakdown: DamageBreakdown
    )

    data class SkillOutcomeResult(
        val outcome: ActionOutcome,
        val events: List<BattleEvent>
    )

    private val elementChart: Map<Element, Map<Element, Float>> = mapOf(
        Element.FIRE to mapOf(Element.AIR to 1.5f, Element.WATER to 0.5f),
        Element.WATER to mapOf(Element.FIRE to 1.5f, Element.EARTH to 0.5f),
        Element.AIR to mapOf(Element.EARTH to 1.5f, Element.FIRE to 0.5f),
        Element.EARTH to mapOf(Element.WATER to 1.5f, Element.AIR to 0.5f),
        Element.LIGHT to mapOf(Element.DARK to 1.5f, Element.SHADOW to 1.5f, Element.VOID to 0.5f),
        Element.DARK to mapOf(Element.LIGHT to 1.5f, Element.VOID to 0.5f),
        Element.SHADOW to mapOf(Element.LIGHT to 1.5f),
        Element.ELECTRIC to mapOf(Element.WATER to 1.5f, Element.EARTH to 0.5f),
        Element.VOID to mapOf(Element.LIGHT to 1.5f, Element.DARK to 1.5f)
    )

    fun getElementMultiplier(attacker: Element, defender: Element): Float {
        return elementChart[attacker]?.get(defender) ?: 1f
    }

    fun computeDamage(
        baseDamage: Int,
        attackerAtk: Int,
        attackerElement: Element,
        defenderElement: Element,
        damageComponent: DamageComponent? = null,
        atkBuffMultiplier: Float = 0f,
        isCrit: Boolean = false,
        isDodged: Boolean = false
    ): DamageResult {
        if (isDodged) {
            return DamageResult(0, isCrit = false, isDodged = true,
                DamageBreakdown(DamageType.PHYSICAL, null, 0))
        }

        val elementMult = getElementMultiplier(attackerElement, damageComponent?.element ?: defenderElement)
        val atkBonus = 1f + attackerAtk / 100f
        val buffMult = 1f + atkBuffMultiplier
        val pct = (damageComponent?.percentage ?: 100).toFloat()

        var total = (baseDamage * (pct / 100f) * elementMult * atkBonus * buffMult).toInt()

        val wasCrit = isCrit
        if (isCrit) {
            total = (total * 1.5f).toInt()
        }

        total = maxOf(total, 1)

        return DamageResult(
            amount = total,
            isCrit = wasCrit,
            isDodged = false,
            breakdown = DamageBreakdown(
                type = damageComponent?.type ?: DamageType.PHYSICAL,
                element = damageComponent?.element,
                amount = total
            )
        )
    }

    fun computeHeal(casterMaxHp: Int, scaling: HealScaling, level: Int = 1): Int {
        return if (scaling.isPercentage) {
            (scaling.baseHeal * casterMaxHp / 100).coerceAtLeast(1)
        } else {
            scaling.baseHeal + scaling.healPerLevel * level
        }
    }

    fun computeShield(casterMaxHp: Int, scaling: ShieldScaling, level: Int = 1): Int {
        return if (scaling.isPercentage && scaling.percentage > 0) {
            (scaling.percentage * casterMaxHp).toInt()
        } else if (scaling.isPercentage) {
            (scaling.baseShield * casterMaxHp / 100).coerceAtLeast(1)
        } else {
            scaling.baseShield + scaling.shieldPerLevel * level
        }
    }

    fun computeSkillOutcome(
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
                        if (isComponentNullified(state, component)) continue

                        val target = state.heroes.find { it.id == targetId && !it.isDefeated }
                            ?: state.monsters.find { it.id == targetId && !it.isDefeated }
                        if (target == null) continue

                        val defenderElement = target.element
                        val skillBase = skill.baseDamage + skill.damagePerLevel * combatant.level
                        val atkBuff = computeBuffMultiplier(state, combatant.id, StatusEffectType.ATK_UP)
                        val spdBuff = computeBuffMultiplier(state, combatant.id, StatusEffectType.SPD_UP)
                        val dmgReduction = computeBuffMultiplier(state, targetId, StatusEffectType.DAMAGE_REDUCTION)

                        val result = computeDamage(
                            baseDamage = skillBase,
                            attackerAtk = combatant.attack,
                            attackerElement = combatant.element,
                            defenderElement = defenderElement,
                            damageComponent = component,
                            atkBuffMultiplier = atkBuff + (spdBuff * 0.1f)
                        )

                        val finalAmount = (result.amount * (1f - dmgReduction)).toInt().coerceAtLeast(1)

                        tDmg += finalAmount
                        breakdown.add(result.breakdown.copy(amount = finalAmount))
                    }
                }
            }

            skill.healScaling?.let { scaling ->
                if (scaling.isPercentage || scaling.baseHeal > 0) {
                    val heal = computeHeal(combatant.maxHp, scaling, combatant.level)
                    if (heal > 0) tHeal += heal
                }
            }

            skill.shieldScaling?.let { scaling ->
                val shield = computeShield(combatant.maxHp, scaling, combatant.level)
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
                damageTypeBreakdown = breakdown, perTargetResult = perTarget
            ),
            events = emptyList()
        )
    }

    fun computeComboOutcome(
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
                    if (isComponentNullified(state, component)) continue
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

    fun computeMonsterOutcome(
        state: BattleState,
        monster: CombatantState,
        skill: Skill,
        targets: List<String>,
        rng: RandomProvider = DefaultRandomProvider
    ): SkillOutcomeResult {
        var totalDamage = 0
        val perTarget = mutableMapOf<String, TargetResult>()
        val breakdown = mutableListOf<DamageBreakdown>()

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
                        val elementMult = getElementMultiplier(monster.element, component.element ?: hero.element)
                        val dmgReduction = computeBuffMultiplier(state, hero.id, StatusEffectType.DAMAGE_REDUCTION)
                        val dmg = (skill.baseDamage * elementMult * (1f - dmgReduction)).toInt().coerceAtLeast(1)
                        tDmg += dmg
                        breakdown.add(DamageBreakdown(component.type, component.element, dmg))
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
                damageDealt = totalDamage, damageTypeBreakdown = breakdown, perTargetResult = perTarget
            ),
            events = emptyList()
        )
    }

    fun applyOutcome(
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

            result.statuses.forEach { sName ->
                val se = skill?.statusEffects?.find { it.type.name == sName }
                    ?: combo?.statusEffects?.find { it.type.name == sName }
                if (se != null) {
                    val existing = newStatusEffects[targetId] ?: emptyList()
                    newStatusEffects = newStatusEffects + (targetId to (existing + BattleStatus(targetId, se.type, se.duration, se.value)))
                }
            }

            if (result.cleansed) {
                newStatusEffects = newStatusEffects - targetId
            }
        }

        val buffs = skill?.buffs ?: combo?.buffs
        buffs?.forEach { buff ->
            val targetsToBuff = if (buff.targetsParty) state.aliveHeroes.map { it.id } else outcome.targets
            targetsToBuff.forEach { id ->
                val existing = newStatusEffects[id] ?: emptyList()
                newStatusEffects = newStatusEffects + (id to (existing + BattleStatus(id, buff.type, buff.duration, buff.value)))
            }
        }

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
                events.add(BattleEvent.MonsterDown(m.id))
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

    fun calculateTurnOrder(
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
    ): List<String> {
        val aliveHeroes = state.aliveHeroes
        val aliveMonsters = state.aliveMonsters
        return when (skill.targetType) {
            TargetType.SELF -> listOf(casterId)
            TargetType.SINGLE_ALLY -> {
                val targets = aliveHeroes.filter { it.id != casterId }
                if (targets.isEmpty()) listOf(casterId) else listOf(targets.first().id)
            }
            TargetType.SINGLE_ENEMY -> listOf(aliveMonsters.firstOrNull()?.id ?: return emptyList())
            TargetType.ALL_ALLIES -> aliveHeroes.map { it.id }
            TargetType.ALL_ENEMIES -> aliveMonsters.map { it.id }
            TargetType.ALL -> aliveHeroes.map { it.id } + aliveMonsters.map { it.id }
        }
    }

    fun checkPhaseTriggers(
        monster: CombatantState
    ): Pair<CombatantState, List<BattleEvent>> {
        val events = mutableListOf<BattleEvent>()
        var updated = monster
        for (i in updated.phases.indices) {
            val phase = updated.phases[i]
            if (updated.hpPercent <= phase.hpThreshold && updated.activePhase < i) {
                updated = updated.copy(activePhase = i)
                phase.triggers.forEach { trigger ->
                    events.add(BattleEvent.PhaseTriggered(updated.id, i, trigger))
                    when (trigger.type) {
                        PhaseTriggerType.EXTRA_ACTION -> updated = updated.copy(extraActionsThisRound = updated.extraActionsThisRound + 1)
                        PhaseTriggerType.DOUBLE_ACTIONS -> updated = updated.copy(extraActionsThisRound = 2)
                        PhaseTriggerType.GAIN_SHIELD -> updated = updated.copy(shield = updated.shield + (updated.maxHp * trigger.value).toInt())
                        else -> {}
                    }
                }
            }
        }
        return Pair(updated, events)
    }

    fun chooseMonsterTarget(
        combatants: List<CombatantState>,
        strategy: TargetStrategy,
        state: BattleState? = null,
        rng: RandomProvider = DefaultRandomProvider
    ): String {
        val alive = combatants.filter { !it.isDefeated }
        if (alive.isEmpty()) return ""
        return when (strategy) {
            TargetStrategy.RANDOM, TargetStrategy.RANDOM_HERO -> {
                val idx = rng.nextInt(alive.size)
                alive[idx].id
            }
            TargetStrategy.LOWEST_HP -> alive.minByOrNull { it.hp }!!.id
            TargetStrategy.HIGHEST_HP -> alive.maxByOrNull { it.hp }!!.id
            TargetStrategy.MOST_BUFFS -> {
                val s = state ?: return alive.maxByOrNull { it.hp }!!.id
                alive.maxByOrNull { hero ->
                    s.getStatusesForTarget(hero.id).size
                }!!.id
            }
        }
    }

    fun computeDamageBreakdown(
        damage: Int,
        type: DamageType,
        element: Element?
    ): DamageBreakdown {
        return DamageBreakdown(type = type, element = element, amount = damage)
    }

    private fun isComponentNullified(state: BattleState, component: DamageComponent): Boolean {
        return state.monsters.any { monster ->
            monster.phases.getOrNull(monster.activePhase)?.triggers?.any { trigger ->
                trigger.type == PhaseTriggerType.NULLIFY_ELEMENT && (
                    (trigger.summonMonsterId == "ALL" && component.type == DamageType.ELEMENTAL) ||
                    component.element?.name == trigger.summonMonsterId
                )
            } == true
        }
    }

    fun computeBuffMultiplier(state: BattleState, targetId: String, type: StatusEffectType): Float {
        return state.statusEffects[targetId]?.filter { it.statusType == type }
            ?.maxOfOrNull { it.value } ?: 0f
    }
}
