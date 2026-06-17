package com.nexuswavetech.nexusplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.nexuswavetech.nexusplus.ui.theme.NexusPlusTheme

/**
 * Desktop app root composable.
 */
@Composable
actual fun App() {
    NexusPlusTheme {
        // Placeholder: Desktop navigation will use NavigationScreenRegistry
        androidx.compose.material3.Text(
            "Nexus Plus on Desktop",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
