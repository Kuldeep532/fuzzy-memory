package com.nexuswavetech.nexusplus.features.tts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Language selector shown only in Mix mode.
 * Lets the user pin a primary language for multi-language TTS segments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixLanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    label: String = "Mix Mode Language",
) {
    var expanded by remember { mutableStateOf(false) }

    val languages = listOf(
        ""         to "Auto-detect per segment",
        "en"       to "English",
        "hi"       to "Hindi",
        "bn"       to "Bengali",
        "ta"       to "Tamil",
        "te"       to "Telugu",
        "mr"       to "Marathi",
        "gu"       to "Gujarati",
        "kn"       to "Kannada",
        "ml"       to "Malayalam",
        "pa"       to "Punjabi",
        "ur"       to "Urdu",
        "es"       to "Spanish",
        "fr"       to "French",
        "de"       to "German",
        "zh"       to "Chinese",
        "ja"       to "Japanese",
        "ko"       to "Korean",
        "ar"       to "Arabic",
        "ru"       to "Russian",
        "pt"       to "Portuguese",
        "it"       to "Italian",
    )

    val selectedLabel = languages.firstOrNull { it.first == selectedLanguage }?.second ?: "Auto-detect per segment"

    Card(
        shape  = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                "Pick a preferred voice language. Auto-detects when unset.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value         = selectedLabel,
                    onValueChange = {},
                    readOnly      = true,
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors        = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier      = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .semantics { contentDescription = "Mix mode language. Current: $selectedLabel" },
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Filled.Language, null, modifier = Modifier.size(18.dp)) },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    languages.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onLanguageSelected(code); expanded = false },
                            leadingIcon = if (code == selectedLanguage) {
                                { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}
