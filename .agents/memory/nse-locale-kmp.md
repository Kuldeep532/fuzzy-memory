---
name: NseLocale KMP type
description: How java.util.Locale was replaced with NseLocale for KMP compatibility across all NSE engine files.
---

## Rule
`java.util.Locale` is JVM-only and must NOT appear in any `commonMain` file. Use `NseLocale(language: String, country: String = "")` instead.

## Where NseLocale lives
`commonMain/features/tts/NseLocale.kt` — data class with `toLanguageTag()`, companion constants (ENGLISH, SIMPLIFIED_CHINESE, JAPANESE, KOREAN, FRENCH, GERMAN) and `forLanguageTag(tag)`.

## androidMain conversion pattern
Both `NseAndroidEngine.kt` and `NsePipelineAndroidEngine.kt` define a local extension:
```kotlin
private fun NseLocale.toJavaLocale(): java.util.Locale =
    if (country.isEmpty()) java.util.Locale(language) else java.util.Locale(language, country)
```
Use this wherever Android TTS API (`isLanguageAvailable`, `engine.language =`) requires `java.util.Locale`.

## NsePipelineEngine marker interface
Defined in `NseRepository.kt` (commonMain):
```kotlin
interface NsePipelineEngine : NseEngine {
    fun cachedPhraseCount(): Int
}
```
`NsePipelineAndroidEngine` (androidMain) implements `NsePipelineEngine` (not just `NseEngine`).
This lets `NseRepository.isPipelineEngine` check `engine is NsePipelineEngine` without a direct androidMain dependency.

## Voice profile creation
When building `NseVoiceProfile` from an Android `Voice`, convert:
```kotlin
locale = NseLocale(v.locale.language, v.locale.country)
```

## NseAccessibilityService
Uses `Locale.getDefault()` (java.util.Locale, fine in androidMain) but must convert to NseLocale before passing to NseSpeechMode:
```kotlin
NseLocale(Locale.getDefault().language, Locale.getDefault().country)
```

## Why
iOS/Native targets don't have `java.util.*`. All commonMain code must compile on iOS, Desktop, AND Android. JVM imports in commonMain → iOS compilation failure.
