---
name: Nexus Plus rebuild decisions
description: Durable constraints and decisions from the Nexus Plus Android app rebuild — login gate, deleted features, TTS redesign, social links, theme, new features, compile fixes, billing.
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

## EncrypterDecrypterScreen — UI Improvements
- Old version (first session): hardcoded fallback key, reflection-based Google Password Manager hack, jargon-heavy error messages, no contentDescriptions.
- Rewritten: AES-256-CBC + PBKDF2-HMAC-SHA256, user always provides password, two tabs (Text / File), full contentDescriptions.
- Latest change: TabRow replaced with full-width FilterChips row (Text + File) with leading icons — cleaner than TabRow which felt like browser tabs.
- **Why:** User found TabRow confusing — FilterChips in a Row are wider, tappable, and clearly labeled.

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
- **Why:** Screens using hardcoded Color() break in dark mode. Always use MaterialTheme tokens.

## Social Media Screen (earlier session)
- Removed: Twitter/X, YouTube, GitHub, LinkedIn
- Kept: Official Website, Instagram, Facebook, WhatsApp, Telegram, Discord, Support Email

## Screen Redesigns Completed
- **NexusTtsScreen.kt** — Latest: simplified from 1027-line 7-step wizard to a clean focused screen: large BasicTextField input, Speak/Stop button (red when speaking), Speed slider, Pitch slider, Voice dropdown (ExposedDropdownMenuBox), quick-phrase LazyRow chips, engine status banner, animated error card. Uses NseViewModel.speak(), stop(), onTextChange(), etc.
- **NexusDialerScreen.kt** — Tab strip (Keypad/Contacts), animated content switching, dialer keys with letter subtext, avatar circles for contacts, call FAB.
- **TextTranslatorScreen.kt** — Offline badge bar, card-based layout, BasicTextField (dark/light safe), animated output card, language picker buttons (not dropdowns), proper Material3 throughout.
- **QrCodeScreen.kt** — Latest: replaced horizontal scrollable FilterChips with ExposedDropdownMenuBox for QR type selection; plain-language labels (no jargon); keep ViewModel intact; show result in separate view with "Save to Gallery" and "Make Another QR Code" buttons.
- All screens: no hardcoded colors, all use MaterialTheme.colorScheme.* for full Light/Dark mode support.

## Firebase / Google Sign-In Fix — ROOT CAUSE
- **Root cause was**: `composeApp/src/androidMain/res/values/strings.xml` had hardcoded `default_web_client_id = "YOUR_WEB_CLIENT_ID"` which OVERRIDES the Google Services Plugin's auto-generated value from `google-services.json`.
- **Fix**: `composeApp/src/androidMain/res/values/strings.xml` now has only a comment — no manual string entries.
- **Rule**: NEVER manually add `default_web_client_id` to any strings.xml. The Google Services Plugin generates it automatically from google-services.json.
- `WelcomeScreen.kt` also has a placeholder-detection guard as a second layer.

## Gemini API Key Integration
- **3-level key priority**: (1) user-entered key in DataStore (Settings screen), (2) `BuildConfig.GEMINI_API_KEY` baked in at build time from `GEMINI_API_KEY` env var / GitHub Secret, (3) empty → Gemini disabled, Aira falls back to free endpoints.
- `app/build.gradle.kts`: `buildConfigField("String", "GEMINI_API_KEY", "\"${environmentVariable("GEMINI_API_KEY") ?: "\"}")`
- `GeminiRepository.kt`: `effectiveApiKey()` method implements the 3-level priority lookup.
- `SettingsScreen.kt`: New "AI & Aira" section with API key field (masked), model selector (Flash/Pro).

## Google Play Billing — ₹35/month + ₹300/year
- `libs.versions.toml`: `playBilling = "7.1.1"` + `play-billing` library entry.
- `app/build.gradle.kts`: `implementation(libs.play.billing)`.
- `BillingManager.kt` — `billing/BillingManager.kt` in androidMain. SKU: `nexusplus_monthly` (plan: `monthly_35`), `nexusplus_yearly` (plan: `yearly_300`). Handles connect-with-retry, purchase flow, acknowledgement, queryCurrentPremiumStatus.
- `PremiumRepository.kt` — thin wrapper over BillingManager; exposes `isPremium`, `purchaseMonthly()`, `purchaseYearly()`.
- `SubscriptionScreen.kt` — Beautiful paywall UI with plan cards (Monthly ₹35 / Yearly ₹300 with "Save 29%" badge). Route: `Screen.Subscription` → `"subscription"`.
- `PremiumState` sealed class: `Unknown`, `Free`, `Pending`, `Premium(productId, token)`.
- Both registered in `AppModule.kt`, initialized in `NexusPlusApplication.onCreate()`.
- `SettingsScreen.kt`: "Nexus Plus Premium" banner card at top → navigates to SubscriptionScreen.
- **Anti-tamper**: Google Play's BillingClient only reports PURCHASED state after server-side validation. Purchases acknowledged via `acknowledgePurchase()` (required by Google policy within 3 days).

## Web Showcase
- Radio and IPTV feature cards removed from index.html (earlier session)
- Tagline updated to "37+ powerful utilities"
