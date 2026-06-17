package com.nexuswavetech.nexusplus.platform

import platform.UIKit.UIApplication

/** iOS implementation — no-op for now, wraps UIApplication sharedApplication. */
actual class PlatformContext private constructor() {
    actual companion object {
        fun get(): PlatformContext = PlatformContext()
    }
}
