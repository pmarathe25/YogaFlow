package com.example.game.viewmodel

import com.example.game.model.BattleState
import com.example.game.model.EquipmentTier
import com.example.game.model.GameProgress
import com.example.game.persistence.DataLoader
import com.example.game.persistence.GameSaveManager
import kotlinx.coroutines.flow.MutableStateFlow

internal class EconomyManager(
    private val _saveData: MutableStateFlow<GameProgress>,
    private val _party: MutableStateFlow<List<com.example.game.model.PartyMemberData>>,
    private val _battleState: MutableStateFlow<BattleState?>,
    private val _goldEarned: MutableStateFlow<Int>,
    private val saveManager: GameSaveManager
) {
    fun purchaseItem(itemId: String): Boolean {
        val item = DataLoader.getEquipment(itemId)
        val data = _saveData.value
        if (data.yogaLevel < item.yogaLevelRequired) return false
        if (data.gold < item.goldCost) return false
        if (itemId in data.inventory) return false
        if (item.tier == EquipmentTier.UNIQUE) return false

        _saveData.value = data.copy(
            gold = data.gold - item.goldCost,
            inventory = data.inventory + itemId
        )
        saveManager.saveGame(_saveData.value)
        return true
    }

    fun resetAllProgress() {
        saveManager.resetToDefault()
        val defaultData = saveManager.loadGame()
        _saveData.value = defaultData
        _party.value = defaultData.party
        _battleState.value = null
        _goldEarned.value = 0
    }
}