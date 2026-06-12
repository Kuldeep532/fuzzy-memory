# Nexus Plus — Android Source Code

**Developer:** Nexus Wave Technologies  
**Version:** 1.0.0  
**Min SDK:** 26 (Android 8.0) | **Target SDK:** 35 (Android 15)

---

## Opening in Android Studio

1. Unzip this folder anywhere on your machine.
2. Open **Android Studio Hedgehog (2023.1.1)** or newer.
3. Choose **File → Open** and select the `NexusPlus/` folder.
4. Wait for Gradle sync to complete (first sync downloads ~300 MB of dependencies).
5. Connect an Android device or start an emulator (API 26+).
6. Press ▶ Run.

---

## Project Structure

```
app/src/main/java/com/nexuswavetech/nexusplus/
├── NexusPlusApplication.kt     — Koin DI initialisation
├── MainActivity.kt             — Single-activity entry point
├── auth/                       — Welcome screen, Google Sign-In stubs, ViewModel
├── core/                       — SessionManager, NexusGatekeeper, FeatureCatalog,
│                                 FavoritesRepository, FeatureItem, FeatureId
├── navigation/                 — NavHost, MainScaffold, Screen/BottomTab routes
├── features/
│   ├── allfeatures/            — All Features grid with search, filter, gatekeeper
│   ├── favorites/              — Favorites tab with empty state
│   ├── more/                   — More tab: settings, legal links, social media
│   ├── radio/                  — Online Radio (Radio Browser API + ExoPlayer)
│   ├── pdf/                    — Native PDF Reader (PdfRenderer + Compose Canvas)
│   ├── imagegen/               — AI Image Generator (Pollinations AI)
│   ├── tts/                    — Nexus Auto Speech Engine (Android TTS + auto-locale)
│   ├── iptv/                   — IPTV / Live TV (M3U parser + ExoPlayer)
│   ├── music/                  — Music Streaming (JioSaavn API + MediaSession)
│   └── stub/                   — Placeholder for features 7–30 (ready to implement)
├── legal/                      — AboutUs, PrivacyPolicy, TermsConditions screens
├── ui/
│   ├── theme/                  — Color, Type, Theme (Material Design 3, dark+light)
│   └── components/             — FeatureCard, GatekeeperDialog, SocialMediaLinks,
│                                 NexusTopBar
└── di/
    └── AppModule.kt            — All Koin singleton + ViewModel bindings
```

---

## Activating Firebase (when ready)

No UI code changes are needed. Just:

1. Add your `google-services.json` to `app/`.
2. In `gradle/libs.versions.toml` — uncomment the Firebase version lines.
3. In `app/build.gradle.kts` — uncomment the Firebase BOM + auth dependency lines and the `google-services` plugin.
4. In `build.gradle.kts` (root) — uncomment the `google-services` classpath.
5. In `di/AppModule.kt` — swap `StubFirebaseAuthRepository` for your real `FirebaseAuthRepositoryImpl`.
6. In `auth/WelcomeScreen.kt` — wire the `googleSignInLauncher` to a real `GoogleSignInClient` and pass the returned `idToken` to `viewModel.onGoogleSignInTokenReceived(idToken)`.

---

## Adding Feature 7–30

Each extended feature follows the same pattern:

1. Create `features/<name>/<Name>Screen.kt` + `<Name>ViewModel.kt`
2. Add a `Screen` object in `navigation/Screen.kt`
3. Add a `composable(Screen.YourFeature.route)` in `navigation/NexusNavHost.kt`
4. Update the route in `core/FeatureCatalog.kt` (replace the stub route)
5. Add `viewModel { YourFeatureViewModel() }` in `di/AppModule.kt`
6. That's it — navigation, gatekeeper, and favorites all work automatically.

---

## Accessibility (TalkBack)

Every interactive element has:
- `Modifier.semantics { contentDescription = "..." }` — screen reader label
- `CustomAccessibilityAction` on FeatureCard for "Add/Remove Favorite"
- `view.announceForAccessibility(...)` for dynamic state changes (playing/paused, page turns)
- `heading()` semantic on all section titles
- `Role.Button` on all custom button surfaces

---

## APIs Used (all public, no key required)

| Feature | API |
|---|---|
| Online Radio | `at1.api.radio-browser.info` |
| AI Image Generator | `image.pollinations.ai` |
| IPTV | `raw.githubusercontent.com/iptv-org/iptv` |
| Music Streaming | `saavn.dev` (JioSaavn wrapper) |

---

© 2025 Nexus Wave Technologies. All rights reserved.
