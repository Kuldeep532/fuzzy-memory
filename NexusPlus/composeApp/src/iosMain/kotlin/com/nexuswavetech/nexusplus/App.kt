package com.nexuswavetech.nexusplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.nexuswavetech.nexusplus.ui.theme.NexusPlusTheme

/**
 * iOS app root composable.
 */
@Composable
actual fun App() {
    NexusPlusTheme {
        // Placeholder: iOS navigation will use NavigationScreenRegistry
        // For now, show a simple placeholder
        androidx.compose.material3.Text(
            "Nexus Plus on iOS",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
