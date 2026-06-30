package com.example.game.model

import com.example.game.model.Element.*
import com.example.game.model.DamageType.*
import com.example.game.model.TargetType.*
import com.example.game.model.StatusEffectType.*
import com.example.game.model.ActionSpeed.*

data class Hero(
    val id: String,
    val name: String,
    val description: String,
    val element: Element,
    val role: HeroRole,
    val baseHp: Int,
    val baseAtk: Int,
    val baseSpd: Int,
    val unlockYogaLevel: Int,
    val skills: List<Skill>,
    val ultimate: Skill,
    val uniqueItemIds: List<String> = emptyList(),
    val setBonusId: String? = null
)

enum class HeroRole {
    HEALER, TANK, DPS, BUFFER, MAGE
}

data class HeroInstance(
    val heroId: String,
    val name: String,
    val level: Int,
    val maxHp: Int,
    var currentHp: Int,
    val atk: Int,
    val spd: Int,
    val element: Element,
    val skills: List<Skill>,
    val ultimate: Skill,
    var shield: Int = 0,
    var ultimateGauge: Int = 0,
    val equippedItems: MutableList<String> = mutableListOf(),
    var isDead: Boolean = false
) {
    val hpPercent: Float get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
}

object HeroDefinitions {
    private val shantiBasic = Skill("shanti_basic", "Gentle Strike",
        "A gentle but precise strike.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL)),
        baseDamage = 8, damagePerLevel = 2, speedWeight = NORMAL, ultimateGain = 20)

    private val shantiSkills = listOf(
        shantiBasic,
        Skill("shanti_skill1", "Pranayama Breath", "Heal target ally and cleanse 1 status effect.",
            SINGLE_ALLY, healScaling = HealScaling(35, 5),
            cleanse = true, speedWeight = FAST, ultimateGain = 20),
        Skill("shanti_skill2", "Calming Presence", "Grant target shield and SPD+.",
            SINGLE_ALLY, shieldScaling = ShieldScaling(0, 0, isPercentage = true),
            buffs = listOf(BuffApplication(SPD_UP, 2, 0.20f)),
            speedWeight = FAST, ultimateGain = 20),
        Skill("shanti_skill3", "Serene Renewal", "Small party heal and cleanse all statuses.",
            ALL_ALLIES, healScaling = HealScaling(20, 3),
            cleanse = true, speedWeight = NORMAL, ultimateGain = 20),
        Skill("shanti_skill4", "Rippling Current", "A splash of water energy that damages the enemy.",
            SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, WATER, 100)),
            baseDamage = 15, damagePerLevel = 4, speedWeight = NORMAL, ultimateGain = 20)
    )

    private val shantiUltimate = Skill("shanti_ultimate", "Calming Radiance",
        "Revive all fallen allies and heal 50% of max HP.",
        ALL_ALLIES, healScaling = HealScaling(0, 0, isPercentage = true),
        revive = true, speedWeight = SLOW, ultimateGain = 0)

    private val santoshaBasic = Skill("santosha_basic", "Bash",
        "A heavy shield bash.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL)),
        baseDamage = 12, damagePerLevel = 3, speedWeight = NORMAL, ultimateGain = 20)

    private val santoshaSkills = listOf(
        santoshaBasic,
        Skill("santosha_skill1", "Inner Sanctuary", "Gain a massive shield and taunt all enemies for 2 turns.",
            SELF, shieldScaling = ShieldScaling(45, 8),
            statusEffects = listOf(StatusEffectInfliction(TAUNT, 1f, 2)),
            speedWeight = FAST, ultimateGain = 20),
        Skill("santosha_skill2", "Solid Foundation", "Grant lowest-HP ally a shield and damage reduction.",
            SINGLE_ALLY, shieldScaling = ShieldScaling(30, 5),
            buffs = listOf(BuffApplication(DAMAGE_REDUCTION, 1, 0.20f)),
            speedWeight = FAST, ultimateGain = 20),
        Skill("santosha_skill3", "Grounding Aura", "Party-wide 15% damage reduction for 1 turn.",
            ALL_ALLIES,
            buffs = listOf(BuffApplication(DAMAGE_REDUCTION, 1, 0.15f, targetsParty = true)),
            speedWeight = NORMAL, ultimateGain = 20),
        Skill("santosha_skill4", "Seismic Slam", "Slam the ground to damage and slow the enemy.",
            SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, EARTH, 100)),
            baseDamage = 12, damagePerLevel = 3,
            statusEffects = listOf(StatusEffectInfliction(SPD_DOWN, 1f, 2)),
            speedWeight = NORMAL, ultimateGain = 20)
    )

    private val santoshaUltimate = Skill("santosha_ultimate", "Unshakable Mountain",
        "Grant the entire party immunity to all damage for 1 turn.",
        ALL_ALLIES, buffs = listOf(BuffApplication(DAMAGE_REDUCTION, 1, 1f, targetsParty = true)),
        speedWeight = SLOW, ultimateGain = 0)

    private val viryaBasic = Skill("virya_basic", "Fierce Strike",
        "A fierce physical strike.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL)),
        baseDamage = 25, damagePerLevel = 6, speedWeight = NORMAL, ultimateGain = 20)

    private val viryaSkills = listOf(
        viryaBasic,
        Skill("virya_skill1", "Tapas Blast", "Heavy fire damage with a chance to burn.",
            SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 100)),
            baseDamage = 32, damagePerLevel = 7,
            statusEffects = listOf(StatusEffectInfliction(BURN, 1f, 3)),
            speedWeight = NORMAL, ultimateGain = 20),
        Skill("virya_skill2", "Inner Fire", "Boost own attack power for 3 turns.",
            SELF, buffs = listOf(BuffApplication(ATK_UP, 3, 0.25f)),
            speedWeight = FAST, ultimateGain = 20),
        Skill("virya_skill3", "Vigorous Assault", "3-hit random physical attack — good for breaking shields.",
            SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL)),
            baseDamage = 10, damagePerLevel = 2, hits = 3,
            speedWeight = FAST, ultimateGain = 20),
        Skill("virya_skill4", "Inferno Wave", "Unleash a wave of fire that scorches all enemies.",
            ALL_ENEMIES, damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 100)),
            baseDamage = 18, damagePerLevel = 4,
            speedWeight = NORMAL, ultimateGain = 20)
    )

    private val viryaUltimate = Skill("virya_ultimate", "Blazing Ascension",
        "8-hit random fire barrage.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 100)),
        baseDamage = 0, damagePerLevel = 0, hits = 8,
        speedWeight = SLOW, ultimateGain = 0)

    private val dhairyaBasic = Skill("dhairya_basic", "Valiant Strike",
        "A courageous physical attack.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL)),
        baseDamage = 18, damagePerLevel = 4, speedWeight = NORMAL, ultimateGain = 20)

    private val dhairyaSkills = listOf(
        dhairyaBasic,
        Skill("dhairya_skill1", "Courageous Strike", "Light-infused physical strike that permanently boosts party ATK.",
            SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL, null, 50), DamageComponent(ELEMENTAL, LIGHT, 50)),
            baseDamage = 20, damagePerLevel = 4,
            buffs = listOf(BuffApplication(ATK_UP, 0, 4f, targetsParty = true, stacksPermanently = true, permanentValue = 4)),
            speedWeight = NORMAL, ultimateGain = 20),
        Skill("dhairya_skill2", "Steadfast Inspiration", "Boost party speed for 3 turns.",
            ALL_ALLIES, buffs = listOf(BuffApplication(SPD_UP, 3, 0.20f, targetsParty = true)),
            speedWeight = FAST, ultimateGain = 20),
        Skill("dhairya_skill3", "Shield of Faith", "Grant target ally a protective shield.",
            SINGLE_ALLY, shieldScaling = ShieldScaling(25, 5),
            cleanse = true, speedWeight = FAST, ultimateGain = 20),
        Skill("dhairya_skill4", "Radiant Burst", "A burst of light that damages all enemies and inspires the party.",
            ALL_ENEMIES, damageComponents = listOf(DamageComponent(ELEMENTAL, LIGHT, 100)),
            baseDamage = 14, damagePerLevel = 3,
            buffs = listOf(BuffApplication(ATK_UP, 2, 0.15f, targetsParty = true)),
            speedWeight = NORMAL, ultimateGain = 20)
    )

    private val dhairyaUltimate = Skill("dhairya_ultimate", "Rallying Cry",
        "Boost all party stats and unleash a heavy light-infused attack.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL, null, 30), DamageComponent(ELEMENTAL, LIGHT, 70)),
        baseDamage = 40, damagePerLevel = 10,
        buffs = listOf(BuffApplication(ATK_UP, 3, 0.25f, targetsParty = true),
            BuffApplication(SPD_UP, 3, 0.20f, targetsParty = true)),
        speedWeight = SLOW, ultimateGain = 0)

    private val maitriBasic = Skill("maitri_basic", "Breeze Slash",
        "A wind-assisted physical strike.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL)),
        baseDamage = 15, damagePerLevel = 4, speedWeight = NORMAL, ultimateGain = 20)

    private val maitriSkills = listOf(
        maitriBasic,
        Skill("maitri_skill1", "Loving Aura", "Deals air damage to all enemies and heals the party.",
            ALL_ENEMIES, damageComponents = listOf(DamageComponent(ELEMENTAL, AIR, 100)),
            baseDamage = 22, damagePerLevel = 5,
            healScaling = HealScaling(15, 3),
            speedWeight = NORMAL, ultimateGain = 20),
        Skill("maitri_skill2", "Compassion's Touch", "Heal a single ally for a large amount.",
            SINGLE_ALLY, healScaling = HealScaling(50, 8),
            speedWeight = FAST, ultimateGain = 20),
        Skill("maitri_skill3", "Gentle Breeze", "Small party heal and speed boost.",
            ALL_ALLIES, healScaling = HealScaling(12, 2),
            buffs = listOf(BuffApplication(SPD_UP, 2, 0.20f, targetsParty = true)),
            speedWeight = FAST, ultimateGain = 20),
        Skill("maitri_skill4", "Zephyr's Wrath", "Unleash cutting winds against all enemies.",
            ALL_ENEMIES, damageComponents = listOf(DamageComponent(ELEMENTAL, AIR, 100)),
            baseDamage = 18, damagePerLevel = 4,
            speedWeight = NORMAL, ultimateGain = 20)
    )

    private val maitriUltimate = Skill("maitri_ultimate", "Universal Embrace",
        "Fully heal the party and deal air damage to all enemies.",
        ALL_ENEMIES, damageComponents = listOf(DamageComponent(ELEMENTAL, AIR, 100)),
        baseDamage = 30, damagePerLevel = 8,
        healScaling = HealScaling(0, 0, isPercentage = true),
        speedWeight = SLOW, ultimateGain = 0)

    val allHeroes: List<Hero> = listOf(
        Hero("Shanti", "Shanti (Calm)", "The restorative spirit of peaceful calm.", WATER, HeroRole.HEALER,
            100, 10, 14, 1, shantiSkills, shantiUltimate,
            uniqueItemIds = listOf("shanti_prayer_beads")),
        Hero("Santosha", "Santosha (Content)", "The unbreakable shield of contentment.", EARTH, HeroRole.TANK,
            160, 7, 8, 2, santoshaSkills, santoshaUltimate,
            uniqueItemIds = listOf("santosha_foundation_stone")),
        Hero("Virya", "Virya (Vigor)", "The blazing fire of yoga-fueled vigor.", FIRE, HeroRole.DPS,
            110, 22, 12, 3, viryaSkills, viryaUltimate,
            uniqueItemIds = listOf("virya_ember_core", "virya_inferno_wrath"),
            setBonusId = "Virya"),
        Hero("Dhairya", "Dhairya (Courage)", "Patient, courageous fortitude.", LIGHT, HeroRole.BUFFER,
            135, 15, 10, 4, dhairyaSkills, dhairyaUltimate,
            uniqueItemIds = listOf("dhairya_battle_standard", "dhairya_light_vanguard"),
            setBonusId = "Dhairya"),
        Hero("Maitri", "Maitri (Loving-Kindness)", "Universal benevolence.", AIR, HeroRole.MAGE,
            95, 18, 13, 5, maitriSkills, maitriUltimate,
            uniqueItemIds = listOf("maitri_universal_key", "maitri_wind_caress", "maitri_heart_embrace"),
            setBonusId = "Maitri")
    )

    fun getHero(id: String): Hero? = allHeroes.find { it.id == id }

    fun createInstance(hero: Hero, level: Int, equippedItems: List<String> = emptyList()): HeroInstance {
        val mult = 1f + (level - 1) * 0.15f
        val hp = (hero.baseHp * mult).toInt()
        return HeroInstance(
            heroId = hero.id,
            name = hero.name.split(" ").first(),
            level = level,
            maxHp = hp,
            currentHp = hp,
            atk = (hero.baseAtk * mult).toInt(),
            spd = (hero.baseSpd * mult).toInt(),
            element = hero.element,
            skills = hero.skills,
            ultimate = hero.ultimate,
            equippedItems = equippedItems.toMutableList()
        )
    }

    fun getHeroForLevel(yogaLevel: Int): Hero? {
        return allHeroes.filter { it.unlockYogaLevel <= yogaLevel }.maxByOrNull { it.unlockYogaLevel }
    }
}
