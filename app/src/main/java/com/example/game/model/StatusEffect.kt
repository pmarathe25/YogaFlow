package com.example.game.model

enum class StatusEffectType {
    BURN,
    SHIELD,
    ATK_UP,
    ATK_DOWN,
    SPD_UP,
    SPD_DOWN,
    DEF_DOWN,
    DAMAGE_REDUCTION,
    TAUNT,
    STUN,
    CONFUSE
}

data class AppliedStatusEffect(
    val type: StatusEffectType,
    val remainingTurns: Int,
    val value: Float = 0f
)

data class StatusEffectInfliction(
    val type: StatusEffectType,
    val chance: Float = 1f,
    val duration: Int = 3,
    val value: Float = 0f
)
