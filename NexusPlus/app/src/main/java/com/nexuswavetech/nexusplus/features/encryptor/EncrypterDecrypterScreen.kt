package com.nexuswavetech.nexusplus.features.encryptor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// ── AES-256 helper ───────────────────────────────────────────────────────────

private const val ALGORITHM = "AES/CBC/PKCS5Padding"
private val FIXED_IV = ByteArray(16) { it.toByte() }

private fun deriveKey(passphrase: String): SecretKeySpec {
    val hash = MessageDigest.getInstance("SHA-256").digest(passphrase.toByteArray())
    return SecretKeySpec(hash, "AES")
}

private fun encryptBytes(data: ByteArray, passphrase: String): ByteArray {
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase), IvParameterSpec(FIXED_IV))
    return cipher.doFinal(data)
}

private fun decryptBytes(data: ByteArray, passphrase: String): ByteArray {
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase), IvParameterSpec(FIXED_IV))
    return cipher.doFinal(data)
}

private fun encryptText(text: String, passphrase: String): String =
    android.util.Base64.encodeToString(encryptBytes(text.toByteArray(), passphrase), android.util.Base64.NO_WRAP)

private fun decryptText(base64: String, passphrase: String): String =
    String(decryptBytes(android.util.Base64.decode(base64, android.util.Base64.NO_WRAP), passphrase))

// ── ViewModel ─────────────────────────────────────────────────────────────────

enum class EncryptMode { TEXT, IMAGE, FILE }

data class EncrypterUiState(
    val mode: EncryptMode = EncryptMode.TEXT,
    val input: String = "",
    val passphrase: String = "",
    val output: String = "",
    val isEncrypting: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFileUri: Uri? = null,
    val selectedFileName: String = ""
)

class EncrypterDecrypterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EncrypterUiState())
    val uiState: StateFlow<EncrypterUiState> = _uiState.asStateFlow()

    fun setMode(mode: EncryptMode) = _uiState.update { it.copy(mode = mode, output = "", error = null) }
    fun setInput(v: String) = _uiState.update { it.copy(input = v) }
    fun setPassphrase(v: String) = _uiState.update { it.copy(passphrase = v) }
    fun toggleEncryptDecrypt() = _uiState.update { it.copy(isEncrypting = !it.isEncrypting, output = "", error = null) }
    fun setFileUri(uri: Uri, name: String) = _uiState.update { it.copy(selectedFileUri = uri, selectedFileName = name, output = "", error = null) }

    fun processText() {
        val s = _uiState.value
        if (s.passphrase.isBlank()) { _uiState.update { it.copy(error = "Passphrase cannot be empty") }; return }
        runCatching {
            val result = if (s.isEncrypting) encryptText(s.input, s.passphrase) else decryptText(s.input, s.passphrase)
            _uiState.update { it.copy(output = result, error = null) }
        }.onFailure { _uiState.update { it.copy(error = "Operation failed: ${it.error}") } }
    }

    fun processFile(context: Context) {
        val s = _uiState.value
        val uri = s.selectedFileUri ?: run { _uiState.update { it.copy(error = "Select a file first") }; return }
        if (s.passphrase.isBlank()) { _uiState.update { it.copy(error = "Passphrase cannot be empty") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: error("Cannot read file")
                    val result = if (s.isEncrypting) encryptBytes(bytes, s.passphrase) else decryptBytes(bytes, s.passphrase)
                    val ext = if (s.isEncrypting) ".enc" else ".dec"
                    val outFile = File(context.cacheDir, s.selectedFileName + ext)
                    outFile.writeBytes(result)
                    _uiState.update { it.copy(isLoading = false, output = "Saved to: ${outFile.absolutePath}", error = null) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Failed: ${e.message}") }
                }
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncrypterDecrypterScreen(
    onBack: () -> Unit,
    viewModel: EncrypterDecrypterViewModel = koinViewModel()
) {
    val uiState  by viewModel.uiState.collectAsState()
    val context  = LocalContext.current
    val view     = LocalView.current
    val haptic   = koinInject<HapticHelper>()
    val settings = koinInject<SettingsRepository>()
    val touchVib by settings.touchVibration.collectAsState(initial = true)

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else "file"
            } ?: "file"
            viewModel.setFileUri(it, name)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Encrypter and Decrypter", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics { contentDescription = "Mode selection. Choose Text, Image, or File." }
            ) {
                EncryptMode.values().forEach { mode ->
                    FilterChip(
                        selected = uiState.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Encrypt / Decrypt toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mode:", style = MaterialTheme.typography.labelLarge)
                FilterChip(
                    selected = uiState.isEncrypting,
                    onClick = { viewModel.toggleEncryptDecrypt() },
                    label = { Text("Encrypt") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null) }
                )
                FilterChip(
                    selected = !uiState.isEncrypting,
                    onClick = { viewModel.toggleEncryptDecrypt() },
                    label = { Text("Decrypt") },
                    leadingIcon = { Icon(Icons.Filled.LockOpen, null) }
                )
            }

            // Passphrase
            OutlinedTextField(
                value = uiState.passphrase,
                onValueChange = viewModel::setPassphrase,
                label = { Text("Passphrase / Secret Key") },
                leadingIcon = { Icon(Icons.Filled.VpnKey, null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Passphrase input. Enter your secret key." }
            )

            when (uiState.mode) {
                EncryptMode.TEXT -> {
                    OutlinedTextField(
                        value = uiState.input,
                        onValueChange = viewModel::setInput,
                        label = { Text(if (uiState.isEncrypting) "Plain text to encrypt" else "Encrypted Base64 to decrypt") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            haptic.confirm(view, touchVib)
                            viewModel.processText()
                            view.announceForAccessibility(if (uiState.isEncrypting) "Encrypting text" else "Decrypting text")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = if (uiState.isEncrypting) "Encrypt text" else "Decrypt text" }
                    ) {
                        Icon(if (uiState.isEncrypting) Icons.Filled.Lock else Icons.Filled.LockOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isEncrypting) "Encrypt" else "Decrypt")
                    }
                }
                EncryptMode.IMAGE, EncryptMode.FILE -> {
                    val mimeType = if (uiState.mode == EncryptMode.IMAGE) "image/*" else "*/*"
                    OutlinedCard(
                        onClick = { fileLauncher.launch(mimeType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Select ${uiState.mode.name.lowercase()} file. Double tap to browse." }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (uiState.mode == EncryptMode.IMAGE) Icons.Filled.Image else Icons.Filled.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (uiState.selectedFileName.isNotBlank()) uiState.selectedFileName else "Tap to select ${uiState.mode.name.lowercase()}",
                                color = if (uiState.selectedFileName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = {
                            haptic.confirm(view, touchVib)
                            viewModel.processFile(context)
                            view.announceForAccessibility("Processing file")
                        },
                        enabled = uiState.selectedFileUri != null && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(if (uiState.isEncrypting) Icons.Filled.Lock else Icons.Filled.LockOpen, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isEncrypting) "Encrypt File" else "Decrypt File")
                        }
                    }
                }
            }

            // Output
            if (uiState.output.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Result: ${uiState.output}" }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Result",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (uiState.mode == EncryptMode.TEXT) {
                                IconButton(onClick = {
                                    haptic.click(view, touchVib)
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("nexus_encrypt", uiState.output))
                                    view.announceForAccessibility("Copied to clipboard")
                                }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy result")
                                }
                            }
                        }
                        Text(
                            text = uiState.output,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            uiState.error?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp).semantics { contentDescription = "Error: $err" }
                    )
                }
            }
        }
    }
}
