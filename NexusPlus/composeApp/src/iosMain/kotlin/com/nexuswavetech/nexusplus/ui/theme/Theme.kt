package com.nexuswavetech.nexusplus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun resolveColorScheme(isDark: Boolean, dynamicColor: Boolean): ColorScheme {
    return if (isDark) DarkColorScheme else LightColorScheme
}

@Composable
actual fun PlatformSystemBarsEffect(isDark: Boolean) {
    // iOS system bar appearance is handled via UIKit / SwiftUI bridge
    // No-op in Compose layer
}
