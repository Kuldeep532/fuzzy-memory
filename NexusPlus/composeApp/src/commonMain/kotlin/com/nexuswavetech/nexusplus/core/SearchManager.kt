package com.nexuswavetech.nexusplus.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Singleton service that drives global feature search.
 * Every feature exposes id / title / description / keywords — all are indexed.
 */
class SearchManager {

    private val _query = MutableStateFlow("")
    val query: Flow<String> = _query.asStateFlow()

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun clear() {
        _query.value = ""
    }

    fun resultsFor(
        catalog: List<FeatureItem>,
        favoriteIds: Set<String>,
    ): Flow<List<FeatureItem>> = _query.map { q ->
        if (q.isBlank()) emptyList()
        else catalog
            .map { it.copy(isFavorite = it.id.name in favoriteIds) }
            .filter { feature ->
                feature.name.contains(q, ignoreCase = true) ||
                feature.description.contains(q, ignoreCase = true) ||
                feature.keywords.any { it.contains(q, ignoreCase = true) }
            }
    }
}
