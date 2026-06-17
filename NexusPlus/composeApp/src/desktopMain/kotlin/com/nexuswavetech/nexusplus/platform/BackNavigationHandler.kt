package com.nexuswavetech.nexusplus.platform

import androidx.compose.runtime.Composable

@Composable
actual fun BackNavigationHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // Desktop uses keyboard shortcuts (Alt+Left, Esc) for back navigation.
    // No-op here; handled by window-level key event listeners if needed.
}
