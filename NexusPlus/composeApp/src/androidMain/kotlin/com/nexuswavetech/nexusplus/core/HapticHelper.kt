package com.nexuswavetech.nexusplus.core

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility for touch haptic feedback gated on the [SettingsRepository.touchVibration] preference.
 *
 * Usage (in a Composable):
 * ```kotlin
 * val haptic = koinInject<HapticHelper>()
 * val view   = LocalView.current
 *
 * Button(onClick = { haptic.click(view) }) { … }
 * ```
 */
class HapticHelper(private val settings: SettingsRepository) {

    /**
     * Perform a lightweight virtual-key click vibration if the user has touch vibration enabled.
     * Safe to call on the main thread (reads cached DataStore value synchronously via [runBlocking]
     * is deliberately avoided — instead the caller collects [SettingsRepository.touchVibration] as
     * a [State] and passes it in, or uses the suspend variant).
     */
    fun click(view: View, enabled: Boolean) {
        if (enabled) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * Long-press / confirm haptic — slightly stronger feedback.
     */
    fun confirm(view: View, enabled: Boolean) {
        if (enabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
