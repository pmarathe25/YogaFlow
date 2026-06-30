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
    val reminderViewModel = ReminderViewModel(application)
    val sessionViewModel = SessionViewModel(application)

    init {
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
    fun setPreferredVoice(voice: String) {
        settingsViewModel.setPreferredVoice(voice)
        sessionViewModel.setPreferredVoice(voice)
    }
    fun setIsMusicMuted(muted: Boolean) {
        settingsViewModel.setIsMusicMuted(muted)
        sessionViewModel.ambientMusicManager.setMute(muted)
    }
    fun setCurrentTrackIndex(index: Int) {
        settingsViewModel.setCurrentTrackIndex(index)
        sessionViewModel.ambientMusicManager.setCurrentTrackIndex(index)
    }

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

    // Stats delegation
    fun clearAllCompletedSessions() = statsViewModel.clearAllCompletedSessions()
}