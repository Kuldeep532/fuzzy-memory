package com.nexuswavetech.nexusplus.core

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data model for a single feature entry in the catalog.
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
    val isPinned: Boolean = false,
    val isNew: Boolean = false,
)

enum class FeatureCategory(val label: String) {
    MEDIA("Media & Entertainment"),
    PRODUCTIVITY("Productivity"),
    UTILITIES("Utilities"),
    TOOLS("Smart Tools"),
    SECURITY("Security & Privacy"),
    SCIENCE("Science & Space"),
}
