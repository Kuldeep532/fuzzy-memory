package com.nexuswavetech.nexusplus.features.translator

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.androidx.compose.koinViewModel

// ── Supported language pairs (on-device MLKit — no API key required) ──────────

data class NexusLanguage(val code: String, val displayName: String)

val SUPPORTED_LANGUAGES = listOf(
    NexusLanguage(TranslateLanguage.ENGLISH,    "English"),
    NexusLanguage(TranslateLanguage.HINDI,      "Hindi"),
    NexusLanguage(TranslateLanguage.ARABIC,     "Arabic"),
    NexusLanguage(TranslateLanguage.SPANISH,    "Spanish"),
    NexusLanguage(TranslateLanguage.FRENCH,     "French"),
    NexusLanguage(TranslateLanguage.GERMAN,     "German"),
    NexusLanguage(TranslateLanguage.PORTUGUESE, "Portuguese"),
    NexusLanguage(TranslateLanguage.RUSSIAN,    "Russian"),
    NexusLanguage(TranslateLanguage.JAPANESE,   "Japanese"),
    NexusLanguage(TranslateLanguage.CHINESE,    "Chinese"),
    NexusLanguage(TranslateLanguage.KOREAN,     "Korean"),
    NexusLanguage(TranslateLanguage.ITALIAN,    "Italian"),
    NexusLanguage(TranslateLanguage.DUTCH,      "Dutch"),
    NexusLanguage(TranslateLanguage.TURKISH,    "Turkish"),
    NexusLanguage(TranslateLanguage.POLISH,     "Polish"),
    NexusLanguage(TranslateLanguage.SWEDISH,    "Swedish"),
    NexusLanguage(TranslateLanguage.INDONESIAN, "Indonesian"),
    NexusLanguage(TranslateLanguage.THAI,       "Thai"),
    NexusLanguage(TranslateLanguage.VIETNAMESE, "Vietnamese"),
    NexusLanguage(TranslateLanguage.BENGALI,    "Bengali")
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class TranslatorUiState(
    val inputText: String = "",
    val outputText: String = "",
    val sourceLanguage: NexusLanguage = SUPPORTED_LANGUAGES[0],
    val targetLanguage: NexusLanguage = SUPPORTED_LANGUAGES[1],
    val isTranslating: Boolean = false,
    val isDownloading: Boolean = false,
    val error: String? = null
)

class TextTranslatorViewModel : ViewModel() {

    var uiState by mutableStateOf(TranslatorUiState())
        private set

    fun onInputChanged(v: String)              { uiState = uiState.copy(inputText = v, outputText = "", error = null) }
    fun onSourceLanguageChanged(l: NexusLanguage) { uiState = uiState.copy(sourceLanguage = l, outputText = "", error = null) }
    fun onTargetLanguageChanged(l: NexusLanguage) { uiState = uiState.copy(targetLanguage = l, outputText = "", error = null) }

    fun swapLanguages() {
        uiState = uiState.copy(
            sourceLanguage = uiState.targetLanguage,
            targetLanguage = uiState.sourceLanguage,
            inputText      = uiState.outputText,
            outputText     = ""
        )
    }

    fun translate() {
        val text = uiState.inputText.trim()
        if (text.isBlank()) { uiState = uiState.copy(error = "Enter text to translate"); return }

        viewModelScope.launch {
            uiState = uiState.copy(isTranslating = true, error = null, isDownloading = false)
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(uiState.sourceLanguage.code)
                .setTargetLanguage(uiState.targetLanguage.code)
                .build()
            val translator = Translation.getClient(options)
            try {
                // Download model if not cached
                uiState = uiState.copy(isDownloading = true)
                translator.downloadModelIfNeeded(
                    DownloadConditions.Builder().build()
                ).await()
                uiState = uiState.copy(isDownloading = false)

                val result = translator.translate(text).await()
                uiState = uiState.copy(outputText = result, isTranslating = false)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isTranslating = false,
                    isDownloading = false,
                    error = "Translation failed: ${e.localizedMessage}"
                )
            } finally {
                translator.close()
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTranslatorScreen(
    onBack: () -> Unit,
    viewModel: TextTranslatorViewModel = koinViewModel()
) {
    val s         = viewModel.uiState
    val clipboard = LocalClipboardManager.current
    val view      = LocalView.current

    LaunchedEffect(s.isDownloading) {
        if (s.isDownloading) view.announceForAccessibility("Downloading language model. This only happens once per language pair.")
    }
    LaunchedEffect(s.outputText) {
        if (s.outputText.isNotBlank()) view.announceForAccessibility("Translation complete.")
    }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Text Translator", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.OfflineBolt, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    Text(
                        "Powered by Google MLKit — works 100% offline after first download. No API key required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Language selectors + swap
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageDropdown(
                    selected  = s.sourceLanguage,
                    label     = "From",
                    onSelect  = viewModel::onSourceLanguageChanged,
                    modifier  = Modifier.weight(1f)
                )
                IconButton(
                    onClick  = { viewModel.swapLanguages(); view.announceForAccessibility("Languages swapped") },
                    modifier = Modifier.semantics { contentDescription = "Swap source and target languages" }
                ) {
                    Icon(Icons.Filled.SwapHoriz, null)
                }
                LanguageDropdown(
                    selected  = s.targetLanguage,
                    label     = "To",
                    onSelect  = viewModel::onTargetLanguageChanged,
                    modifier  = Modifier.weight(1f)
                )
            }

            // Input
            OutlinedTextField(
                value         = s.inputText,
                onValueChange = viewModel::onInputChanged,
                label         = { Text("Enter text (${s.sourceLanguage.displayName})") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .semantics { contentDescription = "Text to translate from ${s.sourceLanguage.displayName}." },
                maxLines      = 8,
                trailingIcon  = {
                    if (s.inputText.isNotBlank()) {
                        IconButton(onClick = { viewModel.onInputChanged("") }) {
                            Icon(Icons.Filled.Clear, "Clear input")
                        }
                    }
                }
            )

            // Translate button
            Button(
                onClick  = {
                    view.announceForAccessibility("Translating from ${s.sourceLanguage.displayName} to ${s.targetLanguage.displayName}")
                    viewModel.translate()
                },
                enabled  = !s.isTranslating && s.inputText.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp).semantics {
                    contentDescription = "Translate text from ${s.sourceLanguage.displayName} to ${s.targetLanguage.displayName}."
                }
            ) {
                if (s.isTranslating) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (s.isDownloading) "Downloading model…" else "Translating…")
                } else {
                    Icon(Icons.Filled.Translate, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Translate")
                }
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
                        "${s.targetLanguage.displayName} translation",
                        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            s.outputText,
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .semantics { contentDescription = "Translation: ${s.outputText}" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    OutlinedButton(
                        onClick  = {
                            clipboard.setText(AnnotatedString(s.outputText))
                            view.announceForAccessibility("Translation copied to clipboard")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ContentCopy, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Copy Translation")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    selected: NexusLanguage,
    label: String,
    onSelect: (NexusLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded        = expanded,
        onExpandedChange = { expanded = it },
        modifier        = modifier
    ) {
        OutlinedTextField(
            value             = selected.displayName,
            onValueChange     = {},
            readOnly          = true,
            label             = { Text(label) },
            trailingIcon      = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier          = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .semantics { contentDescription = "$label language: ${selected.displayName}. Double tap to change." },
            singleLine        = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SUPPORTED_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text     = { Text(lang.displayName) },
                    onClick  = { onSelect(lang); expanded = false },
                    modifier = Modifier.semantics { contentDescription = lang.displayName }
                )
            }
        }
    }
}
