package com.nexuswavetech.nexusplus.navigation

sealed class Screen(val route: String) {
    object Welcome          : Screen("welcome")
    object Main             : Screen("main")

    // ── Original feature screens ─────────────────────────────────────────────
    object RadioPlayer      : Screen("feature/radio")
    object PdfReader        : Screen("feature/pdf")
    object AiImageGenerator : Screen("feature/ai_image")
    object NexusTts         : Screen("feature/tts")
    object IptvPlayer       : Screen("feature/iptv")
    object MusicStreaming    : Screen("feature/music")

    // ── New v1.1 feature screens ─────────────────────────────────────────────
    object TextEncryptor    : Screen("feature/text_encryptor")
    object TextTranslator   : Screen("feature/text_translator")
    object ObjectDetector   : Screen("feature/object_detector")
    object ColorDetector    : Screen("feature/color_detector")
    object SmartImageEditor : Screen("feature/smart_image_editor")
    object HashGenerator    : Screen("feature/hash_generator")
    object PasswordGenerator: Screen("feature/password_generator")
    object Base64Tool       : Screen("feature/base64_tool")
    object MorseCode        : Screen("feature/morse_code")
    object NumberSystem     : Screen("feature/number_system")
    object JsonFormatter    : Screen("feature/json_formatter")
    object RegexTester      : Screen("feature/regex_tester")

    // ── Legal screens ────────────────────────────────────────────────────────
    object AboutUs          : Screen("legal/about")
    object PrivacyPolicy    : Screen("legal/privacy")
    object TermsConditions  : Screen("legal/terms")

    // ── Stub for extended features not yet fully built ───────────────────────
    object Stub             : Screen("feature/stub")
}

sealed class BottomTab(val route: String, val label: String) {
    object Favorites   : BottomTab("tab/favorites",    "Favorites")
    object AllFeatures : BottomTab("tab/all_features", "All Features")
    object More        : BottomTab("tab/more",         "More")
}
