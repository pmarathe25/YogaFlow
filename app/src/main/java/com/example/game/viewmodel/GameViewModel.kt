package com.example.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.YogaDatabase
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
import kotlin.random.Random

enum class GameScreen { HUB, BATTLE, PARTY, EQUIPMENT, TROPHIES, SHOP, SETTINGS, BATTLE_RESULT }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    init {
        DataLoader.init(application)
    }
    private val saveManager = GameSaveManager(application)

    private val _currentScreen = MutableStateFlow(GameScreen.HUB)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()

    private val _battleState = MutableStateFlow<BattleState?>(null)
    val battleState: StateFlow<BattleState?> = _battleState.asStateFlow()

    private fun emitBattleState(state: BattleState) {
        _battleState.value = state.snapshot()
    }

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
            restoreParty(updated) // Ensure party is synced with unlocked heroes
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

    fun addHeroToParty(heroId: String) {
        // Automatically handled by sync
    }

    fun removeHeroFromParty(heroId: String) {
        // Automatically handled by sync
    }

    fun startBattle(monsterId: String) {
        val monster = DataLoader.getMonster(monsterId)
        if (_party.value.isEmpty()) {
            _error.value = "No heroes in party!"
            return
        }
        _currentMonster.value = monster
        val monsterInstance = monster.createInstance()
        val state = BattleState(
            heroes = _party.value.toMutableList(),
            monsters = mutableListOf(monsterInstance)
        )
        calculateTurnOrder(state)
        state.currentActorId = state.turnOrder.firstOrNull()?.id ?: ""
        _battleState.value = state
        _battleLog.value = emptyList()
        _currentScreen.value = GameScreen.BATTLE
        addBattleLog("Battle begins! ${monster.englishName} appears!")

        // Start the first turn
        val firstActor = state.turnOrder.firstOrNull()
        if (firstActor != null) {
            if (firstActor.isHero) {
                state.phase = PLAYER_TURN
                updateComboAvailability(state)
            } else {
                state.phase = ENEMY_TURN
                executeMonsterTurn(state, firstActor)
            }
        }
        emitBattleState(state)
    }

    private fun calculateTurnOrder(state: BattleState) {
        val order = BattleEngine.calculateTurnOrder(state.heroes, state.monsters)
        state.turnOrder.clear()
        state.turnOrder.addAll(order)
        state.currentTurnIndex = 0
    }

    private fun updateComboAvailability(state: BattleState) {
        val aliveHeroIds = state.aliveHeroes.map { it.heroId }.toSet()
        state.isComboAvailable = DataLoader.combos.any { combo ->
            aliveHeroIds.containsAll(combo.requiredHeroes)
        }
    }

    // --- Battle Actions ---

    fun cancelAction() {
        val state = _battleState.value ?: return
        state.pendingSkill = null
        emitBattleState(state)
    }

    fun executeSkill(heroId: String, skill: Skill, customTargets: List<String>? = null) {
        val state = _battleState.value ?: return
        if (state.phase != PLAYER_TURN || _isProcessingTurn.value) return
        val hero = state.heroes.find { it.heroId == heroId && !it.isDead } ?: return

        // Check if we need a target but haven't received one yet
        if (customTargets == null) {
            when (skill.targetType) {
                SINGLE_ENEMY -> {
                    // Auto-target first alive monster
                    val monsterId = state.aliveMonsters.firstOrNull()?.monsterId
                    if (monsterId != null) {
                        return executeSkill(heroId, skill, listOf(monsterId))
                    }
                }
                SINGLE_ALLY, SELF -> {
                    state.pendingSkill = skill
                    emitBattleState(state)
                    return
                }
                else -> {} // Multi-target skills resolve in resolveTargets
            }
        }

        viewModelScope.launch {
            _isProcessingTurn.value = true
            state.pendingSkill = null // Clear pending skill as we are executing

            // Build target list
            val targets = customTargets ?: resolveTargets(state, hero, skill)
            if (targets.isEmpty()) {
                _isProcessingTurn.value = false
                return@launch
            }

            val outcome = computeSkillOutcome(state, hero, skill, targets)
            hero.ultimateGauge = (hero.ultimateGauge + skill.ultimateGain).coerceAtMost(100)
            
            // Set cooldown
            if (skill.cooldown > 0) {
                val cds = state.skillCooldowns.getOrPut(heroId) { mutableMapOf() }
                cds[skill.id] = skill.cooldown
            }

            state.addEvent(BattleEvent.SkillUsed(heroId, skill, targets, listOf(outcome)))
            addBattleLog("${hero.name} uses ${skill.name}!")
            
            if (outcome.healingDone > 0) {
                addBattleLog("${hero.name} heals for ${outcome.healingDone} HP!")
            }
            if (outcome.shieldApplied > 0) {
                addBattleLog("${hero.name} applies ${outcome.shieldApplied} shield!")
            }

            applyOutcome(state, outcome)
            emitBattleState(state)
            
            delay(1000) // Wait for action animation
            advanceTurn(state)
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
            val skill = hero.ultimate
            val targets = resolveTargets(state, hero, skill)
            if (targets.isEmpty()) {
                _isProcessingTurn.value = false
                return@launch
            }

            val outcome = computeSkillOutcome(state, hero, skill, targets)
            hero.ultimateGauge = 0
            state.addEvent(BattleEvent.SkillUsed(heroId, skill, targets, listOf(outcome)))
            addBattleLog("${hero.name} unleashes ${skill.name}!")

            applyOutcome(state, outcome)
            emitBattleState(state)
            
            delay(1500) // Wait for ultimate animation
            advanceTurn(state)
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
            val targets = when (combo.targetType) {
                SELF -> listOf(participants.first().heroId)
                SINGLE_ALLY -> listOf(state.aliveHeroes.firstOrNull()?.heroId ?: run { _isProcessingTurn.value = false; return@launch })
                SINGLE_ENEMY -> listOf(state.aliveMonsters.firstOrNull()?.monsterId ?: run { _isProcessingTurn.value = false; return@launch })
                ALL_ALLIES -> state.aliveHeroes.map { it.heroId }
                ALL_ENEMIES -> state.aliveMonsters.map { it.monsterId }
                ALL -> state.aliveHeroes.map { it.heroId } + state.aliveMonsters.map { it.monsterId }
            }
            if (targets.isEmpty()) {
                _isProcessingTurn.value = false
                return@launch
            }

            val outcome = computeComboOutcome(state, participants, combo, targets)
            participants.forEach { it.ultimateGauge = (it.ultimateGauge + 20).coerceAtMost(100) }
            state.addEvent(BattleEvent.ComboUsed(participantIds, combo, targets, outcome))
            addBattleLog("Party unleashes ${combo.name}!")

            applyOutcome(state, outcome)
            emitBattleState(state)
            
            delay(2000) // Wait for combo animation
            advanceTurn(state, skipCount = participants.size)
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

    private fun advanceTurn(state: BattleState, skipCount: Int = 1) {
        viewModelScope.launch {
            state.turnsTaken++

            val oldIndex = state.currentTurnIndex
            state.currentTurnIndex = (state.currentTurnIndex + skipCount) % state.turnOrder.size
            val wrapped = state.currentTurnIndex < oldIndex ||
                    (state.currentTurnIndex == 0 && (oldIndex + skipCount) >= state.turnOrder.size)

            // Advance round if we wrapped around the turn order
            if (wrapped) {
                state.advanceRound()
                state.monsters.forEach { it.extraActionsThisRound = 0 }
                calculateTurnOrder(state)
            }

            // Check for victory/defeat
            if (state.aliveMonsters.isEmpty()) {
                state.phase = VICTORY
                state.addEvent(BattleEvent.Victory(state.turnsTaken))
                addBattleLog("Victory!")
                emitBattleState(state)
                delay(1000)
                _currentScreen.value = GameScreen.BATTLE_RESULT
                onBattleWon()
                return@launch
            }
            if (state.aliveHeroes.isEmpty()) {
                state.phase = DEFEAT
                state.addEvent(BattleEvent.Defeat(state.round))
                addBattleLog("Defeat...")
                
                // Reset stats even on defeat
                _party.value.forEach { hero ->
                    hero.currentHp = hero.maxHp
                    hero.shield = 0
                    hero.ultimateGauge = 0
                    hero.isDead = false
                }

                emitBattleState(state)
                delay(1000)
                _currentScreen.value = GameScreen.BATTLE_RESULT
                return@launch
            }

            val nextActor = state.turnOrder[state.currentTurnIndex]
            state.currentActorId = nextActor.id
            updateComboAvailability(state)

            // Decrement cooldowns for the actor whose turn is starting
            val actorCds = state.skillCooldowns[nextActor.id]
            if (actorCds != null) {
                val updatedCds = actorCds.toMutableMap()
                updatedCds.forEach { (skillId, turns) ->
                    if (turns > 0) updatedCds[skillId] = turns - 1
                }
                state.skillCooldowns[nextActor.id] = updatedCds
            }

            emitBattleState(state)
            delay(1200) // Delay for Turn Banner to show

            if (nextActor.isHero) {
                state.phase = PLAYER_TURN
            } else {
                state.phase = ENEMY_TURN
                executeMonsterTurn(state, nextActor)
            }
            emitBattleState(state)
        }
    }

    private fun executeMonsterTurn(state: BattleState, actor: BattleActor) {
        viewModelScope.launch {
            val monster = state.monsters.find { it.monsterId == actor.id && !it.isDead } ?: return@launch

            val preEvents = BattleEngine.checkPhaseTriggers(state)
            preEvents.filterIsInstance<BattleEvent.PhaseTriggered>().forEach { event ->
                addBattleLog("${monster.name} triggers: ${event.trigger.type.name}!")
            }

            val useSpecial = Random.nextFloat() < monster.aiBehavior.specialChance ||
                    monster.turnsSinceLastSpecial >= 3
            val skill = if (useSpecial) monster.specialAttack
            else Skill("monster_attack", "Attack", "Basic attack.", SINGLE_ENEMY,
                damageComponents = listOf(DamageComponent(PHYSICAL)), baseDamage = monster.atk)

            if (useSpecial) monster.turnsSinceLastSpecial = 0
            else monster.turnsSinceLastSpecial++

            val target = chooseMonsterTarget(state, monster)
            val targets = if (skill.targetType == ALL_ENEMIES || skill.targetType == ALL)
                state.aliveHeroes.map { it.heroId }
            else listOf(target?.heroId ?: state.aliveHeroes.firstOrNull()?.heroId ?: run { advanceTurn(state); return@launch })

            val outcome = computeMonsterOutcome(state, monster, skill, targets)
            state.addEvent(BattleEvent.MonsterTurn(monster.monsterId, skill, targets, outcome))
            addBattleLog("${monster.name} uses ${skill.name}!")
            applyOutcome(state, outcome)
            emitBattleState(state)

            delay(1000) // Wait for monster action animation

            // Check extra actions (only within same "slot")
            if (monster.extraActionsThisRound > 0) {
                monster.extraActionsThisRound--
                executeMonsterTurn(state, actor)
                return@launch
            }

            val postEvents = BattleEngine.checkPhaseTriggers(state)
            postEvents.filterIsInstance<BattleEvent.PhaseTriggered>().forEach { event ->
                addBattleLog("${monster.name} triggers: ${event.trigger.type.name}!")
            }

            // Move to next turn; advanceTurn handles round advancement
            advanceTurn(state)
        }
    }

    private fun chooseMonsterTarget(state: BattleState, monster: MonsterInstance): HeroInstance? {
        val heroId = BattleEngine.chooseMonsterTarget(state.aliveHeroes, monster.aiBehavior.targetStrategy, state)
        return state.heroes.find { it.heroId == heroId }
    }

    private fun resolveTargets(state: BattleState, hero: HeroInstance, skill: Skill): List<String> {
        return BattleEngine.resolveTargets(skill, hero.heroId, state)
    }

    private fun computeSkillOutcome(
        state: BattleState, hero: HeroInstance, skill: Skill, targets: List<String>
    ): ActionOutcome {
        return BattleEngine.computeSkillOutcome(hero, skill, state, targets).outcome
    }

    private fun computeComboOutcome(
        state: BattleState, participants: List<HeroInstance>, combo: ComboSkill, targets: List<String>
    ): ActionOutcome {
        val result = BattleEngine.computeComboOutcome(combo, participants.first().heroId,
            participants.drop(1).map { it.heroId }, state)
        val outcome = result.outcome

        combo.buffs.forEach { buff ->
            val targetsToBuff = if (buff.targetsParty) state.aliveHeroes.map { it.heroId }
            else targets
            targetsToBuff.forEach { id ->
                state.statusEffects.getOrPut(id) { mutableListOf() }
                    .add(BattleStatus(id, buff.type, buff.duration, buff.value))
            }
        }

        if (combo.cleanse) {
            targets.forEach { id -> state.statusEffects.remove(id) }
        }

        if (combo.revive) {
            state.heroes.filter { it.isDead }.forEach { f ->
                f.isDead = false
                val healPct = combo.healScaling?.let {
                    if (it.isPercentage) it.baseHeal else 50
                } ?: 50
                f.currentHp = (f.maxHp * healPct / 100).coerceAtLeast(1)
            }
        }

        return outcome
    }

    private fun computeMonsterOutcome(
        state: BattleState, monster: MonsterInstance, skill: Skill, targets: List<String>
    ): ActionOutcome {
        return BattleEngine.computeMonsterOutcome(state, monster, skill, targets).outcome
    }

    private fun applyOutcome(state: BattleState, outcome: ActionOutcome) {
        val eventsBefore = state.eventLog.size

        outcome.perTargetResult.forEach { (targetId, result) ->
            if (result.damage <= 0) return@forEach
            val hero = state.heroes.find { it.heroId == targetId }
            val monster = state.monsters.find { it.monsterId == targetId }
            val targetName = hero?.name ?: monster?.name ?: return@forEach
            val targetShield = hero?.shield ?: monster?.shield ?: return@forEach
            if (targetShield >= result.damage) {
                addBattleLog("$targetName's shield absorbs ${result.damage} damage!")
            } else {
                val remaining = result.damage - targetShield
                if (targetShield > 0) {
                    addBattleLog("$targetName's shield absorbs $targetShield damage!")
                }
                addBattleLog("$targetName takes $remaining damage!")
            }
        }

        BattleEngine.applyOutcome(state, outcome)

        val newEvents = state.eventLog.drop(eventsBefore)
        newEvents.forEach { event ->
            when (event) {
                is BattleEvent.HeroDown -> {
                    val h = state.heroes.find { it.heroId == event.heroId }
                    addBattleLog("${h?.name ?: event.heroId} has fallen!")
                }
                is BattleEvent.MonsterDown -> {
                    val m = state.monsters.find { it.monsterId == event.monsterId }
                    addBattleLog("${m?.name ?: event.monsterId} is defeated!")
                }
                else -> {}
            }
        }

        outcome.perTargetResult.forEach { (targetId, result) ->
            val hero = state.heroes.find { it.heroId == targetId }
            val monster = state.monsters.find { it.monsterId == targetId }

            if (result.heal > 0 && hero != null) {
                addBattleLog("${hero.name} heals for ${result.heal} HP!")
            }

            if (result.shield > 0 && hero != null) {
                addBattleLog("${hero.name} gains ${result.shield} shield!")
            }

            result.statuses.forEach { sName ->
                val se = outcome.skillUsed?.statusEffects?.find { it.type.name == sName }
                if (se != null) {
                    addBattleLog("${hero?.name ?: monster?.name} is inflicted with ${se.type.name}!")
                }
            }

            if (result.cleansed) {
                addBattleLog("${hero?.name ?: monster?.name} is cleansed of negative effects!")
            }
        }

        outcome.skillUsed?.buffs?.forEach { buff ->
            val targetsToBuff = if (buff.targetsParty) state.aliveHeroes.map { it.heroId }
            else outcome.targets
            targetsToBuff.forEach { id ->
                val h = state.heroes.find { it.heroId == id }
                val m = state.monsters.find { it.monsterId == id }
                val tName = h?.name ?: m?.name ?: "Unknown"
                if (!buff.targetsParty || id == targetsToBuff.first()) {
                    addBattleLog("${if (buff.targetsParty) "Party" else tName} receives ${buff.type.name}!")
                }
            }
        }

        if (outcome.skillUsed?.revive == true) {
            state.heroes.filter { !it.isDead }.forEach { h ->
                addBattleLog("${h.name} is revived!")
            }
        }
    }

    private fun onBattleWon() {
        val monster = _currentMonster.value ?: return
        val data = _saveData.value
        val karmaReward = 50 + (monster.difficultyTier.ordinal * 25)
        
        // Reset hero stats after battle
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

        // Remove existing item of same slot
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
            // Recalculate stats based on new level
            val def = DataLoader.getHero(heroId)
            val newInstance = def.createInstance(nextLevel, hero.equippedItems)
            
            // Update the hero in the party list
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
        // Spark cost = 10 * yogaLevel requirement
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
