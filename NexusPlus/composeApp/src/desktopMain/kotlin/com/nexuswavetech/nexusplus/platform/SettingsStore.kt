package com.nexuswavetech.nexusplus.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

/**
 * Desktop implementation of [SettingsStore] using java.util.prefs.Preferences.
 */
actual class SettingsStore {

    private val prefs: Preferences = Preferences.userRoot().node("nexusplus")

    actual fun stringFlow(key: String, default: String): Flow<String> =
        MutableStateFlow(prefs.get(key, default)).asStateFlow()

    actual fun booleanFlow(key: String, default: Boolean): Flow<Boolean> =
        MutableStateFlow(prefs.getBoolean(key, default)).asStateFlow()

    actual fun intFlow(key: String, default: Int): Flow<Int> =
        MutableStateFlow(prefs.getInt(key, default)).asStateFlow()

    actual fun floatFlow(key: String, default: Float): Flow<Float> =
        MutableStateFlow(prefs.getFloat(key, default)).asStateFlow()

    actual fun stringSetFlow(key: String, default: Set<String>): Flow<Set<String>> {
        val raw = prefs.get(key, "")
        val value = if (raw.isEmpty()) default else raw.split("\u0000").toSet()
        return MutableStateFlow(value).asStateFlow()
    }

    actual suspend fun setString(key: String, value: String) {
        prefs.put(key, value)
    }

    actual suspend fun setBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }

    actual suspend fun setInt(key: String, value: Int) {
        prefs.putInt(key, value)
    }

    actual suspend fun setFloat(key: String, value: Float) {
        prefs.putFloat(key, value)
    }

    actual suspend fun setStringSet(key: String, value: Set<String>) {
        prefs.put(key, value.joinToString("\u0000"))
    }

    actual suspend fun remove(key: String) {
        prefs.remove(key)
    }

    actual suspend fun clear() {
        prefs.clear()
    }

    actual suspend fun getStringSet(key: String, default: Set<String>): Set<String> {
        val raw = prefs.get(key, "")
        return if (raw.isEmpty()) default else raw.split("\u0000").toSet()
    }

    actual suspend fun getString(key: String, default: String): String =
        prefs.get(key, default)

    actual suspend fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    actual suspend fun getInt(key: String, default: Int): Int =
        prefs.getInt(key, default)

    actual suspend fun getFloat(key: String, default: Float): Float =
        prefs.getFloat(key, default)
}
