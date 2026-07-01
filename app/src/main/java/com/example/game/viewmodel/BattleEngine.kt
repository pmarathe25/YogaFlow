package com.example.game.viewmodel

import com.example.game.model.*
import kotlin.math.*
import kotlin.random.Random

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
        Element.FIRE to mapOf(
            Element.FIRE to 1.0f, Element.WATER to 0.5f, Element.AIR to 2.0f, Element.EARTH to 0.5f,
            Element.LIGHT to 1.0f, Element.DARK to 1.0f, Element.SHADOW to 1.0f, Element.ELECTRIC to 1.5f,
            Element.VOID to 1.0f, Element.NEUTRAL to 1.0f
        ),
        Element.WATER to mapOf(
            Element.FIRE to 2.0f, Element.WATER to 1.0f, Element.AIR to 0.5f, Element.EARTH to 0.5f,
            Element.LIGHT to 1.0f, Element.DARK to 1.0f, Element.SHADOW to 1.0f, Element.ELECTRIC to 1.5f,
            Element.VOID to 1.0f, Element.NEUTRAL to 1.0f
        ),
        Element.AIR to mapOf(
            Element.FIRE to 0.5f, Element.WATER to 2.0f, Element.AIR to 1.0f, Element.EARTH to 0.5f,
            Element.LIGHT to 1.0f, Element.DARK to 1.0f, Element.SHADOW to 1.0f, Element.ELECTRIC to 1.5f,
            Element.VOID to 1.0f, Element.NEUTRAL to 1.0f
        ),
        Element.EARTH to mapOf(
            Element.FIRE to 2.0f, Element.WATER to 2.0f, Element.AIR to 2.0f, Element.EARTH to 1.0f,
            Element.LIGHT to 1.0f, Element.DARK to 1.0f, Element.SHADOW to 1.0f, Element.ELECTRIC to 1.5f,
            Element.VOID to 1.0f, Element.NEUTRAL to 1.0f
        ),
        Element.LIGHT to mapOf(
            Element.FIRE to 1.0f, Element.WATER to 1.0f, Element.AIR to 1.0f, Element.EARTH to 1.0f,
            Element.LIGHT to 1.0f, Element.DARK to 2.0f, Element.SHADOW to 1.0f, Element.ELECTRIC to 1.0f,
            Element.VOID to 1.0f, Element.NEUTRAL to 1.0f
        ),
        Element.DARK to mapOf(
            Element.FIRE to 1.0f, Element.WATER to 1.0f, Element.AIR to 1.0f, Element.EARTH to 1.0f,
            Element.LIGHT to 2.0f, Element.DARK to 1.0f, Element.SHADOW to 1.0f, Element.ELECTRIC to 1.0f,
            Element.VOID to 1.0f, Element.NEUTRAL to 1.0f
        ),
        Element.SHADOW to mapOf(
            Element.FIRE to 1.0f, Element.WATER to 1.0f, Element.AIR to 1.0f, Element.EARTH to 1.0f,
            Element.LIGHT to 1.0f, Element.DARK to 1.0f, Element.SHADOW to 1.0f, Element.ELECTRIC to 1.0f,
            Element.VOID to 2.0f, Element.NEUTRAL to 1.0f
        ),
        Element.ELECTRIC to mapOf(
            Element.FIRE to 1.0f, Element.WATER to 1.0f, Element.AIR to 1.0f, Element.EARTH to 1.0f,
            Element.LIGHT to 1.0f, Element.DARK to 1.0f, Element.SHADOW to 1.0f, Element.ELECTRIC to 1.0f,
            Element.VOID to 2.0f, Element.NEUTRAL to 1.0f
        ),
        Element.VOID to mapOf(
            Element.FIRE to 1.0f, Element.WATER to 1.0f, Element.AIR to 1.0f, Element.EARTH to 1.0f,
            Element.LIGHT to 1.0f, Element.DARK to 1.0f,             Element.SHADOW to 0f, Element.ELECTRIC to 0.5f,
            Element.VOID to 1.0f, Element.NEUTRAL to 1.0f
        ),
        Element.NEUTRAL to mapOf(
            Element.FIRE to 1.0f, Element.WATER to 1.0f, Element.AIR to 1.0f, Element.EARTH to 1.0f,
            Element.LIGHT to 1.0f, Element.DARK to 1.0f, Element.SHADOW to 1.0f, Element.ELECTRIC to 1.0f,
            Element.VOID to 1.0f, Element.NEUTRAL to 1.0f
        )
    )

    fun getElementMultiplier(attacker: Element, defender: Element): Float {
        return elementChart[attacker]?.get(defender) ?: 1f
    }

    fun computeDamage(
        attackerAtk: Int,
        defenderDef: Int,
        attackerElement: Element,
        defenderElement: Element,
        damageComponent: DamageComponent? = null,
        baseDamage: Int = 0,
        isCrit: Boolean = false,
        isDodged: Boolean = false,
        variance: Float = 0.1f
    ): DamageResult {
        if (isDodged) {
            return DamageResult(0, isCrit = false, isDodged = true,
                DamageBreakdown(DamageType.PHYSICAL, null, 0))
        }

        var raw = (attackerAtk * 4) - (defenderDef * 2)
        raw = maxOf(raw, (attackerAtk * 0.5f).toInt())

        var total = raw + baseDamage

        if (damageComponent != null) {
            total = (total * damageComponent.percentage / 100f).toInt()
        }

        val defElement = damageComponent?.element ?: defenderElement
        val mult = getElementMultiplier(attackerElement, defElement)
        total = (total * mult).toInt()

        if (mult == 0f) {
            return DamageResult(0, isCrit = false, isDodged = false,
                DamageBreakdown(damageComponent?.type ?: DamageType.PHYSICAL, damageComponent?.element, 0))
        }

        val varianceAmount = (total * variance * (Random.nextFloat() * 2f - 1f)).toInt()
        total += varianceAmount

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

    fun computeHeal(casterAtk: Int, healScaling: HealScaling): Int {
        return if (healScaling.isPercentage) {
            (healScaling.baseHeal * casterAtk / 100).coerceAtLeast(1)
        } else {
            healScaling.baseHeal + healScaling.healPerLevel
        }
    }

    fun computeShield(casterAtk: Int, shieldScaling: ShieldScaling): Int {
        return if (shieldScaling.isPercentage && shieldScaling.percentage > 0) {
            (shieldScaling.percentage * casterAtk).toInt()
        } else if (shieldScaling.isPercentage) {
            (shieldScaling.baseShield * casterAtk / 100).coerceAtLeast(1)
        } else {
            shieldScaling.baseShield + shieldScaling.shieldPerLevel
        }
    }

    fun computeSkillOutcome(
        hero: HeroInstance,
        skill: Skill,
        state: BattleState,
        targets: List<String>
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

                        val result = computeDamage(
                            attackerAtk = hero.atk,
                            defenderDef = 0,
                            attackerElement = hero.element,
                            defenderElement = defenderElement,
                            damageComponent = component,
                            baseDamage = skillBase
                        )
                        tDmg += result.amount
                        breakdown.add(result.breakdown)
                    }
                }
            }

            skill.healScaling?.let { scaling ->
                if (scaling.isPercentage || scaling.baseHeal > 0) {
                    val heal = computeHeal(hero.atk, scaling)
                    if (heal > 0) tHeal += heal
                }
            }

            skill.shieldScaling?.let { scaling ->
                val shield = computeShield(hero.atk, scaling)
                if (shield > 0) tShield += shield
            }

            skill.statusEffects.forEach { se ->
                if (Random.nextFloat() < se.chance) {
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
        targets: List<String>
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
                        val result = computeDamage(
                            attackerAtk = monster.atk,
                            defenderDef = 0,
                            attackerElement = monster.element,
                            defenderElement = hero.element,
                            damageComponent = component,
                            baseDamage = skill.baseDamage
                        )
                        tDmg += result.amount
                        breakdown.add(result.breakdown)
                    }
                }
            }

            skill.statusEffects.forEach { se ->
                if (Random.nextFloat() < se.chance) {
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
    ): BattleState {
        val skill = outcome.skillUsed

        outcome.perTargetResult.forEach { (targetId, result) ->
            val hero = state.heroes.find { it.heroId == targetId }
            val monster = state.monsters.find { it.monsterId == targetId }
            val target = hero ?: monster ?: return@forEach

            if (result.damage > 0) {
                val dmg = result.damage
                if (hero != null) {
                    if (hero.shield >= dmg) {
                        hero.shield -= dmg
                    } else {
                        val remaining = dmg - hero.shield
                        hero.shield = 0
                        hero.currentHp = (hero.currentHp - remaining).coerceAtLeast(0)
                    }
                } else if (monster != null) {
                    if (monster.shield >= dmg) {
                        monster.shield -= dmg
                    } else {
                        val remaining = dmg - monster.shield
                        monster.shield = 0
                        monster.currentHp = (monster.currentHp - remaining).coerceAtLeast(0)
                    }
                }
            }

            if (result.heal > 0 && hero != null) {
                hero.currentHp = (hero.currentHp + result.heal).coerceAtMost(hero.maxHp)
            }

            if (result.shield > 0 && hero != null) {
                hero.shield += result.shield
            }

            result.statuses.forEach { sName ->
                val se = skill?.statusEffects?.find { it.type.name == sName }
                if (se != null) {
                    val list = state.statusEffects.getOrPut(targetId) { mutableListOf() }
                    list.add(BattleStatus(targetId, se.type, se.duration, se.value))
                }
            }

            if (result.cleansed) {
                state.statusEffects.remove(targetId)
            }
        }

        skill?.buffs?.forEach { buff ->
            val targetsToBuff = if (buff.targetsParty) state.aliveHeroes.map { it.heroId } else outcome.targets
            targetsToBuff.forEach { id ->
                val list = state.statusEffects.getOrPut(id) { mutableListOf() }
                list.add(BattleStatus(id, buff.type, buff.duration, buff.value))
            }
        }

        if (skill?.revive == true) {
            val fallen = state.heroes.filter { it.isDead }
            fallen.forEach { h ->
                h.isDead = false
                val healPct = skill.healScaling?.let { if (it.isPercentage) it.baseHeal else 50 } ?: 50
                h.currentHp = (h.maxHp * healPct / 100).coerceAtLeast(1)
            }
        }

        state.heroes.forEach { h ->
            if (h.currentHp <= 0 && !h.isDead) {
                h.isDead = true
                h.currentHp = 0
                state.addEvent(BattleEvent.HeroDown(h.heroId))
            }
        }
        state.monsters.forEach { m ->
            if (m.currentHp <= 0 && !m.isDead) {
                m.isDead = true
                m.currentHp = 0
                state.addEvent(BattleEvent.MonsterDown(m.monsterId))
            }
        }

        return state
    }

    fun calculateTurnOrder(
        heroes: List<HeroInstance>,
        monsters: List<MonsterInstance>
    ): List<BattleActor> {
        val actors = mutableListOf<BattleActor>()
        heroes.filter { !it.isDead }.forEach { h ->
            actors.add(BattleActor(h.heroId, h.name, h.spd, true, h.element))
        }
        monsters.filter { !it.isDead }.forEach { m ->
            actors.add(BattleActor(m.monsterId, m.name, m.spd, false, m.element))
        }
        return actors.sortedByDescending { actor ->
            val variance = 1f + (Random.nextFloat() * 0.1f - 0.05f)
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
        state: BattleState
    ): List<BattleEvent> {
        val events = mutableListOf<BattleEvent>()
        state.monsters.forEach { monster ->
            val definition = MonsterDefinitions.getMonster(monster.monsterId) ?: return@forEach
            for (i in definition.phases.indices) {
                val phase = definition.phases[i]
                if (monster.hpPercent <= phase.hpThreshold && monster.activePhase < i) {
                    monster.activePhase = i
                    phase.triggers.forEach { trigger ->
                        events.add(BattleEvent.PhaseTriggered(monster.monsterId, i, trigger))
                        when (trigger.type) {
                            PhaseTriggerType.EXTRA_ACTION -> monster.extraActionsThisRound++
                            PhaseTriggerType.DOUBLE_ACTIONS -> monster.extraActionsThisRound = 2
                            PhaseTriggerType.GAIN_SHIELD -> monster.shield += (monster.maxHp * trigger.value).toInt()
                            else -> {}
                        }
                    }
                }
            }
        }
        return events
    }

    fun chooseMonsterTarget(
        heroes: List<HeroInstance>,
        strategy: TargetStrategy,
        state: BattleState? = null
    ): String {
        val alive = heroes.filter { !it.isDead }
        if (alive.isEmpty()) return ""
        return when (strategy) {
            TargetStrategy.RANDOM, TargetStrategy.RANDOM_HERO -> alive.random().heroId
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
            val def = MonsterDefinitions.getMonster(monster.monsterId)
            def?.phases?.getOrNull(monster.activePhase)?.triggers?.any { trigger ->
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
