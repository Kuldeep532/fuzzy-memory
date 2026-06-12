package com.nexuswavetech.nexusplus.features.encryptor

import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import org.koin.androidx.compose.koinViewModel
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// ── Crypto engine — AES-256 / CBC / PKCS5Padding via Android javax.crypto ────

private const val ALGORITHM   = "AES/CBC/PKCS5Padding"
private const val KEY_FACTORY = "PBKDF2WithHmacSHA256"
private const val SALT        = "NexusWaveTech2025"   // static salt (extend with per-message salt for prod)
private const val ITERATIONS  = 65536
private const val KEY_LENGTH  = 256

private fun deriveKey(password: String): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance(KEY_FACTORY)
    val spec    = PBEKeySpec(password.toCharArray(), SALT.toByteArray(), ITERATIONS, KEY_LENGTH)
    val secret  = factory.generateSecret(spec)
    return SecretKeySpec(secret.encoded, "AES")
}

fun aesEncrypt(plainText: String, password: String): String {
    val key    = deriveKey(password)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv         = cipher.iv
    val encrypted  = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    val combined   = iv + encrypted
    return Base64.encodeToString(combined, Base64.NO_WRAP)
}

fun aesDecrypt(cipherText: String, password: String): String {
    val combined  = Base64.decode(cipherText, Base64.NO_WRAP)
    val iv        = combined.sliceArray(0 until 16)
    val encrypted = combined.sliceArray(16 until combined.size)
    val key       = deriveKey(password)
    val cipher    = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
    return String(cipher.doFinal(encrypted), Charsets.UTF_8)
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class EncryptorUiState(
    val inputText: String = "",
    val password: String = "",
    val outputText: String = "",
    val mode: CryptoMode = CryptoMode.ENCRYPT,
    val error: String? = null,
    val showPassword: Boolean = false
)

enum class CryptoMode { ENCRYPT, DECRYPT }

class TextEncryptorViewModel : ViewModel() {
    var uiState by mutableStateOf(EncryptorUiState())
        private set

    fun onInputChanged(v: String) { uiState = uiState.copy(inputText = v, error = null, outputText = "") }
    fun onPasswordChanged(v: String) { uiState = uiState.copy(password = v, error = null) }
    fun onModeChanged(m: CryptoMode) { uiState = uiState.copy(mode = m, outputText = "", error = null) }
    fun toggleShowPassword() { uiState = uiState.copy(showPassword = !uiState.showPassword) }

    fun process() {
        val input = uiState.inputText.trim()
        val pass  = uiState.password
        if (input.isBlank()) { uiState = uiState.copy(error = "Input text is empty"); return }
        if (pass.isBlank())  { uiState = uiState.copy(error = "Password is required");  return }
        uiState = try {
            val result = if (uiState.mode == CryptoMode.ENCRYPT)
                aesEncrypt(input, pass)
            else
                aesDecrypt(input, pass)
            uiState.copy(outputText = result, error = null)
        } catch (e: Exception) {
            uiState.copy(error = "Failed: ${e.localizedMessage ?: "Wrong password or corrupted data"}", outputText = "")
        }
    }

    fun swapInputOutput() {
        uiState = uiState.copy(
            inputText  = uiState.outputText,
            outputText = "",
            mode = if (uiState.mode == CryptoMode.ENCRYPT) CryptoMode.DECRYPT else CryptoMode.ENCRYPT
        )
    }

    fun clearAll() { uiState = EncryptorUiState() }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun TextEncryptorScreen(onBack: () -> Unit, viewModel: TextEncryptorViewModel = koinViewModel()) {
    val s = viewModel.uiState
    val clipboard = LocalClipboardManager.current
    val view      = LocalView.current

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Text Encryptor / Decryptor", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode toggle
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CryptoMode.values().forEach { mode ->
                    FilterChip(
                        selected = s.mode == mode,
                        onClick  = { viewModel.onModeChanged(mode) },
                        label    = { Text(mode.name.replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "${mode.name} mode${if (s.mode == mode) ". Selected." else ""}" }
                    )
                }
            }

            // Input
            OutlinedTextField(
                value         = s.inputText,
                onValueChange = viewModel::onInputChanged,
                label         = { Text(if (s.mode == CryptoMode.ENCRYPT) "Plain text" else "Encrypted text (Base64)") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .semantics { contentDescription = "Text input. Enter the text you want to ${s.mode.name.lowercase()}." },
                maxLines = 10
            )

            // Password field
            OutlinedTextField(
                value         = s.password,
                onValueChange = viewModel::onPasswordChanged,
                label         = { Text("Encryption password") },
                visualTransformation = if (s.showPassword)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon  = {
                    IconButton(
                        onClick = viewModel::toggleShowPassword,
                        modifier = Modifier.semantics { contentDescription = if (s.showPassword) "Hide password" else "Show password" }
                    ) {
                        Icon(if (s.showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Password field for AES-256 encryption key derivation." },
                singleLine = true
            )

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = {
                        viewModel.process()
                        val verb = if (s.mode == CryptoMode.ENCRYPT) "Encrypting" else "Decrypting"
                        view.announceForAccessibility("$verb text with AES-256")
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Filled.EnhancedEncryption, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (s.mode == CryptoMode.ENCRYPT) "Encrypt" else "Decrypt")
                }
                IconButton(
                    onClick  = viewModel::clearAll,
                    modifier = Modifier.semantics { contentDescription = "Clear all fields" }
                ) { Icon(Icons.Filled.Clear, null) }
            }

            // Error
            if (s.error != null) {
                Text(s.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Output
            AnimatedVisibility(s.outputText.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Text(
                        "Result",
                        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            text     = s.outputText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .semantics { contentDescription = "Result: ${s.outputText}" },
                            style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick  = {
                                clipboard.setText(AnnotatedString(s.outputText))
                                view.announceForAccessibility("Result copied to clipboard")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ContentCopy, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Copy")
                        }
                        OutlinedButton(
                            onClick  = viewModel::swapInputOutput,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.SwapVert, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Swap")
                        }
                    }
                }
            }
        }
    }
}
