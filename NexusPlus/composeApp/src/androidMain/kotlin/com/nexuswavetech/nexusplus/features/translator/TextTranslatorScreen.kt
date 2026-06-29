package com.nexuswavetech.nexusplus.features.translator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ── Supported languages ───────────────────────────────────────────────────────

data class NexusLanguage(val code: String, val displayName: String)

val SUPPORTED_LANGUAGES = listOf(
    NexusLanguage(TranslateLanguage.ENGLISH,    "English"),
    NexusLanguage(TranslateLanguage.HINDI,      "हिंदी"),
    NexusLanguage(TranslateLanguage.ARABIC,     "العربية"),
    NexusLanguage(TranslateLanguage.SPANISH,    "Español"),
    NexusLanguage(TranslateLanguage.FRENCH,     "Français"),
    NexusLanguage(TranslateLanguage.GERMAN,     "Deutsch"),
    NexusLanguage(TranslateLanguage.PORTUGUESE, "Português"),
    NexusLanguage(TranslateLanguage.RUSSIAN,    "Русский"),
    NexusLanguage(TranslateLanguage.JAPANESE,   "日本語"),
    NexusLanguage(TranslateLanguage.CHINESE,    "中文"),
    NexusLanguage(TranslateLanguage.KOREAN,     "한국어"),
    NexusLanguage(TranslateLanguage.ITALIAN,    "Italiano"),
    NexusLanguage(TranslateLanguage.DUTCH,      "Nederlands"),
    NexusLanguage(TranslateLanguage.TURKISH,    "Türkçe"),
    NexusLanguage(TranslateLanguage.POLISH,     "Polski"),
    NexusLanguage(TranslateLanguage.SWEDISH,    "Svenska"),
    NexusLanguage(TranslateLanguage.INDONESIAN, "Indonesia"),
    NexusLanguage(TranslateLanguage.THAI,       "ภาษาไทย"),
    NexusLanguage(TranslateLanguage.VIETNAMESE, "Tiếng Việt"),
    NexusLanguage(TranslateLanguage.BENGALI,    "বাংলা"),
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class TranslatorUiState(
    val inputText: String = "",
    val outputText: String = "",
    val sourceLanguage: NexusLanguage = SUPPORTED_LANGUAGES[0],
    val targetLanguage: NexusLanguage = SUPPORTED_LANGUAGES[1],
    val isTranslating: Boolean = false,
    val isDownloading: Boolean = false,
    val error: String? = null,
)

class TextTranslatorViewModel : ViewModel() {

    var uiState by mutableStateOf(TranslatorUiState())
        private set

    fun onInputChanged(v: String)                 { uiState = uiState.copy(inputText = v, outputText = "", error = null) }
    fun onSourceLanguageChanged(l: NexusLanguage) { uiState = uiState.copy(sourceLanguage = l, outputText = "", error = null) }
    fun onTargetLanguageChanged(l: NexusLanguage) { uiState = uiState.copy(targetLanguage = l, outputText = "", error = null) }

    fun swapLanguages() {
        uiState = uiState.copy(
            sourceLanguage = uiState.targetLanguage,
            targetLanguage = uiState.sourceLanguage,
            inputText      = uiState.outputText,
            outputText     = "",
        )
    }

    fun translate() {
        val text = uiState.inputText.trim()
        if (text.isBlank()) { uiState = uiState.copy(error = "Please enter text to translate"); return }
        viewModelScope.launch {
            uiState = uiState.copy(isTranslating = true, error = null, isDownloading = true)
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(uiState.sourceLanguage.code)
                .setTargetLanguage(uiState.targetLanguage.code)
                .build()
            val translator = Translation.getClient(options)
            try {
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                uiState = uiState.copy(isDownloading = false)
                val result = translator.translate(text).await()
                uiState = uiState.copy(outputText = result, isTranslating = false)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isTranslating = false,
                    isDownloading = false,
                    error = "Translation failed: ${e.localizedMessage}",
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
    viewModel: TextTranslatorViewModel = koinViewModel(),
) {
    val s         = viewModel.uiState
    val clipboard = LocalClipboardManager.current
    val view      = LocalView.current

    LaunchedEffect(s.isDownloading) {
        if (s.isDownloading) view.announceForAccessibility("Downloading language model. This only happens once.")
    }
    LaunchedEffect(s.outputText) {
        if (s.outputText.isNotBlank()) view.announceForAccessibility("Translation complete.")
    }

    Scaffold(
        topBar = { NexusTopBar(title = "Translator", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Offline badge ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.OfflineBolt,
                    null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Offline translation — no internet required",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                // ── Language selector panel ────────────────────────────────
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Source language
                        LanguagePickerButton(
                            language = s.sourceLanguage,
                            label = "From",
                            onSelect = viewModel::onSourceLanguageChanged,
                            modifier = Modifier.weight(1f),
                        )

                        // Swap button
                        FilledIconButton(
                            onClick = {
                                viewModel.swapLanguages()
                                view.announceForAccessibility("Languages swapped")
                            },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(40.dp)
                                .semantics { contentDescription = "Swap source and target languages" },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                        }

                        // Target language
                        LanguagePickerButton(
                            language = s.targetLanguage,
                            label = "To",
                            onSelect = viewModel::onTargetLanguageChanged,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── Input field ────────────────────────────────────────────
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                s.sourceLanguage.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (s.inputText.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.onInputChanged("") },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        "Clear input",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        BasicTranslatorTextField(
                            value = s.inputText,
                            onValueChange = viewModel::onInputChanged,
                            placeholder = "Type or paste text here…",
                            contentDesc = "Text to translate from ${s.sourceLanguage.displayName}.",
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                view.announceForAccessibility(
                                    "Translating from ${s.sourceLanguage.displayName} to ${s.targetLanguage.displayName}"
                                )
                                viewModel.translate()
                            },
                            enabled = !s.isTranslating && s.inputText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .semantics {
                                    contentDescription = "Translate from ${s.sourceLanguage.displayName} to ${s.targetLanguage.displayName}."
                                },
                            shape = MaterialTheme.shapes.large,
                        ) {
                            AnimatedContent(targetState = s.isTranslating, label = "btn") { loading ->
                                if (loading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        CircularProgressIndicator(
                                            Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                        Text(if (s.isDownloading) "Downloading model…" else "Translating…")
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Filled.Translate, null, modifier = Modifier.size(18.dp))
                                        Text(
                                            "Translate",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Error ──────────────────────────────────────────────────
                AnimatedVisibility(visible = s.error != null) {
                    if (s.error != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.ErrorOutline,
                                    null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    s.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }

                // ── Output ─────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = s.outputText.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    if (s.outputText.isNotBlank()) {
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        s.targetLanguage.displayName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.semantics { heading() },
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(s.outputText))
                                            view.announceForAccessibility("Translation copied to clipboard")
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .semantics { contentDescription = "Copy translation to clipboard" },
                                    ) {
                                        Icon(
                                            Icons.Filled.ContentCopy,
                                            null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    s.outputText,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "Translation: ${s.outputText}" },
                                )

                                Spacer(Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(s.outputText))
                                        view.announceForAccessibility("Translation copied to clipboard")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                                        )
                                    ),
                                ) {
                                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy Translation")
                                }
                            }
                        }
                    }
                }

                // ── Tip card ───────────────────────────────────────────────
                if (s.outputText.isBlank() && s.inputText.isBlank() && !s.isTranslating) {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Filled.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            )
                            Text(
                                "20 Languages, 100% Offline",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "First translation downloads the language model (≈ 30 MB). After that, everything works offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Sub-components ─────────────────────────────────────────────────────────────

@Composable
private fun BasicTranslatorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    contentDesc: String,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .semantics { contentDescription = contentDesc },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 26.sp,
        ),
        maxLines = 10,
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            inner()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerButton(
    language: NexusLanguage,
    label: String,
    onSelect: (NexusLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics { contentDescription = "$label language: ${language.displayName}" },
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    language.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SUPPORTED_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(lang.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    onClick = { onSelect(lang); expanded = false },
                    trailingIcon = if (lang == language) {
                        { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.semantics { contentDescription = lang.displayName },
                )
            }
        }
    }
}
