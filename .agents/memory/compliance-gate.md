---
name: Compliance gate pattern
description: Auth is blocked until both Privacy Policy and Terms & Conditions are accepted. DataStore-backed, persists across restarts.
---

## Rule
Neither Google Sign-In nor Guest auth proceeds until `legalConsentGranted == true`.

**Enforcement points:**
- `WelcomeViewModel.onGoogleSignInTokenReceived()` — guard at top
- `WelcomeViewModel.onContinueAsGuestClicked()` — guard at top
- `WelcomeScreen` — both auth buttons have `enabled = consentGranted`
- Both buttons have WCAG-compliant `contentDescription` that changes based on consent state

**Why:** Per the security directive: "Authentication must remain blocked until Privacy Policy accepted AND Terms & Conditions accepted."

## Implementation
- `ConsentRepository` (auth/ConsentRepository.kt) — DataStore Preferences, keys: `privacy_policy_accepted`, `terms_conditions_accepted`.
- Registered as Koin `single` (singleton) so consent state persists for the entire app session.
- `WelcomeViewModel` exposes `privacyAccepted`, `termsAccepted`, `legalConsentGranted` as StateFlow via `combine()`.
- Consent is persisted across app restarts — user does not need to re-accept every launch.
