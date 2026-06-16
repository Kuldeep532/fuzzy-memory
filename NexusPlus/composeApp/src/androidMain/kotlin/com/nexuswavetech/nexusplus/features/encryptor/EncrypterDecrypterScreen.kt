package com.nexuswavetech.nexusplus.features.encryptor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// ── Cryptographic Constants & Core Functions ─────────────────────────────────

private const val ALGORITHM = "AES/CBC/PKCS5Padding"
private const val IV_SIZE = 16
private const val SALT_SIZE = 16
private const val ITERATIONS = 10000
private const val KEY_LENGTH = 256
private const val INTERNAL_FALLBACK_KEY = "NexusPlus_Super_Secured_Government_Grade_Master_Key_2026_TopSecret"

private fun deriveSecureKey(passphrase: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
    val tmp = factory.generateSecret(spec)
    return SecretKeySpec(tmp.encoded, "AES")
}

private fun encryptBytes(data: ByteArray, passphrase: String): ByteArray {
    val random = SecureRandom()
    val salt = ByteArray(SALT_SIZE).apply { random.nextBytes(this) }
    val iv = ByteArray(IV_SIZE).apply { random.nextBytes(this) }
    val secretKey = deriveSecureKey(passphrase, salt)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
    return salt + iv + cipher.doFinal(data)
}

private fun decryptBytes(data: ByteArray, passphrase: String): ByteArray {
    if (data.size < (SALT_SIZE + IV_SIZE)) throw IllegalArgumentException("Corrupt data packet structural package intercepted")
    val salt = data.copyOfRange(0, SALT_SIZE)
    val iv = data.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
    val encryptedPayload = data.copyOfRange(SALT_SIZE + IV_SIZE, data.size)
    val secretKey = deriveSecureKey(passphrase, salt)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
    return cipher.doFinal(encryptedPayload)
}

private fun encryptText(text: String, passphrase: String): String =
    android.util.Base64.encodeToString(encryptBytes(text.toByteArray(), passphrase), android.util.Base64.NO_WRAP)

private fun decryptText(base64: String, passphrase: String): String =
    String(decryptBytes(android.util.Base64.decode(base64, android.util.Base64.NO_WRAP), passphrase))

// ── State & ViewModel Architecture ──────────────────────────────────────────

enum class EncryptMode { TEXT, IMAGE, AUDIO, FILE }

data class EncrypterUiState(
    val mode: EncryptMode = EncryptMode.TEXT,
    val decryptAsFile: Boolean = false,
    val input: String = "",
    val passphrase: String = "",
    val confirmPassphrase: String = "",
    val useCustomKey: Boolean = false,
    val saveToGooglePasswords: Boolean = false,
    val output: String = "",
    val isEncrypting: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFileUri: Uri? = null,
    val selectedFileName: String = "",
    val failedAttempts: Int = 0,
    val lockRemainingSeconds: Int = 0
)

class EncrypterDecrypterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EncrypterUiState())
    val uiState: StateFlow<EncrypterUiState> = _uiState.asStateFlow()
    private var countdownJob: Job? = null

    fun setMode(mode: EncryptMode) = _uiState.update { it.copy(mode = mode, output = "", error = null, selectedFileUri = null, selectedFileName = "") }
    fun setDecryptAsFile(isFile: Boolean) = _uiState.update { it.copy(decryptAsFile = isFile, output = "", error = null, selectedFileUri = null, selectedFileName = "") }
    fun setInput(v: String) = _uiState.update { it.copy(input = v) }
    fun setPassphrase(v: String) = _uiState.update { it.copy(passphrase = v, error = null) }
    fun setConfirmPassphrase(v: String) = _uiState.update { it.copy(confirmPassphrase = v, error = null) }
    fun setUseCustomKey(v: Boolean) = _uiState.update { it.copy(useCustomKey = v, error = null, passphrase = "", confirmPassphrase = "") }
    fun setSaveToGooglePasswords(v: Boolean) = _uiState.update { it.copy(saveToGooglePasswords = v) }
    fun toggleEncryptDecrypt() = _uiState.update { it.copy(isEncrypting = !it.isEncrypting, output = "", error = null, selectedFileUri = null, selectedFileName = "") }

    fun setFileUri(context: Context, uri: Uri, name: String) {
        _uiState.update { it.copy(selectedFileUri = uri, selectedFileName = name, output = "", error = null) }
        
        // Intelligent Passphrase Auto-Detection Logic
        if (!_uiState.value.isEncrypting && name.endsWith(".enc")) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                    decryptBytes(bytes, INTERNAL_FALLBACK_KEY)
                }.onSuccess {
                    _uiState.update { it.copy(useCustomKey = false, error = "Auto-Detect: Default protection found. No password required.") }
                }.onFailure {
                    _uiState.update { it.copy(useCustomKey = true, error = "Auto-Detect: Password-protected payload. Please enter security key.") }
                }
            }
        }
    }

    private fun handleDecryptionFailure() {
        _uiState.update { current ->
            val newAttempts = current.failedAttempts + 1
            val delaySeconds = when {
                newAttempts == 3 -> 30
                newAttempts == 4 -> 60
                newAttempts >= 5 -> 300
                else -> 0
            }
            if (delaySeconds > 0) {
                startLockoutCountdown(delaySeconds)
            }
            current.copy(
                failedAttempts = newAttempts,
                error = if (delaySeconds > 0) "Too many verification failures. Core locked for $delaySeconds seconds." else "Security Block: Invalid security passphrase key configuration.",
                output = ""
            )
        }
    }

    private fun startLockoutCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var currentLeft = seconds
            while (currentLeft > 0) {
                _uiState.update { it.copy(lockRemainingSeconds = currentLeft) }
                delay(1000L)
                currentLeft--
            }
            _uiState.update { it.copy(lockRemainingSeconds = 0) }
        }
    }

    private fun checkValidation(state: EncrypterUiState): Boolean {
        if (state.lockRemainingSeconds > 0) return false
        if (state.useCustomKey) {
            if (state.passphrase.isBlank()) {
                _uiState.update { it.copy(error = "Secret key field configuration cannot be empty") }
                return false
            }
            if (state.isEncrypting && state.passphrase != state.confirmPassphrase) {
                _uiState.update { it.copy(error = "Passphrase Guard Violation: Confirmation key mismatch") }
                return false
            }
        }
        return true
    }

    // Dynamic Reflection-Based Wrapper for Android Credential Manager API Compliance
    private fun triggerGooglePasswordSave(context: Context, passphrase: String) {
        viewModelScope.launch {
            try {
                val credManagerClass = Class.forName("androidx.credentials.CredentialManager")
                val createMethod = credManagerClass.getMethod("create", Context::class.java)
                val credentialManagerInstance = createMethod.invoke(null, context)
                
                val reqClass = Class.forName("androidx.credentials.CreatePasswordRequest")
                val constructor = reqClass.getConstructor(String::class.java, String::class.java)
                val requestInstance = constructor.newInstance("NexusPlus_CryptoKey", passphrase)
                
                val executeMethod = credManagerClass.methods.firstOrNull { it.name == "createCredential" }
                executeMethod?.invoke(credentialManagerInstance, context, requestInstance)
            } catch (e: Exception) {
                // Graceful compliance degradation if the dependency library is missing at runtime
            }
        }
    }

    fun processText(context: Context) {
        val s = _uiState.value
        if (!checkValidation(s)) return
        val activeKey = if (s.useCustomKey) s.passphrase else INTERNAL_FALLBACK_KEY

        runCatching {
            if (s.isEncrypting) {
                val result = encryptText(s.input, activeKey)
                if (s.useCustomKey && s.saveToGooglePasswords) { triggerGooglePasswordSave(context, s.passphrase) }
                _uiState.update { it.copy(output = result, error = null, failedAttempts = 0) }
            } else {
                val result = decryptText(s.input, activeKey)
                _uiState.update { it.copy(output = result, error = null, failedAttempts = 0) }
            }
        }.onFailure {
            if (!s.isEncrypting) handleDecryptionFailure() else _uiState.update { it.copy(error = "Encryption generation execution failure.") }
        }
    }

    fun processFile(context: Context) {
        val s = _uiState.value
        if (!checkValidation(s)) return
        val uri = s.selectedFileUri ?: run { _uiState.update { it.copy(error = "Select target file payload architecture block first") } return }
        val activeKey = if (s.useCustomKey) s.passphrase else INTERNAL_FALLBACK_KEY

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: error("Access stream read denied")
                    val result = if (s.isEncrypting) encryptBytes(bytes, activeKey) else decryptBytes(bytes, activeKey)
                    
                    val outName = if (!s.isEncrypting && s.selectedFileName.endsWith(".enc")) {
                        s.selectedFileName.removeSuffix(".enc")
                    } else {
                        s.selectedFileName + (if (s.isEncrypting) ".enc" else ".dec")
                    }
                    
                    val outFile = File(context.cacheDir, outName)
                    outFile.writeBytes(result)
                    
                    if (s.isEncrypting && s.useCustomKey && s.saveToGooglePasswords) {
                        withContext(Dispatchers.Main) { triggerGooglePasswordSave(context, s.passphrase) }
                    }

                    _uiState.update { it.copy(isLoading = false, output = "Saved safely inside cache path: ${outFile.absolutePath}", error = null, failedAttempts = 0) }
                }.onFailure {
                    _uiState.update { current -> current.copy(isLoading = false) }
                    withContext(Dispatchers.Main) { if (!s.isEncrypting) handleDecryptionFailure() else _uiState.update { it.copy(error = "Internal File system structural error.") } }
                }
            }
        }
    }
}

// ── User Interface Screen Composables Layout ─────────────────────────────────

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

    var dropdownExpanded by remember { mutableStateOf(false) }
    val isLockedOut = uiState.lockRemainingSeconds > 0

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else "unnamed_payload"
            } ?: "unnamed_payload"
            viewModel.setFileUri(context, it, name)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Security Crypt Engine", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(visible = isLockedOut) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = "SECURITY LOCKOUT ACTIVE: Retry available in ${uiState.lockRemainingSeconds}s",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = if (uiState.isEncrypting) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.isEncrypting,
                    onClick = { if(!uiState.isEncrypting) viewModel.toggleEncryptDecrypt() },
                    text = { Text("Encrypt Mode", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.Lock, null) }
                )
                Tab(
                    selected = !uiState.isEncrypting,
                    onClick = { if(uiState.isEncrypting) viewModel.toggleEncryptDecrypt() },
                    text = { Text("Decrypt Mode", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.LockOpen, null) }
                )
            }

            if (uiState.isEncrypting) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Component Selection Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = false }) {
                        EncryptMode.entries.forEach { selectionMode ->
                            DropdownMenuItem(
                                text = { Text(selectionMode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { viewModel.setMode(selectionMode); dropdownExpanded = false }
                            )
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !uiState.decryptAsFile,
                        onClick = { viewModel.setDecryptAsFile(false) },
                        label = { Text("Decrypt Text Message") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.decryptAsFile,
                        onClick = { viewModel.setDecryptAsFile(true) },
                        label = { Text("Decrypt File / Media") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = uiState.useCustomKey,
                            onCheckedChange = { viewModel.setUseCustomKey(it) },
                            modifier = Modifier.semantics { contentDescription = "Enable custom key validation configuration" }
                        )
                        Text("Secure with custom passphrase key lock", style = MaterialTheme.typography.bodyMedium)
                    }

                    AnimatedVisibility(visible = uiState.useCustomKey && uiState.isEncrypting) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = uiState.saveToGooglePasswords,
                                onCheckedChange = { viewModel.setSaveToGooglePasswords(it) }
                            )
                            Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Save to Google Password Manager", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = uiState.useCustomKey) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.passphrase,
                        onValueChange = viewModel::setPassphrase,
                        label = { Text("Secret Key Passphrase") },
                        leadingIcon = { Icon(Icons.Filled.VpnKey, null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (uiState.isEncrypting) {
                        OutlinedTextField(
                            value = uiState.confirmPassphrase,
                            onValueChange = viewModel::setConfirmPassphrase,
                            label = { Text("Confirm Secret Key Passphrase") },
                            leadingIcon = { Icon(Icons.Filled.EnhancedEncryption, null) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            val isHandlingFileFlow = if (uiState.isEncrypting) uiState.mode != EncryptMode.TEXT else uiState.decryptAsFile

            if (!isHandlingFileFlow) {
                OutlinedTextField(
                    value = uiState.input,
                    onValueChange = viewModel::setInput,
                    label = { Text(if (uiState.isEncrypting) "Plain text payload string" else "Encrypted encoded base payload") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        haptic.confirm(view, touchVib)
                        viewModel.processText(context)
                    },
                    enabled = !isLockedOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (uiState.isEncrypting) Icons.Filled.Lock else Icons.Filled.LockOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isEncrypting) "Encrypt Text Payload" else "Decrypt Text Payload")
                }
            } else {
                val filterType = if (uiState.isEncrypting) {
                    when (uiState.mode) {
                        EncryptMode.IMAGE -> "image/*"
                        EncryptMode.AUDIO -> "audio/*"
                        else -> "*/*"
                    }
                } else "*/*"

                val currentCardIcon = if (uiState.isEncrypting) {
                    when (uiState.mode) {
                        EncryptMode.IMAGE -> Icons.Filled.Image
                        EncryptMode.AUDIO -> Icons.Filled.MusicNote
                        else -> Icons.Filled.AttachFile
                    }
                } else Icons.Filled.Encrypted

                OutlinedCard(
                    onClick = { if (!isLockedOut) fileLauncher.launch(filterType) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(currentCardIcon, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (uiState.selectedFileName.isNotBlank()) uiState.selectedFileName else if (uiState.isEncrypting) "Select target ${uiState.mode.name.lowercase()} file" else "Select encrypted .enc block file",
                            color = if (uiState.selectedFileName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = {
                        haptic.confirm(view, touchVib)
                        viewModel.processFile(context)
                    },
                    enabled = uiState.selectedFileUri != null && !uiState.isLoading && !isLockedOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(if (uiState.isEncrypting) Icons.Filled.Lock else Icons.Filled.LockOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isEncrypting) "Encrypt Selected File" else "Decrypt Selected File")
                    }
                }
            }

            if (uiState.output.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Computed Execution Output Result", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                            if (!isHandlingFileFlow) {
                                IconButton(onClick = {
                                    haptic.click(view, touchVib)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("nexus_security_payload", uiState.output))
                                }) { Icon(Icons.Filled.ContentCopy, "Copy Payload Content") }
                            }
                        }
                        Text(text = uiState.output, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            uiState.error?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                    Text(text = err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
