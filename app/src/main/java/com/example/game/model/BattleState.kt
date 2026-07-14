package com.example.game.model

import com.example.game.model.TargetType
import com.example.game.model.Element
import com.example.game.model.ActionSpeed

enum class BattlePhase {
    INTRO, START_OF_BATTLE,
    PLAYER_TURN,
    ENEMY_TURN,
    VICTORY,
    DEFEAT
}

enum class TurnAction {
    SKILL, ULTIMATE, DEFEND, COMBO
}

enum class CombatSide {
    HERO, MONSTER
}

data class CombatantState(
    val id: String,
    val side: CombatSide,
    val name: String,
    val element: Element,
    val maxHp: Int,
    val hp: Int,
    val attack: Int,
    val speed: Int,
    val shield: Int = 0,
    val gauge: Int = 0,
    val isDefeated: Boolean = false,
    val level: Int = 1,
    val skills: List<Skill> = emptyList(),
    val ultimate: Skill? = null,
    val englishName: String? = null,
    val specialAttack: Skill? = null,
    val isBoss: Boolean = false,
    val phases: List<MonsterPhase> = emptyList(),
    val activePhase: Int = -1,
    val aiBehavior: AIBehavior? = null,
    val extraActionsThisRound: Int = 0,
    val turnsSinceLastSpecial: Int = 0
) {
    val isAlive: Boolean get() = !isDefeated
    val isDead: Boolean get() = isDefeated
    val hpPercent: Float get() = if (maxHp > 0) hp.toFloat() / maxHp else 0f
}

data class ActionOutcome(
    val action: TurnAction,
    val actorId: String,
    val skillUsed: Skill? = null,
    val targets: List<String> = emptyList(),
    val damageDealt: Int = 0,
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
    val shieldDamage: Int = 0,
    val statuses: List<String> = emptyList(),
    val cleansed: Boolean = false
)

data class DamageBreakdown(
    val type: DamageType,
    val element: Element?,
    val amount: Int
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
        val outcome: ActionOutcome,
        val element: Element = Element.NEUTRAL
    ) : BattleEvent()
    data class PhaseTriggered(
        val monsterId: String,
        val phaseIndex: Int,
        val trigger: PhaseTrigger
    ) : BattleEvent()
    data class HeroDown(val heroId: String) : BattleEvent()
    data class MonsterDown(val monsterId: String, val element: Element = Element.NEUTRAL) : BattleEvent()
    data class Victory(val turnsTaken: Int) : BattleEvent()
    data class Defeat(val round: Int) : BattleEvent()
}

data class BattleState(
    val heroes: List<CombatantState>,
    val monsters: List<CombatantState>,
    val turnOrder: List<BattleActor> = emptyList(),
    val currentTurnIndex: Int = 0,
    val currentActorId: String = "",
    val phase: BattlePhase = BattlePhase.START_OF_BATTLE,
    val round: Int = 1,
    val turnsTaken: Int = 0,
    val statusEffects: Map<String, List<BattleStatus>> = emptyMap(),
    val selectedTargets: List<String> = emptyList(),
    val eventLog: List<BattleEvent> = emptyList(),
    val isComboAvailable: Boolean = false,
    val pendingSkill: Skill? = null,
    val showTargetSelection: Boolean = false,
    val skillCooldowns: Map<String, Map<String, Int>> = emptyMap()
) {
    val aliveHeroes: List<CombatantState> get() = heroes.filter { !it.isDefeated }
    val aliveMonsters: List<CombatantState> get() = monsters.filter { !it.isDefeated }
    val isBattleOver: Boolean get() = phase == BattlePhase.VICTORY || phase == BattlePhase.DEFEAT

    fun getActor(id: String): BattleActor? = turnOrder.find { it.id == id }

    fun getStatusesForTarget(id: String): List<BattleStatus> {
        return statusEffects[id] ?: emptyList()
    }

    fun hasStatus(targetId: String, type: StatusEffectType): Boolean {
        return statusEffects[targetId]?.any { it.statusType == type && it.remainingTurns > 0 } == true
    }

    fun advanceRound(): BattleState {
        val newStatusEffects = statusEffects.mapValues { (_, statuses) ->
            statuses
                .filter { it.remainingTurns > 1 }
                .map { it.copy(remainingTurns = it.remainingTurns - 1) }
        }.filterValues { it.isNotEmpty() }
        return copy(round = round + 1, statusEffects = newStatusEffects)
    }

    fun withUpdatedHero(heroId: String, update: (CombatantState) -> CombatantState): BattleState {
        return copy(heroes = heroes.map { if (it.id == heroId) update(it) else it })
    }

    fun withUpdatedMonster(monsterId: String, update: (CombatantState) -> CombatantState): BattleState {
        return copy(monsters = monsters.map { if (it.id == monsterId) update(it) else it })
    }
}

data class BattleActor(
    val id: String,
    val name: String,
    val speed: Int,
    val isHero: Boolean,
    val element: Element = Element.NEUTRAL
)
