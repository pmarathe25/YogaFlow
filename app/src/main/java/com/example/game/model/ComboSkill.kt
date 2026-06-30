package com.example.game.model

import com.example.game.model.Element.*
import com.example.game.model.DamageType.*
import com.example.game.model.TargetType.*
import com.example.game.model.StatusEffectType.*
import com.example.game.model.ActionSpeed.*

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

object ComboSkillDefinitions {
    // 2-hero combos
    private val shantiSantosha = ComboSkill(
        "combo_shanti_santosha", "Purifying Shelter",
        "Party shield + cleanse all debuffs.",
        setOf("Shanti", "Santosha"), ALL_ALLIES,
        shieldScaling = ShieldScaling(30, 5, isPercentage = true),
        cleanse = true, comboType = ComboType.TWO_HERO
    )

    private val shantiVirya = ComboSkill(
        "combo_shanti_virya", "Meditative Fury",
        "Enemy fire damage + party heal.",
        setOf("Shanti", "Virya"), ALL_ENEMIES,
        damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 100)),
        baseDamage = 40, damagePerLevel = 10,
        healScaling = HealScaling(25, 5),
        comboType = ComboType.TWO_HERO
    )

    private val shantiDhairya = ComboSkill(
        "combo_shanti_dhairya", "Calm Resolve",
        "Party ATK+SPD buff + heal.",
        setOf("Shanti", "Dhairya"), ALL_ALLIES,
        healScaling = HealScaling(30, 5),
        buffs = listOf(
            BuffApplication(ATK_UP, 3, 0.20f, targetsParty = true),
            BuffApplication(SPD_UP, 3, 0.20f, targetsParty = true)
        ),
        comboType = ComboType.TWO_HERO
    )

    private val shantiMaitri = ComboSkill(
        "combo_shanti_maitri", "Tranquil Embrace",
        "Full party revive with 40% HP.",
        setOf("Shanti", "Maitri"), ALL_ALLIES, healScaling = HealScaling(40, 0, isPercentage = true),
        revive = true, comboType = ComboType.TWO_HERO
    )

    private val santoshaVirya = ComboSkill(
        "combo_santosha_virya", "Unyielding Flame",
        "Massive single fire-physical hybrid.",
        setOf("Santosha", "Virya"), SINGLE_ENEMY,
        damageComponents = listOf(DamageComponent(PHYSICAL, null, 60), DamageComponent(ELEMENTAL, FIRE, 40)),
        baseDamage = 70, damagePerLevel = 16,
        comboType = ComboType.TWO_HERO
    )

    private val santoshaDhairya = ComboSkill(
        "combo_santosha_dhairya", "Fortress of Light",
        "Party shield + damage reduction + ATK buff.",
        setOf("Santosha", "Dhairya"), ALL_ALLIES,
        shieldScaling = ShieldScaling(0, 0, isPercentage = true, percentage = 0.40f),
        buffs = listOf(
            BuffApplication(DAMAGE_REDUCTION, 2, 0.25f, targetsParty = true),
            BuffApplication(ATK_UP, 2, 0.20f, targetsParty = true)
        ),
        comboType = ComboType.TWO_HERO
    )

    private val santoshaMaitri = ComboSkill(
        "combo_santosha_maitri", "Gentle Fortitude",
        "Party shield + heal over time.",
        setOf("Santosha", "Maitri"), ALL_ALLIES,
        shieldScaling = ShieldScaling(25, 5),
        healScaling = HealScaling(15, 3),
        comboType = ComboType.TWO_HERO
    )

    private val viryaDhairya = ComboSkill(
        "combo_virya_dhairya", "Inspiring Blaze",
        "Fire-light hybrid damage + ATK buff.",
        setOf("Virya", "Dhairya"), SINGLE_ENEMY,
        damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 70), DamageComponent(ELEMENTAL, LIGHT, 30)),
        baseDamage = 80, damagePerLevel = 20,
        buffs = listOf(BuffApplication(ATK_UP, 3, 0.30f, targetsParty = true)),
        comboType = ComboType.TWO_HERO
    )

    private val viryaMaitri = ComboSkill(
        "combo_virya_maitri", "Wild Compassion",
        "Air-fire hybrid to all + party heal.",
        setOf("Virya", "Maitri"), ALL_ENEMIES,
        damageComponents = listOf(DamageComponent(ELEMENTAL, AIR, 50), DamageComponent(ELEMENTAL, FIRE, 50)),
        baseDamage = 45, damagePerLevel = 10,
        healScaling = HealScaling(20, 4),
        comboType = ComboType.TWO_HERO
    )

    private val dhairyaMaitri = ComboSkill(
        "combo_dhairya_maitri", "Kindled Courage",
        "Party full heal + overshield.",
        setOf("Dhairya", "Maitri"), ALL_ALLIES,
        healScaling = HealScaling(100, 0, isPercentage = true),
        shieldScaling = ShieldScaling(0, 0, isPercentage = true, percentage = 0.25f),
        comboType = ComboType.TWO_HERO
    )

    // 3-hero combos
    private val shantiSantoshaVirya = ComboSkill(
        "combo_shanti_santosha_virya", "Serene Eruption",
        "Party overshield + heavy fire damage.",
        setOf("Shanti", "Santosha", "Virya"), ALL_ENEMIES,
        damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 100)),
        baseDamage = 100, damagePerLevel = 25,
        shieldScaling = ShieldScaling(40, 0, isPercentage = true),
        comboType = ComboType.THREE_HERO
    )

    private val shantiSantoshaDhairya = ComboSkill(
        "combo_shanti_santosha_dhairya", "Unyielding Radiance",
        "Party invincibility + full cleanse + full heal.",
        setOf("Shanti", "Santosha", "Dhairya"), ALL_ALLIES,
        healScaling = HealScaling(100, 0, isPercentage = true),
        cleanse = true,
        buffs = listOf(BuffApplication(DAMAGE_REDUCTION, 2, 1f, targetsParty = true)),
        comboType = ComboType.THREE_HERO
    )

    private val shantiSantoshaMaitri = ComboSkill(
        "combo_shanti_santosha_maitri", "Tranquil Bastion",
        "Party full HP + shield + ATK/SPD buff.",
        setOf("Shanti", "Santosha", "Maitri"), ALL_ALLIES,
        healScaling = HealScaling(100, 0, isPercentage = true),
        shieldScaling = ShieldScaling(30, 0, isPercentage = true),
        buffs = listOf(
            BuffApplication(ATK_UP, 3, 0.25f, targetsParty = true),
            BuffApplication(SPD_UP, 3, 0.25f, targetsParty = true)
        ),
        comboType = ComboType.THREE_HERO
    )

    private val shantiViryaDhairya = ComboSkill(
        "combo_shanti_virya_dhairya", "Blazing Conviction",
        "Massive fire-light to all + party heal + ATK buff.",
        setOf("Shanti", "Virya", "Dhairya"), ALL_ENEMIES,
        damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 60), DamageComponent(ELEMENTAL, LIGHT, 40)),
        baseDamage = 120, damagePerLevel = 30,
        healScaling = HealScaling(30, 6),
        buffs = listOf(BuffApplication(ATK_UP, 3, 0.30f, targetsParty = true)),
        comboType = ComboType.THREE_HERO
    )

    private val shantiViryaMaitri = ComboSkill(
        "combo_shanti_virya_maitri", "Universal Flame",
        "4-element hit + party revive.",
        setOf("Shanti", "Virya", "Maitri"), ALL_ENEMIES,
        damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 40), DamageComponent(ELEMENTAL, AIR, 30),
            DamageComponent(ELEMENTAL, WATER, 30)),
        baseDamage = 100, damagePerLevel = 24,
        healScaling = HealScaling(50, 0, isPercentage = true),
        revive = true,
        comboType = ComboType.THREE_HERO
    )

    private val shantiDhairyaMaitri = ComboSkill(
        "combo_shanti_dhairya_maitri", "Compassionate Radiance",
        "Party max HP + all buffs.",
        setOf("Shanti", "Dhairya", "Maitri"), ALL_ALLIES,
        healScaling = HealScaling(100, 0, isPercentage = true),
        buffs = listOf(
            BuffApplication(ATK_UP, 4, 0.30f, targetsParty = true),
            BuffApplication(SPD_UP, 4, 0.30f, targetsParty = true),
            BuffApplication(DAMAGE_REDUCTION, 4, 0.30f, targetsParty = true)
        ),
        cleanse = true,
        comboType = ComboType.THREE_HERO
    )

    private val santoshaViryaDhairya = ComboSkill(
        "combo_santosha_virya_dhairya", "Fortified Valor",
        "Party invincibility + massive damage to all.",
        setOf("Santosha", "Virya", "Dhairya"), ALL_ENEMIES,
        damageComponents = listOf(DamageComponent(PHYSICAL, null, 50), DamageComponent(ELEMENTAL, FIRE, 30),
            DamageComponent(ELEMENTAL, LIGHT, 20)),
        baseDamage = 130, damagePerLevel = 32,
        buffs = listOf(BuffApplication(DAMAGE_REDUCTION, 2, 1f, targetsParty = true)),
        comboType = ComboType.THREE_HERO
    )

    private val santoshaViryaMaitri = ComboSkill(
        "combo_santosha_virya_maitri", "Nurtured Strength",
        "Party full HP + overshield + air-fire damage.",
        setOf("Santosha", "Virya", "Maitri"), ALL_ENEMIES,
        damageComponents = listOf(DamageComponent(ELEMENTAL, AIR, 50), DamageComponent(ELEMENTAL, FIRE, 50)),
        baseDamage = 90, damagePerLevel = 22,
        healScaling = HealScaling(100, 0, isPercentage = true),
        shieldScaling = ShieldScaling(35, 0, isPercentage = true),
        comboType = ComboType.THREE_HERO
    )

    private val santoshaDhairyaMaitri = ComboSkill(
        "combo_santosha_dhairya_maitri", "Eternal Guardian",
        "Party immortal shield + all stats up.",
        setOf("Santosha", "Dhairya", "Maitri"), ALL_ALLIES,
        shieldScaling = ShieldScaling(60, 0, isPercentage = true),
        buffs = listOf(
            BuffApplication(ATK_UP, 3, 0.35f, targetsParty = true),
            BuffApplication(SPD_UP, 3, 0.35f, targetsParty = true),
            BuffApplication(DAMAGE_REDUCTION, 3, 0.50f, targetsParty = true)
        ),
        cleanse = true,
        comboType = ComboType.THREE_HERO
    )

    private val viryaDhairyaMaitri = ComboSkill(
        "combo_virya_dhairya_maitri", "Radiant Vigor",
        "4-hit 3-element + party revive.",
        setOf("Virya", "Dhairya", "Maitri"), ALL_ENEMIES,
        damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 40), DamageComponent(ELEMENTAL, LIGHT, 30),
            DamageComponent(ELEMENTAL, AIR, 30)),
        baseDamage = 120, damagePerLevel = 28,
        healScaling = HealScaling(50, 0, isPercentage = true),
        revive = true,
        comboType = ComboType.THREE_HERO
    )

    val allCombos: List<ComboSkill> = listOf(
        shantiSantosha, shantiVirya, shantiDhairya, shantiMaitri,
        santoshaVirya, santoshaDhairya, santoshaMaitri,
        viryaDhairya, viryaMaitri, dhairyaMaitri,
        shantiSantoshaVirya, shantiSantoshaDhairya, shantiSantoshaMaitri,
        shantiViryaDhairya, shantiViryaMaitri, shantiDhairyaMaitri,
        santoshaViryaDhairya, santoshaViryaMaitri, santoshaDhairyaMaitri,
        viryaDhairyaMaitri
    )

    fun findCombo(activeHeroes: Set<String>): ComboSkill? {
        return allCombos.find { combo ->
            activeHeroes.size == combo.requiredHeroes.size &&
                    activeHeroes.containsAll(combo.requiredHeroes)
        }
    }

    fun getCombo(id: String): ComboSkill? = allCombos.find { it.id == id }
}
