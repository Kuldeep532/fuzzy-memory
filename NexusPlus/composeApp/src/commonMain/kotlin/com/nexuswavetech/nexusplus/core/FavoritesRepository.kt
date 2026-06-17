package com.nexuswavetech.nexusplus.core

import com.nexuswavetech.nexusplus.platform.SettingsStore
import kotlinx.coroutines.flow.Flow

/**
 * Persists both the user's **favorites** (bookmarks) and **pinned** items
 * (Home-screen tiles) separately within the same SettingsStore.
 *
 * Distinction:
 *  - Favorites  → bookmarked for quick reference; appear in Favorites row.
 *  - Pinned     → promoted to the Home dashboard; appear in Pinned row.
 */
class FavoritesRepository(private val store: SettingsStore) {

    companion object {
        private const val FAVORITES_KEY = "favorite_feature_ids"
        private const val PINNED_KEY    = "pinned_feature_ids"
    }

    // ── Favorites ───────────────────────────────────────────────────────────

    val favoriteIds: Flow<Set<String>> = store.stringSetFlow(FAVORITES_KEY, emptySet())

    suspend fun toggleFavorite(featureId: FeatureId) {
        val current = store.getStringSet(FAVORITES_KEY, emptySet())
        store.setStringSet(FAVORITES_KEY, if (featureId.name in current) current - featureId.name else current + featureId.name)
    }

    suspend fun addFavorite(featureId: FeatureId) {
        val current = store.getStringSet(FAVORITES_KEY, emptySet())
        store.setStringSet(FAVORITES_KEY, current + featureId.name)
    }

    suspend fun removeFavorite(featureId: FeatureId) {
        val current = store.getStringSet(FAVORITES_KEY, emptySet())
        store.setStringSet(FAVORITES_KEY, current - featureId.name)
    }

    // ── Pinned ───────────────────────────────────────────────────────────────

    val pinnedIds: Flow<Set<String>> = store.stringSetFlow(PINNED_KEY, emptySet())

    suspend fun togglePin(featureId: FeatureId) {
        val current = store.getStringSet(PINNED_KEY, emptySet())
        store.setStringSet(PINNED_KEY, if (featureId.name in current) current - featureId.name else current + featureId.name)
    }

    suspend fun pinFeature(featureId: FeatureId) {
        val current = store.getStringSet(PINNED_KEY, emptySet())
        store.setStringSet(PINNED_KEY, current + featureId.name)
    }

    suspend fun unpinFeature(featureId: FeatureId) {
        val current = store.getStringSet(PINNED_KEY, emptySet())
        store.setStringSet(PINNED_KEY, current - featureId.name)
    }

    // ── Clear all ───────────────────────────────────────────────────────────────

    suspend fun clearAll() {
        store.setStringSet(FAVORITES_KEY, emptySet())
        store.setStringSet(PINNED_KEY, emptySet())
    }
}
