package com.nexuswavetech.nexusplus.remoteconfig

/**
 * All Firebase Remote Config keys used throughout Nexus Plus.
 * Change values directly in Firebase Console → Remote Config to update
 * the app without publishing a new APK.
 */
object RemoteConfigKeys {

    // ── Authentication ───────────────────────────────────────────────────────
    /** Show / hide the "Sign in with Google" button on the Welcome screen. */
    const val GOOGLE_SIGNIN_ENABLED = "google_signin_enabled"

    // ── Social / Community links ─────────────────────────────────────────────
    const val INSTAGRAM_URL      = "instagram_url"
    const val FACEBOOK_URL       = "facebook_url"
    const val TWITTER_URL        = "twitter_url"
    const val YOUTUBE_URL        = "youtube_url"
    const val TELEGRAM_URL       = "telegram_url"
    const val WHATSAPP_URL       = "whatsapp_url"
    const val DISCORD_URL        = "discord_url"
    const val GITHUB_URL         = "github_url"
    const val LINKEDIN_URL       = "linkedin_url"
    const val OFFICIAL_WEBSITE   = "official_website_url"

    // ── Contact / Support ────────────────────────────────────────────────────
    const val SUPPORT_EMAIL      = "support_email"
    const val CONTACT_EMAIL      = "contact_email"

    // ── Legal ────────────────────────────────────────────────────────────────
    const val TERMS_URL          = "terms_url"
    const val PRIVACY_URL        = "privacy_url"

    // ── Update dialog ────────────────────────────────────────────────────────
    /** Set to true to show the update prompt dialog on next app launch. */
    const val UPDATE_DIALOG_ENABLED  = "update_dialog_enabled"
    const val UPDATE_DIALOG_TITLE    = "update_dialog_title"
    const val UPDATE_DIALOG_MESSAGE  = "update_dialog_message"
    const val UPDATE_DIALOG_URL      = "update_dialog_url"
    /** Minimum versionCode required; dialog shown when installed < this value. 0 = always. */
    const val UPDATE_MIN_VERSION     = "update_dialog_min_version"

    // ── Announcement banner ──────────────────────────────────────────────────
    const val ANNOUNCEMENT_ENABLED  = "app_announcement_enabled"
    const val ANNOUNCEMENT_TEXT     = "app_announcement"

    // ── Feature premium flags ────────────────────────────────────────────────
    const val FEATURE_AIRA_PREMIUM           = "feature_aira_premium"
    const val FEATURE_BIOMETRIC_VAULT_PREMIUM = "feature_biometric_vault_premium"
    const val FEATURE_HEALTH_VAULT_PREMIUM   = "feature_health_vault_premium"
    const val FEATURE_NSE_PREMIUM            = "feature_nse_premium"
    const val FEATURE_OTT_PREMIUM            = "feature_nexus_ott_premium"
    const val FEATURE_IMAGE_EDITOR_PREMIUM   = "feature_image_editor_premium"
    const val FEATURE_AI_IMAGE_PREMIUM       = "feature_ai_image_premium"
    const val FEATURE_EXPENSE_TRACKER_PREMIUM = "feature_expense_tracker_premium"
    const val FEATURE_VOICE_RECORDER_PREMIUM = "feature_voice_recorder_premium"
}

/**
 * In-app default values for Remote Config.
 * These are used immediately on first launch (before a fetch completes)
 * and whenever the network is unavailable.
 */
object RemoteConfigDefaults {

    val defaults: Map<String, Any> = mapOf(

        // Auth
        RemoteConfigKeys.GOOGLE_SIGNIN_ENABLED to true,

        // Social links (exact current URLs as safe defaults)
        RemoteConfigKeys.INSTAGRAM_URL    to "https://www.instagram.com/nexuswave_technologies?igsh=MTBia3ZxODcwOTFrNg==",
        RemoteConfigKeys.FACEBOOK_URL     to "https://www.facebook.com/profile.php?id=61590971301245",
        RemoteConfigKeys.TWITTER_URL      to "https://x.com/nexuswavetech?s=09",
        RemoteConfigKeys.YOUTUBE_URL      to "https://youtube.com/@nexuswavetech?si=dxhKCVUZjfh85nVj",
        RemoteConfigKeys.TELEGRAM_URL     to "https://t.me/NexusWaveTechnologies27",
        RemoteConfigKeys.WHATSAPP_URL     to "https://whatsapp.com/channel/0029VbDI2cL42Dcc9m6nfm3T",
        RemoteConfigKeys.DISCORD_URL      to "https://discord.gg/3yp8MMwJe",
        RemoteConfigKeys.GITHUB_URL       to "https://github.com/NexusWaveTechnologies",
        RemoteConfigKeys.LINKEDIN_URL     to "",
        RemoteConfigKeys.OFFICIAL_WEBSITE to "https://nexuswavetech.com",

        // Contact
        RemoteConfigKeys.SUPPORT_EMAIL    to "nexuswave@zohomail.in",
        RemoteConfigKeys.CONTACT_EMAIL    to "nexuswave@zohomail.in",

        // Legal
        RemoteConfigKeys.TERMS_URL        to "https://nexuswavetech.com/terms",
        RemoteConfigKeys.PRIVACY_URL      to "https://nexuswavetech.com/privacy",

        // Update dialog (off by default)
        RemoteConfigKeys.UPDATE_DIALOG_ENABLED  to false,
        RemoteConfigKeys.UPDATE_DIALOG_TITLE    to "Update Available",
        RemoteConfigKeys.UPDATE_DIALOG_MESSAGE  to "A new version of Nexus Plus is available with exciting improvements. Please update to continue.",
        RemoteConfigKeys.UPDATE_DIALOG_URL      to "https://play.google.com/store/apps/details?id=com.nexuswavetech.nexusplus",
        RemoteConfigKeys.UPDATE_MIN_VERSION     to 0L,

        // Announcement (off by default)
        RemoteConfigKeys.ANNOUNCEMENT_ENABLED   to false,
        RemoteConfigKeys.ANNOUNCEMENT_TEXT      to "",

        // Premium features (all free by default)
        RemoteConfigKeys.FEATURE_AIRA_PREMIUM            to false,
        RemoteConfigKeys.FEATURE_BIOMETRIC_VAULT_PREMIUM to false,
        RemoteConfigKeys.FEATURE_HEALTH_VAULT_PREMIUM    to false,
        RemoteConfigKeys.FEATURE_NSE_PREMIUM             to false,
        RemoteConfigKeys.FEATURE_OTT_PREMIUM             to false,
        RemoteConfigKeys.FEATURE_IMAGE_EDITOR_PREMIUM    to false,
        RemoteConfigKeys.FEATURE_AI_IMAGE_PREMIUM        to false,
        RemoteConfigKeys.FEATURE_EXPENSE_TRACKER_PREMIUM to false,
        RemoteConfigKeys.FEATURE_VOICE_RECORDER_PREMIUM  to false,
    )
}
