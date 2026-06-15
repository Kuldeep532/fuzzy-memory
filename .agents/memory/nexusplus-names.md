---
name: Nexus Plus canonical feature names and key constraints
description: Canonical names, icon choices, and build constraints for Nexus Plus features
---

## Canonical Feature Names
- NSE → always "Nexus Speech Engine" (never rename)
- Music Player → "Nexus Media Player" (renamed in v1.2)
- New features: Aira AI, Nexus Image Viewer, Nexus Document Reader

## Icon assignments (FeatureCatalog)
- Nexus Media Player → `Icons.Filled.PlayCircleFilled`
- Nexus Document Reader → `Icons.Filled.MenuBook`
- Aira AI → `Icons.Filled.AutoAwesome`
- Nexus Image Viewer → `Icons.Filled.Photo`

## Build constraints
- ML Kit Object Detection was removed from build.gradle.kts — caused build failures. Do NOT re-add.
- All dependencies present: okhttp (202), coil.compose (207), accompanist.permissions (215)

## About screen
- Shows only: App Name, Version (1.2.0), Developer (Kuldeep Kumar Yadav)
- No social links in About screen

## Help section
- MoreScreen "Help" section shows only email: nexuswavetech@yahoo.com

## FileManagerScreen
- Params: `onOpenImageViewer: ((Uri) -> Unit)? = null`, `onOpenDocReader: ((Uri) -> Unit)? = null`
- IMAGE_EXTS / DOC_EXTS sets defined at file-top level
- Falls back to system Intent for audio/video/unknown types

## Aira AI
- Uses `POST https://text.pollinations.ai/` with `{messages:[...], model:"openai"}` — no API key
- AiraViewModel registered as `viewModel { }` in AppModule

## ViewModels registered as viewModel {} (not singleton)
- RadioViewModel, AiraViewModel, AiImageViewModel, IptvViewModel, MusicViewModel
