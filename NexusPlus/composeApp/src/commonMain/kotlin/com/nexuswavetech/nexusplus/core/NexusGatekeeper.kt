package com.nexuswavetech.nexusplus.core

object NexusGatekeeper {

    enum class FeatureAccess { PUBLIC, AUTHENTICATED_ONLY }

    sealed class AccessResult {
        object Allowed : AccessResult()
        data class Blocked(val featureName: String) : AccessResult()
    }

    private val featureAccessMap: Map<FeatureId, FeatureAccess> = mapOf(
        // ── Public ────────────────────────────────────────────────────────────
        FeatureId.RADIO_PLAYER         to FeatureAccess.PUBLIC,
        FeatureId.PDF_SUITE            to FeatureAccess.PUBLIC,
        FeatureId.NEXUS_TTS            to FeatureAccess.PUBLIC,
        FeatureId.FILE_MANAGER         to FeatureAccess.PUBLIC,
        FeatureId.CURRENCY_CONVERTER   to FeatureAccess.PUBLIC,
        FeatureId.QR_GENERATOR         to FeatureAccess.PUBLIC,
        FeatureId.QR_SCANNER           to FeatureAccess.PUBLIC,
        FeatureId.WEATHER              to FeatureAccess.PUBLIC,
        FeatureId.UNIT_CONVERTER       to FeatureAccess.PUBLIC,
        FeatureId.CALCULATOR_CENTER    to FeatureAccess.PUBLIC,
        FeatureId.CALCULATOR           to FeatureAccess.PUBLIC,
        FeatureId.FLASHLIGHT           to FeatureAccess.PUBLIC,
        FeatureId.COMPASS              to FeatureAccess.PUBLIC,
        FeatureId.SPEEDOMETER          to FeatureAccess.PUBLIC,
        FeatureId.ALARM_CLOCK          to FeatureAccess.PUBLIC,
        FeatureId.STOPWATCH            to FeatureAccess.PUBLIC,
        FeatureId.WORLD_CLOCK          to FeatureAccess.PUBLIC,
        FeatureId.VOICE_RECORDER       to FeatureAccess.PUBLIC,
        FeatureId.WIFI_ANALYZER        to FeatureAccess.PUBLIC,
        FeatureId.BATTERY_MONITOR      to FeatureAccess.PUBLIC,
        FeatureId.STORAGE_ANALYZER     to FeatureAccess.PUBLIC,
        FeatureId.TASK_MANAGER         to FeatureAccess.PUBLIC,
        FeatureId.CLIPBOARD_MANAGER    to FeatureAccess.PUBLIC,
        FeatureId.BARCODE_GENERATOR    to FeatureAccess.PUBLIC,
        FeatureId.TEXT_ENCRYPTOR       to FeatureAccess.PUBLIC,
        FeatureId.ENCRYPTER_DECRYPTER  to FeatureAccess.PUBLIC,
        FeatureId.TEXT_TRANSLATOR      to FeatureAccess.PUBLIC,
        FeatureId.OBJECT_DETECTOR      to FeatureAccess.PUBLIC,
        FeatureId.COLOR_DETECTOR       to FeatureAccess.PUBLIC,
        FeatureId.SMART_IMAGE_EDITOR   to FeatureAccess.PUBLIC,
        FeatureId.HASH_GENERATOR       to FeatureAccess.PUBLIC,
        FeatureId.PASSWORD_GENERATOR   to FeatureAccess.PUBLIC,
        FeatureId.BASE64_TOOL          to FeatureAccess.PUBLIC,
        FeatureId.MORSE_CODE           to FeatureAccess.PUBLIC,
        FeatureId.NUMBER_SYSTEM        to FeatureAccess.PUBLIC,
        FeatureId.JSON_FORMATTER       to FeatureAccess.PUBLIC,
        FeatureId.REGEX_TESTER         to FeatureAccess.PUBLIC,
        FeatureId.VOICE_TYPER          to FeatureAccess.PUBLIC,
        FeatureId.DOC_HUB              to FeatureAccess.PUBLIC,
        FeatureId.MY_REMINDER          to FeatureAccess.PUBLIC,
        FeatureId.NEXUS_HEALTH_VAULT   to FeatureAccess.PUBLIC,
        FeatureId.AIRA_AI              to FeatureAccess.PUBLIC,
        FeatureId.NEXUS_IMAGE_VIEWER   to FeatureAccess.PUBLIC,
        FeatureId.NEXUS_DOC_READER     to FeatureAccess.PUBLIC,
        FeatureId.TOTP_AUTHENTICATOR   to FeatureAccess.PUBLIC,
        FeatureId.NETWORK_SPEED_TEST   to FeatureAccess.PUBLIC,
        FeatureId.SMART_DOCUMENT_SCANNER to FeatureAccess.PUBLIC,

        // ── Authenticated only ────────────────────────────────────────────────
        FeatureId.AI_IMAGE_GENERATOR   to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.IPTV_PLAYER          to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.MUSIC_STREAMING      to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.ENCRYPTED_NOTES      to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.CONTACT_BACKUP       to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.SCREEN_RECORDER      to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.APP_LOCKER           to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.BIOMETRIC_VAULT      to FeatureAccess.AUTHENTICATED_ONLY,
    )

    fun checkAccess(featureId: FeatureId, session: UserSession, featureName: String): AccessResult {
        val required = featureAccessMap[featureId] ?: FeatureAccess.PUBLIC
        return if (required == FeatureAccess.AUTHENTICATED_ONLY && session.isGuest) {
            AccessResult.Blocked(featureName)
        } else {
            AccessResult.Allowed
        }
    }
}
