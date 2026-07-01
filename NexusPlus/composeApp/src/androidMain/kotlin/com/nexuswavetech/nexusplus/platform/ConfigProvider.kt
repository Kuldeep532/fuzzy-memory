package com.nexuswavetech.nexusplus.platform

import com.nexuswavetech.nexusplus.composeapp.BuildConfig

/**
 * Safe accessor for all injected secrets from BuildConfig.
 * Provides graceful fallbacks and validation.
 */
object ConfigProvider {

    /**
     * Google Sign-In Web Client ID
     * Injected from WEB_CLIENT_ID GitHub Secret via Gradle buildConfigField
     */
    val webClientId: String
        get() = BuildConfig.WEB_CLIENT_ID.trim()
            .takeIf { it.isNotBlank() && !it.startsWith("YOUR_") }
            ?: ""

    /**
     * Google Gemini API Key
     * Injected from GEMINI_API_KEY GitHub Secret
     */
    val geminiApiKey: String
        get() = BuildConfig.GEMINI_API_KEY.trim()
            .takeIf { it.isNotBlank() && !it.startsWith("YOUR_") }
            ?: ""

    /**
     * Check if required secrets are configured
     */
    fun isConfigured(): Boolean = listOf(
        webClientId,
        geminiApiKey,
    ).any { it.isNotBlank() }

    /**
     * Get all missing secrets for debugging
     */
    fun getMissingSecrets(): List<String> = buildList {
        if (webClientId.isBlank()) add("WEB_CLIENT_ID")
        if (geminiApiKey.isBlank()) add("GEMINI_API_KEY")
    }
}
