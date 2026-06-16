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
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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

// ── ViewModel & State Management ──────────────────────────────────────────────

enum class QrType(val label: String) { 
    TEXT("Text / URL"), 
    UPI("UPI Payment"),
    WIFI("Wi-Fi"),
    WHATSAPP("WhatsApp"),
    EMAIL("Email"),
    CONTACT("Contact (vCard)")
}

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
    val isSaving: Boolean = false
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
            QrType.TEXT -> s.textInput.trim().also {
                if (it.isBlank()) { _uiState.update { st -> st.copy(error = "Enter text or URL") }; return }
            }
            QrType.UPI -> buildString {
                if (s.upiId.isBlank()) { _uiState.update { st -> st.copy(error = "Enter UPI ID") }; return }
                append("upi://pay?pa=${s.upiId}")
                if (s.upiName.isNotBlank()) append("&pn=${s.upiName}")
                if (s.upiAmount.isNotBlank()) append("&am=${s.upiAmount}")
                append("&cu=INR")
                if (s.upiNote.isNotBlank()) append("&tn=${s.upiNote}")
            }
            QrType.WIFI -> buildString {
                if (s.wifiSsid.isBlank()) { _uiState.update { st -> st.copy(error = "Enter Wi-Fi SSID") }; return }
                append("WIFI:S:${s.wifiSsid};T:${s.wifiSecurity};P:${s.wifiPassword};;")
            }
            QrType.WHATSAPP -> buildString {
                if (s.whatsappNumber.isBlank()) { _uiState.update { st -> st.copy(error = "Enter WhatsApp number") }; return }
                val cleanNumber = s.whatsappNumber.replace("[^0-9]".toRegex(), "")
                append("https://wa.me/$cleanNumber")
                if (s.whatsappMessage.isNotBlank()) {
                    append("?text=${URLEncoder.encode(s.whatsappMessage, "UTF-8")}")
                }
            }
            QrType.EMAIL -> buildString {
                if (s.emailAddress.isBlank()) { _uiState.update { st -> st.copy(error = "Enter Email Address") }; return }
                append("mailto:${s.emailAddress}")
                val params = mutableListOf<String>()
                if (s.emailSubject.isNotBlank()) params.add("subject=${URLEncoder.encode(s.emailSubject, "UTF-8")}")
                if (s.emailBody.isNotBlank()) params.add("body=${URLEncoder.encode(s.emailBody, "UTF-8")}")
                if (params.isNotEmpty()) append("?${params.joinToString("&")}")
            }
            QrType.CONTACT -> buildString {
                if (s.contactName.isBlank() || s.contactPhone.isBlank()) { 
                    _uiState.update { st -> st.copy(error = "Name and Phone Number are required") }
                    return 
                }
                append("BEGIN:VCARD\nVERSION:3.0\n")
                append("FN:${s.contactName}\n")
                append("TEL:${s.contactPhone}\n")
                if (s.contactEmail.isNotBlank()) append("EMAIL:${s.contactEmail}\n")
                if (s.contactOrg.isNotBlank()) append("ORG:${s.contactOrg}\n")
                append("END:VCARD")
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
            else _uiState.update { it.copy(error = "Failed to generate QR code") }
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
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QR_Generator")
                    }
                    val imageUri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (imageUri != null) {
                        outputStream = context.contentResolver.openOutputStream(imageUri)
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/QR_Generator"
                    val dir = File(imagesDir)
                    if (!dir.exists()) dir.mkdirs()
                    val image = File(dir, filename)
                    outputStream = FileOutputStream(image)
                }

                if (outputStream != null) {
                    success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
            } finally {
                outputStream?.close()
            }

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isSaving = false) }
                onResult(success)
            }
        }
    }
}

// ── UI View Composables ───────────────────────────────────────────────────────

@Composable
fun QrCodeScreen(
    onBack: () -> Unit,
    viewModel: QrCodeViewModel = koinViewModel()
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Dynamic UI Flow: Show Form Layout only when no QR Code has been generated
            if (uiState.qrBitmap == null) {
                
                // Categorized Horizontal Chips List
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                        .semantics { contentDescription = "Horizontal navigation menu for scanning categories" },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QrType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.qrType == type,
                            onClick = { viewModel.setQrType(type) },
                            label = { Text(type.label) }
                        )
                    }
                }

                // Render Input Cards conditionally based on category selection
                when (uiState.qrType) {
                    QrType.TEXT -> {
                        OutlinedTextField(
                            value = uiState.textInput,
                            onValueChange = viewModel::setTextInput,
                            label = { Text("Text or URL Link") },
                            leadingIcon = { Icon(Icons.Filled.QrCode, null) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Input box for direct text or uniform resource locator" }
                        )
                    }
                    QrType.UPI -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("UPI Payment Setup", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                OutlinedTextField(value = uiState.upiId, onValueChange = viewModel::setUpiId,
                                    label = { Text("UPI Address ID *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("example@bank") })
                                OutlinedTextField(value = uiState.upiName, onValueChange = viewModel::setUpiName,
                                    label = { Text("Merchant or Payee Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = uiState.upiAmount, onValueChange = viewModel::setUpiAmount,
                                    label = { Text("Requested Amount (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = uiState.upiNote, onValueChange = viewModel::setUpiNote,
                                    label = { Text("Reference Note (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    QrType.WIFI -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Wireless Network Configuration", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                OutlinedTextField(value = uiState.wifiSsid, onValueChange = viewModel::setWifiSsid,
                                    label = { Text("Network Name (SSID) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = uiState.wifiPassword, onValueChange = viewModel::setWifiPassword,
                                    label = { Text("Access Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Text("Network Protocol Security Type", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("WPA", "WEP", "nopass").forEach { sec ->
                                        FilterChip(selected = uiState.wifiSecurity == sec, onClick = { viewModel.setWifiSecurity(sec) },
                                            label = { Text(if(sec == "nopass") "Open Network" else sec) })
                                    }
                                }
                            }
                        }
                    }
                    QrType.WHATSAPP -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("WhatsApp Immediate Chat Connection", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                OutlinedTextField(value = uiState.whatsappNumber, onValueChange = viewModel::setWhatsappNumber,
                                    label = { Text("Mobile Number with Country Code *") }, placeholder = { Text("e.g., 919876543210") },
                                    singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Enter target communication telephone number with code prefix" })
                                OutlinedTextField(value = uiState.whatsappMessage, onValueChange = viewModel::setWhatsappMessage,
                                    label = { Text("Pre-defined Message Dispatch (Optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    QrType.EMAIL -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Automated Electronic Mail Structure", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                OutlinedTextField(value = uiState.emailAddress, onValueChange = viewModel::setEmailAddress,
                                    label = { Text("Recipient Destination Email *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = uiState.emailSubject, onValueChange = viewModel::setEmailSubject,
                                    label = { Text("Subject Parameter Line") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = uiState.emailBody, onValueChange = viewModel::setEmailBody,
                                    label = { Text("Body Text Content Details") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    QrType.CONTACT -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("vCard Address Book Integration", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                OutlinedTextField(value = uiState.contactName, onValueChange = viewModel::setContactName,
                                    label = { Text("Full Profile Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = uiState.contactPhone, onValueChange = viewModel::setContactPhone,
                                    label = { Text("Primary Telephone Contact *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = uiState.contactEmail, onValueChange = viewModel::setContactEmail,
                                    label = { Text("Mailing Address Local Contact") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = uiState.contactOrg, onValueChange = viewModel::setContactOrg,
                                    label = { Text("Company or Associated Organization") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                // Process Request Action Trigger Button
                Button(
                    onClick = {
                        haptic.confirm(view, touchVib)
                        viewModel.generate()
                        view.announceForAccessibility("QR Code computation complete. Configuration menus collapsed.")
                    },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Execute matrix transformation to generate QR graphic" }
                ) {
                    Icon(Icons.Filled.QrCode, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate QR Code")
                }
            }

            // Centralized Reactive Error Feedback Handler
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { contentDescription = "Error notification trace: $it" })
            }

            // Success State Section: Display Only the Action Hub and Bitmap Preview
            uiState.qrBitmap?.let { bmp ->
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.size(280.dp).semantics { contentDescription = "Rendered layout representation image of active matrix data code" }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Active matrix array barcode graphics",
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }

                // Construct Context-Aware Accessibility Text Summary
                val accessibleSummary = when (uiState.qrType) {
                    QrType.TEXT -> "Text base data structure format payload code"
                    QrType.UPI -> "Financial transactional direct settlement code to ${uiState.upiId}"
                    QrType.WIFI -> "Access point sharing credential token for network connection target ${uiState.wifiSsid}"
                    QrType.WHATSAPP -> "Direct instant communication link target shortcut profile number ${uiState.whatsappNumber}"
                    QrType.EMAIL -> "Electronic correspondence template addressing direct target user ${uiState.emailAddress}"
                    QrType.CONTACT -> "Business card indexing automation properties metadata card for ${uiState.contactName}"
                }
                
                Text(
                    text = accessibleSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Storage Media Access Serialization Control Engine
                Button(
                    onClick = {
                        haptic.confirm(view, touchVib)
                        viewModel.saveQrToGallery(context, bmp) { success ->
                            if (success) {
                                Toast.makeText(context, "Saved successfully to Pictures/QR_Generator", Toast.LENGTH_SHORT).show()
                                view.announceForAccessibility("Download complete. Asset committed inside your localized gallery structure storage.")
                            } else {
                                Toast.makeText(context, "Error processing download serialization", Toast.LENGTH_SHORT).show()
                                view.announceForAccessibility("Failed system IO execution routine pipeline.")
                            }
                        }
                    },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Commit active graphics asset directly into persistent local multimedia storage arrays" }
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download QR Code")
                    }
                }

                // Reset Controller Layout Stack Engine State
                OutlinedButton(
                    onClick = {
                        haptic.confirm(view, touchVib)
                        viewModel.clearQr()
                        view.announceForAccessibility("Dynamic clearing cache triggered. Form variables restored onto execution main stack view.")
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Purge generated graphics from system memory state and recall input collection templates" }
                ) {
                    Icon(Icons.Filled.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete & Create Another")
                }
            }
        }
    }
}
