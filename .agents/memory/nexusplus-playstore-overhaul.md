---
name: Nexus Plus Play Store overhaul
description: Full audit of Play Store readiness changes: copyright removals, accessibility, manifest, billing migration to UPI, TTS fixes.
---

## Session 1 — Play Store readiness (earlier)

### Copyright removals
- **JioSaavn** completely removed from `MusicStreamingScreen.kt` — local-only player now.
- **PrivacyPolicy** Section 2 updated — no Radio Browser/JioSaavn/IPTV references.
- Removed copyright-risky features from FeatureCatalog and screens.

## Session 2 — Play Billing → UPI Payment Migration

### What was removed
- `BillingManager.kt`, old `PremiumRepository.kt`, old `SubscriptionScreen.kt` — deleted
- `playBilling = "7.1.1"` and `play-billing` library from `libs.versions.toml` — removed
- `implementation(libs.play.billing)` from `app/build.gradle.kts` — removed
- Billing imports from `AppModule.kt` cleaned; `PremiumRepository.init()` from `NexusPlusApplication.kt` removed

### New UPI Payment System (billing/ directory)
- `PremiumRepository.kt` — DataStore-cached premium state, Firestore `premium_users/{uid}` check, Remote Config Base64 UPI ID, submits to Firestore `payment_requests/{uid}`
- `PaymentViewModel.kt` — exposes `isPremiumFlow`, `upiId`, `monthlyAmount`, `yearlyAmount`, `submitPayment()`
- `PaymentScreen.kt` — plan cards (₹35/month, ₹300/year), UPI deep-link button, transaction ID entry, submit for manual review
- `Screen.Subscription.route` unchanged — NavHost now shows `PaymentScreen`

### Remote Config Keys Added
- `payment_upi_id` — Base64-encoded UPI ID (set in Firebase Console — MUST be set by admin)
- `payment_upi_name`, `payment_monthly_amount`, `payment_yearly_amount`

### Firebase Console (admin setup required)
- Remote Config: set `payment_upi_id` = Base64 of actual UPI ID
- Firestore: admin sets `premium_users/{uid}.active = true` after verifying `payment_requests/{uid}`

## Premium = No Ads
- `NexusAdScaffold` — `koinInject<PremiumRepository>()`, collects `isPremiumFlow`; if premium → full-screen Box without any ad
- **Why:** Every feature screen wrapped in NexusAdScaffold; single injection removes ads everywhere for premium users.

## TTS Screen Overhaul
- **Root issue**: Banner had hidden `TextButton("TTS Settings")` launching `Settings.ACTION_ACCESSIBILITY_SETTINGS`
- **Fix**: Replaced with `ExposedDropdownMenuBox` showing Auto/Mix/Dual/Single modes
- Mode description shown inline; Secondary voice dropdown appears only in Dual mode (`AnimatedVisibility`)
- Error card now says "install TTS from Play Store" instead of "open Settings"

## Accessibility Volume Fix
- `NseAndroidEngine.synthesise()`: added `KEY_PARAM_STREAM = STREAM_ACCESSIBILITY` (API 26+) to Bundle; `setAudioAttributes(USAGE_ASSISTANCE_ACCESSIBILITY, CONTENT_TYPE_SPEECH)`
- `NseAudioFocusManager`: pre-O deprecated path changed `STREAM_VOICE_CALL` → `STREAM_MUSIC` (STREAM_ACCESSIBILITY is API 26+ only); API 26+ path already uses `USAGE_ASSISTANCE_ACCESSIBILITY` ✓
- **Why:** TTS volume must track the Accessibility volume slider (same channel TalkBack uses).

## Feedback / Rate App (MainScaffold)
- Already uses `market://details?id=...` with Play Store web URL fallback — no change needed.
