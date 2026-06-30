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
    object Subscription       : Screen("subscription")

    // ── Media feature screens ───────────────────────────────────────────────
    object AiImageGenerator  : Screen("feature/ai_image")
    object NexusTts          : Screen("feature/tts")
    object MusicStreaming     : Screen("feature/music")
    object SmartImageEditor  : Screen("feature/smart_image_editor")
    object NexusImageViewer  : Screen("feature/image_viewer")

    // ── Document feature screens ────────────────────────────────────────────
    object PdfSuite         : Screen("feature/pdf_suite")
    object DocHub           : Screen("feature/doc_hub")
    object NexusDocReader   : Screen("feature/nexus_doc_reader")

    // ── Security feature screens ────────────────────────────────────────────
    object EncrypterDecrypter : Screen("feature/encrypter_decrypter")
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

    // ── Utility feature screens ──────────────────────────────────────────────
    object Flashlight        : Screen("feature/flashlight")
    object Stopwatch         : Screen("feature/stopwatch")
    object WorldClock        : Screen("feature/world_clock")
    object UnitConverter     : Screen("feature/unit_converter")
    object CurrencyConverter : Screen("feature/currency_converter")
    object BatteryMonitor    : Screen("feature/battery_monitor")
    object StorageAnalyzer   : Screen("feature/storage_analyzer")
    object Compass           : Screen("feature/compass")
    object WifiAnalyzer      : Screen("feature/wifi_analyzer")
    object VoiceRecorder     : Screen("feature/voice_recorder")
    object ClipboardManager  : Screen("feature/clipboard_manager")
    object FileManager       : Screen("feature/file_manager")
    object AlarmClock        : Screen("feature/alarm_clock")
    object BarcodeGenerator  : Screen("feature/barcode_generator")
    object Weather           : Screen("feature/weather")

    // ── AI / Smart Tools feature screens ────────────────────────────────────
    object ObjectDetector : Screen("feature/object_detector")
    object ColorDetector  : Screen("feature/color_detector")
    object AiraAi         : Screen("feature/aira_ai")

    // ── Smart Tools (new) ────────────────────────────────────────────────────
    object AppInfoCenter : Screen("feature/app_info_center")
    object NetworkInfo   : Screen("feature/network_info")

    // ── Health & Wellbeing ───────────────────────────────────────────────────
    object NexusHealthVault : Screen("feature/health_vault")

    // ── New in v1.2 ──────────────────────────────────────────────────────────
    object TotpAuthenticator : Screen("feature/totp_authenticator")
    object NetworkSpeedTest  : Screen("feature/network_speed_test")

    // ── Legal ───────────────────────────────────────────────────────────────
    object AboutUs         : Screen("legal/about")
    object PrivacyPolicy   : Screen("legal/privacy")
    object TermsConditions : Screen("legal/terms")

    // ── Stage 4 features ────────────────────────────────────────────────────
    object NexusDialer  : Screen("feature/nexus_dialer")
    object TextAnalyzer : Screen("feature/text_analyzer")

    // ── Stage 5 features ────────────────────────────────────────────────────
    object UrlShortener : Screen("feature/url_shortener")

    // ── News & Science ──────────────────────────────────────────────────────
    object News    : Screen("feature/news")
    object Science : Screen("feature/science")

    // ── Nexus Games Hub ─────────────────────────────────────────────────────
    object NexusGames : Screen("feature/nexus_games")

    // ── Finance Tracker ──────────────────────────────────────────────────────
    object ExpenseTracker : Screen("feature/expense_tracker")

    // ── Download Voices (TTS model manager) ─────────────────────────────────
    object DownloadVoices : Screen("settings/download_voices")

    // ── Social Media & Community ─────────────────────────────────────────────
    object SocialMedia : Screen("community/social_media")

    // ── Security features ────────────────────────────────────────────────────
    object EncryptedNotes  : Screen("feature/encrypted_notes")
    object Speedometer     : Screen("feature/speedometer")
    object TaskManager     : Screen("feature/task_manager")
    object ContactBackup   : Screen("feature/contact_backup")
    object EmergencyGuardian : Screen("feature/emergency_guardian")

    // ── New features (v1.3+) ─────────────────────────────────────────────────
    object TextToPdf    : Screen("feature/text_to_pdf")
    object DailyJournal : Screen("feature/daily_journal")
    object ColorPalette : Screen("feature/color_palette")

    // v1.4.0
    object SmartDocumentScanner : Screen("feature/doc_scanner")
    object VideoDescription     : Screen("feature/video_description")
    object QrCodeScanner        : Screen("feature/qr_scanner")

    // ── Stub catch-all for features under development ───────────────────────
    object Stub : Screen("feature/stub")
}

sealed class BottomTab(val route: String, val label: String) {
    object Home      : BottomTab("tab/home",      "Home")
    object Explore   : BottomTab("tab/explore",   "Explore")
    object Favorites : BottomTab("tab/favorites", "Favorites")
    object More      : BottomTab("tab/more",      "More")
}
