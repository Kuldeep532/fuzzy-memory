package com.nexuswavetech.nexusplus.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.nexuswavetech.nexusplus.navigation.Screen

object FeatureCatalog {

    val allFeatures: List<FeatureItem> = listOf(

        // ── Media & Entertainment ────────────────────────────────────────────
        FeatureItem(
            id = FeatureId.RADIO_PLAYER,
            name = "Online Radio",
            description = "Stream thousands of live radio stations worldwide",
            icon = Icons.Filled.Radio,
            route = Screen.RadioPlayer.route,
            category = FeatureCategory.MEDIA
        ),
        FeatureItem(
            id = FeatureId.AI_IMAGE_GENERATOR,
            name = "AI Image Generator",
            description = "Generate stunning images from text prompts",
            icon = Icons.Filled.AutoAwesome,
            route = Screen.AiImageGenerator.route,
            category = FeatureCategory.MEDIA
        ),
        FeatureItem(
            id = FeatureId.IPTV_PLAYER,
            name = "IPTV / Live TV",
            description = "Stream live Indian TV channels and global M3U playlists",
            icon = Icons.Filled.LiveTv,
            route = Screen.IptvPlayer.route,
            category = FeatureCategory.MEDIA
        ),
        FeatureItem(
            id = FeatureId.MUSIC_STREAMING,
            name = "Music Player",
            description = "Stream online music or play local audio files on-device",
            icon = Icons.Filled.MusicNote,
            route = Screen.MusicStreaming.route,
            category = FeatureCategory.MEDIA
        ),
        FeatureItem(
            id = FeatureId.SMART_IMAGE_EDITOR,
            name = "Smart Image Editor",
            description = "Crop, rotate, flip and adjust images on-device",
            icon = Icons.Filled.PhotoFilter,
            route = Screen.SmartImageEditor.route,
            category = FeatureCategory.MEDIA
        ),

        // ── Productivity ─────────────────────────────────────────────────────
        FeatureItem(
            id = FeatureId.PDF_SUITE,
            name = "PDF Suite",
            description = "Read, create, merge, split and reorder PDF documents",
            icon = Icons.Filled.PictureAsPdf,
            route = Screen.PdfSuite.route,
            category = FeatureCategory.PRODUCTIVITY
        ),
        FeatureItem(
            id = FeatureId.FILE_MANAGER,
            name = "File Manager",
            description = "Browse and manage your device files",
            icon = Icons.Filled.Folder,
            route = Screen.Stub.route + "/file_manager",
            category = FeatureCategory.PRODUCTIVITY
        ),
        FeatureItem(
            id = FeatureId.ALARM_CLOCK,
            name = "Alarm Clock",
            description = "Smart alarm with custom sounds",
            icon = Icons.Filled.Alarm,
            route = Screen.Stub.route + "/alarm",
            category = FeatureCategory.PRODUCTIVITY
        ),
        FeatureItem(
            id = FeatureId.CLIPBOARD_MANAGER,
            name = "Clipboard Manager",
            description = "Save, organise, and reuse clipboard history",
            icon = Icons.Filled.ContentPaste,
            route = Screen.Stub.route + "/clipboard",
            category = FeatureCategory.PRODUCTIVITY
        ),
        FeatureItem(
            id = FeatureId.JSON_FORMATTER,
            name = "JSON Formatter",
            description = "Format, validate and minify JSON — fully offline",
            icon = Icons.Filled.DataObject,
            route = Screen.JsonFormatter.route,
            category = FeatureCategory.PRODUCTIVITY
        ),
        FeatureItem(
            id = FeatureId.REGEX_TESTER,
            name = "Regex Tester",
            description = "Test and debug regular expressions on-device",
            icon = Icons.Filled.Code,
            route = Screen.RegexTester.route,
            category = FeatureCategory.PRODUCTIVITY
        ),
        FeatureItem(
            id = FeatureId.DOC_HUB,
            name = "Doc Hub",
            description = "Quick access storage and organisation for your documents",
            icon = Icons.Filled.Description,
            route = Screen.DocHub.route,
            category = FeatureCategory.PRODUCTIVITY
        ),

        // ── Utilities ────────────────────────────────────────────────────────
        FeatureItem(
            id = FeatureId.NEXUS_TTS,
            name = "Nexus Speech Engine",
            description = "Multi-language text-to-speech with auto-locale detection",
            icon = Icons.Filled.RecordVoiceOver,
            route = Screen.NexusTts.route,
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.VOICE_TYPER,
            name = "Voice Typer",
            description = "Type using your voice via native speech-to-text",
            icon = Icons.Filled.Mic,
            route = Screen.VoiceTyper.route,
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.CURRENCY_CONVERTER,
            name = "Currency Converter",
            description = "Real-time currency conversion rates",
            icon = Icons.Filled.CurrencyExchange,
            route = Screen.Stub.route + "/currency",
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.UNIT_CONVERTER,
            name = "Unit Converter",
            description = "Convert length, weight, volume, and more",
            icon = Icons.Filled.SwapHoriz,
            route = Screen.Stub.route + "/units",
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.CALCULATOR_CENTER,
            name = "Calculator Center",
            description = "Standard math calculator and age calculator in one place",
            icon = Icons.Filled.Calculate,
            route = Screen.CalculatorCenter.route,
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.STOPWATCH,
            name = "Stopwatch",
            description = "Precision stopwatch with lap tracking",
            icon = Icons.Filled.Timer,
            route = Screen.Stub.route + "/stopwatch",
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.WORLD_CLOCK,
            name = "World Clock",
            description = "Track time zones across the globe",
            icon = Icons.Filled.Language,
            route = Screen.Stub.route + "/worldclock",
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.TEXT_TRANSLATOR,
            name = "Text Translator",
            description = "On-device translation — 50+ languages, no internet needed",
            icon = Icons.Filled.Translate,
            route = Screen.TextTranslator.route,
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.MORSE_CODE,
            name = "Morse Code",
            description = "Encode and decode Morse code with audio playback",
            icon = Icons.Filled.Keyboard,
            route = Screen.MorseCode.route,
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.NUMBER_SYSTEM,
            name = "Number System Converter",
            description = "Binary, Octal, Decimal and Hexadecimal converter",
            icon = Icons.Filled.Tag,
            route = Screen.NumberSystem.route,
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.WEATHER,
            name = "Weather",
            description = "Live weather forecasts and alerts",
            icon = Icons.Filled.WbSunny,
            route = Screen.Stub.route + "/weather",
            category = FeatureCategory.UTILITIES
        ),
        FeatureItem(
            id = FeatureId.MY_REMINDER,
            name = "My Reminder",
            description = "Set custom local reminders and notifications",
            icon = Icons.Filled.NotificationsActive,
            route = Screen.MyReminder.route,
            category = FeatureCategory.UTILITIES
        ),

        // ── Smart Tools ──────────────────────────────────────────────────────
        FeatureItem(
            id = FeatureId.QR_GENERATOR,
            name = "QR Code Generator",
            description = "Generate QR codes for text, URLs and UPI payments",
            icon = Icons.Filled.QrCode,
            route = Screen.QrCode.route,
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.FLASHLIGHT,
            name = "Flashlight",
            description = "Instant torch control and strobe modes",
            icon = Icons.Filled.FlashOn,
            route = Screen.Stub.route + "/flashlight",
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.COMPASS,
            name = "Compass",
            description = "Magnetic compass with tilt calibration",
            icon = Icons.Filled.Explore,
            route = Screen.Stub.route + "/compass",
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.SPEEDOMETER,
            name = "Speedometer",
            description = "GPS-powered speed tracker",
            icon = Icons.Filled.Speed,
            route = Screen.Stub.route + "/speedometer",
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.VOICE_RECORDER,
            name = "Voice Recorder",
            description = "High-quality audio recording with waveform",
            icon = Icons.Filled.GraphicEq,
            route = Screen.Stub.route + "/recorder",
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.WIFI_ANALYZER,
            name = "Wi-Fi Analyzer",
            description = "Scan and analyze nearby Wi-Fi networks",
            icon = Icons.Filled.Wifi,
            route = Screen.Stub.route + "/wifi",
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.BATTERY_MONITOR,
            name = "Battery Monitor",
            description = "Real-time battery health and usage stats",
            icon = Icons.Filled.BatteryFull,
            route = Screen.Stub.route + "/battery",
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.STORAGE_ANALYZER,
            name = "Storage Analyzer",
            description = "Visualise and free up device storage",
            icon = Icons.Filled.Storage,
            route = Screen.Stub.route + "/storage",
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.OBJECT_DETECTOR,
            name = "Object Detector",
            description = "Real-time on-device object detection via camera",
            icon = Icons.Filled.CenterFocusStrong,
            route = Screen.ObjectDetector.route,
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.COLOR_DETECTOR,
            name = "Colour Detector",
            description = "Identify any colour from camera with name and hex code",
            icon = Icons.Filled.Colorize,
            route = Screen.ColorDetector.route,
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.BARCODE_GENERATOR,
            name = "Barcode Generator",
            description = "Generate barcodes in multiple formats",
            icon = Icons.Filled.QrCodeScanner,
            route = Screen.Stub.route + "/barcode",
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.TASK_MANAGER,
            name = "Task Manager",
            description = "Monitor and manage running processes",
            icon = Icons.Filled.Dashboard,
            route = Screen.Stub.route + "/tasks",
            category = FeatureCategory.TOOLS
        ),
        FeatureItem(
            id = FeatureId.SCREEN_RECORDER,
            name = "Screen Recorder",
            description = "Record your screen with audio overlay",
            icon = Icons.Filled.ScreenShare,
            route = Screen.Stub.route + "/screenrecorder",
            category = FeatureCategory.TOOLS
        ),

        // ── Security & Privacy ───────────────────────────────────────────────
        FeatureItem(
            id = FeatureId.BIOMETRIC_VAULT,
            name = "Biometric Vault",
            description = "Securely store passwords, notes, cards and documents — biometric protected",
            icon = Icons.Filled.Fingerprint,
            route = Screen.BiometricVault.route,
            category = FeatureCategory.SECURITY
        ),
        FeatureItem(
            id = FeatureId.ENCRYPTER_DECRYPTER,
            name = "Encrypter and Decrypter",
            description = "AES-256 encrypt/decrypt text, images and files on-device",
            icon = Icons.Filled.EnhancedEncryption,
            route = Screen.EncrypterDecrypter.route,
            category = FeatureCategory.SECURITY
        ),
        FeatureItem(
            id = FeatureId.HASH_GENERATOR,
            name = "Hash Generator",
            description = "Generate MD5, SHA-1, SHA-256, SHA-512 hashes",
            icon = Icons.Filled.Tag,
            route = Screen.HashGenerator.route,
            category = FeatureCategory.SECURITY
        ),
        FeatureItem(
            id = FeatureId.PASSWORD_GENERATOR,
            name = "Password Generator",
            description = "Create strong, customisable passwords instantly",
            icon = Icons.Filled.Key,
            route = Screen.PasswordGenerator.route,
            category = FeatureCategory.SECURITY
        ),
        FeatureItem(
            id = FeatureId.BASE64_TOOL,
            name = "Base64 Tool",
            description = "Encode and decode Base64 strings on-device",
            icon = Icons.Filled.SwapVert,
            route = Screen.Base64Tool.route,
            category = FeatureCategory.SECURITY
        ),
        FeatureItem(
            id = FeatureId.ENCRYPTED_NOTES,
            name = "Encrypted Notes",
            description = "Private, AES-256 encrypted personal notes",
            icon = Icons.Filled.Lock,
            route = Screen.Stub.route + "/notes",
            category = FeatureCategory.SECURITY
        ),
        FeatureItem(
            id = FeatureId.APP_LOCKER,
            name = "App Locker",
            description = "Protect apps with biometric authentication",
            icon = Icons.Filled.Security,
            route = Screen.Stub.route + "/applocker",
            category = FeatureCategory.SECURITY
        ),
        FeatureItem(
            id = FeatureId.CONTACT_BACKUP,
            name = "Contact Backup",
            description = "Backup and restore your contacts securely",
            icon = Icons.Filled.Contacts,
            route = Screen.Stub.route + "/contacts",
            category = FeatureCategory.SECURITY
        )
    )
}
