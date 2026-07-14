package com.example.game.model

data class Hero(
    val id: Int,
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
    val setBonusId: String? = null,
    val colorTheme: HeroColorTheme? = null,
    val flavorQuote: String = ""
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
    equippedEquipment: List<Equipment> = emptyList()
): CombatantState {
    val atkPercent = equippedEquipment.flatMap { it.effects }.filter { it.type == EquipmentEffectType.ATK_PERCENT }.sumOf { it.value.toDouble() }.toFloat()
    val hpPercent = equippedEquipment.flatMap { it.effects }.filter { it.type == EquipmentEffectType.HP_PERCENT }.sumOf { it.value.toDouble() }.toFloat()
    val spdPercent = equippedEquipment.flatMap { it.effects }.filter { it.type == EquipmentEffectType.SPD_PERCENT }.sumOf { it.value.toDouble() }.toFloat()
    val allStatsPercent = equippedEquipment.flatMap { it.effects }.filter { it.type == EquipmentEffectType.ALL_STATS_PERCENT }.sumOf { it.value.toDouble() }.toFloat()
    val startShieldPercent = equippedEquipment.flatMap { it.effects }.filter { it.type == EquipmentEffectType.START_SHIELD_PERCENT }.sumOf { it.value.toDouble() }.toFloat()

    val atkMult = (1f + atkPercent) * (1f + allStatsPercent)
    val hpMult = (1f + hpPercent) * (1f + allStatsPercent)
    val spdMult = (1f + spdPercent) * (1f + allStatsPercent)

    val levelMult = 1f + (partyMember.level - 1) * 0.15f
    val finalAtk = (baseAtk * atkMult * levelMult).toInt()
    val finalHp = (baseHp * hpMult * levelMult).toInt()
    val finalSpd = (baseSpd * spdMult * levelMult).toInt()
    val initialShield = if (startShieldPercent > 0f) (finalHp * startShieldPercent).toInt() else 0
    return CombatantState(
        id = id.toString(),
        side = CombatSide.HERO,
        name = name.split(" ").first(),
        element = element,
        maxHp = finalHp,
        hp = finalHp,
        attack = finalAtk,
        speed = finalSpd,
        shield = initialShield,
        level = partyMember.level,
        skills = skills,
        ultimate = ultimate
    )
}
