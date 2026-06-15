---
name: NseRepository API
description: NseRepository speak/stop/initialise are synchronous (not suspend); state is a StateFlow
---

## Rule
`NseRepository.speak(request)`, `stop()`, and `initialise()` are all regular `fun` (not `suspend`). They manage their own coroutine scopes internally.

## Why
NseRepository uses a SupervisorJob-scoped CoroutineScope internally. Callers should never wrap these in `scope.launch`.

## How to apply
- To read document aloud: call `nseRepo.speak(NseSpeechRequest(...))` directly — no coroutine needed
- To check if speaking: observe `nseRepo.state.collectAsState()` — look for `is NseState.Speaking`
- To stop: call `nseRepo.stop()` directly
- To initialise: call `nseRepo.initialise()` in `LaunchedEffect(Unit)` — it returns Unit immediately and fires async internally
- NseRepository is `factory`-scoped in Koin — each injection creates a new instance
- Import: `com.nexuswavetech.nexusplus.features.tts.{NseRepository, NseSpeechRequest, NseSpeechMode, NseState}`
