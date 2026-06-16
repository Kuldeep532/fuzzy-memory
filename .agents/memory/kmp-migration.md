---
name: KMP migration and ad placement
description: How the Compose Multiplatform file structure was set up and where ads are allowed
---

## KMP File Structure (Pragmatic approach)

All source files migrated from `NexusPlus/app/src/main/java/` to:
```
NexusPlus/composeApp/src/
  androidMain/kotlin/com/nexuswavetech/nexusplus/
    ads/, auth/, core/, di/, features/, legal/, navigation/, ui/
  commonMain/kotlin/com/nexuswavetech/nexusplus/
    navigation/Screen.kt   ← ONLY pure Kotlin sealed class, no Android imports
  iosMain/kotlin/com/nexuswavetech/nexusplus/
    platform/Platform.kt   ← iOS stub
NexusPlus/app/src/main/java/com/nexuswavetech/nexusplus/
  MainActivity.kt          ← thin launcher only
  NexusPlusApplication.kt  ← thin launcher only
```

**Why:** No separate Gradle `:composeApp` module needed. `app/build.gradle.kts` adds composeApp dirs as additional sourceSets:
```kotlin
sourceSets {
    getByName("main") {
        java.srcDirs("src/main/java", "../composeApp/src/commonMain/kotlin", "../composeApp/src/androidMain/kotlin")
    }
}
```

**How to apply:** Any new Kotlin source file goes into `composeApp/src/androidMain/` (if it has Android APIs) or `composeApp/src/commonMain/` (if pure Kotlin/pure Compose). Never add new files back to `app/src/main/java/`.

## Ad Placement Rule

`NexusBannerAd` is allowed ONLY on network-dependent / API-cost features:
- ✅ AiraAiScreen (between LazyColumn and input Surface)
- ✅ RadioPlayerScreen (last item in LazyColumn)
- ✅ IptvPlayerScreen (last item in LazyColumn)
- ✅ NexusWeatherScreen (below scrollable content, above Scaffold bottom)
- ✅ NetworkSpeedTestScreen (if added)
- ❌ HomeScreen — NO ads (shown always to all users)
- ❌ All offline features (Calculator, QR, Compass, Storage, etc.)

**Why:** User specified "sirf main features par dikhai de jahan par kaam karne ke liye paise kharch hote hain" — only on features that incur operational costs (API calls, streaming bandwidth).
