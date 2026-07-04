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

fun Hero.toCombatantState(partyMember: PartyMemberData): CombatantState {
    val mult = 1f + (partyMember.level - 1) * 0.15f
    val hp = (baseHp * mult).toInt()
    return CombatantState(
        id = id.toString(),
        side = CombatSide.HERO,
        name = name.split(" ").first(),
        element = element,
        maxHp = hp,
        hp = hp,
        attack = (baseAtk * mult).toInt(),
        speed = (baseSpd * mult).toInt(),
        level = partyMember.level,
        skills = skills,
        ultimate = ultimate
    )
}
