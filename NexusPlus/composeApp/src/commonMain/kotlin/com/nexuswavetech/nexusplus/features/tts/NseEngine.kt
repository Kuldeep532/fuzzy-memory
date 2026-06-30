package com.nexuswavetech.nexusplus.features.tts

import kotlin.random.Random

/**
 * NSE 2.0 — Core abstractions.
 *
 * Separating the contract from any concrete engine implementation
 * keeps the ViewModel and UI layer fully engine-agnostic. Swap the
 * underlying TTS provider without touching a single line of UI code.
 */

// ─── Speech synthesis modes ────────────────────────────────────────────

sealed interface NseSpeechMode {
    /** Engine picks the locale automatically from input text. */
    data object Auto : NseSpeechMode

    /** Caller pins a specific locale/voice. */
    data class SingleVoice(val locale: NseLocale) : NseSpeechMode

    /**
     * Dual mode: uses a primary voice for the main language and a secondary
     * voice for detected alternate language segments.
     */
    data class DualVoice(
        val primaryLocale: NseLocale,
        val secondaryLocale: NseLocale,
    ) : NseSpeechMode

    /**
     * Mix mode: the engine splits the text by detected script segment and
     * synthesises each segment with the best-matching voice.
     */
    data object Mix : NseSpeechMode
}

// ─── Observable engine state ─────────────────────────────────────────

sealed interface NseState {
    data object Initialising  : NseState
    data object Ready         : NseState
    data object Speaking      : NseState
    data object Paused        : NseState
    data class  Error(val message: String, val cause: Throwable? = null) : NseState
}

// ─── Per-utterance result ─────────────────────────────────────────────

sealed interface NseUtteranceResult {
    data class  Started(val utteranceId: String)   : NseUtteranceResult
    data class  Completed(val utteranceId: String) : NseUtteranceResult
    data class  Failed(val utteranceId: String, val error: Int) : NseUtteranceResult
}

// ─── Voice profile ────────────────────────────────────────────────────

data class NseVoiceProfile(
    val name: String,
    val locale: NseLocale,
    val isNetworkRequired: Boolean,
    val quality: Int,
    val latency: Int,
)

// ─── Speech request ─────────────────────────────────────────────────

data class NseSpeechRequest(
    val text: String,
    val mode: NseSpeechMode = NseSpeechMode.Auto,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val utteranceId: String = "nse_${Random.nextLong().and(Long.MAX_VALUE)}",
)

// ─── TTS engine metadata (platform-specific engines like Google, Samsung, etc.) ───

data class NseTtsEngineInfo(
    val name: String,
    val packageName: String,
    val label: String,
    val icon: Int = 0,   // Android resource id; 0 on non-Android platforms
)

// ─── Engine contract ────────────────────────────────────────────────

interface NseEngine {
    /** Initialise the underlying TTS provider. Must be called once. */
    suspend fun initialise(): Result<Unit>

    /** Speak using the provided request. */
    suspend fun speak(request: NseSpeechRequest): Result<Unit>

    /** Stop current utterance immediately. */
    fun stop()

    /** List available voices, filtered by locale if provided. */
    fun availableVoices(locale: NseLocale? = null): List<NseVoiceProfile>

    /** List installed TTS engines on the device (Android-specific; empty elsewhere). */
    fun availableEngines(): List<NseTtsEngineInfo> = emptyList()

    /** Switch to a different TTS engine by package name. Returns success/failure. */
    suspend fun switchEngine(packageName: String): Result<Unit> = Result.failure(UnsupportedOperationException())

    /** Release all resources. Must be called when the engine is no longer needed. */
    fun shutdown()

    /**
     * Utterance lifecycle callback. Receives [NseUtteranceResult] events.
     * Set by [NseRepository] at init.
     */
    var utteranceResultListener: ((NseUtteranceResult) -> Unit)?
}
