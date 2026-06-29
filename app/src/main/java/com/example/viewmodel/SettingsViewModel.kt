package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _themeMode = MutableStateFlow("System")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _keepScreenAwake = MutableStateFlow(true)
    val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

    private val _backgroundAudioEnabled = MutableStateFlow(true)
    val backgroundAudioEnabled: StateFlow<Boolean> = _backgroundAudioEnabled.asStateFlow()

    private val _preferredVoice = MutableStateFlow("en")
    val preferredVoice: StateFlow<String> = _preferredVoice.asStateFlow()

    private val _isMusicMuted = MutableStateFlow(false)
    val isMusicMuted: StateFlow<Boolean> = _isMusicMuted.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    init {
        val app = getApplication<Application>()
        _themeMode.value = SettingsManager.getThemeMode(app)
        _keepScreenAwake.value = SettingsManager.getKeepScreenAwake(app)
        _backgroundAudioEnabled.value = SettingsManager.getBackgroundAudioEnabled(app)
        _preferredVoice.value = SettingsManager.getPreferredVoice(app)
        _isMusicMuted.value = SettingsManager.getIsMusicMuted(app)
        _currentTrackIndex.value = SettingsManager.getCurrentTrackIndex(app)
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        SettingsManager.saveThemeMode(getApplication(), mode)
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        _keepScreenAwake.value = enabled
        SettingsManager.saveKeepScreenAwake(getApplication(), enabled)
    }

    fun setBackgroundAudioEnabled(enabled: Boolean) {
        _backgroundAudioEnabled.value = enabled
        SettingsManager.saveBackgroundAudioEnabled(getApplication(), enabled)
    }

    fun setPreferredVoice(voice: String) {
        _preferredVoice.value = voice
        SettingsManager.savePreferredVoice(getApplication(), voice)
    }

    fun setIsMusicMuted(muted: Boolean) {
        _isMusicMuted.value = muted
        SettingsManager.saveIsMusicMuted(getApplication(), muted)
    }

    fun setCurrentTrackIndex(index: Int) {
        _currentTrackIndex.value = index
        SettingsManager.saveCurrentTrackIndex(getApplication(), index)
    }
}