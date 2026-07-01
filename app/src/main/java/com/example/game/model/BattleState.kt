package com.example.game.model

import com.example.game.model.TargetType
import com.example.game.model.Element
import com.example.game.model.ActionSpeed

enum class BattlePhase {
    START_OF_BATTLE,
    PLAYER_TURN,
    ENEMY_TURN,
    VICTORY,
    DEFEAT,
    PHASE_TRIGGER
}

enum class TurnAction {
    SKILL, ULTIMATE, DEFEND, ITEM, COMBO
}

data class ActionOutcome(
    val action: TurnAction,
    val actorId: String,
    val skillUsed: Skill? = null,
    val targets: List<String> = emptyList(),
    val damageDealt: Int = 0, // Total damage for summary/logs
    val healingDone: Int = 0,
    val shieldApplied: Int = 0,
    val statusApplied: List<String> = emptyList(),
    val statusCleansed: List<String> = emptyList(),
    val buffsApplied: List<String> = emptyList(),
    val revivals: List<String> = emptyList(),
    val wasCrit: Boolean = false,
    val wasDodged: Boolean = false,
    val damageTypeBreakdown: List<DamageBreakdown> = emptyList(),
    val comboTriggered: ComboSkill? = null,
    val perTargetResult: Map<String, TargetResult> = emptyMap()
)

data class TargetResult(
    val damage: Int = 0,
    val heal: Int = 0,
    val shield: Int = 0,
    val statuses: List<String> = emptyList(),
    val cleansed: Boolean = false
)

data class DamageBreakdown(
    val type: DamageType,
    val element: Element?,
    val amount: Int,
    val isPercentile: Boolean = false,
    val percentileAmount: Float = 0f
)

data class BattleStatus(
    val heroId: String,
    val statusType: StatusEffectType,
    val remainingTurns: Int,
    val value: Float = 0f
)

sealed class BattleEvent {
    data class SkillUsed(
        val heroId: String,
        val skill: Skill,
        val targets: List<String>,
        val outcomes: List<ActionOutcome>
    ) : BattleEvent()
    data class ComboUsed(
        val participants: Set<String>,
        val combo: ComboSkill,
        val targets: List<String>,
        val outcome: ActionOutcome
    ) : BattleEvent()
    data class MonsterTurn(
        val monsterId: String,
        val skill: Skill,
        val targets: List<String>,
        val outcome: ActionOutcome
    ) : BattleEvent()
    data class PhaseTriggered(
        val monsterId: String,
        val phaseIndex: Int,
        val trigger: PhaseTrigger
    ) : BattleEvent()
    data class HeroDown(val heroId: String) : BattleEvent()
    data class MonsterDown(val monsterId: String) : BattleEvent()
    data class Victory(val turnsTaken: Int) : BattleEvent()
    data class Defeat(val round: Int) : BattleEvent()
}

class BattleState(
    val heroes: MutableList<HeroInstance>,
    val monsters: MutableList<MonsterInstance>,
    val turnOrder: MutableList<BattleActor> = mutableListOf(),
    var currentTurnIndex: Int = 0,
    var currentActorId: String = "",
    var phase: BattlePhase = BattlePhase.START_OF_BATTLE,
    var round: Int = 1,
    var turnsTaken: Int = 0,
    val statusEffects: MutableMap<String, MutableList<BattleStatus>> = mutableMapOf(),
    var selectedTargets: List<String> = emptyList(),
    private val _eventLog: MutableList<BattleEvent> = mutableListOf(),
    var isComboAvailable: Boolean = false,
    var pendingSkill: Skill? = null,
    var showTargetSelection: Boolean = false,
    val skillCooldowns: MutableMap<String, MutableMap<String, Int>> = mutableMapOf() // heroId -> (skillId -> remainingTurns)
) {
    val eventLog: List<BattleEvent> get() = _eventLog.toList()
    val aliveHeroes: List<HeroInstance> get() = heroes.filter { !it.isDead }
    val aliveMonsters: List<MonsterInstance> get() = monsters.filter { !it.isDead }
    val isBattleOver: Boolean get() = phase == BattlePhase.VICTORY || phase == BattlePhase.DEFEAT

    fun addEvent(event: BattleEvent) {
        _eventLog.add(event)
    }

    fun getActor(id: String): BattleActor? = turnOrder.find { it.id == id }

    fun getStatusesForTarget(id: String): List<BattleStatus> {
        return statusEffects[id]?.toList() ?: emptyList()
    }

    fun hasStatus(targetId: String, type: StatusEffectType): Boolean {
        return statusEffects[targetId]?.any { it.statusType == type && it.remainingTurns > 0 } == true
    }

    fun advanceRound() {
        round++
        statusEffects.forEach { (id, statuses) ->
            statuses.removeAll { it.remainingTurns <= 1 }
            statuses.replaceAll { it.copy(remainingTurns = it.remainingTurns - 1) }
        }
    }

    fun snapshot(): BattleState = BattleState(
        heroes = heroes.map { it.copy(equippedItems = it.equippedItems.toMutableList()) }.toMutableList(),
        monsters = monsters.map { it.copy() }.toMutableList(),
        turnOrder = turnOrder.toMutableList(),
        currentTurnIndex = currentTurnIndex,
        currentActorId = currentActorId,
        phase = phase,
        round = round,
        turnsTaken = turnsTaken,
        statusEffects = statusEffects.mapValues { it.value.toMutableList() }.toMutableMap(),
        selectedTargets = selectedTargets.toList(),
        _eventLog = _eventLog.toMutableList(),
        isComboAvailable = isComboAvailable,
        pendingSkill = pendingSkill,
        showTargetSelection = showTargetSelection,
        skillCooldowns = skillCooldowns.mapValues { it.value.toMutableMap() }.toMutableMap()
    )
}

data class BattleActor(
    val id: String,
    val name: String,
    val speed: Int,
    val isHero: Boolean,
    val element: Element = Element.NEUTRAL
)

data class BattleSaveData(
    val heroData: List<HeroSaveData>,
    val monsterData: List<MonsterSaveData>,
    val turnOrderIds: List<String>,
    val currentTurnIndex: Int,
    val round: Int,
    val turnsTaken: Int,
    val statusData: Map<String, List<StatusSaveData>>
)

data class HeroSaveData(
    val heroId: String,
    val level: Int,
    val currentHp: Int,
    val shield: Int,
    val ultimateGauge: Int,
    val isDead: Boolean,
    val equippedItemIds: List<String>
)

data class MonsterSaveData(
    val monsterId: String,
    val currentHp: Int,
    val shield: Int,
    val activePhase: Int,
    val isDead: Boolean
)

data class StatusSaveData(
    val type: StatusEffectType,
    val remainingTurns: Int,
    val value: Float
)


