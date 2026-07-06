package com.example.game.model

data class Monster(
    val id: String,
    val name: String,
    val englishName: String,
    val element: Element,
    val baseHp: Int,
    val baseAtk: Int,
    val baseSpd: Int,
    val specialAttack: Skill,
    val mechanicDescription: String,
    val flavorText: String = "",
    val aiBehavior: AIBehavior,
    val difficultyTier: DifficultyTier,
    val phases: List<MonsterPhase> = listOf(MonsterPhase(1f, emptyList())),
    val isBoss: Boolean = false,
    val firstDefeatItemReward: String? = null
)

enum class DifficultyTier {
    EASY, MEDIUM, HARD, BOSS, SUPERBOSS
}

data class MonsterPhase(
    val hpThreshold: Float,
    val triggers: List<PhaseTrigger>
)

data class PhaseTrigger(
    val type: PhaseTriggerType,
    val value: Float = 0f,
    val summonMonsterId: String? = null
)

enum class PhaseTriggerType {
    REFLECT_DAMAGE,
    SUMMON_ADD,
    GAIN_SHIELD,
    DOUBLE_ACTIONS,
    BECOME_UNTARGETABLE,
    NULLIFY_ELEMENT,
    EXTRA_ACTION
}

data class AIBehavior(
    val specialChance: Float = 0.3f,
    val tauntPreference: Boolean = true,
    val targetStrategy: TargetStrategy = TargetStrategy.RANDOM
)

enum class TargetStrategy {
    RANDOM, LOWEST_HP, HIGHEST_HP, MOST_BUFFS, RANDOM_HERO
}

fun Monster.toCombatantState(): CombatantState = CombatantState(
    id = id,
    side = CombatSide.MONSTER,
    name = name,
    element = element,
    maxHp = baseHp,
    hp = baseHp,
    attack = baseAtk,
    speed = baseSpd,
    level = 1,
    englishName = englishName,
    specialAttack = specialAttack,
    aiBehavior = aiBehavior,
    phases = phases,
    isBoss = isBoss,
    activePhase = -1
)
