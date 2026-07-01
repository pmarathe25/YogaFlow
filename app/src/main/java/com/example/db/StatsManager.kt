package com.example.db

import com.example.game.model.TrophyRarity
import com.example.model.LevelDefinitions
import com.example.model.XpCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StatsManager(
    private val scope: CoroutineScope,
    private val repository: YogaSessionRepository
) {
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
        scope.launch {
            allSessions.collect { sessions ->
                _totalSessions.value = sessions.size
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
                val uniqueDays = sessions.map {
                    dateFormat.format(Date(it.timestamp))
                }.distinct()
                
                val totalSparksVal = uniqueDays.size
                _totalSparks.value = totalSparksVal
                
                val todayStr = dateFormat.format(Date())
                val questCompleted = uniqueDays.contains(todayStr)
                _dailyQuestCompleted.value = questCompleted
                
                var xpSum = 0
                sessions.forEach { session ->
                    xpSum += XpCalculator.calculateSessionXp(session.durationMinutes, session.flowId)
                }
                xpSum += totalSparksVal * 150
                _totalXp.value = xpSum
                
                val levelDef = LevelDefinitions.getLevelForXp(xpSum)
                _currentLevel.value = levelDef.level
                _currentLevelName.value = levelDef.name
                _levelProgress.value = LevelDefinitions.getLevelProgress(xpSum)
                
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
                        isUnlocked = levelDef.level >= 3,
                        progressText = "Lvl ${levelDef.level}/3"
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
        scope.launch {
            repository.clearSessions()
        }
    }
}

interface AchievementWithRarity {
    val rarity: TrophyRarity?
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val progressText: String = ""
) : AchievementWithRarity {
    override val rarity: TrophyRarity? get() = null
}
