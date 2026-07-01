package com.example.game.model

import androidx.compose.ui.graphics.Color

enum class EquipmentSlot {
    WEAPON, ARMOR, ACCESSORY
}

enum class EquipmentTier {
    GENERIC, CLASS_SPECIFIC, UNIQUE
}

enum class HeroClass {
    HEALER, TANK, DPS, BUFFER, MAGE
}

data class Equipment(
    val id: String,
    val name: String,
    val slot: EquipmentSlot,
    val tier: EquipmentTier,
    val heroClass: HeroClass? = null,
    val heroId: String? = null,
    val minYogaLevel: Int = 1,
    val minHeroLevel: Int = 1,
    val sparkCost: Int = 0,
    val description: String = "",
    val effects: List<EquipmentEffect> = emptyList()
) {
    val yogaLevelRequired: Int get() = minYogaLevel
    val sparksCost: Int get() = sparkCost
    val goldCost: Int get() = sparkCost * when (tier) {
        EquipmentTier.GENERIC -> 5
        EquipmentTier.CLASS_SPECIFIC -> 8
        EquipmentTier.UNIQUE -> 10
    }
    
    val bonusDescription: String get() = effects.joinToString("\n") { effect ->
        val valueStr = if (effect.value > 0) "+${(effect.value * 100).toInt()}%" else "${(effect.value * 100).toInt()}%"
        when (effect.type) {
            EquipmentEffectType.ATK_PERCENT -> "Increases Attack by $valueStr"
            EquipmentEffectType.HP_PERCENT -> "Increases Max HP by $valueStr"
            EquipmentEffectType.SPD_PERCENT -> "Increases Speed by $valueStr"
            EquipmentEffectType.CRIT_CHANCE -> "Increases Critical Chance by $valueStr"
            EquipmentEffectType.CRIT_DAMAGE -> "Increases Critical Damage by $valueStr"
            EquipmentEffectType.HEAL_AMOUNT -> "Increases Healing Output by $valueStr"
            EquipmentEffectType.SHIELD_GAIN -> "Increases Shield Strength by $valueStr"
            EquipmentEffectType.ELEMENTAL_DAMAGE -> "Increases Elemental Damage by $valueStr"
            EquipmentEffectType.AOE_DAMAGE -> "Increases Area Damage by $valueStr"
            EquipmentEffectType.BUFF_DURATION -> "Increases Buff Duration by ${(effect.value).toInt()} turns"
            EquipmentEffectType.BUFF_AMOUNT -> "Increases Buff Effectiveness by $valueStr"
            EquipmentEffectType.STATUS_RESISTANCE -> "Increases Status Resistance by $valueStr"
            EquipmentEffectType.STATUS_DURATION_REDUCTION -> "Reduces Debuff Duration by ${(effect.value).toInt()} turns"
            EquipmentEffectType.INCOMING_HEALING -> "Increases Incoming Healing by $valueStr"
            EquipmentEffectType.ULTIMATE_GAIN_RATE -> "Increases Ultimate Charge Rate by $valueStr"
            EquipmentEffectType.BONUS_FIRE_ON_STRIKE_CHANCE -> "Grants $valueStr chance to deal Fire damage on hit"
            EquipmentEffectType.START_SHIELD_PERCENT -> "Starts battle with $valueStr Max HP as Shield"
            EquipmentEffectType.ALL_STATS_PERCENT -> "Increases All Stats by $valueStr"
            EquipmentEffectType.DOUBLE_STRIKE_CHANCE -> "Grants $valueStr chance to hit twice"
            EquipmentEffectType.PARTY_DAMAGE_REDUCTION -> "Reduces damage taken by Party by $valueStr"
            EquipmentEffectType.DAMAGE_PERCENT -> "Increases All Damage by $valueStr"
            EquipmentEffectType.HEAL_SHIELD_PERCENT -> "Converts $valueStr of Healing into Shield"
            EquipmentEffectType.SHIELD_HEAL_PERCENT -> "Converts $valueStr of Shield into Healing"
            EquipmentEffectType.BURN_CHANCE_ON_ULTIMATE -> "Grants $valueStr chance to Burn on Ultimate"
            EquipmentEffectType.DAMAGE_TO_BURNING -> "Increases damage against Burning foes by $valueStr"
            EquipmentEffectType.CLEANSE_ON_HEAL -> "Grants $valueStr chance to Cleanse on Heal"
            EquipmentEffectType.REVIVE_ON_ULTIMATE -> "Grants $valueStr chance to Revive on Ultimate"
            EquipmentEffectType.SPD_ON_ULTIMATE -> "Increases Speed by ${(effect.value).toInt()} after Ultimate"
            EquipmentEffectType.SPD_ON_SKILL -> "Increases Speed by ${(effect.value).toInt()} after using a Skill"
            EquipmentEffectType.SHIELD_ON_STRIKE -> "Grants $valueStr Max HP as Shield when attacking"
        }
    }

    fun getThemeColor(): Color {
        if (tier == EquipmentTier.UNIQUE && heroId != null) {
            return when (heroId) {
                "Shanti" -> Color(0xFF2196F3)
                "Santosha" -> Color(0xFF795548)
                "Virya" -> Color(0xFFF44336)
                "Dhairya" -> Color(0xFFFFEB3B)
                "Maitri" -> Color(0xFFE1F5FE)
                else -> Color.Gray
            }
        }
        return when (tier) {
            EquipmentTier.UNIQUE -> Color(0xFFFFD700)
            EquipmentTier.CLASS_SPECIFIC -> Color(0xFFB388FF)
            else -> Color.Gray
        }
    }

    fun getIcon(): String {
        return when (slot) {
            EquipmentSlot.WEAPON -> when (tier) {
                EquipmentTier.UNIQUE -> "🔱"
                EquipmentTier.CLASS_SPECIFIC -> "⚔️"
                else -> "🗡️"
            }
            EquipmentSlot.ARMOR -> when (tier) {
                EquipmentTier.UNIQUE -> "✨"
                EquipmentTier.CLASS_SPECIFIC -> "🛡️"
                else -> "🥋"
            }
            EquipmentSlot.ACCESSORY -> when (tier) {
                EquipmentTier.UNIQUE -> "📿"
                EquipmentTier.CLASS_SPECIFIC -> "💎"
                else -> "💠"
            }
        }
    }
}

data class EquipmentEffect(
    val type: EquipmentEffectType,
    val value: Float,
    val target: EffectTarget = EffectTarget.SELF
)

enum class EquipmentEffectType {
    ATK_PERCENT,
    HP_PERCENT,
    SPD_PERCENT,
    CRIT_CHANCE,
    CRIT_DAMAGE,
    HEAL_AMOUNT,
    SHIELD_GAIN,
    ELEMENTAL_DAMAGE,
    AOE_DAMAGE,
    BUFF_DURATION,
    BUFF_AMOUNT,
    STATUS_RESISTANCE,
    STATUS_DURATION_REDUCTION,
    INCOMING_HEALING,
    ULTIMATE_GAIN_RATE,
    BONUS_FIRE_ON_STRIKE_CHANCE,
    START_SHIELD_PERCENT,
    ALL_STATS_PERCENT,
    DOUBLE_STRIKE_CHANCE,
    PARTY_DAMAGE_REDUCTION,
    DAMAGE_PERCENT,
    HEAL_SHIELD_PERCENT,
    SHIELD_HEAL_PERCENT,
    BURN_CHANCE_ON_ULTIMATE,
    DAMAGE_TO_BURNING,
    CLEANSE_ON_HEAL,
    REVIVE_ON_ULTIMATE,
    SPD_ON_ULTIMATE,
    SPD_ON_SKILL,
    SHIELD_ON_STRIKE
}

enum class EffectTarget {
    SELF, PARTY, ALL_ENEMIES
}



data class SetBonus(
    val name: String,
    val description: String,
    val requiredItems: List<String>,
    val effects: List<EquipmentEffect>
)
