package com.example.game.persistence

import android.content.Context
import android.content.SharedPreferences
import com.example.game.model.GameProgress
import com.example.game.model.PartyMemberData
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

    fun loadGame(): GameProgress {
        val blob = prefs.getString(KEY_PROGRESS_BLOB, null)
        if (!blob.isNullOrBlank()) {
            return runCatching { gson.fromJson(blob, GameProgress::class.java).normalized() }
                .getOrElse { loadDefaultSave() }
        }

        if (prefs.all.isNotEmpty()) {
            val migrated = loadLegacySave().normalized()
            saveGame(migrated)
            return migrated
        }

        return loadDefaultSave()
    }

    fun saveGame(data: GameProgress) {
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

    private fun loadDefaultSave(): GameProgress {
        return try {
            val json = context.assets.open("game/default_save.json")
                .bufferedReader().use { it.readText() }
            gson.fromJson(json, GameProgress::class.java).normalized()
        } catch (e: Exception) {
            GameProgress()
        }
    }

    private fun loadLegacySave(): GameProgress {
        return GameProgress(
            party = readJsonList(KEY_PARTY, emptyList<PartyMemberData>()),
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

    private fun GameProgress.normalized(): GameProgress =
        copy(
            version = 2,
            party = party.map {
                it.copy(heroId = normalizeId(it.heroId), equippedItemIds = it.equippedItemIds.map(::normalizeId))
            },
            unlockedHeroIds = unlockedHeroIds.map(::normalizeId).toSet(),
            defeatedMonsterIds = defeatedMonsterIds.map(::normalizeId).toSet()
        )

    private fun normalizeId(id: String): String =
        id.trim()
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .lowercase()
}
