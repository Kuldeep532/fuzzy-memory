package com.nexuswavetech.nexusplus.platform

/**
 * Abstraction for platform-specific context operations.
 * In Android this wraps Context; in iOS it wraps the root view controller.
 */
expect class PlatformContext {
    companion object {
        fun get(): PlatformContext
    }
}
