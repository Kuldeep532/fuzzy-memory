package com.nexuswavetech.nexusplus.features.numbersys

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

enum class NumberBase(val label: String, val radix: Int, val hint: String) {
    DECIMAL    ("Decimal (Base 10)",     10, "e.g. 255"),
    BINARY     ("Binary (Base 2)",        2, "e.g. 11111111"),
    OCTAL      ("Octal (Base 8)",         8, "e.g. 377"),
    HEXADECIMAL("Hexadecimal (Base 16)", 16, "e.g. FF")
}

data class ConversionResult(val base: NumberBase, val value: String)

class NumberSystemViewModel : ViewModel() {
    var input      by mutableStateOf("")
        private set
    var sourceBase by mutableStateOf(NumberBase.DECIMAL)
        private set
    var results    by mutableStateOf<List<ConversionResult>>(emptyList())
        private set
    var error      by mutableStateOf<String?>(null)
        private set

    fun onInputChanged(v: String)      { input = v.uppercase(); convert() }
    fun onSourceBaseChanged(b: NumberBase) { sourceBase = b; input = ""; results = emptyList(); error = null }
    fun clearAll()                     { input = ""; results = emptyList(); error = null }

    private fun convert() {
        error   = null
        results = emptyList()
        if (input.isBlank()) return
        val decimal = try {
            input.toLong(sourceBase.radix)
        } catch (e: NumberFormatException) {
            error = "Invalid ${sourceBase.label} number"
            return
        }
        results = NumberBase.values()
            .filter { it != sourceBase }
            .map { base -> ConversionResult(base, decimal.toString(base.radix).uppercase()) }
    }
}

@Composable
fun NumberSystemScreen(onBack: () -> Unit, viewModel: NumberSystemViewModel = koinViewModel()) {
    val clipboard = LocalClipboardManager.current
    val view      = LocalView.current

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Number System Converter", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source base selector
            Text("Input Base", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.semantics { heading() })
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NumberBase.values().forEach { base ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${base.label}${if (viewModel.sourceBase == base) ". Selected." else ""}"
                            },
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.sourceBase == base,
                            onClick  = { viewModel.onSourceBaseChanged(base) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(base.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            OutlinedTextField(
                value         = viewModel.input,
                onValueChange = viewModel::onInputChanged,
                label         = { Text("Enter ${viewModel.sourceBase.label} number") },
                placeholder   = { Text(viewModel.sourceBase.hint) },
                isError       = viewModel.error != null,
                supportingText = { if (viewModel.error != null) Text(viewModel.error!!) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Number input in ${viewModel.sourceBase.label}. Conversions appear below automatically." },
                trailingIcon  = {
                    if (viewModel.input.isNotBlank()) {
                        IconButton(onClick = viewModel::clearAll) { Icon(Icons.Filled.Clear, "Clear") }
                    }
                },
                singleLine = true
            )

            if (viewModel.results.isNotEmpty()) {
                HorizontalDivider()
                Text("Conversions", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
                viewModel.results.forEach { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "${result.base.label}: ${result.value}. Double tap copy button to copy." },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(result.base.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(result.value, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace))
                            }
                            IconButton(
                                onClick  = {
                                    clipboard.setText(AnnotatedString(result.value))
                                    view.announceForAccessibility("${result.base.label} value copied")
                                },
                                modifier = Modifier.semantics { contentDescription = "Copy ${result.base.label} value" }
                            ) { Icon(Icons.Filled.ContentCopy, null) }
                        }
                    }
                }
            }
        }
    }
}
