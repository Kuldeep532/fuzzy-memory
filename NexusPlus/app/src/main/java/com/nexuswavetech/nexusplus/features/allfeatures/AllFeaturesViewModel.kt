package com.nexuswavetech.nexusplus.features.allfeatures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AllFeaturesUiState(
    val features: List<FeatureItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: FeatureCategory? = null,
    val gatekeeperBlocked: String? = null,
    val pendingRoute: String? = null,
)

class AllFeaturesViewModel(
    private val sessionManager: SessionManager,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllFeaturesUiState())
    val uiState: StateFlow<AllFeaturesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                favoritesRepository.favoriteIds,
                favoritesRepository.pinnedIds,
            ) { favs, pins -> favs to pins }
                .collect { (favoriteIds, pinnedIds) ->
                    val enriched = FeatureCatalog.allFeatures.map {
                        it.copy(
                            isFavorite = it.id.name in favoriteIds,
                            isPinned   = it.id.name in pinnedIds,
                        )
                    }
                    _uiState.update { s ->
                        s.copy(features = filterFeatures(enriched, s.searchQuery, s.selectedCategory))
                    }
                }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { s ->
            s.copy(
                searchQuery = query,
                features    = filterFeatures(s.features, query, s.selectedCategory),
            )
        }
    }

    fun onCategorySelected(category: FeatureCategory?) {
        _uiState.update { s ->
            s.copy(
                selectedCategory = category,
                features         = filterFeatures(s.features, s.searchQuery, category),
            )
        }
    }

    fun onFeatureTapped(feature: FeatureItem) {
        val result = NexusGatekeeper.checkAccess(
            feature.id, sessionManager.currentSession(), feature.name
        )
        when (result) {
            is NexusGatekeeper.AccessResult.Allowed -> _uiState.update { it.copy(pendingRoute = feature.route) }
            is NexusGatekeeper.AccessResult.Blocked -> _uiState.update { it.copy(gatekeeperBlocked = result.featureName) }
        }
    }

    fun onToggleFavorite(feature: FeatureItem) {
        viewModelScope.launch { favoritesRepository.toggleFavorite(feature.id) }
    }

    fun onTogglePin(feature: FeatureItem) {
        viewModelScope.launch { favoritesRepository.togglePin(feature.id) }
    }

    fun onNavigationConsumed() {
        _uiState.update { it.copy(pendingRoute = null) }
    }

    fun onGatekeeperDismissed() {
        _uiState.update { it.copy(gatekeeperBlocked = null) }
    }

    private fun filterFeatures(
        features: List<FeatureItem>,
        query: String,
        category: FeatureCategory?,
    ): List<FeatureItem> = features.filter { f ->
        val matchesQuery = query.isBlank() ||
            f.name.contains(query, ignoreCase = true) ||
            f.description.contains(query, ignoreCase = true) ||
            f.keywords.any { it.contains(query, ignoreCase = true) }
        val matchesCategory = category == null || f.category == category
        matchesQuery && matchesCategory
    }
}
