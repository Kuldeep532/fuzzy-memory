package com.nexuswavetech.nexusplus.navigation

sealed class Screen(val route: String) {

    // ── Entry ───────────────────────────────────────────────────────────────
    object Welcome : Screen("welcome")
    object Main    : Screen("main")

    // ── Hub destinations ────────────────────────────────────────────────────
    object SecurityHub  : Screen("hub/security")
    object DocumentsHub : Screen("hub/documents")
    object AIHub        : Screen("hub/ai")
    object MediaHub     : Screen("hub/media")
    object UtilitiesHub : Screen("hub/utilities")

    // ── Global screens ──────────────────────────────────────────────────────
    object Settings           : Screen("settings")
    object Profile            : Screen("profile")
    object NotificationCenter : Screen("notifications")

    // ── Media feature screens ───────────────────────────────────────────────
    object RadioPlayer      : Screen("feature/radio")
    object AiImageGenerator : Screen("feature/ai_image")
    object NexusTts         : Screen("feature/tts")
    object IptvPlayer       : Screen("feature/iptv")
    object MusicStreaming    : Screen("feature/music")
    object SmartImageEditor : Screen("feature/smart_image_editor")

    // ── Document feature screens ────────────────────────────────────────────
    object PdfSuite  : Screen("feature/pdf_suite")
    object PdfReader : Screen("feature/pdf")
    object DocHub    : Screen("feature/doc_hub")

    // ── Security feature screens ────────────────────────────────────────────
    object EncrypterDecrypter : Screen("feature/encrypter_decrypter")
    object TextEncryptor      : Screen("feature/text_encryptor")
    object HashGenerator      : Screen("feature/hash_generator")
    object PasswordGenerator  : Screen("feature/password_generator")
    object Base64Tool         : Screen("feature/base64_tool")
    object BiometricVault     : Screen("feature/biometric_vault")

    // ── Utilities feature screens ───────────────────────────────────────────
    object TextTranslator   : Screen("feature/text_translator")
    object MorseCode        : Screen("feature/morse_code")
    object NumberSystem     : Screen("feature/number_system")
    object JsonFormatter    : Screen("feature/json_formatter")
    object RegexTester      : Screen("feature/regex_tester")
    object CalculatorCenter : Screen("feature/calculator_center")
    object VoiceTyper       : Screen("feature/voice_typer")
    object MyReminder       : Screen("feature/my_reminder")
    object QrCode           : Screen("feature/qr_code")

    // ── Utility stub → real implementations ─────────────────────────────────
    object Flashlight       : Screen("feature/flashlight")
    object Stopwatch        : Screen("feature/stopwatch")
    object WorldClock       : Screen("feature/world_clock")
    object UnitConverter    : Screen("feature/unit_converter")
    object CurrencyConverter: Screen("feature/currency_converter")
    object BatteryMonitor   : Screen("feature/battery_monitor")
    object StorageAnalyzer  : Screen("feature/storage_analyzer")
    object Compass          : Screen("feature/compass")
    object WifiAnalyzer     : Screen("feature/wifi_analyzer")
    object VoiceRecorder    : Screen("feature/voice_recorder")
    object ClipboardManager : Screen("feature/clipboard_manager")
    object FileManager      : Screen("feature/file_manager")
    object AlarmClock       : Screen("feature/alarm_clock")
    object BarcodeGenerator : Screen("feature/barcode_generator")
    object Weather          : Screen("feature/weather")

    // ── AI / Smart Tools feature screens ────────────────────────────────────
    object ObjectDetector : Screen("feature/object_detector")
    object ColorDetector  : Screen("feature/color_detector")

    // ── Forms ───────────────────────────────────────────────────────────────
    object FormX : Screen("feature/form_x")

    // ── Platform-level evolution systems ────────────────────────────────────
    object NexusIntelligence : Screen("system/intelligence")
    object NexusAutomation   : Screen("system/automation")
    object NexusDevKit       : Screen("system/devkit")
    object NexusHealthVault  : Screen("system/healthvault")

    // ── Legal ───────────────────────────────────────────────────────────────
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
