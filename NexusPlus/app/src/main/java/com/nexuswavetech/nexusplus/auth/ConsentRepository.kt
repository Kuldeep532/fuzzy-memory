package com.nexuswavetech.nexusplus.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.consentDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "nexus_legal_consent")

/**
 * Persists the user's legal consent decisions across app restarts.
 *
 * Both Privacy Policy and Terms & Conditions must be accepted before
 * authentication is permitted (Compliance Gate per the security directive).
 *
 * DataStore is used here instead of SharedPreferences because it is
 * safe to read from coroutines and works correctly with Flow-based UIs.
 */
class ConsentRepository(private val context: Context) {

    companion object {
        private val KEY_PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
        private val KEY_TERMS_ACCEPTED   = booleanPreferencesKey("terms_conditions_accepted")
    }

    val privacyAccepted: Flow<Boolean> = context.consentDataStore.data
        .map { prefs -> prefs[KEY_PRIVACY_ACCEPTED] ?: false }

    val termsAccepted: Flow<Boolean> = context.consentDataStore.data
        .map { prefs -> prefs[KEY_TERMS_ACCEPTED] ?: false }

    suspend fun setPrivacyAccepted(accepted: Boolean) {
        context.consentDataStore.edit { prefs ->
            prefs[KEY_PRIVACY_ACCEPTED] = accepted
        }
    }

    suspend fun setTermsAccepted(accepted: Boolean) {
        context.consentDataStore.edit { prefs ->
            prefs[KEY_TERMS_ACCEPTED] = accepted
        }
    }

    suspend fun resetConsent() {
        context.consentDataStore.edit { prefs ->
            prefs[KEY_PRIVACY_ACCEPTED] = false
            prefs[KEY_TERMS_ACCEPTED]   = false
        }
    }
}
