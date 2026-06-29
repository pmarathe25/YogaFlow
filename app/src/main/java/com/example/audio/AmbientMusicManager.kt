package com.example.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.model.AudioTrack

class AmbientMusicManager(private val context: Context) {
    val tracks: List<AudioTrack> by lazy { AudioTrack.loadTracks() }

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

    fun getCurrentTrackName(): String = tracks.getOrNull(getCurrentTrackIndexSync())?.name ?: ""
    
    fun getCurrentTrackIndexSync(): Int {
        return context.getSharedPreferences("ambient_music_prefs", Context.MODE_PRIVATE)
            .getInt("current_track_index", 0)
    }
}
