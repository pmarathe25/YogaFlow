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
    fun `default save loads with version 2`() {
        val data = saveManager.loadGame()
        assertEquals(2, data.version)
        assertTrue(data.party.isEmpty())
        assertTrue(data.unlockedHeroIds.isEmpty())
        assertEquals(0, data.sparks)
        assertEquals(1, data.yogaLevel)
    }

    @Test
    fun `save then load returns identical data`() {
        val original = GameProgress(
            version = 2,
            party = listOf(
                PartyMemberData("shanti", 3, listOf("training_blade")),
                PartyMemberData("virya", 5, listOf("ember_pendant"))
            ),
            unlockedHeroIds = setOf("shanti", "santosha", "virya"),
            sparks = 150,
            yogaLevel = 4,
            earnedTrophyIds = setOf("badge_bhaya", "trophy_fearless"),
            totalBattlesWon = 12,
            inventory = listOf("training_blade", "crystal_sword"),
            lastPlayedTimestamp = 1000000L,
            lastSyncedMainSparks = 10,
            totalYogaXp = 5200,
            totalGoldSpent = 120,
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
        assertEquals(original.totalGoldSpent, loaded.totalGoldSpent)
    }

    @Test
    fun `normalizeId converts PascalCase to lower snake case`() {
        val data = GameProgress(
            party = listOf(PartyMemberData("HeroA", 1)),
            unlockedHeroIds = setOf("Shanti")
        )
        saveManager.saveGame(data)
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ctx.getSharedPreferences("game_save", android.content.Context.MODE_PRIVATE)
        val blob = prefs.getString("progress_blob_v2", null)

        assertNotNull(blob)
        assertTrue("blob should contain normalized hero_a", blob!!.contains("hero_a"))
        assertTrue("blob should contain normalized shanti", blob.contains("shanti"))
    }

    @Test
    fun `save with PascalCase hero IDs normalizes to lower snake case on save`() {
        val data = GameProgress(
            party = listOf(PartyMemberData("HeroA", 1)),
            unlockedHeroIds = setOf("HeroB", "HeroC"),
            defeatedMonsterIds = setOf("MonsterX")
        )
        saveManager.saveGame(data)
        val loaded = saveManager.loadGame()

        assertTrue("hero_a should be lowercase", loaded.party.any { it.heroId == "hero_a" })
        assertTrue("hero_b should be lowercase", loaded.unlockedHeroIds.contains("hero_b"))
        assertTrue("hero_c should be lowercase", loaded.unlockedHeroIds.contains("hero_c"))
        assertTrue("monster_x should be lowercase", loaded.defeatedMonsterIds.contains("monster_x"))
    }

    @Test
    fun `save with mixed case IDs normalizes correctly`() {
        val data = GameProgress(
            party = listOf(
                PartyMemberData("Shanti", 1),
                PartyMemberData("VIRYA", 2),
                PartyMemberData("dhairya", 3)
            ),
            unlockedHeroIds = setOf("Maitri_Santosha"),
            defeatedMonsterIds = setOf("Bhaya_Fear", "Krodha--Anger")
        )
        saveManager.saveGame(data)
        val loaded = saveManager.loadGame()

        assertEquals("shanti", loaded.party[0].heroId)
        assertEquals("virya", loaded.party[1].heroId)
        assertEquals("dhairya", loaded.party[2].heroId)
        assertTrue(loaded.unlockedHeroIds.contains("maitri_santosha"))
        assertTrue(loaded.defeatedMonsterIds.contains("bhaya_fear"))
        assertTrue(loaded.defeatedMonsterIds.contains("krodha_anger"))
    }

    @Test
    fun `empty save roundtrip preserves defaults`() {
        val original = GameProgress()
        saveManager.saveGame(original)
        val loaded = saveManager.loadGame()

        assertEquals(2, loaded.version)
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
            party = listOf(PartyMemberData("shanti", 5)),
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
        assertEquals(2, loaded.version)
    }

    @Test
    fun `party save data roundtrip preserves all fields`() {
        val original = GameProgress(
            party = listOf(
                PartyMemberData("shanti", 3, listOf("training_blade", "simple_beads")),
                PartyMemberData("virya", 5, listOf("fury_blade")),
                PartyMemberData("santosha", 2),
                PartyMemberData("dhairya", 1)
            )
        )
        saveManager.saveGame(original)
        val loaded = saveManager.loadGame()

        assertEquals(4, loaded.party.size)

        val shanti = loaded.party.find { it.heroId == "shanti" }
        assertNotNull(shanti)
        assertEquals(3, shanti!!.level)
        assertEquals(listOf("training_blade", "simple_beads"), shanti.equippedItemIds)

        val virya = loaded.party.find { it.heroId == "virya" }
        assertNotNull(virya)
        assertEquals(5, virya!!.level)

        val dhairya = loaded.party.find { it.heroId == "dhairya" }
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
        assertEquals(2, data.version)
        assertEquals(0, data.sparks)
        assertEquals(1, data.yogaLevel)
    }

    @Test
    fun `versioned JSON blob roundtrip preserves all fields`() {
        val original = GameProgress(
            version = 2,
            party = listOf(PartyMemberData("shanti", 3, listOf("blade"))),
            unlockedHeroIds = setOf("shanti"),
            sparks = 200,
            yogaLevel = 5,
            totalBattlesWon = 20,
            inventory = listOf("potion"),
            defeatedMonsterIds = setOf("bhaya"),
            totalYogaXp = 8000,
            totalGoldSpent = 500
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
        assertEquals(2, data.version)
        assertNotNull(data.party)
        assertNotNull(data.unlockedHeroIds)
        assertNotNull(data.inventory)
        assertNotNull(data.defeatedMonsterIds)
    }
}
