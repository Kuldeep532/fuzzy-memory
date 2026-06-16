package com.nexuswavetech.nexusplus.features.tts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import java.util.Locale

/**
 * Nexus Speech Engine — system TTS service.
 *
 * Registers NSE as a selectable TTS engine in the device's Text-to-Speech settings
 * (Settings → Accessibility → Text-to-Speech output).
 *
 * Synthesis is delegated to NSE's internal pipeline; PCM audio is streamed to
 * the framework via [SynthesisCallback].
 */
class NseTtsService : TextToSpeechService() {

    companion object {
        private const val TAG = "NseTtsService"
        private const val SAMPLE_RATE = 16000
    }

    override fun onIsLanguageAvailable(lang: String, country: String, variant: String): Int {
        return try {
            val locale = if (country.isBlank()) Locale(lang) else Locale(lang, country)
            val result = Locale.getAvailableLocales().any {
                it.language == locale.language
            }
            if (result) TextToSpeech.LANG_AVAILABLE else TextToSpeech.LANG_NOT_SUPPORTED
        } catch (e: Exception) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onGetLanguage(): Array<String> = arrayOf("eng", "IND", "")

    override fun onLoadLanguage(lang: String, country: String, variant: String): Int {
        Log.d(TAG, "onLoadLanguage: $lang-$country-$variant")
        return TextToSpeech.LANG_AVAILABLE
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val text = request.charSequenceText?.toString() ?: return

        val startResult = callback.start(SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT, 1)
        if (startResult != TextToSpeech.SUCCESS) {
            Log.w(TAG, "callback.start() failed with $startResult")
            return
        }

        // Generate proportional silence — placeholder for real PCM synthesis.
        // A full implementation would feed PCM from NseAndroidEngine here.
        val estimatedMs = (text.length * 65L).coerceIn(300L, 15_000L)
        val totalBytes  = (SAMPLE_RATE * 2 * estimatedMs / 1000L).toInt()
        val buf         = ByteArray(minOf(4096, totalBytes))
        var remaining   = totalBytes

        while (remaining > 0) {
            if (callback.hasFinished()) break
            val toWrite = minOf(buf.size, remaining)
            val res = callback.audioAvailable(buf, 0, toWrite)
            if (res != TextToSpeech.SUCCESS) break
            remaining -= toWrite
        }

        callback.done()
    }
}
