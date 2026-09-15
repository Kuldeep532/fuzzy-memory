package com.nexuswavetech.nexusplus.navigation

sealed class Screen(val route: String) {

    // ── Entry ───────────────────────────────────────────────────────────
    object Welcome : Screen("welcome")
    object Main    : Screen("main")

    // ── Global screens ──────────────────────────────────────────────────────
    object Settings           : Screen("settings")
    object Profile            : Screen("profile")
    object NotificationCenter : Screen("notifications")
    object Subscription       : Screen("subscription")

    // ── Utilities feature screens ───────────────────────────────────────────
    object CalculatorCenter : Screen("feature/calculator_center")
    object Stopwatch        : Screen("feature/stopwatch")
    object CurrencyConverter : Screen("feature/currency_converter")
    object UnitConverter     : Screen("feature/unit_converter")
    object Weather           : Screen("feature/weather")

    // ── Smart Tools feature screens ──────────────────────────────────────────
    object QrCode        : Screen("feature/qr_code")
    object Flashlight    : Screen("feature/flashlight")
    object Compass       : Screen("feature/compass")
    object BatteryMonitor : Screen("feature/battery_monitor")
    object StorageAnalyzer : Screen("feature/storage_analyzer")

    // ── Security feature screens ────────────────────────────────────────────
    object PasswordGenerator  : Screen("feature/password_generator")
    object Base64Tool         : Screen("feature/base64_tool")
    object HashGenerator      : Screen("feature/hash_generator")
    object BiometricVault     : Screen("feature/biometric_vault")
    object EncrypterDecrypter : Screen("feature/encrypter_decrypter")

    // ── Legal ──────────────────────────────────────────────────────────
    object AboutUs         : Screen("legal/about")
    object PrivacyPolicy   : Screen("legal/privacy")
    object TermsConditions : Screen("legal/terms")

    // ── Stub catch-all for features under development ───────────────────────
    object Stub : Screen("feature/stub")
}

sealed class BottomTab(val route: String, val label: String) {
    object Home      : BottomTab("tab/home",      "Home")
    object Explore   : BottomTab("tab/explore",   "Explore")
    object Favorites : BottomTab("tab/favorites", "Favorites")
    object More      : BottomTab("tab/more",      "More")
}
