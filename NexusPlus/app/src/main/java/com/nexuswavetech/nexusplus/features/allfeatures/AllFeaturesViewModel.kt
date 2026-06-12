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
    val gatekeeperBlocked: String? = null,  // non-null = show restriction dialog
    val pendingRoute: String? = null
)

class AllFeaturesViewModel(
    private val sessionManager: SessionManager,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllFeaturesUiState())
    val uiState: StateFlow<AllFeaturesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favoriteIds.collect { favoriteIds ->
                val allFeatures = FeatureCatalog.allFeatures.map { it.copy(isFavorite = it.id.name in favoriteIds) }
                _uiState.update { it.copy(features = filterFeatures(allFeatures, it.searchQuery, it.selectedCategory)) }
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { state ->
            val allWithFav = FeatureCatalog.allFeatures.map { feature ->
                feature.copy(isFavorite = state.features.find { it.id == feature.id }?.isFavorite ?: false)
            }
            state.copy(
                searchQuery = query,
                features = filterFeatures(allWithFav, query, state.selectedCategory)
            )
        }
    }

    fun onCategorySelected(category: FeatureCategory?) {
        _uiState.update { state ->
            val allWithFav = FeatureCatalog.allFeatures.map { feature ->
                feature.copy(isFavorite = state.features.find { it.id == feature.id }?.isFavorite ?: false)
            }
            state.copy(
                selectedCategory = category,
                features = filterFeatures(allWithFav, state.searchQuery, category)
            )
        }
    }

    fun onFeatureTapped(feature: FeatureItem) {
        val session = sessionManager.currentSession()
        val result = NexusGatekeeper.checkAccess(feature.id, session, feature.name)
        when (result) {
            is NexusGatekeeper.AccessResult.Allowed -> {
                _uiState.update { it.copy(pendingRoute = feature.route) }
            }
            is NexusGatekeeper.AccessResult.Blocked -> {
                _uiState.update { it.copy(gatekeeperBlocked = result.featureName) }
            }
        }
    }

    fun onNavigationConsumed() {
        _uiState.update { it.copy(pendingRoute = null) }
    }

    fun onGatekeeperDismissed() {
        _uiState.update { it.copy(gatekeeperBlocked = null) }
    }

    fun onToggleFavorite(feature: FeatureItem) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(feature.id)
        }
    }

    private fun filterFeatures(
        features: List<FeatureItem>,
        query: String,
        category: FeatureCategory?
    ): List<FeatureItem> {
        return features.filter { feature ->
            val matchesQuery = query.isBlank() ||
                feature.name.contains(query, ignoreCase = true) ||
                feature.description.contains(query, ignoreCase = true)
            val matchesCategory = category == null || feature.category == category
            matchesQuery && matchesCategory
        }
    }
}
