package com.example.game.viewmodel

import android.app.Application
import com.example.db.YogaDatabase
import com.example.game.model.GameProgress
import com.example.game.model.PartyMemberData
import com.example.game.persistence.GameSaveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GameSyncManager(
    private val _saveData: MutableStateFlow<GameProgress>,
    private val _party: MutableStateFlow<List<PartyMemberData>>,
    private val saveManager: GameSaveManager,
    private val application: Application,
    private val viewModelScope: CoroutineScope
) {
    private var lastSyncedMainSparks: Int = 0

    fun refreshSync() {
        viewModelScope.launch { syncWithMainApp() }
    }

    suspend fun syncWithMainApp() = withContext(Dispatchers.IO) {
        val db = YogaDatabase.getDatabase(application)
        val sessions = db.yogaSessionDao().getAllSessions().first()

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
        val uniqueDays = sessions.map { dateFormat.format(java.util.Date(it.timestamp)) }.distinct()
        val mainSparks = uniqueDays.size

        var xpSum = 0
        sessions.forEach { session ->
            xpSum += com.example.model.XpCalculator.calculateSessionXp(session.durationMinutes, session.flowId)
        }
        xpSum += mainSparks * 150
        val computedLevel = com.example.model.LevelDefinitions.getLevelForXp(xpSum).level

        val data = _saveData.value
        var updated = data
        if (data.yogaLevel != computedLevel) {
            updated = updated.copy(yogaLevel = computedLevel)
        }
        val delta = mainSparks - lastSyncedMainSparks
        if (delta > 0) {
            updated = updated.copy(sparks = updated.sparks + delta)
        }
        lastSyncedMainSparks = mainSparks
        updated = updated.copy(totalYogaXp = xpSum)
        if (xpSum > data.totalYogaXp) {
            val newGoldEarned = (xpSum - data.totalYogaXp) / 10
            if (newGoldEarned > 0) {
                updated = updated.copy(gold = updated.gold + newGoldEarned)
            }
        }
        if (updated != data) {
            _saveData.value = updated
            _party.value = updated.party
            saveManager.saveGame(updated)
        }
    }
}