package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.ReminderManager
import com.example.db.SettingsManager
import com.example.db.StatsManager
import com.example.db.YogaDatabase
import com.example.db.YogaSessionRepository
import com.example.model.YogaFlow
import com.example.db.Achievement
import com.example.db.ReminderEntity
import kotlinx.coroutines.flow.StateFlow

class YogaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YogaSessionRepository(YogaDatabase.getDatabase(application).yogaSessionDao())
    val allSessions = repository.allSessions

    // Managers
    val settingsManager = SettingsManager(application)
    val statsManager = StatsManager(viewModelScope, repository)
    val reminderManager = ReminderManager(application, viewModelScope, YogaDatabase.getDatabase(application), repository)
    val sessionManager = SessionManager(application, viewModelScope, repository, settingsManager)

    init {
        reminderManager.rescheduleAllReminders()
    }

    // Delegate Stats
    val totalSessions: StateFlow<Int> = statsManager.totalSessions
    val totalXp: StateFlow<Int> = statsManager.totalXp
    val currentLevel: StateFlow<Int> = statsManager.currentLevel
    val currentLevelName: StateFlow<String> = statsManager.currentLevelName
    val levelProgress: StateFlow<Float> = statsManager.levelProgress
    val totalSparks: StateFlow<Int> = statsManager.totalSparks
    val dailyQuestCompleted: StateFlow<Boolean> = statsManager.dailyQuestCompleted
    val achievements: StateFlow<List<Achievement>> = statsManager.achievements

    // Delegate Settings
    val themeMode: StateFlow<String> = settingsManager.themeMode
    val keepScreenAwake: StateFlow<Boolean> = settingsManager.keepScreenAwake
    val backgroundAudioEnabled: StateFlow<Boolean> = settingsManager.backgroundAudioEnabled
    val isVoiceEnabled: StateFlow<Boolean> = sessionManager.isVoiceEnabled
    val preferredVoice: StateFlow<String> = settingsManager.preferredVoice
    val isMusicMuted: StateFlow<Boolean> = settingsManager.isMusicMuted
    val currentTrackIndex: StateFlow<Int> = settingsManager.currentTrackIndex

    // Delegate Reminders
    val allReminders = reminderManager.allReminders
    val favoriteFlowIds = reminderManager.favoriteFlowIds

    // Delegate Session
    val flow = sessionManager.flow
    val currentPoseIndex = sessionManager.currentPoseIndex
    val remainingTimeSec = sessionManager.remainingTimeSec
    val isPlaying = sessionManager.isPlaying
    val isSessionCompleted = sessionManager.isSessionCompleted
    val isCountdownActive = sessionManager.isCountdownActive
    val countdownRemaining = sessionManager.countdownRemaining
    val speechState = sessionManager.speechState
    val currentPose = sessionManager.currentPose
    val ambientMusicManager = sessionManager.ambientMusicManager

    fun setThemeMode(mode: String) = settingsManager.setThemeMode(mode)
    fun setKeepScreenAwake(enabled: Boolean) = settingsManager.setKeepScreenAwake(enabled)
    fun setBackgroundAudioEnabled(enabled: Boolean) = settingsManager.setBackgroundAudioEnabled(enabled)
    fun setPreferredVoice(voice: String) = sessionManager.setPreferredVoice(voice)
    fun setIsMusicMuted(muted: Boolean) = settingsManager.setIsMusicMuted(muted)
    fun setCurrentTrackIndex(index: Int) = settingsManager.setCurrentTrackIndex(index)

    fun selectFlow(yogaFlow: YogaFlow) = sessionManager.selectFlow(yogaFlow)
    fun togglePlay() = sessionManager.togglePlay()
    fun toggleMusicMute() = sessionManager.toggleMusicMute()
    fun selectAmbientTrack(index: Int) = sessionManager.selectAmbientTrack(index)
    fun skipForward() = sessionManager.skipForward()
    fun skipBackward() = sessionManager.skipBackward()
    fun selectPoseDirectly(index: Int) = sessionManager.selectPoseDirectly(index)
    fun toggleVoice(enabled: Boolean) = sessionManager.toggleVoice(enabled)
    fun resetForDashboard() = sessionManager.resetForDashboard()
    fun restartSession() = sessionManager.restartSession()
    fun startCountdown() = sessionManager.startCountdown()
    fun cancelCountdown() = sessionManager.cancelCountdown()
    fun skipCountdownAndStart() = sessionManager.skipCountdownAndStart()
    fun triggerVoiceCueForCurrentPose() = sessionManager.triggerVoiceCueForCurrentPose()

    fun addReminder(reminder: ReminderEntity, onResult: (Boolean) -> Unit = {}) = reminderManager.addReminder(reminder, onResult)
    fun updateReminder(reminder: ReminderEntity) = reminderManager.updateReminder(reminder)
    fun deleteReminder(reminder: ReminderEntity) = reminderManager.deleteReminder(reminder)
    fun toggleFavoriteFlow(flowId: String) = reminderManager.toggleFavoriteFlow(flowId)

    fun clearAllCompletedSessions() = statsManager.clearAllCompletedSessions()

    override fun onCleared() {
        super.onCleared()
        sessionManager.release()
    }
}
