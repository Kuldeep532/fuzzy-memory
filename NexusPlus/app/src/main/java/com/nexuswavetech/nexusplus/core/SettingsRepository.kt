package com.nexuswavetech.nexusplus.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "nexus_app_settings")

/**
 * Persists user-configurable app settings (theme, accessibility, security, features).
 * Backed by DataStore — process-scoped, coroutine-safe.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_THEME               = stringPreferencesKey("app_theme")
        private val KEY_DYNAMIC_COLOR       = booleanPreferencesKey("dynamic_color")
        private val KEY_HIGH_CONTRAST       = booleanPreferencesKey("high_contrast")
        private val KEY_REDUCE_MOTION       = booleanPreferencesKey("reduce_motion")
        private val KEY_FONT_SCALE          = stringPreferencesKey("font_scale")

        // Security
        private val KEY_VAULT_AUTO_LOCK_MIN = intPreferencesKey("vault_auto_lock_minutes")

        // Haptics
        private val KEY_TOUCH_VIBRATION     = booleanPreferencesKey("touch_vibration")

        // Feature — TTS
        private val KEY_TTS_SPEECH_RATE     = floatPreferencesKey("tts_speech_rate")
        private val KEY_TTS_LANGUAGE        = stringPreferencesKey("tts_language")
        private val KEY_TTS_SCREEN_READER_MODE = stringPreferencesKey("tts_screen_reader_mode")
        private val KEY_TTS_VOICE_SELECTION = stringPreferencesKey("tts_voice_selection")
        private val KEY_TTS_PITCH           = floatPreferencesKey("tts_pitch")
        private val KEY_TTS_SECONDARY_LANG  = stringPreferencesKey("tts_secondary_language")
        private val KEY_TTS_AUTO_START      = booleanPreferencesKey("tts_auto_start")
        private val KEY_TTS_NOTIFICATION_FILTER = booleanPreferencesKey("tts_notification_filter")
        private val KEY_TTS_WINDOW_CHANGE   = booleanPreferencesKey("tts_window_change")
        private val KEY_TTS_FOCUS_TRACKING  = booleanPreferencesKey("tts_focus_tracking")
        private val KEY_TTS_CONTINUOUS_READ = booleanPreferencesKey("tts_continuous_read")
        private val KEY_TTS_DUPLICATE_FILTER = booleanPreferencesKey("tts_duplicate_filter")

        // Feature — Morse Code
        private val KEY_MORSE_VIB_SPEED     = stringPreferencesKey("morse_vibration_speed")

        // Feature — Radio / IPTV
        private val KEY_BUFFER_QUALITY      = stringPreferencesKey("buffer_quality")

        // Feature — Translator
        private val KEY_TRANSLATOR_AUTO     = booleanPreferencesKey("translator_auto_detect")

        // Feature — Reminder
        private val KEY_REMINDER_SNOOZE_MIN = intPreferencesKey("reminder_snooze_minutes")

        // Feature — Calculator
        private val KEY_CALC_ANGLE_UNIT     = stringPreferencesKey("calculator_angle_unit")

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
    }

    // ── Appearance ────────────────────────────────────────────────────────────
    val theme: Flow<String>         = context.settingsDataStore.data.map { it[KEY_THEME] ?: THEME_SYSTEM }
    val dynamicColor: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: false }
    val highContrast: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_HIGH_CONTRAST] ?: false }
    val reduceMotion: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_REDUCE_MOTION] ?: false }
    val fontScale: Flow<String>     = context.settingsDataStore.data.map { it[KEY_FONT_SCALE] ?: FONT_NORMAL }

    // ── Security ──────────────────────────────────────────────────────────────
    /** Vault auto-lock timeout in minutes. 0 = never auto-lock. */
    val vaultAutoLockMinutes: Flow<Int> = context.settingsDataStore.data.map {
        it[KEY_VAULT_AUTO_LOCK_MIN] ?: VAULT_LOCK_DEFAULT
    }

    // ── Haptics ───────────────────────────────────────────────────────────────
    val touchVibration: Flow<Boolean> = context.settingsDataStore.data.map {
        it[KEY_TOUCH_VIBRATION] ?: true
    }

    // ── Feature: TTS ──────────────────────────────────────────────────────────
    val ttsDefaultRate: Flow<Float> = context.settingsDataStore.data.map {
        it[KEY_TTS_SPEECH_RATE] ?: 1.0f
    }
    val ttsDefaultLanguage: Flow<String> = context.settingsDataStore.data.map {
        it[KEY_TTS_LANGUAGE] ?: TTS_LANG_AUTO
    }
    val ttsScreenReaderMode: Flow<String> = context.settingsDataStore.data.map {
        it[KEY_TTS_SCREEN_READER_MODE] ?: TTS_MODE_AUTO
    }
    val ttsVoiceSelection: Flow<String> = context.settingsDataStore.data.map {
        it[KEY_TTS_VOICE_SELECTION] ?: ""
    }
    val ttsPitch: Flow<Float> = context.settingsDataStore.data.map {
        it[KEY_TTS_PITCH] ?: 1.0f
    }
    val ttsSecondaryLanguage: Flow<String> = context.settingsDataStore.data.map {
        it[KEY_TTS_SECONDARY_LANG] ?: TTS_LANG_AUTO
    }
    val ttsAutoStart: Flow<Boolean> = context.settingsDataStore.data.map {
        it[KEY_TTS_AUTO_START] ?: false
    }
    val ttsNotificationFilter: Flow<Boolean> = context.settingsDataStore.data.map {
        it[KEY_TTS_NOTIFICATION_FILTER] ?: true
    }
    val ttsWindowChangeDetection: Flow<Boolean> = context.settingsDataStore.data.map {
        it[KEY_TTS_WINDOW_CHANGE] ?: true
    }
    val ttsFocusTracking: Flow<Boolean> = context.settingsDataStore.data.map {
        it[KEY_TTS_FOCUS_TRACKING] ?: true
    }
    val ttsContinuousRead: Flow<Boolean> = context.settingsDataStore.data.map {
        it[KEY_TTS_CONTINUOUS_READ] ?: true
    }
    val ttsDuplicateFilter: Flow<Boolean> = context.settingsDataStore.data.map {
        it[KEY_TTS_DUPLICATE_FILTER] ?: true
    }

    // ── Feature: Morse Code ───────────────────────────────────────────────────
    val morseVibrationSpeed: Flow<String> = context.settingsDataStore.data.map {
        it[KEY_MORSE_VIB_SPEED] ?: MORSE_SPEED_NORMAL
    }

    // ── Feature: Radio / IPTV ─────────────────────────────────────────────────
    val bufferQuality: Flow<String> = context.settingsDataStore.data.map {
        it[KEY_BUFFER_QUALITY] ?: BUFFER_NORMAL
    }

    // ── Feature: Translator ───────────────────────────────────────────────────
    val translatorAutoDetect: Flow<Boolean> = context.settingsDataStore.data.map {
        it[KEY_TRANSLATOR_AUTO] ?: true
    }

    // ── Feature: Reminder ─────────────────────────────────────────────────────
    val reminderSnoozeMins: Flow<Int> = context.settingsDataStore.data.map {
        it[KEY_REMINDER_SNOOZE_MIN] ?: REMINDER_SNOOZE_DEFAULT
    }

    // ── Feature: Calculator ───────────────────────────────────────────────────
    val calculatorAngleUnit: Flow<String> = context.settingsDataStore.data.map {
        it[KEY_CALC_ANGLE_UNIT] ?: ANGLE_DEG
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    suspend fun setTheme(v: String)                { context.settingsDataStore.edit { it[KEY_THEME] = v } }
    suspend fun setDynamicColor(v: Boolean)        { context.settingsDataStore.edit { it[KEY_DYNAMIC_COLOR] = v } }
    suspend fun setHighContrast(v: Boolean)        { context.settingsDataStore.edit { it[KEY_HIGH_CONTRAST] = v } }
    suspend fun setReduceMotion(v: Boolean)        { context.settingsDataStore.edit { it[KEY_REDUCE_MOTION] = v } }
    suspend fun setFontScale(v: String)            { context.settingsDataStore.edit { it[KEY_FONT_SCALE] = v } }
    suspend fun setVaultAutoLockMinutes(v: Int)    { context.settingsDataStore.edit { it[KEY_VAULT_AUTO_LOCK_MIN] = v } }
    suspend fun setTouchVibration(v: Boolean)      { context.settingsDataStore.edit { it[KEY_TOUCH_VIBRATION] = v } }
    suspend fun setTtsDefaultRate(v: Float)        { context.settingsDataStore.edit { it[KEY_TTS_SPEECH_RATE] = v } }
    suspend fun setTtsDefaultLanguage(v: String)   { context.settingsDataStore.edit { it[KEY_TTS_LANGUAGE] = v } }
    suspend fun setTtsScreenReaderMode(v: String) { context.settingsDataStore.edit { it[KEY_TTS_SCREEN_READER_MODE] = v } }
    suspend fun setTtsVoiceSelection(v: String)   { context.settingsDataStore.edit { it[KEY_TTS_VOICE_SELECTION] = v } }
    suspend fun setTtsPitch(v: Float)              { context.settingsDataStore.edit { it[KEY_TTS_PITCH] = v } }
    suspend fun setTtsSecondaryLanguage(v: String) { context.settingsDataStore.edit { it[KEY_TTS_SECONDARY_LANG] = v } }
    suspend fun setTtsAutoStart(v: Boolean)        { context.settingsDataStore.edit { it[KEY_TTS_AUTO_START] = v } }
    suspend fun setTtsNotificationFilter(v: Boolean) { context.settingsDataStore.edit { it[KEY_TTS_NOTIFICATION_FILTER] = v } }
    suspend fun setTtsWindowChange(v: Boolean)     { context.settingsDataStore.edit { it[KEY_TTS_WINDOW_CHANGE] = v } }
    suspend fun setTtsFocusTracking(v: Boolean)   { context.settingsDataStore.edit { it[KEY_TTS_FOCUS_TRACKING] = v } }
    suspend fun setTtsContinuousRead(v: Boolean)  { context.settingsDataStore.edit { it[KEY_TTS_CONTINUOUS_READ] = v } }
    suspend fun setTtsDuplicateFilter(v: Boolean) { context.settingsDataStore.edit { it[KEY_TTS_DUPLICATE_FILTER] = v } }
    suspend fun setMorseVibrationSpeed(v: String)  { context.settingsDataStore.edit { it[KEY_MORSE_VIB_SPEED] = v } }
    suspend fun setBufferQuality(v: String)        { context.settingsDataStore.edit { it[KEY_BUFFER_QUALITY] = v } }
    suspend fun setTranslatorAutoDetect(v: Boolean){ context.settingsDataStore.edit { it[KEY_TRANSLATOR_AUTO] = v } }
    suspend fun setReminderSnoozeMins(v: Int)      { context.settingsDataStore.edit { it[KEY_REMINDER_SNOOZE_MIN] = v } }
    suspend fun setCalculatorAngleUnit(v: String)  { context.settingsDataStore.edit { it[KEY_CALC_ANGLE_UNIT] = v } }
}
