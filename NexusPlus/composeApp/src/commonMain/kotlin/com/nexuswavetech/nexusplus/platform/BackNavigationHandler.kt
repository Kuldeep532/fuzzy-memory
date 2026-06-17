package com.nexuswavetech.nexusplus.platform

import androidx.compose.runtime.Composable

/**
 * Platform-specific back navigation handler.
 *
 * On Android: delegates to the system back button via BackHandler.
 * On iOS/Desktop: no-op (iOS has no hardware back button, Desktop uses keyboard shortcuts).
 */
@Composable
expect fun BackNavigationHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
