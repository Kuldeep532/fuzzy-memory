package com.nexuswavetech.nexusplus.shared.domain

/**
 * Shared — platform-agnostic user session model.
 *
 * Lives in commonMain so it can be used identically on Android, Desktop,
 * and any future iOS target.  No java.* or platform imports.
 */
sealed class UserSession {

    /** Fully authenticated via Google / Firebase. */
    data class Authenticated(
        val uid        : String,
        val displayName: String,
        val email      : String,
        val photoUrl   : String? = null,
    ) : UserSession()

    /** Guest user who provided their name. */
    data class Guest(val name: String) : UserSession()

    /** No session established yet. */
    data object None : UserSession()
}

/** True when the session belongs to a guest. */
val UserSession.isGuest: Boolean
    get() = this is UserSession.Guest

/** Display name regardless of session type. */
val UserSession.displayName: String
    get() = when (this) {
        is UserSession.Authenticated -> displayName
        is UserSession.Guest         -> name
        is UserSession.None          -> ""
    }
