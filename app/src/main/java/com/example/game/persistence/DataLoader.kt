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

    fun getHero(id: Int): Hero = heroes.first { it.id == id }

    fun getMonster(id: String): Monster = monsters.first { it.id == id }

    fun getEquipment(id: String): Equipment = equipment.first { it.id == id }

    fun getCombo(id: String): ComboSkill = combos.first { it.id == id }

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
