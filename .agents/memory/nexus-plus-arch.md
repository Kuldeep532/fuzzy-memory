---
name: Nexus Plus v1.2.0 Architecture
description: Key conventions, patterns, and gotchas for the NexusPlus Android project (Kotlin/Compose/Koin).
---

## Package & DI
- App package: `com.nexuswavetech.nexusplus`
- DI: Koin — singletons in `appModule`, ViewModels via `viewModel { ... }`
- New ViewModels defined *inside* their Screen.kt file (co-location pattern for newer features)
- AppModule must import the ViewModel class from its Screen.kt file (e.g., `EncrypterDecrypterViewModel` from `features.encryptor`)

## Navigation
- Single-activity NavHost in `NexusNavHost.kt`
- Three bottom tabs: `Home` (pinned), `All Features`, `More`
- `BottomTab` sealed class route: `tab/home`, `tab/all_features`, `tab/more`
- `Screen.kt` defines all feature routes; `FeatureId.kt` has all feature IDs
- Legacy routes (`TextEncryptor`, `PdfReader`) kept in NavHost for backwards-compat with saved favorites

## FavoritesRepository
- Uses DataStore; only suspend function is `toggleFavorite(featureId: FeatureId)`
- HomeScreen calls it inside `scope.launch {}` (rememberCoroutineScope)
- `favoriteIds: Flow<Set<String>>` — IDs stored as `FeatureId.name` strings

## Key conventions
- `NexusGatekeeper.checkAccess()` returns `Allowed` or `Blocked(featureName)`; must add new FeatureIds to the map
- `FeatureCatalog.allFeatures` is the single source of truth for all feature metadata
- Stub route pattern: `Screen.Stub.route + "/feature_key"` for unimplemented features
- `NexusPlaybackService` (Media3 MediaSessionService) handles background audio for Radio, IPTV, Music

## New in v1.2.0
- 7 new features: Biometric Vault, My Reminder (WorkManager), QR Code Generator (ZXing), Calculator Center, Doc Hub, Voice Typer (SpeechRecognizer), PDF Suite (6 tools)
- Encrypter and Decrypter: extended from text-only to text+image+file (AES-256 CBC)
- IPTV: India as default region, region picker enum, group filter chips
- Music: Offline tab (MediaStore scan) + Online tab (JioSaavn saavn.dev API)
- Favorites tab renamed to Home tab

**Why:** These notes prevent re-deriving conventions from code during future feature additions.
