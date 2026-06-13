---
name: NSE 2.0 architecture
description: Design decisions for the Nexus Speech Engine 2.0 — layer contracts, DI scope, language detector coverage, and stability rules.
---

## Rule
The NSE is split into 6 files in `features/tts/`. Never merge them back into a single file.

**Layers (in dependency order):**
1. `NseEngine.kt` — pure interfaces + sealed types (NseSpeechMode, NseState, NseSpeechRequest, NseVoiceProfile). No Android imports.
2. `NseLanguageDetector.kt` — stateless object, Unicode block scoring + Mix segmentation. 16 scripts.
3. `NseAudioFocusManager.kt` — wraps AudioFocusRequest API, prevents clash with media players. Must request focus before speak, abandon on stop/done.
4. `NseAndroidEngine.kt` — concrete NseEngine wrapping android.speech.tts.TextToSpeech. Only file that imports Android TTS directly.
5. `NseRepository.kt` — owns engine lifecycle, exposes StateFlow<NseState>, serialises speak/stop via SupervisorJob scope.
6. `NseViewModel.kt` — user parameter state (text, pitch, rate, mode), debounced language detection, delegates all effects to repository.

## DI Scope
`NseAudioFocusManager`, `NseAndroidEngine`, `NseRepository` are registered as **factory** (not singleton) in AppModule so each screen instance owns its own engine + audio focus lifecycle. `NseViewModel` is a standard koin `viewModel`.

**Why:** singleton engine would cause audio focus conflicts when the user navigates away and back.

## Language Detector Coverage
Auto/Mix modes cover 16 scripts: Devanagari (hi), Bengali (bn), Gujarati (gu), Gurmukhi (pa), Telugu (te), Tamil (ta), Kannada (kn), Malayalam (ml), Arabic (ar), Hebrew (he), Thai (th), CJK/Chinese (zh), Japanese (ja), Korean (ko), Cyrillic (ru), Greek (el), Latin (en/es/fr/de via diacritic heuristic).

## Stability guarantees built in
- Audio focus acquired before every speak(), abandoned in onDone/onError/stop() — prevents stuttering with media players.
- Engine init is suspendable (suspendCancellableCoroutine) — no race between init callback and first speak().
- Language availability checked before synthesis; falls back to Locale.ENGLISH to prevent LANG_NOT_SUPPORTED crash.
- Mix mode uses QUEUE_ADD for all segments after the first, producing seamless multi-language playback.
- Short segments (<3 chars) merged into neighbours to avoid voice-switching on punctuation.

## Sacred greeting
`strings.xml` has `jai_shri_krishna` and `cd_jai_shri_krishna` entries. Rendered in `JaiShriKrishnaHeader()` composable inside NexusTtsScreen with `semantics { contentDescription = "Jai Shri Krishna" }`.
