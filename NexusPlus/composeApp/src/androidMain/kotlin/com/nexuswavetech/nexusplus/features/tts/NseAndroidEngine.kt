package com.nexuswavetech.nexusplus.features.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale as JavaLocale
import kotlin.coroutines.resume

/**
 * NSE 2.0 — Android TTS engine implementation.
 *
 * Wraps android.speech.tts.TextToSpeech behind the [NseEngine] contract.
 * All public methods are safe to call from any thread; the underlying
 * TTS engine callbacks are marshalled through the coroutine machinery.
 *
 * Stability improvements over the legacy implementation:
 *  - Initialization is suspendable — no race between "engine ready" and
 *    the first speak() call.
 *  - Audio params bundle is rebuilt per-request so pitch/rate changes are
 *    always reflected without reinitialising the engine.
 *  - UtteranceProgressListener is registered once; callbacks are forwarded
 *    to an injectable listener for testability.
 *  - Language availability is checked before synthesis; falls back to
 *    Locale.ENGLISH to prevent LANG_NOT_SUPPORTED failures.
 */
class NseAndroidEngine(
    private val context: Context,
    private val audioFocus: NseAudioFocusManager,
) : NseEngine {

    private var tts: TextToSpeech? = null

    @Volatile private var engineReady = false

    override var utteranceResultListener: ((NseUtteranceResult) -> Unit)? = null

    // ── Initialisation ─────────────────────────────────────────────────────

    override suspend fun initialise(): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val engine = TextToSpeech(context) { status ->
                engineReady = status == TextToSpeech.SUCCESS
                if (engineReady) {
                    registerUtteranceListener()
                    cont.resume(Result.success(Unit))
                } else {
                    cont.resume(Result.failure(IllegalStateException("TTS init failed: status=$status")))
                }
            }
            tts = engine
            cont.invokeOnCancellation { engine.shutdown() }
        }

    // ── Synthesis ──────────────────────────────────────────────────────────

    override suspend fun speak(request: NseSpeechRequest): Result<Unit> {
        val engine = tts ?: return Result.failure(IllegalStateException("Engine not initialised"))
        if (!engineReady)    return Result.failure(IllegalStateException("Engine not ready"))

        if (!audioFocus.requestFocus()) {
            return Result.failure(IllegalStateException("Could not acquire audio focus"))
        }

        audioFocus.onFocusLost = { engine.stop(); audioFocus.abandonFocus() }

        return when (request.mode) {
            is NseSpeechMode.Auto       -> speakAuto(engine, request)
            is NseSpeechMode.SingleVoice -> speakSingleVoice(engine, request, request.mode.locale)
            is NseSpeechMode.DualVoice   -> speakDual(engine, request)
            is NseSpeechMode.Mix        -> speakMix(engine, request)
        }
    }

    private fun speakAuto(engine: TextToSpeech, req: NseSpeechRequest): Result<Unit> {
        val locale = NseLanguageDetector.detect(req.text)
        return synthesise(engine, req.text, locale, req.pitch, req.speechRate, req.utteranceId, flush = true)
    }

    private fun speakSingleVoice(engine: TextToSpeech, req: NseSpeechRequest, locale: NseLocale): Result<Unit> =
        synthesise(engine, req.text, locale, req.pitch, req.speechRate, req.utteranceId, flush = true)

    private fun speakDual(engine: TextToSpeech, req: NseSpeechRequest): Result<Unit> {
        val mode = req.mode as? NseSpeechMode.DualVoice ?: return Result.failure(
            IllegalStateException("Dual mode required")
        )
        val segments = NseLanguageDetector.segmentByScript(req.text)
        if (segments.isEmpty()) return Result.success(Unit)

        segments.forEachIndexed { index, (segText, segLocale) ->
            val assigned = if (segLocale == mode.primaryLocale || segLocale == mode.secondaryLocale) segLocale
            else if (NseLanguageDetector.detect(segText) == mode.primaryLocale) mode.primaryLocale
            else mode.secondaryLocale
            val uid = "${req.utteranceId}_seg$index"
            val flush = index == 0
            val result = synthesise(engine, segText, assigned, req.pitch, req.speechRate, uid, flush)
            if (result.isFailure) return result
        }
        return Result.success(Unit)
    }

    private fun speakMix(engine: TextToSpeech, req: NseSpeechRequest): Result<Unit> {
        val segments = NseLanguageDetector.segmentByScript(req.text)
        if (segments.isEmpty()) return Result.success(Unit)

        segments.forEachIndexed { index, (segText, locale) ->
            val uid  = "${req.utteranceId}_seg$index"
            val flush = index == 0
            val result = synthesise(engine, segText, locale, req.pitch, req.speechRate, uid, flush)
            if (result.isFailure) return result
        }
        return Result.success(Unit)
    }

    private fun NseLocale.toJavaLocale(): JavaLocale =
        if (country.isEmpty()) JavaLocale(language) else JavaLocale(language, country)

    private fun synthesise(
        engine: TextToSpeech,
        text: String,
        locale: NseLocale,
        pitch: Float,
        speechRate: Float,
        utteranceId: String,
        flush: Boolean,
    ): Result<Unit> {
        val javaLocale = locale.toJavaLocale()
        val supported = engine.isLanguageAvailable(javaLocale)
        val resolvedLocale = if (supported >= TextToSpeech.LANG_AVAILABLE) javaLocale else JavaLocale.ENGLISH

        engine.language  = resolvedLocale
        engine.setPitch(pitch)
        engine.setSpeechRate(speechRate)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val res = engine.speak(text, queueMode, params, utteranceId)

        return if (res == TextToSpeech.SUCCESS) Result.success(Unit)
        else Result.failure(IllegalStateException("TTS speak() failed: code=$res"))
    }

    // ── Control ────────────────────────────────────────────────────────────

    override fun stop() {
        tts?.stop()
        audioFocus.abandonFocus()
    }

    override fun availableVoices(locale: NseLocale?): List<NseVoiceProfile> {
        val engine = tts ?: return emptyList()
        return try {
            engine.voices
                ?.map { v ->
                    NseVoiceProfile(
                        name              = v.name,
                        locale            = NseLocale(v.locale.language, v.locale.country),
                        isNetworkRequired = v.isNetworkConnectionRequired,
                        quality           = v.quality,
                        latency           = v.latency,
                    )
                }
                ?.filter { locale == null || it.locale == locale }
                ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    override fun shutdown() {
        audioFocus.abandonFocus()
        tts?.stop()
        tts?.shutdown()
        tts = null
        engineReady = false
    }

    // ── Utterance listener ─────────────────────────────────────────────────

    private fun registerUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                utteranceResultListener?.invoke(NseUtteranceResult.Started(utteranceId))
            }
            override fun onDone(utteranceId: String) {
                audioFocus.abandonFocus()
                utteranceResultListener?.invoke(NseUtteranceResult.Completed(utteranceId))
            }
            @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, errorCode)"))
            override fun onError(utteranceId: String) {
                audioFocus.abandonFocus()
                utteranceResultListener?.invoke(NseUtteranceResult.Failed(utteranceId, -1))
            }
            override fun onError(utteranceId: String, errorCode: Int) {
                audioFocus.abandonFocus()
                utteranceResultListener?.invoke(NseUtteranceResult.Failed(utteranceId, errorCode))
            }
        })
    }
}
