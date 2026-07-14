package com.example.game.viewmodel

import com.example.game.battle.TurnManager
import com.example.game.model.*
import com.example.game.model.BattlePhase.*
import com.example.game.model.TargetType.*
import com.example.game.persistence.DataLoader
import com.example.game.persistence.GameSaveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class BattleOrchestrator(
    private val turnManager: TurnManager,
    private val saveManager: GameSaveManager,
    private val _battleState: MutableStateFlow<BattleState?>,
    private val _currentMonster: MutableStateFlow<Monster?>,
    private val _currentScreen: MutableStateFlow<GameScreen>,
    private val _battleLog: MutableStateFlow<List<String>>,
    private val _isProcessingTurn: MutableStateFlow<Boolean>,
    private val _goldEarned: MutableStateFlow<Int>,
    private val _party: MutableStateFlow<List<PartyMemberData>>,
    private val _saveData: MutableStateFlow<GameProgress>,
    private val _error: MutableStateFlow<String?>,
    private val viewModelScope: CoroutineScope
) {
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
            val equipped = pm.equippedItemIds.mapNotNull { DataLoader.getEquipment(it) }
            heroDef.toCombatantState(pm, equipped)
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

    fun skipTurn(heroId: String) {
        val state = _battleState.value ?: return
        if (state.phase != PLAYER_TURN || _isProcessingTurn.value) return
        val hero = state.heroes.find { it.id == heroId && !it.isDefeated } ?: return

        viewModelScope.launch {
            _isProcessingTurn.value = true
            _battleState.value = state.copy(pendingSkill = null)

            val result = turnManager.defend(_battleState.value ?: return@launch, heroId)
            _battleState.value = updateComboAvailability(result.newState)
            result.logMessages.forEach { addBattleLog(it) }

            delay(400)
            advanceToNextTurn()
            _isProcessingTurn.value = false
        }
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
            val currentState = _battleState.value ?: run { _isProcessingTurn.value = false; return@launch }
            _battleState.value = currentState.copy(pendingSkill = null)
            val heroStillAlive = currentState.heroes.find { it.id == heroId && !it.isDefeated } ?: run {
                _isProcessingTurn.value = false; return@launch
            }
            val result = turnManager.executeSkill(currentState, heroId, skill, targets)
            _battleState.value = updateComboAvailability(result.newState)
            result.logMessages.forEach { addBattleLog(it) }

            delay(1300)
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

            delay(1800)
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

            delay(2300)
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
            delay(2500)
            _currentScreen.value = GameScreen.BATTLE_RESULT
            onBattleWon()
            return
        }
        if (result.defeat) {
            delay(2500)
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

        val goldReward = when (monster.difficultyTier) {
            DifficultyTier.EASY -> 10
            DifficultyTier.MEDIUM -> 25
            DifficultyTier.HARD -> 50
            DifficultyTier.BOSS -> 100
            DifficultyTier.SUPERBOSS -> 200
        }
        _goldEarned.value = goldReward

        val defeatedIds = data.defeatedMonsterIds + monster.id.lowercase()
        val finalState = _battleState.value
        val battleLog = _battleLog.value
        val comboUses = battleLog.count { it.contains("Party links") }
        val usedUltimateOrCombo = battleLog.any { it.contains("unleashes") || it.contains("Party links") }
        val noHeroDied = finalState?.heroes?.all { !it.isDefeated } ?: true
        val heroesAboveHalf = finalState?.heroes?.all { it.hp >= it.maxHp * 0.5 } ?: false
        val killedBeforeBerserk = finalState?.monsters?.firstOrNull { it.id == monster.id }?.activePhase == -1
        val round = finalState?.round ?: 1
        val allBossesDefeated = DataLoader.monsters.filter { it.isBoss }.all { it.id.lowercase() in defeatedIds }

        val existing = data.earnedTrophyIds.toMutableSet()
        val allBadges = DataLoader.trophies.filter { it.category == TrophyCategory.BADGE }
        val allTrophies = DataLoader.trophies

        allBadges.forEach { t ->
            if (t.id !in existing && t.monsterId != null && t.monsterId.lowercase() in defeatedIds) {
                existing.add(t.id)
            }
        }

        DataLoader.trophies.filter { it.category == TrophyCategory.TROPHY }.forEach { t ->
            if (t.id in existing) return@forEach
            val met = when (t.condition) {
                TrophyCondition.DEFEAT_MONSTER -> t.monsterId != null && t.monsterId.lowercase() in defeatedIds
                TrophyCondition.NO_HERO_BELOW_50 -> heroesAboveHalf
                TrophyCondition.WITHIN_ROUNDS -> round <= 5
                TrophyCondition.KILL_BEFORE_BERSERK -> killedBeforeBerserk
                TrophyCondition.NO_DEATHS -> noHeroDied
                TrophyCondition.FIRST_ROUND -> round == 1
                TrophyCondition.ALL_COMBOS -> comboUses > 0
                TrophyCondition.NO_ULTIMATE_OR_COMBO -> !usedUltimateOrCombo
                TrophyCondition.ALL_BOSSES_SAME_PARTY -> allBossesDefeated
                TrophyCondition.THREE_HERO_COMBOS_SINGLE_BATTLE -> comboUses >= 3
                else -> false
            }
            if (met) existing.add(t.id)
        }

        DataLoader.trophies.filter { it.category == TrophyCategory.TROPHY }.forEach { t ->
            if (t.id in existing) return@forEach
            val met = when (t.condition) {
                TrophyCondition.COLLECT_ALL_BADGES -> allBadges.all { it.id in existing }
                TrophyCondition.COLLECT_ALL -> allTrophies.all { it.id == t.id || it.id in existing }
                else -> false
            }
            if (met) existing.add(t.id)
        }

        _saveData.value = data.copy(
            totalBattlesWon = data.totalBattlesWon + 1,
            defeatedMonsterIds = defeatedIds,
            gold = data.gold + goldReward,
            inventory = inventory,
            earnedTrophyIds = existing.toSet(),
            lastPlayedTimestamp = System.currentTimeMillis()
        )
        saveManager.saveGame(_saveData.value)
    }

    private fun updateComboAvailability(state: BattleState): BattleState {
        val aliveHeroIds = state.aliveHeroes.map { it.id }.toSet()
        val isAvailable = DataLoader.combos.any { combo ->
            aliveHeroIds.containsAll(combo.requiredHeroes.map(Int::toString))
        }
        return state.copy(isComboAvailable = isAvailable)
    }

    private fun addBattleLog(message: String) {
        _battleLog.value = _battleLog.value + message
    }
}