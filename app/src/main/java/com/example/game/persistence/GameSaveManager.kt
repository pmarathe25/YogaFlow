package com.example.game.persistence

import android.content.Context
import android.content.SharedPreferences
import com.example.game.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GameSaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_save", Context.MODE_PRIVATE)

    private companion object {
        private val gson = Gson()
        const val KEY_BATTLE_STATE = "battle_state"
        const val KEY_PARTY = "party"
        const val KEY_UNLOCKED_HERO_IDS = "unlocked_hero_ids"
        const val KEY_KARMA_XP = "karma_xp"
        const val KEY_SPARKS = "sparks"
        const val KEY_YOGA_LEVEL = "yoga_level"
        const val KEY_EARNED_TROPHY_IDS = "earned_trophy_ids"
        const val KEY_TOTAL_BATTLES_WON = "total_battles_won"
        const val KEY_CONSUMABLES = "consumables"
        const val KEY_INVENTORY = "inventory"
        const val KEY_EQUIPPED_SKINS = "equipped_skins"
        const val KEY_UNLOCKED_SKIN_IDS = "unlocked_skin_ids"
        const val KEY_TOTAL_PLAY_TIME_MS = "total_play_time_ms"
        const val KEY_HIGHEST_COMBO_HITS = "highest_combo_hits"
        const val KEY_FASTEST_BATTLE_TURNS = "fastest_battle_turns"
        const val KEY_LAST_PLAYED_TIMESTAMP = "last_played_timestamp"
    }

    data class GameSaveData(
        val battleState: BattleSaveData? = null,
        val party: List<HeroSaveData> = emptyList(),
        val unlockedHeroIds: Set<String> = emptySet(),
        val karmaXp: Map<String, Int> = emptyMap(),
        val sparks: Int = 0,
        val yogaLevel: Int = 1,
        val earnedTrophyIds: Set<String> = emptySet(),
        val totalBattlesWon: Int = 0,
        val consumables: Map<String, Int> = emptyMap(),
        val inventory: List<String> = emptyList(),
        val equippedSkins: Map<String, String> = emptyMap(),
        val unlockedSkinIds: Set<String> = emptySet(),
        val totalPlayTimeMs: Long = 0L,
        val highestComboHits: Int = 0,
        val fastestBattleTurns: Int = Int.MAX_VALUE,
        val lastPlayedTimestamp: Long = 0L
    ) {
        fun toSaveMap(): Map<String, String> = mapOf(
            KEY_BATTLE_STATE to if (battleState != null) gson.toJson(battleState) else "",
            KEY_PARTY to gson.toJson(party),
            KEY_UNLOCKED_HERO_IDS to gson.toJson(unlockedHeroIds.toList()),
            KEY_KARMA_XP to gson.toJson(karmaXp),
            KEY_SPARKS to sparks.toString(),
            KEY_YOGA_LEVEL to yogaLevel.toString(),
            KEY_EARNED_TROPHY_IDS to gson.toJson(earnedTrophyIds.toList()),
            KEY_TOTAL_BATTLES_WON to totalBattlesWon.toString(),
            KEY_CONSUMABLES to gson.toJson(consumables),
            KEY_INVENTORY to gson.toJson(inventory),
            KEY_EQUIPPED_SKINS to gson.toJson(equippedSkins),
            KEY_UNLOCKED_SKIN_IDS to gson.toJson(unlockedSkinIds.toList()),
            KEY_TOTAL_PLAY_TIME_MS to totalPlayTimeMs.toString(),
            KEY_HIGHEST_COMBO_HITS to highestComboHits.toString(),
            KEY_FASTEST_BATTLE_TURNS to fastestBattleTurns.toString(),
            KEY_LAST_PLAYED_TIMESTAMP to lastPlayedTimestamp.toString()
        )
    }

    fun loadGame(): GameSaveData {
        val battleStateStr = prefs.getString(KEY_BATTLE_STATE, "") ?: ""
        val battleState = if (battleStateStr.isNotBlank()) {
            try { gson.fromJson(battleStateStr, BattleSaveData::class.java) } catch (e: Exception) { null }
        } else null

        return GameSaveData(
            battleState = battleState,
            party = try {
                val type = object : TypeToken<List<HeroSaveData>>() {}.type
                gson.fromJson(prefs.getString(KEY_PARTY, "[]"), type) ?: emptyList()
            } catch (e: Exception) { emptyList() },
            unlockedHeroIds = try {
                val type = object : TypeToken<List<String>>() {}.type
                (gson.fromJson(prefs.getString(KEY_UNLOCKED_HERO_IDS, "[]"), type) as? List<String>)?.toSet() ?: emptySet()
            } catch (e: Exception) { emptySet() },
            karmaXp = try {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                gson.fromJson(prefs.getString(KEY_KARMA_XP, "{}"), type) ?: emptyMap()
            } catch (e: Exception) { emptyMap() },
            sparks = prefs.getInt(KEY_SPARKS, 0),
            yogaLevel = prefs.getInt(KEY_YOGA_LEVEL, 1),
            earnedTrophyIds = try {
                val type = object : TypeToken<List<String>>() {}.type
                (gson.fromJson(prefs.getString(KEY_EARNED_TROPHY_IDS, "[]"), type) as? List<String>)?.toSet() ?: emptySet()
            } catch (e: Exception) { emptySet() },
            totalBattlesWon = prefs.getInt(KEY_TOTAL_BATTLES_WON, 0),
            consumables = try {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                gson.fromJson(prefs.getString(KEY_CONSUMABLES, "{}"), type) ?: emptyMap()
            } catch (e: Exception) { emptyMap() },
            inventory = try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(prefs.getString(KEY_INVENTORY, "[]"), type) ?: emptyList()
            } catch (e: Exception) { emptyList() },
            equippedSkins = try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson(prefs.getString(KEY_EQUIPPED_SKINS, "{}"), type) ?: emptyMap()
            } catch (e: Exception) { emptyMap() },
            unlockedSkinIds = try {
                val type = object : TypeToken<List<String>>() {}.type
                (gson.fromJson(prefs.getString(KEY_UNLOCKED_SKIN_IDS, "[]"), type) as? List<String>)?.toSet() ?: emptySet()
            } catch (e: Exception) { emptySet() },
            totalPlayTimeMs = prefs.getLong(KEY_TOTAL_PLAY_TIME_MS, 0L),
            highestComboHits = prefs.getInt(KEY_HIGHEST_COMBO_HITS, 0),
            fastestBattleTurns = prefs.getInt(KEY_FASTEST_BATTLE_TURNS, Int.MAX_VALUE),
            lastPlayedTimestamp = prefs.getLong(KEY_LAST_PLAYED_TIMESTAMP, 0L)
        )
    }

    fun saveGame(data: GameSaveData) {
        val map = data.toSaveMap()
        prefs.edit().apply {
            map.forEach { (key, value) -> putString(key, value) }
            putInt(KEY_SPARKS, data.sparks)
            putInt(KEY_YOGA_LEVEL, data.yogaLevel)
            putInt(KEY_TOTAL_BATTLES_WON, data.totalBattlesWon)
            putLong(KEY_TOTAL_PLAY_TIME_MS, data.totalPlayTimeMs)
            putInt(KEY_HIGHEST_COMBO_HITS, data.highestComboHits)
            putInt(KEY_FASTEST_BATTLE_TURNS, data.fastestBattleTurns)
            putLong(KEY_LAST_PLAYED_TIMESTAMP, data.lastPlayedTimestamp)
            apply()
        }
    }

    fun clearSave() {
        prefs.edit().clear().apply()
    }
}
