package com.nexuswavetech.nexusplus.core

/**
 * Represents the current authenticated session of the user.
 */
sealed class UserSession {
    /** User is fully authenticated via Google / Firebase. */
    data class Authenticated(
        val uid: String,
        val displayName: String,
        val email: String,
        val photoUrl: String? = null,
        val isAdmin: Boolean  = false,
    ) : UserSession()

    /** User chose the guest flow and provided their name. */
    data class Guest(val name: String) : UserSession()

    /** No session established yet — app is at the Welcome screen. */
    object None : UserSession()
}

/** Returns true when the session belongs to a guest user. */
val UserSession.isGuest: Boolean
    get() = this is UserSession.Guest

/** Returns a display name regardless of session type. */
val UserSession.displayName: String
    get() = when (this) {
        is UserSession.Authenticated -> displayName
        is UserSession.Guest -> name
        is UserSession.None -> ""
    }
