package com.example.model

data class LevelDef(
    val level: Int,
    val name: String,
    val xpRange: IntRange
)

object LevelDefinitions {
    val levels = listOf(
        LevelDef(1, "Prana Sprout", 0..300),
        LevelDef(2, "Zen Seeker", 301..900),
        LevelDef(3, "Flow Initiate", 901..2100),
        LevelDef(4, "Mindfulness Guide", 2101..4500),
        LevelDef(5, "Prana Master", 4501..9300),
        LevelDef(6, "Cosmic Flow Yogi", 9301..18900),
        LevelDef(7, "Inner Light Sage", 18901..38100),
        LevelDef(8, "Chakra Harmonizer", 38101..76500),
        LevelDef(9, "Nirvana Ascendant", 76501..153300),
        LevelDef(10, "Infinite Samadhi", 153301..Int.MAX_VALUE)
    )

    fun getLevelName(level: Int): String =
        levels.firstOrNull { it.level == level }?.name ?: "Unknown"

    fun getLevelForXp(xp: Int): LevelDef =
        levels.last { it.xpRange.contains(xp) }

    fun getXpForLevel(level: Int): Int =
        levels.firstOrNull { it.level == level }?.xpRange?.first ?: 0

    fun getLevelProgress(xp: Int): Float {
        val currentLevel = getLevelForXp(xp)
        val nextLevel = levels.firstOrNull { it.level == currentLevel.level + 1 }
        if (nextLevel == null) return 1f
        val rangeSize = nextLevel.xpRange.first - currentLevel.xpRange.first
        if (rangeSize <= 0) return 1f
        val progress = (xp - currentLevel.xpRange.first).toFloat() / rangeSize
        return progress.coerceIn(0f, 1f)
    }

    fun remainingXpToNextLevel(xp: Int): String {
        val currentLevel = getLevelForXp(xp)
        val nextLevel = levels.firstOrNull { it.level == currentLevel.level + 1 }
        if (nextLevel == null) return "Maximum level achieved!"
        val needed = nextLevel.xpRange.first - xp
        return "$needed XP to level up!"
    }
}
