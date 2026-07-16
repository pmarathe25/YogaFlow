package com.example.db

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(private val context: Context) {

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
        _themeMode.value = getThemeMode(context)
        _keepScreenAwake.value = getKeepScreenAwake(context)
        _backgroundAudioEnabled.value = getBackgroundAudioEnabled(context)
        _preferredVoice.value = getPreferredVoice(context)
        _isMusicMuted.value = getIsMusicMuted(context)
        _currentTrackIndex.value = getCurrentTrackIndex(context)
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        saveString(context, "theme_mode", mode)
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        _keepScreenAwake.value = enabled
        saveBoolean(context, "keep_screen_awake", enabled)
    }

    fun setBackgroundAudioEnabled(enabled: Boolean) {
        _backgroundAudioEnabled.value = enabled
        saveBoolean(context, "background_audio_enabled", enabled)
    }

    fun setIsMusicMuted(muted: Boolean) {
        _isMusicMuted.value = muted
        saveBoolean(context, "is_music_muted", muted)
    }

    fun setCurrentTrackIndex(index: Int) {
        _currentTrackIndex.value = index
        saveInt(context, "current_track_index", index)
    }

    fun setIsVoiceEnabled(enabled: Boolean) {
        saveBoolean(context, "is_voice_enabled", enabled)
    }

    fun getIsVoiceEnabled(): Boolean = getIsVoiceEnabled(context)

    fun setPreferredVoice(voice: String) {
        _preferredVoice.value = voice
        saveString(context, "preferred_voice", voice)
    }

    fun getFlowLoopCount(flowId: String): Int {
        return getFlowLoopCount(context, flowId)
    }

    fun setFlowLoopCount(flowId: String, count: Int) {
        setFlowLoopCount(context, flowId, count)
    }

    companion object {
        private const val PREFS_NAME = "yoga_settings"

        private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun getThemeMode(context: Context): String = getPrefs(context).getString("theme_mode", "System") ?: "System"
        fun getKeepScreenAwake(context: Context): Boolean = getPrefs(context).getBoolean("keep_screen_awake", true)
        fun getBackgroundAudioEnabled(context: Context): Boolean = getPrefs(context).getBoolean("background_audio_enabled", true)
        fun getIsMusicMuted(context: Context): Boolean = getPrefs(context).getBoolean("is_music_muted", false)
        fun getCurrentTrackIndex(context: Context): Int = getPrefs(context).getInt("current_track_index", 0)
        fun getIsVoiceEnabled(context: Context): Boolean = getPrefs(context).getBoolean("is_voice_enabled", true)
        fun getPreferredVoice(context: Context): String = getPrefs(context).getString("preferred_voice", "en") ?: "en"

        fun saveString(context: Context, key: String, value: String) = getPrefs(context).edit().putString(key, value).apply()
        fun saveBoolean(context: Context, key: String, value: Boolean) = getPrefs(context).edit().putBoolean(key, value).apply()
        fun saveInt(context: Context, key: String, value: Int) = getPrefs(context).edit().putInt(key, value).apply()

        fun getFlowLoopCount(context: Context, flowId: String): Int =
            getPrefs(context).getInt("flow_loop_$flowId", 1)

        fun setFlowLoopCount(context: Context, flowId: String, count: Int) =
            getPrefs(context).edit().putInt("flow_loop_$flowId", count).apply()
    }
}
