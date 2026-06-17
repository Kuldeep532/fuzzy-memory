package com.nexuswavetech.nexusplus

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.nexuswavetech.nexusplus.di.commonModule
import com.nexuswavetech.nexusplus.platform.SettingsStore
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Desktop entry point — JVM application.
 */
fun main() = application {
    // Initialize Koin with common module + desktop-specific bindings
    startKoin {
        modules(
            commonModule,
            module {
                // Desktop-specific SettingsStore using java.util.prefs
                single<SettingsStore> { SettingsStore() }
            }
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nexus Plus",
    ) {
        App()
    }
}
