package com.example.game.model

data class GameProgress(
    val version: Int = 2,
    val party: List<PartyMemberData> = emptyList(),
    val unlockedHeroIds: Set<String> = emptySet(),
    val defeatedMonsterIds: Set<String> = emptySet(),
    val inventory: List<String> = emptyList(),
    val sparks: Int = 0,
    val yogaLevel: Int = 1,
    val totalYogaXp: Int = 0,
    val totalGoldSpent: Int = 0,
    val lastSyncedMainSparks: Int = 0,
    val totalBattlesWon: Int = 0,
    val earnedTrophyIds: Set<String> = emptySet(),
    val lastPlayedTimestamp: Long = 0L,
    val consumables: Map<String, Int> = emptyMap(),
    val equippedSkins: Map<String, String> = emptyMap(),
    val unlockedSkinIds: Set<String> = emptySet(),
    val totalPlayTimeMs: Long = 0L,
    val highestComboHits: Int = 0,
    val fastestBattleTurns: Int = Int.MAX_VALUE
)

data class PartyMemberData(
    val heroId: String,
    val level: Int = 1,
    val equippedItemIds: List<String> = emptyList()
)
