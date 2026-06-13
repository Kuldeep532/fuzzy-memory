---
name: Duplicate hub cleanup
description: Which evolution screens were removed as duplicates, final feature count, and HubHeader API change.
---

## What was removed (v1.2.0 cleanup)

Three "evolution system" screens were deleted because they were hub-within-a-hub duplicates — they only listed links to features already directly accessible:

| Deleted screen | Why it was a duplicate |
|---|---|
| `NexusIntelligenceScreen` | Duplicated AIHubScreen (same AI features) |
| `NexusAutomationScreen` | Duplicated UtilitiesHub entries |
| `NexusDevKitScreen` | Duplicated UtilitiesHub + SecurityHub tools |
| `TextEncryptorScreen.kt` | Dead file; NavHost routed TextEncryptor → EncrypterDecrypterScreen |

Deleted FeatureId entries: `NEXUS_INTELLIGENCE`, `NEXUS_AUTOMATION`, `NEXUS_DEV_KIT`  
Removed from: `FeatureId.kt`, `FeatureCatalog.kt`, `NexusGatekeeper.kt`, `NexusNavHost.kt`, `Screen.kt`, `MoreScreen.kt`, `AIHubScreen.kt`

**Why:** These screens re-listed the same features already reachable from hub screens. Users saw the same tool in multiple places.

## What was kept

`NexusHealthVaultScreen` — genuinely new feature (health records tracker). Now lives at `Screen.NexusHealthVault` (`feature/health_vault`), category SECURITY.

## Final feature count

**50 features** in `FeatureCatalog.allFeatures` (47 unique FeatureItem entries; legacy alias IDs do not generate catalog rows).

Category breakdown: Media 5, Productivity 7, Utilities 12, Smart Tools 13, Security 13.

## HubHeader API change

`HubHeader` in `HubSharedComponents.kt` now requires a `title: String` first parameter (added for the polished gradient header redesign). All 5 hub screens updated. Signature:

```kotlin
HubHeader(title, icon, description, color, count)
```

## SecurityHubScreen

Now has a search bar (was the only hub screen missing it). All 5 hub screens now have consistent search functionality.
