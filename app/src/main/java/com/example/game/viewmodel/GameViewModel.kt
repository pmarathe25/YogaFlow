package com.example.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.battle.TurnManager
import com.example.game.model.*
import com.example.game.model.HeroSkin
import com.example.game.model.BattlePhase.*
import com.example.game.persistence.DataLoader
import com.example.game.persistence.GameSaveManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _goldEarned = MutableStateFlow(0)
    val goldEarned: StateFlow<Int> = _goldEarned.asStateFlow()

    private val battleOrchestrator = BattleOrchestrator(
        turnManager, saveManager,
        _battleState, _currentMonster, _currentScreen, _battleLog,
        _isProcessingTurn, _goldEarned, _party, _saveData, _error,
        viewModelScope
    )
    private val partyManager = PartyManager(_party, _saveData, saveManager)
    private val economyManager = EconomyManager(_saveData, _party, _battleState, _goldEarned, saveManager)
    private val gameSyncManager = GameSyncManager(_saveData, _party, saveManager, application, viewModelScope)

    init {
        loadGame()
        viewModelScope.launch { gameSyncManager.syncWithMainApp() }
    }

    fun refreshSync() = gameSyncManager.refreshSync()

    fun navigateTo(screen: GameScreen) {
        _currentScreen.value = screen
    }

    fun navigateBack() {
        _currentScreen.value = GameScreen.HUB
    }

    private fun loadGame() {
        val data = saveManager.loadGame()
        _saveData.value = data
        _party.value = data.party
        partyManager.initializeDefaultSkins()
    }

    fun startBattle(monsterId: String) = battleOrchestrator.startBattle(monsterId)
    fun onIntroComplete() = battleOrchestrator.onIntroComplete()
    fun skipTurn(heroId: String) = battleOrchestrator.skipTurn(heroId)
    fun cancelAction() = battleOrchestrator.cancelAction()
    fun selectHero(heroId: String) {
        val state = _battleState.value ?: return
        if (state.phase != PLAYER_TURN) return
        if (heroId in state.heroesActedThisRound) return
        _battleState.value = state.copy(selectedHeroId = heroId, currentActorId = heroId)
    }
    fun deselectHero() {
        val state = _battleState.value ?: return
        _battleState.value = state.copy(selectedHeroId = "", currentActorId = "")
    }
    fun executeSkill(heroId: String, skill: Skill, customTargets: List<String>? = null) =
        battleOrchestrator.executeSkill(heroId, skill, customTargets)
    fun executeUltimate(heroId: String) = battleOrchestrator.executeUltimate(heroId)
    fun executeCombo(participantIds: Set<String>) = battleOrchestrator.executeCombo(participantIds)
    fun executeComboById(comboId: String) = battleOrchestrator.executeComboById(comboId)

    fun purchaseHero(heroId: Int): Boolean = partyManager.purchaseHero(heroId)
    fun levelUpHero(heroId: Int): Boolean = partyManager.levelUpHero(heroId)
    fun getHeroLevelUpCost(heroId: Int): Int = partyManager.getHeroLevelUpCost(heroId)
    fun equipItem(heroId: Int, itemId: String): Boolean = partyManager.equipItem(heroId, itemId)
    fun unequipItem(heroId: Int, itemId: String) = partyManager.unequipItem(heroId, itemId)
    fun getEquippedItems(heroId: Int): List<Equipment> = partyManager.getEquippedItems(heroId)
    fun isSkillUnlocked(heroId: Int, skillId: String): Boolean = partyManager.isSkillUnlocked(heroId, skillId)
    fun unlockSkill(heroId: Int, skillId: String): Boolean = partyManager.unlockSkill(heroId, skillId)
    fun equipSkin(heroId: Int, skinId: String): Boolean = partyManager.equipSkin(heroId, skinId)
    fun unlockSkin(skinId: String): Boolean = partyManager.unlockSkin(skinId)
    fun getEquippedSkin(heroId: Int): HeroSkin? = partyManager.getEquippedSkin(heroId)
    fun getUnlockedSkinsForHero(heroId: Int): List<HeroSkin> = partyManager.getUnlockedSkinsForHero(heroId)

    fun purchaseItem(itemId: String): Boolean = economyManager.purchaseItem(itemId)
    fun resetAllProgress() = economyManager.resetAllProgress()

    fun clearError() { _error.value = null }
}