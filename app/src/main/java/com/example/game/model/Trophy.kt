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

object TrophyDefinitions {
    val allBadges: List<Trophy> = listOf(
        Trophy("badge_bhaya", "Fear Purged", "Defeat Bhaya", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Bhaya"),
        Trophy("badge_tandra", "Fatigue Lifted", "Defeat Tandra", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Tandra"),
        Trophy("badge_chinta", "Anxiety Calmed", "Defeat Chinta", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Chinta"),
        Trophy("badge_alasya", "Sloth Overcome", "Defeat Alasya", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Alasya"),
        Trophy("badge_matsarya", "Envy Released", "Defeat Matsarya", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Matsarya"),
        Trophy("badge_krodha", "Rage Tamed", "Defeat Krodha", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Krodha"),
        Trophy("badge_dvesha", "Aversion Healed", "Defeat Dvesha", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Dvesha"),
        Trophy("badge_moha", "Delusion Dispelled", "Defeat Moha", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Moha"),
        Trophy("badge_lobha", "Greed Loosened", "Defeat Lobha", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Lobha"),
        Trophy("badge_abhimana", "Conceit Humbled", "Defeat Abhimana", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Abhimana"),
        Trophy("badge_mada", "Pride Defeated", "Defeat Mada", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Mada"),
        Trophy("badge_irsya", "Jealousy Resolved", "Defeat Irsya", TrophyRarity.BRONZE, TrophyCategory.BADGE, monsterId = "Irsya"),
        Trophy("badge_ahankara", "Ego Dissolved", "Defeat Ahankara", TrophyRarity.SILVER, TrophyCategory.BADGE, monsterId = "Ahankara"),
        Trophy("badge_maya", "Illusion Shattered", "Defeat Maya", TrophyRarity.SILVER, TrophyCategory.BADGE, monsterId = "Maya"),
        Trophy("badge_klesh", "Turmoil Quelled", "Defeat Klesh", TrophyRarity.GOLD, TrophyCategory.BADGE, monsterId = "Klesh"),
        Trophy("badge_samsara", "Cycle Broken", "Defeat Samsara", TrophyRarity.PLATINUM, TrophyCategory.BADGE, monsterId = "Samsara")
    )

    val allTrophies: List<Trophy> = listOf(
        Trophy("trophy_fearless", "Fearless", "Defeat Bhaya without any hero falling below 50% HP", TrophyRarity.SILVER, TrophyCategory.TROPHY, condition = TrophyCondition.NO_HERO_BELOW_50),
        Trophy("trophy_anxiety_breaker", "Anxiety Breaker", "Defeat Chinta within 3 rounds", TrophyRarity.SILVER, TrophyCategory.TROPHY, condition = TrophyCondition.WITHIN_ROUNDS),
        Trophy("trophy_rage_tamer", "Rage Tamer", "Defeat Krodha before it berserks (kill above 50% HP)", TrophyRarity.SILVER, TrophyCategory.TROPHY, condition = TrophyCondition.KILL_BEFORE_BERSERK),
        Trophy("trophy_unshaken", "Unshaken", "Defeat Ahankara without any hero dying", TrophyRarity.SILVER, TrophyCategory.TROPHY, condition = TrophyCondition.NO_DEATHS),
        Trophy("trophy_speed_demon", "Speed Demon", "Defeat any monster on the first round", TrophyRarity.SILVER, TrophyCategory.TROPHY, condition = TrophyCondition.FIRST_ROUND),
        Trophy("trophy_true_calm", "True Calm", "Collect all basic badges", TrophyRarity.GOLD, TrophyCategory.TROPHY, condition = TrophyCondition.COLLECT_ALL_BADGES),
        Trophy("trophy_combo_master", "Combo Master", "Use all 2-hero combos at least once", TrophyRarity.GOLD, TrophyCategory.TROPHY, condition = TrophyCondition.ALL_COMBOS),
        Trophy("trophy_perfect_run", "Perfect Run", "Beat Klesh without using any combo or ultimate", TrophyRarity.GOLD, TrophyCategory.TROPHY, condition = TrophyCondition.NO_ULTIMATE_OR_COMBO),
        Trophy("trophy_perfect_harmony", "Perfect Harmony", "Beat all 4 bosses with the same party, no deaths", TrophyRarity.PLATINUM, TrophyCategory.TROPHY, condition = TrophyCondition.ALL_BOSSES_SAME_PARTY),
        Trophy("trophy_trinity", "Trinity", "Use all 3-hero combos in a single battle", TrophyRarity.PLATINUM, TrophyCategory.TROPHY, condition = TrophyCondition.THREE_HERO_COMBOS_SINGLE_BATTLE),
        Trophy("trophy_zen_master", "Zen Master", "Collect all badges and trophies", TrophyRarity.PLATINUM, TrophyCategory.TROPHY, condition = TrophyCondition.COLLECT_ALL)
    )

    fun all(): List<Trophy> = allBadges + allTrophies
}
