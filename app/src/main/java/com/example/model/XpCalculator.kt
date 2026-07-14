package com.example.model

object XpCalculator {
    fun calculateSessionXp(durationMinutes: Int, flowId: String): Int {
        val base = 150
        val difficultyBonus = when (flowId) {
            "sun_salutation" -> 50
            "warrior_flow" -> 100
            else -> 30
        }
        return base + durationMinutes * 10 + difficultyBonus
    }
}
