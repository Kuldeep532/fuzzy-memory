package com.nexuswavetech.nexusplus.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Wraps Firebase Remote Config and exposes typed accessors.
 *
 * ## How to use
 * 1. Go to Firebase Console → Remote Config.
 * 2. Create a parameter using any key from [RemoteConfigKeys].
 * 3. Set the value you want.
 * 4. Publish the change.
 * 5. The app will pick it up within 1 hour (or on the next cold start after the cache expires).
 *
 * ## Examples
 * - Disable Google Sign-In:  `google_signin_enabled = false`
 * - Change Instagram link:   `instagram_url = https://instagram.com/your_new_handle`
 * - Show update dialog:      `update_dialog_enabled = true`
 *                             `update_dialog_message = Please update to v2.0!`
 *                             `update_dialog_url = https://play.google.com/...`
 * - Lock a feature as paid:  `feature_aira_premium = true`
 * - Show announcement:       `app_announcement_enabled = true`
 *                             `app_announcement = 🎉 Nexus Plus v2 is here!`
 */
class RemoteConfigRepository {

    private val config: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600L)
            .build()
        config.setConfigSettingsAsync(settings)
        config.setDefaultsAsync(RemoteConfigDefaults.defaults)
    }

    // ── Fetch ────────────────────────────────────────────────────────────────

    /**
     * Non-blocking fetch + activate.
     * Automatically called from Application.onCreate() via Koin eager init.
     * Latest values become available after the next app foreground (within 1 h TTL).
     */
    fun fetchAndActivate() {
        config.fetchAndActivate()
    }

    // ── Typed accessors ──────────────────────────────────────────────────────

    fun getString(key: String): String  = config.getString(key)
    fun getBoolean(key: String): Boolean = config.getBoolean(key)
    fun getLong(key: String): Long       = config.getLong(key)
    fun getDouble(key: String): Double   = config.getDouble(key)

    // ── Convenience helpers ──────────────────────────────────────────────────

    val googleSignInEnabled: Boolean
        get() = getBoolean(RemoteConfigKeys.GOOGLE_SIGNIN_ENABLED)

    val instagramUrl: String  get() = getString(RemoteConfigKeys.INSTAGRAM_URL)
    val facebookUrl: String   get() = getString(RemoteConfigKeys.FACEBOOK_URL)
    val twitterUrl: String    get() = getString(RemoteConfigKeys.TWITTER_URL)
    val youtubeUrl: String    get() = getString(RemoteConfigKeys.YOUTUBE_URL)
    val tiktokUrl: String     get() = getString(RemoteConfigKeys.TIKTOK_URL)
    val telegramUrl: String   get() = getString(RemoteConfigKeys.TELEGRAM_URL)
    val whatsappUrl: String   get() = getString(RemoteConfigKeys.WHATSAPP_URL)
    val discordUrl: String    get() = getString(RemoteConfigKeys.DISCORD_URL)
    val githubUrl: String     get() = getString(RemoteConfigKeys.GITHUB_URL)
    val linkedinUrl: String   get() = getString(RemoteConfigKeys.LINKEDIN_URL)
    val officialWebsiteUrl: String get() = getString(RemoteConfigKeys.OFFICIAL_WEBSITE)

    val supportEmail: String  get() = getString(RemoteConfigKeys.SUPPORT_EMAIL)
    val contactEmail: String  get() = getString(RemoteConfigKeys.CONTACT_EMAIL)

    val termsUrl: String      get() = getString(RemoteConfigKeys.TERMS_URL)
    val privacyUrl: String    get() = getString(RemoteConfigKeys.PRIVACY_URL)

    val updateDialogEnabled: Boolean get() = getBoolean(RemoteConfigKeys.UPDATE_DIALOG_ENABLED)
    val updateDialogTitle: String    get() = getString(RemoteConfigKeys.UPDATE_DIALOG_TITLE)
    val updateDialogMessage: String  get() = getString(RemoteConfigKeys.UPDATE_DIALOG_MESSAGE)
    val updateDialogUrl: String      get() = getString(RemoteConfigKeys.UPDATE_DIALOG_URL)
    val updateMinVersion: Long       get() = getLong(RemoteConfigKeys.UPDATE_MIN_VERSION)

    val announcementEnabled: Boolean get() = getBoolean(RemoteConfigKeys.ANNOUNCEMENT_ENABLED)
    val announcementText: String     get() = getString(RemoteConfigKeys.ANNOUNCEMENT_TEXT)

    fun isFeaturePremium(key: String): Boolean = getBoolean(key)
}
