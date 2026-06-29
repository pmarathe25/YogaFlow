package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.YogaDatabase
import com.example.db.YogaSessionRepository
import com.example.model.YogaFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class YogaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = YogaDatabase.getDatabase(application)
    private val repository = YogaSessionRepository(database.yogaSessionDao())
    val allSessions = repository.allSessions

    // Sub-ViewModels
    val statsViewModel = StatsViewModel(application)
    val settingsViewModel = SettingsViewModel(application)
    val rpgViewModel = RpgViewModel(application)
    val reminderViewModel = ReminderViewModel(application)
    val sessionViewModel = SessionViewModel(application)

    init {
        // Wire up RPG ViewModel to get XP/Sparks from Stats
        rpgViewModel.totalXpProvider = { statsViewModel.totalXp.value }
        rpgViewModel.totalSparksProvider = { statsViewModel.totalSparks.value }
        
        // Reschedule reminders on startup
        reminderViewModel.rescheduleAllReminders()
    }

    // Delegate Stats
    val totalSessions: StateFlow<Int> = statsViewModel.totalSessions
    val totalXp: StateFlow<Int> = statsViewModel.totalXp
    val currentLevel: StateFlow<Int> = statsViewModel.currentLevel
    val currentLevelName: StateFlow<String> = statsViewModel.currentLevelName
    val levelProgress: StateFlow<Float> = statsViewModel.levelProgress
    val totalSparks: StateFlow<Int> = statsViewModel.totalSparks
    val dailyQuestCompleted: StateFlow<Boolean> = statsViewModel.dailyQuestCompleted
    val achievements: StateFlow<List<Achievement>> = statsViewModel.achievements

    // Delegate Settings
    val themeMode: StateFlow<String> = settingsViewModel.themeMode
    val keepScreenAwake: StateFlow<Boolean> = settingsViewModel.keepScreenAwake
    val backgroundAudioEnabled: StateFlow<Boolean> = settingsViewModel.backgroundAudioEnabled
    val isVoiceEnabled: StateFlow<Boolean> = sessionViewModel.isVoiceEnabled
    val preferredVoice: StateFlow<String> = settingsViewModel.preferredVoice
    val isMusicMuted: StateFlow<Boolean> = settingsViewModel.isMusicMuted
    val currentTrackIndex: StateFlow<Int> = settingsViewModel.currentTrackIndex

    // Delegate RPG
    val gardenItems = rpgViewModel.gardenItems
    val availableXp: StateFlow<Int> = rpgViewModel.availableXp
    val availableSparks: StateFlow<Int> = rpgViewModel.availableSparks
    val rpgUnlockedHeroes: StateFlow<Set<String>> = rpgViewModel.rpgUnlockedHeroes
    val rpgActiveParty: StateFlow<List<String>> = rpgViewModel.rpgActiveParty
    val rpgSpentKarmaXp: StateFlow<Int> = rpgViewModel.rpgSpentKarmaXp
    val rpgSpentSparks: StateFlow<Int> = rpgViewModel.rpgSpentSparks
    val rpgExtraKarmaXp: StateFlow<Int> = rpgViewModel.rpgExtraKarmaXp
    val rpgExtraSparks: StateFlow<Int> = rpgViewModel.rpgExtraSparks
    val rpgStats: StateFlow<Pair<Int, Int>> = rpgViewModel.rpgStats
    val rpgHeroLevels: StateFlow<Map<String, Int>> = rpgViewModel.rpgHeroLevels

    // Delegate Reminders
    val allReminders = reminderViewModel.allReminders
    val favoriteFlowIds = reminderViewModel.favoriteFlowIds

    // Delegate Session
    val flow = sessionViewModel.flow
    val currentPoseIndex = sessionViewModel.currentPoseIndex
    val remainingTimeSec = sessionViewModel.remainingTimeSec
    val isPlaying = sessionViewModel.isPlaying
    val preferredVoiceSession = sessionViewModel.preferredVoice
    val isSessionCompleted = sessionViewModel.isSessionCompleted
    val isCountdownActive = sessionViewModel.isCountdownActive
    val countdownRemaining = sessionViewModel.countdownRemaining
    val speechState = sessionViewModel.speechState
    val currentPose = sessionViewModel.currentPose
    val ambientMusicManager = sessionViewModel.ambientMusicManager

    // Settings delegation methods
    fun setThemeMode(mode: String) = settingsViewModel.setThemeMode(mode)
    fun setKeepScreenAwake(enabled: Boolean) = settingsViewModel.setKeepScreenAwake(enabled)
    fun setBackgroundAudioEnabled(enabled: Boolean) = settingsViewModel.setBackgroundAudioEnabled(enabled)
    fun setIsVoiceEnabled(enabled: Boolean) = sessionViewModel.toggleVoice(enabled)
    fun setPreferredVoice(voice: String) = settingsViewModel.setPreferredVoice(voice)
    fun setIsMusicMuted(muted: Boolean) = settingsViewModel.setIsMusicMuted(muted)
    fun setCurrentTrackIndex(index: Int) = settingsViewModel.setCurrentTrackIndex(index)

    // Session delegation methods
    fun selectFlow(yogaFlow: com.example.model.YogaFlow) = sessionViewModel.selectFlow(yogaFlow)
    fun togglePlay() = sessionViewModel.togglePlay()
    fun toggleMusicMute() = sessionViewModel.toggleMusicMute()
    fun selectAmbientTrack(index: Int) = sessionViewModel.selectAmbientTrack(index)
    fun skipForward() = sessionViewModel.skipForward()
    fun skipBackward() = sessionViewModel.skipBackward()
    fun selectPoseDirectly(index: Int) = sessionViewModel.selectPoseDirectly(index)
    fun toggleVoice(enabled: Boolean) = sessionViewModel.toggleVoice(enabled)
    fun setPreferredVoiceSession(voice: String) = sessionViewModel.setPreferredVoice(voice)
    fun resetForDashboard() = sessionViewModel.resetForDashboard()
    fun restartSession() = sessionViewModel.restartSession()
    fun startCountdown() = sessionViewModel.startCountdown()
    fun cancelCountdown() = sessionViewModel.cancelCountdown()
    fun skipCountdownAndStart() = sessionViewModel.skipCountdownAndStart()
    fun triggerVoiceCueForCurrentPose() = sessionViewModel.triggerVoiceCueForCurrentPose()

    // Reminder delegation methods
    fun addReminder(reminder: com.example.db.ReminderEntity, onResult: (Boolean) -> Unit = {}) = reminderViewModel.addReminder(reminder, onResult)
    fun updateReminder(reminder: com.example.db.ReminderEntity) = reminderViewModel.updateReminder(reminder)
    fun deleteReminder(reminder: com.example.db.ReminderEntity) = reminderViewModel.deleteReminder(reminder)
    fun toggleFavoriteFlow(flowId: String) = reminderViewModel.toggleFavoriteFlow(flowId)

    // RPG delegation methods
    fun loadRpgData() = rpgViewModel.loadRpgData()
    fun buyGardenItem(type: String, x: Float, y: Float, onResult: (Boolean) -> Unit) = rpgViewModel.buyGardenItem(type, x, y, onResult)
    fun upgradeZone(zoneId: String, newLevel: Int, onResult: (Boolean) -> Unit) = rpgViewModel.upgradeZone(zoneId, newLevel, onResult)
    fun removeGardenItem(item: com.example.db.GardenItemEntity) = rpgViewModel.removeGardenItem(item)
    fun rpgUnlockHero(heroId: String, costSparks: Int, onResult: (Boolean) -> Unit) = rpgViewModel.rpgUnlockHero(heroId, costSparks, onResult)
    fun rpgLevelUpHero(heroId: String, costXp: Int, onResult: (Boolean) -> Unit) = rpgViewModel.rpgLevelUpHero(heroId, costXp, onResult)
    fun rpgTogglePartyMember(heroId: String) = rpgViewModel.rpgTogglePartyMember(heroId)
    fun rpgRecordBattleResult(won: Boolean, xpEarned: Int, sparksEarned: Int) = rpgViewModel.rpgRecordBattleResult(won, xpEarned, sparksEarned)

    // Stats delegation
    fun clearAllCompletedSessions() = statsViewModel.clearAllCompletedSessions()
}