package com.example.game.battle

import com.example.game.model.*

internal class TargetResolver {
    fun resolveTargets(
        skill: Skill,
        casterId: String,
        state: BattleState
    ): List<String> {
        val aliveHeroes = state.aliveHeroes
        val aliveMonsters = state.aliveMonsters.filter { !state.isMonsterUntargetable(it.id) }
        return when (skill.targetType) {
            TargetType.SELF -> listOf(casterId)
            TargetType.SINGLE_ALLY -> {
                val targets = aliveHeroes.filter { it.id != casterId }
                if (targets.isEmpty()) listOf(casterId) else listOf(targets.first().id)
            }
            TargetType.SINGLE_ENEMY -> listOf(aliveMonsters.firstOrNull()?.id ?: return emptyList())
            TargetType.ALL_ALLIES -> aliveHeroes.map { it.id }
            TargetType.ALL_ENEMIES -> aliveMonsters.map { it.id }
            TargetType.ALL -> aliveHeroes.map { it.id } + aliveMonsters.map { it.id }
        }
    }
}