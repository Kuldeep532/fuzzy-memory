package com.nexuswavetech.nexusplus.features.tts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NSE 3.0 — In-memory PCM audio LRU cache.
 *
 * Stores synthesized PCM data keyed by (locale + rate + pitch + text-hash).
 * Avoids re-synthesis of repeated phrases — nav labels, notification texts,
 * commonly spoken words, etc., play instantly from cache.
 *
 * Uses a LinkedHashMap in access-order mode for O(1) LRU eviction.
 * All operations are synchronized.
 */
class NsePcmCache(private val maxEntries: Int = 60) {

    data class Entry(
        val pcm: ByteArray,
        val sampleRate: Int,
        val channelConfig: Int,
        val encoding: Int,
        val byteCount: Int = pcm.size,
    )

    private val _size = MutableStateFlow(0)
    /** Observable count of cached phrases. */
    val size: StateFlow<Int> = _size.asStateFlow()

    private val map = object : LinkedHashMap<String, Entry>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Entry>?) = size > maxEntries
    }

    /** Build a cache key from synthesis parameters + text. */
    fun key(text: String, localeTag: String, rate: Float, pitch: Float): String {
        val textHash = text.take(120).hashCode()
        val rateKey  = "%.2f".format(rate)
        val pitchKey = "%.2f".format(pitch)
        return "${localeTag}_${rateKey}_${pitchKey}_$textHash"
    }

    @Synchronized
    fun get(key: String): Entry? = map[key]

    @Synchronized
    fun put(key: String, entry: Entry) {
        map[key] = entry
        _size.value = map.size
    }

    @Synchronized
    fun clear() {
        map.clear()
        _size.value = 0
    }

    @Synchronized
    fun entryCount(): Int = map.size

    /** Approximate total PCM bytes held in cache. */
    @Synchronized
    fun totalBytes(): Long = map.values.sumOf { it.byteCount.toLong() }
}
