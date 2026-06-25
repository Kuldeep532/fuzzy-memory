---
name: Nexus Plus Play Store overhaul
description: Summary of all Play Store readiness changes across two sessions. Reference .local/nexusplus_remaining_work.md for outstanding items.
---

## Session 1 — what was done

### Copyright removals
- **JioSaavn** completely removed from `MusicStreamingScreen.kt` — local-only player now.
- **PrivacyPolicy** Section 2 updated — no Radio Browser/JioSaavn/IPTV references.
- **Nexus OTT** removed from `FeatureCatalog.kt`, `NexusNavHost.kt`, `FeatureId.kt`.

### New features (replacing Radio / IPTV / FormX)
- `TextToPdfScreen` — commonMain. Callback: `onExportPdf(title, body, fontSizePt)`.
- `DailyJournalScreen` — commonMain. Persists via `SettingsStore` (DataStore on Android).
- `ColorPaletteScreen` — commonMain. `onCopyToClip` callback via ClipboardManager.
- All wired in `FeatureId.kt`, `Screen.kt`, `FeatureCatalog.kt`, `NexusNavHost.kt`.

### TTS screen
- Order: Language → TTS Engine → Voice. Modes: Auto / Single / Mix / Advanced.

### Core screen redesigns
- **MoreScreen** — Material3 hero header, StatPill, colored icon cards. `onRateApp` param.
- **AllFeaturesScreen** — banner, animated count, icon chips, improved empty state.
- **HomeScreen** — emoji-free `contentDescription` via `getAccessibleGreeting()`.

### Manifest & build
- `AndroidManifest.xml` — Play Protect justification inline comments per permission group.
- `AndroidManifest.xml` — **FileProvider** added for PDF sharing (Session 2).
- `app/src/main/res/xml/file_provider_paths.xml` — created (Session 2).
- `build.gradle.kts` `prepareGoogleServicesJson` already correctly wired; verified.

### Settings
- Gemini API key section removed. Radio/IPTV buffer section removed.

### FileManager
- System back button fixed — `navController.popBackStack()` instead of navigate home.

## Session 2 — additional fixes

### Daily Journal — DataStore persistence
- `DailyJournalScreen.kt` rewritten — injects `SettingsStore` via `koinInject()`.
- Entries encoded as `id\u0000dateLabel\u0000mood\u0000content` per entry, `\u0001` between entries.
- Key: `"journal_entries_v1"` in DataStore. Survives app restarts.
- Uses `store.stringFlow(JOURNAL_KEY, "")` observed as `collectAsState`, `scope.launch { store.setString(...) }` on save/delete.

### TextToPdf — real PDF generation
- `TextToPdfScreen.kt`: callback renamed `onShareText` → `onExportPdf(title, body, fontSizePt)`.
- `NexusNavHost.kt`: `buildPdfFile(context, title, body, fontSizePt)` private helper generates A4 PDF using `android.graphics.pdf.PdfDocument`. Uses `Paint.breakText()` for word-wrap. Title rendered in bold +8pt. Multi-page support.
- PDF shared via `FileProvider` (authority: `${packageName}.fileprovider`) as `application/pdf`.
- Icon on Export button changed to `Icons.Filled.PictureAsPdf`.

### MoreScreen — Rate the App
- `MoreScreen` now accepts `onRateApp: () -> Unit = {}` parameter.
- `MainScaffold.kt` passes Play Store intent (market:// with https:// fallback).
- iOS caller unchanged (uses default `{}`).

## Key architecture notes
- NavHost pattern: `composable(Screen.X.route) { NexusAdScaffold { XScreen(onBack = { navController.popBackStack() }) } }`
- New screens are in `commonMain`; Android-specific callbacks injected at NavHost layer in `androidMain`.
- `FeatureCategory.TOOLS` is valid — confirmed used by Speed Test and new features.
- `ContactBackup` exists in `FeatureId.kt` / `Screen.kt` but has no screen, catalog entry, or route — dead code, harmless.
- Journal serialization: `\u0000` as field sep, `\u0001` as entry sep, `split(limit=4)` on decode so content may contain `\u0000`.

## Outstanding items
See `.local/nexusplus_remaining_work.md`. Only non-code blockers remain:
1. Play Console Data Safety form — manual step in Play Console UI.
2. ContactBackup screen — not yet implemented (no catalog entry, so users can't reach it).
3. ProGuard rules audit for media3/coil.
