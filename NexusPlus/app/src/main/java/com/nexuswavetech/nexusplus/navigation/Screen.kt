package com.nexuswavetech.nexusplus.navigation

sealed class Screen(val route: String) {
    object Welcome          : Screen("welcome")
    object Main             : Screen("main")

    // ── Original feature screens ─────────────────────────────────────────────
    object RadioPlayer      : Screen("feature/radio")
    object AiImageGenerator : Screen("feature/ai_image")
    object NexusTts         : Screen("feature/tts")
    object IptvPlayer       : Screen("feature/iptv")
    object MusicStreaming    : Screen("feature/music")

    // ── PDF Suite (replaces standalone PdfReader) ─────────────────────────────
    object PdfSuite         : Screen("feature/pdf_suite")
    // Keep old route so any saved favorites still resolve
    object PdfReader        : Screen("feature/pdf")

    // ── Security ──────────────────────────────────────────────────────────────
    object EncrypterDecrypter : Screen("feature/encrypter_decrypter")
    // Legacy alias
    object TextEncryptor    : Screen("feature/text_encryptor")

    object HashGenerator    : Screen("feature/hash_generator")
    object PasswordGenerator: Screen("feature/password_generator")
    object Base64Tool       : Screen("feature/base64_tool")
    object BiometricVault   : Screen("feature/biometric_vault")

    // ── Utilities ─────────────────────────────────────────────────────────────
    object TextTranslator   : Screen("feature/text_translator")
    object MorseCode        : Screen("feature/morse_code")
    object NumberSystem     : Screen("feature/number_system")
    object JsonFormatter    : Screen("feature/json_formatter")
    object RegexTester      : Screen("feature/regex_tester")
    object CalculatorCenter : Screen("feature/calculator_center")
    object VoiceTyper       : Screen("feature/voice_typer")
    object MyReminder       : Screen("feature/my_reminder")

    // ── Smart Tools ───────────────────────────────────────────────────────────
    object ObjectDetector   : Screen("feature/object_detector")
    object ColorDetector    : Screen("feature/color_detector")
    object SmartImageEditor : Screen("feature/smart_image_editor")
    object QrCode           : Screen("feature/qr_code")
    object DocHub           : Screen("feature/doc_hub")

    // ── Forms ────────────────────────────────────────────────────────────────
    object FormX            : Screen("feature/form_x")

    // ── Legal screens ────────────────────────────────────────────────────────
    object AboutUs          : Screen("legal/about")
    object PrivacyPolicy    : Screen("legal/privacy")
    object TermsConditions  : Screen("legal/terms")

    // ── Stub for features not yet fully built ────────────────────────────────
    object Stub             : Screen("feature/stub")
}

sealed class BottomTab(val route: String, val label: String) {
    object Home        : BottomTab("tab/home",         "Home")
    object AllFeatures : BottomTab("tab/all_features", "All Features")
    object More        : BottomTab("tab/more",         "More")
}
