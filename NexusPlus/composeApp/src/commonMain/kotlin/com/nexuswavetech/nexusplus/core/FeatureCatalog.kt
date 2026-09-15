package com.nexuswavetech.nexusplus.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.nexuswavetech.nexusplus.navigation.Screen

/**
 * NEXUS PLUS v2.0 - CORE FEATURES ONLY
 * 
 * 15 Production-Ready Features:
 * - 5 Utilities (Calculator, Stopwatch, Currency, Units, Weather)
 * - 5 Smart Tools (QR Gen, Flashlight, Compass, Battery, Storage)
 * - 5 Security (Password Gen, Base64, Hash Gen, Biometric Vault, Encrypter)
 * 
 * All broken/incomplete features removed.
 * New features coming in future versions.
 */

object FeatureCatalog {

    val allFeatures: List<FeatureItem> = listOf(

        // ── UTILITIES (5) ──────────────────────────────────────────────
        FeatureItem(
            id          = FeatureId.CALCULATOR_CENTER,
            name        = "Calculator",
            description = "Simple and fast calculator",
            icon        = Icons.Filled.Calculate,
            route       = Screen.CalculatorCenter.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("calculator", "math", "compute"),
        ),
        FeatureItem(
            id          = FeatureId.STOPWATCH,
            name        = "Stopwatch",
            description = "Timer with lap tracking",
            icon        = Icons.Filled.Timer,
            route       = Screen.Stopwatch.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("stopwatch", "timer", "lap", "time"),
        ),
        FeatureItem(
            id          = FeatureId.CURRENCY_CONVERTER,
            name        = "Currency Converter",
            description = "Convert currencies offline",
            icon        = Icons.Filled.CurrencyExchange,
            route       = Screen.CurrencyConverter.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("currency", "convert", "exchange"),
        ),
        FeatureItem(
            id          = FeatureId.UNIT_CONVERTER,
            name        = "Unit Converter",
            description = "Convert length, weight, temperature",
            icon        = Icons.Filled.SwapHoriz,
            route       = Screen.UnitConverter.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("unit", "convert", "length", "weight"),
        ),
        FeatureItem(
            id          = FeatureId.WEATHER,
            name        = "Weather",
            description = "Live weather forecasts",
            icon        = Icons.Filled.WbSunny,
            route       = Screen.Weather.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("weather", "forecast", "temperature"),
        ),

        // ── SMART TOOLS (5) ────────────────────────────────────────────
        FeatureItem(
            id          = FeatureId.QR_GENERATOR,
            name        = "QR Code Generator",
            description = "Generate QR codes instantly",
            icon        = Icons.Filled.QrCode,
            route       = Screen.QrCode.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("qr", "generate", "barcode"),
        ),
        FeatureItem(
            id          = FeatureId.FLASHLIGHT,
            name        = "Flashlight",
            description = "Turn on device flashlight",
            icon        = Icons.Filled.FlashOn,
            route       = Screen.Flashlight.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("flashlight", "torch", "light"),
        ),
        FeatureItem(
            id          = FeatureId.COMPASS,
            name        = "Compass",
            description = "Navigation compass",
            icon        = Icons.Filled.Explore,
            route       = Screen.Compass.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("compass", "direction", "navigate"),
        ),
        FeatureItem(
            id          = FeatureId.BATTERY_MONITOR,
            name        = "Battery Monitor",
            description = "Check battery health and stats",
            icon        = Icons.Filled.BatteryFull,
            route       = Screen.BatteryMonitor.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("battery", "health", "charge"),
        ),
        FeatureItem(
            id          = FeatureId.STORAGE_ANALYZER,
            name        = "Storage Analyzer",
            description = "View storage usage",
            icon        = Icons.Filled.Storage,
            route       = Screen.StorageAnalyzer.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("storage", "disk", "space"),
        ),

        // ── SECURITY (5) ───────────────────────────────────────────────
        FeatureItem(
            id          = FeatureId.PASSWORD_GENERATOR,
            name        = "Password Generator",
            description = "Create strong passwords",
            icon        = Icons.Filled.Key,
            route       = Screen.PasswordGenerator.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("password", "generate", "secure"),
        ),
        FeatureItem(
            id          = FeatureId.BASE64_TOOL,
            name        = "Base64 Tool",
            description = "Encode and decode Base64",
            icon        = Icons.Filled.SwapVert,
            route       = Screen.Base64Tool.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("base64", "encode", "decode"),
        ),
        FeatureItem(
            id          = FeatureId.HASH_GENERATOR,
            name        = "Hash Generator",
            description = "Generate MD5 and SHA hashes",
            icon        = Icons.Filled.Tag,
            route       = Screen.HashGenerator.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("hash", "md5", "sha", "checksum"),
        ),
        FeatureItem(
            id          = FeatureId.BIOMETRIC_VAULT,
            name        = "Biometric Vault",
            description = "Secure encrypted vault",
            icon        = Icons.Filled.Fingerprint,
            route       = Screen.BiometricVault.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("vault", "secure", "password"),
        ),
        FeatureItem(
            id          = FeatureId.ENCRYPTER_DECRYPTER,
            name        = "Encrypter/Decrypter",
            description = "AES-256 encryption tool",
            icon        = Icons.Filled.EnhancedEncryption,
            route       = Screen.EncrypterDecrypter.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("encrypt", "decrypt", "secure"),
        ),

    )

    /** All features grouped by category. */
    val byCategory: Map<FeatureCategory, List<FeatureItem>> by lazy {
        FeatureCategory.entries.associateWith { category ->
            allFeatures.filter { it.category == category }
        }
    }

    /** Features belonging to a specific category. */
    fun forCategory(category: FeatureCategory): List<FeatureItem> = byCategory[category] ?: emptyList()

    /** Register all features into the dynamic FeatureRegistry. */
    fun registerAllFeatures() {
        com.nexuswavetech.nexusplus.core.registry.FeatureRegistry.registerAll(allFeatures)
    }

    /** Total feature count. */
    val totalCount: Int get() = allFeatures.size
}
