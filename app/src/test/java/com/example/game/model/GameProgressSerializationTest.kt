package com.example.game.model

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class GameProgressSerializationTest {

    private val gson = Gson()

    @Test
    fun `serialize and deserialize GameProgress`() {
        val original = GameProgress(
            version = 4,
            party = listOf(
                PartyMemberData(1, 3, listOf("training_blade")),
                PartyMemberData(3, 5, listOf("ember_pendant"))
            ),
            unlockedHeroIds = setOf(1, 3),
            sparks = 150,
            yogaLevel = 4,
            totalBattlesWon = 12,
            karmaXp = 250,
            unlockedSkillIds = mapOf(
                1 to setOf("shanti_basic", "shanti_skill1"),
                3 to setOf("virya_basic")
            )
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
        assertEquals(original.karmaXp, restored.karmaXp)
        assertEquals(original.unlockedSkillIds, restored.unlockedSkillIds)
        assertEquals(original.heroSkins, restored.heroSkins)
        assertEquals(original.unlockedSkinIds, restored.unlockedSkinIds)
    }

    @Test
    fun `version field is respected`() {
        val json = """{"version":4,"sparks":100}"""
        val restored = gson.fromJson(json, GameProgress::class.java)
        assertEquals(4, restored.version)
        assertEquals(100, restored.sparks)
    }

    @Test
    fun `default values on missing fields`() {
        val json = """{}"""
        val restored = gson.fromJson(json, GameProgress::class.java)
        assertEquals(4, restored.version)
        assertTrue(restored.party.isEmpty())
        assertTrue(restored.unlockedHeroIds.isEmpty())
        assertEquals(0, restored.sparks)
        assertEquals(1, restored.yogaLevel)
        assertEquals(0, restored.totalBattlesWon)
        assertTrue(restored.inventory.isEmpty())
        assertEquals(0L, restored.lastPlayedTimestamp)
        assertTrue(restored.heroSkins.isEmpty())
        assertTrue(restored.unlockedSkinIds.isEmpty())
    }

    @Test
    fun `skin fields serialize and deserialize`() {
        val original = GameProgress(
            version = 4,
            heroSkins = mapOf(1 to "shanti_default", 3 to "virya_inferno"),
            unlockedSkinIds = setOf("shanti_default", "virya_inferno", "virya_azure"),
            party = listOf(
                PartyMemberData(1, 2, emptyList(), "shanti_default"),
                PartyMemberData(3, 4, emptyList(), "virya_inferno")
            )
        )
        val json = gson.toJson(original)
        val restored = gson.fromJson(json, GameProgress::class.java)
        assertEquals(original.heroSkins, restored.heroSkins)
        assertEquals(original.unlockedSkinIds, restored.unlockedSkinIds)
        assertEquals(original.party[0].skinId, restored.party[0].skinId)
        assertEquals("shanti_default", restored.party[0].skinId)
        assertEquals("virya_inferno", restored.party[1].skinId)
    }
}
