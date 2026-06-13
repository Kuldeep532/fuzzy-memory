---
name: Hub refactor architecture
description: How the 5-hub feature consolidation was implemented and what decisions were made.
---

## Decision: Navigation-level hub consolidation

Hub screens are **overview grids** (LazyVerticalGrid of FeatureCards) that navigate to existing individual feature screens — they do NOT embed screen content in tabs/pagers. This was chosen because:
- Existing screens each own their ViewModel scope; embedding them in pagers would break lifecycle
- Avoids refactoring all 42+ screens to accept a `showTopBar: Boolean` parameter
- Still satisfies "consolidation" by grouping related features in one navigation destination

**Why:** Any future approach that tries to embed existing screens inside a hub tab pager will hit ViewModel scoping issues — each screen calls `koinViewModel()` and expects to own its scope.

## Hub → Category mapping
`FeatureCategory.toHub()` in `FeatureItem.kt`:
- SECURITY → FeatureHub.SECURITY
- PRODUCTIVITY → FeatureHub.DOCUMENTS
- MEDIA → FeatureHub.MEDIA
- UTILITIES → FeatureHub.UTILITIES
- TOOLS → FeatureHub.UTILITIES

AI Hub is **cross-category** — it hand-picks features by `FeatureId` set (AI_IMAGE_GENERATOR, NEXUS_TTS, TEXT_TRANSLATOR, VOICE_TYPER, OBJECT_DETECTOR, COLOR_DETECTOR). Do not rely on `forHub(FeatureHub.AI)` for the AI hub screen; use the explicit ID set.

## Navigation: 4-tab bottom bar
BottomTab: Home | Explore | Search | More
- Home = full dashboard (hub cards, recent activity, favorites, search shortcut)
- Explore = existing AllFeaturesScreen unchanged
- Search = new SearchScreen (real-time keyword search across all 49 features)
- More = MoreScreen with Settings entry added at top

## New singletons in AppModule
- `SearchManager` — stateless search filter, no DataStore
- `RecentActivityRepository` — DataStore `nexus_recent_activity`, max 10 entries
- `SettingsRepository` — DataStore `nexus_app_settings` (theme/font/accessibility)

**DataStore names in use (must not collide):**
- nexus_favorites
- nexus_legal_consent
- nexus_recent_activity  ← new
- nexus_app_settings     ← new

## Stub feature routes
FeatureCatalog uses `Screen.Stub.route + "/feature_key"` pattern (e.g. `"feature/stub/file_manager"`). NexusNavHost matches `"feature/stub/{feature_key}"` — the last path segment becomes the featureKey shown in StubFeatureScreen.
