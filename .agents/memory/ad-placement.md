---
name: Ad placement — NexusAdScaffold
description: How ads are integrated across all feature screens and the Ad Unit IDs used
---

## Strategy

`NexusAdScaffold` wraps every feature screen in `NexusNavHost.kt`.  
- One wrapper in NavHost = ads on ALL screens without touching individual screen files.
- `NexusAdScaffold` is a `Scaffold` with a sticky 50dp `NexusBannerAd` as `bottomBar`.
- Inner content is padded by `calculateBottomPadding()` so content scrolls above the ad.

## Live Ad Unit IDs (ca-app-pub-9723434393305967)

| Type | Ad Unit ID |
|------|-----------|
| Banner (sticky bottom) | `ca-app-pub-9723434393305967/3163996172` |
| Interstitial | `ca-app-pub-9723434393305967/6401326195` |

Set in `NexusAdIds` object in `NexusAdManager.kt`.

## What's wrapped

51 `NexusAdScaffold` calls in NexusNavHost cover every feature screen (Media, Documents, Security, Utilities, AI, Health, Legal screens).  
HomeScreen, MainScaffold, WelcomeScreen, Hub screens — NOT wrapped (navigation surfaces, not content screens).

## Why

User requested "har jagah par ad dikhai de lekin smart tarike se" — sticky bottom banner is the least intrusive format (always visible, never blocks content, no interruption to workflow).

**How to apply:** Any new feature screen added to NavHost MUST be wrapped: `composable(Screen.X.route) { NexusAdScaffold { XScreen(onBack = ...) } }`
