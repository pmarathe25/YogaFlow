package com.example.game.battle

import com.example.game.model.*

data class TurnResult(
    val newState: BattleState,
    val logMessages: List<String> = emptyList(),
    val events: List<BattleEvent> = emptyList(),
    val victory: Boolean = false,
    val defeat: Boolean = false
)

data class AdvanceTurnResult(
    val newState: BattleState,
    val logMessages: List<String> = emptyList(),
    val events: List<BattleEvent> = emptyList(),
    val victory: Boolean = false,
    val defeat: Boolean = false
)

class TurnManager(rng: RandomProvider = DefaultRandomProvider) {
    private val reducer = BattleReducer(rng)

    fun startBattle(
        heroes: List<CombatantState>,
        monsters: List<CombatantState>
    ): BattleState = reducer.startBattle(heroes, monsters)

    fun advanceTurn(state: BattleState): AdvanceTurnResult =
        reducer.reduce(state, BattleCommand.AdvanceTurn).toAdvanceResult()

    fun executeSkill(
        state: BattleState,
        heroId: String,
        skill: Skill,
        targets: List<String>
    ): TurnResult = reducer.reduce(state, BattleCommand.UseSkill(heroId, skill, targets))

    fun executeUltimate(
        state: BattleState,
        heroId: String
    ): TurnResult = reducer.reduce(state, BattleCommand.UseUltimate(heroId))

    fun executeCombo(
        state: BattleState,
        combo: ComboSkill,
        participantIds: Set<String>
    ): TurnResult = reducer.reduce(state, BattleCommand.UseCombo(combo, participantIds))

    fun defend(
        state: BattleState,
        heroId: String
    ): TurnResult = reducer.reduce(state, BattleCommand.Defend(heroId))

    fun executeMonsterTurn(
        state: BattleState,
        monsterId: String
    ): TurnResult = reducer.reduce(state, BattleCommand.MonsterAct(monsterId))

    fun resolveTargets(skill: Skill, casterId: String, state: BattleState): List<String> =
        reducer.resolveTargets(skill, casterId, state)
}

private fun TurnResult.toAdvanceResult(): AdvanceTurnResult =
    AdvanceTurnResult(newState, logMessages, events, victory, defeat)
