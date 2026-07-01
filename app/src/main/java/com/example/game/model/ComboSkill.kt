package com.example.game.model

data class ComboSkill(
    val id: String,
    val name: String,
    val description: String,
    val requiredHeroes: Set<String>,
    val targetType: TargetType,
    val damageComponents: List<DamageComponent> = emptyList(),
    val baseDamage: Int = 0,
    val damagePerLevel: Int = 0,
    val hits: Int = 1,
    val healScaling: HealScaling? = null,
    val shieldScaling: ShieldScaling? = null,
    val buffs: List<BuffApplication> = emptyList(),
    val statusEffects: List<StatusEffectInfliction> = emptyList(),
    val cleanse: Boolean = false,
    val revive: Boolean = false,
    val speedWeight: ActionSpeed = ActionSpeed.SLOW,
    val comboType: ComboType
)

enum class ComboType {
    TWO_HERO, THREE_HERO
}


