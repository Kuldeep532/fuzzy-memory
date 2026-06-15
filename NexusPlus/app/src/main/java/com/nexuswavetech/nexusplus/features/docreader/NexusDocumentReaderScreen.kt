package com.nexuswavetech.nexusplus.features.docreader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.features.tts.NseRepository
import com.nexuswavetech.nexusplus.features.tts.NseSpeechMode
import com.nexuswavetech.nexusplus.features.tts.NseSpeechRequest
import com.nexuswavetech.nexusplus.features.tts.NseState
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.*
import org.koin.compose.koinInject

private val SUPPORTED_DOC_TYPES = arrayOf(
    "application/pdf",
    "text/plain",
    "text/html",
    "application/rtf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
)

private data class DocFile(val uri: Uri, val name: String, val type: String)

@Composable
fun NexusDocumentReaderScreen(
    initialUri: Uri? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view    = LocalView.current

    var currentDoc    by remember { mutableStateOf<DocFile?>(null) }
    var currentPage   by remember { mutableIntStateOf(0) }
    var totalPages    by remember { mutableIntStateOf(0) }
    var pageBitmap    by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading     by remember { mutableStateOf(false) }
    var scale         by remember { mutableFloatStateOf(1f) }
    var pdfRenderer   by remember { mutableStateOf<PdfRenderer?>(null) }
    var plainText     by remember { mutableStateOf<String?>(null) }

    val nseRepo: NseRepository = koinInject()
    val nseState by nseRepo.state.collectAsState()
    val isSpeaking = nseState is NseState.Speaking

    LaunchedEffect(Unit) {
        nseRepo.initialise()
        if (initialUri != null) {
            val name = context.contentResolver.query(initialUri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else "Document"
            } ?: "Document"
            val type = context.contentResolver.getType(initialUri) ?: "application/octet-stream"
            currentDoc = DocFile(initialUri, name, type)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else "Document"
            } ?: "Document"
            val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
            currentDoc = DocFile(uri, name, type)
            scale = 1f; currentPage = 0
        }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(currentDoc) {
        val doc = currentDoc ?: return@LaunchedEffect
        isLoading = true
        pageBitmap = null
        plainText = null
        when {
            doc.type == "application/pdf" -> {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val pfd = context.contentResolver.openFileDescriptor(doc.uri, "r")
                        pfd?.let {
                            pdfRenderer?.close()
                            val renderer = PdfRenderer(it)
                            pdfRenderer = renderer
                            totalPages = renderer.pageCount
                            currentPage = 0
                        }
                    }
                    isLoading = false
                }
            }
            doc.type.startsWith("text/") -> {
                withContext(Dispatchers.IO) {
                    plainText = runCatching {
                        context.contentResolver.openInputStream(doc.uri)?.bufferedReader()?.readText()
                    }.getOrNull() ?: "Unable to read file content."
                    isLoading = false
                }
            }
            else -> {
                plainText = "Preview not available for this file type.\n\nFile: ${doc.name}\n\nType: ${doc.type}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentPage, pdfRenderer) {
        val renderer = pdfRenderer ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            isLoading = true
            runCatching {
                val page = renderer.openPage(currentPage)
                val w = page.width * 2; val h = page.height * 2
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                android.graphics.Canvas(bmp).drawColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pageBitmap = bmp
            }
            isLoading = false
        }
        view.announceForAccessibility("Page ${currentPage + 1} of $totalPages")
    }

    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
            nseRepo.stop()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title = "Nexus Document Reader",
            onBack = onBack,
            actions = {
                if (currentDoc != null) {
                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                nseRepo.stop()
                                view.announceForAccessibility("Reading stopped")
                            } else {
                                val textToSpeak = when {
                                    plainText != null -> plainText!!.take(5000)
                                    pageBitmap != null -> "Page ${currentPage + 1} of $totalPages. PDF document: ${currentDoc!!.name}"
                                    else -> currentDoc!!.name
                                }
                                nseRepo.speak(
                                    NseSpeechRequest(
                                        text = textToSpeak,
                                        mode = NseSpeechMode.Auto,
                                        utteranceId = "doc_reader_${System.currentTimeMillis()}"
                                    )
                                )
                                view.announceForAccessibility("Reading document aloud")
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (isSpeaking) "Stop reading aloud" else "Read document aloud with NSE"
                        }
                    ) {
                        Icon(
                            if (isSpeaking) Icons.Filled.Stop else Icons.Filled.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        )

        if (currentDoc == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Filled.MenuBook, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Nexus Document Reader",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Supports PDF, TXT, DOC, DOCX, XLS, XLSX, PPT, PPTX, RTF\nBuilt-in TTS via NSE",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { filePicker.launch(SUPPORTED_DOC_TYPES) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Open a document file" }
                    ) {
                        Icon(Icons.Filled.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Document")
                    }
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AllInclusive, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Browse All Files")
                    }
                }
            }
        } else {
            currentDoc?.let { doc ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Description, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            doc.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (totalPages > 0) {
                            Text("${currentPage + 1}/$totalPages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { filePicker.launch(SUPPORTED_DOC_TYPES) },
                            modifier = Modifier.size(32.dp).semantics { contentDescription = "Open another document" }
                        ) {
                            Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (pageBitmap != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 4f)
                            }
                        }
                        .semantics { contentDescription = "PDF page ${currentPage + 1} of $totalPages. Pinch to zoom." },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading page" })
                    } else {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "Document page ${currentPage + 1}",
                            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            } else if (plainText != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(text = plainText!!, style = MaterialTheme.typography.bodyMedium)
                }
            } else if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text("Loading document…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (totalPages > 0) {
                Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0,
                            modifier = Modifier.semantics { contentDescription = "Previous page" }
                        ) { Icon(Icons.Filled.ChevronLeft, null) }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f) }, modifier = Modifier.semantics { contentDescription = "Zoom out" }) {
                                Icon(Icons.Filled.ZoomOut, null)
                            }
                            IconButton(onClick = { scale = 1f }, modifier = Modifier.semantics { contentDescription = "Reset zoom" }) {
                                Icon(Icons.Filled.FitScreen, null)
                            }
                            IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(4f) }, modifier = Modifier.semantics { contentDescription = "Zoom in" }) {
                                Icon(Icons.Filled.ZoomIn, null)
                            }
                        }

                        IconButton(
                            onClick = { if (currentPage < totalPages - 1) currentPage++ },
                            enabled = currentPage < totalPages - 1,
                            modifier = Modifier.semantics { contentDescription = "Next page" }
                        ) { Icon(Icons.Filled.ChevronRight, null) }
                    }
                }
            }

            if (isSpeaking) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.GraphicEq, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("NSE reading aloud…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { nseRepo.stop() }) { Text("Stop") }
                    }
                }
            }
        }
    }
}
