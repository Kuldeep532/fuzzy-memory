package com.nexuswavetech.nexusplus.features.tts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * NSE 4.0 — ViewModel.
 *
 * Exposes all user-controllable speech parameters as [StateFlow] and
 * delegates every side-effecting operation to [NseRepository].
 *
 * Four screen-reader synthesis modes available to the UI:
 *   • Auto      — engine detects language automatically.
 *   • SingleVoice — user picks one voice for all content.
 *   • DualVoice — primary voice for main language, secondary for alternate.
 *   • Mix       — multi-language input, voiced per-script segment.
 *
 * Settings are loaded from [SettingsRepository] on init and persisted
 * via the Save / Reset actions.
 */
@OptIn(FlowPreview::class)
class NseViewModel(
    private val repository: NseRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    // ── Engine state (forwarded from repository) ──────────────────────────

    val engineState: StateFlow<NseState> = repository.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, NseState.Initialising)

    val detectedLocale: StateFlow<NseLocale?> = repository.detectedLocale
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val availableVoices: StateFlow<List<NseVoiceProfile>> = repository.availableVoices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** True when the NSE 4.0 pipeline engine is active. */
    val isPipelineEngine: Boolean get() = repository.isPipelineEngine

    /** Observable count of phrases currently in the PCM cache. */
    val cachedPhraseCount: StateFlow<Int> = repository.cachedPhraseCount
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── User-editable parameters ────────────────────────────────────────

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _mode = MutableStateFlow<NseSpeechMode>(NseSpeechMode.Auto)
    val mode: StateFlow<NseSpeechMode> = _mode.asStateFlow()

    private val _selectedVoice = MutableStateFlow<NseVoiceProfile?>(null)
    val selectedVoice: StateFlow<NseVoiceProfile?> = _selectedVoice.asStateFlow()

    private val _secondaryVoice = MutableStateFlow<NseVoiceProfile?>(null)
    val secondaryVoice: StateFlow<NseVoiceProfile?> = _secondaryVoice.asStateFlow()

    // ── TTS engine selection ────────────────────────────────────────────────

    private val _engines = MutableStateFlow<List<NseTtsEngineInfo>>(emptyList())
    val engines: StateFlow<List<NseTtsEngineInfo>> = _engines.asStateFlow()

    private val _selectedEngine = MutableStateFlow<NseTtsEngineInfo?>(null)
    val selectedEngine: StateFlow<NseTtsEngineInfo?> = _selectedEngine.asStateFlow()

    // ── Predictive Pre-Buffer — unique fast-screen-reader feature ──────────────
    // When ON: pipeline pre-synthesizes 2 sentences ahead (instead of 1).
    // Makes even slow TTS engines feel instant — critical for blind users.

    private val _predictivePreBuffer = MutableStateFlow(false)
    val predictivePreBuffer: StateFlow<Boolean> = _predictivePreBuffer.asStateFlow()

    // ── Screen-reader settings (persisted) ──────────────────────────

    private val _screenReaderMode = MutableStateFlow(SettingsRepository.TTS_MODE_AUTO)
    val screenReaderMode: StateFlow<String> = _screenReaderMode.asStateFlow()

    private val _notificationFilter = MutableStateFlow(true)
    val notificationFilter: StateFlow<Boolean> = _notificationFilter.asStateFlow()

    private val _windowChangeDetection = MutableStateFlow(true)
    val windowChangeDetection: StateFlow<Boolean> = _windowChangeDetection.asStateFlow()

    private val _focusTracking = MutableStateFlow(true)
    val focusTracking: StateFlow<Boolean> = _focusTracking.asStateFlow()

    private val _continuousRead = MutableStateFlow(true)
    val continuousRead: StateFlow<Boolean> = _continuousRead.asStateFlow()

    private val _duplicateFilter = MutableStateFlow(true)
    val duplicateFilter: StateFlow<Boolean> = _duplicateFilter.asStateFlow()

    private val _autoStart = MutableStateFlow(false)
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    // ── Derived convenience ────────────────────────────────────────────

    val isSpeaking: StateFlow<Boolean> = repository.state
        .let { flow ->
            MutableStateFlow(false).also { sf ->
                flow.onEach { sf.value = it == NseState.Speaking }.launchIn(viewModelScope)
            }
        }

    val isReady: StateFlow<Boolean> = repository.state
        .let { flow ->
            MutableStateFlow(false).also { sf ->
                flow.onEach { sf.value = it == NseState.Ready }.launchIn(viewModelScope)
            }
        }

    // ── Initialisation ───────────────────────────────────────────────

    init {
        repository.initialise()
        loadSavedSettings()
        refreshEngines()

        // Debounced language detection as user types
        _inputText
            .debounce(400)
            .distinctUntilChanged()
            .onEach { text -> repository.updateDetectedLocale(text) }
            .launchIn(viewModelScope)
    }

    // ── Settings persistence ────────────────────────────────────────

    private fun loadSavedSettings() {
        viewModelScope.launch {
            val savedRate = settings.ttsDefaultRate.first().coerceIn(0.25f, 3.0f)
            _speechRate.value = savedRate

            val savedPitch = settings.ttsPitch.first().coerceIn(0.5f, 2.0f)
            _pitch.value = savedPitch

            val savedMode = settings.ttsScreenReaderMode.first()
            _screenReaderMode.value = savedMode
            _mode.value = mapModeString(savedMode)

            val savedVoice = settings.ttsVoiceSelection.first()
            if (savedVoice.isNotBlank()) {
                val locale = NseLocale.forLanguageTag(savedVoice)
                _selectedVoice.value = repository.voicesForLocale(locale).firstOrNull()
            }

            val savedSecLang = settings.ttsSecondaryLanguage.first()
            if (savedSecLang.isNotBlank() && savedSecLang != SettingsRepository.TTS_LANG_AUTO) {
                val locale = NseLocale.forLanguageTag(savedSecLang)
                _secondaryVoice.value = repository.voicesForLocale(locale).firstOrNull()
            }

            _notificationFilter.value = settings.ttsNotificationFilter.first()
            _windowChangeDetection.value = settings.ttsWindowChangeDetection.first()
            _focusTracking.value = settings.ttsFocusTracking.first()
            _continuousRead.value = settings.ttsContinuousRead.first()
            _duplicateFilter.value = settings.ttsDuplicateFilter.first()
            _autoStart.value = settings.ttsAutoStart.first()
            _predictivePreBuffer.value = settings.predictivePreBuffer.first()
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            settings.setTtsDefaultRate(_speechRate.value)
            settings.setTtsPitch(_pitch.value)
            settings.setTtsScreenReaderMode(_screenReaderMode.value)
            settings.setTtsVoiceSelection(_selectedVoice.value?.locale?.toLanguageTag() ?: "")
            settings.setTtsSecondaryLanguage(_secondaryVoice.value?.locale?.toLanguageTag() ?: SettingsRepository.TTS_LANG_AUTO)
            settings.setTtsNotificationFilter(_notificationFilter.value)
            settings.setTtsWindowChange(_windowChangeDetection.value)
            settings.setTtsFocusTracking(_focusTracking.value)
            settings.setTtsContinuousRead(_continuousRead.value)
            settings.setTtsDuplicateFilter(_duplicateFilter.value)
            settings.setTtsAutoStart(_autoStart.value)
            settings.setPredictivePreBuffer(_predictivePreBuffer.value)
            _hasUnsavedChanges.value = false
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            _speechRate.value = 1.0f
            _pitch.value = 1.0f
            _mode.value = NseSpeechMode.Auto
            _screenReaderMode.value = SettingsRepository.TTS_MODE_AUTO
            _selectedVoice.value = null
            _secondaryVoice.value = null
            _notificationFilter.value = true
            _windowChangeDetection.value = true
            _focusTracking.value = true
            _continuousRead.value = true
            _duplicateFilter.value = true
            _autoStart.value = false
            _predictivePreBuffer.value = false
            _hasUnsavedChanges.value = true
        }
    }

    // ── User actions ───────────────────────────────────────────────

    fun onTextChange(text: String) {
        _inputText.value = text
    }

    fun onPitchChange(value: Float) {
        _pitch.value = value.coerceIn(0.5f, 2.0f)
        _hasUnsavedChanges.value = true
    }

    fun onSpeechRateChange(value: Float) {
        _speechRate.value = value.coerceIn(0.25f, 3.0f)
        _hasUnsavedChanges.value = true
    }

    fun onModeChange(mode: NseSpeechMode) {
        _mode.value = mode
        _screenReaderMode.value = mapModeToString(mode)
        _hasUnsavedChanges.value = true
    }

    fun onScreenReaderModeChange(modeStr: String) {
        _screenReaderMode.value = modeStr
        _mode.value = mapModeString(modeStr)
        _hasUnsavedChanges.value = true
    }

    fun onVoiceSelected(voice: NseVoiceProfile?) {
        _selectedVoice.value = voice
        if (voice != null) {
            _mode.value = NseSpeechMode.SingleVoice(voice.locale)
            _screenReaderMode.value = SettingsRepository.TTS_MODE_SINGLE
        } else {
            _mode.value = NseSpeechMode.Auto
            _screenReaderMode.value = SettingsRepository.TTS_MODE_AUTO
        }
        _hasUnsavedChanges.value = true
    }

    fun onSecondaryVoiceSelected(voice: NseVoiceProfile?) {
        _secondaryVoice.value = voice
        if (_mode.value is NseSpeechMode.Auto || _mode.value is NseSpeechMode.SingleVoice) {
            // Switch to dual if secondary voice is selected
            val primary = _selectedVoice.value
            if (primary != null && voice != null) {
                _mode.value = NseSpeechMode.DualVoice(primary.locale, voice.locale)
                _screenReaderMode.value = SettingsRepository.TTS_MODE_DUAL
            }
        }
        _hasUnsavedChanges.value = true
    }

    fun onNotificationFilterChange(v: Boolean) {
        _notificationFilter.value = v
        _hasUnsavedChanges.value = true
    }

    fun onWindowChangeDetectionChange(v: Boolean) {
        _windowChangeDetection.value = v
        _hasUnsavedChanges.value = true
    }

    fun onFocusTrackingChange(v: Boolean) {
        _focusTracking.value = v
        _hasUnsavedChanges.value = true
    }

    fun onContinuousReadChange(v: Boolean) {
        _continuousRead.value = v
        _hasUnsavedChanges.value = true
    }

    fun onDuplicateFilterChange(v: Boolean) {
        _duplicateFilter.value = v
        _hasUnsavedChanges.value = true
    }

    fun onAutoStartChange(v: Boolean) {
        _autoStart.value = v
        _hasUnsavedChanges.value = true
    }

    // ── Engine selection ────────────────────────────────────────────────

    fun refreshEngines() {
        viewModelScope.launch {
            _engines.value = repository.availableEngines()
        }
    }

    fun onEngineSelected(engine: NseTtsEngineInfo?) {
        viewModelScope.launch {
            _selectedEngine.value = engine
            if (engine != null) {
                repository.switchEngine(engine.packageName)
                    .onSuccess {
                        // Re-initialise voice list after engine switch
                        _availableVoices.value = repository.availableVoices()
                        _engines.value = repository.availableEngines()
                    }
            }
            _hasUnsavedChanges.value = true
        }
    }

    fun onPredictivePreBufferChange(v: Boolean) {
        _predictivePreBuffer.value = v
        _hasUnsavedChanges.value = true
    }

    fun speak() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        repository.speak(
            NseSpeechRequest(
                text = text,
                mode = _mode.value,
                pitch = _pitch.value,
                speechRate = _speechRate.value,
            )
        )
    }

    fun stop() = repository.stop()

    fun clearText() {
        _inputText.value = ""
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun mapModeString(mode: String): NseSpeechMode = when (mode) {
        SettingsRepository.TTS_MODE_SINGLE -> {
            val v = _selectedVoice.value
            if (v != null) NseSpeechMode.SingleVoice(v.locale) else NseSpeechMode.Auto
        }
        SettingsRepository.TTS_MODE_DUAL -> {
            val p = _selectedVoice.value
            val s = _secondaryVoice.value
            if (p != null && s != null) NseSpeechMode.DualVoice(p.locale, s.locale)
            else if (p != null) NseSpeechMode.SingleVoice(p.locale)
            else NseSpeechMode.Auto
        }
        SettingsRepository.TTS_MODE_MIXED -> NseSpeechMode.Mix
        else -> NseSpeechMode.Auto
    }

    private fun mapModeToString(mode: NseSpeechMode): String = when (mode) {
        is NseSpeechMode.Auto -> SettingsRepository.TTS_MODE_AUTO
        is NseSpeechMode.SingleVoice -> SettingsRepository.TTS_MODE_SINGLE
        is NseSpeechMode.DualVoice -> SettingsRepository.TTS_MODE_DUAL
        is NseSpeechMode.Mix -> SettingsRepository.TTS_MODE_MIXED
    }

    // ── Lifecycle ────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        repository.shutdown()
    }
}
