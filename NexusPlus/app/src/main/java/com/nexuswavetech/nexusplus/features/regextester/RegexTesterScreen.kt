package com.nexuswavetech.nexusplus.features.regextester

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import org.koin.androidx.compose.koinViewModel

data class RegexMatch(val start: Int, val end: Int, val value: String, val groups: List<String>)

data class RegexUiState(
    val pattern: String = "",
    val testInput: String = "",
    val ignoreCase: Boolean = false,
    val multiline: Boolean = false,
    val matches: List<RegexMatch> = emptyList(),
    val isValid: Boolean? = null,
    val error: String? = null,
    val replacement: String = "",
    val replacedOutput: String = ""
)

class RegexTesterViewModel : ViewModel() {
    var uiState by mutableStateOf(RegexUiState())
        private set

    fun onPatternChanged(v: String)     { uiState = uiState.copy(pattern = v); test() }
    fun onTestInputChanged(v: String)   { uiState = uiState.copy(testInput = v); test() }
    fun onReplacementChanged(v: String) { uiState = uiState.copy(replacement = v); test() }
    fun onIgnoreCaseToggled()           { uiState = uiState.copy(ignoreCase = !uiState.ignoreCase); test() }
    fun onMultilineToggled()            { uiState = uiState.copy(multiline  = !uiState.multiline);  test() }
    fun clearAll()                      { uiState = RegexUiState() }

    private fun test() {
        val pattern = uiState.pattern
        val input   = uiState.testInput
        if (pattern.isBlank()) { uiState = uiState.copy(matches = emptyList(), isValid = null, error = null, replacedOutput = ""); return }

        var flags = 0
        if (uiState.ignoreCase) flags = flags or RegexOption.IGNORE_CASE.ordinal
        if (uiState.multiline)  flags = flags or RegexOption.MULTILINE.ordinal

        val options = mutableSetOf<RegexOption>().apply {
            if (uiState.ignoreCase) add(RegexOption.IGNORE_CASE)
            if (uiState.multiline)  add(RegexOption.MULTILINE)
        }

        uiState = try {
            val regex   = Regex(pattern, options)
            val matches = regex.findAll(input).map { m ->
                RegexMatch(
                    start  = m.range.first,
                    end    = m.range.last + 1,
                    value  = m.value,
                    groups = m.groupValues.drop(1)
                )
            }.toList()
            val replaced = if (uiState.replacement.isNotBlank()) regex.replace(input, uiState.replacement) else ""
            uiState.copy(matches = matches, isValid = true, error = null, replacedOutput = replaced)
        } catch (e: Exception) {
            uiState.copy(matches = emptyList(), isValid = false, error = "Regex error: ${e.localizedMessage}", replacedOutput = "")
        }
    }
}

@Composable
fun RegexTesterScreen(onBack: () -> Unit, viewModel: RegexTesterViewModel = koinViewModel()) {
    val s    = viewModel.uiState
    val view = LocalView.current

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Regex Tester", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Pattern input
            OutlinedTextField(
                value         = s.pattern,
                onValueChange = viewModel::onPatternChanged,
                label         = { Text("Regular expression pattern") },
                placeholder   = { Text("e.g. \\d{3}-\\d{4}") },
                isError       = s.isValid == false,
                supportingText = { if (s.error != null) Text(s.error!!) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Regex pattern input. Matches are highlighted in real time." },
                textStyle     = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                trailingIcon  = {
                    if (s.isValid == true) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                    else if (s.isValid == false) Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                },
                singleLine    = true
            )

            // Flags
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Ignore case flag. ${if (s.ignoreCase) "Enabled" else "Disabled"}." }
                ) {
                    Checkbox(checked = s.ignoreCase, onCheckedChange = { viewModel.onIgnoreCaseToggled() })
                    Text("Ignore case", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Multiline flag. ${if (s.multiline) "Enabled" else "Disabled"}." }
                ) {
                    Checkbox(checked = s.multiline, onCheckedChange = { viewModel.onMultilineToggled() })
                    Text("Multiline", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = viewModel::clearAll, modifier = Modifier.semantics { contentDescription = "Clear all fields" }) {
                    Icon(Icons.Filled.Clear, null)
                }
            }

            // Test input
            OutlinedTextField(
                value         = s.testInput,
                onValueChange = viewModel::onTestInputChanged,
                label         = { Text("Test input string") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .semantics { contentDescription = "Test string input. Regex is applied as you type." },
                maxLines      = 8
            )

            // Replacement
            OutlinedTextField(
                value         = s.replacement,
                onValueChange = viewModel::onReplacementChanged,
                label         = { Text("Replacement string (optional)") },
                modifier      = Modifier.fillMaxWidth().semantics { contentDescription = "Replacement string for regex replace. Leave empty to skip." },
                singleLine    = true
            )

            // Match summary
            if (s.pattern.isNotBlank() && s.isValid == true) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text     = "${s.matches.size} match${if (s.matches.size != 1) "es" else ""} found",
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .semantics { contentDescription = "${s.matches.size} matches found." },
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Matches list
            AnimatedVisibility(s.matches.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider()
                    Text("Matches", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
                    s.matches.forEachIndexed { i, match ->
                        Card(
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "Match ${i + 1}: \"${match.value}\" at position ${match.start} to ${match.end}"
                            }
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("#${i + 1}  \"${match.value}\"", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                                Text("pos ${match.start}–${match.end}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                match.groups.forEachIndexed { gi, g ->
                                    if (g.isNotBlank()) Text("Group ${gi + 1}: \"$g\"", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Replacement output
            AnimatedVisibility(s.replacedOutput.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider()
                    Text("Replaced Output", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(s.replacedOutput, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    }
                }
            }
        }
    }
}
