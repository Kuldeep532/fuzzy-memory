package com.nexuswavetech.nexusplus.core

/**
 * Canonical identifier for every feature in Nexus Plus.
 *
 * Rules:
 *  - One entry per feature, never duplicated.
 *  - Legacy backward-compat aliases grouped at the bottom — do NOT remove
 *    them until a DataStore migration canonicalises old saved key names.
 */
enum class FeatureId {

    // ── Media & Entertainment ─────────────────────────────────────────────
    RADIO_PLAYER,
    AI_IMAGE_GENERATOR,
    IPTV_PLAYER,
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
    FORM_X,
    NEWS,
    SCIENCE,

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
    SCREEN_RECORDER,

    // ── AI ────────────────────────────────────────────────────────────────
    AIRA_AI,

    // ── Security & Privacy ────────────────────────────────────────────────
    BIOMETRIC_VAULT,
    ENCRYPTER_DECRYPTER,
    HASH_GENERATOR,
    PASSWORD_GENERATOR,
    BASE64_TOOL,
    ENCRYPTED_NOTES,
    APP_LOCKER,
    CONTACT_BACKUP,
    NEXUS_HEALTH_VAULT,
    TOTP_AUTHENTICATOR,

    // ── Smart Tools (additional) ──────────────────────────────────────────
    NETWORK_SPEED_TEST,

    // ── Stage 4 ───────────────────────────────────────────────────────────
    NEXUS_DIALER,
    TEXT_ANALYZER,

    // ── Stage 5 ───────────────────────────────────────────────────────────
    URL_SHORTENER,

    // ── Games Hub ─────────────────────────────────────────────────────────
    NEXUS_GAMES,

    // ── OTT ───────────────────────────────────────────────────────────────
    NEXUS_OTT,

    // ── Finance ───────────────────────────────────────────────────────────────
    EXPENSE_TRACKER,

    // ── Legacy backward-compat aliases ────────────────────────────────────
    PDF_READER,      // → PDF_SUITE
    TEXT_ENCRYPTOR,  // → ENCRYPTER_DECRYPTER
    QR_SCANNER,      // → QR_GENERATOR
    CALCULATOR,      // → CALCULATOR_CENTER
}
