package com.example.model

object XpCalculator {
    fun calculateSessionXp(durationMinutes: Int, flowId: String): Int {
        val base = 150
        val difficultyBonus = when (flowId) {
            "restorative_yin", "morning_energizer", "bedtime_wind_down" -> 30
            "sun_salutation", "heart_opening" -> 50
            "warrior_flow", "core_balance" -> 100
            "power_vinyasa", "ashtanga_core", "balance_mastery" -> 150
            else -> 30
        }
        return base + durationMinutes * 10 + difficultyBonus
    }
}
