package com.nexuswavetech.nexusplus.shared.speech

/**
 * Shared — platform-agnostic NSE speech state types.
 *
 * These types contain zero platform imports and compile identically on
 * Android, Desktop, and iOS targets.
 *
 * Platform-specific types (NseVoiceProfile with java.util.Locale,
 * NseAndroidEngine wrapping android.speech.tts.TextToSpeech) live in the
 * Android :app module and depend on these shared contracts.
 */

// ── Speech mode ────────────────────────────────────────────────────────────

sealed interface NseSpeechMode {
    /** Engine auto-detects locale from input text. */
    data object Auto : NseSpeechMode

    /**
     * Caller pins a specific language via BCP 47 tag (e.g. "hi-IN", "en-US").
     * Kept as a String so this type is fully platform-agnostic.
     */
    data class SingleVoice(val languageBcp47: String) : NseSpeechMode

    /**
     * Multi-language input: engine splits by detected script segment and
     * synthesises each with the best-matching voice.
     */
    data object Mix : NseSpeechMode
}

// ── Engine state ───────────────────────────────────────────────────────────

sealed interface NseSpeechEngineState {
    data object Initialising : NseSpeechEngineState
    data object Ready        : NseSpeechEngineState
    data object Speaking     : NseSpeechEngineState
    data object Paused       : NseSpeechEngineState
    data class  Error(val message: String, val cause: Throwable? = null) : NseSpeechEngineState
}

// ── Utterance lifecycle ────────────────────────────────────────────────────

sealed interface NseUtteranceEvent {
    data class Started  (val utteranceId: String)              : NseUtteranceEvent
    data class Completed(val utteranceId: String)              : NseUtteranceEvent
    data class Failed   (val utteranceId: String, val code: Int): NseUtteranceEvent
}

// ── Speech request params ──────────────────────────────────────────────────

data class NseSpeechParams(
    val text        : String,
    val mode        : NseSpeechMode = NseSpeechMode.Auto,
    val pitch       : Float         = 1.0f,
    val speechRate  : Float         = 1.0f,
    val utteranceId : String        = "nse_${currentTimeMillis()}",
)

/** Platform-agnostic time source; actual impl provided per-platform. */
expect fun currentTimeMillis(): Long
