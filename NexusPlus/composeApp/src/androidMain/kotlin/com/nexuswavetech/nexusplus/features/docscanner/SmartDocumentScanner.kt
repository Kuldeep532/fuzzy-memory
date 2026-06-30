package com.nexuswavetech.nexusplus.features.docscanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── ViewModel state ───────────────────────────────────────────────────────────

data class ScanResult(
    val imageUri: Uri? = null,
    val ocrText: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null,
)

// ── Screen ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SmartDocumentScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var state by remember { mutableStateOf(ScanResult()) }
    var showExportDialog by remember { mutableStateOf(false) }

    val camPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bmp ->
        if (bmp != null) {
            val uri = saveBitmap(context, bmp)
            state = ScanResult(imageUri = uri, ocrText = "", isProcessing = true)
            scope.launch { runOcr(context, uri) { s -> state = s } }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            state = ScanResult(imageUri = uri, ocrText = "", isProcessing = true)
            scope.launch { runOcr(context, uri) { s -> state = s } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title  = "Document Scanner",
            onBack = onBack,
            actions = {
                if (state.ocrText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Scanned text", state.ocrText))
                        },
                        modifier = Modifier.semantics { contentDescription = "Copy text to clipboard" },
                    ) {
                        Icon(Icons.Filled.ContentCopy, null)
                    }
                }
            },
        )

        if (!camPermission.status.isGranted) {
            PermissionGate(onGrant = { camPermission.launchPermissionRequest() })
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Capture buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = { cameraLauncher.launch(null) },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Camera", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick  = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery", fontWeight = FontWeight.SemiBold)
                }
            }

            // Preview
            AnimatedVisibility(visible = state.imageUri != null) {
                state.imageUri?.let { uri ->
                    var bmp by remember(uri) { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(uri) {
                        bmp = loadBitmap(context, uri)
                    }
                    bmp?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Scanned document",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 300.dp)
                                .clip(MaterialTheme.shapes.large),
                        )
                    }
                }
            }

            // Processing
            if (state.isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Reading text from image…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            state.error?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = MaterialTheme.shapes.large) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // OCR Result
            AnimatedVisibility(visible = state.ocrText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Extracted Text", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(state.ocrText, style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("Scanned text", state.ocrText))
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy Text")
                            }
                            Button(
                                onClick = { showExportDialog = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Export PDF")
                            }
                        }
                    }
                }
            }

            if (state.imageUri == null && !state.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.DocumentScanner, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        Text("Smart Document Scanner", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Snap a photo or pick an image. OCR extracts text using on-device ML Kit — no internet needed. Export as PDF with one tap.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportPdfDialog(
            imageUri = state.imageUri,
            ocrText  = state.ocrText,
            onDismiss = { showExportDialog = false },
        )
    }
}

// ── OCR ───────────────────────────────────────────────────────────────────────────────

private suspend fun runOcr(context: Context, uri: Uri, update: (ScanResult) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = recognizer.process(image).await()
            val text = result.text
            withContext(Dispatchers.Main) {
                update(ScanResult(imageUri = uri, ocrText = text, isProcessing = false))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                update(ScanResult(imageUri = uri, ocrText = "", isProcessing = false, error = "OCR failed: ${e.localizedMessage}"))
            }
        }
    }
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
}.getOrNull()

private fun saveBitmap(context: Context, bmp: Bitmap): Uri {
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val dir = File(context.cacheDir, "scans").apply { mkdirs() }
    val file = File(dir, "scan_$ts.jpg")
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    return Uri.fromFile(file)
}

// ── PDF Export Dialog ──────────────────────────────────────────────────────────────────────

@Composable
private fun ExportPdfDialog(
    imageUri: Uri?,
    ocrText: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var includeImage by remember { mutableStateOf(true) }
    var includeText  by remember { mutableStateOf(true) }
    var isExporting  by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export as PDF", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose what to include in the PDF:")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = includeImage, onCheckedChange = { includeImage = it })
                    Text("Include scanned image")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = includeText, onCheckedChange = { includeText = it })
                    Text("Include extracted text")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!includeImage && !includeText) return@Button
                    isExporting = true
                    runCatching {
                        val file = buildScanPdf(context, imageUri, ocrText, includeImage, includeText)
                        exportedFile = file
                    }
                    isExporting = false
                },
                enabled = (includeImage || includeText) && !isExporting,
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Export PDF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    exportedFile?.let { file ->
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Scanned Document")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(android.content.Intent.createChooser(intent, "Share PDF")) }
        exportedFile = null
        onDismiss()
    }
}

private fun buildScanPdf(
    context: Context,
    imageUri: Uri?,
    ocrText: String,
    includeImage: Boolean,
    includeText: Boolean,
): File {
    val pageW = 595
    val pageH = 842
    val pdf   = PdfDocument()
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = android.graphics.Color.BLACK }
    val margin = 40f
    val lineH  = 18f
    var pageN  = 1

    fun newPage(): Pair<PdfDocument.Page, Canvas> {
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageN++).create()
        val p    = pdf.startPage(info)
        return p to p.canvas
    }

    var (page, canvas) = newPage()
    var y = margin + 30f

    // Title
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; isFakeBoldText = true; color = android.graphics.Color.BLACK }
    canvas.drawText("Scanned Document", margin, y, titlePaint)
    y += 30f

    val tsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = android.graphics.Color.GRAY }
    val dateStr = SimpleDateFormat("MMM d, yyyy · hh:mm a", Locale.getDefault()).format(Date())
    canvas.drawText("Nexus Plus Document Scanner · $dateStr", margin, y, tsPaint)
    y += 25f

    // Image
    if (includeImage && imageUri != null) {
        val bmp = loadBitmap(context, imageUri)
        bmp?.let {
            val scale = ((pageW - 2 * margin) / it.width.toFloat()).coerceAtMost((pageH - y - margin) / it.height.toFloat()).coerceAtMost(1f)
            val w = (it.width * scale).toInt()
            val h = (it.height * scale).toInt()
            val dstX = ((pageW - w) / 2f).coerceAtLeast(margin)
            if (y + h > pageH - margin) {
                pdf.finishPage(page); val (p2, c2) = newPage(); page = p2; canvas = c2; y = margin + 20f
            }
            canvas.drawBitmap(it, null, android.graphics.RectF(dstX, y, dstX + w, y + h), null)
            y += h + 20f
        }
    }

    // Text
    if (includeText && ocrText.isNotBlank()) {
        if (y + lineH > pageH - margin) {
            pdf.finishPage(page); val (p2, c2) = newPage(); page = p2; canvas = c2; y = margin + 20f
        }
        canvas.drawText("Extracted Text:", margin, y, titlePaint.apply { textSize = 14f })
        y += 25f

        val words = ocrText.split(Regex("\\s+"))
        var line = ""
        val textW = pageW - 2 * margin
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > textW) {
                if (y + lineH > pageH - margin) {
                    pdf.finishPage(page); val (p2, c2) = newPage(); page = p2; canvas = c2; y = margin + 20f
                }
                canvas.drawText(line, margin, y, paint)
                y += lineH
                line = word
            } else {
                line = test
            }
        }
        if (line.isNotBlank()) {
            if (y + lineH > pageH - margin) {
                pdf.finishPage(page); val (p2, c2) = newPage(); page = p2; canvas = c2; y = margin + 20f
            }
            canvas.drawText(line, margin, y, paint)
        }
    }

    pdf.finishPage(page)
    val dir  = File(context.cacheDir, "shared_pdfs").apply { mkdirs() }
    val file = File(dir, "scan_${System.currentTimeMillis()}.pdf")
    file.outputStream().use { pdf.writeTo(it) }
    pdf.close()
    return file
}

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            Text("Camera Permission Required", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Text("Allow camera access to scan documents with OCR and save them as PDFs.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Grant camera permission" }) {
                Text("Grant Permission")
            }
        }
    }
}
