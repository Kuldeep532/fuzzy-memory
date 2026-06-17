package com.nexuswavetech.nexusplus.core.registry

import com.nexuswavetech.nexusplus.core.FeatureId
import com.nexuswavetech.nexusplus.core.FeatureItem

/**
 * FeatureRegistry — dynamic, reflection-free feature registry.
 *
 * Priority 4: Eliminates hardcoded feature counts, enables runtime discovery
 * of features across all hubs. Each feature self-registers via its companion.
 *
 * All feature counts are derived from the registry, not hardcoded.
 */
object FeatureRegistry {

    private val all = mutableMapOf<String, FeatureItem>()
    private val byCategory = mutableMapOf<String, MutableList<String>>()
    private val byHub = mutableMapOf<String, MutableList<String>>()

    /** Register a feature. Called by each feature's companion on init or by a build plugin. */
    fun register(feature: FeatureItem) {
        all[feature.id] = feature
        byCategory.getOrPut(feature.category) { mutableListOf() }.add(feature.id)
        if (feature.hub != null) {
            byHub.getOrPut(feature.hub) { mutableListOf() }.add(feature.id)
        }
    }

    /** Register many at once (e.g., from a generated list). */
    fun registerAll(features: List<FeatureItem>) {
        features.forEach(::register)
    }

    /** Get a feature by its ID. */
    operator fun get(id: String): FeatureItem? = all[id]

    /** Get all registered features. */
    fun allFeatures(): List<FeatureItem> = all.values.toList()

    /** Get features by category. */
    fun byCategory(category: String): List<FeatureItem> =
        byCategory[category]?.mapNotNull { all[it] } ?: emptyList()

    /** Get features by hub. */
    fun byHub(hub: String): List<FeatureItem> =
        byHub[hub]?.mapNotNull { all[it] } ?: emptyList()

    /** Count of all features — no hardcoding. */
    val totalCount: Int get() = all.size

    /** Count by category — no hardcoding. */
    fun countByCategory(category: String): Int = byCategory[category]?.size ?: 0

    /** Count by hub — no hardcoding. */
    fun countByHub(hub: String): Int = byHub[hub]?.size ?: 0

    /** Search features by name or description. */
    fun search(query: String): List<FeatureItem> {
        val q = query.lowercase()
        return all.values.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    /** Clear all registrations (for testing). */
    fun clear() {
        all.clear()
        byCategory.clear()
        byHub.clear()
    }
}

/** Convenience: get the count of all features as a string for UI. */
fun featureCountString(): String = FeatureRegistry.totalCount.toString()
