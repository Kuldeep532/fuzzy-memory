package com.nexuswavetech.nexusplus.core

/**
 * NEXUS PLUS v2.0 - FEATURE IDS
 * 
 * Active features: Only 15 core features.
 * Legacy entries kept for DataStore backward compatibility.
 */
enum class FeatureId {

    // ── ACTIVE FEATURES (15) ──────────────────────────────────────────
    
    // Utilities
    CALCULATOR_CENTER,
    STOPWATCH,
    CURRENCY_CONVERTER,
    UNIT_CONVERTER,
    WEATHER,

    // Smart Tools
    QR_GENERATOR,
    FLASHLIGHT,
    COMPASS,
    BATTERY_MONITOR,
    STORAGE_ANALYZER,

    // Security
    PASSWORD_GENERATOR,
    BASE64_TOOL,
    HASH_GENERATOR,
    BIOMETRIC_VAULT,
    ENCRYPTER_DECRYPTER,

    // ── LEGACY ENTRIES (kept for DataStore migration) ──────────────────
    // DO NOT REMOVE - existing app data needs these for migration
    
    // Removed v2.0 (Media)
    MUSIC_STREAMING,
    SMART_IMAGE_EDITOR,
    NEXUS_IMAGE_VIEWER,
    
    // Removed v2.0 (Productivity)
    PDF_SUITE,
    FILE_MANAGER,
    ALARM_CLOCK,
    CLIPBOARD_MANAGER,
    JSON_FORMATTER,
    REGEX_TESTER,
    DOC_HUB,
    NEXUS_DOC_READER,
    TEXT_TO_PDF,
    
    // Removed v2.0 (Utilities)
    NEXUS_TTS,
    VOICE_TYPER,
    TEXT_TRANSLATOR,
    MORSE_CODE,
    NUMBER_SYSTEM,
    MY_REMINDER,
    
    // Removed v2.0 (Tools)
    VOICE_RECORDER,
    WIFI_ANALYZER,
    OBJECT_DETECTOR,
    COLOR_DETECTOR,
    BARCODE_GENERATOR,
    APP_INFO_CENTER,
    NETWORK_INFO,
    
    // Removed v2.0 (AI/Advanced)
    AIRA_AI,
    AI_IMAGE_GENERATOR,
    
    // Removed v2.0 (Additional Security)
    EMERGENCY_GUARDIAN,
    ENCRYPTED_NOTES,
    NEXUS_HEALTH_VAULT,
    TOTP_AUTHENTICATOR,
    
    // Removed v2.0 (Tools v1.4+)
    NETWORK_SPEED_TEST,
    SMART_DOCUMENT_SCANNER,
    VIDEO_DESCRIPTION,
    QR_CODE_SCANNER,
    
    // Removed v2.0 (Advanced v1.5+)
    VIDEO_GENERATION,
    API_MANAGER,
    
    // Removed v2.0 (Science)
    NASA_APOD,
    NASA_MARS_ROVER,
    NEXUS_GPT,
    
    // Old legacy entries
    NEWS,
    SCIENCE,
    NEXUS_GAMES,
    DAILY_JOURNAL,
    COLOR_PALETTE,
    EXPENSE_TRACKER,
    NEXUS_DIALER,
    TEXT_ANALYZER,
    URL_SHORTENER,
    CONTACT_BACKUP,
    SPEEDOMETER,
    TASK_MANAGER,
    RADIO_PLAYER,
    IPTV_PLAYER,
    FORM_X,
    SCREEN_RECORDER,
    APP_LOCKER,
    PDF_READER,
    TEXT_ENCRYPTOR,
    QR_SCANNER,
    CALCULATOR,
}
