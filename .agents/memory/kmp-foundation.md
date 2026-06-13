---
name: KMP foundation layout
description: The :shared KMP module structure, what lives in commonMain vs androidMain, and the expect/actual pattern used.
---

## Module structure
`:shared` module declared in `settings.gradle.kts`. Targets: `androidTarget` + `jvm("desktop")`.

## Plugins
- `kotlin-multiplatform` and `android-library` added to `libs.versions.toml` and root `build.gradle.kts`.
- `:app` depends on `:shared` via `implementation(project(":shared"))` in app/build.gradle.kts.

## What lives in commonMain (pure Kotlin, zero java.* imports)
- `shared/domain/UserSession.kt` — sealed class (Authenticated | Guest | None) + isGuest/displayName extensions
- `shared/domain/FeatureDomain.kt` — FeatureCategory enum, FeatureAccessLevel enum
- `shared/speech/NseSpeechState.kt` — NseSpeechMode (Auto/SingleVoice/Mix using BCP47 String), NseSpeechEngineState, NseUtteranceEvent, NseSpeechParams

## What stays in androidMain (:app module)
- Everything using java.util.Locale (NseVoiceProfile, NseLanguageDetector, NseAndroidEngine)
- All Compose/AndroidX imports
- All screens, ViewModels, DI modules

## expect/actual
- `currentTimeMillis(): Long` — expect in commonMain/speech, actual in androidMain + desktopMain (both use System.currentTimeMillis()).

**Why:** Keep language-agnostic domain contracts in shared so future iOS (via KMP/Swift interop) and Desktop targets can reuse them without touching Android code.

## Migration note
The Android `UserSession` and `FeatureCategory` in :app still exist for backward compat. New code should prefer the shared versions. A future migration can deprecate the app-local copies.
