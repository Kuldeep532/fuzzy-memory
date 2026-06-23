---
name: Nexus Plus rebuild decisions
description: Durable constraints and decisions from the Nexus Plus Android app rebuild — login gate, deleted features, TTS redesign, social links.
---

## Login Gate — AppConfig.LOGIN_REQUIRED
- File: `NexusPlus/composeApp/src/androidMain/kotlin/com/nexuswavetech/nexusplus/core/AppConfig.kt`
- `LOGIN_REQUIRED = false` → app goes straight to `Screen.Main`, skipping WelcomeScreen entirely.
- `NexusNavHost.kt` uses: `startDestination = if (AppConfig.LOGIN_REQUIRED) Screen.Welcome.route else Screen.Main.route`
- **Why:** Login is temporarily disabled while Google Sign-In is being re-integrated. Single flag re-enables all feature gates.

## Deleted Features (files + catalog entries removed)
- Online Radio (`features/radio/`) — deleted
- IPTV/Live TV (`features/iptv/`) — deleted
- FormX (`features/formx/`) — deleted
- `Screen.RadioPlayer`, `Screen.IptvPlayer`, `Screen.FormX` still exist in Screen.kt (kept to avoid cascading compile errors in iOS nav host)
- `FeatureCatalog.kt` entries for all three removed
- `AppModule.kt` — RadioViewModel and IptvViewModel imports and viewModel{} registrations removed

## Social Media Screen
- Removed: Twitter/X, YouTube, GitHub, LinkedIn
- Kept: Official Website, Instagram, Facebook, WhatsApp, Telegram, Discord, Support Email
- All controlled by Firebase Remote Config URLs; set URL to "" to hide any link.

## NSE TTS Screen — complete redesign
- File: `NexusPlus/composeApp/src/androidMain/.../features/tts/NexusTtsScreen.kt`
- Section 1: TTS Engine (shows active engine, buttons to change/download)
- Section 2: Language picker (always visible, not mode-gated)
- Section 3: Voice picker (always visible)
- Section 4: Quick speak test
- Section 5: Speed/Pitch sliders
- Section 6: Screen Reader Mode (advanced)
- Section 7: Accessibility toggles
- TTS engine listing via PackageManager + `android.intent.action.TTS_SERVICE` intent
- Download voices → opens `com.android.settings.TTS_SETTINGS` intent, falls back to Play Store link for Google TTS

## Theme Colors (Color.kt)
- Background deepened: `#07070F` (was `#0A0A16`)
- Primary: `#818CF8` soft indigo (was `#7B6FFF`)
- Secondary: `#34D399` emerald green (was `#00D9B5`)
- Added: `NexusTertiaryContainer`, `NexusOnTertiaryContainer`, `AccentMedia`, `AccentSecurity`, `AccentProductivity`, `AccentTools`, `AccentUtilities`
- NexusPlusTheme.kt updated with tertiaryContainer/onTertiaryContainer for both dark and light schemes.

## Web Showcase
- Radio and IPTV feature cards removed from index.html
- Tagline updated to "37+ powerful utilities"
- Tech stack section: "OTT streaming and music playback" (was "Radio, IPTV, and music playback")
