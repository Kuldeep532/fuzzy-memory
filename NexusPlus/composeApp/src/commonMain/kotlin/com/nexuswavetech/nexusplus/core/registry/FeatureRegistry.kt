package com.nexuswavetech.nexusplus.core.registry

import com.nexuswavetech.nexusplus.core.FeatureCategory
import com.nexuswavetech.nexusplus.core.FeatureHub
import com.nexuswavetech.nexusplus.core.FeatureId
import com.nexuswavetech.nexusplus.core.FeatureItem
import com.nexuswavetech.nexusplus.core.toHub

/**
 * FeatureRegistry — dynamic, reflection-free feature registry.
 *
 * All feature counts are derived from the registry at runtime — never hardcoded.
 * Keyed by [FeatureId] enum for type safety.
 */
object FeatureRegistry {

    private val all = mutableMapOf<FeatureId, FeatureItem>()

    fun register(feature: FeatureItem) {
        all[feature.id] = feature
    }

    fun registerAll(features: List<FeatureItem>) {
        features.forEach(::register)
    }

    operator fun get(id: FeatureId): FeatureItem? = all[id]

    fun allFeatures(): List<FeatureItem> = all.values.toList()

    fun byCategory(category: FeatureCategory): List<FeatureItem> =
        all.values.filter { it.category == category }

    fun byHub(hub: FeatureHub): List<FeatureItem> =
        all.values.filter { it.category.toHub() == hub }

    val totalCount: Int get() = all.size

    fun countByCategory(category: FeatureCategory): Int =
        all.values.count { it.category == category }

    fun countByHub(hub: FeatureHub): Int =
        all.values.count { it.category.toHub() == hub }

    fun search(query: String): List<FeatureItem> {
        val q = query.lowercase()
        return all.values.filter {
            it.name.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.keywords.any { kw -> kw.lowercase().contains(q) }
        }
    }

    fun clear() { all.clear() }
}

fun featureCountString(): String = FeatureRegistry.totalCount.toString()
