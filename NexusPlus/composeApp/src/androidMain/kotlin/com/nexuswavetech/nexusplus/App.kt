package com.nexuswavetech.nexusplus

import androidx.compose.runtime.Composable
import com.nexuswavetech.nexusplus.navigation.NexusNavHost
import com.nexuswavetech.nexusplus.ui.theme.NexusPlusTheme

/**
 * Android actual for [App]. MainActivity calls NexusNavHost directly, so this
 * composable is provided purely to satisfy the KMP expect/actual contract and
 * can also be used from Compose previews or tests.
 */
@Composable
actual fun App() {
    NexusPlusTheme {
        NexusNavHost()
    }
}
