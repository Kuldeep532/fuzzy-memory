package com.nexuswavetech.nexusplus.features.imageeditor

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── On-device image transformations — pure Android Bitmap/Canvas/Matrix API ──

fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

fun flipBitmap(src: Bitmap, horizontal: Boolean): Bitmap {
    val matrix = Matrix().apply {
        if (horizontal) postScale(-1f, 1f, src.width / 2f, src.height / 2f)
        else            postScale(1f, -1f, src.width / 2f, src.height / 2f)
    }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

fun adjustBrightness(src: Bitmap, brightness: Float): Bitmap {
    val b = ((brightness - 1f) * 255f).toInt()
    val cm = ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, b.toFloat(),
        0f, 1f, 0f, 0f, b.toFloat(),
        0f, 0f, 1f, 0f, b.toFloat(),
        0f, 0f, 0f, 1f, 0f
    ))
    return applyColorMatrix(src, cm)
}

fun adjustContrast(src: Bitmap, contrast: Float): Bitmap {
    val t = ((1f - contrast) / 2f) * 255f
    val cm = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, t,
        0f, contrast, 0f, 0f, t,
        0f, 0f, contrast, 0f, t,
        0f, 0f, 0f, 1f, 0f
    ))
    return applyColorMatrix(src, cm)
}

fun toGrayscale(src: Bitmap): Bitmap {
    val cm = ColorMatrix().apply { setSaturation(0f) }
    return applyColorMatrix(src, cm)
}

fun toSepia(src: Bitmap): Bitmap {
    val cm = ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    return applyColorMatrix(src, cm)
}

fun invertColors(src: Bitmap): Bitmap {
    val cm = ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    ))
    return applyColorMatrix(src, cm)
}

private fun applyColorMatrix(src: Bitmap, cm: ColorMatrix): Bitmap {
    val result = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas  = Canvas(result)
    val paint   = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return result
}

// ── Screen ────────────────────────────────────────────────────────────────────

enum class ImageFilter(val label: String) {
    ORIGINAL("Original"), GRAYSCALE("Grayscale"), SEPIA("Sepia"), INVERT("Invert")
}

@Composable
fun SmartImageEditorScreen(onBack: () -> Unit) {
    val context     = LocalContext.current
    val view        = LocalView.current
    val scope       = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editedBitmap   by remember { mutableStateOf<Bitmap?>(null) }
    var brightness     by remember { mutableStateOf(1.0f) }
    var contrast       by remember { mutableStateOf(1.0f) }
    var rotation       by remember { mutableStateOf(0f) }
    var activeFilter   by remember { mutableStateOf(ImageFilter.ORIGINAL) }
    var scale          by remember { mutableStateOf(1f) }
    var savedSuccess   by remember { mutableStateOf(false) }

    // Re-apply all edits whenever any parameter changes
    LaunchedEffect(originalBitmap, brightness, contrast, rotation, activeFilter) {
        val src = originalBitmap ?: return@LaunchedEffect
        scope.launch(Dispatchers.Default) {
            var result = src
            result = adjustBrightness(result, brightness)
            result = adjustContrast(result, contrast)
            if (rotation != 0f) result = rotateBitmap(result, rotation)
            result = when (activeFilter) {
                ImageFilter.GRAYSCALE -> toGrayscale(result)
                ImageFilter.SEPIA     -> toSepia(result)
                ImageFilter.INVERT    -> invertColors(result)
                ImageFilter.ORIGINAL  -> result
            }
            editedBitmap = result
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val stream = context.contentResolver.openInputStream(uri)
                originalBitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                brightness = 1f; contrast = 1f; rotation = 0f; activeFilter = ImageFilter.ORIGINAL
                view.announceForAccessibility("Image loaded. Use controls below to edit.")
            }
        }
    }

    fun saveImage() {
        val bmp = editedBitmap ?: return
        scope.launch(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "nexus_edit_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NexusPlus")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { s ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, s)
                }
                savedSuccess = true
                kotlinx.coroutines.delay(2000)
                savedSuccess = false
            }
        }
        view.announceForAccessibility("Image saved to gallery")
    }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Smart Image Editor", onBack = onBack)

        if (originalBitmap == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier            = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Filled.PhotoFilter, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Open an image to start editing", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Button(
                        onClick  = { imagePicker.launch("image/*") },
                        modifier = Modifier.semantics { contentDescription = "Open image from gallery or files." }
                    ) {
                        Icon(Icons.Filled.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Image")
                    }
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image preview
                editedBitmap?.let { bmp ->
                    Image(
                        bitmap       = bmp.asImageBitmap(),
                        contentDescription = "Edited image preview. ${activeFilter.label} filter applied. Brightness ${(brightness * 100).roundToInt()}%. Contrast ${(contrast * 100).roundToInt()}%. Rotation $rotation degrees.",
                        modifier     = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ -> scale = (scale * zoom).coerceIn(0.5f, 4f) }
                            }
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        contentScale = ContentScale.Fit
                    )
                }

                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Rotation buttons
                    Text("Rotate & Flip", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.semantics { heading() })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("Rotate Left",  Icons.Filled.RotateLeft)  { rotation -= 90f },
                            Triple("Rotate Right", Icons.Filled.RotateRight) { rotation += 90f },
                            Triple("Flip H",       Icons.Filled.Flip)        {
                                originalBitmap = flipBitmap(originalBitmap!!, true)
                            },
                            Triple("Flip V",       Icons.Filled.Flip)        {
                                originalBitmap = flipBitmap(originalBitmap!!, false)
                            }
                        ).forEach { (label, icon, action) ->
                            FilledTonalButton(
                                onClick          = { action(); view.announceForAccessibility(label) },
                                modifier         = Modifier.weight(1f),
                                contentPadding   = PaddingValues(4.dp)
                            ) {
                                Icon(icon, null, Modifier.size(16.dp))
                            }
                        }
                    }

                    // Brightness
                    Text("Brightness: ${(brightness * 100).roundToInt()}%", modifier = Modifier.semantics { contentDescription = "Brightness ${(brightness * 100).roundToInt()} percent" })
                    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.2f..2.0f,
                        modifier = Modifier.semantics { contentDescription = "Brightness slider." })

                    // Contrast
                    Text("Contrast: ${(contrast * 100).roundToInt()}%", modifier = Modifier.semantics { contentDescription = "Contrast ${(contrast * 100).roundToInt()} percent" })
                    Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.2f..2.0f,
                        modifier = Modifier.semantics { contentDescription = "Contrast slider." })

                    // Filters
                    Text("Filter", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.semantics { heading() })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ImageFilter.values().forEach { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick  = { activeFilter = filter; view.announceForAccessibility("${filter.label} filter applied") },
                                label    = { Text(filter.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Action row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick  = { imagePicker.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Filled.FolderOpen, null); Spacer(Modifier.width(4.dp)); Text("Open") }
                        Button(
                            onClick  = ::saveImage,
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Filled.SaveAlt, null); Spacer(Modifier.width(4.dp)); Text("Save") }
                    }

                    if (savedSuccess) {
                        Text(
                            "✓ Saved to Pictures/NexusPlus",
                            color    = MaterialTheme.colorScheme.secondary,
                            style    = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { contentDescription = "Image saved successfully to gallery." }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
