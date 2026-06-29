package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.*
import java.util.Locale

class AudioCueManager(private val context: Context) : TextToSpeech.OnInitListener {
    private val tag = "AudioCueManager"

    private var localTts: TextToSpeech? = null
    private var isLocalTtsReady = false

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var onSpeechStateChanged: ((SpeechState) -> Unit)? = null

    sealed class SpeechState {
        object Idle : SpeechState()
        object Loading : SpeechState()
        data class Playing(val source: String) : SpeechState()
        data class Error(val message: String) : SpeechState()
    }

    init {
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

    fun speak(apiKey: String, text: String, preferredVoice: String = "en") {
        stop()
        speakLocalFallback(text, preferredVoice)
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

            localTts?.setPitch(1.0f)
            localTts?.setSpeechRate(1.0f)

            localTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "yoga_fallback_utterance")
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

    fun stop() {
        try {
            if (localTts?.isSpeaking == true) {
                localTts?.stop()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping Local TTS: ${e.message}")
        }
        onSpeechStateChanged?.invoke(SpeechState.Idle)
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
