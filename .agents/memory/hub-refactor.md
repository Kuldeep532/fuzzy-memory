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

## Universal Feature Action System (Phase 2 complete)

`FeatureCard` (Phase 2) supports 5 actions via long-press DropdownMenu — no visible action button:
1. Open
2. Add/Remove Favorites  
3. Pin/Unpin from Home
4. Share (Intent.ACTION_SEND)
5. Info (AlertDialog with full details)

Semantic custom actions expose all 5 to TalkBack/VoiceOver without requiring gestures.
Subtle icon badges (14dp) appear in the card top-right ONLY when state is active (isPinned / isFavorite).
NEW badge (Tertiary color) shown on cards when `feature.isNew = true`.

**Why:** The spec explicitly says "no visible Favorite/Pin button on cards" to avoid UI clutter. Keeping all actions behind long-press (and semantic actions for accessibility) achieves this cleanly.

## FeatureItem model fields

`FeatureItem` now has 3 state booleans, all defaulting to false:
- `isFavorite: Boolean` — bookmarked (FavoritesRepository, DataStore key `favorite_feature_ids`)
- `isPinned: Boolean`   — pinned to Home (FavoritesRepository, DataStore key `pinned_feature_ids`)
- `isNew: Boolean`      — marked new in FeatureCatalog (static — 5 features currently marked)

Features currently marked `isNew = true`: AI Image Generator, Smart Image Editor, Form X, Colour Detector, Biometric Vault.

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
- Home = full dashboard (Pinned · Recently Used · Most Used · Favorites · Suggested · Hub Cards)
- Explore = AllFeaturesScreen (ViewModel-backed, grid + category filter chips)
- Search = SearchScreen (real-time keyword search across all 49 features, uses FeatureCard)
- More = MoreScreen with Settings entry added at top

## Home dashboard sections (all auto-populated, no manual config needed)
Order: New Features → Pinned → Recently Used → Most Used → Favorites → Hub Cards → Suggested
Each section is hidden entirely if it has no items.
Empty state shows a tip card explaining long-press to pin/favorite.

## Visit count tracking (Most Used)

RecentActivityRepository stores two DataStore keys:
- `recent_feature_ids` — comma-separated recency list (max 10)
- `visit_counts` — `featureId:count;featureId:count` encoded string

`mostUsedIds` Flow sorts by count descending, top 10. Most Used row only appears if ≥ 3 visits tracked.

## New singletons in AppModule
- `SearchManager` — stateless search filter, no DataStore
- `RecentActivityRepository` — DataStore `nexus_recent_activity`, max 10 entries, visit counts
- `SettingsRepository` — DataStore `nexus_app_settings` (theme/font/accessibility)

**DataStore names in use (must not collide):**
- nexus_favorites           ← favorites + pinned
- nexus_legal_consent
- nexus_recent_activity     ← recency list + visit counts
- nexus_app_settings

## Call site pattern for hub screens (all 5)

All hub screens use `koinInject()` (not ViewModel) for `favoritesRepository`, `sessionManager`, `recentRepo`.
All collect both `favoriteIds` and `pinnedIds` and pass both `onToggleFavorite` and `onTogglePin` to FeatureCard.
All screens call `recentRepo.recordVisit(feature.id)` before navigating.
All screens call `view.announceForAccessibility(msg)` before toggles.

## AllFeaturesViewModel: combine() pattern

Uses `combine(favoriteIds, pinnedIds)` to enrich features — avoids nested `collect` calls.
Keywords are now also included in the `filterFeatures` search logic.

## Stub feature routes
FeatureCatalog uses `Screen.Stub.route + "/feature_key"` pattern (e.g. `"feature/stub/file_manager"`). NexusNavHost matches `"feature/stub/{feature_key}"` — the last path segment becomes the featureKey shown in StubFeatureScreen.

## Compose Multiplatform (iOS/Desktop)
CMP migration explicitly deferred as a separate major initiative. The `shared/` module already has `jvm("desktop")` target in Gradle; Compose Multiplatform plugin + iOS KMP target is the remaining step. No Compose code is in commonMain currently — all Compose/UI is in `app/` (androidMain).
