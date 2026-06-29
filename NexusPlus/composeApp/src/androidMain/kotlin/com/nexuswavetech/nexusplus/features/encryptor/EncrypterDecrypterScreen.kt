@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// AES-256-CBC with PBKDF2-HMAC-SHA256 key derivation

private const val ALGORITHM  = "AES/CBC/PKCS5Padding"
private const val IV_SIZE    = 16
private const val SALT_SIZE  = 16
private const val ITERATIONS = 10_000
private const val KEY_BITS   = 256

private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec    = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
    return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
}

private fun encryptBytes(data: ByteArray, password: String): ByteArray {
    val salt   = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
    val iv     = ByteArray(IV_SIZE).also   { SecureRandom().nextBytes(it) }
    val key    = deriveKey(password, salt)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
    return salt + iv + cipher.doFinal(data)
}

private fun decryptBytes(data: ByteArray, password: String): ByteArray {
    require(data.size >= SALT_SIZE + IV_SIZE) { "Data is too short or corrupt." }
    val salt    = data.copyOfRange(0, SALT_SIZE)
    val iv      = data.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
    val payload = data.copyOfRange(SALT_SIZE + IV_SIZE, data.size)
    val key     = deriveKey(password, salt)
    val cipher  = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
    return cipher.doFinal(payload)
}

private fun encryptText(text: String, password: String): String =
    java.util.Base64.getEncoder().encodeToString(encryptBytes(text.toByteArray(Charsets.UTF_8), password))

private fun decryptText(base64: String, password: String): String =
    String(decryptBytes(java.util.Base64.getDecoder().decode(base64), password), Charsets.UTF_8)

@Composable
fun EncrypterDecrypterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Text & File Encryptor", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Text" to Icons.Filled.TextFields, "File" to Icons.Filled.InsertDriveFile)
                .forEachIndexed { index, (label, icon) ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        label    = { Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "$label mode. ${if (selectedTab == index) "Selected." else "Tap to switch."}" },
                    )
                }
        }

        when (selectedTab) {
            0 -> TextTab(context = context)
            1 -> FileTab(context = context)
        }
    }
}

@Composable
private fun TextTab(context: Context) {
    var input        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isEncrypting by remember { mutableStateOf(true) }
    var output       by remember { mutableStateOf("") }
    var error        by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModeToggle(
            isEncrypting = isEncrypting,
            onToggle     = { isEncrypting = it; output = ""; error = null }
        )

        OutlinedTextField(
            value         = input,
            onValueChange = { input = it; error = null },
            label         = { Text(if (isEncrypting) "Text to encrypt" else "Encrypted text") },
            minLines      = 4,
            maxLines      = 8,
            modifier      = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isEncrypting) "Enter text to encrypt" else "Paste encrypted text here"
                }
        )

        PasswordField(
            password     = password,
            showPassword = showPassword,
            onPasswordChange    = { password = it; error = null },
            onToggleVisibility  = { showPassword = !showPassword }
        )

        ErrorBanner(message = error)

        Button(
            onClick = {
                error = null
                when {
                    input.isBlank()    -> error = "Please enter some text."
                    password.isBlank() -> error = "Please enter a password."
                    else -> runCatching {
                        output = if (isEncrypting) encryptText(input, password)
                                 else              decryptText(input, password)
                    }.onFailure {
                        output = ""
                        error  = if (isEncrypting) "Encryption failed. Please try again."
                                 else "Decryption failed. Make sure the text and password are correct."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = if (isEncrypting) "Encrypt text" else "Decrypt text"
            }
        ) {
            Icon(
                if (isEncrypting) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isEncrypting) "Encrypt" else "Decrypt")
        }

        AnimatedVisibility(visible = output.isNotBlank()) {
            ResultCard(text = output, context = context)
        }
    }
}

@Composable
private fun FileTab(context: Context) {
    val scope = rememberCoroutineScope()
    var isEncrypting  by remember { mutableStateOf(true) }
    var password      by remember { mutableStateOf("") }
    var showPassword  by remember { mutableStateOf(false) }
    var selectedUri   by remember { mutableStateOf<Uri?>(null) }
    var selectedName  by remember { mutableStateOf("") }
    var isProcessing  by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError       by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else "file"
            } ?: "file"
            selectedUri   = uri
            selectedName  = name
            statusMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModeToggle(
            isEncrypting = isEncrypting,
            onToggle     = { isEncrypting = it; statusMessage = null },
            encryptLabel = "Encrypt file",
            decryptLabel = "Decrypt file"
        )

        OutlinedCard(
            onClick  = { fileLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Tap to select a file" }
        ) {
            Row(
                modifier              = Modifier.padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    if (selectedUri == null) {
                        Text("Select a file", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(selectedName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                    }
                }
                if (selectedUri != null) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "File selected", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        PasswordField(
            password            = password,
            showPassword        = showPassword,
            onPasswordChange    = { password = it },
            onToggleVisibility  = { showPassword = !showPassword }
        )

        statusMessage?.let { msg ->
            Surface(
                shape    = MaterialTheme.shapes.small,
                color    = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                        contentDescription = if (isError) "Error" else "Success",
                        tint     = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Button(
            onClick = {
                when {
                    selectedUri == null -> { statusMessage = "Please select a file first."; isError = true }
                    password.isBlank() -> { statusMessage = "Please enter a password.";    isError = true }
                    else -> scope.launch {
                        isProcessing  = true
                        statusMessage = null
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val bytes = context.contentResolver.openInputStream(selectedUri!!)
                                    ?.readBytes() ?: error("Cannot read the selected file.")
                                val result = if (isEncrypting) encryptBytes(bytes, password)
                                             else              decryptBytes(bytes, password)
                                val outName = if (!isEncrypting && selectedName.endsWith(".enc")) {
                                    selectedName.removeSuffix(".enc")
                                } else {
                                    "$selectedName.enc"
                                }
                                val outFile = File(context.cacheDir, outName)
                                outFile.writeBytes(result)
                                withContext(Dispatchers.Main) {
                                    isError       = false
                                    statusMessage = if (isEncrypting)
                                        "File encrypted. Saved as: ${outFile.name}"
                                    else
                                        "File decrypted. Saved as: ${outFile.name}"
                                }
                            }.onFailure {
                                withContext(Dispatchers.Main) {
                                    isError       = true
                                    statusMessage = if (isEncrypting)
                                        "Encryption failed. Please try again."
                                    else
                                        "Decryption failed. Make sure the file and password are correct."
                                }
                            }
                        }
                        isProcessing = false
                    }
                }
            },
            enabled  = !isProcessing,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = if (isEncrypting) "Encrypt selected file" else "Decrypt selected file"
            }
        ) {
            if (isProcessing) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Processing...")
            } else {
                Icon(
                    if (isEncrypting) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isEncrypting) "Encrypt File" else "Decrypt File")
            }
        }
    }
}

@Composable
private fun ModeToggle(
    isEncrypting  : Boolean,
    onToggle      : (Boolean) -> Unit,
    encryptLabel  : String = "Encrypt",
    decryptLabel  : String = "Decrypt",
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected    = isEncrypting,
            onClick     = { onToggle(true) },
            label       = { Text(encryptLabel) },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, Modifier.size(18.dp)) },
            modifier    = Modifier.weight(1f).semantics { contentDescription = "Switch to $encryptLabel mode" }
        )
        FilterChip(
            selected    = !isEncrypting,
            onClick     = { onToggle(false) },
            label       = { Text(decryptLabel) },
            leadingIcon = { Icon(Icons.Filled.LockOpen, contentDescription = null, Modifier.size(18.dp)) },
            modifier    = Modifier.weight(1f).semantics { contentDescription = "Switch to $decryptLabel mode" }
        )
    }
}

@Composable
private fun PasswordField(
    password           : String,
    showPassword       : Boolean,
    onPasswordChange   : (String) -> Unit,
    onToggleVisibility : () -> Unit,
) {
    OutlinedTextField(
        value                = password,
        onValueChange        = onPasswordChange,
        label                = { Text("Password") },
        leadingIcon          = { Icon(Icons.Filled.Key, contentDescription = null) },
        trailingIcon         = {
            IconButton(
                onClick  = onToggleVisibility,
                modifier = Modifier.semantics { contentDescription = if (showPassword) "Hide password" else "Show password" }
            ) {
                Icon(
                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null
                )
            }
        },
        singleLine           = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        modifier             = Modifier.fillMaxWidth().semantics { contentDescription = "Encryption password field" }
    )
}

@Composable
private fun ErrorBanner(message: String?) {
    AnimatedVisibility(visible = message != null) {
        if (message != null) {
            Surface(
                shape    = MaterialTheme.shapes.small,
                color    = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
private fun ResultCard(text: String, context: Context) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Result",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick  = {
                        val cb = context.getSystemService(ClipboardManager::class.java)
                        cb?.setPrimaryClip(ClipData.newPlainText("Encrypted text", text))
                    },
                    modifier = Modifier.semantics { contentDescription = "Copy result to clipboard" }
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text     = text,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { contentDescription = "Encryption result text" }
            )
        }
    }
}
