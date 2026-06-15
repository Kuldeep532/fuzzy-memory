package com.nexuswavetech.nexusplus.features.tts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * NSE 3.0 — Repository layer.
 *
 * Owns the engine lifecycle, exposes observable state, and serialises
 * concurrent speak/stop requests through a [SupervisorJob]-scoped
 * coroutine scope.
 *
 * Now accepts [NseEngine] (interface) rather than the concrete
 * [NseAndroidEngine], enabling the NSE 3.0 pipeline engine ([NsePipelineAndroidEngine])
 * to be injected transparently while preserving the same public API
 * consumed by all ViewModels and screens.
 *
 * New in NSE 3.0:
 *  - [isPipelineEngine] — true when the injected engine is the pipeline variant
 *  - [cachedPhraseCount] — observable count of PCM-cached phrases (pipeline only)
 */
class NseRepository(private val engine: NseEngine) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow<NseState>(NseState.Initialising)
    val state: StateFlow<NseState> = _state.asStateFlow()

    private val _detectedLocale = MutableStateFlow<Locale?>(null)
    val detectedLocale: StateFlow<Locale?> = _detectedLocale.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<NseVoiceProfile>>(emptyList())
    val availableVoices: StateFlow<List<NseVoiceProfile>> = _availableVoices.asStateFlow()

    /** True when running the NSE 3.0 pre-synthesis pipeline engine. */
    val isPipelineEngine: Boolean get() = engine is NsePipelineAndroidEngine

    /** Number of phrases currently in the PCM cache (pipeline engine only). */
    val cachedPhraseCount: StateFlow<Int> =
        (engine as? NsePipelineAndroidEngine)?.pcmCache?.size
            ?: MutableStateFlow(0).asStateFlow()

    init {
        engine.utteranceResultListener = { result ->
            when (result) {
                is NseUtteranceResult.Started   -> _state.value = NseState.Speaking
                is NseUtteranceResult.Completed -> _state.value = NseState.Ready
                is NseUtteranceResult.Failed    ->
                    _state.value = NseState.Error("Utterance failed (code=${result.error})", null)
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun initialise() {
        scope.launch {
            _state.value = NseState.Initialising
            engine.initialise()
                .onSuccess {
                    _state.value = NseState.Ready
                    _availableVoices.value = engine.availableVoices()
                }
                .onFailure { ex ->
                    _state.value = NseState.Error("NSE engine failed to start: ${ex.message}", ex)
                }
        }
    }

    // ── Synthesis ────────────────────────────────────────────────────────────

    fun speak(request: NseSpeechRequest) {
        scope.launch {
            _state.value = NseState.Speaking
            if (request.mode == NseSpeechMode.Auto && request.text.length > 5) {
                _detectedLocale.value = NseLanguageDetector.detect(request.text)
            }
            engine.speak(request)
                .onFailure { ex ->
                    _state.value = NseState.Error(ex.message ?: "Unknown error", ex)
                }
        }
    }

    fun stop() {
        engine.stop()
        _state.value = NseState.Ready
    }

    // ── Query ────────────────────────────────────────────────────────────────

    fun updateDetectedLocale(text: String) {
        if (text.length > 5) {
            _detectedLocale.value = NseLanguageDetector.detect(text)
        } else {
            _detectedLocale.value = null
        }
    }

    fun voicesForLocale(locale: Locale): List<NseVoiceProfile> =
        engine.availableVoices(locale)

    // ── Cleanup ──────────────────────────────────────────────────────────────

    fun shutdown() {
        engine.shutdown()
        _state.value = NseState.Initialising
    }
}
