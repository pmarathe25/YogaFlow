package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.*

class AmbientMusicService : Service() {
    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SET_TRACK = "ACTION_SET_TRACK"
        const val ACTION_SET_MUTE = "ACTION_SET_MUTE"
        
        const val EXTRA_TRACK_INDEX = "EXTRA_TRACK_INDEX"
        const val EXTRA_MUTE = "EXTRA_MUTE"
        
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "ambient_music_channel"
    }

    private val tag = "AmbientMusicService"
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false
    private var currentTrackIndex = 0
    private var fadeJob: Job? = null
    private var loopMonitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val tracks: List<com.example.model.AudioTrack> by lazy { com.example.model.AudioTrack.loadTracks() }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY -> play()
                ACTION_PAUSE -> pause()
                ACTION_STOP -> stopSelf()
                ACTION_SET_TRACK -> {
                    val index = intent.getIntExtra(EXTRA_TRACK_INDEX, 0)
                    setTrack(index)
                }
                ACTION_SET_MUTE -> {
                    val mute = intent.getBooleanExtra(EXTRA_MUTE, false)
                    setMute(mute)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun loadState() {
        val prefs = getSharedPreferences("ambient_music_prefs", Context.MODE_PRIVATE)
        currentTrackIndex = prefs.getInt("current_track_index", 0)
        isMuted = prefs.getBoolean("is_muted", false)
    }

    private fun saveState() {
        val prefs = getSharedPreferences("ambient_music_prefs", Context.MODE_PRIVATE).edit()
        prefs.putInt("current_track_index", currentTrackIndex)
        prefs.putBoolean("is_muted", isMuted)
        prefs.apply()
    }

    private fun play() {
        if (mediaPlayer == null) {
            preparePlayer(currentTrackIndex)
        }
        mediaPlayer?.start()
        if (!isMuted) fadeIn()
        startLoopMonitor()
        updateNotification()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun preparePlayer(index: Int) {
        stopPlayer()
        if (index < 0 || index >= tracks.size) return
        
        currentTrackIndex = index
        saveState()
        
        try {
            mediaPlayer = MediaPlayer.create(this, tracks[currentTrackIndex].resId).apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                isLooping = false
                setVolume(0f, 0f)
                setOnCompletionListener { 
                    // Loop handled by monitor
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to prepare MediaPlayer: ${e.message}")
        }
    }

    private fun setTrack(index: Int) {
        if (index < 0 || index >= tracks.size) return
        
        val wasPlaying = mediaPlayer?.isPlaying == true
        currentTrackIndex = index
        saveState()
        
        if (wasPlaying) {
            preparePlayer(index)
            fadeIn()
            startLoopMonitor()
        }
        updateNotification()
    }

    private fun pause() {
        loopMonitorJob?.cancel()
        fadeOut { 
            mediaPlayer?.pause() 
            stopForeground(false)
            updateNotification()
        }
    }

    private fun stopPlayer() {
        fadeJob?.cancel()
        loopMonitorJob?.cancel()
        try {
            mediaPlayer?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping player: ${e.message}")
        }
        mediaPlayer = null
    }

    private fun setMute(mute: Boolean) {
        isMuted = mute
        saveState()
        if (isMuted) {
            mediaPlayer?.setVolume(0f, 0f)
        } else {
            fadeIn()
        }
        updateNotification()
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

    private fun startLoopMonitor() {
        loopMonitorJob?.cancel()
        loopMonitorJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer ?: break
                if (player.isPlaying) {
                    val duration = player.duration
                    val position = player.currentPosition
                    val fadeDurationMs = 2000
                    
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

    private fun cleanup() {
        stopPlayer()
        scope.cancel()
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ambient Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background ambient music for yoga practice"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val track = tracks.getOrNull(currentTrackIndex)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ambient Music")
            .setContentText(track?.name ?: "Paused")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }
}
