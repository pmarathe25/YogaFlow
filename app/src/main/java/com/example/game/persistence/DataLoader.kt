package com.example.game.persistence

import android.content.Context
import com.example.game.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DataLoader {

    private lateinit var context: Context
    private val gson = Gson()

    val isInitialized: Boolean
        get() = ::context.isInitialized

    val heroes: List<Hero> by lazy { loadList("heroes.json") }
    val skins: List<HeroSkin> by lazy { loadList("skins.json") }
    val monsters: List<Monster> by lazy { loadList("monsters.json") }
    val equipment: List<Equipment> by lazy {
        val text = context.assets.open("game/equipment.json").bufferedReader().use { it.readText() }
        val map = gson.fromJson(text, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        val items = map["items"] as? List<Map<String, Any>> ?: emptyList()
        gson.fromJson(gson.toJson(items), object : TypeToken<List<Equipment>>() {}.type)
    }
    val combos: List<ComboSkill> by lazy { loadList("combos.json") }
    val trophies: List<Trophy> by lazy { loadList("trophies.json") }

    val setBonuses: List<SetBonus> by lazy {
        val text = context.assets.open("game/equipment.json")
            .bufferedReader().use { it.readText() }
        val map = gson.fromJson(text, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        val bonuses = map["setBonuses"] as? List<Map<String, Any>> ?: emptyList()
        gson.fromJson(gson.toJson(bonuses), object : TypeToken<List<SetBonus>>() {}.type)
    }

    fun init(appContext: Context) {
        context = appContext.applicationContext
        heroes
        monsters
        equipment
        combos
        trophies
        setBonuses
    }

    // Safe lookups: never throw (which would otherwise crash the whole app on a
    // missing id). Fall back to the first entry so callers always get a valid object.
    fun getHero(id: Int): Hero = heroes.firstOrNull { it.id == id } ?: heroes.first()

    fun getMonster(id: String): Monster = monsters.firstOrNull { it.id == id } ?: monsters.first()

    fun getEquipment(id: String): Equipment = equipment.firstOrNull { it.id == id } ?: equipment.first()

    fun getCombo(id: String): ComboSkill = combos.firstOrNull { it.id == id } ?: combos.first()

    fun getSkin(skinId: String): HeroSkin? = skins.firstOrNull { it.skinId == skinId }

    fun getSkinsForHero(heroId: Int): List<HeroSkin> = skins.filter { it.heroId == heroId }

    fun getDefaultSkin(heroId: Int): HeroSkin? = skins.firstOrNull { it.heroId == heroId && it.unlockMethod == SkinUnlockMethod.DEFAULT }

    fun findCombo(heroIds: List<String>): ComboSkill? =
        combos.firstOrNull { combo ->
            val intIds = heroIds.mapNotNull { it.toIntOrNull() }.toSet()
            intIds == combo.requiredHeroes
        }

    private inline fun <reified T> loadList(file: String): List<T> {
        val json = context.assets.open("game/$file").bufferedReader().use { it.readText() }
        return gson.fromJson(json, object : TypeToken<List<T>>() {}.type)
    }
}
