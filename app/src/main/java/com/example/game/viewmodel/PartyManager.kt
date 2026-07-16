package com.example.game.viewmodel

import com.example.game.model.*
import com.example.game.model.SkinUnlockMethod
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

        val starterSkills = hero.skills
            .filter { it.karmaXpCost == 0 && it.ultimateGain != 0 }
            .map { it.id }
            .toSet()
        val newUnlockedMap = data.unlockedSkillIds.toMutableMap()
        newUnlockedMap[heroId] = starterSkills

        _saveData.value = data.copy(
            sparks = data.sparks - sparkCost,
            unlockedHeroIds = data.unlockedHeroIds + heroId,
            party = newParty,
            unlockedSkillIds = newUnlockedMap
        )
        saveManager.saveGame(_saveData.value)
        return true
    }

    fun isSkillUnlocked(heroId: Int, skillId: String): Boolean {
        val unlockedForHero = _saveData.value.unlockedSkillIds[heroId] ?: emptySet()
        return skillId in unlockedForHero
    }

    fun unlockSkill(heroId: Int, skillId: String): Boolean {
        val data = _saveData.value
        val heroDef = DataLoader.heroes.find { it.id == heroId } ?: return false
        val skill = (heroDef.skills + heroDef.ultimate).find { it.id == skillId } ?: return false

        val currentUnlocked = data.unlockedSkillIds[heroId] ?: emptySet()
        if (skillId in currentUnlocked) return false

        if (data.karmaXp < skill.karmaXpCost) return false

        if (heroId !in data.unlockedHeroIds) return false

        val newUnlocked = currentUnlocked + skillId
        val newUnlockedMap = data.unlockedSkillIds.toMutableMap()
        newUnlockedMap[heroId] = newUnlocked

        _saveData.value = data.copy(
            karmaXp = data.karmaXp - skill.karmaXpCost,
            unlockedSkillIds = newUnlockedMap
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

    fun initializeDefaultSkins() {
        val data = _saveData.value
        var updated = data
        var changed = false

        val defaultSkins = DataLoader.skins.filter { it.unlockMethod == SkinUnlockMethod.DEFAULT }
        for (skin in defaultSkins) {
            if (skin.skinId !in updated.unlockedSkinIds) {
                updated = updated.copy(unlockedSkinIds = updated.unlockedSkinIds + skin.skinId)
                changed = true
            }
        }

        val newParty = updated.party.map { pm ->
            if (pm.skinId == null || DataLoader.getSkin(pm.skinId) == null) {
                val defaultSkin = DataLoader.getDefaultSkin(pm.heroId)
                pm.copy(skinId = defaultSkin?.skinId)
            } else pm
        }

        val newHeroSkins = updated.heroSkins.toMutableMap()
        for (pm in newParty) {
            val skinId = pm.skinId
            if (skinId != null && updated.heroSkins[pm.heroId] != skinId) {
                newHeroSkins[pm.heroId] = skinId
                changed = true
            }
        }

        if (newParty != updated.party) changed = true

        if (changed) {
            updated = updated.copy(party = newParty, heroSkins = newHeroSkins)
            _saveData.value = updated
            _party.value = newParty
            saveManager.saveGame(updated)
        }
    }

    fun equipSkin(heroId: Int, skinId: String): Boolean {
        val skin = DataLoader.getSkin(skinId) ?: return false
        if (skin.heroId != heroId) return false
        val data = _saveData.value
        if (skinId !in data.unlockedSkinIds) return false

        val newParty = _party.value.map {
            if (it.heroId == heroId) it.copy(skinId = skinId) else it
        }
        val newHeroSkins = data.heroSkins + (heroId to skinId)

        _party.value = newParty
        _saveData.value = data.copy(party = newParty, heroSkins = newHeroSkins)
        saveManager.saveGame(_saveData.value)
        return true
    }

    fun unlockSkin(skinId: String): Boolean {
        val skin = DataLoader.getSkin(skinId) ?: return false
        val data = _saveData.value
        if (skinId in data.unlockedSkinIds) return false

        val canUnlock = when (skin.unlockMethod) {
            SkinUnlockMethod.DEFAULT -> true
            SkinUnlockMethod.PURCHASE -> data.gold >= skin.unlockCost
            SkinUnlockMethod.YOGA_LEVEL -> data.yogaLevel >= skin.unlockYogaLevel
            SkinUnlockMethod.ACHIEVEMENT -> skin.unlockAchievementId?.let { it in data.earnedTrophyIds } ?: false
        }
        if (!canUnlock) return false

        val cost = if (skin.unlockMethod == SkinUnlockMethod.PURCHASE) skin.unlockCost else 0
        val newData = data.copy(
            gold = data.gold - cost,
            unlockedSkinIds = data.unlockedSkinIds + skinId
        )
        _saveData.value = newData
        saveManager.saveGame(newData)
        return true
    }

    fun getEquippedSkin(heroId: Int): HeroSkin? {
        val pm = _party.value.find { it.heroId == heroId }
        val skinId = pm?.skinId ?: _saveData.value.heroSkins[heroId]
        return skinId?.let { DataLoader.getSkin(it) }
    }

    fun getUnlockedSkinsForHero(heroId: Int): List<HeroSkin> {
        val unlocked = _saveData.value.unlockedSkinIds
        return DataLoader.getSkinsForHero(heroId).filter { it.skinId in unlocked }
    }
}