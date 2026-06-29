package com.nexuswavetech.nexusplus.features.qrcode

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLEncoder

// ── QR Types ──────────────────────────────────────────────────────────────────

enum class QrType(val label: String) {
    TEXT("Text / URL"),
    UPI("UPI Payment"),
    WIFI("Wi-Fi"),
    WHATSAPP("WhatsApp"),
    EMAIL("Email"),
    CONTACT("Contact Card")
}

// ── State & ViewModel ─────────────────────────────────────────────────────────

data class QrUiState(
    val qrType: QrType = QrType.TEXT,
    val textInput: String = "",
    val upiId: String = "",
    val upiName: String = "",
    val upiAmount: String = "",
    val upiNote: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: String = "WPA",
    val whatsappNumber: String = "",
    val whatsappMessage: String = "",
    val emailAddress: String = "",
    val emailSubject: String = "",
    val emailBody: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val contactOrg: String = "",
    val qrBitmap: Bitmap? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
)

class QrCodeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()

    fun setQrType(t: QrType) = _uiState.update { it.copy(qrType = t, qrBitmap = null, error = null) }
    fun setTextInput(v: String) = _uiState.update { it.copy(textInput = v) }
    fun setUpiId(v: String) = _uiState.update { it.copy(upiId = v) }
    fun setUpiName(v: String) = _uiState.update { it.copy(upiName = v) }
    fun setUpiAmount(v: String) = _uiState.update { it.copy(upiAmount = v) }
    fun setUpiNote(v: String) = _uiState.update { it.copy(upiNote = v) }
    fun setWifiSsid(v: String) = _uiState.update { it.copy(wifiSsid = v) }
    fun setWifiPassword(v: String) = _uiState.update { it.copy(wifiPassword = v) }
    fun setWifiSecurity(v: String) = _uiState.update { it.copy(wifiSecurity = v) }
    fun setWhatsappNumber(v: String) = _uiState.update { it.copy(whatsappNumber = v) }
    fun setWhatsappMessage(v: String) = _uiState.update { it.copy(whatsappMessage = v) }
    fun setEmailAddress(v: String) = _uiState.update { it.copy(emailAddress = v) }
    fun setEmailSubject(v: String) = _uiState.update { it.copy(emailSubject = v) }
    fun setEmailBody(v: String) = _uiState.update { it.copy(emailBody = v) }
    fun setContactName(v: String) = _uiState.update { it.copy(contactName = v) }
    fun setContactPhone(v: String) = _uiState.update { it.copy(contactPhone = v) }
    fun setContactEmail(v: String) = _uiState.update { it.copy(contactEmail = v) }
    fun setContactOrg(v: String) = _uiState.update { it.copy(contactOrg = v) }
    fun clearQr() = _uiState.update { it.copy(qrBitmap = null, error = null) }

    fun generate() {
        val s = _uiState.value
        val content = when (s.qrType) {
            QrType.TEXT -> {
                if (s.textInput.isBlank()) { _uiState.update { it.copy(error = "Please enter text or a URL") }; return }
                s.textInput.trim()
            }
            QrType.UPI -> {
                if (s.upiId.isBlank()) { _uiState.update { it.copy(error = "Please enter your UPI ID") }; return }
                buildString {
                    append("upi://pay?pa=${s.upiId}")
                    if (s.upiName.isNotBlank()) append("&pn=${s.upiName}")
                    if (s.upiAmount.isNotBlank()) append("&am=${s.upiAmount}")
                    append("&cu=INR")
                    if (s.upiNote.isNotBlank()) append("&tn=${s.upiNote}")
                }
            }
            QrType.WIFI -> {
                if (s.wifiSsid.isBlank()) { _uiState.update { it.copy(error = "Please enter the Wi-Fi name") }; return }
                "WIFI:S:${s.wifiSsid};T:${s.wifiSecurity};P:${s.wifiPassword};;"
            }
            QrType.WHATSAPP -> {
                if (s.whatsappNumber.isBlank()) { _uiState.update { it.copy(error = "Please enter the phone number with country code") }; return }
                val num = s.whatsappNumber.replace("[^0-9]".toRegex(), "")
                buildString {
                    append("https://wa.me/$num")
                    if (s.whatsappMessage.isNotBlank()) append("?text=${URLEncoder.encode(s.whatsappMessage, "UTF-8")}")
                }
            }
            QrType.EMAIL -> {
                if (s.emailAddress.isBlank()) { _uiState.update { it.copy(error = "Please enter an email address") }; return }
                buildString {
                    append("mailto:${s.emailAddress}")
                    val params = mutableListOf<String>()
                    if (s.emailSubject.isNotBlank()) params.add("subject=${URLEncoder.encode(s.emailSubject, "UTF-8")}")
                    if (s.emailBody.isNotBlank()) params.add("body=${URLEncoder.encode(s.emailBody, "UTF-8")}")
                    if (params.isNotEmpty()) append("?${params.joinToString("&")}")
                }
            }
            QrType.CONTACT -> {
                if (s.contactName.isBlank() || s.contactPhone.isBlank()) {
                    _uiState.update { it.copy(error = "Name and phone number are required") }; return
                }
                buildString {
                    append("BEGIN:VCARD\nVERSION:3.0\n")
                    append("FN:${s.contactName}\n")
                    append("TEL:${s.contactPhone}\n")
                    if (s.contactEmail.isNotBlank()) append("EMAIL:${s.contactEmail}\n")
                    if (s.contactOrg.isNotBlank()) append("ORG:${s.contactOrg}\n")
                    append("END:VCARD")
                }
            }
        }

        viewModelScope.launch {
            val bmp = withContext(Dispatchers.Default) {
                runCatching {
                    val size = 512
                    val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8")
                    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
                    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
                    for (x in 0 until size) for (y in 0 until size) {
                        bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                    bmp
                }.getOrNull()
            }
            if (bmp != null) _uiState.update { it.copy(qrBitmap = bmp, error = null) }
            else _uiState.update { it.copy(error = "Could not generate QR code. Please try again.") }
        }
    }

    fun saveQrToGallery(context: Context, bitmap: Bitmap, onResult: (Boolean) -> Unit) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val filename = "QR_${System.currentTimeMillis()}.png"
            var outputStream: OutputStream? = null
            var success = false
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val cv = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QR_Generator")
                    }
                    val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                    if (uri != null) outputStream = context.contentResolver.openOutputStream(uri)
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QR_Generator")
                    if (!dir.exists()) dir.mkdirs()
                    outputStream = FileOutputStream(File(dir, filename))
                }
                if (outputStream != null) { success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); outputStream.flush() }
            } catch (_: Exception) { success = false } finally { outputStream?.close() }
            withContext(Dispatchers.Main) { _uiState.update { it.copy(isSaving = false) }; onResult(success) }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScreen(
    onBack: () -> Unit,
    viewModel: QrCodeViewModel = koinViewModel(),
) {
    val uiState  by viewModel.uiState.collectAsState()
    val view     = LocalView.current
    val context  = LocalContext.current
    val haptic   = koinInject<HapticHelper>()
    val settings = koinInject<SettingsRepository>()
    val touchVib by settings.touchVibration.collectAsState(initial = true)

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "QR Code Generator", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── When QR is generated — show result ─────────────────────────
            if (uiState.qrBitmap != null) {
                QrResultView(
                    uiState   = uiState,
                    context   = context,
                    view      = view,
                    haptic    = haptic,
                    touchVib  = touchVib,
                    onSave    = { bmp ->
                        viewModel.saveQrToGallery(context, bmp) { ok ->
                            if (ok) {
                                Toast.makeText(context, "Saved to Photos / QR_Generator", Toast.LENGTH_SHORT).show()
                                view.announceForAccessibility("QR code saved to gallery")
                            } else {
                                Toast.makeText(context, "Could not save. Check storage permission.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onMakeAnother = {
                        haptic.confirm(view, touchVib)
                        viewModel.clearQr()
                        view.announceForAccessibility("Form cleared. Ready for a new QR code.")
                    },
                )
                return@Column
            }

            // ── QR type selector (dropdown) ────────────────────────────────
            QrTypeDropdown(
                selected = uiState.qrType,
                onSelect = { viewModel.setQrType(it) },
            )

            // ── Input fields for selected type ─────────────────────────────
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        when (uiState.qrType) {
                            QrType.TEXT     -> "Text or Website URL"
                            QrType.UPI      -> "UPI Payment Details"
                            QrType.WIFI     -> "Wi-Fi Network Details"
                            QrType.WHATSAPP -> "WhatsApp Chat Link"
                            QrType.EMAIL    -> "Email Details"
                            QrType.CONTACT  -> "Contact Card Details"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    when (uiState.qrType) {
                        QrType.TEXT -> OutlinedTextField(
                            value = uiState.textInput,
                            onValueChange = viewModel::setTextInput,
                            label = { Text("Text or URL *") },
                            leadingIcon = { Icon(Icons.Filled.Link, null) },
                            placeholder = { Text("e.g. https://example.com or any text") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "Enter the text or URL for your QR code"
                            },
                        )

                        QrType.UPI -> {
                            OutlinedTextField(uiState.upiId, viewModel::setUpiId, label = { Text("UPI ID *") }, placeholder = { Text("example@bank") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(uiState.upiName, viewModel::setUpiName, label = { Text("Payee Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(uiState.upiAmount, viewModel::setUpiAmount, label = { Text("Amount in ₹ (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(uiState.upiNote, viewModel::setUpiNote, label = { Text("Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }

                        QrType.WIFI -> {
                            OutlinedTextField(uiState.wifiSsid, viewModel::setWifiSsid, label = { Text("Wi-Fi Name (SSID) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(uiState.wifiPassword, viewModel::setWifiPassword, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Text("Security Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("WPA" to "WPA/WPA2", "WEP" to "WEP", "nopass" to "No Password").forEach { (sec, lbl) ->
                                    FilterChip(
                                        selected = uiState.wifiSecurity == sec,
                                        onClick = { viewModel.setWifiSecurity(sec) },
                                        label = { Text(lbl) },
                                        modifier = Modifier.semantics { contentDescription = "Security type: $lbl" },
                                    )
                                }
                            }
                        }

                        QrType.WHATSAPP -> {
                            OutlinedTextField(uiState.whatsappNumber, viewModel::setWhatsappNumber, label = { Text("Phone Number with Country Code *") }, placeholder = { Text("919876543210") }, singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Enter phone number including country code, for example 919876543210 for India" })
                            OutlinedTextField(uiState.whatsappMessage, viewModel::setWhatsappMessage, label = { Text("Pre-filled Message (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        }

                        QrType.EMAIL -> {
                            OutlinedTextField(uiState.emailAddress, viewModel::setEmailAddress, label = { Text("Email Address *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(uiState.emailSubject, viewModel::setEmailSubject, label = { Text("Subject (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(uiState.emailBody, viewModel::setEmailBody, label = { Text("Message Body (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        }

                        QrType.CONTACT -> {
                            OutlinedTextField(uiState.contactName, viewModel::setContactName, label = { Text("Full Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(uiState.contactPhone, viewModel::setContactPhone, label = { Text("Phone Number *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(uiState.contactEmail, viewModel::setContactEmail, label = { Text("Email (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(uiState.contactOrg, viewModel::setContactOrg, label = { Text("Company / Organisation (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // ── Error ──────────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // ── Generate button ────────────────────────────────────────────
            Button(
                onClick = {
                    haptic.confirm(view, touchVib)
                    viewModel.generate()
                    view.announceForAccessibility("Generating QR code")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Generate QR code" },
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Filled.QrCode, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate QR Code", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── QR Type Dropdown ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrTypeDropdown(
    selected: QrType,
    onSelect: (QrType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("What do you want to make a QR for?") },
            leadingIcon = {
                Icon(
                    when (selected) {
                        QrType.TEXT     -> Icons.Filled.TextFields
                        QrType.UPI      -> Icons.Filled.CurrencyRupee
                        QrType.WIFI     -> Icons.Filled.Wifi
                        QrType.WHATSAPP -> Icons.Filled.Chat
                        QrType.EMAIL    -> Icons.Filled.Email
                        QrType.CONTACT  -> Icons.Filled.ContactPage
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
                .semantics { contentDescription = "QR type selector. Currently: ${selected.label}." },
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            QrType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onSelect(type); expanded = false },
                    leadingIcon = {
                        Icon(
                            when (type) {
                                QrType.TEXT     -> Icons.Filled.TextFields
                                QrType.UPI      -> Icons.Filled.CurrencyRupee
                                QrType.WIFI     -> Icons.Filled.Wifi
                                QrType.WHATSAPP -> Icons.Filled.Chat
                                QrType.EMAIL    -> Icons.Filled.Email
                                QrType.CONTACT  -> Icons.Filled.ContactPage
                            },
                            contentDescription = null,
                            tint = if (type == selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = if (type == selected) {
                        { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.semantics { contentDescription = type.label },
                )
            }
        }
    }
}

// ── QR Result View ─────────────────────────────────────────────────────────────

@Composable
private fun QrResultView(
    uiState: QrUiState,
    context: Context,
    view: android.view.View,
    haptic: HapticHelper,
    touchVib: Boolean,
    onSave: (Bitmap) -> Unit,
    onMakeAnother: () -> Unit,
) {
    val bmp = uiState.qrBitmap ?: return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Your QR Code is ready!",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .size(280.dp)
                .semantics {
                    contentDescription = "Generated QR code for ${uiState.qrType.label}. Use the Save button below to save it."
                },
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )
        }

        Text(
            when (uiState.qrType) {
                QrType.TEXT     -> "Scan to open: ${uiState.textInput.take(40)}${if (uiState.textInput.length > 40) "…" else ""}"
                QrType.UPI      -> "Scan to pay: ${uiState.upiId}"
                QrType.WIFI     -> "Scan to connect to: ${uiState.wifiSsid}"
                QrType.WHATSAPP -> "Scan to chat with: ${uiState.whatsappNumber}"
                QrType.EMAIL    -> "Scan to email: ${uiState.emailAddress}"
                QrType.CONTACT  -> "Scan to save contact: ${uiState.contactName}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { haptic.confirm(view, touchVib); onSave(bmp) },
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = "Save QR code to your phone gallery" },
            shape = MaterialTheme.shapes.large,
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Saving…")
            } else {
                Icon(Icons.Filled.SaveAlt, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save to Gallery", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = onMakeAnother,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Go back and create a new QR code" },
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Make Another QR Code")
        }
    }
}
