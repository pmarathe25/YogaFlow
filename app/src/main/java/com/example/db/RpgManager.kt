package com.example.db

import android.content.Context

object RpgManager {
    private const val PREFS_NAME = "rpg_game_state"

    fun getUnlockedHeroes(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet("unlocked_heroes", setOf("Shanti")) ?: setOf("Shanti")
    }

    fun unlockHero(context: Context, heroId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getUnlockedHeroes(context).toMutableSet()
        current.add(heroId)
        prefs.edit().putStringSet("unlocked_heroes", current).apply()
    }

    fun getHeroLevel(context: Context, heroId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("hero_level_$heroId", 1)
    }

    fun levelUpHero(context: Context, heroId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLvl = getHeroLevel(context, heroId)
        prefs.edit().putInt("hero_level_$heroId", currentLvl + 1).apply()
    }

    fun getActiveParty(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = prefs.getString("active_party", "Shanti") ?: "Shanti"
        return csv.split(",").filter { it.isNotEmpty() }
    }

    fun saveActiveParty(context: Context, party: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("active_party", party.joinToString(",")).apply()
    }

    fun getSpentSparks(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt("spent_sparks", 0)
    }

    fun saveSpentSparks(context: Context, sparks: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt("spent_sparks", sparks).apply()
    }

    fun getSpentKarmaXp(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt("spent_karma_xp", 0)
    }

    fun saveSpentKarmaXp(context: Context, xp: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt("spent_karma_xp", xp).apply()
    }

    fun getExtraKarmaXp(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt("extra_karma_xp", 0)
    }

    fun saveExtraKarmaXp(context: Context, xp: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt("extra_karma_xp", xp).apply()
    }

    fun getExtraSparks(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt("extra_sparks", 0)
    }

    fun saveExtraSparks(context: Context, sparks: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt("extra_sparks", sparks).apply()
    }

    fun getStats(context: Context): Pair<Int, Int> { // Wins, Battles
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Pair(prefs.getInt("wins", 0), prefs.getInt("battles", 0))
    }

    fun recordBattle(context: Context, won: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentWins = prefs.getInt("wins", 0)
        val currentBattles = prefs.getInt("battles", 0)
        val edit = prefs.edit().putInt("battles", currentBattles + 1)
        if (won) {
            edit.putInt("wins", currentWins + 1)
        }
        edit.apply()
    }
}
