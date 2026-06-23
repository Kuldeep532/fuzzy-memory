package com.nexuswavetech.nexusplus.core

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              NEXUS PLUS  —  GLOBAL FEATURE FLAGS               ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 *  LOGIN_REQUIRED
 * ─────────────────────────────────────────────────────────────────
 *   false (current) → App opens directly to Home screen.
 *                     No login / sign-in required for any feature.
 *
 *   true            → WelcomeScreen is shown on every cold start.
 *                     All features are gated behind authentication.
 *
 *  ► HOW TO RE-ENABLE LOGIN IN ONE LINE:
 *        Change  `false`  →  `true`  below, then rebuild.
 *        Every feature in the app will automatically require login.
 *
 *  ► FIREBASE REMOTE CONFIG (optional runtime control):
 *        Replace the constant with:
 *        val LOGIN_REQUIRED = remoteConfig.getBoolean("login_required")
 *        and set  login_required = true/false  in the Firebase console.
 */
object AppConfig {

    /**
     * Master login gate.
     * false = no login required (current state — Google Sign-In being re-integrated)
     * true  = show WelcomeScreen and block all features until authenticated
     */
    const val LOGIN_REQUIRED: Boolean = false
}
