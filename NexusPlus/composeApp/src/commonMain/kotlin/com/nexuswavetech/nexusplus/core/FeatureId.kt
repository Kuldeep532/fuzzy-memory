package com.nexuswavetech.nexusplus.core

/**
 * Canonical identifier for every feature in Nexus Plus v2.0.
 *
 * Rules:
 *  - One entry per active feature.
 *  - Legacy backward-compat aliases kept at bottom for DataStore migration.
 *  - Do NOT remove legacy entries until migration is complete.
 */
enum class FeatureId {

    // ── Utilities (5) ─────────────────────────────────────────────────
    CALCULATOR_CENTER,
    STOPWATCH,
    CURRENCY_CONVERTER,
    UNIT_CONVERTER,
    WEATHER,

    // ── Smart Tools (5) ───────────────────────────────────────────────
    QR_GENERATOR,
    FLASHLIGHT,
    COMPASS,
    BATTERY_MONITOR,
    STORAGE_ANALYZER,

    // ── Security (5) ──────────────────────────────────────────────────
    PASSWORD_GENERATOR,
    BASE64_TOOL,
    HASH_GENERATOR,
    BIOMETRIC_VAULT,
    ENCRYPTER_DECRYPTER,

    // ── Legacy backward-compat aliases (for DataStore migration) ──────
    // DO NOT REMOVE - needed for existing app data migration
    
    // Removed in v2.0
    MUSIC_STREAMING,
    SMART_IMAGE_EDITOR,
    NEXUS_IMAGE_VIEWER,
    PDF_SUITE,
    FILE_MANAGER,
    ALARM_CLOCK,
    CLIPBOARD_MANAGER,
    JSON_FORMATTER,
    REGEX_TESTER,
    DOC_HUB,
    NEXUS_DOC_READER,
    TEXT_TO_PDF,
    NEXUS_TTS,
    VOICE_TYPER,
    TEXT_TRANSLATOR,
    MORSE_CODE,
    NUMBER_SYSTEM,
    MY_REMINDER,
    VOICE_RECORDER,
    WIFI_ANALYZER,
    OBJECT_DETECTOR,
    COLOR_DETECTOR,
    BARCODE_GENERATOR,
    APP_INFO_CENTER,
    NETWORK_INFO,
    AIRA_AI,
    EMERGENCY_GUARDIAN,
    ENCRYPTED_NOTES,
    NEXUS_HEALTH_VAULT,
    TOTP_AUTHENTICATOR,
    NETWORK_SPEED_TEST,
    SMART_DOCUMENT_SCANNER,
    VIDEO_DESCRIPTION,
    QR_CODE_SCANNER,
    VIDEO_GENERATION,
    API_MANAGER,
    NASA_APOD,
    NASA_MARS_ROVER,
    NEXUS_GPT,
    AI_IMAGE_GENERATOR,

    // Very old legacy entries
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
