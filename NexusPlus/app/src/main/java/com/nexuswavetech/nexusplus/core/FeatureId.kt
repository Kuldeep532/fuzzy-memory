package com.nexuswavetech.nexusplus.core

/**
 * Canonical identifier for every feature in Nexus Plus.
 *
 * Rules:
 *  - One entry per feature, never duplicated.
 *  - TRANSLATION_ENGINE removed: was never in FeatureCatalog; was a dead
 *    Gatekeeper entry with no route, no screen, no FeatureItem.
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

    // ── Productivity ──────────────────────────────────────────────────────
    PDF_SUITE,
    FILE_MANAGER,
    ALARM_CLOCK,
    CLIPBOARD_MANAGER,
    JSON_FORMATTER,
    REGEX_TESTER,
    DOC_HUB,

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

    // ── Security & Privacy ────────────────────────────────────────────────
    BIOMETRIC_VAULT,
    ENCRYPTER_DECRYPTER,
    HASH_GENERATOR,
    PASSWORD_GENERATOR,
    BASE64_TOOL,
    ENCRYPTED_NOTES,
    APP_LOCKER,
    CONTACT_BACKUP,

    // ── Platform-level Evolution Systems ─────────────────────────────────
    NEXUS_INTELLIGENCE,
    NEXUS_AUTOMATION,
    NEXUS_DEV_KIT,
    NEXUS_HEALTH_VAULT,

    // ── Legacy backward-compat aliases ────────────────────────────────────
    PDF_READER,      // → PDF_SUITE
    TEXT_ENCRYPTOR,  // → ENCRYPTER_DECRYPTER
    QR_SCANNER,      // → QR_GENERATOR
    CALCULATOR,      // → CALCULATOR_CENTER
}
