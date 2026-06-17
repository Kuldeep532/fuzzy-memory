---
name: NexusSoundManager
description: Android sound effects system for Nexus Plus — SoundPool-based, settings-gated
---

Location: `NexusPlus/composeApp/src/androidMain/kotlin/com/nexuswavetech/nexusplus/sound/NexusSoundManager.kt`

Object singleton. Call `NexusSoundManager.init(context, settingsRepository)` once at app startup (e.g. in Application.onCreate). Call `NexusSoundManager.play(SoundEvent.X)` from anywhere.

**Events**: NAVIGATE, BACK, SELECT, TOGGLE_ON, TOGGLE_OFF, SUCCESS, ERROR, BUTTON_TAP, DIALOG_OPEN, DIALOG_CLOSE, FEATURE_LAUNCH, PAGE_TURN_FORWARD, PAGE_TURN_BACKWARD.

**Gate**: all playback skipped when `SettingsRepository.soundEffectsEnabled` is false.

**Implementation**: generates sine-wave PCM WAV files to cache dir and loads them into SoundPool at init time. No bundled audio assets needed.

**Why:** avoids large binary audio assets in repo; sine waves at different frequencies give distinctive short UI feedback sounds.
