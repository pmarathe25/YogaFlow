package com.example.game.viewmodel

import com.example.game.model.*
import com.example.game.persistence.DataLoader
import com.example.game.persistence.GameSaveManager
import kotlinx.coroutines.flow.MutableStateFlow

internal class PartyManager(
    private val _party: MutableStateFlow<List<PartyMemberData>>,
    private val _saveData: MutableStateFlow<GameProgress>,
    private val saveManager: GameSaveManager
) {
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
        saveManager.saveGame(_saveData.value)
        return true
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
            saveManager.saveGame(_saveData.value)
            return true
        }
        return false
    }

    fun getHeroLevelUpCost(heroId: Int): Int {
        val hero = _party.value.find { it.heroId == heroId } ?: return 0
        return hero.level
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
        saveManager.saveGame(_saveData.value)
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
        saveManager.saveGame(_saveData.value)
    }

    fun getEquippedItems(heroId: Int): List<Equipment> {
        val partyMember = _party.value.find { it.heroId == heroId } ?: return emptyList()
        return partyMember.equippedItemIds.mapNotNull { DataLoader.getEquipment(it) }
    }
}