package com.nexuswavetech.nexusplus.features.tts

/**
 * KMP-compatible locale representation used throughout the NSE engine.
 * Replaces [java.util.Locale] so that commonMain code compiles on iOS/Desktop.
 *
 * Android and Desktop (JVM) can convert to a real [java.util.Locale] via
 * an internal extension in their respective source sets.
 */
data class NseLocale(
    val language: String,
    val country:  String = "",
) {
    /** Returns a BCP-47 language tag, e.g. "en-US" or "hi-IN". */
    fun toLanguageTag(): String =
        if (country.isEmpty()) language else "$language-$country"

    companion object {
        val ENGLISH           = NseLocale("en", "US")
        val SIMPLIFIED_CHINESE = NseLocale("zh", "CN")
        val JAPANESE          = NseLocale("ja", "JP")
        val KOREAN            = NseLocale("ko", "KR")
        val FRENCH            = NseLocale("fr", "FR")
        val GERMAN            = NseLocale("de", "DE")

        fun forLanguageTag(tag: String): NseLocale {
            val parts = tag.replace('_', '-').split('-')
            return NseLocale(
                language = parts.getOrElse(0) { "en" },
                country  = parts.getOrElse(1) { "" },
            )
        }
    }
}
