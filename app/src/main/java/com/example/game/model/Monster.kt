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
    val aiBehavior: AIBehavior,
    val difficultyTier: DifficultyTier,
    val phases: List<MonsterPhase> = listOf(MonsterPhase(1f, emptyList())),
    val isBoss: Boolean = false
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

data class MonsterInstance(
    val monsterId: String,
    val name: String,
    val englishName: String,
    val element: Element,
    val maxHp: Int,
    var currentHp: Int,
    val atk: Int,
    val spd: Int,
    val specialAttack: Skill,
    val aiBehavior: AIBehavior,
    val phases: List<MonsterPhase>,
    var shield: Int = 0,
    var activePhase: Int = 0,
    var isBoss: Boolean = false,
    var isDead: Boolean = false,
    var turnsSinceLastSpecial: Int = 0,
    var extraActionsThisRound: Int = 0
) {
    val hpPercent: Float get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
}

fun Monster.createInstance(): MonsterInstance {
    return MonsterInstance(
        monsterId = id,
        name = name,
        englishName = englishName,
        element = element,
        maxHp = baseHp,
        currentHp = baseHp,
        atk = baseAtk,
        spd = baseSpd,
        specialAttack = specialAttack,
        aiBehavior = aiBehavior,
        phases = phases,
        isBoss = isBoss
    )
}
