package com.nexuswavetech.nexusplus.shared.domain

/**
 * Shared — platform-agnostic feature domain types.
 *
 * [FeatureCategory] and [FeatureAccessLevel] are pure Kotlin enums usable
 * on every KMP target.  Platform-specific data (ImageVector icons, routes)
 * stays in the Android :app module's FeatureItem / FeatureCatalog.
 */

enum class FeatureCategory(val label: String) {
    MEDIA       ("Media & Entertainment"),
    PRODUCTIVITY("Productivity"),
    UTILITIES   ("Utilities"),
    TOOLS       ("Smart Tools"),
    SECURITY    ("Security & Privacy"),
}

enum class FeatureAccessLevel {
    PUBLIC,
    AUTHENTICATED_ONLY,
}
