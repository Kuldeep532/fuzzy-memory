package com.nexuswavetech.nexusplus.core

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data model for a single feature entry shown in All Features and Favorites.
 */
data class FeatureItem(
    val id: FeatureId,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val category: FeatureCategory,
    val isFavorite: Boolean = false
)

enum class FeatureCategory(val label: String) {
    MEDIA("Media & Entertainment"),
    PRODUCTIVITY("Productivity"),
    UTILITIES("Utilities"),
    TOOLS("Smart Tools"),
    SECURITY("Security & Privacy")
}
