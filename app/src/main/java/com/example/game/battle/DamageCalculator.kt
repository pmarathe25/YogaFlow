package com.example.game.battle

import com.example.game.model.*

internal object BattleTuning {
    const val CRIT_CHANCE = 0.1f
}

internal data class DamageResult(
    val amount: Int,
    val isCrit: Boolean,
    val isDodged: Boolean,
    val breakdown: DamageBreakdown
)

internal class DamageCalculator(private val rng: RandomProvider) {

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

    fun computeBuffMultiplier(state: BattleState, targetId: String, type: StatusEffectType): Float {
        return state.statusEffects[targetId]?.filter { it.statusType == type }
            ?.maxOfOrNull { it.value } ?: 0f
    }

    fun isComponentNullified(state: BattleState, component: DamageComponent): Boolean {
        return state.monsters.any { monster ->
            monster.phases.getOrNull(monster.activePhase)?.triggers?.any { trigger ->
                trigger.type == PhaseTriggerType.NULLIFY_ELEMENT && (
                    (trigger.summonMonsterId == "ALL" && component.type == DamageType.ELEMENTAL) ||
                    component.element?.name == trigger.summonMonsterId
                )
            } == true
        }
    }
}