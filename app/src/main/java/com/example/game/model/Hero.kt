package com.example.game.model

data class Hero(
    val id: Int,
    val name: String,
    val description: String,
    val element: Element,
    val role: HeroRole,
    val baseHp: Int,
    val baseAtk: Int,
    val unlockYogaLevel: Int,
    val skills: List<Skill>,
    val ultimate: Skill,
    val uniqueItemIds: List<String> = emptyList(),
    val setBonusId: String? = null,
    val colorTheme: HeroColorTheme? = null,
    val flavorQuote: String = ""
)

enum class SkinUnlockMethod {
    DEFAULT,
    PURCHASE,
    ACHIEVEMENT,
    YOGA_LEVEL
}

data class HeroSkin(
    val skinId: String,
    val heroId: Int,
    val name: String,
    val description: String,
    val primaryColor: String,
    val secondaryColor: String,
    val unlockMethod: SkinUnlockMethod,
    val unlockCost: Int = 0,
    val unlockYogaLevel: Int = 0,
    val unlockAchievementId: String? = null
)

data class HeroColorTheme(
    val primary: String,
    val secondary: String
)

enum class HeroRole {
    HEALER, TANK, DPS, BUFFER, MAGE
}

fun Hero.toCombatantState(
    partyMember: PartyMemberData,
    equippedEquipment: List<Equipment> = emptyList(),
    unlockedSkillIds: Map<Int, Set<String>> = emptyMap(),
    heroSkin: HeroSkin? = null
): CombatantState {
    val atkPercent = equippedEquipment.flatMap { it.effects }.filter { it.type == EquipmentEffectType.ATK_PERCENT }.sumOf { it.value.toDouble() }.toFloat()
    val hpPercent = equippedEquipment.flatMap { it.effects }.filter { it.type == EquipmentEffectType.HP_PERCENT }.sumOf { it.value.toDouble() }.toFloat()
    val startShieldPercent = equippedEquipment.flatMap { it.effects }.filter { it.type == EquipmentEffectType.START_SHIELD_PERCENT }.sumOf { it.value.toDouble() }.toFloat()

    val atkMult = (1f + atkPercent)
    val hpMult = (1f + hpPercent)

    val levelMult = 1f + (partyMember.level - 1) * 0.15f
    val finalAtk = (baseAtk * atkMult * levelMult).toInt()
    val finalHp = (baseHp * hpMult * levelMult).toInt()
    val initialShield = if (startShieldPercent > 0f) (finalHp * startShieldPercent).toInt() else 0

    val heroUnlockedSkills = unlockedSkillIds[partyMember.heroId] ?: emptySet()
    val availableSkills = skills.filter { it.id in heroUnlockedSkills }

    return CombatantState(
        id = id.toString(),
        side = CombatSide.HERO,
        name = name.split(" ").first(),
        element = element,
        maxHp = finalHp,
        hp = finalHp,
        attack = finalAtk,
        shield = initialShield,
        level = partyMember.level,
        skills = availableSkills,
        ultimate = ultimate,
        skinPrimaryColor = heroSkin?.primaryColor,
        skinSecondaryColor = heroSkin?.secondaryColor
    )
}
