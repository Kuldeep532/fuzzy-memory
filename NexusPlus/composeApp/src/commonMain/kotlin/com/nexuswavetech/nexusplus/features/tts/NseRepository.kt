package com.nexuswavetech.nexusplus.features.tts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Marker interface for pipeline-capable engines.
 * [NsePipelineAndroidEngine] (androidMain) implements this so that
 * [NseRepository] can expose [isPipelineEngine] and [cachedPhraseCount]
 * without a direct androidMain dependency.
 */
interface NsePipelineEngine : NseEngine {
    fun cachedPhraseCount(): Int
}

/**
 * NSE 3.0 — Repository layer.
 */
class NseRepository(private val engine: NseEngine) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow<NseState>(NseState.Initialising)
    val state: StateFlow<NseState> = _state.asStateFlow()

    private val _detectedLocale = MutableStateFlow<NseLocale?>(null)
    val detectedLocale: StateFlow<NseLocale?> = _detectedLocale.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<NseVoiceProfile>>(emptyList())
    val availableVoices: StateFlow<List<NseVoiceProfile>> = _availableVoices.asStateFlow()

    val isPipelineEngine: Boolean get() = engine is NsePipelineEngine

    private val _cachedPhraseCount = MutableStateFlow(0)
    val cachedPhraseCount: StateFlow<Int> = _cachedPhraseCount.asStateFlow()

    init {
        engine.utteranceResultListener = { result ->
            when (result) {
                is NseUtteranceResult.Started   -> _state.value = NseState.Speaking
                is NseUtteranceResult.Completed -> {
                    _state.value = NseState.Ready
                    (engine as? NsePipelineEngine)?.let {
                        _cachedPhraseCount.value = it.cachedPhraseCount()
                    }
                }
                is NseUtteranceResult.Failed    ->
                    _state.value = NseState.Error("Utterance failed (code=${result.error})", null)
            }
        }
    }

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

    fun updateDetectedLocale(text: String) {
        if (text.length > 5) {
            _detectedLocale.value = NseLanguageDetector.detect(text)
        } else {
            _detectedLocale.value = null
        }
    }

    fun voicesForLocale(locale: NseLocale): List<NseVoiceProfile> =
        engine.availableVoices(locale)

    fun availableEngines(): List<NseTtsEngineInfo> = engine.availableEngines()

    suspend fun switchEngine(packageName: String): Result<Unit> =
        engine.switchEngine(packageName)

    fun refreshVoices() {
        _availableVoices.value = engine.availableVoices()
    }

    fun shutdown() {
        engine.shutdown()
        _state.value = NseState.Initialising
    }
}
