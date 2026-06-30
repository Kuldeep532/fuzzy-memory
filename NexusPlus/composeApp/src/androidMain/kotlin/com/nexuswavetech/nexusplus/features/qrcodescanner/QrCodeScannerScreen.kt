package com.nexuswavetech.nexusplus.features.qrcodescanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import java.util.*

// ── Domain ────────────────────────────────────────────────────────────────────────────────────

data class ScanEntry(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: ScanType,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class ScanType(val label: String, val icon: ImageVector, val color: Color) {
    URL       ("URL",       Icons.Filled.Link,          Color(0xFF42A5F5)),
    WIFI      ("Wi-Fi",     Icons.Filled.Wifi,          Color(0xFF66BB6A)),
    CONTACT   ("Contact",   Icons.Filled.Contacts,      Color(0xFFAB47BC)),
    EMAIL     ("Email",     Icons.Filled.Email,         Color(0xFFEF5350)),
    PHONE     ("Phone",     Icons.Filled.Call,          Color(0xFFFFA726)),
    TEXT      ("Text",      Icons.Filled.TextSnippet,   Color(0xFF78909C)),
    UNKNOWN   ("Other",     Icons.Filled.QrCode,        Color(0xFF9E9E9E)),
}

fun classifyScan(content: String): ScanType = when {
    content.startsWith("http://", true) || content.startsWith("https://", true) -> ScanType.URL
    content.startsWith("WIFI:", true) -> ScanType.WIFI
    content.startsWith("BEGIN:VCARD", true) || content.startsWith("MECARD:", true) -> ScanType.CONTACT
    content.startsWith("mailto:", true) || content.startsWith("MATMSG:", true) -> ScanType.EMAIL
    content.startsWith("tel:", true) -> ScanType.PHONE
    content.matches(Regex("^\\d{10,15}$")) -> ScanType.PHONE
    else -> ScanType.TEXT
}

// ── Screen ─────────────────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScannerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf<List<ScanEntry>>(emptyList()) }
    var lastScan by remember { mutableStateOf<ScanEntry?>(null) }
    var pickedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showDialog by remember { mutableStateOf<ScanEntry?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pickedBitmap = loadBitmap(ctx, it)
            decodeQr(ctx, it)?.let { content ->
                val entry = ScanEntry(content = content, type = classifyScan(content))
                history = listOf(entry) + history.take(49)
                lastScan = entry
                showDialog = entry
            }
        }
    }

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "QR Scanner Pro",
                onBack = onBack,
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { history = emptyList(); lastScan = null }) {
                            Icon(Icons.Filled.DeleteSweep, "Clear", modifier = Modifier.size(22.dp))
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Scan button ───────────────────────────────────────────────────────────────
            item {
                Button(
                    onClick = { pickImage.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Scan from Image", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Picked image preview ──────────────────────────────────────────────────
            pickedBitmap?.let { bmp ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Scanned image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(8.dp),
                        )
                    }
                }
            }

            // ── Last scan result ───────────────────────────────────────────────────
            lastScan?.let { scan ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = scan.type.color.copy(alpha = 0.12f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(scan.type.icon, null, tint = scan.type.color, modifier = Modifier.size(22.dp))
                                Text(scan.type.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scan.type.color)
                            }
                            Text(scan.content, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionChip("Copy") {
                                    val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("QR", scan.content))
                                }
                                ActionChip("Share") {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, scan.content)
                                    }
                                    ctx.startActivity(Intent.createChooser(intent, "Share"))
                                }
                                if (scan.type == ScanType.URL) {
                                    ActionChip("Open") {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scan.content)))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── History ─────────────────────────────────────────────────────────────────────────────
            if (history.isNotEmpty()) {
                item { Text("History (${history.size})", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary) }
                items(history, key = { it.id }) { entry ->
                    HistoryRow(entry) { showDialog = entry }
                }
            }

            if (history.isEmpty() && lastScan == null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.QrCode, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Text("No scans yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Tap the button above to pick an image containing a QR code or barcode. The app will decode it instantly using on-device processing.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Action dialog
    showDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDialog = null },
            icon = { Icon(entry.type.icon, null, tint = entry.type.color) },
            title = { Text("${entry.type.label} Detected") },
            text = { Text(entry.content, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    when (entry.type) {
                        ScanType.URL -> ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.content)))
                        ScanType.PHONE -> ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entry.content}")))
                        ScanType.EMAIL -> ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${entry.content}")))
                        else -> {
                            val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("QR", entry.content))
                        }
                    }
                    showDialog = null
                }) { Text("Open / Copy") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) { Text("Close") }
            },
        )
    }
}

// ── Components ──────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
}

@Composable
private fun HistoryRow(entry: ScanEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(entry.type.icon, null, tint = entry.type.color, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.type.label, style = MaterialTheme.typography.labelSmall, color = entry.type.color, fontWeight = FontWeight.Bold)
                Text(entry.content.take(55) + if (entry.content.length > 55) "…" else "", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

// ── QR decode helpers (zxing-core) ───────────────────────────────────────────────────────────

private fun loadBitmap(ctx: Context, uri: Uri): android.graphics.Bitmap? {
    return runCatching {
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}

private fun decodeQr(ctx: Context, uri: Uri): String? {
    val bitmap = loadBitmap(ctx, uri) ?: return null
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
    val reader = MultiFormatReader()
    return runCatching {
        reader.decode(binaryBitmap).text
    }.getOrNull()
}
