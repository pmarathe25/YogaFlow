package com.example.game.persistence

import android.content.Context
import android.content.SharedPreferences
import com.example.game.model.HeroSaveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GameSaveManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_save", Context.MODE_PRIVATE)

    private companion object {
        private val gson = Gson()
        const val KEY_PROGRESS_BLOB = "progress_blob_v2"

        const val KEY_PARTY = "party"
        const val KEY_UNLOCKED_HERO_IDS = "unlocked_hero_ids"
        const val KEY_SPARKS = "sparks"
        const val KEY_YOGA_LEVEL = "yoga_level"
        const val KEY_EARNED_TROPHY_IDS = "earned_trophy_ids"
        const val KEY_TOTAL_BATTLES_WON = "total_battles_won"
        const val KEY_INVENTORY = "inventory"
        const val KEY_LAST_PLAYED_TIMESTAMP = "last_played_timestamp"
        const val KEY_LAST_SYNCED_MAIN_SPARKS = "last_synced_main_sparks"
        const val KEY_TOTAL_YOGA_XP = "total_yoga_xp"
        const val KEY_TOTAL_GOLD_SPENT = "total_gold_spent"
        const val KEY_DEFEATED_MONSTER_IDS = "defeated_monster_ids"
    }

    data class GameSaveData(
        val version: Int = 2,
        val party: List<HeroSaveData> = emptyList(),
        val unlockedHeroIds: Set<String> = emptySet(),
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
        val lastPlayedTimestamp: Long = 0L,
        val lastSyncedMainSparks: Int = 0,
        val totalYogaXp: Int = 0,
        val totalGoldSpent: Int = 0,
        val defeatedMonsterIds: Set<String> = emptySet()
    )

    fun loadGame(): GameSaveData {
        val blob = prefs.getString(KEY_PROGRESS_BLOB, null)
        if (!blob.isNullOrBlank()) {
            return runCatching { gson.fromJson(blob, GameSaveData::class.java).normalized() }
                .getOrElse { loadDefaultSave() }
        }

        if (prefs.all.isNotEmpty()) {
            val migrated = loadLegacySave().normalized()
            saveGame(migrated)
            return migrated
        }

        return loadDefaultSave()
    }

    fun saveGame(data: GameSaveData) {
        val normalized = data.normalized()
        prefs.edit()
            .clear()
            .putString(KEY_PROGRESS_BLOB, gson.toJson(normalized.copy(version = 2)))
            .apply()
    }

    fun resetToDefault() {
        saveGame(loadDefaultSave())
    }

    fun clearSave() {
        prefs.edit().clear().apply()
    }

    private fun loadDefaultSave(): GameSaveData {
        return try {
            val json = context.assets.open("game/default_save.json")
                .bufferedReader().use { it.readText() }
            gson.fromJson(json, GameSaveData::class.java).normalized()
        } catch (e: Exception) {
            GameSaveData()
        }
    }

    private fun loadLegacySave(): GameSaveData {
        return GameSaveData(
            party = readJsonList(KEY_PARTY, emptyList<HeroSaveData>()),
            unlockedHeroIds = readJsonStringSet(KEY_UNLOCKED_HERO_IDS),
            sparks = prefs.getInt(KEY_SPARKS, 0),
            yogaLevel = prefs.getInt(KEY_YOGA_LEVEL, 1),
            earnedTrophyIds = readJsonStringSet(KEY_EARNED_TROPHY_IDS),
            totalBattlesWon = prefs.getInt(KEY_TOTAL_BATTLES_WON, 0),
            inventory = readJsonList(KEY_INVENTORY, emptyList<String>()),
            lastPlayedTimestamp = prefs.getLong(KEY_LAST_PLAYED_TIMESTAMP, 0L),
            lastSyncedMainSparks = prefs.getInt(KEY_LAST_SYNCED_MAIN_SPARKS, 0),
            totalYogaXp = prefs.getString(KEY_TOTAL_YOGA_XP, "0")?.toIntOrNull() ?: 0,
            totalGoldSpent = prefs.getString(KEY_TOTAL_GOLD_SPENT, "0")?.toIntOrNull() ?: 0,
            defeatedMonsterIds = readJsonStringSet(KEY_DEFEATED_MONSTER_IDS)
        )
    }

    private inline fun <reified T> readJsonList(key: String, default: List<T>): List<T> {
        return try {
            val type = object : TypeToken<List<T>>() {}.type
            gson.fromJson<List<T>>(prefs.getString(key, "[]"), type) ?: default
        } catch (e: Exception) {
            default
        }
    }

    private fun readJsonStringSet(key: String): Set<String> =
        readJsonList(key, emptyList<String>()).toSet()

    private fun GameSaveData.normalized(): GameSaveData =
        copy(
            version = 2,
            party = party.map {
                it.copy(heroId = normalizeId(it.heroId), equippedItemIds = it.equippedItemIds.map(::normalizeKnownHeroBoundId))
            },
            unlockedHeroIds = unlockedHeroIds.map(::normalizeId).toSet(),
            defeatedMonsterIds = defeatedMonsterIds.map(::normalizeId).toSet()
        )

    private fun normalizeKnownHeroBoundId(id: String): String = id

    private fun normalizeId(id: String): String =
        id.trim()
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .lowercase()
}
