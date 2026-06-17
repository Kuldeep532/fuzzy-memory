package com.nexuswavetech.nexusplus.platform

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "nexus_app_settings")

/**
 * Android implementation of [SettingsStore] using DataStore.
 */
actual class SettingsStore(private val context: Context) {

    actual fun stringFlow(key: String, default: String): Flow<String> =
        context.settingsDataStore.data.map { it[stringPreferencesKey(key)] ?: default }

    actual fun booleanFlow(key: String, default: Boolean): Flow<Boolean> =
        context.settingsDataStore.data.map { it[booleanPreferencesKey(key)] ?: default }

    actual fun intFlow(key: String, default: Int): Flow<Int> =
        context.settingsDataStore.data.map { it[intPreferencesKey(key)] ?: default }

    actual fun floatFlow(key: String, default: Float): Flow<Float> =
        context.settingsDataStore.data.map { it[floatPreferencesKey(key)] ?: default }

    actual fun stringSetFlow(key: String, default: Set<String>): Flow<Set<String>> =
        context.settingsDataStore.data.map { it[stringSetPreferencesKey(key)] ?: default }

    actual suspend fun setString(key: String, value: String) {
        context.settingsDataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    actual suspend fun setBoolean(key: String, value: Boolean) {
        context.settingsDataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    actual suspend fun setInt(key: String, value: Int) {
        context.settingsDataStore.edit { it[intPreferencesKey(key)] = value }
    }

    actual suspend fun setFloat(key: String, value: Float) {
        context.settingsDataStore.edit { it[floatPreferencesKey(key)] = value }
    }

    actual suspend fun setStringSet(key: String, value: Set<String>) {
        context.settingsDataStore.edit { it[stringSetPreferencesKey(key)] = value }
    }

    actual suspend fun remove(key: String) {
        context.settingsDataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    actual suspend fun clear() {
        context.settingsDataStore.edit { it.clear() }
    }

    actual suspend fun getStringSet(key: String, default: Set<String>): Set<String> =
        context.settingsDataStore.data.map { it[stringSetPreferencesKey(key)] ?: default }.first()

    actual suspend fun getString(key: String, default: String): String =
        context.settingsDataStore.data.map { it[stringPreferencesKey(key)] ?: default }.first()

    actual suspend fun getBoolean(key: String, default: Boolean): Boolean =
        context.settingsDataStore.data.map { it[booleanPreferencesKey(key)] ?: default }.first()

    actual suspend fun getInt(key: String, default: Int): Int =
        context.settingsDataStore.data.map { it[intPreferencesKey(key)] ?: default }.first()

    actual suspend fun getFloat(key: String, default: Float): Float =
        context.settingsDataStore.data.map { it[floatPreferencesKey(key)] ?: default }.first()
}
