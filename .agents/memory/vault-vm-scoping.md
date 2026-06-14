---
name: Vault ViewModel scoping fix
description: BiometricVaultViewModel must be activity-scoped (not nav-backstack-scoped) or it re-locks on every navigation. Also locks vault on Lifecycle.Event.ON_STOP.
---

# Vault ViewModel scoping

**Rule:** `BiometricVaultViewModel` must be obtained activity-scoped in `BiometricVaultScreen`:
```kotlin
val activity = context as ComponentActivity
val vm: BiometricVaultViewModel = koinViewModel(viewModelStoreOwner = activity)
```

**Why:** Koin's default `koinViewModel()` scopes to the `NavBackStackEntry`. Every time the user navigates away from and back to `BiometricVaultScreen`, a fresh ViewModel is created → immediate re-lock, forcing re-authentication every navigation hop. Activity scope keeps the single instance alive for the whole session.

**How to apply:** Any ViewModel that maintains sensitive session state (unlocked/locked, auth token) that should survive intra-activity navigation must be retrieved with `viewModelStoreOwner = activity`.

**Background lock:** `BiometricVaultScreen` registers a `LifecycleEventObserver` on `LocalLifecycleOwner` and calls `vm.lock()` on `Lifecycle.Event.ON_STOP`. ON_STOP (not ON_PAUSE) is correct — ON_PAUSE fires for dialogs and overlays too, which would cause spurious re-locks.

**Configurable timeout:** `BiometricVaultViewModel` now accepts `SettingsRepository` and reads `vaultAutoLockMinutes` via a Flow collector. `sessionSecsLeft = -1` means "never lock" (auto-lock disabled). The countdown is hidden in the UI when `sessionSecsLeft < 0`.
