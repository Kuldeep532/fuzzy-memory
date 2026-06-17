package com.nexuswavetech.nexusplus.platform

import android.content.Context

/** Android implementation wrapping Context. */
actual class PlatformContext private constructor(private val context: Context) {
    actual companion object {
        fun get(context: Context): PlatformContext = PlatformContext(context)
    }
}
