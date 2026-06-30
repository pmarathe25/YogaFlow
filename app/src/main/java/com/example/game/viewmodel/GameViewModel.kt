package com.example.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.YogaDatabase
import com.example.game.model.*
import com.example.game.model.Element.*
import com.example.game.model.TargetType.*
import com.example.game.model.StatusEffectType.*
import com.example.game.model.DamageType.*
import com.example.game.model.BattlePhase.*
import com.example.game.model.TurnAction.*
import com.example.game.model.PhaseTriggerType.*
import com.example.game.model.DifficultyTier.*
import com.example.game.model.ComboType.*
import com.example.game.persistence.GameSaveManager
import com.example.game.persistence.GameSaveManager.GameSaveData
import com.example.model.LevelDefinitions
import com.example.model.XpCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.random.Random

enum class GameScreen { HUB, BATTLE, PARTY, EQUIPMENT, TROPHIES, SHOP, SETTINGS, BATTLE_RESULT }

class GameViewModel(application: Application) : AndroidViewModel(application) {
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
        if (data.party.isNotEmpty()) {
            _party.value = data.party.map { hs ->
                val hero = HeroDefinitions.getHero(hs.heroId) ?: return@map null
                HeroDefinitions.createInstance(hero, hs.level, hs.equippedItemIds)
                    .also { it.currentHp = hs.currentHp; it.shield = hs.shield; it.isDead = hs.isDead }
            }.filterNotNull()
        }
    }

    fun getUnlockedHeroes(): List<Hero> {
        return HeroDefinitions.allHeroes.filter { it.unlockYogaLevel <= _saveData.value.yogaLevel }
    }

    fun getAvailableHeroes(): List<Hero> {
        return getUnlockedHeroes().filter { h -> _party.value.none { it.heroId == h.id } }
    }

    fun addHeroToParty(heroId: String) {
        val hero = HeroDefinitions.getHero(heroId) ?: return
        if (_party.value.any { it.heroId == heroId }) return
        val instance = HeroDefinitions.createInstance(hero, 1)
        _party.value = _party.value + instance
        _saveData.value = _saveData.value.copy(party = _party.value.map { it.toSaveData() })
        saveGame()
    }

    fun removeHeroFromParty(heroId: String) {
        _party.value = _party.value.filter { it.heroId != heroId }
        _saveData.value = _saveData.value.copy(party = _party.value.map { it.toSaveData() })
        saveGame()
    }

    fun startBattle(monsterId: String) {
        val monster = MonsterDefinitions.getMonster(monsterId) ?: run {
            _error.value = "Unknown monster: $monsterId"
            return
        }
        if (_party.value.isEmpty()) {
            _error.value = "No heroes in party!"
            return
        }
        _currentMonster.value = monster
        val monsterInstance = MonsterDefinitions.createInstance(monster)
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
        val actors = mutableListOf<BattleActor>()
        state.heroes.filter { !it.isDead }.forEach { h ->
            actors.add(BattleActor(h.heroId, h.name, h.spd, true, h.element))
        }
        state.monsters.filter { !it.isDead }.forEach { m ->
            actors.add(BattleActor(m.monsterId, m.name, m.spd, false, m.element))
        }
        state.turnOrder.clear()
        state.turnOrder.addAll(actors.sortedByDescending { it.speed })
        state.currentTurnIndex = 0
    }

    private fun updateComboAvailability(state: BattleState) {
        val aliveHeroIds = state.aliveHeroes.map { it.heroId }.toSet()
        state.isComboAvailable = ComboSkillDefinitions.allCombos.any { combo ->
            aliveHeroIds.containsAll(combo.requiredHeroes)
        }
    }

    // --- Battle Actions ---

    fun executeSkill(heroId: String, skill: Skill) {
        val state = _battleState.value ?: return
        if (state.phase != PLAYER_TURN) return
        val hero = state.heroes.find { it.heroId == heroId && !it.isDead } ?: return

        // Build target list
        val targets = resolveTargets(state, hero, skill)
        if (targets.isEmpty()) return

        val outcome = computeSkillOutcome(state, hero, skill, targets)
        hero.ultimateGauge = (hero.ultimateGauge + skill.ultimateGain).coerceAtMost(100)
        state.addEvent(BattleEvent.SkillUsed(heroId, skill, targets, listOf(outcome)))
        addBattleLog("${hero.name} uses ${skill.name}!")

        applyOutcome(state, outcome)
        advanceTurn(state)
        emitBattleState(state)
    }

    fun executeUltimate(heroId: String) {
        val state = _battleState.value ?: return
        if (state.phase != PLAYER_TURN) return
        val hero = state.heroes.find { it.heroId == heroId && !it.isDead } ?: return
        if (hero.ultimateGauge < 100) return

        val skill = hero.ultimate
        val targets = resolveTargets(state, hero, skill)
        if (targets.isEmpty()) return

        val outcome = computeSkillOutcome(state, hero, skill, targets)
        hero.ultimateGauge = 0
        state.addEvent(BattleEvent.SkillUsed(heroId, skill, targets, listOf(outcome)))
        addBattleLog("${hero.name} unleashes ${skill.name}!")

        applyOutcome(state, outcome)
        advanceTurn(state)
        emitBattleState(state)
    }

    fun executeCombo(participantIds: Set<String>) {
        val state = _battleState.value ?: return
        if (state.phase != PLAYER_TURN) return
        val combo = ComboSkillDefinitions.findCombo(participantIds) ?: return
        val participants = participantIds.mapNotNull { id -> state.heroes.find { it.heroId == id && !it.isDead } }
        if (participants.size != combo.requiredHeroes.size) return

        val targets = when (combo.targetType) {
            SELF -> listOf(participants.first().heroId)
            SINGLE_ALLY -> listOf(state.aliveHeroes.firstOrNull()?.heroId ?: return)
            SINGLE_ENEMY -> listOf(state.aliveMonsters.firstOrNull()?.monsterId ?: return)
            ALL_ALLIES -> state.aliveHeroes.map { it.heroId }
            ALL_ENEMIES -> state.aliveMonsters.map { it.monsterId }
            ALL -> state.aliveHeroes.map { it.heroId } + state.aliveMonsters.map { it.monsterId }
        }
        if (targets.isEmpty()) return

        val outcome = computeComboOutcome(state, participants, combo, targets)
        participants.forEach { it.ultimateGauge = (it.ultimateGauge + 20).coerceAtMost(100) }
        state.addEvent(BattleEvent.ComboUsed(participantIds, combo, targets, outcome))
        addBattleLog("Party unleashes ${combo.name}!")

        applyOutcome(state, outcome)
        advanceTurn(state, skipCount = participants.size)
        emitBattleState(state)
    }

    private fun advanceTurn(state: BattleState, skipCount: Int = 1) {
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
            _currentScreen.value = GameScreen.BATTLE_RESULT
            onBattleWon()
            return
        }
        if (state.aliveHeroes.isEmpty()) {
            state.phase = DEFEAT
            state.addEvent(BattleEvent.Defeat(state.round))
            addBattleLog("Defeat...")
            emitBattleState(state)
            _currentScreen.value = GameScreen.BATTLE_RESULT
            return
        }

        val nextActor = state.turnOrder[state.currentTurnIndex]
        state.currentActorId = nextActor.id
        updateComboAvailability(state)

        if (nextActor.isHero) {
            state.phase = PLAYER_TURN
        } else {
            state.phase = ENEMY_TURN
            executeMonsterTurn(state, nextActor)
        }
    }

    private fun executeMonsterTurn(state: BattleState, actor: BattleActor) {
        val monster = state.monsters.find { it.monsterId == actor.id && !it.isDead } ?: return

        checkPhaseTriggers(state, monster)

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
        else listOf(target?.heroId ?: state.aliveHeroes.firstOrNull()?.heroId ?: return)

        val outcome = computeMonsterOutcome(state, monster, skill, targets)
        state.addEvent(BattleEvent.MonsterTurn(monster.monsterId, skill, targets, outcome))
        addBattleLog("${monster.name} uses ${skill.name}!")
        applyOutcome(state, outcome)

        // Check extra actions (only within same "slot")
        if (monster.extraActionsThisRound > 0) {
            monster.extraActionsThisRound--
            executeMonsterTurn(state, actor)
            return
        }

        checkPhaseTriggers(state, monster)

        // Move to next turn; advanceTurn handles round advancement
        advanceTurn(state)
    }

    private fun checkPhaseTriggers(state: BattleState, monster: MonsterInstance) {
        val definition = MonsterDefinitions.getMonster(monster.monsterId) ?: return
        for (i in definition.phases.indices) {
            val phase = definition.phases[i]
            if (monster.hpPercent <= phase.hpThreshold && monster.activePhase < i) {
                monster.activePhase = i
                phase.triggers.forEach { trigger ->
                    state.addEvent(BattleEvent.PhaseTriggered(monster.monsterId, i, trigger))
                    addBattleLog("${monster.name} triggers: ${trigger.type.name}!")
                    when (trigger.type) {
                        EXTRA_ACTION -> monster.extraActionsThisRound++
                        DOUBLE_ACTIONS -> monster.extraActionsThisRound = 2
                        GAIN_SHIELD -> monster.shield += (monster.maxHp * trigger.value).toInt()
                        else -> {} // Handled in damage calc
                    }
                }
            }
        }
    }

    private fun chooseMonsterTarget(state: BattleState, monster: MonsterInstance): HeroInstance? {
        val alive = state.aliveHeroes
        if (alive.isEmpty()) return null
        return when (monster.aiBehavior.targetStrategy) {
            TargetStrategy.RANDOM, TargetStrategy.RANDOM_HERO -> alive.random()
            TargetStrategy.LOWEST_HP -> alive.minByOrNull { it.currentHp }
            TargetStrategy.HIGHEST_HP -> alive.maxByOrNull { it.currentHp }
            TargetStrategy.MOST_BUFFS -> {
                alive.maxByOrNull { hero ->
                    state.getStatusesForTarget(hero.heroId).size
                }
            }
        }
    }

    private fun resolveTargets(state: BattleState, hero: HeroInstance, skill: Skill): List<String> {
        val aliveHeroes = state.aliveHeroes
        val aliveMonsters = state.aliveMonsters
        return when (skill.targetType) {
            SELF -> listOf(hero.heroId)
            SINGLE_ALLY -> {
                val targets = aliveHeroes.filter { it.heroId != hero.heroId }
                if (targets.isEmpty()) listOf(hero.heroId) else listOf(targets.first().heroId)
            }
            SINGLE_ENEMY -> listOf(aliveMonsters.firstOrNull()?.monsterId ?: return emptyList())
            ALL_ALLIES -> aliveHeroes.map { it.heroId }
            ALL_ENEMIES -> aliveMonsters.map { it.monsterId }
            ALL -> aliveHeroes.map { it.heroId } + aliveMonsters.map { it.monsterId }
        }
    }

    private fun computeSkillOutcome(
        state: BattleState, hero: HeroInstance, skill: Skill, targets: List<String>
    ): ActionOutcome {
        var totalDamage = 0
        var totalHeal = 0
        var totalShield = 0
        val appliedStatuses = mutableListOf<String>()
        val cleansedStatuses = mutableListOf<String>()
        val appliedBuffs = mutableListOf<String>()
        val revived = mutableListOf<String>()
        val breakdown = mutableListOf<DamageBreakdown>()

        for (targetId in targets) {
            // Damage
            if (skill.damageComponents.isNotEmpty() || skill.baseDamage > 0) {
                for (component in skill.damageComponents) {
                    val dmg = computeDamage(state, hero, skill, component, targetId)
                    if (dmg > 0) {
                        totalDamage += dmg
                        breakdown.add(DamageBreakdown(component.type, component.element, dmg))
                    }
                }
            }

            // Heal
            skill.healScaling?.let { scaling ->
                if (scaling.isPercentage || scaling.baseHeal > 0) {
                    val heal = computeHeal(hero, scaling, targetId)
                    if (heal > 0) totalHeal += heal
                }
            }

            // Shield
            skill.shieldScaling?.let { scaling ->
                val shield = computeShield(hero, scaling)
                if (shield > 0) totalShield += shield
            }

            // Status effects
            skill.statusEffects.forEach { se ->
                if (Random.nextFloat() < se.chance) {
                    val existing = state.statusEffects.getOrPut(targetId) { mutableListOf() }
                    existing.add(BattleStatus(targetId, se.type, se.duration, se.value))
                    appliedStatuses.add(se.type.name)
                }
            }

            // Buffs
            skill.buffs.forEach { buff ->
                val targetsToBuff = if (buff.targetsParty) state.aliveHeroes.map { it.heroId }
                else listOf(targetId)
                targetsToBuff.forEach { id ->
                    val existing = state.statusEffects.getOrPut(id) { mutableListOf() }
                    existing.add(BattleStatus(id, buff.type, buff.duration, buff.value))
                    appliedBuffs.add(buff.type.name)
                }
            }

            // Cleanse
            if (skill.cleanse) {
                state.statusEffects.remove(targetId)
                cleansedStatuses.add(targetId)
            }

            // Revive
            if (skill.revive) {
                val fallen = state.heroes.filter { it.isDead }
                fallen.forEach { f ->
                    f.isDead = false
                    val healPct = skill.healScaling?.let {
                        if (it.isPercentage) it.baseHeal else 50
                    } ?: 50
                    f.currentHp = (f.maxHp * healPct / 100).coerceAtLeast(1)
                    revived.add(f.heroId)
                }
            }
        }

        return ActionOutcome(
            action = SKILL, actorId = hero.heroId, skillUsed = skill, targets = targets,
            damageDealt = totalDamage, healingDone = totalHeal, shieldApplied = totalShield,
            statusApplied = appliedStatuses, statusCleansed = cleansedStatuses,
            buffsApplied = appliedBuffs, revivals = revived, damageTypeBreakdown = breakdown
        )
    }

    private fun computeComboOutcome(
        state: BattleState, participants: List<HeroInstance>, combo: ComboSkill, targets: List<String>
    ): ActionOutcome {
        var totalDamage = 0
        var totalHeal = 0
        var totalShield = 0
        val appliedStatuses = mutableListOf<String>()
        val cleansedStatuses = mutableListOf<String>()
        val appliedBuffs = mutableListOf<String>()
        val revived = mutableListOf<String>()
        val breakdown = mutableListOf<DamageBreakdown>()

        val avgLevel = participants.map { it.level }.average().toInt()

        for (targetId in targets) {
            // Damage
            if (combo.damageComponents.isNotEmpty()) {
                for (component in combo.damageComponents) {
                    if (isComponentNullified(state, component)) continue

                    val dmg = combo.baseDamage + combo.damagePerLevel * avgLevel
                    totalDamage += dmg
                    breakdown.add(DamageBreakdown(component.type, component.element, dmg))
                }
            }

            // Heal
            combo.healScaling?.let { scaling ->
                val avgHero = participants.first()
                val heal = if (scaling.isPercentage) {
                    (scaling.baseHeal * participants.first().maxHp / 100).coerceAtLeast(1)
                } else scaling.baseHeal + scaling.healPerLevel * avgLevel
                if (heal > 0) totalHeal += heal
            }

            // Shield
            combo.shieldScaling?.let { scaling ->
                val shield = if (scaling.isPercentage && scaling.percentage > 0) {
                    (scaling.percentage * participants.first().maxHp).toInt()
                } else scaling.baseShield + scaling.shieldPerLevel * avgLevel
                if (shield > 0) totalShield += shield
            }

            // Buffs
            combo.buffs.forEach { buff ->
                val targetsToBuff = if (buff.targetsParty) state.aliveHeroes.map { it.heroId }
                else listOf(targetId)
                targetsToBuff.forEach { id ->
                    state.statusEffects.getOrPut(id) { mutableListOf() }
                        .add(BattleStatus(id, buff.type, buff.duration, buff.value))
                    appliedBuffs.add(buff.type.name)
                }
            }

            // Cleanse
            if (combo.cleanse) {
                state.statusEffects.remove(targetId)
                cleansedStatuses.add(targetId)
            }

            // Revive
            if (combo.revive) {
                state.heroes.filter { it.isDead }.forEach { f ->
                    f.isDead = false
                    val healPct = combo.healScaling?.let {
                        if (it.isPercentage) it.baseHeal else 50
                    } ?: 50
                    f.currentHp = (f.maxHp * healPct / 100).coerceAtLeast(1)
                    revived.add(f.heroId)
                }
            }
        }

        return ActionOutcome(
            action = COMBO, actorId = participants.first().heroId,
            skillUsed = null, targets = targets,
            damageDealt = totalDamage, healingDone = totalHeal, shieldApplied = totalShield,
            statusApplied = appliedStatuses, statusCleansed = cleansedStatuses,
            buffsApplied = appliedBuffs, revivals = revived,
            damageTypeBreakdown = breakdown, comboTriggered = combo
        )
    }

    private fun computeMonsterOutcome(
        state: BattleState, monster: MonsterInstance, skill: Skill, targets: List<String>
    ): ActionOutcome {
        var totalDamage = 0
        val appliedStatuses = mutableListOf<String>()
        val breakdown = mutableListOf<DamageBreakdown>()

        for (targetId in targets) {
            val hero = state.heroes.find { it.heroId == targetId } ?: continue

            // Damage
            if (skill.damageComponents.isNotEmpty() || skill.baseDamage > 0) {
                for (component in skill.damageComponents) {
                    val dmg = computeMonsterDamage(state, monster, skill, component, hero)
                    val shielded = hero.shield
                    if (shielded >= dmg) {
                        hero.shield -= dmg
                    } else {
                        val remaining = dmg - shielded
                        hero.shield = 0
                        hero.currentHp = (hero.currentHp - remaining).coerceAtLeast(0)
                    }
                    totalDamage += dmg
                    breakdown.add(DamageBreakdown(component.type, component.element, dmg))
                }
            }

            // Status effects
            skill.statusEffects.forEach { se ->
                if (Random.nextFloat() < se.chance) {
                    state.statusEffects.getOrPut(targetId) { mutableListOf() }
                        .add(BattleStatus(targetId, se.type, se.duration, se.value))
                    appliedStatuses.add(se.type.name)
                }
            }

            if (hero.currentHp <= 0) {
                hero.isDead = true
                hero.currentHp = 0
                state.addEvent(BattleEvent.HeroDown(hero.heroId))
            }
        }

        return ActionOutcome(
            action = SKILL, actorId = monster.monsterId, skillUsed = skill, targets = targets,
            damageDealt = totalDamage, damageTypeBreakdown = breakdown,
            statusApplied = appliedStatuses
        )
    }

    private fun isComponentNullified(state: BattleState, component: DamageComponent): Boolean {
        return state.monsters.any { monster ->
            val def = MonsterDefinitions.getMonster(monster.monsterId)
            def?.phases?.getOrNull(monster.activePhase)?.triggers?.any { trigger ->
                trigger.type == NULLIFY_ELEMENT && (
                    (trigger.summonMonsterId == "ALL" && component.type == DamageType.ELEMENTAL) ||
                    component.element?.name == trigger.summonMonsterId
                )
            } == true
        }
    }

    private fun computeDamage(
        state: BattleState, hero: HeroInstance, skill: Skill,
        component: DamageComponent, targetId: String
    ): Int {
        if (isComponentNullified(state, component)) return 0
        val monster = state.monsters.find { it.monsterId == targetId && !it.isDead } ?: return 0
        val base = skill.baseDamage + skill.damagePerLevel * hero.level
        val elementMult = getElementMultiplier(hero.element, component.element ?: monster.element)
        val atkBonus = 1f + hero.atk / 100f
        val buffMult = 1f + computeBuffMultiplier(state, hero.heroId, ATK_UP)
        val dmg = (base * (component.percentage / 100f) * elementMult * atkBonus * buffMult).toInt()
        val shielded = monster.shield
        if (shielded >= dmg) {
            monster.shield -= dmg
            return 0
        }
        val remaining = dmg - shielded
        monster.shield = 0
        monster.currentHp = (monster.currentHp - remaining).coerceAtLeast(0)
        if (monster.currentHp <= 0) {
            monster.isDead = true
            monster.currentHp = 0
            state.addEvent(BattleEvent.MonsterDown(monster.monsterId))
        }
        return dmg
    }

    private fun computeMonsterDamage(
        state: BattleState, monster: MonsterInstance, skill: Skill,
        component: DamageComponent, hero: HeroInstance
    ): Int {
        val base = skill.baseDamage
        val elementMult = getElementMultiplier(monster.element, component.element ?: hero.element)
        val dmgReduction = computeBuffMultiplier(state, hero.heroId, DAMAGE_REDUCTION)
        val dmg = (base * elementMult * (1f - dmgReduction)).toInt().coerceAtLeast(1)
        return dmg
    }

    private fun computeHeal(hero: HeroInstance, scaling: HealScaling, targetId: String): Int {
        return if (scaling.isPercentage) {
            (scaling.baseHeal * hero.maxHp / 100).coerceAtLeast(1)
        } else {
            scaling.baseHeal + scaling.healPerLevel * hero.level
        }
    }

    private fun computeShield(hero: HeroInstance, scaling: ShieldScaling): Int {
        return if (scaling.isPercentage && scaling.percentage > 0) {
            (scaling.percentage * hero.maxHp).toInt()
        } else if (scaling.isPercentage) {
            (scaling.baseShield * hero.maxHp / 100).coerceAtLeast(1)
        } else {
            scaling.baseShield + scaling.shieldPerLevel * hero.level
        }
    }

    private fun computeBuffMultiplier(state: BattleState, targetId: String, type: StatusEffectType): Float {
        return state.statusEffects[targetId]?.filter { it.statusType == type }
            ?.maxOfOrNull { it.value } ?: 0f
    }

    private fun getElementMultiplier(attackerElement: Element, defenderElement: Element): Float {
        // Simple effectiveness chart
        val chart: Map<Element, Map<Element, Float>> = mapOf(
            FIRE to mapOf(AIR to 1.5f, WATER to 0.5f),
            WATER to mapOf(FIRE to 1.5f, EARTH to 0.5f),
            AIR to mapOf(EARTH to 1.5f, FIRE to 0.5f),
            EARTH to mapOf(WATER to 1.5f, AIR to 0.5f),
            LIGHT to mapOf(DARK to 1.5f, SHADOW to 1.5f, VOID to 0.5f),
            DARK to mapOf(LIGHT to 1.5f, VOID to 0.5f),
            SHADOW to mapOf(LIGHT to 1.5f),
            ELECTRIC to mapOf(WATER to 1.5f, EARTH to 0.5f),
            VOID to mapOf(LIGHT to 1.5f, DARK to 1.5f)
        )
        return chart[attackerElement]?.get(defenderElement) ?: 1f
    }

    private fun applyOutcome(state: BattleState, outcome: ActionOutcome) {
        for (targetId in outcome.targets) {
            val hero = state.heroes.find { it.heroId == targetId }
            val monster = state.monsters.find { it.monsterId == targetId }

            // Healing
            if (outcome.healingDone > 0 && hero != null) {
                hero.currentHp = (hero.currentHp + outcome.healingDone).coerceAtMost(hero.maxHp)
            }

            // Shield (only apply to heroes from player actions)
            if (outcome.shieldApplied > 0 && hero != null) {
                hero.shield += outcome.shieldApplied
            }
        }

        // Check deaths
        state.heroes.filter { it.currentHp <= 0 && !it.isDead }.forEach { h ->
            h.isDead = true
            h.currentHp = 0
            state.addEvent(BattleEvent.HeroDown(h.heroId))
        }
        state.monsters.filter { it.currentHp <= 0 && !it.isDead }.forEach { m ->
            m.isDead = true
            m.currentHp = 0
            state.addEvent(BattleEvent.MonsterDown(m.monsterId))
        }
    }

    private fun onBattleWon() {
        val monster = _currentMonster.value ?: return
        val data = _saveData.value
        val karmaReward = 50 + (monster.difficultyTier.ordinal * 25)
        _saveData.value = data.copy(
            totalBattlesWon = data.totalBattlesWon + 1,
            totalKarmaXp = data.totalKarmaXp + karmaReward,
            defeatedMonsterIds = data.defeatedMonsterIds + monster.id,
            lastPlayedTimestamp = System.currentTimeMillis()
        )
        saveGame()
    }

    // --- Equipment ---

    fun purchaseItem(itemId: String): Boolean {
        val item = EquipmentDefinitions.allEquipment.find { it.id == itemId } ?: return false
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
        val item = EquipmentDefinitions.allEquipment.find { it.id == itemId } ?: return false
        if (itemId !in _saveData.value.inventory) return false

        // Remove existing item of same slot
        hero.equippedItems.removeAll { existingId ->
            EquipmentDefinitions.allEquipment.find { it.id == existingId }?.slot == item.slot
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
        return hero.equippedItems.mapNotNull { EquipmentDefinitions.getEquipment(it) }
    }

    // --- Economy ---

    fun getAvailableGold(): Int {
        val data = _saveData.value
        return data.totalKarmaXp - data.totalGoldSpent
    }

    // --- Hero Level Up ---

    fun levelUpHero(heroId: String): Boolean {
        val hero = _party.value.find { it.heroId == heroId } ?: return false
        val data = _saveData.value
        val sparkCost = kotlin.math.ceil(hero.level * 1.5).toInt()
        if (data.sparks < sparkCost) return false

        hero.level++
        _saveData.value = data.copy(
            sparks = data.sparks - sparkCost,
            party = _party.value.map { it.toSaveData() }
        )
        saveGame()
        return true
    }

    // --- Hero Purchase ---

    fun purchaseHero(heroId: String): Boolean {
        val hero = HeroDefinitions.getHero(heroId) ?: return false
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
