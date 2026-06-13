---
name: Nexus Plus v1.2.0 Architecture
description: Key conventions, patterns, and gotchas for the NexusPlus Android project (Kotlin/Compose/Koin).
---

## Package & DI
- App package: `com.nexuswavetech.nexusplus`
- DI: Koin — singletons in `appModule`, ViewModels via `viewModel { ... }`
- AppModule must register new repositories as `single` and new ViewModels as `viewModel`
- FavoritesRepository now has `clearAll()` (clears both FAVORITES_KEY and PINNED_KEY)

## Navigation
- Single-activity NavHost in `NexusNavHost.kt`
- Four bottom tabs: `Home` (pinned), `Explore` (all features), `Favorites`, `More`
- `BottomTab` sealed class route: `tab/home`, `tab/explore`, `tab/favorites`, `tab/more`
- `Screen.kt` defines all feature routes; `FeatureId.kt` has all feature IDs
- Legacy routes (`TextEncryptor`, `PdfReader`) kept in NavHost for backwards-compat
- Evolution systems use `system/` prefix: `system/intelligence`, `system/automation`, `system/devkit`, `system/healthvault`

## NexusTopBar signature
```kotlin
NexusTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {})
```

## Feature catalog conventions
- `FeatureCatalog.allFeatures` — single source of truth for 53 features (v1.2)
- Stub route pattern: `Screen.Stub.route + "/feature_key"` for unimplemented features
- `isNew = true` shows "New" badge in grid

## Feature count (v1.2.0)
- Total: 53 (Media 5, Productivity 7, Utilities 13, Smart Tools 15, Security 13)
- New in v1.2: 11 features (AI Image, Smart Image Editor, Form X, Colour Detector, Biometric Vault upgraded, Nexus Intelligence, Nexus Automation, Nexus DevKit, Nexus Health Vault + more utility screens)

## Adding a new feature (checklist)
1. Add `FeatureId` enum entry in `FeatureId.kt`
2. Add `FeatureItem` in `FeatureCatalog.kt`
3. Add `Screen` object in `Screen.kt`
4. Add `composable()` route in `NexusNavHost.kt`
5. Register repository (single) and ViewModel (viewModel) in `AppModule.kt`

## Key architecture notes
- `AlarmReceiver` is top-level in `AlarmClockScreen.kt`, registered in Manifest as `.features.alarm.AlarmReceiver` — correct
- `BiometricVaultRepository` uses AES-256-GCM via Android Keystore; encryption is IV-prepended Base64
- `FLAG_SECURE` set in `BiometricVaultScreen` to block screenshots; cleared on Dispose
- `NotificationRepository` seeds 3 default notifications on first run (no stored data)
- `NexusPlaybackService` (Media3 MediaSessionService) handles background audio for Radio, IPTV, Music
- Compose BOM 2024.06.00 → Material3 1.2.1 (SwipeToDismiss still valid, not yet deprecated)

**Why:** These notes prevent re-deriving conventions from code during future feature additions.
