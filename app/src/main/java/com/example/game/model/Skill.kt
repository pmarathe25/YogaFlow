package com.example.game.model

enum class Element {
    FIRE, WATER, AIR, EARTH, LIGHT, DARK, SHADOW, ELECTRIC, VOID, NEUTRAL
}

enum class DamageType {
    PHYSICAL, ELEMENTAL
}

enum class TargetType {
    SELF, SINGLE_ALLY, SINGLE_ENEMY, ALL_ALLIES, ALL_ENEMIES, ALL
}

enum class ActionSpeed {
    FAST, NORMAL, SLOW
}

data class DamageComponent(
    val type: DamageType,
    val element: Element? = null,
    val percentage: Int = 100
)

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val targetType: TargetType,
    val damageComponents: List<DamageComponent> = emptyList(),
    val baseDamage: Int = 0,
    val damagePerLevel: Int = 0,
    val hits: Int = 1,
    val healScaling: HealScaling? = null,
    val shieldScaling: ShieldScaling? = null,
    val statusEffects: List<StatusEffectInfliction> = emptyList(),
    val buffs: List<BuffApplication> = emptyList(),
    val cleanse: Boolean = false,
    val revive: Boolean = false,
    val speedWeight: ActionSpeed = ActionSpeed.NORMAL,
    val ultimateGain: Int = 20
)

data class HealScaling(
    val baseHeal: Int,
    val healPerLevel: Int,
    val isPercentage: Boolean = false,
    val targetMissingHpBonus: Float = 0f
)

data class ShieldScaling(
    val baseShield: Int,
    val shieldPerLevel: Int,
    val isPercentage: Boolean = false,
    val percentage: Float = 0f
)

data class BuffApplication(
    val type: StatusEffectType,
    val duration: Int = 3,
    val value: Float = 0.25f,
    val targetsParty: Boolean = false,
    val stacksPermanently: Boolean = false,
    val permanentValue: Int = 0
)
