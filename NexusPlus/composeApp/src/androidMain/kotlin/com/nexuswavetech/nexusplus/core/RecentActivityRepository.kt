package com.nexuswavetech.nexusplus.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentDataStore by preferencesDataStore(name = "nexus_recent_activity")

/**
 * Persists the user's recently-visited features (max 10) and all-time visit
 * counts per feature.
 *
 * Both are stored in the same DataStore to keep edit operations atomic.
 */
class RecentActivityRepository(private val context: Context) {

    companion object {
        private val RECENT_KEY      = stringPreferencesKey("recent_feature_ids")
        private val VISIT_COUNT_KEY = stringPreferencesKey("visit_counts")
        private const val MAX_RECENT = 10
        private const val SEP       = ","
        private const val PAIR_SEP  = ";"
        private const val KV_SEP    = ":"
    }

    // ── Recent list (ordered, newest first) ───────────────────────────────

    val recentIds: Flow<List<String>> = context.recentDataStore.data.map { prefs ->
        (prefs[RECENT_KEY] ?: "").split(SEP).filter { it.isNotBlank() }
    }

    // ── Visit counts (map: featureId → count) ─────────────────────────────

    val visitCounts: Flow<Map<String, Int>> = context.recentDataStore.data.map { prefs ->
        decodeCountMap(prefs[VISIT_COUNT_KEY] ?: "")
    }

    /** Top features sorted by total visit count. */
    val mostUsedIds: Flow<List<String>> = visitCounts.map { counts ->
        counts.entries
            .sortedByDescending { it.value }
            .take(MAX_RECENT)
            .map { it.key }
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    suspend fun recordVisit(featureId: FeatureId) {
        context.recentDataStore.edit { prefs ->
            // 1. Update recency list
            val recent = (prefs[RECENT_KEY] ?: "")
                .split(SEP).filter { it.isNotBlank() }.toMutableList()
            recent.remove(featureId.name)
            recent.add(0, featureId.name)
            prefs[RECENT_KEY] = recent.take(MAX_RECENT).joinToString(SEP)

            // 2. Increment visit count
            val counts = decodeCountMap(prefs[VISIT_COUNT_KEY] ?: "").toMutableMap()
            counts[featureId.name] = (counts[featureId.name] ?: 0) + 1
            prefs[VISIT_COUNT_KEY] = encodeCountMap(counts)
        }
    }

    suspend fun clearHistory() {
        context.recentDataStore.edit { prefs ->
            prefs[RECENT_KEY]      = ""
            prefs[VISIT_COUNT_KEY] = ""
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

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
