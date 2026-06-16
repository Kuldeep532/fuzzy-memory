package com.nexuswavetech.nexusplus.features.qrcode

import android.graphics.Bitmap
import android.graphics.Color
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

// ── ViewModel ─────────────────────────────────────────────────────────────────

enum class QrType(val label: String) { TEXT("Text / URL"), UPI("UPI Payment") }

data class QrUiState(
    val qrType: QrType = QrType.TEXT,
    val textInput: String = "",
    val upiId: String = "",
    val upiName: String = "",
    val upiAmount: String = "",
    val upiNote: String = "",
    val qrBitmap: Bitmap? = null,
    val error: String? = null
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
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun QrCodeScreen(
    onBack: () -> Unit,
    viewModel: QrCodeViewModel = koinViewModel()
) {
    val uiState  by viewModel.uiState.collectAsState()
    val view     = LocalView.current
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
            // Type selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics { contentDescription = "QR type selection" }
            ) {
                QrType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.qrType == type,
                        onClick = { viewModel.setQrType(type) },
                        label = { Text(type.label) }
                    )
                }
            }

            when (uiState.qrType) {
                QrType.TEXT -> {
                    OutlinedTextField(
                        value = uiState.textInput,
                        onValueChange = viewModel::setTextInput,
                        label = { Text("Text or URL") },
                        leadingIcon = { Icon(Icons.Filled.QrCode, null) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Text or URL input for QR code" }
                    )
                }
                QrType.UPI -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("UPI Payment Details", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            OutlinedTextField(value = uiState.upiId, onValueChange = viewModel::setUpiId,
                                label = { Text("UPI ID *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("username@bank") })
                            OutlinedTextField(value = uiState.upiName, onValueChange = viewModel::setUpiName,
                                label = { Text("Payee Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = uiState.upiAmount, onValueChange = viewModel::setUpiAmount,
                                label = { Text("Amount (₹, optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = uiState.upiNote, onValueChange = viewModel::setUpiNote,
                                label = { Text("Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            Button(
                onClick = {
                    haptic.confirm(view, touchVib)
                    viewModel.generate()
                    view.announceForAccessibility("Generating QR code")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Generate QR code" }
            ) {
                Icon(Icons.Filled.QrCode, null)
                Spacer(Modifier.width(8.dp))
                Text("Generate QR Code")
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            uiState.qrBitmap?.let { bmp ->
                Card(
                    modifier = Modifier
                        .size(260.dp)
                        .semantics { contentDescription = "Generated QR code" }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Generated QR code",
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    )
                }
                if (uiState.qrType == QrType.UPI && uiState.upiId.isNotBlank()) {
                    Text(
                        "Scan to pay ${uiState.upiName.ifBlank { uiState.upiId }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
