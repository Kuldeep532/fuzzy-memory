package com.nexuswavetech.nexusplus.features.tts

import java.util.Locale

/**
 * NSE 2.0 — Core abstractions.
 *
 * Separating the contract from any concrete engine implementation
 * keeps the ViewModel and UI layer fully engine-agnostic. Swap the
 * underlying TTS provider without touching a single line of UI code.
 */

// ─── Speech synthesis modes ────────────────────────────────────────────────

sealed interface NseSpeechMode {
    /** Engine picks the locale automatically from input text. */
    data object Auto : NseSpeechMode

    /** Caller pins a specific locale/voice. */
    data class SingleVoice(val locale: Locale) : NseSpeechMode

    /**
     * Dual mode: uses a primary voice for the main language and a secondary
     * voice for detected alternate language segments. Ideal for bilingual users.
     */
    data class DualVoice(
        val primaryLocale: Locale,
        val secondaryLocale: Locale,
    ) : NseSpeechMode

    /**
     * Mix mode: the engine splits the text by detected script segment and
     * synthesises each segment with the best-matching voice, then queues
     * them back-to-back. Produces a seamless multi-language experience.
     */
    data object Mix : NseSpeechMode
}

// ─── Observable engine state ───────────────────────────────────────────────

sealed interface NseState {
    data object Initialising  : NseState
    data object Ready         : NseState
    data object Speaking      : NseState
    data object Paused        : NseState
    data class  Error(val message: String, val cause: Throwable? = null) : NseState
}

// ─── Per-utterance result ──────────────────────────────────────────────────

sealed interface NseUtteranceResult {
    data class  Started(val utteranceId: String)  : NseUtteranceResult
    data class  Completed(val utteranceId: String): NseUtteranceResult
    data class  Failed(val utteranceId: String, val error: Int): NseUtteranceResult
}

// ─── Voice profile (wraps android.speech.tts.Voice metadata) ──────────────

data class NseVoiceProfile(
    val name: String,
    val locale: Locale,
    val isNetworkRequired: Boolean,
    val quality: Int,
    val latency: Int,
)

// ─── Speech request ────────────────────────────────────────────────────────

data class NseSpeechRequest(
    val text: String,
    val mode: NseSpeechMode = NseSpeechMode.Auto,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val utteranceId: String = "nse_${System.nanoTime()}",
)

// ─── Engine contract ───────────────────────────────────────────────────────

interface NseEngine {
    /** Initialise the underlying TTS provider. Must be called once. */
    suspend fun initialise(): Result<Unit>

    /** Speak using the provided request. */
    suspend fun speak(request: NseSpeechRequest): Result<Unit>

    /** Stop current utterance immediately. */
    fun stop()

    /** List available voices, filtered by locale if provided. */
    fun availableVoices(locale: Locale? = null): List<NseVoiceProfile>

    /** Release all resources. Must be called when the engine is no longer needed. */
    fun shutdown()

    /**
     * Utterance lifecycle callback. Receives [NseUtteranceResult] events for
     * started / completed / failed utterances. Set by [NseRepository] at init.
     */
    var utteranceResultListener: ((NseUtteranceResult) -> Unit)?
}
