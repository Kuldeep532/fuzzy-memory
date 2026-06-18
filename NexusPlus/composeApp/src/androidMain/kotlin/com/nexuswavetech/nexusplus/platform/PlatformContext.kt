package com.nexuswavetech.nexusplus.platform

import android.content.Context

/** Android implementation wrapping Context. */
actual class PlatformContext private constructor(val context: Context) {
    actual companion object {
        @Volatile private var appContext: Context? = null

        /** Call this once in Application.onCreate() before Koin starts. */
        fun init(ctx: Context) {
            appContext = ctx.applicationContext
        }

        actual fun get(): PlatformContext =
            PlatformContext(
                appContext ?: error(
                    "PlatformContext not initialised. " +
                        "Call PlatformContext.init(context) in Application.onCreate()."
                )
            )

        /** Convenience overload for callers that already have a Context. */
        fun get(context: Context): PlatformContext = PlatformContext(context)
    }
}
