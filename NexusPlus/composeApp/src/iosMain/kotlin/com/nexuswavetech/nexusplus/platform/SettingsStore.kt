package com.nexuswavetech.nexusplus.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of [SettingsStore] using NSUserDefaults.
 */
actual class SettingsStore {

    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun stringFlow(key: String, default: String): Flow<String> =
        MutableStateFlow(defaults.stringForKey(key) ?: default).asStateFlow()

    actual fun booleanFlow(key: String, default: Boolean): Flow<Boolean> =
        MutableStateFlow(defaults.boolForKey(key) ?: default).asStateFlow()

    actual fun intFlow(key: String, default: Int): Flow<Int> =
        MutableStateFlow((defaults.objectForKey(key) as? Long)?.toInt() ?: default).asStateFlow()

    actual fun floatFlow(key: String, default: Float): Flow<Float> =
        MutableStateFlow(defaults.objectForKey(key) as? Float ?: default).asStateFlow()

    actual fun stringSetFlow(key: String, default: Set<String>): Flow<Set<String>> =
        MutableStateFlow(
            (defaults.objectForKey(key) as? List<*>)?.filterIsInstance<String>()?.toSet() ?: default
        ).asStateFlow()

    actual suspend fun setString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    actual suspend fun setBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    actual suspend fun setInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    actual suspend fun setFloat(key: String, value: Float) {
        defaults.setFloat(value, forKey = key)
    }

    actual suspend fun setStringSet(key: String, value: Set<String>) {
        defaults.setObject(value.toList(), forKey = key)
    }

    actual suspend fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    actual suspend fun clear() {
        defaults.removePersistentDomainForName(defaults.bundleIdentifier ?: "")
    }

    actual suspend fun getStringSet(key: String, default: Set<String>): Set<String> =
        (defaults.objectForKey(key) as? List<*>)?.filterIsInstance<String>()?.toSet() ?: default

    actual suspend fun getString(key: String, default: String): String =
        defaults.stringForKey(key) ?: default

    actual suspend fun getBoolean(key: String, default: Boolean): Boolean =
        defaults.boolForKey(key) ?: default

    actual suspend fun getInt(key: String, default: Int): Int =
        (defaults.objectForKey(key) as? Long)?.toInt() ?: default

    actual suspend fun getFloat(key: String, default: Float): Float =
        defaults.objectForKey(key) as? Float ?: default
}
