package com.nexuswavetech.nexusplus.core

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data model for a single feature entry in the catalog.
 * All fields are searchable — name, description, and keywords are indexed
 * by [SearchManager] for global search.
 */
data class FeatureItem(
    val id: FeatureId,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val category: FeatureCategory,
    val keywords: List<String> = emptyList(),
    val isFavorite: Boolean = false,
)

enum class FeatureCategory(val label: String) {
    MEDIA("Media & Entertainment"),
    PRODUCTIVITY("Productivity"),
    UTILITIES("Utilities"),
    TOOLS("Smart Tools"),
    SECURITY("Security & Privacy"),
}

/** Maps each [FeatureCategory] to its parent [FeatureHub]. */
fun FeatureCategory.toHub(): FeatureHub = when (this) {
    FeatureCategory.SECURITY     -> FeatureHub.SECURITY
    FeatureCategory.PRODUCTIVITY -> FeatureHub.DOCUMENTS
    FeatureCategory.MEDIA        -> FeatureHub.MEDIA
    FeatureCategory.UTILITIES    -> FeatureHub.UTILITIES
    FeatureCategory.TOOLS        -> FeatureHub.UTILITIES
}
