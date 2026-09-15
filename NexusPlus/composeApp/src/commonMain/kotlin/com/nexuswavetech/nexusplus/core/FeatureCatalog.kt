package com.nexuswavetech.nexusplus.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.nexuswavetech.nexusplus.navigation.Screen

/**
 * NEXUS PLUS 2.0 - CLEAN FEATURE CATALOG
 * 
 * Only tested, production-ready features included.
 * Removed: Broken, half-baked, or untested features
 * 
 * Categories:
 * - MEDIA: Audio/Video/Images
 * - PRODUCTIVITY: Documents, Files, Notes
 * - UTILITIES: Converters, Tools, Info
 * - TOOLS: Smart Tools
 * - SECURITY: Encryption, Vault, Privacy
 * - SCIENCE: NASA, Space, Advanced AI
 */

object FeatureCatalog {

    val allFeatures: List<FeatureItem> = listOf(

        // ── Media & Entertainment ─────────────────────��───────────────────────
        FeatureItem(
            id          = FeatureId.MUSIC_STREAMING,
            name        = "Nexus Media Player",
            description = "Play local audio & video files with full controls",
            icon        = Icons.Filled.PlayCircleFilled,
            route       = Screen.MusicStreaming.route,
            category    = FeatureCategory.MEDIA,
            keywords    = listOf("music", "audio", "video", "player", "local", "media"),
        ),
        FeatureItem(
            id          = FeatureId.NEXUS_IMAGE_VIEWER,
            name        = "Image Viewer",
            description = "View, zoom, rotate images with pinch support",
            icon        = Icons.Filled.Photo,
            route       = Screen.NexusImageViewer.route,
            category    = FeatureCategory.MEDIA,
            keywords    = listOf("image", "viewer", "photo", "gallery", "zoom"),
        ),
        FeatureItem(
            id          = FeatureId.SMART_IMAGE_EDITOR,
            name        = "Image Editor",
            description = "Crop, rotate, flip and enhance images",
            icon        = Icons.Filled.PhotoFilter,
            route       = Screen.SmartImageEditor.route,
            category    = FeatureCategory.MEDIA,
            keywords    = listOf("image", "editor", "crop", "rotate", "filter"),
        ),

        // ── Productivity ──────────────────────────────────────────────────────
        FeatureItem(
            id          = FeatureId.PDF_SUITE,
            name        = "PDF Suite",
            description = "Read and manage PDF documents",
            icon        = Icons.Filled.PictureAsPdf,
            route       = Screen.PdfSuite.route,
            category    = FeatureCategory.PRODUCTIVITY,
            keywords    = listOf("pdf", "document", "reader", "file"),
        ),
        FeatureItem(
            id          = FeatureId.FILE_MANAGER,
            name        = "File Manager",
            description = "Browse and manage device files",
            icon        = Icons.Filled.Folder,
            route       = Screen.FileManager.route,
            category    = FeatureCategory.PRODUCTIVITY,
            keywords    = listOf("file", "folder", "manager", "storage"),
        ),
        FeatureItem(
            id          = FeatureId.CLIPBOARD_MANAGER,
            name        = "Clipboard Manager",
            description = "Save and reuse clipboard history",
            icon        = Icons.Filled.ContentPaste,
            route       = Screen.ClipboardManager.route,
            category    = FeatureCategory.PRODUCTIVITY,
            keywords    = listOf("clipboard", "copy", "paste", "history"),
        ),
        FeatureItem(
            id          = FeatureId.TEXT_TO_PDF,
            name        = "Text to PDF",
            description = "Convert text to PDF documents",
            icon        = Icons.Filled.PictureAsPdf,
            route       = Screen.TextToPdf.route,
            category    = FeatureCategory.PRODUCTIVITY,
            keywords    = listOf("text", "pdf", "convert", "document"),
        ),
        FeatureItem(
            id          = FeatureId.NEXUS_DOC_READER,
            name        = "Document Reader",
            description = "Read PDF, TXT, DOC files",
            icon        = Icons.Filled.MenuBook,
            route       = Screen.NexusDocReader.route,
            category    = FeatureCategory.PRODUCTIVITY,
            keywords    = listOf("document", "reader", "pdf", "text"),
        ),

        // ── Utilities ───────────────────────────────────────────────────────
        FeatureItem(
            id          = FeatureId.NEXUS_TTS,
            name        = "Text to Speech",
            description = "Multi-language text-to-speech engine",
            icon        = Icons.Filled.RecordVoiceOver,
            route       = Screen.NexusTts.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("tts", "speak", "voice", "audio"),
        ),
        FeatureItem(
            id          = FeatureId.VOICE_TYPER,
            name        = "Voice Typer",
            description = "Type using voice commands",
            icon        = Icons.Filled.Mic,
            route       = Screen.VoiceTyper.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("voice", "speech", "dictate"),
        ),
        FeatureItem(
            id          = FeatureId.CURRENCY_CONVERTER,
            name        = "Currency Converter",
            description = "Convert currencies offline",
            icon        = Icons.Filled.CurrencyExchange,
            route       = Screen.CurrencyConverter.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("currency", "convert", "exchange", "money"),
        ),
        FeatureItem(
            id          = FeatureId.UNIT_CONVERTER,
            name        = "Unit Converter",
            description = "Convert length, weight, temperature and more",
            icon        = Icons.Filled.SwapHoriz,
            route       = Screen.UnitConverter.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("unit", "convert", "length", "weight"),
        ),
        FeatureItem(
            id          = FeatureId.CALCULATOR_CENTER,
            name        = "Calculator",
            description = "Standard calculator with math operations",
            icon        = Icons.Filled.Calculate,
            route       = Screen.CalculatorCenter.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("calculator", "math", "compute"),
        ),
        FeatureItem(
            id          = FeatureId.STOPWATCH,
            name        = "Stopwatch",
            description = "Precision timing with lap tracking",
            icon        = Icons.Filled.Timer,
            route       = Screen.Stopwatch.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("stopwatch", "timer", "lap", "time"),
        ),
        FeatureItem(
            id          = FeatureId.WORLD_CLOCK,
            name        = "World Clock",
            description = "Track multiple time zones",
            icon        = Icons.Filled.Language,
            route       = Screen.WorldClock.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("clock", "time", "timezone", "world"),
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
        FeatureItem(
            id          = FeatureId.MY_REMINDER,
            name        = "Reminders",
            description = "Set and manage reminders",
            icon        = Icons.Filled.NotificationsActive,
            route       = Screen.MyReminder.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("reminder", "notification", "schedule"),
        ),
        FeatureItem(
            id          = FeatureId.NUMBER_SYSTEM,
            name        = "Number Converter",
            description = "Binary, Octal, Hex conversion",
            icon        = Icons.Filled.Tag,
            route       = Screen.NumberSystem.route,
            category    = FeatureCategory.UTILITIES,
            keywords    = listOf("binary", "hex", "number", "convert"),
        ),

        // ── Smart Tools ───────────────────────────────────────────────────────
        FeatureItem(
            id          = FeatureId.QR_GENERATOR,
            name        = "QR Generator",
            description = "Generate QR codes and barcodes",
            icon        = Icons.Filled.QrCode,
            route       = Screen.QrCode.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("qr", "barcode", "generate", "scan"),
        ),
        FeatureItem(
            id          = FeatureId.QR_CODE_SCANNER,
            name        = "QR Scanner",
            description = "Scan QR codes and barcodes",
            icon        = Icons.Filled.QrCodeScanner,
            route       = Screen.QrCodeScanner.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("qr", "scanner", "barcode", "scan"),
        ),
        FeatureItem(
            id          = FeatureId.FLASHLIGHT,
            name        = "Flashlight",
            description = "Instant torch control",
            icon        = Icons.Filled.FlashOn,
            route       = Screen.Flashlight.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("flashlight", "torch", "light"),
        ),
        FeatureItem(
            id          = FeatureId.COMPASS,
            name        = "Compass",
            description = "Navigation compass with direction",
            icon        = Icons.Filled.Explore,
            route       = Screen.Compass.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("compass", "direction", "navigate"),
        ),
        FeatureItem(
            id          = FeatureId.VOICE_RECORDER,
            name        = "Voice Recorder",
            description = "Record high-quality audio",
            icon        = Icons.Filled.GraphicEq,
            route       = Screen.VoiceRecorder.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("record", "audio", "microphone"),
        ),
        FeatureItem(
            id          = FeatureId.WIFI_ANALYZER,
            name        = "Wi-Fi Analyzer",
            description = "Scan nearby Wi-Fi networks",
            icon        = Icons.Filled.Wifi,
            route       = Screen.WifiAnalyzer.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("wifi", "network", "scan", "signal"),
        ),
        FeatureItem(
            id          = FeatureId.BATTERY_MONITOR,
            name        = "Battery Monitor",
            description = "Monitor battery health and stats",
            icon        = Icons.Filled.BatteryFull,
            route       = Screen.BatteryMonitor.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("battery", "health", "charge", "monitor"),
        ),
        FeatureItem(
            id          = FeatureId.STORAGE_ANALYZER,
            name        = "Storage Analyzer",
            description = "Visualise storage usage",
            icon        = Icons.Filled.Storage,
            route       = Screen.StorageAnalyzer.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("storage", "disk", "space", "analyze"),
        ),
        FeatureItem(
            id          = FeatureId.NETWORK_INFO,
            name        = "Network Info",
            description = "View network and IP details",
            icon        = Icons.Filled.NetworkCheck,
            route       = Screen.NetworkInfo.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("network", "ip", "dns", "connection"),
        ),
        FeatureItem(
            id          = FeatureId.APP_INFO_CENTER,
            name        = "Installed Apps",
            description = "Browse all installed applications",
            icon        = Icons.Filled.Apps,
            route       = Screen.AppInfoCenter.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("apps", "installed", "packages", "info"),
        ),
        FeatureItem(
            id          = FeatureId.JSON_FORMATTER,
            name        = "JSON Formatter",
            description = "Format and validate JSON",
            icon        = Icons.Filled.DataObject,
            route       = Screen.JsonFormatter.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("json", "format", "validate", "code"),
        ),
        FeatureItem(
            id          = FeatureId.REGEX_TESTER,
            name        = "Regex Tester",
            description = "Test regular expressions",
            icon        = Icons.Filled.Code,
            route       = Screen.RegexTester.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("regex", "pattern", "test", "code"),
        ),
        FeatureItem(
            id          = FeatureId.TEXT_TRANSLATOR,
            name        = "Translator",
            description = "Translate text offline",
            icon        = Icons.Filled.Translate,
            route       = Screen.TextTranslator.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("translate", "language", "text"),
        ),
        FeatureItem(
            id          = FeatureId.MORSE_CODE,
            name        = "Morse Code",
            description = "Encode/decode Morse code",
            icon        = Icons.Filled.Keyboard,
            route       = Screen.MorseCode.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("morse", "code", "encode", "decode"),
        ),
        FeatureItem(
            id          = FeatureId.BARCODE_GENERATOR,
            name        = "Barcode Generator",
            description = "Generate various barcode types",
            icon        = Icons.Filled.QrCodeScanner,
            route       = Screen.BarcodeGenerator.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("barcode", "generate", "scan"),
        ),
        FeatureItem(
            id          = FeatureId.SMART_DOCUMENT_SCANNER,
            name        = "Document Scanner",
            description = "Scan documents and export as PDF",
            icon        = Icons.Filled.DocumentScanner,
            route       = Screen.SmartDocumentScanner.route,
            category    = FeatureCategory.TOOLS,
            keywords    = listOf("scan", "document", "ocr", "pdf"),
        ),

        // ── Security & Privacy ────────────────────────────────────────────────
        FeatureItem(
            id          = FeatureId.BIOMETRIC_VAULT,
            name        = "Biometric Vault",
            description = "Secure encrypted vault for sensitive data",
            icon        = Icons.Filled.Fingerprint,
            route       = Screen.BiometricVault.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("vault", "biometric", "secure", "password"),
        ),
        FeatureItem(
            id          = FeatureId.ENCRYPTER_DECRYPTER,
            name        = "Encrypter/Decrypter",
            description = "AES-256 encrypt and decrypt data",
            icon        = Icons.Filled.EnhancedEncryption,
            route       = Screen.EncrypterDecrypter.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("encrypt", "decrypt", "secure", "aes"),
        ),
        FeatureItem(
            id          = FeatureId.HASH_GENERATOR,
            name        = "Hash Generator",
            description = "Generate MD5, SHA hashes",
            icon        = Icons.Filled.Tag,
            route       = Screen.HashGenerator.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("hash", "md5", "sha", "checksum"),
        ),
        FeatureItem(
            id          = FeatureId.PASSWORD_GENERATOR,
            name        = "Password Generator",
            description = "Create strong random passwords",
            icon        = Icons.Filled.Key,
            route       = Screen.PasswordGenerator.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("password", "generate", "strong"),
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
            id          = FeatureId.ENCRYPTED_NOTES,
            name        = "Encrypted Notes",
            description = "Private AES-256 encrypted notes",
            icon        = Icons.Filled.Lock,
            route       = Screen.EncryptedNotes.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("notes", "encrypted", "secure"),
        ),
        FeatureItem(
            id          = FeatureId.TOTP_AUTHENTICATOR,
            name        = "TOTP Authenticator",
            description = "Generate 2FA one-time passwords",
            icon        = Icons.Filled.Key,
            route       = Screen.TotpAuthenticator.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("totp", "2fa", "authenticator"),
        ),
        FeatureItem(
            id          = FeatureId.EMERGENCY_GUARDIAN,
            name        = "Emergency Guardian",
            description = "SOS trigger: send location via SMS",
            icon        = Icons.Filled.Shield,
            route       = Screen.EmergencyGuardian.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("emergency", "sos", "safety"),
        ),
        FeatureItem(
            id          = FeatureId.NEXUS_HEALTH_VAULT,
            name        = "Health Vault",
            description = "Secure health records storage",
            icon        = Icons.Filled.HealthAndSafety,
            route       = Screen.NexusHealthVault.route,
            category    = FeatureCategory.SECURITY,
            keywords    = listOf("health", "medical", "vault"),
        ),

        // ── Science & Space ────────────────────────────────────────────────
        FeatureItem(
            id          = FeatureId.NASA_APOD,
            name        = "NASA APOD",
            description = "Astronomy Picture of the Day",
            icon        = Icons.Filled.WbSunny,
            route       = Screen.NasaApod.route,
            category    = FeatureCategory.SCIENCE,
            keywords    = listOf("nasa", "space", "astronomy", "apod"),
        ),
        FeatureItem(
            id          = FeatureId.NASA_MARS_ROVER,
            name        = "Mars Rover Photos",
            description = "Explore Mars via NASA rover cameras",
            icon        = Icons.Filled.Rocket,
            route       = Screen.NasaMarsRover.route,
            category    = FeatureCategory.SCIENCE,
            keywords    = listOf("mars", "nasa", "rover", "space"),
        ),
        FeatureItem(
            id          = FeatureId.NEXUS_GPT,
            name        = "Nexus GPT",
            description = "Advanced AI assistant with memory",
            icon        = Icons.Filled.ChatBubble,
            route       = Screen.NexusGpt.route,
            category    = FeatureCategory.SCIENCE,
            keywords    = listOf("gpt", "ai", "assistant", "chat"),
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
