package com.example.game.persistence

import android.content.Context
import com.example.game.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DataLoader {

    private lateinit var context: Context
    private val gson = Gson()

    val heroes: List<Hero> by lazy { loadList("heroes.json") }
    val monsters: List<Monster> by lazy { loadList("monsters.json") }
    val equipment: List<Equipment> by lazy {
        val text = context.assets.open("game/equipment.json").bufferedReader().use { it.readText() }
        val map = gson.fromJson(text, Map::class.java)
        val items = map["items"] as? List<Map<String, Any>> ?: emptyList()
        gson.fromJson(gson.toJson(items), object : TypeToken<List<Equipment>>() {}.type)
    }
    val combos: List<ComboSkill> by lazy { loadList("combos.json") }
    val trophies: List<Trophy> by lazy { loadList("trophies.json") }

    val setBonuses: List<SetBonus> by lazy {
        val text = context.assets.open("game/equipment.json")
            .bufferedReader().use { it.readText() }
        val map = gson.fromJson(text, Map::class.java)
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

    fun getHero(id: String): Hero = heroes.first { it.id == id }

    fun getHeroForLevel(level: Int): Hero =
        heroes.filter { level >= it.unlockYogaLevel }.maxByOrNull { it.unlockYogaLevel }
            ?: heroes.first()

    fun getMonster(id: String): Monster = monsters.first { it.id == id }

    fun getEquipment(id: String): Equipment = equipment.first { it.id == id }

    fun getCombo(id: String): ComboSkill = combos.first { it.id == id }

    fun findCombo(heroIds: List<String>): ComboSkill? =
        combos.firstOrNull { it.requiredHeroes.toSet() == heroIds.toSet() }

    fun getTrophy(id: String): Trophy = trophies.first { it.id == id }

    fun getTrophiesByCategory(category: TrophyCategory): List<Trophy> =
        trophies.filter { it.category == category }

    private inline fun <reified T> loadList(file: String): List<T> {
        val json = context.assets.open("game/$file").bufferedReader().use { it.readText() }
        return gson.fromJson(json, object : TypeToken<List<T>>() {}.type)
    }
}
