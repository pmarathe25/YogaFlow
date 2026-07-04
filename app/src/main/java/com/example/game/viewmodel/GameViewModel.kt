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

    private val _saveData = MutableStateFlow(GameProgress())
    val saveData: StateFlow<GameProgress> = _saveData.asStateFlow()

    private val _party = MutableStateFlow<List<PartyMemberData>>(emptyList())
    val party: StateFlow<List<PartyMemberData>> = _party.asStateFlow()

    private val _currentMonster = MutableStateFlow<Monster?>(null)
    val currentMonster: StateFlow<Monster?> = _currentMonster.asStateFlow()

    private val _battleLog = MutableStateFlow<List<String>>(emptyList())
    val battleLog: StateFlow<List<String>> = _battleLog.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isProcessingTurn = MutableStateFlow(false)
    val isProcessingTurn: StateFlow<Boolean> = _isProcessingTurn.asStateFlow()

    private val _selectedCardId = MutableStateFlow<String?>(null)
    val selectedCardId: StateFlow<String?> = _selectedCardId.asStateFlow()

    fun selectCard(cardId: String?) {
        _selectedCardId.value = cardId
    }

    fun dismissSelectedCard() {
        _selectedCardId.value = null
    }

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
            xpSum += com.example.model.XpCalculator.calculateSessionXp(session.durationMinutes, session.flowId)
        }
        xpSum += mainSparks * 150
        val computedLevel = com.example.model.LevelDefinitions.getLevelForXp(xpSum).level

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
        updated = updated.copy(totalYogaXp = xpSum)
        val previousGold = data.gold
        val expectedGold = xpSum / 10
        if (expectedGold > previousGold) {
            updated = updated.copy(gold = expectedGold)
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

    private fun restoreParty(data: GameProgress) {
        _party.value = data.party
    }

    fun getUnlockedHeroes(): List<Hero> {
        val unlockedIds = _saveData.value.unlockedHeroIds
        return DataLoader.heroes.filter { it.id in unlockedIds }
    }

    fun getAvailableHeroes(): List<Hero> {
        val data = _saveData.value
        return DataLoader.heroes.filter { h ->
            h.unlockYogaLevel <= data.yogaLevel && h.id !in data.unlockedHeroIds
        }
    }

    fun startBattle(monsterId: String) {
        val monster = DataLoader.getMonster(monsterId)
        val partyMembers = _party.value
        if (partyMembers.isEmpty()) {
            _error.value = "No heroes in party!"
            return
        }
        _currentMonster.value = monster

        val battleHeroes = partyMembers.mapNotNull { pm ->
            val heroDef = DataLoader.heroes.find { it.id == pm.heroId } ?: return@mapNotNull null
            heroDef.toCombatantState(pm)
        }
        val monsterCombatant = monster.toCombatantState()

        val initialState = turnManager.startBattle(battleHeroes, listOf(monsterCombatant))
        val firstActor = initialState.turnOrder.firstOrNull()
        val introState = if (firstActor != null) {
            initialState.copy(phase = BattlePhase.INTRO, currentActorId = firstActor.id)
        } else {
            initialState
        }
        _battleState.value = introState
        _battleLog.value = emptyList()
        _currentScreen.value = GameScreen.BATTLE
        addBattleLog("Battle begins! ${monster.englishName} appears!")
    }

    fun onIntroComplete() {
        val state = _battleState.value ?: return
        val firstActor = state.turnOrder.firstOrNull() ?: return
        val newState = if (firstActor.isHero) {
            state.copy(phase = PLAYER_TURN, currentActorId = firstActor.id)
        } else {
            state.copy(phase = ENEMY_TURN, currentActorId = firstActor.id)
        }
        _battleState.value = newState
        if (!firstActor.isHero) {
            viewModelScope.launch {
                delay(1200)
                executeMonsterTurnLoop(firstActor.id)
            }
        }
    }

    private fun updateComboAvailability(state: BattleState): BattleState {
        val aliveHeroIds = state.aliveHeroes.map { it.id }.toSet()
        val isAvailable = DataLoader.combos.any { combo ->
            aliveHeroIds.containsAll(combo.requiredHeroes.map(Int::toString))
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
        val hero = state.heroes.find { it.id == heroId && !it.isDefeated } ?: return

        val targets = customTargets ?: turnManager.resolveTargets(skill, heroId, state)

        if (customTargets == null) {
            val autoTarget = when (skill.targetType) {
                SINGLE_ENEMY -> state.aliveMonsters.firstOrNull()?.id
                SINGLE_ALLY -> if (state.aliveHeroes.size == 1) hero.id else null
                SELF -> hero.id
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
        val hero = state.heroes.find { it.id == heroId && !it.isDefeated } ?: return
        if (hero.gauge < 100) return

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
        val participants = participantIds.mapNotNull { id -> state.heroes.find { it.id == id && !it.isDefeated } }
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
        val participantIds = combo.requiredHeroes.mapNotNull { heroId ->
            val heroDef = DataLoader.heroes.find { it.id == heroId }
            val heroName = heroDef?.name?.split(" ")?.first()
            _battleState.value?.heroes?.find { it.name == heroName && !it.isDefeated }?.id
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

        val monster = result.newState.monsters.find { it.id == monsterId }
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
        val isFirstDefeat = monster.id.lowercase() !in data.defeatedMonsterIds

        var inventory = data.inventory
        if (isFirstDefeat && monster.firstDefeatItemReward != null) {
            inventory = inventory + monster.firstDefeatItemReward
        }

        _saveData.value = data.copy(
            totalBattlesWon = data.totalBattlesWon + 1,
            defeatedMonsterIds = data.defeatedMonsterIds + monster.id.lowercase(),
            inventory = inventory,
            lastPlayedTimestamp = System.currentTimeMillis()
        )
        saveGame()
    }

    fun resetAllProgress() {
        saveManager.resetToDefault()
        val defaultData = saveManager.loadGame()
        _saveData.value = defaultData
        _party.value = defaultData.party
        _battleState.value = null
    }

    // --- Equipment ---

    fun purchaseItem(itemId: String): Boolean {
        val item = DataLoader.getEquipment(itemId)
        val data = _saveData.value
        if (data.yogaLevel < item.yogaLevelRequired) return false
        if (data.gold < item.goldCost) return false
        if (itemId in data.inventory) return false

        _saveData.value = data.copy(
            gold = data.gold - item.goldCost,
            inventory = data.inventory + itemId
        )
        saveGame()
        return true
    }

    fun equipItem(heroId: Int, itemId: String): Boolean {
        val partyMember = _party.value.find { it.heroId == heroId } ?: return false
        val item = DataLoader.getEquipment(itemId)
        if (itemId !in _saveData.value.inventory) return false

        val updatedItems = partyMember.equippedItemIds.toMutableList()
        updatedItems.removeAll { existingId ->
            DataLoader.equipment.find { it.id == existingId }?.slot == item.slot
        }
        updatedItems.add(itemId)

        val newParty = _party.value.map {
            if (it.heroId == heroId) it.copy(equippedItemIds = updatedItems) else it
        }
        _party.value = newParty
        _saveData.value = _saveData.value.copy(
            inventory = _saveData.value.inventory - itemId,
            party = newParty
        )
        saveGame()
        return true
    }

    fun unequipItem(heroId: Int, itemId: String) {
        val partyMember = _party.value.find { it.heroId == heroId } ?: return

        val updatedItems = partyMember.equippedItemIds.toMutableList()
        updatedItems.remove(itemId)

        val newParty = _party.value.map {
            if (it.heroId == heroId) it.copy(equippedItemIds = updatedItems) else it
        }
        _party.value = newParty
        _saveData.value = _saveData.value.copy(
            inventory = _saveData.value.inventory + itemId,
            party = newParty
        )
        saveGame()
    }

    fun getEquippedItems(heroId: Int): List<Equipment> {
        val partyMember = _party.value.find { it.heroId == heroId } ?: return emptyList()
        return partyMember.equippedItemIds.mapNotNull { DataLoader.getEquipment(it) }
    }

    // --- Economy ---

    // --- Hero Level Up ---

    fun getHeroLevelUpCost(heroId: Int): Int {
        val hero = _party.value.find { it.heroId == heroId } ?: return 0
        return hero.level
    }

    fun levelUpHero(heroId: Int): Boolean {
        val hero = _party.value.find { it.heroId == heroId } ?: return false
        val cost = getHeroLevelUpCost(heroId)
        val data = _saveData.value

        if (data.sparks >= cost) {
            val nextLevel = hero.level + 1
            val newParty = _party.value.map { if (it.heroId == heroId) it.copy(level = nextLevel) else it }

            _party.value = newParty
            _saveData.value = data.copy(
                sparks = data.sparks - cost,
                party = newParty
            )
            saveGame()
            return true
        }
        return false
    }

    // --- Hero Purchase ---

    fun purchaseHero(heroId: Int): Boolean {
        val hero = DataLoader.getHero(heroId)
        val data = _saveData.value
        if (heroId in data.unlockedHeroIds) return false
        if (data.yogaLevel < hero.unlockYogaLevel) return false
        val sparkCost = hero.unlockYogaLevel
        if (data.sparks < sparkCost) return false

        val newParty = data.party + PartyMemberData(heroId = heroId)
        _party.value = _party.value + PartyMemberData(heroId = heroId)
        _saveData.value = data.copy(
            sparks = data.sparks - sparkCost,
            unlockedHeroIds = data.unlockedHeroIds + heroId,
            party = newParty
        )
        saveGame()
        return true
    }

    // --- Error handling ---

    fun clearError() { _error.value = null }
}