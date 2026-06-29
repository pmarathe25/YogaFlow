package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import com.example.api.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AudioCueManager(private val context: Context) : TextToSpeech.OnInitListener {
    private val tag = "AudioCueManager"

    // Local Android TextToSpeech as robust fallback
    private var localTts: TextToSpeech? = null
    private var isLocalTtsReady = false

    // MediaPlayer for Gemini TTS audio playback
    private var mediaPlayer: MediaPlayer? = null
    private var tempAudioFile: File? = null

    // Job tracking for async Gemini API calls
    private var fetchJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // UI state callbacks
    var onSpeechStateChanged: ((SpeechState) -> Unit)? = null

    sealed class SpeechState {
        object Idle : SpeechState()
        object Loading : SpeechState()
        data class Playing(val source: String) : SpeechState() // "Gemini AI" or "Local Fallback"
        data class Error(val message: String) : SpeechState()
    }

    init {
        // Initialize Android Local TTS fallback
        try {
            localTts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize local TTS: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = localTts?.setLanguage(Locale.Builder().setLanguage("sa").setRegion("IN").build())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(tag, "Local TTS Sanskrit (sa-IN) not supported directly, trying Hindi.")
                val resultHi = localTts?.setLanguage(Locale.Builder().setLanguage("hi").setRegion("IN").build())
                if (resultHi == TextToSpeech.LANG_MISSING_DATA || resultHi == TextToSpeech.LANG_NOT_SUPPORTED) {
                    localTts?.setLanguage(Locale.US)
                }
            }
            isLocalTtsReady = true
            Log.d(tag, "Local TTS initialized successfully.")
        } else {
            Log.e(tag, "Local TTS Initialization failed.")
        }
    }

    /**
     * Speaks the given text using device-native high-quality Text-To-Speech (Gemini removed as requested)
     */
    fun speak(apiKey: String, text: String, preferredVoice: String = "en") {
        // Stop any active playback or fetch job
        stop()

        speakLocalFallback(text, preferredVoice)
    }

    private fun playAudioBase64(base64Data: String) {
        try {
            // Decode the Base64 audio bytes
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)

            // Write to a temporary file
            tempAudioFile = File.createTempFile("gemini_tts_", ".mp3", context.cacheDir).apply {
                deleteOnExit()
                FileOutputStream(this).use { fos ->
                    fos.write(audioBytes)
                }
            }

            // Create and prepare MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(tempAudioFile!!.absolutePath)
                setOnPreparedListener {
                    it.start()
                    onSpeechStateChanged?.invoke(SpeechState.Playing("Gemini AI Voice"))
                }
                setOnCompletionListener {
                    onSpeechStateChanged?.invoke(SpeechState.Idle)
                    cleanupTempFile()
                    it.release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(tag, "MediaPlayer error: what=$what, extra=$extra")
                    onSpeechStateChanged?.invoke(SpeechState.Error("MediaPlayer Error code $what"))
                    cleanupTempFile()
                    false
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            Log.e(tag, "Failed to decode/play Gemini audio: ${e.message}", e)
            onSpeechStateChanged?.invoke(SpeechState.Error("Audio Playback Failed"))
        }
    }

    private fun speakLocalFallback(text: String, voiceLang: String) {
        Log.d(tag, "Speaking with Local TTS fallback. Option: $voiceLang")
        if (isLocalTtsReady && localTts != null) {
            val locale = if (voiceLang == "sa") {
                Locale.Builder().setLanguage("sa").setRegion("IN").build()
            } else {
                Locale.US
            }
            try {
                val result = localTts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    if (voiceLang == "sa") {
                        Log.d(tag, "Sanskrit not supported directly, falling back to Hindi locale.")
                        val resHi = localTts?.setLanguage(Locale.Builder().setLanguage("hi").setRegion("IN").build())
                        if (resHi == TextToSpeech.LANG_MISSING_DATA || resHi == TextToSpeech.LANG_NOT_SUPPORTED) {
                            localTts?.setLanguage(Locale.US)
                        } else {
                            localTts?.voices?.forEach { voice ->
                                if (voice.locale.language == "hi" && voice.locale.country == "IN") {
                                    localTts?.voice = voice
                                }
                            }
                        }
                    } else {
                        localTts?.setLanguage(Locale.US)
                    }
                } else {
                    localTts?.voices?.forEach { voice ->
                        if (voiceLang == "sa" && (voice.locale.language == "sa" || (voice.locale.language == "hi" && voice.locale.country == "IN"))) {
                            localTts?.voice = voice
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error setting dynamic language: ${e.message}")
            }

            val accentLabel = when (voiceLang) {
                "sa" -> "Sanskrit Voice (sa-IN/hi-IN)"
                else -> "English Voice (Default)"
            }
            onSpeechStateChanged?.invoke(SpeechState.Playing("Local Device Voice ($accentLabel)"))
            
            // Keep original pitch and speech rate
            localTts?.setPitch(1.0f)
            localTts?.setSpeechRate(1.0f)
            
            localTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "yoga_fallback_utterance")
            // Periodically check local TTS active status to emit Idle state
            coroutineScope.launch {
                delay(200)
                while (localTts?.isSpeaking == true) {
                    delay(200)
                }
                onSpeechStateChanged?.invoke(SpeechState.Idle)
            }
        } else {
            onSpeechStateChanged?.invoke(SpeechState.Error("Local Voice Engine Uninitialized"))
            Log.e(tag, "Local TTS is not initialized or available.")
        }
    }

    /**
     * Instantly stops any spoken audio or requests
     */
    fun stop() {
        fetchJob?.cancel()
        fetchJob = null

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(tag, "Error stopping MediaPlayer: ${e.message}")
        }

        try {
            if (localTts?.isSpeaking == true) {
                localTts?.stop()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping Local TTS: ${e.message}")
        }

        cleanupTempFile()
        onSpeechStateChanged?.invoke(SpeechState.Idle)
    }

    private fun cleanupTempFile() {
        try {
            tempAudioFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
            tempAudioFile = null
        } catch (e: Exception) {
            Log.e(tag, "Error deleting temp file: ${e.message}")
        }
    }

    fun release() {
        stop()
        coroutineScope.cancel()
        try {
            localTts?.shutdown()
            localTts = null
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down Local TTS: ${e.message}")
        }
    }
}

object ZenSoundSynthesizer {
    fun playBell() {
        Thread {
            val sampleRate = 22050
            val durationSec = 2.5
            val totalSamples = (sampleRate * durationSec).toInt()
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioTrack = try {
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize.coerceAtLeast(totalSamples * 2),
                    AudioTrack.MODE_STATIC
                )
            } catch (e: Exception) {
                null
            } ?: return@Thread

            val buffer = ShortArray(totalSamples)
            val f0 = 440.0 // Fundamental frequency (A4)
            val harmonics = doubleArrayOf(1.0, 1.5, 1.9, 2.2, 3.0)
            val amplitudes = doubleArrayOf(0.5, 0.25, 0.15, 0.08, 0.05)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val decay = kotlin.math.exp(-2.0 * t)
                
                var sampleVal = 0.0
                for (h in harmonics.indices) {
                    val freq = f0 * harmonics[h]
                    val vibrato = 1.0 + 0.005 * kotlin.math.sin(2.0 * Math.PI * 4.0 * t)
                    sampleVal += amplitudes[h] * kotlin.math.sin(2.0 * Math.PI * freq * vibrato * t)
                }
                
                val finalVal = (sampleVal * decay * 14000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = finalVal.toInt().toShort()
            }

            try {
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep((durationSec * 1000).toLong())
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                try { audioTrack.release() } catch (ex: Exception) {}
            }
        }.start()
    }

    fun playWoodTap() {
        Thread {
            val sampleRate = 22050
            val durationSec = 0.2 // slightly longer for the resonance
            val totalSamples = (sampleRate * durationSec).toInt()
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioTrack = try {
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize.coerceAtLeast(totalSamples * 2),
                    AudioTrack.MODE_STATIC
                )
            } catch (e: Exception) {
                null
            } ?: return@Thread

            val buffer = ShortArray(totalSamples)
            val f0 = 350.0 // Pitch around 350 Hz for a deep, soft wooden sound
            val harmonics = doubleArrayOf(1.0, 2.7, 5.4) // Marimba-like inharmonics
            val amplitudes = doubleArrayOf(0.8, 0.3, 0.1)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val decay = kotlin.math.exp(-15.0 * t) // Slightly softer decay for resonance
                
                var sampleVal = 0.0
                for (h in harmonics.indices) {
                    val freq = f0 * harmonics[h]
                    sampleVal += amplitudes[h] * kotlin.math.sin(2.0 * Math.PI * freq * t)
                }
                
                val finalVal = (sampleVal * decay * 14000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = finalVal.toInt().toShort()
            }

            try {
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep((durationSec * 1000).toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                try { audioTrack.release() } catch (ex: Exception) {}
            }
        }.start()
    }
}
