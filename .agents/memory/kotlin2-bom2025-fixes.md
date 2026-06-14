---
name: Kotlin 2.0 + BOM 2025.02.00 compile fixes
description: The 8 actual compile errors found from CI build logs (not guessed from code review), their root causes, and fixes applied.
---

## Rule
Always read actual build logs (CI zip) before assuming what the compile errors are. Code review found the wrong files; the CI log found the real 8.

**Why:** Previous analysis guessed at errors (NotificationCenterScreen deprecated API, EncrypterDecrypterScreen lambda shadowing, AiImageGeneratorScreen.kt) — only some were right, and 5 real errors were completely missed.

## The 8 real errors (Kotlin 2.0.21 + AGP 8.7.3 + BOM 2025.02.00)

### 1. SmartImageEditorScreen.kt
`import android.graphics.*` + `import androidx.compose.ui.graphics.*` wildcard clash.
Both provide `ColorMatrix` and `Matrix` — ambiguity errors.
**Fix:** Replace `import android.graphics.*` with explicit imports (Bitmap, Canvas, ColorMatrix, ColorMatrixColorFilter, Matrix, Paint). Explicit wins over wildcard.

### 2. ObjectDetectorScreen.kt
Local `data class DetectedObject` naming collision with ML Kit's `com.google.mlkit.vision.objects.DetectedObject`.
Kotlin 2.0 type inference resolves `obj` (the ML Kit object) to the wrong type, making `obj.labels` and `obj.boundingBox.left/top/right/bottom` unresolved.
**Fix:** Add `import com.google.mlkit.vision.objects.DetectedObject as MlDetectedObject` and add explicit type annotation: `.addOnSuccessListener { objects: List<MlDetectedObject> ->`.

### 3. NseViewModel.kt
`StateFlow.distinctUntilChanged()` is deprecated — in Kotlin 2.0, treated as an error (not warning).
**Fix:** Remove the `.distinctUntilChanged()` calls; StateFlow already emits distinct values. Also remove the unused import.

### 4. VoiceTyperScreen.kt
`ExposedDropdownMenuBox`, `ExposedDropdownMenuDefaults.TrailingIcon`, `menuAnchor()`, `ExposedDropdownMenu` are `@ExperimentalMaterial3Api`.
**Fix:** Add `ExperimentalMaterial3Api::class` to the existing `@OptIn` annotation on `VoiceTyperScreen`.

### 5. NseVectorIcon.kt
`cubicTo()` does not exist in `PathBuilder` (the vector path DSL). Correct name is `curveTo()`.
**Fix:** `cubicTo(...)` → `curveTo(...)`.

### 6. AiImageGeneratorScreen.kt
`FlowRow` is `@ExperimentalLayoutApi` — used without OptIn in the composable.
**Fix:** Add `@OptIn(ExperimentalLayoutApi::class)` to `AiImageGeneratorScreen`.

### 7. HealthVaultRepository.kt
`private fun firestoreSet(...) = runCatching { ... return }` — bare `return` inside an inline lambda returns `Unit` from the outer function, but the function's inferred return type is `Result<Task<Void>>`. Type mismatch.
**Fix:** Change to regular function body: `private fun firestoreSet(...) { val uid = ... ?: return; runCatching { ... } }`. Same for `firestoreDelete`.

### 8. NotificationCenterScreen.kt
`rememberDismissState`, `SwipeToDismiss`, `DismissValue`, `DismissDirection` are REMOVED (not just deprecated) in Material3 BOM 2025.02.00.
**Fix:** Replace with `rememberSwipeToDismissBoxState`, `SwipeToDismissBox`, `SwipeToDismissBoxValue.EndToStart`, `enableDismissFromEndToStart = true`. Function already had `@OptIn(ExperimentalMaterial3Api::class)`.

## How to apply
When a build fails on this project: extract the CI zip log, grep for `e: file://` lines, fix only those files. Do not guess from code review.
