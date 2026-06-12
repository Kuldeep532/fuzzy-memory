package com.nexuswavetech.nexusplus.features.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.withContext

@Composable
fun PdfReaderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current

    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pdfUri = uri
            runCatching {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                pfd?.let {
                    pdfRenderer?.close()
                    val renderer = PdfRenderer(it)
                    pdfRenderer = renderer
                    totalPages = renderer.pageCount
                    currentPage = 0
                }
            }
        }
    }

    suspend fun renderPage(page: Int) {
        val renderer = pdfRenderer ?: return
        if (page < 0 || page >= renderer.pageCount) return
        withContext(Dispatchers.IO) {
            isLoading = true
            val pdfPage = renderer.openPage(page)
            val width = pdfPage.width * 2
            val height = pdfPage.height * 2
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(bitmap).drawColor(android.graphics.Color.WHITE)
            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfPage.close()
            pageBitmap = bitmap
            isLoading = false
        }
    }

    LaunchedEffect(currentPage, pdfRenderer) {
        if (pdfRenderer != null) {
            renderPage(currentPage)
            view.announceForAccessibility("Page ${currentPage + 1} of $totalPages")
        }
    }

    DisposableEffect(Unit) {
        onDispose { pdfRenderer?.close() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title = "PDF Reader",
            onBack = onBack,
            actions = {
                if (totalPages > 0) {
                    Text(
                        text = "${currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .semantics { contentDescription = "Page ${currentPage + 1} of $totalPages" }
                    )
                }
            }
        )

        if (pdfUri == null) {
            // File picker prompt
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Open a PDF file",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Button(
                        onClick = { filePicker.launch("application/pdf") },
                        modifier = Modifier.semantics {
                            contentDescription = "Open PDF file. Tap to browse files and select a PDF."
                        }
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Browse Files")
                    }
                }
            }
        } else {
            // PDF viewer area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 4f)
                        }
                    }
                    .semantics {
                        contentDescription = "PDF page ${currentPage + 1} of $totalPages. " +
                            "Pinch to zoom. Use navigation buttons below to change pages."
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription = "Loading page ${currentPage + 1}"
                        }
                    )
                } else {
                    pageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF page ${currentPage + 1} content",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(scaleX = scale, scaleY = scale),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Navigation bar
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentPage > 0) currentPage--
                        },
                        enabled = currentPage > 0,
                        modifier = Modifier.semantics { contentDescription = "Previous page" }
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                    }

                    // Zoom controls
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f) },
                            modifier = Modifier.semantics { contentDescription = "Zoom out" }
                        ) {
                            Icon(Icons.Filled.ZoomOut, contentDescription = null)
                        }
                        IconButton(
                            onClick = { scale = 1f },
                            modifier = Modifier.semantics { contentDescription = "Reset zoom" }
                        ) {
                            Icon(Icons.Filled.FitScreen, contentDescription = null)
                        }
                        IconButton(
                            onClick = { scale = (scale + 0.25f).coerceAtMost(4f) },
                            modifier = Modifier.semantics { contentDescription = "Zoom in" }
                        ) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = null)
                        }
                    }

                    IconButton(
                        onClick = {
                            if (currentPage < totalPages - 1) currentPage++
                        },
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.semantics { contentDescription = "Next page" }
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}
