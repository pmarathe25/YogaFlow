package com.example.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AudioCueManager
import com.example.audio.ZenSoundSynthesizer
import com.example.model.YogaFlow
import com.example.model.YogaPose
import com.example.model.YogaFlowRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.first

class YogaViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "YogaViewModel"

    // Room Database Integration
    private val database = com.example.db.YogaDatabase.getDatabase(application)
    private val repository = com.example.db.YogaSessionRepository(database.yogaSessionDao())
    val allSessions = repository.allSessions

    // Reminder State
    val allReminders = database.reminderDao().getAllReminders()
    
    fun addReminder(reminder: com.example.db.ReminderEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val existingReminders = database.reminderDao().getRemindersForFlow(reminder.flowId).first()
            val newDays = reminder.daysOfWeek.split(",").filter { it.isNotEmpty() }.toSet()
            
            val isSubsetOrDuplicate = existingReminders.any {
                val existingDays = it.daysOfWeek.split(",").filter { d -> d.isNotEmpty() }.toSet()
                it.hour == reminder.hour &&
                it.minute == reminder.minute &&
                (existingDays.containsAll(newDays) || newDays.isEmpty()) // If new reminder has no specific days, and there's already an everyday/empty reminder? Actually, if days are empty, it's a one-time reminder.
            }
            
            val actualIsSubset = existingReminders.any {
                val existingDays = it.daysOfWeek.split(",").filter { d -> d.isNotEmpty() }.toSet()
                it.hour == reminder.hour &&
                it.minute == reminder.minute &&
                (if (newDays.isEmpty()) existingDays.isEmpty() else existingDays.containsAll(newDays))
            }
            
            if (!actualIsSubset) {
                val id = database.reminderDao().insert(reminder).toInt()
                val reminderWithId = reminder.copy(id = id)
                com.example.db.ReminderManager.scheduleNextAlarm(getApplication(), reminderWithId)
                
                // Sync backup
                val updatedReminders = database.reminderDao().getAllReminders().first()
                com.example.db.ReminderManager.saveRemindersBackup(getApplication(), updatedReminders)
                
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun updateReminder(reminder: com.example.db.ReminderEntity) {
        viewModelScope.launch {
            database.reminderDao().update(reminder)
            com.example.db.ReminderManager.scheduleNextAlarm(getApplication(), reminder)
            
            // Sync backup
            val updatedReminders = database.reminderDao().getAllReminders().first()
            com.example.db.ReminderManager.saveRemindersBackup(getApplication(), updatedReminders)
        }
    }

    fun deleteReminder(reminder: com.example.db.ReminderEntity) {
        viewModelScope.launch {
            database.reminderDao().delete(reminder)
            
            // Sync backup
            val updatedReminders = database.reminderDao().getAllReminders().first()
            com.example.db.ReminderManager.saveRemindersBackup(getApplication(), updatedReminders)
        }
    }

    // Favorite Flows State
    val favoriteFlowIds: StateFlow<List<String>> = repository.favoriteFlowIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleFavoriteFlow(flowId: String) {
        viewModelScope.launch {
            val isCurrentlyFavorite = favoriteFlowIds.value.contains(flowId)
            repository.toggleFavorite(flowId, !isCurrentlyFavorite)
        }
    }

    // Reactive Stats State
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

    // Garden State
    val gardenItems = database.gardenItemDao().getAllItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availableXp = combine(totalXp, gardenItems) { total, items ->
        val spent = items.sumOf { com.example.model.GardenShop.getCost(it.itemType) }
        total - spent
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun buyGardenItem(type: String, x: Float, y: Float, onResult: (Boolean) -> Unit) {
        val cost = com.example.model.GardenShop.getCost(type)
        if (availableXp.value >= cost) {
            viewModelScope.launch {
                database.gardenItemDao().insert(com.example.db.GardenItemEntity(itemType = type, x = x, y = y))
                onResult(true)
            }
        } else {
            onResult(false)
        }
    }

    fun upgradeZone(zoneId: String, newLevel: Int, onResult: (Boolean) -> Unit) {
        val newType = "${zoneId}_${newLevel}"
        val cost = com.example.model.GardenShop.getCost(newType)
        if (availableXp.value >= cost) {
            viewModelScope.launch {
                val currentItems = database.gardenItemDao().getAllItems().first()
                val oldItem = currentItems.find { it.itemType.startsWith("${zoneId}_") }
                if (oldItem != null) {
                    database.gardenItemDao().delete(oldItem)
                }
                database.gardenItemDao().insert(com.example.db.GardenItemEntity(itemType = newType, x = 0f, y = 0f))
                onResult(true)
            }
        } else {
            onResult(false)
        }
    }

    fun removeGardenItem(item: com.example.db.GardenItemEntity) {
        viewModelScope.launch {
            database.gardenItemDao().delete(item)
        }
    }

    // --- Sanskrit RPG Battle Game States ---
    private val _rpgUnlockedHeroes = MutableStateFlow<Set<String>>(emptySet())
    val rpgUnlockedHeroes: StateFlow<Set<String>> = _rpgUnlockedHeroes.asStateFlow()

    private val _rpgActiveParty = MutableStateFlow<List<String>>(emptyList())
    val rpgActiveParty: StateFlow<List<String>> = _rpgActiveParty.asStateFlow()

    private val _rpgSpentKarmaXp = MutableStateFlow(0)
    val rpgSpentKarmaXp: StateFlow<Int> = _rpgSpentKarmaXp.asStateFlow()

    private val _rpgSpentSparks = MutableStateFlow(0)
    val rpgSpentSparks: StateFlow<Int> = _rpgSpentSparks.asStateFlow()

    private val _rpgExtraKarmaXp = MutableStateFlow(0)
    val rpgExtraKarmaXp: StateFlow<Int> = _rpgExtraKarmaXp.asStateFlow()

    private val _rpgExtraSparks = MutableStateFlow(0)
    val rpgExtraSparks: StateFlow<Int> = _rpgExtraSparks.asStateFlow()

    private val _rpgStats = MutableStateFlow<Pair<Int, Int>>(Pair(0, 0)) // Wins, Battles
    val rpgStats: StateFlow<Pair<Int, Int>> = _rpgStats.asStateFlow()

    private val _rpgHeroLevels = MutableStateFlow<Map<String, Int>>(emptyMap())
    val rpgHeroLevels: StateFlow<Map<String, Int>> = _rpgHeroLevels.asStateFlow()

    fun loadRpgData() {
        val app = getApplication<Application>()
        _rpgUnlockedHeroes.value = com.example.db.RpgManager.getUnlockedHeroes(app)
        _rpgActiveParty.value = com.example.db.RpgManager.getActiveParty(app)
        _rpgSpentKarmaXp.value = com.example.db.RpgManager.getSpentKarmaXp(app)
        _rpgSpentSparks.value = com.example.db.RpgManager.getSpentSparks(app)
        _rpgExtraKarmaXp.value = com.example.db.RpgManager.getExtraKarmaXp(app)
        _rpgExtraSparks.value = com.example.db.RpgManager.getExtraSparks(app)
        _rpgStats.value = com.example.db.RpgManager.getStats(app)

        val levels = mutableMapOf<String, Int>()
        listOf("Shanti", "Santosha", "Virya", "Dhairya", "Maitri").forEach { heroId ->
            levels[heroId] = com.example.db.RpgManager.getHeroLevel(app, heroId)
        }
        _rpgHeroLevels.value = levels
    }

    val rpgAvailableKarmaXp = combine(totalXp, _rpgExtraKarmaXp, _rpgSpentKarmaXp) { yogaXp, extraXp, spentXp ->
        (yogaXp + extraXp - spentXp).coerceAtLeast(0)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val rpgAvailableSparks = combine(totalSparks, _rpgExtraSparks, _rpgSpentSparks) { yogaSparks, extraSparks, spentSparks ->
        (yogaSparks + extraSparks - spentSparks).coerceAtLeast(0)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun rpgUnlockHero(heroId: String, costSparks: Int, onResult: (Boolean) -> Unit) {
        val app = getApplication<Application>()
        if (rpgAvailableSparks.value >= costSparks) {
            val newSpent = _rpgSpentSparks.value + costSparks
            com.example.db.RpgManager.saveSpentSparks(app, newSpent)
            com.example.db.RpgManager.unlockHero(app, heroId)
            
            _rpgSpentSparks.value = newSpent
            _rpgUnlockedHeroes.value = com.example.db.RpgManager.getUnlockedHeroes(app)
            onResult(true)
        } else {
            onResult(false)
        }
    }

    fun rpgLevelUpHero(heroId: String, costXp: Int, onResult: (Boolean) -> Unit) {
        val app = getApplication<Application>()
        if (rpgAvailableKarmaXp.value >= costXp) {
            val newSpent = _rpgSpentKarmaXp.value + costXp
            com.example.db.RpgManager.saveSpentKarmaXp(app, newSpent)
            com.example.db.RpgManager.levelUpHero(app, heroId)
            
            _rpgSpentKarmaXp.value = newSpent
            loadRpgData()
            onResult(true)
        } else {
            onResult(false)
        }
    }

    fun rpgTogglePartyMember(heroId: String) {
        val app = getApplication<Application>()
        val currentParty = _rpgActiveParty.value.toMutableList()
        if (currentParty.contains(heroId)) {
            if (currentParty.size > 1) {
                currentParty.remove(heroId)
            }
        } else {
            if (currentParty.size < 3) {
                currentParty.add(heroId)
            }
        }
        com.example.db.RpgManager.saveActiveParty(app, currentParty)
        _rpgActiveParty.value = currentParty
    }

    fun rpgRecordBattleResult(won: Boolean, xpEarned: Int, sparksEarned: Int) {
        val app = getApplication<Application>()
        com.example.db.RpgManager.recordBattle(app, won)
        _rpgStats.value = com.example.db.RpgManager.getStats(app)
        
        val newExtraXp = _rpgExtraKarmaXp.value + xpEarned
        val newExtraSparks = _rpgExtraSparks.value + sparksEarned
        com.example.db.RpgManager.saveExtraKarmaXp(app, newExtraXp)
        com.example.db.RpgManager.saveExtraSparks(app, newExtraSparks)
        _rpgExtraKarmaXp.value = newExtraXp
        _rpgExtraSparks.value = newExtraSparks
    }

    // Theme Mode: "System", "Light", "Dark"
    private val _themeMode = MutableStateFlow("System")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        com.example.db.SettingsManager.saveThemeMode(getApplication(), mode)
    }

    // Keep Screen Awake during Practice: Default true
    private val _keepScreenAwake = MutableStateFlow(true)
    val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

    fun setKeepScreenAwake(enabled: Boolean) {
        _keepScreenAwake.value = enabled
        com.example.db.SettingsManager.saveKeepScreenAwake(getApplication(), enabled)
    }

    // Background Audio Playback when Screen is Off: Default true
    private val _backgroundAudioEnabled = MutableStateFlow(true)
    val backgroundAudioEnabled: StateFlow<Boolean> = _backgroundAudioEnabled.asStateFlow()

    fun setBackgroundAudioEnabled(enabled: Boolean) {
        _backgroundAudioEnabled.value = enabled
        com.example.db.SettingsManager.saveBackgroundAudioEnabled(getApplication(), enabled)
        updateWakeLockState()
    }

    // Countdown state variables
    private val _isCountdownActive = MutableStateFlow(false)
    val isCountdownActive: StateFlow<Boolean> = _isCountdownActive.asStateFlow()

    private val _countdownRemaining = MutableStateFlow(5)
    val countdownRemaining: StateFlow<Int> = _countdownRemaining.asStateFlow()

    private var countdownJob: Job? = null

    // Audio cue manager instance
    val audioCueManager = AudioCueManager(application)

    private val zenSoundSynthesizer = ZenSoundSynthesizer(application)

    // Ambient Music Manager
    val ambientMusicManager = com.example.audio.AmbientMusicManager(application)

    private val _isMusicMuted = MutableStateFlow(false)
    val isMusicMuted: StateFlow<Boolean> = _isMusicMuted.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    // Predefined flow
    private val _flow = MutableStateFlow<YogaFlow>(YogaFlowRepository.sunSalutationFlow)
    val flow: StateFlow<YogaFlow> = _flow.asStateFlow()

    // Current session state
    private val _currentPoseIndex = MutableStateFlow(0)
    val currentPoseIndex: StateFlow<Int> = _currentPoseIndex.asStateFlow()

    private val _remainingTimeSec = MutableStateFlow(30)
    val remainingTimeSec: StateFlow<Int> = _remainingTimeSec.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _preferredVoice = MutableStateFlow("en")
    val preferredVoice: StateFlow<String> = _preferredVoice.asStateFlow()

    private val _isSessionCompleted = MutableStateFlow(false)
    val isSessionCompleted: StateFlow<Boolean> = _isSessionCompleted.asStateFlow()

    // Audio source indicator for UI feedback
    private val _speechState = MutableStateFlow<AudioCueManager.SpeechState>(AudioCueManager.SpeechState.Idle)
    val speechState: StateFlow<AudioCueManager.SpeechState> = _speechState.asStateFlow()

    // Timer Job
    private var timerJob: Job? = null

    // Tone generator for transit beeps
    // ToneGenerator removed

    init {
        loadRpgData()
        try {
            // No init needed
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize ToneGenerator: ${e.message}")
        }
        
        // Load settings
        val app = getApplication<Application>()
        _themeMode.value = com.example.db.SettingsManager.getThemeMode(app)
        _keepScreenAwake.value = com.example.db.SettingsManager.getKeepScreenAwake(app)
        _backgroundAudioEnabled.value = com.example.db.SettingsManager.getBackgroundAudioEnabled(app)
        _isVoiceEnabled.value = com.example.db.SettingsManager.getIsVoiceEnabled(app)
        _preferredVoice.value = com.example.db.SettingsManager.getPreferredVoice(app)
        
        val isMuted = com.example.db.SettingsManager.getIsMusicMuted(app)
        val trackIdx = com.example.db.SettingsManager.getCurrentTrackIndex(app)
        _isMusicMuted.value = isMuted
        _currentTrackIndex.value = trackIdx
        if (isMuted) {
            ambientMusicManager.toggleMute() // Sets to muted, assuming default is unmuted
        }
        ambientMusicManager.setCurrentTrackIndex(trackIdx)

        // Reschedule all reminders on app startup to handle device reboot / app reinstall restorations
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val dbReminders = database.reminderDao().getAllReminders().first()
                val backupReminders = com.example.db.ReminderManager.getRemindersBackup(getApplication())
                
                if (dbReminders.isEmpty() && backupReminders.isNotEmpty()) {
                    Log.d(tag, "Database empty but backup found! Restoring ${backupReminders.size} reminders.")
                    for (backup in backupReminders) {
                        val newReminder = com.example.db.ReminderEntity(
                            flowId = backup.flowId,
                            flowName = backup.flowName,
                            hour = backup.hour,
                            minute = backup.minute,
                            daysOfWeek = backup.daysOfWeek
                        )
                        val newId = database.reminderDao().insert(newReminder).toInt()
                        val reminderWithId = newReminder.copy(id = newId)
                        com.example.db.ReminderManager.scheduleNextAlarm(getApplication(), reminderWithId)
                    }
                    val updatedReminders = database.reminderDao().getAllReminders().first()
                    com.example.db.ReminderManager.saveRemindersBackup(getApplication(), updatedReminders)
                } else {
                    for (reminder in dbReminders) {
                        com.example.db.ReminderManager.scheduleNextAlarm(getApplication(), reminder)
                    }
                    com.example.db.ReminderManager.saveRemindersBackup(getApplication(), dbReminders)
                }
                Log.d(tag, "Successfully processed all reminders on startup.")
            } catch (e: Exception) {
                Log.e(tag, "Failed to restore/reschedule reminders on startup: ${e.message}")
            }
        }

        // Link speech state changes to update our live UI
        audioCueManager.onSpeechStateChanged = { state ->
            _speechState.value = state
        }

        // Reactively calculate session stats and gamified progress on database update
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
                    xpSum += 150 // Base XP
                    xpSum += session.durationMinutes * 10 // Duration XP
                    val difficultyBonus = when (session.flowId) {
                        "restorative_yin", "morning_energizer", "bedtime_wind_down" -> 30
                        "sun_salutation", "heart_opening" -> 50
                        "warrior_flow", "core_balance" -> 100
                        "power_vinyasa", "ashtanga_core", "balance_mastery" -> 150
                        else -> 30
                    }
                    xpSum += difficultyBonus
                }
                xpSum += totalSparksVal * 150 // Daily Zen Spark bonus (+150 XP per day practiced)
                _totalXp.value = xpSum
                
                var level = 1
                var levelName = "Prana Sprout"
                var progress = 0f
                if (xpSum <= 300) {
                    level = 1
                    levelName = "Prana Sprout"
                    progress = xpSum.toFloat() / 300f
                } else if (xpSum <= 900) {
                    level = 2
                    levelName = "Zen Seeker"
                    progress = (xpSum - 300).toFloat() / 600f
                } else if (xpSum <= 2100) {
                    level = 3
                    levelName = "Flow Initiate"
                    progress = (xpSum - 900).toFloat() / 1200f
                } else if (xpSum <= 4500) {
                    level = 4
                    levelName = "Mindfulness Guide"
                    progress = (xpSum - 2100).toFloat() / 2400f
                } else if (xpSum <= 9300) {
                    level = 5
                    levelName = "Prana Master"
                    progress = (xpSum - 4500).toFloat() / 4800f
                } else if (xpSum <= 18900) {
                    level = 6
                    levelName = "Cosmic Flow Yogi"
                    progress = (xpSum - 9300).toFloat() / 9600f
                } else if (xpSum <= 38100) {
                    level = 7
                    levelName = "Inner Light Sage"
                    progress = (xpSum - 18900).toFloat() / 19200f
                } else if (xpSum <= 76500) {
                    level = 8
                    levelName = "Chakra Harmonizer"
                    progress = (xpSum - 38100).toFloat() / 38400f
                } else if (xpSum <= 153300) {
                    level = 9
                    levelName = "Nirvana Ascendant"
                    progress = (xpSum - 76500).toFloat() / 76800f
                } else {
                    level = 10
                    levelName = "Infinite Samadhi"
                    progress = 1.0f
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

    val currentPose: StateFlow<YogaPose?> = combine(_flow, _currentPoseIndex) { flow, index ->
        if (index in flow.poses.indices) flow.poses[index] else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun selectFlow(yogaFlow: YogaFlow) {
        _flow.value = yogaFlow
        _currentPoseIndex.value = 0
        _remainingTimeSec.value = yogaFlow.poses.firstOrNull()?.holdDurationSec ?: 30
        _isSessionCompleted.value = false
        _isPlaying.value = false
        _isCountdownActive.value = false
        countdownJob?.cancel()
        countdownJob = null
    }

    fun togglePlay() {
        if (_isSessionCompleted.value) {
            restartSession()
            return
        }

        if (_isCountdownActive.value) {
            skipCountdownAndStart()
            return
        }

        if (_isPlaying.value) {
            pauseSession()
        } else {
            resumeSession()
        }
    }

    private fun resumeSession() {
        _isPlaying.value = true
        _isCountdownActive.value = false
        updateWakeLockState()
        startTimer()
        ambientMusicManager.play()
        // If we were at the very start of a pose (30 seconds), speak immediately
        if (_remainingTimeSec.value == (_flow.value.poses.getOrNull(_currentPoseIndex.value)?.holdDurationSec ?: 30)) {
            triggerVoiceCueForCurrentPose()
        }
    }

    private fun pauseSession() {
        _isPlaying.value = false
        updateWakeLockState()
        timerJob?.cancel()
        timerJob = null
        audioCueManager.stop()
        ambientMusicManager.pause()
    }

    fun toggleMusicMute() {
        ambientMusicManager.toggleMute()
        val muted = ambientMusicManager.isMuted()
        _isMusicMuted.value = muted
        com.example.db.SettingsManager.saveIsMusicMuted(getApplication(), muted)
    }

    fun selectAmbientTrack(index: Int) {
        if (index >= 0 && index < ambientMusicManager.tracks.size) {
            _currentTrackIndex.value = index
            ambientMusicManager.setCurrentTrackIndex(index)
            com.example.db.SettingsManager.saveCurrentTrackIndex(getApplication(), index)
            if (_isPlaying.value || _isCountdownActive.value) {
                ambientMusicManager.playTrack(index)
            }
        }
    }

    fun skipForward() {
        val nextIndex = _currentPoseIndex.value + 1
        if (nextIndex < _flow.value.poses.size) {
            _currentPoseIndex.value = nextIndex
            _remainingTimeSec.value = _flow.value.poses[_currentPoseIndex.value].holdDurationSec
            _isSessionCompleted.value = false
            if (_isPlaying.value) {
                triggerVoiceCueForCurrentPose()
            }
        } else {
            completeSession()
        }
    }

    fun skipBackward() {
        val prevIndex = _currentPoseIndex.value - 1
        if (prevIndex >= 0) {
            _currentPoseIndex.value = prevIndex
            _remainingTimeSec.value = _flow.value.poses[_currentPoseIndex.value].holdDurationSec
            _isSessionCompleted.value = false
            if (_isPlaying.value) {
                triggerVoiceCueForCurrentPose()
            }
        }
    }

    fun selectPoseDirectly(index: Int) {
        if (index in _flow.value.poses.indices) {
            _currentPoseIndex.value = index
            _remainingTimeSec.value = _flow.value.poses[index].holdDurationSec
            _isSessionCompleted.value = false
            if (_isPlaying.value) {
                triggerVoiceCueForCurrentPose()
            }
        }
    }

    fun toggleVoice(enabled: Boolean) {
        _isVoiceEnabled.value = enabled
        com.example.db.SettingsManager.saveIsVoiceEnabled(getApplication(), enabled)
        if (!enabled) {
            audioCueManager.stop()
        } else if (_isPlaying.value) {
            triggerVoiceCueForCurrentPose()
        }
    }

    fun setPreferredVoice(voice: String) {
        _preferredVoice.value = voice
        com.example.db.SettingsManager.savePreferredVoice(getApplication(), voice)
        if (_isPlaying.value) {
            if (_isVoiceEnabled.value) {
                triggerVoiceCueForCurrentPose()
            }
        } else if (_isVoiceEnabled.value) {
            val previewText = when (voice) {
                "sa" -> "स्वस्ति। संस्कृत ध्वनि मार्गदर्शिका।"
                else -> "English voice guide selected."
            }
            val apiKey = BuildConfig.GEMINI_API_KEY
            audioCueManager.speak(apiKey, previewText, voice)
        }
    }

    fun resetForDashboard() {
        audioCueManager.stop()
        ambientMusicManager.stop()
        _currentPoseIndex.value = 0
        _remainingTimeSec.value = _flow.value.poses.firstOrNull()?.holdDurationSec ?: 30
        _isSessionCompleted.value = false
        _isPlaying.value = false
        _isCountdownActive.value = false
        updateWakeLockState()
        countdownJob?.cancel()
        countdownJob = null
        timerJob?.cancel()
        timerJob = null
    }

    fun restartSession() {
        audioCueManager.stop()
        ambientMusicManager.stop()
        _currentPoseIndex.value = 0
        _remainingTimeSec.value = _flow.value.poses.firstOrNull()?.holdDurationSec ?: 30
        _isSessionCompleted.value = false
        timerJob?.cancel()
        timerJob = null
        
        startCountdown()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_remainingTimeSec.value > 1) {
                    _remainingTimeSec.value -= 1
                } else {
                    _remainingTimeSec.value = 0
                    onPoseTimeComplete()
                }
            }
        }
    }

    private fun onPoseTimeComplete() {
        val nextIndex = _currentPoseIndex.value + 1
        if (nextIndex < _flow.value.poses.size) {
            // Move to next pose
            playTransitBeep()
            _currentPoseIndex.value = nextIndex
            _remainingTimeSec.value = _flow.value.poses[_currentPoseIndex.value].holdDurationSec
            triggerVoiceCueForCurrentPose()
        } else {
            // End of entire flow
            completeSession()
        }
    }

    private fun completeSession() {
        _isPlaying.value = false
        _isSessionCompleted.value = true
        updateWakeLockState()
        timerJob?.cancel()
        timerJob = null
        playCompletionBeep()
        ambientMusicManager.stop()

        // Log completed session in Room database
        viewModelScope.launch {
            try {
                repository.insertSession(
                    com.example.db.YogaSession(
                        flowId = _flow.value.id,
                        flowName = _flow.value.name,
                        durationMinutes = _flow.value.totalDurationMinutes
                    )
                )
                Log.d(tag, "Successfully logged completed yoga session to database.")
            } catch (e: Exception) {
                Log.e(tag, "Error saving session to Room database: ${e.message}")
            }
        }

        if (_isVoiceEnabled.value) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val message = if (_preferredVoice.value == "sa") {
                "अभिनन्दनम्। योगसाधना समाप्ता। ओम् शान्तिः शान्तिः शान्तिः।"
            } else {
                "Congratulations! You have completed your ${flow.value.name} practice. Namaste."
            }
            audioCueManager.speak(apiKey, message, _preferredVoice.value)
        }
    }

    fun getSanskritPromptForPose(poseId: Int): String {
        return when (poseId) {
            1 -> "प्रणामासनं कुर्यात्। करसम्पुटं हृदयस्थाने धारयन्तु। दीर्घं निःश्वस्य शान्तं भवन्तु।"
            2 -> "हस्तौत्तानासनं कुर्यात्। उद्गस्य हस्तौ ऊर्ध्वं प्रयच्छन्तु। सर्वशरीरं दीर्घतानन्तु।"
            3 -> "उत्तानासनं कुर्यात्। अधोगत्वा पार्श्वपादौ स्पृशन्तु। शिरोग्रीवं शिथिलीकुर्यात्।"
            4 -> "अश्वसञ्चालनासनं कुर्यात्। दक्षिणपादं पृष्ठे स्थापयन्तु। वक्षःप्रसास्य अग्रे पश्यन्तु।"
            5 -> "फलकासनं कुर्यात्। वामपादं पृष्ठे स्थापयन्तु। शरीरं सरलरेखा-रूपेण धारयन्तु।"
            6 -> "अष्टाङ्गनमस्कारं कुर्यात्। जानु-वक्षः-हनु भूमौ स्थापयन्तु। नितम्बं कोष्ठोन्नतं धारयन्तु।"
            7 -> "भुजङ्गासनं कुर्यात्। अग्रे सर्प-रूपेण शरीरं ऊर्ध्वं उत्थापयन्तु। वक्षःप्रसास्य हृदयं उद्भासन्तु।"
            8 -> "अधोमुखश्वानासनं कुर्यात्। नितम्बं ऊर्ध्वं प्रयच्छन्तु। सर्वशरीरं पर्वताकारं धारयन्तु।"
            9 -> "अश्वसञ्चालनासनं कुर्यात्। दक्षिणपादं अग्रे स्थापयन्तु। अग्रे पश्यन्तु।"
            10 -> "उत्तानासनं कुर्यात्। वामपादं अग्रे स्थापयन्तु, दीर्घं नमन्तु।"
            11 -> "हस्तौत्तानासनं कुर्यात्। उद्गस्य हस्तौ ऊर्ध्वं प्रयच्छन्तु, वक्षः उद्भासन्तु।"
            12 -> "प्रणामासनं कुर्यात्। निःश्वस्य हस्तौ हृदयस्थाने स्थापयन्तु। ओम् शान्तिः शान्तिः शान्तिः।"
            
            201 -> "ताडासनं कुर्यात्। समस्थिति। पादौ भूमौ धारयन्तु। स्वास्थ्य स्थितौ भवन्तु।"
            202 -> "वीरभद्रासनं प्रथमं कुर्यात्। वामपादं पृष्ठे धारयन्तु, हस्तौ ऊर्ध्वं उत्थापयन्तु।"
            203 -> "वीरभद्रासनं द्वितीयं कुर्यात्। हस्तौ पार्श्वे प्रसार्यन्तु, अग्रे पश्यन्तु।"
            204 -> "त्रिकोणासनं कुर्यात्। पार्श्वभङ्गेन हस्त-शिखरे पश्यन्तु।"
            205 -> "बालासनं कुर्यात्। नितम्बौ पादपृष्ठे स्थापयन्तु, शिरो भूमौ नमन्तु। शान्तं भवन्तु।"
            
            301 -> "सालम्बभुजङ्गासनं कुर्यात्। कूर्परौ भूमौ धारयन्तु, ऊर्ध्वं पश्यन्तु।"
            302 -> "बद्धकोणासनं कुर्यात्। पादतलौ परस्परं संयोजयन्तु।"
            303 -> "सुप्तमत्स्येन्द्रासनं कुर्यात्। पार्श्वपरिवर्तनेन ध्यानं कुर्यात्।"
            304 -> "शवासनं कुर्यात्। सर्वशरीरं शिथिलीकुर्यात्, ध्यानमयस्थितौ विश्रामन्तु। ओम्, शान्तिः, शान्तिः, शान्तिः।"
            else -> "ध्यानं कुर्यात्। शान्तं ध्यानं।"
        }
    }

    fun triggerVoiceCueForCurrentPose() {
        if (!_isVoiceEnabled.value) return
        viewModelScope.launch {
            var waitTime = 0
            while (currentPose.value == null && waitTime < 2000) {
                delay(100)
                waitTime += 100
            }
            if (!_isPlaying.value && !_isCountdownActive.value) return@launch
            val current = currentPose.value ?: return@launch
            val apiKey = BuildConfig.GEMINI_API_KEY
            var text = if (_preferredVoice.value == "sa") {
                getSanskritPromptForPose(current.id)
            } else {
                current.voicePrompt
            }
            text = text.replace(Regex("^.*? (Step \\d+:)"), "$1")
            audioCueManager.speak(apiKey, text, _preferredVoice.value)
        }
    }

    // --- Countdown Start Implementation ---
    fun startCountdown() {
        countdownJob?.cancel()
        timerJob?.cancel()
        _isCountdownActive.value = true
        _countdownRemaining.value = 3
        _isPlaying.value = false
        updateWakeLockState()
        
        // Start ambient music in background at comfortable volume
        ambientMusicManager.play()

        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _countdownRemaining.value = i
                zenSoundSynthesizer.playWoodTap()
                delay(1000)
            }
            
            // Countdown finished!
            _isCountdownActive.value = false
            _isPlaying.value = true
            
            // Start the actual practice
            startTimer()
            
            triggerVoiceCueForCurrentPose()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        timerJob?.cancel()
        timerJob = null
        _isCountdownActive.value = false
        _isPlaying.value = false
        updateWakeLockState()
        audioCueManager.stop()
        ambientMusicManager.stop()
    }

    fun skipCountdownAndStart() {
        countdownJob?.cancel()
        countdownJob = null
        _isCountdownActive.value = false
        _isPlaying.value = true
        updateWakeLockState()
        startTimer()
        
        triggerVoiceCueForCurrentPose()
    }

    private fun getCountdownWord(number: Int, lang: String): String {
        return if (lang == "sa") {
            when (number) {
                5 -> "पञ्च" // Pancha
                4 -> "चत्वारि" // Chatvari
                3 -> "त्रीणि" // Trini
                2 -> "द्वे" // Dve
                1 -> "एकम्" // Ekam
                else -> ""
            }
        } else {
            when (number) {
                5 -> "Five"
                4 -> "Four"
                3 -> "Three"
                2 -> "Two"
                1 -> "One"
                else -> ""
            }
        }
    }

    fun clearAllCompletedSessions() {
        viewModelScope.launch {
            repository.clearSessions()
        }
    }

    private fun playTransitBeep() {
        try {
            zenSoundSynthesizer.playWoodTap()
        } catch (e: Exception) {
            Log.e(tag, "Failed to play transit beep: ${e.message}")
        }
    }

    private fun playCompletionBeep() {
        try {
            zenSoundSynthesizer.playWoodTap()
        } catch (e: Exception) {
            Log.e(tag, "Failed to play completion beep: ${e.message}")
        }
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private fun updateWakeLockState() {
        val shouldHold = (_isPlaying.value || _isCountdownActive.value) && _backgroundAudioEnabled.value
        if (shouldHold) {
            if (wakeLock == null) {
                try {
                    val powerManager = getApplication<Application>().getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                    wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "YogaFlow::BackgroundAudioWakeLock").apply {
                        acquire()
                    }
                    Log.d(tag, "Acquired CPU WakeLock for background audio playback.")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to acquire CPU WakeLock: ${e.message}")
                }
            }
        } else {
            releaseWakeLock()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(tag, "Released CPU WakeLock.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing WakeLock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseWakeLock()
        audioCueManager.release()
        countdownJob?.cancel()
        countdownJob = null
        ambientMusicManager.stop()
        zenSoundSynthesizer.release()
    }
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val progressText: String = ""
)
