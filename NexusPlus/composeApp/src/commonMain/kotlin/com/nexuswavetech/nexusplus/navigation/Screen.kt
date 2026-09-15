package com.nexuswavetech.nexusplus.navigation

/**
 * NEXUS PLUS v2.0 - NAVIGATION ROUTES
 * 
 * Only 15 active feature routes + global screens.
 * All broken/incomplete routes removed.
 */

sealed class Screen(val route: String) {

    // ── Entry Points ────────────────────────────────────────────────────
    object Welcome : Screen("welcome")
    object Main    : Screen("main")

    // ── Global Screens ──────────────────────────────────────────────────
    object Settings           : Screen("settings")
    object Profile            : Screen("profile")
    object NotificationCenter : Screen("notifications")
    object Subscription       : Screen("subscription")

    // ── UTILITIES (5) ─────────────────────────────────────────────────
    object CalculatorCenter : Screen("feature/calculator")
    object Stopwatch        : Screen("feature/stopwatch")
    object CurrencyConverter : Screen("feature/currency")
    object UnitConverter     : Screen("feature/units")
    object Weather           : Screen("feature/weather")

    // ── SMART TOOLS (5) ────────────────────────────────────────────────
    object QrCode        : Screen("feature/qr_generator")
    object Flashlight    : Screen("feature/flashlight")
    object Compass       : Screen("feature/compass")
    object BatteryMonitor : Screen("feature/battery")
    object StorageAnalyzer : Screen("feature/storage")

    // ── SECURITY (5) ──────────────────────────────────────────────────
    object PasswordGenerator  : Screen("feature/password_gen")
    object Base64Tool         : Screen("feature/base64")
    object HashGenerator      : Screen("feature/hash_gen")
    object BiometricVault     : Screen("feature/biometric_vault")
    object EncrypterDecrypter : Screen("feature/encrypter")

    // ── Legal ──────────────────────────────────────────────────────────
    object AboutUs         : Screen("legal/about")
    object PrivacyPolicy   : Screen("legal/privacy")
    object TermsConditions : Screen("legal/terms")

    // ── Placeholder for future features ────────────────────────────────
    object Stub : Screen("feature/stub")
}

sealed class BottomTab(val route: String, val label: String) {
    object Home      : BottomTab("tab/home",      "Home")
    object Explore   : BottomTab("tab/explore",   "Explore")
    object Favorites : BottomTab("tab/favorites", "Favorites")
    object More      : BottomTab("tab/more",      "More")
}
