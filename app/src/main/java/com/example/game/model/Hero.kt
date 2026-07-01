package com.example.game.model

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

data class HeroInstance(
    val heroId: String,
    val name: String,
    var level: Int,
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

fun Hero.createInstance(level: Int, equippedItems: List<String> = emptyList()): HeroInstance {
    val mult = 1f + (level - 1) * 0.15f
    val hp = (baseHp * mult).toInt()
    return HeroInstance(
        heroId = id,
        name = name.split(" ").first(),
        level = level,
        maxHp = hp,
        currentHp = hp,
        atk = (baseAtk * mult).toInt(),
        spd = (baseSpd * mult).toInt(),
        element = element,
        skills = skills,
        ultimate = ultimate,
        equippedItems = equippedItems.toMutableList()
    )
}
