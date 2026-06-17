package com.nexuswavetech.nexusplus

import androidx.compose.ui.window.ComposeUIViewController
import com.nexuswavetech.nexusplus.di.commonModule
import com.nexuswavetech.nexusplus.platform.SettingsStore
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * iOS entry point — ComposeUIViewController.
 *
 * Creates a UIKit view controller backed by Compose Multiplatform.
 */
fun MainViewController() = ComposeUIViewController {
    // Initialize Koin with common module + iOS-specific bindings
    startKoin {
        modules(
            commonModule,
            module {
                // iOS-specific SettingsStore using NSUserDefaults
                single<SettingsStore> { SettingsStore() }
            }
        )
    }

    // Main app content
    App()
}

