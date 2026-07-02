package com.example.game.model

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class GameProgressSerializationTest {

    private val gson = Gson()

    @Test
    fun `serialize and deserialize GameProgress`() {
        val original = GameProgress(
            version = 2,
            party = listOf(
                PartyMemberData("shanti", 3, listOf("training_blade")),
                PartyMemberData("virya", 5, listOf("ember_pendant"))
            ),
            unlockedHeroIds = setOf("shanti", "virya"),
            sparks = 150,
            yogaLevel = 4,
            totalBattlesWon = 12
        )
        val json = gson.toJson(original)
        val restored = gson.fromJson(json, GameProgress::class.java)
        assertEquals(original.version, restored.version)
        assertEquals(original.party.size, restored.party.size)
        assertEquals(original.party[0].heroId, restored.party[0].heroId)
        assertEquals(original.party[0].level, restored.party[0].level)
        assertEquals(original.party[1].equippedItemIds, restored.party[1].equippedItemIds)
        assertEquals(original.unlockedHeroIds, restored.unlockedHeroIds)
        assertEquals(original.sparks, restored.sparks)
        assertEquals(original.yogaLevel, restored.yogaLevel)
        assertEquals(original.totalBattlesWon, restored.totalBattlesWon)
    }

    @Test
    fun `version field is respected`() {
        val json = """{"version":3,"sparks":100}"""
        val restored = gson.fromJson(json, GameProgress::class.java)
        assertEquals(3, restored.version)
        assertEquals(100, restored.sparks)
    }

    @Test
    fun `default values on missing fields`() {
        val json = """{}"""
        val restored = gson.fromJson(json, GameProgress::class.java)
        assertEquals(2, restored.version)
        assertTrue(restored.party.isEmpty())
        assertTrue(restored.unlockedHeroIds.isEmpty())
        assertEquals(0, restored.sparks)
        assertEquals(1, restored.yogaLevel)
        assertEquals(0, restored.totalBattlesWon)
        assertTrue(restored.inventory.isEmpty())
        assertEquals(0L, restored.lastPlayedTimestamp)
    }
}
