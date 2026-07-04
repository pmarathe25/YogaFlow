package com.example.game.persistence

import androidx.test.core.app.ApplicationProvider
import com.example.game.model.GameProgress
import com.example.game.model.PartyMemberData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameSaveManagerTest {

    private lateinit var saveManager: GameSaveManager

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.getSharedPreferences("game_save", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
        saveManager = GameSaveManager(ctx)
    }

    @Test
    fun `default save loads with version 3`() {
        val data = saveManager.loadGame()
        assertEquals(3, data.version)
        assertTrue(data.party.isEmpty())
        assertTrue(data.unlockedHeroIds.isEmpty())
        assertEquals(0, data.sparks)
        assertEquals(1, data.yogaLevel)
    }

    @Test
    fun `save then load returns identical data`() {
        val original = GameProgress(
            version = 3,
            party = listOf(
                PartyMemberData(1, 3, listOf("training_blade")),
                PartyMemberData(3, 5, listOf("ember_pendant"))
            ),
            unlockedHeroIds = setOf(1, 2, 3),
            sparks = 150,
            yogaLevel = 4,
            earnedTrophyIds = setOf("badge_bhaya", "trophy_fearless"),
            totalBattlesWon = 12,
            inventory = listOf("training_blade", "crystal_sword"),
            lastPlayedTimestamp = 1000000L,
            lastSyncedMainSparks = 10,
            totalYogaXp = 5200,
            gold = 400,
            defeatedMonsterIds = setOf("bhaya", "tandra", "chinta")
        )

        saveManager.saveGame(original)
        val loaded = saveManager.loadGame()

        assertEquals(original.version, loaded.version)
        assertEquals(original.party.size, loaded.party.size)
        assertEquals(original.party[0].heroId, loaded.party[0].heroId)
        assertEquals(original.party[0].level, loaded.party[0].level)
        assertEquals(original.party[0].equippedItemIds, loaded.party[0].equippedItemIds)
        assertEquals(original.party[1].heroId, loaded.party[1].heroId)
        assertEquals(original.unlockedHeroIds, loaded.unlockedHeroIds)
        assertEquals(original.sparks, loaded.sparks)
        assertEquals(original.yogaLevel, loaded.yogaLevel)
        assertEquals(original.earnedTrophyIds, loaded.earnedTrophyIds)
        assertEquals(original.totalBattlesWon, loaded.totalBattlesWon)
        assertEquals(original.inventory, loaded.inventory)
        assertEquals(original.defeatedMonsterIds, loaded.defeatedMonsterIds)
        assertEquals(original.totalYogaXp, loaded.totalYogaXp)
        assertEquals(original.gold, loaded.gold)
    }

    @Test
    fun `save with int hero IDs preserves exact values`() {
        val data = GameProgress(
            party = listOf(PartyMemberData(1, 1)),
            unlockedHeroIds = setOf(2, 3),
            defeatedMonsterIds = setOf("MonsterX")
        )
        saveManager.saveGame(data)
        val loaded = saveManager.loadGame()

        assertTrue("hero 1 should be 1", loaded.party.any { it.heroId == 1 })
        assertTrue("hero 2 should be present", loaded.unlockedHeroIds.contains(2))
        assertTrue("hero 3 should be present", loaded.unlockedHeroIds.contains(3))
        assertTrue("monster_x should be lowercase", loaded.defeatedMonsterIds.contains("monsterx"))
    }

    @Test
    fun `save with mixed case IDs normalizes correctly`() {
        val data = GameProgress(
            party = listOf(
                PartyMemberData(1, 1),
                PartyMemberData(3, 2),
                PartyMemberData(4, 3)
            ),
            unlockedHeroIds = setOf(5, 2),
            defeatedMonsterIds = setOf("Bhaya_Fear", "Krodha--Anger")
        )
        saveManager.saveGame(data)
        val loaded = saveManager.loadGame()

        assertEquals(1, loaded.party[0].heroId)
        assertEquals(3, loaded.party[1].heroId)
        assertEquals(4, loaded.party[2].heroId)
        assertTrue(loaded.unlockedHeroIds.contains(5))
        assertTrue(loaded.unlockedHeroIds.contains(2))
        assertTrue(loaded.defeatedMonsterIds.contains("bhaya_fear"))
        assertTrue(loaded.defeatedMonsterIds.contains("krodha_anger"))
    }

    @Test
    fun `empty save roundtrip preserves defaults`() {
        val original = GameProgress()
        saveManager.saveGame(original)
        val loaded = saveManager.loadGame()

        assertEquals(3, loaded.version)
        assertTrue(loaded.party.isEmpty())
        assertTrue(loaded.unlockedHeroIds.isEmpty())
        assertEquals(0, loaded.sparks)
        assertEquals(1, loaded.yogaLevel)
        assertEquals(0, loaded.totalBattlesWon)
        assertTrue(loaded.inventory.isEmpty())
        assertTrue(loaded.defeatedMonsterIds.isEmpty())
    }

    @Test
    fun `resetToDefault restores fresh save state`() {
        val original = GameProgress(
            sparks = 999, yogaLevel = 10,
            party = listOf(PartyMemberData(1, 5)),
            totalBattlesWon = 50
        )
        saveManager.saveGame(original)

        saveManager.resetToDefault()
        val loaded = saveManager.loadGame()

        assertEquals(0, loaded.sparks)
        assertEquals(1, loaded.yogaLevel)
        assertTrue(loaded.party.isEmpty())
        assertEquals(0, loaded.totalBattlesWon)
    }

    @Test
    fun `multiple save cycles preserve data integrity`() {
        var data = GameProgress(sparks = 100, totalBattlesWon = 5)
        saveManager.saveGame(data)

        data = saveManager.loadGame()
        data = data.copy(sparks = 200, totalBattlesWon = 10)
        saveManager.saveGame(data)

        data = saveManager.loadGame()
        data = data.copy(sparks = 300, totalBattlesWon = 15)
        saveManager.saveGame(data)

        val loaded = saveManager.loadGame()
        assertEquals(300, loaded.sparks)
        assertEquals(15, loaded.totalBattlesWon)
        assertEquals(3, loaded.version)
    }

    @Test
    fun `party save data roundtrip preserves all fields`() {
        val original = GameProgress(
            party = listOf(
                PartyMemberData(1, 3, listOf("training_blade", "simple_beads")),
                PartyMemberData(3, 5, listOf("fury_blade")),
                PartyMemberData(2, 2),
                PartyMemberData(4, 1)
            )
        )
        saveManager.saveGame(original)
        val loaded = saveManager.loadGame()

        assertEquals(4, loaded.party.size)

        val shanti = loaded.party.find { it.heroId == 1 }
        assertNotNull(shanti)
        assertEquals(3, shanti!!.level)
        assertEquals(listOf("training_blade", "simple_beads"), shanti.equippedItemIds)

        val virya = loaded.party.find { it.heroId == 3 }
        assertNotNull(virya)
        assertEquals(5, virya!!.level)

        val dhairya = loaded.party.find { it.heroId == 4 }
        assertNotNull(dhairya)
        assertEquals(1, dhairya!!.level)
    }

    @Test
    fun `load returns default when no save exists`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.getSharedPreferences("game_save", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()

        val fresh = GameSaveManager(ctx)
        val data = fresh.loadGame()
        assertEquals(3, data.version)
        assertEquals(0, data.sparks)
        assertEquals(1, data.yogaLevel)
    }

    @Test
    fun `versioned JSON blob roundtrip preserves all fields`() {
        val original = GameProgress(
            version = 3,
            party = listOf(PartyMemberData(1, 3, listOf("blade"))),
            unlockedHeroIds = setOf(1),
            sparks = 200,
            yogaLevel = 5,
            totalBattlesWon = 20,
            inventory = listOf("potion"),
            defeatedMonsterIds = setOf("bhaya"),
            totalYogaXp = 8000,
            gold = 500
        )
        saveManager.saveGame(original)
        val loaded = saveManager.loadGame()
        assertEquals(original.version, loaded.version)
        assertEquals(original.party.first().heroId, loaded.party.first().heroId)
        assertEquals(original.unlockedHeroIds, loaded.unlockedHeroIds)
        assertEquals(original.sparks, loaded.sparks)
        assertEquals(original.yogaLevel, loaded.yogaLevel)
        assertEquals(original.totalBattlesWon, loaded.totalBattlesWon)
    }

    @Test
    fun `default save loaded from assets has expected structure`() {
        val data = saveManager.loadGame()
        assertEquals(3, data.version)
        assertNotNull(data.party)
        assertNotNull(data.unlockedHeroIds)
        assertNotNull(data.inventory)
        assertNotNull(data.defeatedMonsterIds)
    }
}