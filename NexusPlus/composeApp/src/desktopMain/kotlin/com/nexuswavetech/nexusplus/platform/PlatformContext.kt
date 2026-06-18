package com.nexuswavetech.nexusplus.platform

/** Desktop implementation — no platform context needed. */
actual class PlatformContext private constructor() {
    actual companion object {
        actual fun get(): PlatformContext = PlatformContext()
    }
}
