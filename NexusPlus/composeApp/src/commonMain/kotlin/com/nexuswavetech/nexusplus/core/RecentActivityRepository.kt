package com.nexuswavetech.nexusplus.core

import com.nexuswavetech.nexusplus.platform.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the user's recently-visited features (max 10) and all-time visit
 * counts per feature.
 *
 * Both are stored in the same SettingsStore to keep edit operations atomic.
 */
class RecentActivityRepository(private val store: SettingsStore) {

    companion object {
        private const val RECENT_KEY      = "recent_feature_ids"
        private const val VISIT_COUNT_KEY = "visit_counts"
        private const val MAX_RECENT = 10
        private const val SEP       = ","
        private const val PAIR_SEP  = ";"
        private const val KV_SEP    = ":"
    }

    // ── Recent list (ordered, newest first) ──────────────────────────────────

    val recentIds: Flow<List<String>> = store.stringFlow(RECENT_KEY, "")
        .map { it.split(SEP).filter { id -> id.isNotBlank() } }

    // ── Visit counts (map: featureId → count) ──────────────────────────

    val visitCounts: Flow<Map<String, Int>> = store.stringFlow(VISIT_COUNT_KEY, "")
        .map { decodeCountMap(it) }

    /** Top features sorted by total visit count. */
    val mostUsedIds: Flow<List<String>> = visitCounts.map { counts ->
        counts.entries
            .sortedByDescending { it.value }
            .take(MAX_RECENT)
            .map { it.key }
    }

    // ── Mutations ───────────────────────────────────────────────────────────

    suspend fun recordVisit(featureId: FeatureId) {
        // 1. Update recency list
        val recentStr = store.getString(RECENT_KEY, "")
        val recent = recentStr.split(SEP).filter { it.isNotBlank() }.toMutableList()
        recent.remove(featureId.name)
        recent.add(0, featureId.name)
        store.setString(RECENT_KEY, recent.take(MAX_RECENT).joinToString(SEP))

        // 2. Increment visit count
        val countsStr = store.getString(VISIT_COUNT_KEY, "")
        val counts = decodeCountMap(countsStr).toMutableMap()
        counts[featureId.name] = (counts[featureId.name] ?: 0) + 1
        store.setString(VISIT_COUNT_KEY, encodeCountMap(counts))
    }

    suspend fun clearHistory() {
        store.setString(RECENT_KEY, "")
        store.setString(VISIT_COUNT_KEY, "")
    }

    // ── Private helpers ─────────────────────────────────────────────────────────────

    private fun decodeCountMap(encoded: String): Map<String, Int> {
        if (encoded.isBlank()) return emptyMap()
        return encoded.split(PAIR_SEP).mapNotNull { pair ->
            val parts = pair.split(KV_SEP)
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 1) else null
        }.toMap()
    }

    private fun encodeCountMap(map: Map<String, Int>): String =
        map.entries.joinToString(PAIR_SEP) { "${it.key}${KV_SEP}${it.value}" }
}
