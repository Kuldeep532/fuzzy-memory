---
name: Nexus Plus Play Store overhaul
description: Summary of all Play Store readiness changes — what was done and what remains. Reference .local/nexusplus_remaining_work.md for outstanding items.
---

## What was done

### Copyright removals
- **JioSaavn** completely removed from `MusicStreamingScreen.kt` — `MusicMode.ONLINE` enum value, `OkHttpClient`, `searchMusic()` ViewModel method, `OnlineMusicContent` composable, all `okhttp3.*` / `JSONObject` imports all gone. Screen is now local-only player.
- **PrivacyPolicy** Section 2 updated to remove Radio Browser API, JioSaavn API, IPTV-org references; now accurately describes local-only music and specific APIs used.
- **Nexus OTT** removed from `FeatureCatalog.kt`, `NexusNavHost.kt` (composable + imports), and `FeatureId.kt`.

### New features (replacing Radio / IPTV / FormX)
- `TextToPdfScreen` — commonMain, converts typed text to shareable PDF via Android Intent.
- `DailyJournalScreen` — commonMain, mood-tagged daily entries (in-memory; needs DataStore persistence — see remaining work).
- `ColorPaletteScreen` — commonMain, colour palette generator with clipboard copy.
- All three wired in `FeatureId.kt`, `Screen.kt`, `FeatureCatalog.kt`, `NexusNavHost.kt`.
- NavHost passes Android-specific callbacks: `onShareText` (Intent.ACTION_SEND) for TextToPdf; `onCopyToClip` (ClipboardManager) for ColorPalette.

### TTS screen
- Section order: Language → TTS Engine → Voice (was Language → Voice → TTS Engine).
- Modes renamed: Auto / Single / Mix / Advanced (was Auto / Single / Dual / Advanced).
- "Secondary Voice (Dual Mode)" label → "Mix Mode Voice".

### Core screen redesigns
- **MoreScreen** — Material3 hero header, `StatPill` row, colored icon `MoreMenuCard` grid.
- **AllFeaturesScreen** — banner header with animated feature count, icon-labeled category chips, improved empty state with Clear Search button.
- **HomeScreen** — `getAccessibleGreeting()` strips emoji from `contentDescription`; visual improvements in place.

### Manifest & build
- `AndroidManifest.xml` — full Play Protect justification inline comments for every `uses-permission` group.
- `build.gradle.kts` `prepareGoogleServicesJson` task already correctly wired to `processGoogleServices` (lines 163-167); no change needed.

### Settings
- Gemini API key section removed from `SettingsScreen.kt`.
- Radio/IPTV buffer settings section removed from `SettingsScreen.kt`.

### FileManager
- System back button now calls `navController.popBackStack()` instead of navigating home.

### Accessibility
- No emoji characters in any `contentDescription` / `semantics` block across all modified screens.

## Key architecture notes
- NavHost pattern: `composable(Screen.X.route) { NexusAdScaffold { XScreen(onBack = { navController.popBackStack() }) } }`
- All new screens are in `commonMain` (pure Compose, no Android APIs). Android-specific callbacks are injected at the NavHost layer in `androidMain`.
- `FeatureCategory.TOOLS` is a valid enum value (confirmed used by Speed Test and new features).
- `ContactBackup` exists in `FeatureId.kt` and `Screen.kt` but has no screen file, catalog entry, or NavHost route — dead code, harmless.

## Outstanding items
See `.local/nexusplus_remaining_work.md` for full list. Key blockers for Play Store:
1. `DailyJournalScreen` entries need DataStore/Room persistence (lost on restart).
2. `TextToPdfScreen` needs real `PdfDocument` generation (currently shares plain text).
3. Play Console Data Safety form must be filled out manually.
4. ProGuard rules need audit for new dependencies.
