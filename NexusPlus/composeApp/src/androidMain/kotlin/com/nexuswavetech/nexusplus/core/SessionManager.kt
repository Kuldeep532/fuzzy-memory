package com.nexuswavetech.nexusplus.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton session manager. Holds the current [UserSession] and exposes it
 * as a [StateFlow] so all composables can observe changes reactively.
 */
class SessionManager {

    private val _session = MutableStateFlow<UserSession>(UserSession.None)
    val session: StateFlow<UserSession> = _session.asStateFlow()

    fun setAuthenticatedSession(
        uid: String,
        name: String,
        email: String,
        photoUrl: String? = null,
        isAdmin: Boolean = false,
    ) {
        _session.value = UserSession.Authenticated(
            uid         = uid,
            displayName = name,
            email       = email,
            photoUrl    = photoUrl,
            isAdmin     = isAdmin,
        )
    }

    fun setGuestSession(name: String) {
        _session.value = UserSession.Guest(name = name.trim())
    }

    fun clearSession() {
        _session.value = UserSession.None
    }

    fun currentSession(): UserSession = _session.value
}
