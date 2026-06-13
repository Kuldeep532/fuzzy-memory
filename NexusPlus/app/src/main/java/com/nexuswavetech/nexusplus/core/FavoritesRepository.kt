package com.nexuswavetech.nexusplus.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nexus_favorites")

/**
 * Persists both the user's **favorites** (bookmarks) and **pinned** items
 * (Home-screen tiles) separately within the same DataStore.
 *
 * Distinction:
 *  - Favorites  → bookmarked for quick reference; appear in Favorites row.
 *  - Pinned     → promoted to the Home dashboard; appear in Pinned row.
 */
class FavoritesRepository(private val context: Context) {

    companion object {
        private val FAVORITES_KEY = stringSetPreferencesKey("favorite_feature_ids")
        private val PINNED_KEY    = stringSetPreferencesKey("pinned_feature_ids")
    }

    // ── Favorites ─────────────────────────────────────────────────────────

    val favoriteIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[FAVORITES_KEY] ?: emptySet()
    }

    suspend fun toggleFavorite(featureId: FeatureId) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            prefs[FAVORITES_KEY] = if (featureId.name in current)
                current - featureId.name else current + featureId.name
        }
    }

    suspend fun addFavorite(featureId: FeatureId) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            prefs[FAVORITES_KEY] = current + featureId.name
        }
    }

    suspend fun removeFavorite(featureId: FeatureId) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            prefs[FAVORITES_KEY] = current - featureId.name
        }
    }

    // ── Pinned ────────────────────────────────────────────────────────────

    val pinnedIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PINNED_KEY] ?: emptySet()
    }

    suspend fun togglePin(featureId: FeatureId) {
        context.dataStore.edit { prefs ->
            val current = prefs[PINNED_KEY] ?: emptySet()
            prefs[PINNED_KEY] = if (featureId.name in current)
                current - featureId.name else current + featureId.name
        }
    }

    suspend fun pinFeature(featureId: FeatureId) {
        context.dataStore.edit { prefs ->
            val current = prefs[PINNED_KEY] ?: emptySet()
            prefs[PINNED_KEY] = current + featureId.name
        }
    }

    suspend fun unpinFeature(featureId: FeatureId) {
        context.dataStore.edit { prefs ->
            val current = prefs[PINNED_KEY] ?: emptySet()
            prefs[PINNED_KEY] = current - featureId.name
        }
    }

    // ── Clear all ─────────────────────────────────────────────────────────

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs[FAVORITES_KEY] = emptySet()
            prefs[PINNED_KEY]    = emptySet()
        }
    }
}
