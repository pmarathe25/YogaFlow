package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioCueManager
import com.example.audio.ZenSoundSynthesizer
import com.example.db.YogaDatabase
import com.example.db.YogaSessionRepository
import com.example.model.FlowLoader
import com.example.model.YogaFlow
import com.example.model.YogaPose
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "SessionViewModel"
    
    private val database = YogaDatabase.getDatabase(application)
    private val repository = YogaSessionRepository(database.yogaSessionDao())

    // Audio managers
    val audioCueManager = AudioCueManager(application)
    private val zenSoundSynthesizer = ZenSoundSynthesizer(application)
    val ambientMusicManager = com.example.audio.AmbientMusicManager(application)

    // Flow state - load default flow from FlowLoader
    private val _flow = MutableStateFlow<YogaFlow>(FlowLoader.getFlowById(application, "sun_salutation") 
        ?: FlowLoader.loadFlows(application).firstOrNull() 
        ?: YogaFlow(id="empty", name="Empty", description="", difficulty="", totalDurationMinutes=0, poses=emptyList()))
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
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    val currentPose: StateFlow<YogaPose?> = combine(_flow, _currentPoseIndex) { flow, index ->
        if (index in flow.poses.indices) flow.poses[index] else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        audioCueManager.onSpeechStateChanged = { state ->
            _speechState.value = state
        }

        val app = getApplication<Application>()
        _isVoiceEnabled.value = com.example.db.SettingsManager.getIsVoiceEnabled(app)
        _preferredVoice.value = com.example.db.SettingsManager.getPreferredVoice(app)
        
        val isMuted = com.example.db.SettingsManager.getIsMusicMuted(app)
        val trackIdx = com.example.db.SettingsManager.getCurrentTrackIndex(app)
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
        ambientMusicManager.setMute(!com.example.db.SettingsManager.getIsMusicMuted(getApplication()))
        val muted = com.example.db.SettingsManager.getIsMusicMuted(getApplication())
        com.example.db.SettingsManager.saveIsMusicMuted(getApplication(), muted)
    }

    fun selectAmbientTrack(index: Int) {
        if (index >= 0 && index < ambientMusicManager.tracks.size) {
            _currentTrackIndex.value = index
            ambientMusicManager.setCurrentTrackIndex(index)
            com.example.db.SettingsManager.saveCurrentTrackIndex(getApplication(), index)
            if (_isPlaying.value || _isCountdownActive.value) {
                ambientMusicManager.play()
            }
        }
    }

    private var _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

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
        com.example.db.SettingsManager.saveIsVoiceEnabled(getApplication(), enabled)
        if (!enabled) {
            audioCueManager.stop()
        } else if (_isPlaying.value) {
            triggerVoiceCueForCurrentPose()
        }
    }

    fun setPreferredVoice(voice: String) {
        _preferredVoice.value = voice
        com.example.db.SettingsManager.savePreferredVoice(getApplication(), voice)
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
        timerJob = viewModelScope.launch {
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

        viewModelScope.launch {
            try {
                repository.insertSession(
                    com.example.db.YogaSession(
                        flowId = _flow.value.id,
                        flowName = _flow.value.name,
                        durationMinutes = _flow.value.totalDurationMinutes
                    )
                )
                android.util.Log.d(tag, "Successfully logged completed yoga session to database.")
            } catch (e: Exception) {
                android.util.Log.e(tag, "Error saving session to Room database: ${e.message}")
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
        viewModelScope.launch {
            var waitTime = 0
            while (currentPose.value == null && waitTime < 2000) {
                delay(100)
                waitTime += 100
            }
            if (!_isPlaying.value && !_isCountdownActive.value) return@launch
            val current = currentPose.value ?: return@launch
            var text = if (_preferredVoice.value == "sa") {
                FlowLoader.getSanskritPrompt(getApplication<Application>(), current.id)
                    ?: getSanskritPromptFallback(current.id)
            } else {
                current.voicePrompt
            }
            text = text.replace(Regex("^.*? (Step \\d+:)"), "$1")
            audioCueManager.speak(text, _preferredVoice.value)
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

        countdownJob = viewModelScope.launch {
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

    private fun getSanskritPromptFallback(poseId: Int): String {
        return when (poseId) {
            1 -> "प्रणामासनं कुर्यात्। करसम्पुटं हृदयस्थाने धारयन्तु। दीर्घं निःश्वस्य शान्तं भवन्तु।"
            2 -> "हस्तौत्तानासनं कुर्यात्। उद्गस्य हस्तौ ऊर्ध्वं प्रयच्छन्तु। सर्वशरीरं दीर्घतानन्तु।"
            3 -> "उत्तानासनं कुर्यात्। अधोगत्वा पार्श्वपादौ स्पृशन्तु। शिरोग्रीवं शिथिलीकुर्यात्।"
            4 -> "अश्वसञ्चालनासनं कुर्यात्। दक्षिणपादं पृष्ठे स्थापयन्तु। वक्षःप्रसास्य अग्रे पश्यन्तु।"
            5 -> "फलकासनं कुर्यात्। वामपादं पृष्ठे स्थापयन्तु। शरीरं सरलरेखा-रूपेण धारयन्तु।"
            6 -> "अष्टाङ्गनमस्कारं कुर्यात्। जानु-वक्षः-हनु भूमौ स्थापयन्तु। नितम्बं कोष्ठोन्नतं धारयन्तु।"
            7 -> "भुजङ्गासनं कुर्यात्। अग्रे सर्प-रूपेण शरीरं ऊर्ध्वं उत्थापयन्तु। वक्षःप्रसास्य हृदयं उद्भासन्तु।"
            8 -> "अधोमुखश्वानासनं कुर्यात्। नितम्बं ऊर्ध्वं प्रयच्छन्तु। सर्वशरीरं पर्वताकारं धारयन्तु।"
            9 -> "अश्वसञ्चालनासनं कुर्यात्। दक्षिणपादं अग्रे स्थापयन्तु। अग्रे पश्यन्तु।"
            10 -> "उत्तानासनं कुर्यात्। वामपादं अग्रे स्थापयन्तु, दीर्घं नमन्तु।"
            11 -> "हस्तौत्तानासनं कुर्यात्। उद्गस्य हस्तौ ऊर्ध्वं प्रयच्छन्तु, वक्षः उद्भासन्तु।"
            12 -> "प्रणामासनं कुर्यात्। निःश्वस्य हस्तौ हृदयस्थाने स्थापयन्तु। ओम् शान्तिः शान्तिः शान्तिः।"
            
            201 -> "ताडासनं कुर्यात्। समस्थिति। पादौ भूमौ धारयन्तु। स्वास्थ्य स्थितौ भवन्तु।"
            202 -> "वीरभद्रासनं प्रथमं कुर्यात्। वामपादं पृष्ठे धारयन्तु, हस्तौ ऊर्ध्वं उत्थापयन्तु।"
            203 -> "वीरभद्रासनं द्वितीयं कुर्यात्। हस्तौ पार्श्वे प्रसार्यन्तु, अग्रे पश्यन्तु।"
            204 -> "त्रिकोणासनं कुर्यात्। पार्श्वभङ्गेन हस्त-शिखरे पश्यन्तु।"
            205 -> "बालासनं कुर्यात्। नितम्बौ पादपृष्ठे स्थापयन्तु, शिरो भूमौ नमन्तु। शान्तं भवन्तु।"
            
            301 -> "सालम्बभुजङ्गासनं कुर्यात्। कूर्परौ भूमौ धारयन्तु, ऊर्ध्वं पश्यन्तु।"
            302 -> "बद्धकोणासनं कुर्यात्। पादतलौ परस्परं संयोजयन्तु।"
            303 -> "सुप्तमत्स्येन्द्रासनं कुर्यात्। पार्श्वपरिवर्तनेन ध्यानं कुर्यात्।"
            304 -> "शवासनं कुर्यात्। सर्वशरीरं शिथिलीकुर्यात्, ध्यानमयस्थितौ विश्रामन्तु। ओम्, शान्तिः, शान्तिः, शान्तिः।"
            else -> "ध्यानं कुर्यात्। शान्तं ध्यानं।"
        }
    }

    private fun playWoodTap() {
        try {
            zenSoundSynthesizer.playWoodTap()
        } catch (e: Exception) {
            android.util.Log.e(tag, "Failed to play wood tap: ${e.message}")
        }
    }

    private fun updateWakeLockState() {
        val backgroundAudioEnabled = com.example.db.SettingsManager.getBackgroundAudioEnabled(getApplication())
        val shouldHold = (_isPlaying.value || _isCountdownActive.value) && backgroundAudioEnabled
        if (shouldHold) {
            if (wakeLock == null) {
                try {
                    val powerManager = getApplication<Application>().getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                    wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "YogaFlow::BackgroundAudioWakeLock").apply {
                        acquire()
                    }
                    android.util.Log.d(tag, "Acquired CPU WakeLock for background audio playback.")
                } catch (e: Exception) {
                    android.util.Log.e(tag, "Failed to acquire CPU WakeLock: ${e.message}")
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
                android.util.Log.d(tag, "Released CPU WakeLock.")
            }
        } catch (e: Exception) {
            android.util.Log.e(tag, "Error releasing WakeLock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseWakeLock()
        audioCueManager.release()
        countdownJob?.cancel()
        countdownJob = null
        ambientMusicManager.stop()
        zenSoundSynthesizer.release()
    }
}