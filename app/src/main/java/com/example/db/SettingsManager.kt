package com.example.db

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "app_settings_backup"

    fun saveThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("themeMode", mode).apply()
    }
    fun getThemeMode(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("themeMode", "System") ?: "System"

    fun saveKeepScreenAwake(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("keepScreenAwake", enabled).apply()
    }
    fun getKeepScreenAwake(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("keepScreenAwake", true)

    fun saveBackgroundAudioEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("backgroundAudioEnabled", enabled).apply()
    }
    fun getBackgroundAudioEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("backgroundAudioEnabled", true)

    fun saveIsMusicMuted(context: Context, muted: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("isMusicMuted", muted).apply()
    }
    fun getIsMusicMuted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("isMusicMuted", false)

    fun saveCurrentTrackIndex(context: Context, index: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt("currentTrackIndex", index).apply()
    }
    fun getCurrentTrackIndex(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt("currentTrackIndex", 0)

    fun saveIsVoiceEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("isVoiceEnabled", enabled).apply()
    }
    fun getIsVoiceEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("isVoiceEnabled", true)

    fun savePreferredVoice(context: Context, voice: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("preferredVoice", voice).apply()
    }
    fun getPreferredVoice(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("preferredVoice", "en") ?: "en"
}
