---
name: Nexus Plus rebuild decisions
description: Durable constraints and decisions from the Nexus Plus Android app rebuild — login gate, deleted features, TTS redesign, social links, theme, new features, compile fixes.
---

## Login Gate — AppConfig.LOGIN_REQUIRED
- File: `NexusPlus/composeApp/src/androidMain/kotlin/com/nexuswavetech/nexusplus/core/AppConfig.kt`
- `LOGIN_REQUIRED = false` → app goes straight to `Screen.Main`, skipping WelcomeScreen entirely.
- `NexusNavHost.kt` uses: `startDestination = if (AppConfig.LOGIN_REQUIRED) Screen.Welcome.route else Screen.Main.route`
- **Why:** Login is temporarily disabled while Google Sign-In is being re-integrated. Single flag re-enables all feature gates.

## Deleted Features (files + catalog entries removed in earlier session)
- Online Radio (`features/radio/`) — deleted
- IPTV/Live TV (`features/iptv/`) — deleted
- FormX (`features/formx/`) — deleted
- `Screen.RadioPlayer`, `Screen.IptvPlayer`, `Screen.FormX` still exist in Screen.kt (kept to avoid cascading compile errors in iOS nav host)
- `FeatureCatalog.kt` entries for all three removed
- `AppModule.kt` — RadioViewModel and IptvViewModel imports and viewModel{} registrations removed

## Stub Features Removed (current session)
- **Screen Recorder** (`Screen.Stub.route + "/screenrecorder"`) — was never implemented; removed from FeatureCatalog.
- **App Locker** (`Screen.Stub.route + "/applocker"`) — was never implemented; removed from FeatureCatalog.
- Both kept as legacy aliases in FeatureId (DataStore migration safety) but not shown in the UI.
- **Why:** Stub routes showed an unhelpful placeholder screen; replaced with real features.

## New Features Added (current session — replacing stubs)
- **Installed Apps** (`AppInfoCenterScreen`) — user-installed app browser; name, version, APK size, install date; search; tap → Settings.
  - Package: `features/appinfo/AppInfoCenterScreen.kt`
  - Route: `Screen.AppInfoCenter` → `"feature/app_info_center"`, FeatureId: `APP_INFO_CENTER`
- **Network Info** (`NetworkInfoScreen`) — connection type, local IP, public IP (via api.ipify.org), Wi-Fi SSID/signal/speed/freq, DNS.
  - Package: `features/networkinfo/NetworkInfoScreen.kt`
  - Route: `Screen.NetworkInfo` → `"feature/network_info"`, FeatureId: `NETWORK_INFO`
- Both registered in NexusNavHost with NexusAdScaffold wrapper.

## Contact Backup — Wired Up (current session)
- `ContactBackupScreen.kt` existed but FeatureCatalog was pointing to `Screen.Stub.route + "/contacts"`.
- Fixed: FeatureCatalog now uses `Screen.ContactBackup.route` (`"feature/contact_backup"`).
- NavHost composable added for `Screen.ContactBackup`.

## EncrypterDecrypterScreen — Complete Rewrite (current session)
- Old version had: hardcoded fallback key, reflection-based Google Password Manager hack, jargon-heavy error messages, no contentDescriptions.
- New version: AES-256-CBC + PBKDF2-HMAC-SHA256, user always provides password, two tabs (Text / File), full contentDescriptions, all MaterialTheme colors, no emoji.
- File-level `@file:OptIn(ExperimentalMaterial3Api::class)` for OutlinedCard.

## FeatureId Compile Error Fixed (current session)
- `FeatureId.kt` had TEXT_TO_PDF, DAILY_JOURNAL, COLOR_PALETTE defined twice — Kotlin enum compile error.
- Fixed by removing duplicate section. Also removed APP_LOCKER and SCREEN_RECORDER from active enum values (kept as legacy aliases).
- **Why:** Duplicate enum constants are a compile error in Kotlin.

## Google Services JSON (current session)
- `app/google-services.json` deleted from repo and added to `.gitignore`.
- `app/build.gradle.kts` `prepareGoogleServicesJson` task behaviour:
  1. `GOOGLE_SERVICES_JSON` env var set → writes from env (CI/CD).
  2. File already present locally → skips.
  3. Neither → auto-generates a local debug placeholder (prevents gradle build failure).
- Placeholder uses package `com.nexuswavetech.nexusplus` and dummy values — for debug only.

## Theme System
- Full Light + Dark palette in commonMain:
  - `Color.kt` — NexusPrimary*/NexusPrimaryLight* tokens; `outlineVariant` token added to both schemes.
  - `Type.kt` — NexusTypography full M3 scale
  - `Shape.kt` — NexusShapes + convenience tokens
  - `NexusPlusTheme.kt` — wraps MaterialTheme; reads SettingsRepository.theme (DARK/LIGHT/SYSTEM)
- Platform actuals: androidMain uses Material You on API 31+; iOS/Desktop are no-op.
- All screens must use `MaterialTheme.colorScheme.*` — no hardcoded Color() in composables.

## Social Media Screen (earlier session)
- Removed: Twitter/X, YouTube, GitHub, LinkedIn
- Kept: Official Website, Instagram, Facebook, WhatsApp, Telegram, Discord, Support Email

## NSE TTS Screen (earlier session)
- Complete redesign — see original notes for section breakdown.

## Web Showcase
- Radio and IPTV feature cards removed from index.html (earlier session)
- Tagline updated to "37+ powerful utilities"
