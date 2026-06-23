package com.nexuswavetech.nexusplus

import androidx.compose.runtime.Composable
import com.nexuswavetech.nexusplus.navigation.NexusIosNavHost
import com.nexuswavetech.nexusplus.ui.theme.NexusPlusTheme

/**
 * iOS actual for [App]. MainViewController calls this composable.
 * Uses NexusIosNavHost which wires up all available cross-platform features
 * and shows StubFeatureScreen for Android-only features.
 */
@Composable
actual fun App() {
    NexusPlusTheme {
        NexusIosNavHost()
    }
}
