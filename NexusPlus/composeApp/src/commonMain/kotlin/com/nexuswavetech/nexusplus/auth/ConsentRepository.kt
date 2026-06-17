package com.nexuswavetech.nexusplus.auth

import com.nexuswavetech.nexusplus.platform.SettingsStore
import kotlinx.coroutines.flow.Flow

/**
 * Persists the user's legal consent decisions across app restarts.
 *
 * Both Privacy Policy and Terms & Conditions must be accepted before
 * authentication is permitted (Compliance Gate per the security directive).
 *
 * Backed by [SettingsStore] — DataStore on Android, NSUserDefaults on iOS.
 */
class ConsentRepository(private val store: SettingsStore) {

    companion object {
        private const val KEY_PRIVACY_ACCEPTED = "privacy_policy_accepted"
        private const val KEY_TERMS_ACCEPTED   = "terms_conditions_accepted"
    }

    val privacyAccepted: Flow<Boolean> = store.booleanFlow(KEY_PRIVACY_ACCEPTED, false)

    val termsAccepted: Flow<Boolean> = store.booleanFlow(KEY_TERMS_ACCEPTED, false)

    suspend fun setPrivacyAccepted(accepted: Boolean) {
        store.setBoolean(KEY_PRIVACY_ACCEPTED, accepted)
    }

    suspend fun setTermsAccepted(accepted: Boolean) {
        store.setBoolean(KEY_TERMS_ACCEPTED, accepted)
    }

    suspend fun resetConsent() {
        store.setBoolean(KEY_PRIVACY_ACCEPTED, false)
        store.setBoolean(KEY_TERMS_ACCEPTED, false)
    }
}
