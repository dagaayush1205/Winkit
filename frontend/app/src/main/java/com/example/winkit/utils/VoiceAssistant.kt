package com.example.winkit.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

class VoiceAssistant(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    var isReady = false
        private set

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("WinkIT_Voice", "Language pack missing")
            } else {
                isReady = true
                Log.d("WinkIT_Voice", "JARVIS is online.")
            }
        }
    }

    // A universal speak function! 
    fun speak(message: String, isHindi: Boolean = false) {
        if (!isReady) return

        if (isHindi) {
            tts?.language = Locale("hi", "IN")
        } else {
            tts?.language = Locale("en", "IN")
        }
        
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "WINKIT_TTS")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

// THE MAGIC KEY: This lets us access the Voice Assistant from ANY screen
val LocalVoiceAssistant = staticCompositionLocalOf<VoiceAssistant> { 
    error("VoiceAssistant not provided!") 
}
