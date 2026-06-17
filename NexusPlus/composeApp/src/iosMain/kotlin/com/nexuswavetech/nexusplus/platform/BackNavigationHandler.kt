package com.nexuswavetech.nexusplus.platform

import androidx.compose.runtime.Composable

@Composable
actual fun BackNavigationHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // iOS has no hardware back button — gesture navigation is handled by the system.
    // No-op on iOS.
}
