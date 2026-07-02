package com.example.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.YogaDatabase
import com.example.game.battle.TurnManager
import com.example.game.model.*
import com.example.game.model.TargetType.*
import com.example.game.model.DamageType.*
import com.example.game.model.BattlePhase.*
import com.example.game.model.TurnAction.*
import com.example.game.persistence.DataLoader
import com.example.game.persistence.GameSaveManager
import com.example.game.persistence.GameSaveManager.GameSaveData
import com.example.model.LevelDefinitions
import com.example.model.XpCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GameScreen { HUB, BATTLE, PARTY, EQUIPMENT, TROPHIES, SHOP, SETTINGS, BATTLE_RESULT }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    init {
        DataLoader.init(application)
    }
    private val saveManager = GameSaveManager(application)
    private val turnManager = TurnManager()

    private val _currentScreen = MutableStateFlow(GameScreen.HUB)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()

    private val _battleState = MutableStateFlow<BattleState?>(null)
    val battleState: StateFlow<BattleState?> = _battleState.asStateFlow()

    private val _saveData = MutableStateFlow(GameSaveData())
    val saveData: StateFlow<GameSaveData> = _saveData.asStateFlow()

    private val _party = MutableStateFlow<List<HeroInstance>>(emptyList())
    val party: StateFlow<List<HeroInstance>> = _party.asStateFlow()

    private val _currentMonster = MutableStateFlow<Monster?>(null)
    val currentMonster: StateFlow<Monster?> = _currentMonster.asStateFlow()

    private val _battleLog = MutableStateFlow<List<String>>(emptyList())
    val battleLog: StateFlow<List<String>> = _battleLog.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isProcessingTurn = MutableStateFlow(false)
    val isProcessingTurn: StateFlow<Boolean> = _isProcessingTurn.asStateFlow()

    init {
        loadGame()
        viewModelScope.launch { syncWithMainApp() }
    }

    fun refreshSync() {
        viewModelScope.launch { syncWithMainApp() }
    }

    private suspend fun syncWithMainApp() = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        val db = YogaDatabase.getDatabase(app)
        val sessions = db.yogaSessionDao().getAllSessions().first()

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
        val uniqueDays = sessions.map { dateFormat.format(java.util.Date(it.timestamp)) }.distinct()
        val mainSparks = uniqueDays.size

        var xpSum = 0
        sessions.forEach { session ->
            xpSum += XpCalculator.calculateSessionXp(session.durationMinutes, session.flowId)
        }
        xpSum += mainSparks * 150
        val computedLevel = LevelDefinitions.getLevelForXp(xpSum).level

        val data = _saveData.value
        var updated = data
        if (data.yogaLevel != computedLevel) {
            updated = updated.copy(yogaLevel = computedLevel)
        }
        val delta = mainSparks - data.lastSyncedMainSparks
        if (delta > 0) {
            updated = updated.copy(sparks = updated.sparks + delta)
        }
        if (updated.lastSyncedMainSparks != mainSparks) {
            updated = updated.copy(lastSyncedMainSparks = mainSparks)
        }
        if (updated != data) {
            _saveData.value = updated
            restoreParty(updated)
            saveGame()
        }
    }

    fun navigateTo(screen: GameScreen) {
        _currentScreen.value = screen
    }

    fun navigateBack() {
        _currentScreen.value = GameScreen.HUB
    }

    fun addBattleLog(message: String) {
        _battleLog.value = _battleLog.value + message
    }

    private fun loadGame() {
        val data = saveManager.loadGame()
        _saveData.value = data
        restoreParty(data)
    }

    private fun saveGame() {
        saveManager.saveGame(_saveData.value)
    }

    private fun restoreParty(data: GameSaveData) {
        val unlocked = DataLoader.heroes.filter { it.unlockYogaLevel <= data.yogaLevel }
        val newParty = unlocked.map { heroDef ->
            val savedHero = data.party.find { it.heroId == heroDef.id }
            if (savedHero != null) {
                heroDef.createInstance(savedHero.level, savedHero.equippedItemIds)
            } else {
                heroDef.createInstance(1, emptyList())
            }
        }
        _party.value = newParty
    }

    fun getUnlockedHeroes(): List<Hero> {
        return DataLoader.heroes.filter { it.unlockYogaLevel <= _saveData.value.yogaLevel }
    }

    fun getAvailableHeroes(): List<Hero> {
        return getUnlockedHeroes().filter { h -> _party.value.none { it.heroId == h.id } }
    }

    fun startBattle(monsterId: String) {
        val monster = DataLoader.getMonster(monsterId)
        if (_party.value.isEmpty()) {
            _error.value = "No heroes in party!"
            return
        }
        _currentMonster.value = monster
        val monsterInstance = monster.createInstance()
        val initialState = turnManager.startBattle(_party.value, listOf(monsterInstance))
        _battleState.value = initialState
        _battleLog.value = emptyList()
        _currentScreen.value = GameScreen.BATTLE
        addBattleLog("Battle begins! ${monster.englishName} appears!")

        if (initialState.phase == ENEMY_TURN) {
            scheduleMonsterTurn(initialState.currentActorId)
        }
    }

    private fun updateComboAvailability(state: BattleState): BattleState {
        val aliveHeroIds = state.aliveHeroes.map { it.heroId }.toSet()
        val isAvailable = DataLoader.combos.any { combo ->
            aliveHeroIds.containsAll(combo.requiredHeroes)
        }
        return state.copy(isComboAvailable = isAvailable)
    }

    fun cancelAction() {
        val state = _battleState.value ?: return
        _battleState.value = state.copy(pendingSkill = null)
    }

    fun executeSkill(heroId: String, skill: Skill, customTargets: List<String>? = null) {
        val state = _battleState.value ?: return
        if (state.phase != PLAYER_TURN || _isProcessingTurn.value) return
        val hero = state.heroes.find { it.heroId == heroId && !it.isDead } ?: return

        val targets = customTargets ?: BattleEngine.resolveTargets(skill, heroId, state)

        if (customTargets == null) {
            val autoTarget = when (skill.targetType) {
                SINGLE_ENEMY -> state.aliveMonsters.firstOrNull()?.monsterId
                SINGLE_ALLY -> if (state.aliveHeroes.size == 1) hero.heroId else null
                SELF -> hero.heroId
                ALL_ALLIES, ALL_ENEMIES, ALL -> "ALL"
                else -> null
            }

            if (autoTarget == "ALL") {
                // proceed
            } else if (autoTarget != null) {
                executeSkill(heroId, skill, listOf(autoTarget))
                return
            } else {
                _battleState.value = state.copy(pendingSkill = skill)
                return
            }
        }

        if (targets.isEmpty()) return

        viewModelScope.launch {
            _isProcessingTurn.value = true
            _battleState.value = state.copy(pendingSkill = null)

            val result = turnManager.executeSkill(_battleState.value ?: return@launch, heroId, skill, targets)
            _battleState.value = updateComboAvailability(result.newState)
            result.logMessages.forEach { addBattleLog(it) }

            delay(1000)
            advanceToNextTurn()
            _isProcessingTurn.value = false
        }
    }

    fun executeUltimate(heroId: String) {
        val state = _battleState.value ?: return
        if (state.phase != PLAYER_TURN || _isProcessingTurn.value) return
        val hero = state.heroes.find { it.heroId == heroId && !it.isDead } ?: return
        if (hero.ultimateGauge < 100) return

        viewModelScope.launch {
            _isProcessingTurn.value = true

            val result = turnManager.executeUltimate(_battleState.value ?: return@launch, heroId)
            _battleState.value = updateComboAvailability(result.newState)
            result.logMessages.forEach { addBattleLog(it) }

            delay(1500)
            advanceToNextTurn()
            _isProcessingTurn.value = false
        }
    }

    fun executeCombo(participantIds: Set<String>) {
        val state = _battleState.value ?: return
        if (state.phase != PLAYER_TURN || _isProcessingTurn.value) return
        val combo = DataLoader.findCombo(participantIds.toList()) ?: return
        val participants = participantIds.mapNotNull { id -> state.heroes.find { it.heroId == id && !it.isDead } }
        if (participants.size != combo.requiredHeroes.size) return

        viewModelScope.launch {
            _isProcessingTurn.value = true

            val result = turnManager.executeCombo(_battleState.value ?: return@launch, combo, participantIds)
            _battleState.value = updateComboAvailability(result.newState)
            result.logMessages.forEach { addBattleLog(it) }

            delay(2000)
            advanceToNextTurn()
            _isProcessingTurn.value = false
        }
    }

    fun executeComboById(comboId: String) {
        val combo = DataLoader.getCombo(comboId)
        val participantIds = combo.requiredHeroes.mapNotNull { name ->
            _battleState.value?.heroes?.find { it.name == name && !it.isDead }?.heroId
        }.toSet()
        if (participantIds.size != combo.requiredHeroes.size) return
        executeCombo(participantIds)
    }

    private suspend fun advanceToNextTurn() {
        val state = _battleState.value ?: return
        val result = turnManager.advanceTurn(state)
        _battleState.value = updateComboAvailability(result.newState)
        result.logMessages.forEach { addBattleLog(it) }

        if (result.victory) {
            delay(1000)
            _currentScreen.value = GameScreen.BATTLE_RESULT
            onBattleWon()
            return
        }
        if (result.defeat) {
            _party.value.forEach { hero ->
                hero.currentHp = hero.maxHp
                hero.shield = 0
                hero.ultimateGauge = 0
                hero.isDead = false
            }
            delay(1000)
            _currentScreen.value = GameScreen.BATTLE_RESULT
            return
        }

        delay(1200)

        if (result.newState.phase == ENEMY_TURN) {
            scheduleMonsterTurn(result.newState.currentActorId)
        }
    }

    private fun scheduleMonsterTurn(monsterId: String) {
        viewModelScope.launch {
            executeMonsterTurnLoop(monsterId)
        }
    }

    private suspend fun executeMonsterTurnLoop(monsterId: String) {
        var state = _battleState.value ?: return
        var result = turnManager.executeMonsterTurn(state, monsterId)
        _battleState.value = result.newState
        result.logMessages.forEach { addBattleLog(it) }

        delay(1500)

        val monster = result.newState.monsters.find { it.monsterId == monsterId }
        while (monster != null && monster.extraActionsThisRound > 0 && !result.newState.isBattleOver) {
            state = _battleState.value ?: return
            result = turnManager.executeMonsterTurn(state, monsterId)
            _battleState.value = result.newState
            result.logMessages.forEach { addBattleLog(it) }
            delay(1500)
        }

        advanceToNextTurn()
    }

    private fun onBattleWon() {
        val monster = _currentMonster.value ?: return
        val data = _saveData.value
        val karmaReward = 50 + (monster.difficultyTier.ordinal * 25)

        _party.value.forEach { hero ->
            hero.currentHp = hero.maxHp
            hero.shield = 0
            hero.ultimateGauge = 0
            hero.isDead = false
        }

        _saveData.value = data.copy(
            party = _party.value.map { it.toSaveData() },
            totalBattlesWon = data.totalBattlesWon + 1,
            totalKarmaXp = data.totalKarmaXp + karmaReward,
            defeatedMonsterIds = data.defeatedMonsterIds + monster.id,
            lastPlayedTimestamp = System.currentTimeMillis()
        )
        saveGame()
    }

    // --- Equipment ---

    fun purchaseItem(itemId: String): Boolean {
        val item = DataLoader.getEquipment(itemId)
        val data = _saveData.value
        val availableGold = data.totalKarmaXp - data.totalGoldSpent
        if (data.yogaLevel < item.yogaLevelRequired) return false
        if (availableGold < item.goldCost) return false
        if (itemId in data.inventory) return false

        _saveData.value = data.copy(
            totalGoldSpent = data.totalGoldSpent + item.goldCost,
            inventory = data.inventory + itemId
        )
        saveGame()
        return true
    }

    fun equipItem(heroId: String, itemId: String): Boolean {
        val hero = _party.value.find { it.heroId == heroId } ?: return false
        val item = DataLoader.getEquipment(itemId)
        if (itemId !in _saveData.value.inventory) return false

        hero.equippedItems.removeAll { existingId ->
            DataLoader.equipment.find { it.id == existingId }?.slot == item.slot
        }

        hero.equippedItems.add(itemId)
        _saveData.value = _saveData.value.copy(
            inventory = _saveData.value.inventory - itemId,
            party = _party.value.map { it.toSaveData() }
        )
        saveGame()
        return true
    }

    fun unequipItem(heroId: String, itemId: String) {
        val hero = _party.value.find { it.heroId == heroId } ?: return
        hero.equippedItems.remove(itemId)
        _saveData.value = _saveData.value.copy(
            inventory = _saveData.value.inventory + itemId,
            party = _party.value.map { it.toSaveData() }
        )
        saveGame()
    }

    fun getEquippedItems(heroId: String): List<Equipment> {
        val hero = _party.value.find { it.heroId == heroId } ?: return emptyList()
        return hero.equippedItems.mapNotNull { DataLoader.getEquipment(it) }
    }

    // --- Economy ---

    fun getAvailableGold(): Int {
        val data = _saveData.value
        return data.totalKarmaXp - data.totalGoldSpent
    }

    // --- Hero Level Up ---

    fun getHeroLevelUpCost(heroId: String): Int {
        val hero = _party.value.find { it.heroId == heroId } ?: return 0
        return 100 * hero.level
    }

    fun levelUpHero(heroId: String): Boolean {
        val hero = _party.value.find { it.heroId == heroId } ?: return false
        val cost = getHeroLevelUpCost(heroId)
        val data = _saveData.value
        val availableGold = data.totalKarmaXp - data.totalGoldSpent

        if (availableGold >= cost) {
            val nextLevel = hero.level + 1
            val def = DataLoader.getHero(heroId)
            val newInstance = def.createInstance(nextLevel, hero.equippedItems)

            _party.value = _party.value.map { if (it.heroId == heroId) newInstance else it }

            _saveData.value = data.copy(
                totalGoldSpent = data.totalGoldSpent + cost,
                party = _party.value.map { it.toSaveData() }
            )
            saveGame()
            return true
        }
        return false
    }

    // --- Hero Purchase ---

    fun purchaseHero(heroId: String): Boolean {
        val hero = DataLoader.getHero(heroId)
        val data = _saveData.value
        if (heroId in data.unlockedHeroIds) return false
        if (data.yogaLevel < hero.unlockYogaLevel) return false
        val sparkCost = hero.unlockYogaLevel * 10
        if (data.sparks < sparkCost) return false

        _saveData.value = data.copy(
            sparks = data.sparks - sparkCost,
            unlockedHeroIds = data.unlockedHeroIds + heroId
        )
        saveGame()
        return true
    }

    // --- Error handling ---

    fun clearError() { _error.value = null }
}

private fun HeroInstance.toSaveData() = HeroSaveData(
    heroId = heroId, level = level, currentHp = currentHp,
    shield = shield, ultimateGauge = ultimateGauge,
    isDead = isDead, equippedItemIds = equippedItems.toList()
)
