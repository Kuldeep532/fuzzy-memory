package com.nexuswavetech.nexusplus.core

/**
 * NexusGatekeeper — centralised feature-access engine.
 *
 * Every feature is registered here with a [FeatureAccess] level.
 * Call [checkAccess] before launching any feature from the All Features tab.
 */
object NexusGatekeeper {

    /** Access requirements for a feature. */
    enum class FeatureAccess {
        /** Freely available to every user, including guests. */
        PUBLIC,
        /** Requires a fully authenticated (Google) account. */
        AUTHENTICATED_ONLY
    }

    /** Result of an access check. */
    sealed class AccessResult {
        object Allowed : AccessResult()
        data class Blocked(val featureName: String) : AccessResult()
    }

    /**
     * Canonical capability matrix.
     * Add new features here as the app grows — navigation logic never changes.
     */
    private val featureAccessMap: Map<FeatureId, FeatureAccess> = mapOf(
        FeatureId.RADIO_PLAYER         to FeatureAccess.PUBLIC,
        FeatureId.PDF_READER           to FeatureAccess.PUBLIC,
        FeatureId.AI_IMAGE_GENERATOR   to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.NEXUS_TTS            to FeatureAccess.PUBLIC,
        FeatureId.IPTV_PLAYER          to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.MUSIC_STREAMING      to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.FILE_MANAGER         to FeatureAccess.PUBLIC,
        FeatureId.CURRENCY_CONVERTER   to FeatureAccess.PUBLIC,
        FeatureId.ENCRYPTED_NOTES      to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.QR_SCANNER           to FeatureAccess.PUBLIC,
        FeatureId.WEATHER              to FeatureAccess.PUBLIC,
        FeatureId.UNIT_CONVERTER       to FeatureAccess.PUBLIC,
        FeatureId.CALCULATOR           to FeatureAccess.PUBLIC,
        FeatureId.FLASHLIGHT           to FeatureAccess.PUBLIC,
        FeatureId.COMPASS              to FeatureAccess.PUBLIC,
        FeatureId.SPEEDOMETER          to FeatureAccess.PUBLIC,
        FeatureId.ALARM_CLOCK          to FeatureAccess.PUBLIC,
        FeatureId.STOPWATCH            to FeatureAccess.PUBLIC,
        FeatureId.WORLD_CLOCK          to FeatureAccess.PUBLIC,
        FeatureId.CONTACT_BACKUP       to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.VOICE_RECORDER       to FeatureAccess.PUBLIC,
        FeatureId.SCREEN_RECORDER      to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.APP_LOCKER           to FeatureAccess.AUTHENTICATED_ONLY,
        FeatureId.WIFI_ANALYZER        to FeatureAccess.PUBLIC,
        FeatureId.BATTERY_MONITOR      to FeatureAccess.PUBLIC,
        FeatureId.STORAGE_ANALYZER     to FeatureAccess.PUBLIC,
        FeatureId.TASK_MANAGER         to FeatureAccess.PUBLIC,
        FeatureId.CLIPBOARD_MANAGER    to FeatureAccess.PUBLIC,
        FeatureId.TRANSLATION_ENGINE   to FeatureAccess.PUBLIC,
        FeatureId.BARCODE_GENERATOR    to FeatureAccess.PUBLIC
    )

    /**
     * Checks whether [session] may access [featureId].
     * Guest users are blocked from AUTHENTICATED_ONLY features.
     */
    fun checkAccess(featureId: FeatureId, session: UserSession, featureName: String): AccessResult {
        val required = featureAccessMap[featureId] ?: FeatureAccess.PUBLIC
        return if (required == FeatureAccess.AUTHENTICATED_ONLY && session.isGuest) {
            AccessResult.Blocked(featureName)
        } else {
            AccessResult.Allowed
        }
    }
}
