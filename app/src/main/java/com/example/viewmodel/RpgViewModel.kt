package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.RpgManager
import com.example.db.YogaDatabase
import com.example.model.GardenShop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class RpgViewModel(application: Application) : AndroidViewModel(application) {
    private val database = YogaDatabase.getDatabase(application)
    
    val gardenItems = database.gardenItemDao().getAllItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // This will be injected from StatsViewModel
    var totalXpProvider: (() -> Int) = { 0 }
    var totalSparksProvider: (() -> Int) = { 0 }

    val availableXp = MutableStateFlow(0)
    val availableSparks = MutableStateFlow(0)

    private val _rpgUnlockedHeroes = MutableStateFlow<Set<String>>(emptySet())
    val rpgUnlockedHeroes: StateFlow<Set<String>> = _rpgUnlockedHeroes.asStateFlow()

    private val _rpgActiveParty = MutableStateFlow<List<String>>(emptyList())
    val rpgActiveParty: StateFlow<List<String>> = _rpgActiveParty.asStateFlow()

    private val _rpgSpentKarmaXp = MutableStateFlow(0)
    val rpgSpentKarmaXp: StateFlow<Int> = _rpgSpentKarmaXp.asStateFlow()

    private val _rpgSpentSparks = MutableStateFlow(0)
    val rpgSpentSparks: StateFlow<Int> = _rpgSpentSparks.asStateFlow()

    private val _rpgExtraKarmaXp = MutableStateFlow(0)
    val rpgExtraKarmaXp: StateFlow<Int> = _rpgExtraKarmaXp.asStateFlow()

    private val _rpgExtraSparks = MutableStateFlow(0)
    val rpgExtraSparks: StateFlow<Int> = _rpgExtraSparks.asStateFlow()

    private val _rpgStats = MutableStateFlow<Pair<Int, Int>>(Pair(0, 0))
    val rpgStats: StateFlow<Pair<Int, Int>> = _rpgStats.asStateFlow()

    private val _rpgHeroLevels = MutableStateFlow<Map<String, Int>>(emptyMap())
    val rpgHeroLevels: StateFlow<Map<String, Int>> = _rpgHeroLevels.asStateFlow()

    init {
        loadRpgData()
        // Observe XP and Sparks changes
        viewModelScope.launch {
            while (true) {
                availableXp.value = (totalXpProvider() + _rpgExtraKarmaXp.value - _rpgSpentKarmaXp.value).coerceAtLeast(0)
                availableSparks.value = (totalSparksProvider() + _rpgExtraSparks.value - _rpgSpentSparks.value).coerceAtLeast(0)
                delay(500)
            }
        }
    }

    fun loadRpgData() {
        val app = getApplication<Application>()
        _rpgUnlockedHeroes.value = RpgManager.getUnlockedHeroes(app)
        _rpgActiveParty.value = RpgManager.getActiveParty(app)
        _rpgSpentKarmaXp.value = RpgManager.getSpentKarmaXp(app)
        _rpgSpentSparks.value = RpgManager.getSpentSparks(app)
        _rpgExtraKarmaXp.value = RpgManager.getExtraKarmaXp(app)
        _rpgExtraSparks.value = RpgManager.getExtraSparks(app)
        _rpgStats.value = RpgManager.getStats(app)

        val levels = mutableMapOf<String, Int>()
        listOf("Shanti", "Santosha", "Virya", "Dhairya", "Maitri").forEach { heroId ->
            levels[heroId] = RpgManager.getHeroLevel(app, heroId)
        }
        _rpgHeroLevels.value = levels
    }

    fun buyGardenItem(type: String, x: Float, y: Float, onResult: (Boolean) -> Unit) {
        val cost = GardenShop.getCost(type)
        if (availableXp.value >= cost) {
            viewModelScope.launch {
                database.gardenItemDao().insert(com.example.db.GardenItemEntity(itemType = type, x = x, y = y))
                onResult(true)
            }
        } else {
            onResult(false)
        }
    }

    fun upgradeZone(zoneId: String, newLevel: Int, onResult: (Boolean) -> Unit) {
        val newType = "${zoneId}_${newLevel}"
        val cost = GardenShop.getCost(newType)
        if (availableXp.value >= cost) {
            viewModelScope.launch {
                val currentItems = database.gardenItemDao().getAllItems().first()
                val oldItem = currentItems.find { it.itemType.startsWith("${zoneId}_") }
                if (oldItem != null) {
                    database.gardenItemDao().delete(oldItem)
                }
                database.gardenItemDao().insert(com.example.db.GardenItemEntity(itemType = newType, x = 0f, y = 0f))
                onResult(true)
            }
        } else {
            onResult(false)
        }
    }

    fun removeGardenItem(item: com.example.db.GardenItemEntity) {
        viewModelScope.launch {
            database.gardenItemDao().delete(item)
        }
    }

    fun rpgUnlockHero(heroId: String, costSparks: Int, onResult: (Boolean) -> Unit) {
        val app = getApplication<Application>()
        if (availableSparks.value >= costSparks) {
            val newSpent = _rpgSpentSparks.value + costSparks
            RpgManager.saveSpentSparks(app, newSpent)
            RpgManager.unlockHero(app, heroId)
            
            _rpgSpentSparks.value = newSpent
            _rpgUnlockedHeroes.value = RpgManager.getUnlockedHeroes(app)
            onResult(true)
        } else {
            onResult(false)
        }
    }

    fun rpgLevelUpHero(heroId: String, costXp: Int, onResult: (Boolean) -> Unit) {
        val app = getApplication<Application>()
        if (availableXp.value >= costXp) {
            val newSpent = _rpgSpentKarmaXp.value + costXp
            RpgManager.saveSpentKarmaXp(app, newSpent)
            RpgManager.levelUpHero(app, heroId)
            
            _rpgSpentKarmaXp.value = newSpent
            loadRpgData()
            onResult(true)
        } else {
            onResult(false)
        }
    }

    fun rpgTogglePartyMember(heroId: String) {
        val app = getApplication<Application>()
        val currentParty = _rpgActiveParty.value.toMutableList()
        if (currentParty.contains(heroId)) {
            if (currentParty.size > 1) {
                currentParty.remove(heroId)
            }
        } else {
            if (currentParty.size < 3) {
                currentParty.add(heroId)
            }
        }
        RpgManager.saveActiveParty(app, currentParty)
        _rpgActiveParty.value = currentParty
    }

    fun rpgRecordBattleResult(won: Boolean, xpEarned: Int, sparksEarned: Int) {
        val app = getApplication<Application>()
        RpgManager.recordBattle(app, won)
        _rpgStats.value = RpgManager.getStats(app)
        
        val newExtraXp = _rpgExtraKarmaXp.value + xpEarned
        val newExtraSparks = _rpgExtraSparks.value + sparksEarned
        RpgManager.saveExtraKarmaXp(app, newExtraXp)
        RpgManager.saveExtraSparks(app, newExtraSparks)
        _rpgExtraKarmaXp.value = newExtraXp
        _rpgExtraSparks.value = newExtraSparks
    }
}