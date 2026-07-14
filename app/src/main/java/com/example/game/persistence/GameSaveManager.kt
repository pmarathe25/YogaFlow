package com.example.game.persistence

import android.content.Context
import android.content.SharedPreferences
import com.example.game.model.GameProgress
import com.example.game.model.PartyMemberData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

class GameSaveManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_save", Context.MODE_PRIVATE)

    private companion object {
        private val gson = Gson()
        const val KEY_PROGRESS_BLOB = "progress_blob_v3"

        const val KEY_PARTY = "party"
        const val KEY_UNLOCKED_HERO_IDS = "unlocked_hero_ids"
        const val KEY_SPARKS = "sparks"
        const val KEY_YOGA_LEVEL = "yoga_level"
        const val KEY_EARNED_TROPHY_IDS = "earned_trophy_ids"
        const val KEY_TOTAL_BATTLES_WON = "total_battles_won"
        const val KEY_INVENTORY = "inventory"
        const val KEY_LAST_PLAYED_TIMESTAMP = "last_played_timestamp"
        const val KEY_TOTAL_YOGA_XP = "total_yoga_xp"
        const val KEY_DEFEATED_MONSTER_IDS = "defeated_monster_ids"
    }

    private val stringHeroIdToInt = mapOf(
        "shanti" to 1, "santosha" to 2, "virya" to 3, "dhairya" to 4, "maitri" to 5
    )

    fun loadGame(): GameProgress {
        val blob = prefs.getString(KEY_PROGRESS_BLOB, null)
        if (!blob.isNullOrBlank()) {
            return runCatching {
                val migrated = migrateBlobIfNeeded(blob)
                gson.fromJson(migrated, GameProgress::class.java).normalized()
            }.getOrElse { loadDefaultSave() }
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
            .putString(KEY_PROGRESS_BLOB, gson.toJson(normalized.copy(version = 3)))
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
            gson.fromJson(json, GameProgress::class.java)?.normalized() ?: GameProgress().normalized()
        } catch (e: Exception) {
            GameProgress().normalized()
        }
    }

    private fun loadLegacySave(): GameProgress {
        val xp = prefs.getString(KEY_TOTAL_YOGA_XP, "0")?.toIntOrNull() ?: 0
        return GameProgress(
            party = readJsonList(KEY_PARTY, emptyList<PartyMemberData>()),
            unlockedHeroIds = readJsonStringSet(KEY_UNLOCKED_HERO_IDS).mapNotNull { id -> stringHeroIdToInt[id.trim().lowercase()] }.toSet(),
            sparks = prefs.getInt(KEY_SPARKS, 0),
            yogaLevel = prefs.getInt(KEY_YOGA_LEVEL, 1),
            earnedTrophyIds = readJsonStringSet(KEY_EARNED_TROPHY_IDS),
            totalBattlesWon = prefs.getInt(KEY_TOTAL_BATTLES_WON, 0),
            inventory = readJsonList(KEY_INVENTORY, emptyList<String>()),
            lastPlayedTimestamp = prefs.getLong(KEY_LAST_PLAYED_TIMESTAMP, 0L),
            totalYogaXp = xp,
            gold = xp / 10,
            defeatedMonsterIds = readJsonStringSet(KEY_DEFEATED_MONSTER_IDS)
        )
    }

    private fun migrateBlobIfNeeded(blob: String): String {
        return try {
            val root = JsonParser.parseString(blob).asJsonObject
            val version = root.get("version")?.asInt ?: 0
            if (version >= 3) return blob

            // Migrate party heroId from string to int
            root.getAsJsonArray("party")?.forEach { partyElem ->
                val partyObj = partyElem.asJsonObject
                val heroId = partyObj.get("heroId")
                if (heroId != null && heroId.isJsonPrimitive && heroId.asJsonPrimitive.isString) {
                    val intId = stringHeroIdToInt[heroId.asString.trim().lowercase()]
                    if (intId != null) {
                        partyObj.addProperty("heroId", intId)
                    } else {
                        partyObj.addProperty("heroId", 1)
                    }
                }
            }

            // Migrate unlockedHeroIds from strings to ints
            val unlocked = root.getAsJsonArray("unlockedHeroIds")
            if (unlocked != null) {
                val newArray = com.google.gson.JsonArray()
                unlocked.forEach { elem ->
                    val strId = if (elem.isJsonPrimitive && elem.asJsonPrimitive.isString) {
                        elem.asString.trim().lowercase()
                    } else {
                        elem.asString
                    }
                    val intId = stringHeroIdToInt[strId]
                    if (intId != null) {
                        newArray.add(intId)
                    }
                }
                root.add("unlockedHeroIds", newArray)
            }

            root.addProperty("version", 3)
            root.toString()
        } catch (e: Exception) {
            blob
        }
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

    private fun GameProgress.normalized(): GameProgress {
        var result = copy(
            version = 3,
            party = (party ?: emptyList()).map {
                it.copy(
                    level = it.level ?: 1,
                    equippedItemIds = it.equippedItemIds.orEmpty()
                )
            },
            unlockedHeroIds = unlockedHeroIds ?: emptySet(),
            defeatedMonsterIds = (defeatedMonsterIds ?: emptySet()).map(::normalizeMonsterId).toSet()
        )
        if (1 !in result.unlockedHeroIds) {
            result = result.copy(unlockedHeroIds = result.unlockedHeroIds + 1)
        }
        if (result.party.none { it.heroId == 1 }) {
            result = result.copy(party = result.party + PartyMemberData(heroId = 1))
        }
        return result
    }

    private fun normalizeItemId(id: String): String = id.trim().lowercase()
    private fun normalizeMonsterId(id: String): String = id.trim().lowercase()
}