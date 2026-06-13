package com.nexuswavetech.nexusplus.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentDataStore by preferencesDataStore(name = "nexus_recent_activity")

/**
 * Persists the user's recently-visited feature IDs (max 10).
 * Backed by DataStore — safe to read from coroutines, Flow-friendly.
 */
class RecentActivityRepository(private val context: Context) {

    companion object {
        private val RECENT_KEY = stringPreferencesKey("recent_feature_ids")
        private const val MAX_RECENT = 10
        private const val SEP = ","
    }

    val recentIds: Flow<List<String>> = context.recentDataStore.data.map { prefs ->
        (prefs[RECENT_KEY] ?: "").split(SEP).filter { it.isNotBlank() }
    }

    suspend fun recordVisit(featureId: FeatureId) {
        context.recentDataStore.edit { prefs ->
            val current = (prefs[RECENT_KEY] ?: "")
                .split(SEP)
                .filter { it.isNotBlank() }
                .toMutableList()
            current.remove(featureId.name)
            current.add(0, featureId.name)
            prefs[RECENT_KEY] = current.take(MAX_RECENT).joinToString(SEP)
        }
    }

    suspend fun clearHistory() {
        context.recentDataStore.edit { it[RECENT_KEY] = "" }
    }
}
