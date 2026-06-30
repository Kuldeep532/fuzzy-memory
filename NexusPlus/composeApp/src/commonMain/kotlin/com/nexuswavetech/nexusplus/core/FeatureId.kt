package com.nexuswavetech.nexusplus.core

/**
 * Canonical identifier for every feature in Nexus Plus.
 *
 * Rules:
 *  - One entry per feature, never duplicated.
 *  - Legacy backward-compat aliases grouped at the bottom.
 *    Do NOT remove them until a DataStore migration canonicalises old saved key names.
 */
enum class FeatureId {

    // ── Media & Entertainment ─────────────────────────────────────────────
    AI_IMAGE_GENERATOR,
    MUSIC_STREAMING,
    SMART_IMAGE_EDITOR,
    NEXUS_IMAGE_VIEWER,

    // ── Productivity ──────────────────────────────────────────────────────
    PDF_SUITE,
    FILE_MANAGER,
    ALARM_CLOCK,
    CLIPBOARD_MANAGER,
    JSON_FORMATTER,
    REGEX_TESTER,
    DOC_HUB,
    NEXUS_DOC_READER,
    TEXT_TO_PDF,
    DAILY_JOURNAL,

    // ── Utilities ─────────────────────────────────────────────────────────
    NEXUS_TTS,
    VOICE_TYPER,
    CURRENCY_CONVERTER,
    UNIT_CONVERTER,
    CALCULATOR_CENTER,
    STOPWATCH,
    WORLD_CLOCK,
    TEXT_TRANSLATOR,
    MORSE_CODE,
    NUMBER_SYSTEM,
    WEATHER,
    MY_REMINDER,
    NEWS,
    SCIENCE,
    COLOR_PALETTE,

    // ── Smart Tools ───────────────────────────────────────────────────────
    QR_GENERATOR,
    FLASHLIGHT,
    COMPASS,
    SPEEDOMETER,
    VOICE_RECORDER,
    WIFI_ANALYZER,
    BATTERY_MONITOR,
    STORAGE_ANALYZER,
    OBJECT_DETECTOR,
    COLOR_DETECTOR,
    BARCODE_GENERATOR,
    TASK_MANAGER,
    APP_INFO_CENTER,
    NETWORK_INFO,

    // ── AI ────────────────────────────────────────────────────────────────
    AIRA_AI,

    // ── Security & Privacy ────────────────────────────────────────────────
    EMERGENCY_GUARDIAN,
    BIOMETRIC_VAULT,
    ENCRYPTER_DECRYPTER,
    HASH_GENERATOR,
    PASSWORD_GENERATOR,
    BASE64_TOOL,
    ENCRYPTED_NOTES,
    CONTACT_BACKUP,
    NEXUS_HEALTH_VAULT,
    TOTP_AUTHENTICATOR,

    // ── Smart Tools (additional) ──────────────────────────────────────────
    NETWORK_SPEED_TEST,

    // ── Utilities (additional) ────────────────────────────────────────────
    NEXUS_DIALER,
    TEXT_ANALYZER,
    URL_SHORTENER,

    // ── Entertainment ─────────────────────────────────────────────────────
    NEXUS_GAMES,

    // ── Finance ───────────────────────────────────────────────────────────
    EXPENSE_TRACKER,

    // v1.4.0
    SMART_DOCUMENT_SCANNER,
    VIDEO_DESCRIPTION,
    QR_CODE_SCANNER,

    // ── Legacy backward-compat aliases ────────────────────────────────────
    RADIO_PLAYER,      // removed feature — kept for DataStore migration
    IPTV_PLAYER,       // removed feature — kept for DataStore migration
    FORM_X,            // removed feature — kept for DataStore migration
    SCREEN_RECORDER,   // removed feature — kept for DataStore migration
    APP_LOCKER,        // removed feature — kept for DataStore migration
    PDF_READER,        // → PDF_SUITE
    TEXT_ENCRYPTOR,    // → ENCRYPTER_DECRYPTER
    QR_SCANNER,        // → QR_GENERATOR
    CALCULATOR,        // → CALCULATOR_CENTER
}
