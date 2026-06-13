package com.nexuswavetech.nexusplus.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "nexus_app_settings")

/**
 * Persists user-configurable app settings (theme, accessibility, etc.).
 * Backed by DataStore — process-scoped, coroutine-safe.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_THEME         = stringPreferencesKey("app_theme")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        private val KEY_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        private val KEY_FONT_SCALE    = stringPreferencesKey("font_scale")

        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_DARK   = "DARK"
        const val THEME_LIGHT  = "LIGHT"

        const val FONT_NORMAL = "NORMAL"
        const val FONT_LARGE  = "LARGE"
        const val FONT_XLARGE = "XLARGE"
    }

    val theme: Flow<String>         = context.settingsDataStore.data.map { it[KEY_THEME] ?: THEME_SYSTEM }
    val dynamicColor: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: false }
    val highContrast: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_HIGH_CONTRAST] ?: false }
    val reduceMotion: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_REDUCE_MOTION] ?: false }
    val fontScale: Flow<String>     = context.settingsDataStore.data.map { it[KEY_FONT_SCALE] ?: FONT_NORMAL }

    suspend fun setTheme(v: String)         { context.settingsDataStore.edit { it[KEY_THEME] = v } }
    suspend fun setDynamicColor(v: Boolean) { context.settingsDataStore.edit { it[KEY_DYNAMIC_COLOR] = v } }
    suspend fun setHighContrast(v: Boolean) { context.settingsDataStore.edit { it[KEY_HIGH_CONTRAST] = v } }
    suspend fun setReduceMotion(v: Boolean) { context.settingsDataStore.edit { it[KEY_REDUCE_MOTION] = v } }
    suspend fun setFontScale(v: String)     { context.settingsDataStore.edit { it[KEY_FONT_SCALE] = v } }
}
