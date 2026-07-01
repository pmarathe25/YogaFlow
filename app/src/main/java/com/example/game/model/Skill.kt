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
    val description: String, // Flavor text
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
    val ultimateGain: Int = 20,
    val cooldown: Int = 0
) {
    fun getMechanicsDescription(heroLevel: Int = 1): String {
        val sb = StringBuilder()
        
        // Damage
        val totalBase = baseDamage + (damagePerLevel * heroLevel)
        if (totalBase > 0 || damageComponents.isNotEmpty()) {
            val component = damageComponents.firstOrNull()
            val type = component?.type ?: DamageType.PHYSICAL
            val element = component?.element ?: Element.NEUTRAL
            
            sb.append("Deals $totalBase ")
            if (element != Element.NEUTRAL) {
                sb.append("${element.name} ")
            }
            sb.append("${type.name.lowercase().replaceFirstChar { it.uppercase() }} damage")
            if (hits > 1) sb.append(" over $hits hits")
            sb.append(". ")
        }
        
        // Healing
        healScaling?.let { h ->
            val amount = h.baseHeal + (h.healPerLevel * heroLevel)
            sb.append("Heals ")
            if (h.isPercentage) sb.append("$amount% Max HP") else sb.append("$amount HP")
            sb.append(". ")
        }
        
        // Shielding
        shieldScaling?.let { s ->
            val amount = s.baseShield + (s.shieldPerLevel * heroLevel)
            sb.append("Grants ")
            if (s.isPercentage) sb.append("${(s.percentage * 100).toInt()}% Max HP") else sb.append("$amount HP")
            sb.append(" Shield. ")
        }
        
        // Buffs/Debuffs
        buffs.forEach { b ->
            val valPct = (b.value * 100).toInt()
            sb.append("Increases ${b.type.name.replace("_", " ").lowercase()} by $valPct% for ${b.duration} turns. ")
        }
        
        statusEffects.forEach { s ->
            val chancePct = (s.chance * 100).toInt()
            sb.append("$chancePct% chance to inflict ${s.type.name.replace("_", " ").lowercase()} for ${s.duration} turns. ")
        }
        
        if (cleanse) sb.append("Cleanses negative effects. ")
        if (revive) sb.append("Revives fallen allies. ")
        
        return sb.toString().trim()
    }
}

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
