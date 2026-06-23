package com.nexuswavetech.nexusplus.platform

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic key-value settings store.
 * Android implementation uses DataStore; iOS uses NSUserDefaults.
 */
expect class SettingsStore {
    /**
     * Returns a Flow that emits the string value for [key], or [default] if not set.
     */
    fun stringFlow(key: String, default: String): Flow<String>

    /**
     * Returns a Flow that emits the boolean value for [key], or [default] if not set.
     */
    fun booleanFlow(key: String, default: Boolean): Flow<Boolean>

    /**
     * Returns a Flow that emits the int value for [key], or [default] if not set.
     */
    fun intFlow(key: String, default: Int): Flow<Int>

    /**
     * Returns a Flow that emits the float value for [key], or [default] if not set.
     */
    fun floatFlow(key: String, default: Float): Flow<Float>

    /**
     * Returns a Flow that emits the string set for [key], or [default] if not set.
     */
    fun stringSetFlow(key: String, default: Set<String>): Flow<Set<String>>

    /**
     * Suspend function to set a string value.
     */
    suspend fun setString(key: String, value: String)

    /**
     * Suspend function to set a boolean value.
     */
    suspend fun setBoolean(key: String, value: Boolean)

    /**
     * Suspend function to set an int value.
     */
    suspend fun setInt(key: String, value: Int)

    /**
     * Suspend function to set a float value.
     */
    suspend fun setFloat(key: String, value: Float)

    /**
     * Suspend function to set a string set.
     */
    suspend fun setStringSet(key: String, value: Set<String>)

    /**
     * Suspend function to remove a key.
     */
    suspend fun remove(key: String)

    /**
     * Suspend function to clear all settings.
     */
    suspend fun clear()

    /**
     * Read the current string set value synchronously (blocking).
     * Android uses runBlocking { dataStore.data.first() }; iOS reads NSUserDefaults directly.
     */
    suspend fun getStringSet(key: String, default: Set<String>): Set<String>

    /**
     * Read the current string value synchronously.
     */
    suspend fun getString(key: String, default: String): String

    /**
     * Read the current boolean value synchronously.
     */
    suspend fun getBoolean(key: String, default: Boolean): Boolean

    /**
     * Read the current int value synchronously.
     */
    suspend fun getInt(key: String, default: Int): Int

    /**
     * Read the current float value synchronously.
     */
    suspend fun getFloat(key: String, default: Float): Float
}
