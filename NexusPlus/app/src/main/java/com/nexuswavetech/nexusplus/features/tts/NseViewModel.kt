package com.nexuswavetech.nexusplus.features.tts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

/**
 * NSE 2.0 — ViewModel.
 *
 * Exposes all user-controllable speech parameters as [StateFlow] and
 * delegates every side-effecting operation to [NseRepository].
 *
 * Three synthesis modes available to the UI:
 *   • Auto      — engine detects language automatically.
 *   • SingleVoice — user picks a locale from the available voice list.
 *   • Mix       — multi-language input, voiced per-script segment.
 */
@OptIn(FlowPreview::class)
class NseViewModel(private val repository: NseRepository) : ViewModel() {

    // ── Engine state (forwarded from repository) ───────────────────────────

    val engineState: StateFlow<NseState> = repository.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, NseState.Initialising)

    val detectedLocale: StateFlow<Locale?> = repository.detectedLocale
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val availableVoices: StateFlow<List<NseVoiceProfile>> = repository.availableVoices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── User-editable parameters ───────────────────────────────────────────

    private val _inputText   = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _pitch       = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _speechRate  = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _mode        = MutableStateFlow<NseSpeechMode>(NseSpeechMode.Auto)
    val mode: StateFlow<NseSpeechMode> = _mode.asStateFlow()

    private val _selectedVoice = MutableStateFlow<NseVoiceProfile?>(null)
    val selectedVoice: StateFlow<NseVoiceProfile?> = _selectedVoice.asStateFlow()

    // ── Derived convenience ────────────────────────────────────────────────

    val isSpeaking: StateFlow<Boolean> = repository.state
        .distinctUntilChanged()
        .let { flow ->
            MutableStateFlow(false).also { sf ->
                flow.onEach { sf.value = it == NseState.Speaking }.launchIn(viewModelScope)
            }
        }

    val isReady: StateFlow<Boolean> = repository.state
        .distinctUntilChanged()
        .let { flow ->
            MutableStateFlow(false).also { sf ->
                flow.onEach { sf.value = it == NseState.Ready }.launchIn(viewModelScope)
            }
        }

    // ── Initialisation ─────────────────────────────────────────────────────

    init {
        repository.initialise()

        // Debounced language detection as user types
        _inputText
            .debounce(400)
            .distinctUntilChanged()
            .onEach { text -> repository.updateDetectedLocale(text) }
            .launchIn(viewModelScope)
    }

    // ── User actions ───────────────────────────────────────────────────────

    fun onTextChange(text: String) {
        _inputText.value = text
    }

    fun onPitchChange(value: Float) {
        _pitch.value = value.coerceIn(0.5f, 2.0f)
    }

    fun onSpeechRateChange(value: Float) {
        _speechRate.value = value.coerceIn(0.25f, 3.0f)
    }

    fun onModeChange(mode: NseSpeechMode) {
        _mode.value = mode
    }

    fun onVoiceSelected(voice: NseVoiceProfile?) {
        _selectedVoice.value = voice
        if (voice != null) {
            _mode.value = NseSpeechMode.SingleVoice(voice.locale)
        } else {
            _mode.value = NseSpeechMode.Auto
        }
    }

    fun speak() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        val resolvedMode = when (val m = _mode.value) {
            is NseSpeechMode.SingleVoice -> m
            is NseSpeechMode.Mix        -> m
            is NseSpeechMode.Auto       -> NseSpeechMode.Auto
        }
        repository.speak(
            NseSpeechRequest(
                text       = text,
                mode       = resolvedMode,
                pitch      = _pitch.value,
                speechRate = _speechRate.value,
            )
        )
    }

    fun stop() = repository.stop()

    fun clearText() {
        _inputText.value = ""
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        repository.shutdown()
    }
}
