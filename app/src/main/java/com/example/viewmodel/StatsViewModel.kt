package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.YogaDatabase
import com.example.db.YogaSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = YogaDatabase.getDatabase(application)
    private val repository = YogaSessionRepository(database.yogaSessionDao())
    val allSessions = repository.allSessions

    private val _totalSessions = MutableStateFlow(0)
    val totalSessions: StateFlow<Int> = _totalSessions.asStateFlow()

    private val _totalXp = MutableStateFlow(0)
    val totalXp: StateFlow<Int> = _totalXp.asStateFlow()

    private val _currentLevel = MutableStateFlow(1)
    val currentLevel: StateFlow<Int> = _currentLevel.asStateFlow()

    private val _currentLevelName = MutableStateFlow("Prana Sprout")
    val currentLevelName: StateFlow<String> = _currentLevelName.asStateFlow()

    private val _levelProgress = MutableStateFlow(0f)
    val levelProgress: StateFlow<Float> = _levelProgress.asStateFlow()

    private val _totalSparks = MutableStateFlow(0)
    val totalSparks: StateFlow<Int> = _totalSparks.asStateFlow()

    private val _dailyQuestCompleted = MutableStateFlow(false)
    val dailyQuestCompleted: StateFlow<Boolean> = _dailyQuestCompleted.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    init {
        viewModelScope.launch {
            allSessions.collect { sessions ->
                _totalSessions.value = sessions.size
                
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getDefault()
                }
                val uniqueDays = sessions.map {
                    dateFormat.format(java.util.Date(it.timestamp))
                }.distinct()
                
                val totalSparksVal = uniqueDays.size
                _totalSparks.value = totalSparksVal
                
                val todayStr = dateFormat.format(java.util.Date())
                val questCompleted = uniqueDays.contains(todayStr)
                _dailyQuestCompleted.value = questCompleted
                
                var xpSum = 0
                sessions.forEach { session ->
                    xpSum += 150
                    xpSum += session.durationMinutes * 10
                    val difficultyBonus = when (session.flowId) {
                        "restorative_yin", "morning_energizer", "bedtime_wind_down" -> 30
                        "sun_salutation", "heart_opening" -> 50
                        "warrior_flow", "core_balance" -> 100
                        "power_vinyasa", "ashtanga_core", "balance_mastery" -> 150
                        else -> 30
                    }
                    xpSum += difficultyBonus
                }
                xpSum += totalSparksVal * 150
                _totalXp.value = xpSum
                
                var level = 1
                var levelName = "Prana Sprout"
                var progress = 0f
                if (xpSum <= 300) {
                    level = 1; levelName = "Prana Sprout"; progress = xpSum.toFloat() / 300f
                } else if (xpSum <= 900) {
                    level = 2; levelName = "Zen Seeker"; progress = (xpSum - 300).toFloat() / 600f
                } else if (xpSum <= 2100) {
                    level = 3; levelName = "Flow Initiate"; progress = (xpSum - 900).toFloat() / 1200f
                } else if (xpSum <= 4500) {
                    level = 4; levelName = "Mindfulness Guide"; progress = (xpSum - 2100).toFloat() / 2400f
                } else if (xpSum <= 9300) {
                    level = 5; levelName = "Prana Master"; progress = (xpSum - 4500).toFloat() / 4800f
                } else if (xpSum <= 18900) {
                    level = 6; levelName = "Cosmic Flow Yogi"; progress = (xpSum - 9300).toFloat() / 9600f
                } else if (xpSum <= 38100) {
                    level = 7; levelName = "Inner Light Sage"; progress = (xpSum - 18900).toFloat() / 19200f
                } else if (xpSum <= 76500) {
                    level = 8; levelName = "Chakra Harmonizer"; progress = (xpSum - 38100).toFloat() / 38400f
                } else if (xpSum <= 153300) {
                    level = 9; levelName = "Nirvana Ascendant"; progress = (xpSum - 76500).toFloat() / 76800f
                } else {
                    level = 10; levelName = "Infinite Samadhi"; progress = 1.0f
                }
                _currentLevel.value = level
                _currentLevelName.value = levelName
                _levelProgress.value = progress.coerceIn(0f, 1f)
                
                val uniqueFlowsCount = sessions.map { it.flowId }.distinct().size
                val hasDeepDevotee = sessions.any { it.durationMinutes >= 5 }
                
                val list = listOf(
                    Achievement(
                        id = "first_breath",
                        title = "First Breath",
                        description = "Complete any yoga flow session.",
                        isUnlocked = sessions.isNotEmpty(),
                        progressText = "${minOf(sessions.size, 1)}/1"
                    ),
                    Achievement(
                        id = "zen_spark_collector",
                        title = "Zen Spark Collector",
                        description = "Earn 3 Daily Zen Sparks by practicing on different days.",
                        isUnlocked = totalSparksVal >= 3,
                        progressText = "$totalSparksVal/3"
                    ),
                    Achievement(
                        id = "tri_fold_harmony",
                        title = "Tri-Fold Harmony",
                        description = "Practice all 3 unique yoga flows.",
                        isUnlocked = uniqueFlowsCount >= 3,
                        progressText = "$uniqueFlowsCount/3"
                    ),
                    Achievement(
                        id = "yogi_adept",
                        title = "Yogi Adept",
                        description = "Reach Level 3 (Flow Initiate).",
                        isUnlocked = level >= 3,
                        progressText = "Lvl $level/3"
                    ),
                    Achievement(
                        id = "deep_devotee",
                        title = "Deep Devotee",
                        description = "Complete a deep session (5+ mins).",
                        isUnlocked = hasDeepDevotee,
                        progressText = if (hasDeepDevotee) "1/1" else "0/1"
                    )
                )
                _achievements.value = list
            }
        }
    }

    fun clearAllCompletedSessions() {
        viewModelScope.launch {
            repository.clearSessions()
        }
    }
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val progressText: String = ""
)