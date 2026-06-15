package com.nexuswavetech.nexusplus.features.hashgen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.security.MessageDigest

enum class HashAlgorithm(val label: String, val jvmName: String) {
    MD5("MD5", "MD5"),
    SHA1("SHA-1", "SHA-1"),
    SHA256("SHA-256", "SHA-256"),
    SHA512("SHA-512", "SHA-512")
}

data class HashResult(val algorithm: HashAlgorithm, val hash: String)

class HashGeneratorViewModel : ViewModel() {
    var input by mutableStateOf("")
        private set
    var results by mutableStateOf<List<HashResult>>(emptyList())
        private set

    fun onInputChanged(v: String) {
        input = v
        results = if (v.isBlank()) emptyList()
        else HashAlgorithm.entries.map { algo ->
            val digest = MessageDigest.getInstance(algo.jvmName)
            val bytes  = digest.digest(v.toByteArray(Charsets.UTF_8))
            HashResult(algo, bytes.joinToString("") { "%02x".format(it) })
        }
    }

    fun clearAll() { input = ""; results = emptyList() }
}

@Composable
fun HashGeneratorScreen(onBack: () -> Unit, viewModel: HashGeneratorViewModel = koinViewModel()) {
    val clipboard = LocalClipboardManager.current
    val view      = LocalView.current

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Hash Generator", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Generate MD5, SHA-1, SHA-256 and SHA-512 hashes instantly — 100% on-device, no internet required.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value         = viewModel.input,
                onValueChange = viewModel::onInputChanged,
                label         = { Text("Enter text to hash…") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .semantics { contentDescription = "Text input. Hashes are generated automatically as you type." },
                trailingIcon  = {
                    if (viewModel.input.isNotBlank()) {
                        IconButton(onClick = viewModel::clearAll) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear input")
                        }
                    }
                },
                maxLines = 6
            )

            if (viewModel.results.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Hash Results",
                    style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )

                viewModel.results.forEach { result ->
                    HashResultCard(result = result, onCopy = {
                        clipboard.setText(AnnotatedString(result.hash))
                        view.announceForAccessibility("${result.algorithm.label} hash copied to clipboard")
                    })
                }
            }
        }
    }
}

@Composable
private fun HashResultCard(result: HashResult, onCopy: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${result.algorithm.label} hash: ${result.hash}. Double tap copy button to copy."
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    result.algorithm.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick  = onCopy,
                    modifier = Modifier
                        .size(28.dp)
                        .semantics { contentDescription = "Copy ${result.algorithm.label} hash" }
                ) {
                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp))
                }
            }
            Text(
                text  = result.hash,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
