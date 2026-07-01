package com.nexuswavetech.nexusplus.platform

import com.nexuswavetech.nexusplus.BuildConfig

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
     * AdMob App ID
     * Injected from ADMOB_APP_ID GitHub Secret
     */
    val admobAppId: String
        get() = BuildConfig.ADMOB_APP_ID.trim()
            .takeIf { it.isNotBlank() && !it.startsWith("YOUR_") }
            ?: ""

    /**
     * AdMob Banner ID
     * Injected from ADMOB_BANNER_ID GitHub Secret
     */
    val admobBannerId: String
        get() = BuildConfig.ADMOB_BANNER_ID.trim()
            .takeIf { it.isNotBlank() && !it.startsWith("YOUR_") }
            ?: ""

    /**
     * AdMob Interstitial ID
     * Injected from ADMOB_INTERSTITIAL_ID GitHub Secret
     */
    val admobInterstitialId: String
        get() = BuildConfig.ADMOB_INTERSTITIAL_ID.trim()
            .takeIf { it.isNotBlank() && !it.startsWith("YOUR_") }
            ?: ""

    /**
     * Check if required secrets are configured
     */
    fun isConfigured(): Boolean = listOf(
        webClientId,
        geminiApiKey,
        admobAppId,
    ).any { it.isNotBlank() }

    /**
     * Get all missing secrets for debugging
     */
    fun getMissingSecrets(): List<String> = buildList {
        if (webClientId.isBlank()) add("WEB_CLIENT_ID")
        if (geminiApiKey.isBlank()) add("GEMINI_API_KEY")
        if (admobAppId.isBlank()) add("ADMOB_APP_ID")
        if (admobBannerId.isBlank()) add("ADMOB_BANNER_ID")
        if (admobInterstitialId.isBlank()) add("ADMOB_INTERSTITIAL_ID")
    }
}
