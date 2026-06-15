---
name: FileManager viewer routing
description: FileManagerScreen routes tapped files to Nexus Image Viewer or Nexus Document Reader via lambda callbacks
---

## Rule
`FileManagerScreen` has two optional lambda params for routing:
- `onOpenImageViewer: ((Uri) -> Unit)? = null` — called for IMAGE_EXTS (jpg/png/gif/webp/bmp/heic)
- `onOpenDocReader: ((Uri) -> Unit)? = null` — called for DOC_EXTS (pdf/txt/doc/docx/xls/xlsx/ppt/pptx/rtf/md/log)
- Falls back to `openFileWithSystem()` (Intent.ACTION_VIEW with FileProvider) for audio/video and unknowns

## Why
Tapping a file in the manager should open it in the appropriate Nexus viewer rather than always launching an external app.

## How to apply
In NexusNavHost, pass navigate lambdas:
```kotlin
FileManagerScreen(
    onBack            = { navController.popBackStack() },
    onOpenImageViewer = { uri -> navController.navigate(Screen.NexusImageViewer.route) },
    onOpenDocReader   = { uri -> navController.navigate(Screen.NexusDocReader.route) }
)
```
The URI is obtained via FileProvider inside FileManagerScreen before calling the lambda.
