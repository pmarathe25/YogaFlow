package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.R
import kotlinx.coroutines.*

class AmbientMusicManager(private val context: Context) {
    private val tag = "AmbientMusicManager"
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false
    private var currentTrackIndex = 0
    private var fadeJob: Job? = null
    private var loopMonitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

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
        if (index >= 0 && index < tracks.size) {
            currentTrackIndex = index
        }
    }

    fun playTrack(index: Int) {
        if (index < 0 || index >= tracks.size) return
        currentTrackIndex = index
        stop()

        try {
            mediaPlayer = MediaPlayer.create(context, tracks[currentTrackIndex].resId).apply {
                setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                isLooping = false // We'll handle looping manually for the crossfade
                setVolume(0f, 0f)
                start()
            }
            fadeIn()
            startLoopMonitor()
        } catch (e: Exception) {
            Log.e(tag, "Failed to start MediaPlayer: ${e.message}")
        }
    }

    private fun startLoopMonitor() {
        loopMonitorJob?.cancel()
        loopMonitorJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer ?: break
                if (player.isPlaying) {
                    val duration = player.duration
                    val position = player.currentPosition
                    val fadeDurationMs = 2000 // 2 seconds fade out

                    // If we're within the fade-out window at the end of the track
                    if (duration > 0 && duration - position <= fadeDurationMs && fadeJob?.isActive != true) {
                        fadeOut {
                            player.seekTo(0)
                            player.start()
                            fadeIn()
                        }
                    }
                }
                delay(200)
            }
        }
    }

    private fun fadeIn() {
        fadeJob?.cancel()
        if (isMuted) return
        fadeJob = scope.launch {
            var vol = 0f
            while (vol < 0.35f) {
                vol += 0.05f
                if (vol > 0.35f) vol = 0.35f
                mediaPlayer?.setVolume(vol, vol)
                delay(100)
            }
        }
    }

    private fun fadeOut(onComplete: () -> Unit) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            var vol = 0.35f
            while (vol > 0f) {
                vol -= 0.05f
                if (vol < 0f) vol = 0f
                mediaPlayer?.setVolume(vol, vol)
                delay(100)
            }
            onComplete()
        }
    }

    fun play() {
        if (mediaPlayer == null) {
            playTrack(currentTrackIndex)
        } else {
            mediaPlayer?.start()
            fadeIn()
            startLoopMonitor()
        }
    }

    fun pause() {
        loopMonitorJob?.cancel()
        try {
            fadeOut { mediaPlayer?.pause() }
        } catch (e: Exception) {
            Log.e(tag, "Error pausing background music: ${e.message}")
        }
    }

    fun stop() {
        fadeJob?.cancel()
        loopMonitorJob?.cancel()
        try {
            mediaPlayer?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping background music: ${e.message}")
        }
        mediaPlayer = null
    }

    fun setMute(mute: Boolean) {
        isMuted = mute
        if (isMuted) {
            mediaPlayer?.setVolume(0f, 0f)
        } else {
            fadeIn()
        }
    }

    fun toggleMute(): Boolean {
        setMute(!isMuted)
        return isMuted
    }

    fun isMuted() = isMuted

    fun getCurrentTrackName() = tracks[currentTrackIndex].name
    fun getCurrentTrackIndex() = currentTrackIndex
}
