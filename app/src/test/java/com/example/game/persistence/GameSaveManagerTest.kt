package com.example.game.persistence

import androidx.test.core.app.ApplicationProvider
import com.example.game.model.HeroSaveData
import com.example.game.persistence.GameSaveManager.GameSaveData
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
        val original = GameSaveData(
            version = 2,
            party = listOf(
                HeroSaveData("shanti", 3, 300, 0, 20, false, listOf("training_blade")),
                HeroSaveData("virya", 5, 400, 50, 40, false, listOf("ember_pendant"))
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
        assertEquals(original.party[0].currentHp, loaded.party[0].currentHp)
        assertEquals(original.party[0].shield, loaded.party[0].shield)
        assertEquals(original.party[0].ultimateGauge, loaded.party[0].ultimateGauge)
        assertEquals(original.party[0].isDead, loaded.party[0].isDead)
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
        val data = GameSaveData(
            party = listOf(HeroSaveData("HeroA", 1, 100, 0, 0, false, emptyList())),
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
        val data = GameSaveData(
            party = listOf(HeroSaveData("HeroA", 1, 100, 0, 0, false, emptyList())),
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
        val data = GameSaveData(
            party = listOf(
                HeroSaveData("Shanti", 1, 100, 0, 0, false, emptyList()),
                HeroSaveData("VIRYA", 2, 200, 0, 0, false, emptyList()),
                HeroSaveData("dhairya", 3, 300, 0, 0, false, emptyList())
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
        val original = GameSaveData()
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
        val original = GameSaveData(
            sparks = 999, yogaLevel = 10,
            party = listOf(HeroSaveData("shanti", 5, 500, 0, 0, false, emptyList())),
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
        var data = GameSaveData(sparks = 100, totalBattlesWon = 5)
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
        val original = GameSaveData(
            party = listOf(
                HeroSaveData("shanti", 3, 280, 15, 40, false, listOf("training_blade", "simple_beads")),
                HeroSaveData("virya", 5, 320, 0, 100, false, listOf("fury_blade")),
                HeroSaveData("santosha", 2, 450, 30, 10, false, emptyList()),
                HeroSaveData("dhairya", 1, 100, 0, 0, true, listOf("guiding_lance"))
            )
        )
        saveManager.saveGame(original)
        val loaded = saveManager.loadGame()

        assertEquals(4, loaded.party.size)

        val shanti = loaded.party.find { it.heroId == "shanti" }
        assertNotNull(shanti)
        assertEquals(3, shanti!!.level)
        assertEquals(280, shanti.currentHp)
        assertEquals(15, shanti.shield)
        assertEquals(40, shanti.ultimateGauge)
        assertFalse(shanti.isDead)
        assertEquals(listOf("training_blade", "simple_beads"), shanti.equippedItemIds)

        val virya = loaded.party.find { it.heroId == "virya" }
        assertNotNull(virya)
        assertEquals(5, virya!!.level)
        assertEquals(100, virya.ultimateGauge)

        val dhairya = loaded.party.find { it.heroId == "dhairya" }
        assertNotNull(dhairya)
        assertTrue(dhairya!!.isDead)
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
}
