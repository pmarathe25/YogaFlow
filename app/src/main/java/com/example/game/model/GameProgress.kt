package com.example.game.model

data class GameProgress(
    val version: Int = 3,
    val party: List<PartyMemberData> = emptyList(),
    val unlockedHeroIds: Set<Int> = emptySet(),
    val defeatedMonsterIds: Set<String> = emptySet(),
    val inventory: List<String> = emptyList(),
    val sparks: Int = 0,
    val yogaLevel: Int = 1,
    val totalYogaXp: Int = 0,
    val gold: Int = 0,
    val totalBattlesWon: Int = 0,
    val earnedTrophyIds: Set<String> = emptySet(),
    val lastPlayedTimestamp: Long = 0L
)

data class PartyMemberData(
    val heroId: Int,
    val level: Int = 1,
    val equippedItemIds: List<String> = emptyList()
)
