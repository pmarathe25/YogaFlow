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
        hero: HeroInstance,
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

                        val target = state.heroes.find { it.heroId == targetId && !it.isDead }
                            ?: state.monsters.find { it.monsterId == targetId && !it.isDead }
                        if (target == null) continue

                        val defenderElement = if (target is HeroInstance) target.element
                            else (target as MonsterInstance).element
                        val skillBase = skill.baseDamage + skill.damagePerLevel * hero.level
                        val atkBuff = computeBuffMultiplier(state, hero.heroId, StatusEffectType.ATK_UP)
                        val spdBuff = computeBuffMultiplier(state, hero.heroId, StatusEffectType.SPD_UP)
                        val dmgReduction = computeBuffMultiplier(state, targetId, StatusEffectType.DAMAGE_REDUCTION)

                        val result = computeDamage(
                            baseDamage = skillBase,
                            attackerAtk = hero.atk,
                            attackerElement = hero.element,
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
                    val heal = computeHeal(hero.maxHp, scaling, hero.level)
                    if (heal > 0) tHeal += heal
                }
            }

            skill.shieldScaling?.let { scaling ->
                val shield = computeShield(hero.maxHp, scaling, hero.level)
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
                action = TurnAction.SKILL, actorId = hero.heroId, skillUsed = skill, targets = targets,
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
            state.heroes.find { it.heroId == id && !it.isDead }
        }
        if (participants.isEmpty()) return SkillOutcomeResult(
            ActionOutcome(action = TurnAction.COMBO, actorId = casterId),
            emptyList()
        )

        val avgLevel = participants.map { it.level }.average().toInt()

        val targets = when (combo.targetType) {
            TargetType.SELF -> listOf(casterId)
            TargetType.SINGLE_ALLY -> listOf(state.aliveHeroes.firstOrNull()?.heroId ?: casterId)
            TargetType.SINGLE_ENEMY -> listOf(state.aliveMonsters.firstOrNull()?.monsterId ?: "")
            TargetType.ALL_ALLIES -> state.aliveHeroes.map { it.heroId }
            TargetType.ALL_ENEMIES -> state.aliveMonsters.map { it.monsterId }
            TargetType.ALL -> state.aliveHeroes.map { it.heroId } + state.aliveMonsters.map { it.monsterId }
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
            val existing = perTarget[h.heroId]
            val healAmt = if (comboHeal > 0) comboHeal else 0
            val shieldAmt = if (comboShield > 0) comboShield else 0
            perTarget[h.heroId] = existing?.let {
                it.copy(heal = it.heal + healAmt, shield = it.shield + shieldAmt,
                    cleansed = it.cleansed || combo.cleanse)
            } ?: TargetResult(heal = healAmt, shield = shieldAmt, cleansed = combo.cleanse)
        }

        combo.buffs.forEach { buff ->
            val targetsToBuff = state.aliveHeroes.map { it.heroId }
            targetsToBuff.forEach { id ->
                appliedBuffs.add(buff.type.name)
            }
        }

        if (combo.revive) {
            state.heroes.filter { it.isDead }.forEach { h ->
                revived.add(h.heroId)
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
        monster: MonsterInstance,
        skill: Skill,
        targets: List<String>,
        rng: RandomProvider = DefaultRandomProvider
    ): SkillOutcomeResult {
        var totalDamage = 0
        val perTarget = mutableMapOf<String, TargetResult>()
        val breakdown = mutableListOf<DamageBreakdown>()

        for (targetId in targets) {
            val hero = state.heroes.find { it.heroId == targetId && !it.isDead } ?: continue
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
                        val dmgReduction = computeBuffMultiplier(state, hero.heroId, StatusEffectType.DAMAGE_REDUCTION)
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
                action = TurnAction.SKILL, actorId = monster.monsterId, skillUsed = skill, targets = targets,
                damageDealt = totalDamage, damageTypeBreakdown = breakdown, perTargetResult = perTarget
            ),
            events = emptyList()
        )
    }

    fun applyOutcome(
        state: BattleState,
        outcome: ActionOutcome
    ): Pair<BattleState, List<BattleEvent>> {
        val events = mutableListOf<BattleEvent>()
        val skill = outcome.skillUsed
        val combo = outcome.comboTriggered

        val heroUpdates = mutableMapOf<String, HeroInstance>()
        val monsterUpdates = mutableMapOf<String, MonsterInstance>()
        var newStatusEffects = state.statusEffects

        outcome.perTargetResult.forEach { (targetId, result) ->
            val hero = heroUpdates[targetId] ?: state.heroes.find { it.heroId == targetId }
            val monster = monsterUpdates[targetId] ?: state.monsters.find { it.monsterId == targetId }

            if (result.damage > 0) {
                if (hero != null) {
                    var h = hero
                    val dmg = result.damage
                    if (h.shield >= dmg) {
                        h = h.copy(shield = h.shield - dmg)
                    } else {
                        val remaining = dmg - h.shield
                        h = h.copy(shield = 0, currentHp = (h.currentHp - remaining).coerceAtLeast(0))
                    }
                    heroUpdates[targetId] = h
                } else if (monster != null) {
                    var m = monster
                    val dmg = result.damage
                    if (m.shield >= dmg) {
                        m = m.copy(shield = m.shield - dmg)
                    } else {
                        val remaining = dmg - m.shield
                        m = m.copy(shield = 0, currentHp = (m.currentHp - remaining).coerceAtLeast(0))
                    }
                    monsterUpdates[targetId] = m
                }
            }

            if (result.heal > 0 && hero != null) {
                val h = heroUpdates[targetId] ?: hero
                heroUpdates[targetId] = h.copy(currentHp = (h.currentHp + result.heal).coerceAtMost(h.maxHp))
            }

            if (result.shield > 0 && hero != null) {
                val h = heroUpdates[targetId] ?: hero
                heroUpdates[targetId] = h.copy(shield = h.shield + result.shield)
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
            val targetsToBuff = if (buff.targetsParty) state.aliveHeroes.map { it.heroId } else outcome.targets
            targetsToBuff.forEach { id ->
                val existing = newStatusEffects[id] ?: emptyList()
                newStatusEffects = newStatusEffects + (id to (existing + BattleStatus(id, buff.type, buff.duration, buff.value)))
            }
        }

        if (skill?.revive == true || combo?.revive == true) {
            val fallen = state.heroes.filter { it.isDead }
            fallen.forEach { h ->
                val scaling = skill?.healScaling ?: combo?.healScaling
                val healPct = scaling?.let { if (it.isPercentage) it.baseHeal else 50 } ?: 50
                val prev = heroUpdates[h.heroId] ?: h
                heroUpdates[h.heroId] = prev.copy(
                    isDead = false,
                    currentHp = (h.maxHp * healPct / 100).coerceAtLeast(1)
                )
            }
        }

        val newHeroes = state.heroes.map { h -> heroUpdates[h.heroId] ?: h }
        val newMonsters = state.monsters.map { m -> monsterUpdates[m.monsterId] ?: m }

        val finalHeroes = newHeroes.map { h ->
            if (h.currentHp <= 0 && !h.isDead) {
                events.add(BattleEvent.HeroDown(h.heroId))
                h.copy(isDead = true, currentHp = 0)
            } else {
                h
            }
        }
        val finalMonsters = newMonsters.map { m ->
            if (m.currentHp <= 0 && !m.isDead) {
                events.add(BattleEvent.MonsterDown(m.monsterId))
                m.copy(isDead = true, currentHp = 0)
            } else {
                m
            }
        }

        val newState = state.copy(
            heroes = finalHeroes,
            monsters = finalMonsters,
            statusEffects = newStatusEffects
        )

        return Pair(newState, events)
    }

    fun calculateTurnOrder(
        heroes: List<HeroInstance>,
        monsters: List<MonsterInstance>,
        rng: RandomProvider = DefaultRandomProvider
    ): List<BattleActor> {
        val actors = mutableListOf<BattleActor>()
        heroes.filter { !it.isDead }.forEach { h ->
            actors.add(BattleActor(h.heroId, h.name, h.spd, true, h.element))
        }
        monsters.filter { !it.isDead }.forEach { m ->
            actors.add(BattleActor(m.monsterId, m.name, m.spd, false, m.element))
        }
        return actors.sortedByDescending { actor ->
            val variance = 1f + (rng.nextFloat() * 0.1f - 0.05f)
            actor.speed * variance
        }
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
                val targets = aliveHeroes.filter { it.heroId != casterId }
                if (targets.isEmpty()) listOf(casterId) else listOf(targets.first().heroId)
            }
            TargetType.SINGLE_ENEMY -> listOf(aliveMonsters.firstOrNull()?.monsterId ?: return emptyList())
            TargetType.ALL_ALLIES -> aliveHeroes.map { it.heroId }
            TargetType.ALL_ENEMIES -> aliveMonsters.map { it.monsterId }
            TargetType.ALL -> aliveHeroes.map { it.heroId } + aliveMonsters.map { it.monsterId }
        }
    }

    fun checkPhaseTriggers(
        monster: MonsterInstance
    ): Pair<MonsterInstance, List<BattleEvent>> {
        val events = mutableListOf<BattleEvent>()
        var updated = monster
        for (i in updated.phases.indices) {
            val phase = updated.phases[i]
            if (updated.hpPercent <= phase.hpThreshold && updated.activePhase < i) {
                updated = updated.copy(activePhase = i)
                phase.triggers.forEach { trigger ->
                    events.add(BattleEvent.PhaseTriggered(updated.monsterId, i, trigger))
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
        heroes: List<HeroInstance>,
        strategy: TargetStrategy,
        state: BattleState? = null,
        rng: RandomProvider = DefaultRandomProvider
    ): String {
        val alive = heroes.filter { !it.isDead }
        if (alive.isEmpty()) return ""
        return when (strategy) {
            TargetStrategy.RANDOM, TargetStrategy.RANDOM_HERO -> {
                val idx = rng.nextInt(alive.size)
                alive[idx].heroId
            }
            TargetStrategy.LOWEST_HP -> alive.minByOrNull { it.currentHp }!!.heroId
            TargetStrategy.HIGHEST_HP -> alive.maxByOrNull { it.currentHp }!!.heroId
            TargetStrategy.MOST_BUFFS -> {
                val s = state ?: return alive.maxByOrNull { it.currentHp }!!.heroId
                alive.maxByOrNull { hero ->
                    s.getStatusesForTarget(hero.heroId).size
                }!!.heroId
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
