package com.nexuswavetech.nexusplus.features.pdfsuite

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class PdfTool(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    READ("Read PDF", Icons.Filled.MenuBook),
    IMAGES_TO_PDF("Images → PDF", Icons.Filled.PhotoLibrary),
    TEXT_TO_PDF("Text → PDF", Icons.Filled.TextFields),
    MERGE("Merge PDFs", Icons.Filled.CallMerge),
    SPLIT("Split PDF", Icons.Filled.CallSplit),
    REORDER("Reorder Pages", Icons.Filled.SwapVert)
}

@Composable
fun PdfSuiteScreen(onBack: () -> Unit) {
    var selectedTool by remember { mutableStateOf<PdfTool?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title = if (selectedTool != null) selectedTool!!.label else "PDF Suite",
            onBack = { if (selectedTool != null) selectedTool = null else onBack() }
        )

        if (selectedTool == null) {
            // Tool picker grid
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Choose a PDF tool",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(PdfTool.values()) { tool ->
                    Card(
                        onClick = { selectedTool = tool },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "${tool.label}. Double tap to open." }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Text(tool.label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        } else {
            when (selectedTool) {
                PdfTool.READ         -> ReadPdfTool(context)
                PdfTool.IMAGES_TO_PDF-> ImagesToPdfTool(context)
                PdfTool.TEXT_TO_PDF  -> TextToPdfTool(context)
                PdfTool.MERGE        -> MergePdfTool(context)
                PdfTool.SPLIT        -> SplitPdfTool(context)
                PdfTool.REORDER      -> ReorderPagesTool(context)
                null -> {}
            }
        }
    }
}

// ── Read PDF ─────────────────────────────────────────────────────────────────

@Composable
fun ReadPdfTool(context: Context) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pdfUri = it
            scope.launch {
                isLoading = true
                pages = withContext(Dispatchers.IO) { renderPdfPages(context, it, maxPages = 50) }
                isLoading = false
                view.announceForAccessibility("${pages.size} pages loaded")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { launcher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Open PDF")
        }
        if (isLoading) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        if (pages.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0) { Icon(Icons.Filled.NavigateBefore, "Previous page") }
                Text("Page ${currentPage + 1} / ${pages.size}", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { if (currentPage < pages.size - 1) currentPage++ }, enabled = currentPage < pages.size - 1) { Icon(Icons.Filled.NavigateNext, "Next page") }
            }
            Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = "PDF page ${currentPage + 1}" }) {
                Image(bitmap = pages[currentPage].asImageBitmap(), contentDescription = "PDF page ${currentPage + 1}", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun renderPdfPages(context: Context, uri: Uri, maxPages: Int): List<Bitmap> {
    val pages = mutableListOf<Bitmap>()
    runCatching {
        val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return pages
        fd.use {
            val renderer = PdfRenderer(it)
            renderer.use { r ->
                val count = minOf(r.pageCount, maxPages)
                for (i in 0 until count) {
                    r.openPage(i).use { page ->
                        val scale = 2f
                        val bmp = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp).apply { drawColor(android.graphics.Color.WHITE) }
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages.add(bmp)
                    }
                }
            }
        }
    }
    return pages
}

// ── Images to PDF ─────────────────────────────────────────────────────────────

@Composable
fun ImagesToPdfTool(context: Context) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        imageUris = uris; view.announceForAccessibility("${uris.size} images selected")
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select images to combine into a PDF.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { launcher.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PhotoLibrary, null); Spacer(Modifier.width(4.dp)); Text("Select Images")
            }
            Button(
                onClick = {
                    if (imageUris.isEmpty()) return@Button
                    scope.launch {
                        isLoading = true
                        result = withContext(Dispatchers.IO) { convertImagesToPdf(context, imageUris) }
                        isLoading = false
                        view.announceForAccessibility("PDF created")
                    }
                },
                enabled = imageUris.isNotEmpty() && !isLoading,
                modifier = Modifier.weight(1f)
            ) { if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else { Icon(Icons.Filled.PictureAsPdf, null); Spacer(Modifier.width(4.dp)); Text("Convert") } }
        }
        if (imageUris.isNotEmpty()) Text("${imageUris.size} image(s) selected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        if (result.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
            Text(result, modifier = Modifier.padding(12.dp))
        }
    }
}

private fun convertImagesToPdf(context: Context, uris: List<Uri>): String {
    return runCatching {
        val doc = PdfDocument()
        uris.forEachIndexed { idx, uri ->
            val bmp = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: return@forEachIndexed
            val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, idx + 1).create()
            val page = doc.startPage(info)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(page)
        }
        val outFile = File(context.cacheDir, "nexus_images_${System.currentTimeMillis()}.pdf")
        doc.writeTo(FileOutputStream(outFile))
        doc.close()
        "PDF saved: ${outFile.name}"
    }.getOrElse { "Failed: ${it.message}" }
}

// ── Text to PDF ───────────────────────────────────────────────────────────────

@Composable
fun TextToPdfTool(context: Context) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Type or paste text to generate a PDF.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = inputText, onValueChange = { inputText = it },
            label = { Text("Enter text content") }, minLines = 8,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    result = withContext(Dispatchers.IO) { convertTextToPdf(context, inputText) }
                    isLoading = false
                    view.announceForAccessibility("PDF created")
                }
            },
            enabled = inputText.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else { Icon(Icons.Filled.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text("Generate PDF") }
        }
        if (result.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
            Text(result, modifier = Modifier.padding(12.dp))
        }
    }
}

private fun convertTextToPdf(context: Context, text: String): String {
    return runCatching {
        val doc = PdfDocument()
        val paint = Paint().apply { textSize = 14f; color = android.graphics.Color.BLACK }
        val pageWidth = 595; val pageHeight = 842
        val margin = 40; val lineHeight = 20
        val maxWidth = pageWidth - 2 * margin
        val lines = mutableListOf<String>()
        text.split("\n").forEach { para ->
            var remaining = para
            while (remaining.isNotEmpty()) {
                val width = paint.measureText(remaining)
                if (width <= maxWidth) { lines.add(remaining); break }
                var end = remaining.length
                while (end > 0 && paint.measureText(remaining.substring(0, end)) > maxWidth) end--
                val breakAt = remaining.lastIndexOf(' ', end).takeIf { it > 0 } ?: end
                lines.add(remaining.substring(0, breakAt))
                remaining = remaining.substring(breakAt).trimStart()
            }
        }
        var pageNum = 1; var y = margin + lineHeight
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = doc.startPage(pageInfo)
        for (line in lines) {
            if (y + lineHeight > pageHeight - margin) {
                doc.finishPage(page)
                pageNum++; y = margin + lineHeight
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = doc.startPage(pageInfo)
            }
            page.canvas.drawText(line, margin.toFloat(), y.toFloat(), paint)
            y += lineHeight
        }
        doc.finishPage(page)
        val outFile = File(context.cacheDir, "nexus_text_${System.currentTimeMillis()}.pdf")
        doc.writeTo(FileOutputStream(outFile))
        doc.close()
        "PDF saved: ${outFile.name} ($pageNum page${if (pageNum != 1) "s" else ""})"
    }.getOrElse { "Failed: ${it.message}" }
}

// ── Merge PDFs ────────────────────────────────────────────────────────────────

@Composable
fun MergePdfTool(context: Context) {
    val view = LocalView.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CallMerge, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Merge PDFs", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Text(
                "PDF merging requires page-level byte manipulation beyond Android's built-in PdfDocument API.\n\n" +
                "To merge PDFs:\n• Use a third-party PDF library (e.g., iText7)\n• Or use the PDF Suite's Text → PDF / Images → PDF tools to build combined documents.\n\n" +
                "This feature is planned for a future update with library integration.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Split PDF ─────────────────────────────────────────────────────────────────

@Composable
fun SplitPdfTool(context: Context) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var fromPage by remember { mutableStateOf("1") }
    var toPage by remember { mutableStateOf("1") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pdfUri = it
            scope.launch {
                pageCount = withContext(Dispatchers.IO) {
                    runCatching {
                        val fd = context.contentResolver.openFileDescriptor(it, "r") ?: return@runCatching 0
                        fd.use { pfd -> PdfRenderer(pfd).use { r -> r.pageCount } }
                    }.getOrDefault(0)
                }
                toPage = pageCount.toString()
                view.announceForAccessibility("$pageCount pages found")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { launcher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Select PDF")
        }
        if (pageCount > 0) {
            Text("$pageCount pages found", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fromPage, onValueChange = { fromPage = it }, label = { Text("From page") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = toPage, onValueChange = { toPage = it }, label = { Text("To page") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val from = (fromPage.toIntOrNull() ?: 1) - 1
                        val to = (toPage.toIntOrNull() ?: pageCount) - 1
                        result = withContext(Dispatchers.IO) {
                            runCatching {
                                val fd = context.contentResolver.openFileDescriptor(pdfUri!!, "r")!!
                                val bitmaps = fd.use { pfd ->
                                    PdfRenderer(pfd).use { r ->
                                        (from..minOf(to, r.pageCount - 1)).map { idx ->
                                            r.openPage(idx).use { p ->
                                                Bitmap.createBitmap(p.width, p.height, Bitmap.Config.ARGB_8888).also { b ->
                                                    Canvas(b).drawColor(android.graphics.Color.WHITE)
                                                    p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                }
                                            }
                                        }
                                    }
                                }
                                val doc = PdfDocument()
                                bitmaps.forEachIndexed { i, bmp ->
                                    val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, i + 1).create()
                                    val pg = doc.startPage(info); pg.canvas.drawBitmap(bmp, 0f, 0f, null); doc.finishPage(pg)
                                }
                                val outFile = File(context.cacheDir, "nexus_split_${System.currentTimeMillis()}.pdf")
                                doc.writeTo(FileOutputStream(outFile)); doc.close()
                                "Split PDF saved: ${outFile.name} (${bitmaps.size} pages)"
                            }.getOrElse { "Failed: ${it.message}" }
                        }
                        isLoading = false
                        view.announceForAccessibility("Split complete")
                    }
                },
                enabled = !isLoading, modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else { Icon(Icons.Filled.CallSplit, null); Spacer(Modifier.width(8.dp)); Text("Extract Pages") }
            }
        }
        if (result.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
            Text(result, modifier = Modifier.padding(12.dp))
        }
    }
}

// ── Reorder Pages ─────────────────────────────────────────────────────────────

@Composable
fun ReorderPagesTool(context: Context) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var thumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var pageOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pdfUri = it
            scope.launch {
                isLoading = true
                thumbnails = withContext(Dispatchers.IO) { renderPdfPages(context, it, maxPages = 20) }
                pageOrder = thumbnails.indices.toList()
                isLoading = false
                view.announceForAccessibility("${thumbnails.size} pages loaded")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { launcher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Open PDF")
        }
        if (isLoading) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        if (pageOrder.isNotEmpty()) {
            Text("Drag pages using Up/Down buttons to reorder:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(pageOrder) { position, origIdx ->
                    Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Page ${origIdx + 1} at position ${position + 1}" }) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (thumbnails.size > origIdx) Image(thumbnails[origIdx].asImageBitmap(), "Page ${origIdx + 1}", modifier = Modifier.size(width = 60.dp, height = 80.dp))
                            Text("Page ${origIdx + 1}", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Column {
                                IconButton(onClick = {
                                    if (position > 0) {
                                        val newOrder = pageOrder.toMutableList()
                                        newOrder.removeAt(position); newOrder.add(position - 1, origIdx)
                                        pageOrder = newOrder
                                        view.announceForAccessibility("Page ${origIdx + 1} moved up")
                                    }
                                }, enabled = position > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
                                IconButton(onClick = {
                                    if (position < pageOrder.size - 1) {
                                        val newOrder = pageOrder.toMutableList()
                                        newOrder.removeAt(position); newOrder.add(position + 1, origIdx)
                                        pageOrder = newOrder
                                        view.announceForAccessibility("Page ${origIdx + 1} moved down")
                                    }
                                }, enabled = position < pageOrder.size - 1) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        result = withContext(Dispatchers.IO) {
                            runCatching {
                                val fd = context.contentResolver.openFileDescriptor(pdfUri!!, "r")!!
                                val reordered = fd.use { pfd ->
                                    PdfRenderer(pfd).use { r ->
                                        pageOrder.map { idx ->
                                            r.openPage(idx).use { p ->
                                                Bitmap.createBitmap(p.width, p.height, Bitmap.Config.ARGB_8888).also { b ->
                                                    Canvas(b).drawColor(android.graphics.Color.WHITE)
                                                    p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                }
                                            }
                                        }
                                    }
                                }
                                val doc = PdfDocument()
                                reordered.forEachIndexed { i, bmp ->
                                    val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, i + 1).create()
                                    val pg = doc.startPage(info); pg.canvas.drawBitmap(bmp, 0f, 0f, null); doc.finishPage(pg)
                                }
                                val outFile = File(context.cacheDir, "nexus_reordered_${System.currentTimeMillis()}.pdf")
                                doc.writeTo(FileOutputStream(outFile)); doc.close()
                                "Reordered PDF saved: ${outFile.name}"
                            }.getOrElse { "Failed: ${it.message}" }
                        }
                        isLoading = false
                        view.announceForAccessibility("Reorder complete")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Save, null); Spacer(Modifier.width(8.dp)); Text("Save Reordered PDF")
            }
        }
        if (result.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
            Text(result, modifier = Modifier.padding(12.dp))
        }
    }
}
