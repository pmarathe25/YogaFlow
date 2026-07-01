package com.example.viewmodel

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.example.audio.AmbientMusicManager
import com.example.audio.AudioCueManager
import com.example.audio.ZenSoundSynthesizer
import com.example.db.SettingsManager
import com.example.db.YogaDatabase
import com.example.db.YogaSession
import com.example.db.YogaSessionRepository
import com.example.model.FlowLoader
import com.example.model.YogaFlow
import com.example.model.YogaPose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SessionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: YogaSessionRepository,
    private val settingsManager: SettingsManager
) {
    private val tag = "SessionManager"
    
    val audioCueManager = AudioCueManager(context)
    private val zenSoundSynthesizer = ZenSoundSynthesizer(context)
    val ambientMusicManager = AmbientMusicManager(context)

    private val _flow = MutableStateFlow<YogaFlow>(
        FlowLoader.getFlowById(context, "sun_salutation") 
        ?: FlowLoader.loadFlows(context).firstOrNull() 
        ?: YogaFlow(id="empty", name="Empty", description="", difficulty="", totalDurationMinutes=0, poses=emptyList())
    )
    val flow: StateFlow<YogaFlow> = _flow.asStateFlow()

    private val _currentPoseIndex = MutableStateFlow(0)
    val currentPoseIndex: StateFlow<Int> = _currentPoseIndex.asStateFlow()

    private val _remainingTimeSec = MutableStateFlow(30)
    val remainingTimeSec: StateFlow<Int> = _remainingTimeSec.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _preferredVoice = MutableStateFlow("en")
    val preferredVoice: StateFlow<String> = _preferredVoice.asStateFlow()

    private val _isSessionCompleted = MutableStateFlow(false)
    val isSessionCompleted: StateFlow<Boolean> = _isSessionCompleted.asStateFlow()

    private val _isCountdownActive = MutableStateFlow(false)
    val isCountdownActive: StateFlow<Boolean> = _isCountdownActive.asStateFlow()

    private val _countdownRemaining = MutableStateFlow(3)
    val countdownRemaining: StateFlow<Int> = _countdownRemaining.asStateFlow()

    private val _speechState = MutableStateFlow<AudioCueManager.SpeechState>(AudioCueManager.SpeechState.Idle)
    val speechState: StateFlow<AudioCueManager.SpeechState> = _speechState.asStateFlow()

    private var timerJob: Job? = null
    private var countdownJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    val currentPose: StateFlow<YogaPose?> = combine(_flow, _currentPoseIndex) { flow, index ->
        if (index in flow.poses.indices) flow.poses[index] else null
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        audioCueManager.onSpeechStateChanged = { state ->
            _speechState.value = state
        }

        _isVoiceEnabled.value = settingsManager.getIsVoiceEnabled()
        _preferredVoice.value = settingsManager.preferredVoice.value
        
        val isMuted = settingsManager.isMusicMuted.value
        val trackIdx = settingsManager.currentTrackIndex.value
        ambientMusicManager.setMute(isMuted)
        ambientMusicManager.setCurrentTrackIndex(trackIdx)
    }

    fun selectFlow(yogaFlow: YogaFlow) {
        _flow.value = yogaFlow
        _currentPoseIndex.value = 0
        _remainingTimeSec.value = yogaFlow.poses.firstOrNull()?.holdDurationSec ?: 30
        _isSessionCompleted.value = false
        _isPlaying.value = false
        _isCountdownActive.value = false
        countdownJob?.cancel()
        countdownJob = null
    }

    fun togglePlay() {
        if (_isSessionCompleted.value) {
            restartSession()
            return
        }

        if (_isCountdownActive.value) {
            skipCountdownAndStart()
            return
        }

        if (_isPlaying.value) {
            pauseSession()
        } else {
            resumeSession()
        }
    }

    private fun resumeSession() {
        _isPlaying.value = true
        _isCountdownActive.value = false
        updateWakeLockState()
        startTimer()
        ambientMusicManager.play()
        if (_remainingTimeSec.value == (_flow.value.poses.getOrNull(_currentPoseIndex.value)?.holdDurationSec ?: 30)) {
            triggerVoiceCueForCurrentPose()
        }
    }

    private fun pauseSession() {
        _isPlaying.value = false
        updateWakeLockState()
        timerJob?.cancel()
        timerJob = null
        audioCueManager.stop()
        ambientMusicManager.pause()
    }

    fun toggleMusicMute() {
        val newMuted = !settingsManager.isMusicMuted.value
        ambientMusicManager.setMute(newMuted)
        settingsManager.setIsMusicMuted(newMuted)
    }

    fun selectAmbientTrack(index: Int) {
        if (index >= 0 && index < ambientMusicManager.tracks.size) {
            ambientMusicManager.setCurrentTrackIndex(index)
            settingsManager.setCurrentTrackIndex(index)
            if (_isPlaying.value || _isCountdownActive.value) {
                ambientMusicManager.play()
            }
        }
    }

    fun skipForward() {
        val nextIndex = _currentPoseIndex.value + 1
        if (nextIndex < _flow.value.poses.size) {
            _currentPoseIndex.value = nextIndex
            _remainingTimeSec.value = _flow.value.poses[_currentPoseIndex.value].holdDurationSec
            _isSessionCompleted.value = false
            if (_isPlaying.value) {
                triggerVoiceCueForCurrentPose()
            }
        } else {
            completeSession()
        }
    }

    fun skipBackward() {
        val prevIndex = _currentPoseIndex.value - 1
        if (prevIndex >= 0) {
            _currentPoseIndex.value = prevIndex
            _remainingTimeSec.value = _flow.value.poses[_currentPoseIndex.value].holdDurationSec
            _isSessionCompleted.value = false
            if (_isPlaying.value) {
                triggerVoiceCueForCurrentPose()
            }
        }
    }

    fun selectPoseDirectly(index: Int) {
        if (index in _flow.value.poses.indices) {
            _currentPoseIndex.value = index
            _remainingTimeSec.value = _flow.value.poses[index].holdDurationSec
            _isSessionCompleted.value = false
            if (_isPlaying.value) {
                triggerVoiceCueForCurrentPose()
            }
        }
    }

    fun toggleVoice(enabled: Boolean) {
        _isVoiceEnabled.value = enabled
        settingsManager.setIsVoiceEnabled(enabled)
        if (!enabled) {
            audioCueManager.stop()
        } else if (_isPlaying.value) {
            triggerVoiceCueForCurrentPose()
        }
    }

    fun setPreferredVoice(voice: String) {
        _preferredVoice.value = voice
        settingsManager.setPreferredVoice(voice)
        if (_isPlaying.value) {
            if (_isVoiceEnabled.value) {
                triggerVoiceCueForCurrentPose()
            }
        } else if (_isVoiceEnabled.value) {
            val previewText = when (voice) {
                "sa" -> "स्वस्ति। संस्कृत ध्वनि मार्गदर्शिका।"
                else -> "English voice guide selected."
            }
            audioCueManager.speak(previewText, voice)
        }
    }

    fun resetForDashboard() {
        audioCueManager.stop()
        ambientMusicManager.stop()
        _currentPoseIndex.value = 0
        _remainingTimeSec.value = _flow.value.poses.firstOrNull()?.holdDurationSec ?: 30
        _isSessionCompleted.value = false
        _isPlaying.value = false
        _isCountdownActive.value = false
        updateWakeLockState()
        countdownJob?.cancel()
        countdownJob = null
        timerJob?.cancel()
        timerJob = null
    }

    fun restartSession() {
        audioCueManager.stop()
        ambientMusicManager.stop()
        _currentPoseIndex.value = 0
        _remainingTimeSec.value = _flow.value.poses.firstOrNull()?.holdDurationSec ?: 30
        _isSessionCompleted.value = false
        timerJob?.cancel()
        timerJob = null
        
        startCountdown()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (_remainingTimeSec.value > 1) {
                    _remainingTimeSec.value -= 1
                } else {
                    _remainingTimeSec.value = 0
                    onPoseTimeComplete()
                }
            }
        }
    }

    private fun onPoseTimeComplete() {
        val nextIndex = _currentPoseIndex.value + 1
        if (nextIndex < _flow.value.poses.size) {
            playWoodTap()
            _currentPoseIndex.value = nextIndex
            _remainingTimeSec.value = _flow.value.poses[_currentPoseIndex.value].holdDurationSec
            triggerVoiceCueForCurrentPose()
        } else {
            completeSession()
        }
    }

    private fun completeSession() {
        _isPlaying.value = false
        _isSessionCompleted.value = true
        updateWakeLockState()
        timerJob?.cancel()
        timerJob = null
        playWoodTap()
        ambientMusicManager.stop()

        scope.launch {
            try {
                repository.insertSession(
                    YogaSession(
                        flowId = _flow.value.id,
                        flowName = _flow.value.name,
                        durationMinutes = _flow.value.totalDurationMinutes
                    )
                )
                Log.d(tag, "Successfully logged completed yoga session to database.")
            } catch (e: Exception) {
                Log.e(tag, "Error saving session to Room database: ${e.message}")
            }
        }

        if (_isVoiceEnabled.value) {
            val message = if (_preferredVoice.value == "sa") {
                "अभिनन्दनम्। योगसाधना समाप्ता। ओम् शान्तिः शान्तिः शान्तिः।"
            } else {
                "Congratulations! You have completed your ${flow.value.name} practice. Namaste."
            }
            audioCueManager.speak(message, _preferredVoice.value)
        }
    }

    fun triggerVoiceCueForCurrentPose() {
        if (!_isVoiceEnabled.value) return
        scope.launch {
            var waitTime = 0
            while (currentPose.value == null && waitTime < 2000) {
                delay(100)
                waitTime += 100
            }
            if (!_isPlaying.value && !_isCountdownActive.value) return@launch
            val current = currentPose.value ?: return@launch
            val stepNumber = _currentPoseIndex.value + 1
            val voice = _preferredVoice.value
            
            val text = if (voice == "sa") {
                "सोपानं $stepNumber: ${current.sanskritInstructions}"
            } else {
                "Step $stepNumber: ${current.voicePrompt}"
            }
            audioCueManager.speak(text, voice)
        }
    }

    fun startCountdown() {
        countdownJob?.cancel()
        timerJob?.cancel()
        _isCountdownActive.value = true
        _countdownRemaining.value = 3
        _isPlaying.value = false
        updateWakeLockState()
        
        ambientMusicManager.play()

        countdownJob = scope.launch {
            for (i in 3 downTo 1) {
                _countdownRemaining.value = i
                zenSoundSynthesizer.playWoodTap()
                delay(1000)
            }
            
            _isCountdownActive.value = false
            _isPlaying.value = true
            
            startTimer()
            
            triggerVoiceCueForCurrentPose()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        timerJob?.cancel()
        timerJob = null
        _isCountdownActive.value = false
        _isPlaying.value = false
        updateWakeLockState()
        audioCueManager.stop()
        ambientMusicManager.stop()
    }

    fun skipCountdownAndStart() {
        countdownJob?.cancel()
        countdownJob = null
        _isCountdownActive.value = false
        _isPlaying.value = true
        updateWakeLockState()
        startTimer()
        
        triggerVoiceCueForCurrentPose()
    }



    private fun playWoodTap() {
        try {
            zenSoundSynthesizer.playWoodTap()
        } catch (e: Exception) {
            Log.e(tag, "Failed to play wood tap: ${e.message}")
        }
    }

    private fun updateWakeLockState() {
        val backgroundAudioEnabled = settingsManager.backgroundAudioEnabled.value
        val shouldHold = (_isPlaying.value || _isCountdownActive.value) && backgroundAudioEnabled
        if (shouldHold) {
            if (wakeLock == null) {
                try {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YogaFlow::BackgroundAudioWakeLock").apply {
                        acquire()
                    }
                    Log.d(tag, "Acquired CPU WakeLock for background audio playback.")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to acquire CPU WakeLock: ${e.message}")
                }
            }
        } else {
            releaseWakeLock()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(tag, "Released CPU WakeLock.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing WakeLock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    fun release() {
        releaseWakeLock()
        audioCueManager.release()
        countdownJob?.cancel()
        countdownJob = null
        timerJob?.cancel()
        timerJob = null
        ambientMusicManager.stop()
        zenSoundSynthesizer.release()
    }
}
