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

object EquipmentDefinitions {
    val genericWeapons = listOf(
        Equipment("training_blade", "Training Blade", EquipmentSlot.WEAPON, EquipmentTier.GENERIC, minYogaLevel = 1, sparkCost = 2,
            description = "A simple blade for basic training.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.ATK_PERCENT, 0.05f))),
        Equipment("crystal_sword", "Crystal Sword", EquipmentSlot.WEAPON, EquipmentTier.GENERIC, minYogaLevel = 3, sparkCost = 5,
            description = "A crystalline blade that sharpens focus.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.ATK_PERCENT, 0.10f), EquipmentEffect(EquipmentEffectType.CRIT_CHANCE, 0.03f))),
        Equipment("mythril_edge", "Mythril Edge", EquipmentSlot.WEAPON, EquipmentTier.GENERIC, minYogaLevel = 6, sparkCost = 10,
            description = "Legendary mythril forged for the worthy.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.ATK_PERCENT, 0.15f), EquipmentEffect(EquipmentEffectType.CRIT_CHANCE, 0.05f), EquipmentEffect(EquipmentEffectType.CRIT_DAMAGE, 0.10f)))
    )

    val genericArmors = listOf(
        Equipment("traveler_cloak", "Traveler's Cloak", EquipmentSlot.ARMOR, EquipmentTier.GENERIC, minYogaLevel = 1, sparkCost = 2,
            description = "A light cloak for the wandering yogi.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.HP_PERCENT, 0.05f))),
        Equipment("enchanted_robe", "Enchanted Robe", EquipmentSlot.ARMOR, EquipmentTier.GENERIC, minYogaLevel = 3, sparkCost = 5,
            description = "Robes woven with protective energies.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.HP_PERCENT, 0.10f), EquipmentEffect(EquipmentEffectType.STATUS_RESISTANCE, 0.05f))),
        Equipment("ethereal_vestment", "Ethereal Vestment", EquipmentSlot.ARMOR, EquipmentTier.GENERIC, minYogaLevel = 6, sparkCost = 10,
            description = "Armor that exists between realms.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.HP_PERCENT, 0.15f), EquipmentEffect(EquipmentEffectType.STATUS_RESISTANCE, 0.10f), EquipmentEffect(EquipmentEffectType.INCOMING_HEALING, 0.05f)))
    )

    val genericAccessories = listOf(
        Equipment("simple_beads", "Simple Beads", EquipmentSlot.ACCESSORY, EquipmentTier.GENERIC, minYogaLevel = 1, sparkCost = 3,
            description = "Beads that help track your breath.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.STATUS_DURATION_REDUCTION, 1f))),
        Equipment("swift_boots", "Swift Boots", EquipmentSlot.ACCESSORY, EquipmentTier.GENERIC, minYogaLevel = 4, sparkCost = 6,
            description = "Boots that carry the wind.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.SPD_PERCENT, 0.12f))),
        Equipment("sages_tome", "Sage's Tome", EquipmentSlot.ACCESSORY, EquipmentTier.GENERIC, minYogaLevel = 7, sparkCost = 12,
            description = "Ancient knowledge that quickens the mind.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.ULTIMATE_GAIN_RATE, 0.15f), EquipmentEffect(EquipmentEffectType.SPD_PERCENT, 0.05f))),
        Equipment("ember_pendant", "Ember Pendant", EquipmentSlot.ACCESSORY, EquipmentTier.GENERIC, minYogaLevel = 3, sparkCost = 5,
            description = "A warm pendant with latent fire energy.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.BONUS_FIRE_ON_STRIKE_CHANCE, 0.10f))),
        Equipment("crystal_ward", "Crystal Ward", EquipmentSlot.ARMOR, EquipmentTier.GENERIC, minYogaLevel = 5, sparkCost = 8,
            description = "A crystal that forms a protective barrier.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.START_SHIELD_PERCENT, 0.15f))),
        Equipment("force_amulet", "Force Amulet", EquipmentSlot.ACCESSORY, EquipmentTier.GENERIC, minYogaLevel = 6, sparkCost = 10,
            description = "Amulet radiating pure force energy.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.DAMAGE_PERCENT, 0.08f)))
    )

    val classWeapons = listOf(
        Equipment("staff_of_life", "Staff of Life", EquipmentSlot.WEAPON, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.HEALER, minYogaLevel = 2, sparkCost = 8,
            description = "A staff that amplifies healing energy.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.HEAL_AMOUNT, 0.20f))),
        Equipment("bulwark_shield", "Bulwark Shield", EquipmentSlot.WEAPON, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.TANK, minYogaLevel = 2, sparkCost = 8,
            description = "An unyielding shield of pure fortitude.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.SHIELD_GAIN, 0.20f))),
        Equipment("fury_blade", "Fury Blade", EquipmentSlot.WEAPON, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.DPS, minYogaLevel = 2, sparkCost = 8,
            description = "A blade burning with elemental fury.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.ELEMENTAL_DAMAGE, 0.20f))),
        Equipment("guiding_lance", "Guiding Lance", EquipmentSlot.WEAPON, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.BUFFER, minYogaLevel = 3, sparkCost = 8,
            description = "A lance that inspires all who see it.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.BUFF_DURATION, 0.20f))),
        Equipment("prism_staff", "Prism Staff", EquipmentSlot.WEAPON, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.MAGE, minYogaLevel = 3, sparkCost = 8,
            description = "A staff that refracts energy into AOE.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.AOE_DAMAGE, 0.20f)))
    )

    val classArmors = listOf(
        Equipment("vitality_mantle", "Vitality Mantle", EquipmentSlot.ARMOR, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.TANK, minYogaLevel = 4, sparkCost = 10,
            description = "A mantle that grants a defensive start.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.START_SHIELD_PERCENT, 0.20f))),
        Equipment("healers_vestments", "Healer's Vestments", EquipmentSlot.ARMOR, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.HEALER, minYogaLevel = 4, sparkCost = 10,
            description = "Robes that channel healing energy back.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.INCOMING_HEALING, 0.20f), EquipmentEffect(EquipmentEffectType.HP_PERCENT, 0.08f))),
        Equipment("assault_plate", "Assault Plate", EquipmentSlot.ARMOR, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.DPS, minYogaLevel = 4, sparkCost = 10,
            description = "Offensive armor that sacrifices defense.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.ATK_PERCENT, 0.10f), EquipmentEffect(EquipmentEffectType.HP_PERCENT, -0.05f))),
        Equipment("commanders_plate", "Commander's Plate", EquipmentSlot.ARMOR, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.BUFFER, minYogaLevel = 5, sparkCost = 10,
            description = "Armor that enhances all aspects of the leader.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.ALL_STATS_PERCENT, 0.10f))),
        Equipment("flowing_robes", "Flowing Robes", EquipmentSlot.ARMOR, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.MAGE, minYogaLevel = 5, sparkCost = 10,
            description = "Robes that move like the wind itself.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.SPD_PERCENT, 0.15f)))
    )

    val classAccessories = listOf(
        Equipment("pacifiers_charm", "Pacifier's Charm", EquipmentSlot.ACCESSORY, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.HEALER, minYogaLevel = 7, sparkCost = 15,
            description = "A charm that turns healing into protection.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.HEAL_SHIELD_PERCENT, 0.10f))),
        Equipment("defenders_crest", "Defender's Crest", EquipmentSlot.ACCESSORY, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.TANK, minYogaLevel = 7, sparkCost = 15,
            description = "A crest that retaliates with barriers.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.START_SHIELD_PERCENT, 0.10f))),
        Equipment("berserkers_band", "Berserker's Band", EquipmentSlot.ACCESSORY, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.DPS, minYogaLevel = 7, sparkCost = 15,
            description = "A band that fuels relentless assault.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.DOUBLE_STRIKE_CHANCE, 0.15f))),
        Equipment("inspirers_crown", "Inspirer's Crown", EquipmentSlot.ACCESSORY, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.BUFFER, minYogaLevel = 7, sparkCost = 15,
            description = "A crown that amplifies all inspirations.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.BUFF_AMOUNT, 0.15f))),
        Equipment("empath_ring", "Empath's Ring", EquipmentSlot.ACCESSORY, EquipmentTier.CLASS_SPECIFIC, heroClass = HeroClass.MAGE, minYogaLevel = 7, sparkCost = 15,
            description = "A ring that purifies through empathy.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.CLEANSE_ON_HEAL, 1f)))
    )

    val uniqueItems = listOf(
        Equipment("shanti_prayer_beads", "Shanti's Prayer Beads", EquipmentSlot.ACCESSORY, EquipmentTier.UNIQUE, heroId = "Shanti", minYogaLevel = 5, minHeroLevel = 3, sparkCost = 15,
            description = "Beads that spread calm to all.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.SPD_ON_ULTIMATE, 3f))),
        Equipment("santosha_foundation_stone", "Santosha's Foundation Stone", EquipmentSlot.ARMOR, EquipmentTier.UNIQUE, heroId = "Santosha", minYogaLevel = 5, minHeroLevel = 3, sparkCost = 15,
            description = "A stone that grounds the spirit before battle.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.START_SHIELD_PERCENT, 0.30f))),
        Equipment("virya_ember_core", "Virya's Ember Core", EquipmentSlot.ACCESSORY, EquipmentTier.UNIQUE, heroId = "Virya", minYogaLevel = 5, minHeroLevel = 3, sparkCost = 15,
            description = "The burning heart of a true yogi.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.BURN_CHANCE_ON_ULTIMATE, 0.30f))),
        Equipment("virya_inferno_wrath", "Virya's Inferno Wrath", EquipmentSlot.WEAPON, EquipmentTier.UNIQUE, heroId = "Virya", minYogaLevel = 8, minHeroLevel = 6, sparkCost = 20,
            description = "A blade that burns hotter against aflame foes.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.DAMAGE_TO_BURNING, 0.30f))),
        Equipment("dhairya_battle_standard", "Dhairya's Battle Standard", EquipmentSlot.WEAPON, EquipmentTier.UNIQUE, heroId = "Dhairya", minYogaLevel = 5, minHeroLevel = 3, sparkCost = 15,
            description = "A banner that rallies beyond the moment.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.BUFF_DURATION, 2f))),
        Equipment("dhairya_light_vanguard", "Dhairya's Light Vanguard", EquipmentSlot.ARMOR, EquipmentTier.UNIQUE, heroId = "Dhairya", minYogaLevel = 8, minHeroLevel = 6, sparkCost = 20,
            description = "Armor that shields all with courage.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.SHIELD_ON_STRIKE, 0.10f))),
        Equipment("maitri_universal_key", "Maitri's Universal Key", EquipmentSlot.ACCESSORY, EquipmentTier.UNIQUE, heroId = "Maitri", minYogaLevel = 5, minHeroLevel = 3, sparkCost = 15,
            description = "A key that opens the door to universal love.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.REVIVE_ON_ULTIMATE, 1f))),
        Equipment("maitri_wind_caress", "Maitri's Wind Caress", EquipmentSlot.WEAPON, EquipmentTier.UNIQUE, heroId = "Maitri", minYogaLevel = 8, minHeroLevel = 6, sparkCost = 20,
            description = "A feather-light weapon carrying the breeze.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.DAMAGE_PERCENT, 0.30f))),
        Equipment("maitri_heart_embrace", "Maitri's Heart Embrace", EquipmentSlot.ARMOR, EquipmentTier.UNIQUE, heroId = "Maitri", minYogaLevel = 10, minHeroLevel = 8, sparkCost = 25,
            description = "Armor that grants swiftness through compassion.",
            effects = listOf(EquipmentEffect(EquipmentEffectType.SPD_ON_SKILL, 3f)))
    )

    val setBonuses = mapOf(
        "Virya" to SetBonus("Raging Inferno", "Tapas Blast burn chance +20%",
            listOf("virya_ember_core", "virya_inferno_wrath"),
            listOf(EquipmentEffect(EquipmentEffectType.BURN_CHANCE_ON_ULTIMATE, 0.20f))),
        "Dhairya" to SetBonus("Inspiring Presence", "Battle start: party ATK+ for 3 turns",
            listOf("dhairya_battle_standard", "dhairya_light_vanguard"),
            listOf(EquipmentEffect(EquipmentEffectType.ALL_STATS_PERCENT, 0.10f))),
        "Maitri" to SetBonus("Universal Love", "Loving Aura also cleanses statuses from all allies",
            listOf("maitri_universal_key", "maitri_wind_caress", "maitri_heart_embrace"),
            listOf(EquipmentEffect(EquipmentEffectType.CLEANSE_ON_HEAL, 1f)))
    )

    fun allGeneric(): List<Equipment> = genericWeapons + genericArmors + genericAccessories
    fun allClassSpecific(): List<Equipment> = classWeapons + classArmors + classAccessories
    fun allUnique(): List<Equipment> = uniqueItems
    fun all(): List<Equipment> = allGeneric() + allClassSpecific() + allUnique()

    val allEquipment: List<Equipment> get() = all()

    fun getEquipment(id: String): Equipment? = all().find { it.id == id }
}

data class SetBonus(
    val name: String,
    val description: String,
    val requiredItems: List<String>,
    val effects: List<EquipmentEffect>
)
