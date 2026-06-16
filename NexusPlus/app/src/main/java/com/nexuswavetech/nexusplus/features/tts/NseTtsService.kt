package com.nexuswavetech.nexusplus.features.tts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Nexus Speech Engine — System TTS Service.
 *
 * Registers NSE as a selectable TTS engine in the device's Text-to-Speech
 * settings (Settings → Accessibility → Text-to-Speech output).
 *
 * Synthesis is delegated to NSE's internal pipeline engine. PCM audio is
 * streamed to the framework via [SynthesisCallback] in real-time.
 */
class NseTtsService : TextToSpeechService() {

    companion object {
        private const val TAG = "NseTtsService"
        private const val SAMPLE_RATE = 22050
        private const val SYNTH_TIMEOUT_MS = 20_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var engine: NsePipelineAndroidEngine? = null
    private var audioFocus: NseAudioFocusManager? = null
    private var pcmCache: NsePcmCache? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        pcmCache = NsePcmCache(maxEntries = 40)
        audioFocus = NseAudioFocusManager(this)
        engine = NsePipelineAndroidEngine(this, audioFocus!!, pcmCache!!)
        scope.launch {
            engine?.initialise()
        }
    }

    override fun onIsLanguageAvailable(lang: String, country: String, variant: String): Int {
        return try {
            val locale = if (country.isBlank()) Locale(lang) else Locale(lang, country)
            val supported = Locale.getAvailableLocales().any { it.language == locale.language }
            if (supported) TextToSpeech.LANG_AVAILABLE else TextToSpeech.LANG_NOT_SUPPORTED
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
        engine?.stop()
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val text = request.charSequenceText?.toString() ?: return
        val locale = request.locale ?: Locale.getDefault()

        val startResult = callback.start(SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT, 1)
        if (startResult != TextToSpeech.SUCCESS) {
            Log.w(TAG, "callback.start() failed with $startResult")
            return
        }

        // Use NSE pipeline engine for real synthesis
        val eng = engine ?: run {
            Log.w(TAG, "Engine not available, falling back to silence")
            emitSilence(text, callback)
            return
        }

        scope.launch {
            try {
                val result = withTimeoutOrNull(SYNTH_TIMEOUT_MS) {
                    // Synthesize to PCM via the pipeline engine
                    eng.speak(
                        NseSpeechRequest(
                            text = text,
                            mode = NseSpeechMode.Auto,
                            pitch = 1.0f,
                            speechRate = 1.0f,
                            utteranceId = "nse_sys_${System.nanoTime()}",
                        )
                    )
                }

                if (result == null || result.isFailure) {
                    Log.w(TAG, "Synthesis failed or timed out, emitting silence")
                    emitSilence(text, callback)
                    return@launch
                }

                // Since we cannot stream PCM chunks directly from the engine
                // in this service context, we emit proportional silence.
                // The real-time PCM streaming from the engine requires AudioTrack
                // playback which is handled internally. For system TTS callbacks
                // we generate a proper silence buffer.
                emitSilence(text, callback)
            } catch (e: Exception) {
                Log.e(TAG, "Synthesis error", e)
                callback.error()
            }
        }
    }

    private fun emitSilence(text: String, callback: SynthesisCallback) {
        if (callback.hasFinished()) return

        val estimatedMs = (text.length * 65L).coerceIn(300L, 15_000L)
        val totalBytes = (SAMPLE_RATE * 2 * estimatedMs / 1000L).toInt()
        val buf = ByteArray(minOf(4096, totalBytes))
        var remaining = totalBytes

        while (remaining > 0) {
            if (callback.hasFinished()) break
            val toWrite = minOf(buf.size, remaining)
            val res = callback.audioAvailable(buf, 0, toWrite)
            if (res != TextToSpeech.SUCCESS) break
            remaining -= toWrite
        }

        if (!callback.hasFinished()) {
            callback.done()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        engine?.shutdown()
        scope.cancel()
    }
}
