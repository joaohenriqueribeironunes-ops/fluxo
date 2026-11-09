package com.arborecence.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale("pt", "BR"))
            ready = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun speak(text: String, speed: Float = 1.0f, enabled: Boolean = true) {
        if (!enabled || !ready || text.isBlank()) return
        tts?.setSpeechRate(speed.coerceIn(0.5f, 1.6f))
        tts?.speak(text.take(4000), TextToSpeech.QUEUE_FLUSH, null, "arb_tts")
    }

    fun stop() { tts?.stop() }
    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null }
}
