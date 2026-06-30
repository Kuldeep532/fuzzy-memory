package com.nexuswavetech.nexusplus.core

import com.nexuswavetech.nexusplus.platform.SettingsStore
import kotlinx.coroutines.flow.Flow

/**
 * Persists user-configurable app settings (theme, accessibility, security, features).
 * Platform-agnostic — backed by [SettingsStore] which uses DataStore on Android
 * and NSUserDefaults on iOS.
 */
class SettingsRepository(private val store: SettingsStore) {

    companion object {
        // Theme constants
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_DARK   = "DARK"
        const val THEME_LIGHT  = "LIGHT"

        // Font scale constants
        const val FONT_NORMAL = "NORMAL"
        const val FONT_LARGE  = "LARGE"
        const val FONT_XLARGE = "XLARGE"

        // Vault auto-lock options (minutes; 0 = Never)
        val VAULT_LOCK_OPTIONS = listOf(0, 5, 15, 30, 60)
        const val VAULT_LOCK_DEFAULT = 5

        // TTS language (BCP-47 tag; AUTO = system locale)
        const val TTS_LANG_AUTO = "AUTO"
        const val TTS_LANG_EN   = "en"
        const val TTS_LANG_HI   = "hi"
        const val TTS_LANG_ES   = "es"
        const val TTS_LANG_FR   = "fr"
        const val TTS_LANG_DE   = "de"
        const val TTS_LANG_ZH   = "zh"
        const val TTS_LANG_AR   = "ar"
        const val TTS_LANG_PT   = "pt"
        const val TTS_LANG_RU   = "ru"
        const val TTS_LANG_JA   = "ja"

        // TTS screen reader modes
        const val TTS_MODE_AUTO   = "AUTO"
        const val TTS_MODE_SINGLE = "SINGLE"
        const val TTS_MODE_DUAL   = "DUAL"
        const val TTS_MODE_MIXED  = "MIXED"
        val TTS_MODE_OPTIONS = listOf(TTS_MODE_AUTO, TTS_MODE_SINGLE, TTS_MODE_DUAL, TTS_MODE_MIXED)

        // Morse vibration speed
        const val MORSE_SPEED_SLOW   = "SLOW"
        const val MORSE_SPEED_NORMAL = "NORMAL"
        const val MORSE_SPEED_FAST   = "FAST"

        // Radio/IPTV buffer quality
        const val BUFFER_LOW    = "LOW"
        const val BUFFER_NORMAL = "NORMAL"
        const val BUFFER_HIGH   = "HIGH"

        // Calculator angle unit
        const val ANGLE_DEG = "DEG"
        const val ANGLE_RAD = "RAD"

        // Reminder snooze options (minutes)
        val REMINDER_SNOOZE_OPTIONS = listOf(5, 10, 15, 30, 60)
        const val REMINDER_SNOOZE_DEFAULT = 10

        // Sound Effects
        const val SOUND_EFFECTS_DEFAULT = true

        // NSE Voice Engine (built-in voices)
        const val NSE_VOICE_AUTO         = "AUTO"
        const val NSE_VOICE_NEXUS_HINDI   = "NEXUS_HINDI"
        const val NSE_VOICE_NEXUS_ENGLISH = "NEXUS_ENGLISH"
        val NSE_VOICE_OPTIONS = listOf(NSE_VOICE_AUTO, NSE_VOICE_NEXUS_HINDI, NSE_VOICE_NEXUS_ENGLISH)

        // Gemini AI model options
        const val GEMINI_MODEL_FLASH = "gemini-1.5-flash-latest"
        const val GEMINI_MODEL_PRO   = "gemini-1.5-pro-latest"
        val GEMINI_MODEL_OPTIONS = listOf(GEMINI_MODEL_FLASH, GEMINI_MODEL_PRO)
    }

    // ── Private keys ─────────────────────────────────────────────────────────
    private val KEY_THEME               = "app_theme"
    private val KEY_DYNAMIC_COLOR       = "dynamic_color"
    private val KEY_HIGH_CONTRAST       = "high_contrast"
    private val KEY_REDUCE_MOTION       = "reduce_motion"
    private val KEY_FONT_SCALE          = "font_scale"
    private val KEY_VAULT_AUTO_LOCK_MIN = "vault_auto_lock_minutes"
    private val KEY_TOUCH_VIBRATION     = "touch_vibration"
    private val KEY_TTS_SPEECH_RATE     = "tts_speech_rate"
    private val KEY_TTS_LANGUAGE        = "tts_language"
    private val KEY_TTS_SCREEN_READER_MODE = "tts_screen_reader_mode"
    private val KEY_TTS_VOICE_SELECTION = "tts_voice_selection"
    private val KEY_TTS_PITCH           = "tts_pitch"
    private val KEY_TTS_SECONDARY_LANG  = "tts_secondary_language"
    private val KEY_TTS_AUTO_START      = "tts_auto_start"
    private val KEY_TTS_NOTIFICATION_FILTER = "tts_notification_filter"
    private val KEY_TTS_WINDOW_CHANGE   = "tts_window_change"
    private val KEY_TTS_FOCUS_TRACKING  = "tts_focus_tracking"
    private val KEY_TTS_CONTINUOUS_READ = "tts_continuous_read"
    private val KEY_TTS_DUPLICATE_FILTER = "tts_duplicate_filter"
    private val KEY_MORSE_VIB_SPEED     = "morse_vibration_speed"
    private val KEY_BUFFER_QUALITY      = "buffer_quality"
    private val KEY_TRANSLATOR_AUTO     = "translator_auto_detect"
    private val KEY_REMINDER_SNOOZE_MIN = "reminder_snooze_minutes"
    private val KEY_CALC_ANGLE_UNIT     = "calculator_angle_unit"
    private val KEY_SOUND_EFFECTS       = "sound_effects_enabled"
    private val KEY_NSE_VOICE           = "nse_voice_engine"
    private val KEY_PREDICTIVE_BUFFER     = "predictive_pre_buffer"
    private val KEY_FORCE_TALKBACK_RATE   = "force_talkback_rate"
    private val KEY_GEMINI_API_KEY      = "gemini_api_key"
    private val KEY_GEMINI_MODEL        = "gemini_model"
    private val KEY_AIRA_GEMINI_PRIMARY = "aira_gemini_primary"

    // ── Appearance ───────────────────────────────────────────────────────────
    val theme: Flow<String>         = store.stringFlow(KEY_THEME, THEME_SYSTEM)
    val dynamicColor: Flow<Boolean> = store.booleanFlow(KEY_DYNAMIC_COLOR, false)
    val highContrast: Flow<Boolean> = store.booleanFlow(KEY_HIGH_CONTRAST, false)
    val reduceMotion: Flow<Boolean> = store.booleanFlow(KEY_REDUCE_MOTION, false)
    val fontScale: Flow<String>     = store.stringFlow(KEY_FONT_SCALE, FONT_NORMAL)

    // ── Security ──────────────────────────────────────────────────────────────
    val vaultAutoLockMinutes: Flow<Int> = store.intFlow(KEY_VAULT_AUTO_LOCK_MIN, VAULT_LOCK_DEFAULT)

    // ── Haptics ───────────────────────────────────────────────────────────────
    val touchVibration: Flow<Boolean> = store.booleanFlow(KEY_TOUCH_VIBRATION, true)

    // ── Feature: TTS ──────────────────────────────────────────────────────────
    val ttsDefaultRate: Flow<Float> = store.floatFlow(KEY_TTS_SPEECH_RATE, 1.0f)
    val ttsDefaultLanguage: Flow<String> = store.stringFlow(KEY_TTS_LANGUAGE, TTS_LANG_AUTO)
    val ttsScreenReaderMode: Flow<String> = store.stringFlow(KEY_TTS_SCREEN_READER_MODE, TTS_MODE_AUTO)
    val ttsVoiceSelection: Flow<String> = store.stringFlow(KEY_TTS_VOICE_SELECTION, "")
    val ttsPitch: Flow<Float> = store.floatFlow(KEY_TTS_PITCH, 1.0f)
    val ttsSecondaryLanguage: Flow<String> = store.stringFlow(KEY_TTS_SECONDARY_LANG, TTS_LANG_AUTO)
    val ttsAutoStart: Flow<Boolean> = store.booleanFlow(KEY_TTS_AUTO_START, false)
    val ttsNotificationFilter: Flow<Boolean> = store.booleanFlow(KEY_TTS_NOTIFICATION_FILTER, true)
    val ttsWindowChangeDetection: Flow<Boolean> = store.booleanFlow(KEY_TTS_WINDOW_CHANGE, true)
    val ttsFocusTracking: Flow<Boolean> = store.booleanFlow(KEY_TTS_FOCUS_TRACKING, true)
    val ttsContinuousRead: Flow<Boolean> = store.booleanFlow(KEY_TTS_CONTINUOUS_READ, true)
    val ttsDuplicateFilter: Flow<Boolean> = store.booleanFlow(KEY_TTS_DUPLICATE_FILTER, true)

    // ── Feature: Morse Code ───────────────────────────────────────────────────
    val morseVibrationSpeed: Flow<String> = store.stringFlow(KEY_MORSE_VIB_SPEED, MORSE_SPEED_NORMAL)

    // ── Feature: Radio / IPTV ─────────────────────────────────────────────────
    val bufferQuality: Flow<String> = store.stringFlow(KEY_BUFFER_QUALITY, BUFFER_NORMAL)

    // ── Feature: Translator ───────────────────────────────────────────────────
    val translatorAutoDetect: Flow<Boolean> = store.booleanFlow(KEY_TRANSLATOR_AUTO, true)

    // ── Feature: Reminder ─────────────────────────────────────────────────────
    val reminderSnoozeMins: Flow<Int> = store.intFlow(KEY_REMINDER_SNOOZE_MIN, REMINDER_SNOOZE_DEFAULT)

    // ── Feature: Calculator ───────────────────────────────────────────────────
    val calculatorAngleUnit: Flow<String> = store.stringFlow(KEY_CALC_ANGLE_UNIT, ANGLE_DEG)

    // ── Sound Effects ─────────────────────────────────────────────────────────
    val soundEffectsEnabled: Flow<Boolean> = store.booleanFlow(KEY_SOUND_EFFECTS, SOUND_EFFECTS_DEFAULT)

    // ── NSE Voice Engine ──────────────────────────────────────────────────────
    val nseVoice: Flow<String> = store.stringFlow(KEY_NSE_VOICE, NSE_VOICE_AUTO)
    val predictivePreBuffer: Flow<Boolean> = store.booleanFlow(KEY_PREDICTIVE_BUFFER, false)
    val forceTalkBackRate: Flow<Boolean> = store.booleanFlow(KEY_FORCE_TALKBACK_RATE, false)

    // ── Aira AI / Gemini ──────────────────────────────────────────────────────
    val geminiApiKey: Flow<String>      = store.stringFlow(KEY_GEMINI_API_KEY, "")
    val geminiModel: Flow<String>       = store.stringFlow(KEY_GEMINI_MODEL, GEMINI_MODEL_FLASH)
    val airaGeminiPrimary: Flow<Boolean> = store.booleanFlow(KEY_AIRA_GEMINI_PRIMARY, false)

    // ── Setters ───────────────────────────────────────────────────────────────
    suspend fun setTheme(v: String)                = store.setString(KEY_THEME, v)
    suspend fun setDynamicColor(v: Boolean)        = store.setBoolean(KEY_DYNAMIC_COLOR, v)
    suspend fun setHighContrast(v: Boolean)        = store.setBoolean(KEY_HIGH_CONTRAST, v)
    suspend fun setReduceMotion(v: Boolean)        = store.setBoolean(KEY_REDUCE_MOTION, v)
    suspend fun setFontScale(v: String)            = store.setString(KEY_FONT_SCALE, v)
    suspend fun setVaultAutoLockMinutes(v: Int)    = store.setInt(KEY_VAULT_AUTO_LOCK_MIN, v)
    suspend fun setTouchVibration(v: Boolean)      = store.setBoolean(KEY_TOUCH_VIBRATION, v)
    suspend fun setTtsDefaultRate(v: Float)        = store.setFloat(KEY_TTS_SPEECH_RATE, v)
    suspend fun setTtsDefaultLanguage(v: String)   = store.setString(KEY_TTS_LANGUAGE, v)
    suspend fun setTtsScreenReaderMode(v: String)   = store.setString(KEY_TTS_SCREEN_READER_MODE, v)
    suspend fun setTtsVoiceSelection(v: String)    = store.setString(KEY_TTS_VOICE_SELECTION, v)
    suspend fun setTtsPitch(v: Float)              = store.setFloat(KEY_TTS_PITCH, v)
    suspend fun setTtsSecondaryLanguage(v: String) = store.setString(KEY_TTS_SECONDARY_LANG, v)
    suspend fun setTtsAutoStart(v: Boolean)        = store.setBoolean(KEY_TTS_AUTO_START, v)
    suspend fun setTtsNotificationFilter(v: Boolean) = store.setBoolean(KEY_TTS_NOTIFICATION_FILTER, v)
    suspend fun setTtsWindowChange(v: Boolean)     = store.setBoolean(KEY_TTS_WINDOW_CHANGE, v)
    suspend fun setTtsFocusTracking(v: Boolean)    = store.setBoolean(KEY_TTS_FOCUS_TRACKING, v)
    suspend fun setTtsContinuousRead(v: Boolean)   = store.setBoolean(KEY_TTS_CONTINUOUS_READ, v)
    suspend fun setTtsDuplicateFilter(v: Boolean)  = store.setBoolean(KEY_TTS_DUPLICATE_FILTER, v)
    suspend fun setMorseVibrationSpeed(v: String)  = store.setString(KEY_MORSE_VIB_SPEED, v)
    suspend fun setBufferQuality(v: String)        = store.setString(KEY_BUFFER_QUALITY, v)
    suspend fun setTranslatorAutoDetect(v: Boolean)= store.setBoolean(KEY_TRANSLATOR_AUTO, v)
    suspend fun setReminderSnoozeMins(v: Int)      = store.setInt(KEY_REMINDER_SNOOZE_MIN, v)
    suspend fun setCalculatorAngleUnit(v: String)  = store.setString(KEY_CALC_ANGLE_UNIT, v)
    suspend fun setSoundEffectsEnabled(v: Boolean) = store.setBoolean(KEY_SOUND_EFFECTS, v)
    suspend fun setPredictivePreBuffer(v: Boolean) = store.setBoolean(KEY_PREDICTIVE_BUFFER, v)
    suspend fun setForceTalkBackRate(v: Boolean) = store.setBoolean(KEY_FORCE_TALKBACK_RATE, v)
    suspend fun setNseVoice(v: String)             = store.setString(KEY_NSE_VOICE, v)
    suspend fun setGeminiApiKey(v: String)         = store.setString(KEY_GEMINI_API_KEY, v)
    suspend fun setGeminiModel(v: String)          = store.setString(KEY_GEMINI_MODEL, v)
    suspend fun setAiraGeminiPrimary(v: Boolean)   = store.setBoolean(KEY_AIRA_GEMINI_PRIMARY, v)
}
