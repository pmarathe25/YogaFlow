package com.example.game.model

enum class TrophyRarity {
    BRONZE, SILVER, GOLD, PLATINUM
}

enum class TrophyCategory {
    BADGE, TROPHY
}

data class Trophy(
    val id: String,
    val name: String,
    val description: String,
    val rarity: TrophyRarity,
    val category: TrophyCategory,
    val monsterId: String? = null,
    val condition: TrophyCondition = TrophyCondition.DEFEAT_MONSTER
)

enum class TrophyCondition {
    DEFEAT_MONSTER,
    NO_HERO_BELOW_50,
    WITHIN_ROUNDS,
    KILL_BEFORE_BERSERK,
    NO_DEATHS,
    FIRST_ROUND,
    COLLECT_ALL_BADGES,
    ALL_COMBOS,
    NO_ULTIMATE_OR_COMBO,
    ALL_BOSSES_SAME_PARTY,
    THREE_HERO_COMBOS_SINGLE_BATTLE,
    COLLECT_ALL
}


