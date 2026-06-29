package com.example.audio

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.R

class AmbientMusicManager(private val context: Context) {
    private val tag = "AmbientMusicManager"

    val tracks: List<Track> by lazy {
        val fields = R.raw::class.java.fields
        fields.filter { it.name.startsWith("track_") }.map { field ->
            val resId = field.getInt(null)
            val nameParts = field.name.removePrefix("track_").split("_")
            val name = nameParts.filter { it.toIntOrNull() == null }
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            val isMusic = !name.contains("Breath", ignoreCase = true) && !name.contains("Ocean", ignoreCase = true)
            Track(name, resId, isMusic)
        }.sortedBy { it.name }
    }

    data class Track(val name: String, val resId: Int, val isMusic: Boolean)

    fun setCurrentTrackIndex(index: Int) {
        val intent = Intent(context, AmbientMusicService::class.java).apply {
            action = AmbientMusicService.ACTION_SET_TRACK
            putExtra(AmbientMusicService.EXTRA_TRACK_INDEX, index)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun play() {
        val intent = Intent(context, AmbientMusicService::class.java).apply {
            action = AmbientMusicService.ACTION_PLAY
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun pause() {
        val intent = Intent(context, AmbientMusicService::class.java).apply {
            action = AmbientMusicService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun stop() {
        val intent = Intent(context, AmbientMusicService::class.java).apply {
            action = AmbientMusicService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun setMute(mute: Boolean) {
        val intent = Intent(context, AmbientMusicService::class.java).apply {
            action = AmbientMusicService.ACTION_SET_MUTE
            putExtra(AmbientMusicService.EXTRA_MUTE, mute)
        }
        context.startService(intent)
    }

    fun toggleMute(): Boolean {
        // We can't synchronously get the current mute state from the service
        // This would need a callback or shared prefs approach
        // For now, just send the toggle - the service will handle it
        // The UI will need to reflect the change via a callback or state flow
        true
    }

    fun isMuted(): Boolean {
        // Would need to read from SharedPreferences or callback
        // For now return false as placeholder
        false
    }

    fun getCurrentTrackName(): String = tracks.getOrNull(getCurrentTrackIndexSync())?.name ?: ""
    
    fun getCurrentTrackIndexSync(): Int {
        // Read from SharedPreferences where the service saves it
        return context.getSharedPreferences("ambient_music_prefs", Context.MODE_PRIVATE)
            .getInt("current_track_index", 0)
    }
}