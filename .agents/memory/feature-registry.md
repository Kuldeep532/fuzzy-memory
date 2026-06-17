---
name: FeatureRegistry typed keys
description: FeatureRegistry uses FeatureId enum as key, not String — avoids key mismatch bugs
---

`FeatureRegistry` in `core/registry/FeatureRegistry.kt` is keyed by `FeatureId` enum.

Do NOT use String keys. All lookups and registrations must go through `FeatureId`.

`FeatureCatalog.registerAllFeatures()` calls `FeatureRegistry.registerAll(allFeatures)` — this is the canonical registration point, called once at app startup.

`FeatureRegistry.totalCount` is derived at runtime from the registry — never hardcode feature counts.

**Why:** original implementation used String keys and referenced a non-existent `feature.hub` field, causing runtime crashes. Enum keys make mismatches a compile error.

**How to apply:** any new feature must have a FeatureId enum entry, a Screen object, a FeatureItem in FeatureCatalog.allFeatures, and a composable in NexusNavHost.
