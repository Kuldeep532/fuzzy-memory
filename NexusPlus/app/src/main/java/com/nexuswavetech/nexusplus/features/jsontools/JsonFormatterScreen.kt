package com.nexuswavetech.nexusplus.features.jsontools

import androidx.compose.animation.*
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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel

enum class JsonAction { FORMAT, MINIFY, VALIDATE }

data class JsonUiState(
    val input: String = "",
    val output: String = "",
    val status: String = "",
    val isValid: Boolean? = null,
    val error: String? = null
)

class JsonFormatterViewModel : ViewModel() {
    var uiState by mutableStateOf(JsonUiState())
        private set

    fun onInputChanged(v: String) { uiState = uiState.copy(input = v, output = "", isValid = null, error = null, status = "") }

    fun process(action: JsonAction) {
        val raw = uiState.input.trim()
        if (raw.isBlank()) { uiState = uiState.copy(error = "Input is empty"); return }
        uiState = try {
            val parsed: Any = try { JSONObject(raw) } catch (e: JSONException) { JSONArray(raw) }
            val formatted = (parsed as? JSONObject)?.toString(2) ?: (parsed as JSONArray).toString(2)
            when (action) {
                JsonAction.FORMAT   -> uiState.copy(output = formatted, status = "Valid JSON — formatted", isValid = true, error = null)
                JsonAction.MINIFY   -> {
                    val min = (parsed as? JSONObject)?.toString() ?: (parsed as JSONArray).toString()
                    uiState.copy(output = min, status = "Valid JSON — minified (${min.length} chars)", isValid = true, error = null)
                }
                JsonAction.VALIDATE -> uiState.copy(output = "", status = "Valid JSON ✓", isValid = true, error = null)
            }
        } catch (e: JSONException) {
            uiState.copy(
                output  = "",
                isValid = false,
                error   = "Invalid JSON: ${e.message}",
                status  = ""
            )
        }
    }

    fun clearAll() { uiState = JsonUiState() }
}

@Composable
fun JsonFormatterScreen(onBack: () -> Unit, viewModel: JsonFormatterViewModel = koinViewModel()) {
    val s         = viewModel.uiState
    val clipboard = LocalClipboardManager.current
    val view      = LocalView.current

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "JSON Formatter", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value         = s.input,
                onValueChange = viewModel::onInputChanged,
                label         = { Text("Paste JSON here…") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .semantics { contentDescription = "JSON input field. Paste your JSON to format, minify, or validate." },
                maxLines      = 20,
                textStyle     = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )

            // Action buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Format",   JsonAction.FORMAT,   Icons.Filled.FormatAlignLeft),
                    Triple("Minify",   JsonAction.MINIFY,   Icons.Filled.Compress),
                    Triple("Validate", JsonAction.VALIDATE, Icons.Filled.CheckCircle)
                ).forEach { (label, action, icon) ->
                    FilledTonalButton(
                        onClick  = {
                            viewModel.process(action)
                            view.announceForAccessibility("$label JSON")
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(icon, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
                IconButton(onClick = viewModel::clearAll) { Icon(Icons.Filled.Clear, "Clear") }
            }

            // Status badge
            if (s.isValid != null || s.error != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (s.isValid) {
                        true  -> MaterialTheme.colorScheme.secondaryContainer
                        false -> MaterialTheme.colorScheme.errorContainer
                        null  -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text     = s.error ?: s.status,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .semantics { contentDescription = s.error ?: s.status },
                        style    = MaterialTheme.typography.bodySmall,
                        color    = when (s.isValid) {
                            true  -> MaterialTheme.colorScheme.onSecondaryContainer
                            false -> MaterialTheme.colorScheme.onErrorContainer
                            null  -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            AnimatedVisibility(s.output.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Text("Output", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            s.output,
                            Modifier.fillMaxWidth().padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                    OutlinedButton(
                        onClick  = {
                            clipboard.setText(AnnotatedString(s.output))
                            view.announceForAccessibility("JSON output copied to clipboard")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("Copy Output") }
                }
            }
        }
    }
}
