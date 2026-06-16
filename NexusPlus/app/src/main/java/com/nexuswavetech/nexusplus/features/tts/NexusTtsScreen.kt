package com.nexuswavetech.nexusplus.features.tts

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusTtsScreen(
    onBack: () -> Unit,
    viewModel: NseViewModel = koinViewModel(),
) {
    val context       = LocalContext.current
    val settings      = koinInject<SettingsRepository>()
    val scope         = rememberCoroutineScope()

    val engineState   by viewModel.engineState.collectAsState()
    val voices        by viewModel.availableVoices.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val pitch         by viewModel.pitch.collectAsState()
    val speechRate    by viewModel.speechRate.collectAsState()
    val ttsLang       by settings.ttsDefaultLanguage.collectAsState(initial = SettingsRepository.TTS_LANG_AUTO)

    val languageOptions: List<Pair<String, String>> = remember(voices) {
        val extra = voices.map { it.locale.language }
            .distinct()
            .sorted()
            .map { code -> code to (Locale(code).displayLanguage.ifBlank { code }) }
        listOf(SettingsRepository.TTS_LANG_AUTO to "Auto (system locale)") + extra
    }

    val filteredVoices: List<NseVoiceProfile> = remember(voices, ttsLang) {
        if (ttsLang == SettingsRepository.TTS_LANG_AUTO) voices
        else voices.filter { it.locale.language == ttsLang }
    }

    val voiceOptions: List<Pair<NseVoiceProfile?, String>> = remember(filteredVoices) {
        listOf(null to "Auto") + filteredVoices.map { v ->
            v to v.locale.displayName.ifBlank { v.locale.language }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Speech Engine Settings", onBack = onBack)

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Engine status ──────────────────────────────────────────────
            EngineStatusBanner(state = engineState)

            // ── Engine badge ───────────────────────────────────────────────
            Surface(
                shape  = MaterialTheme.shapes.medium,
                color  = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nexus Speech Engine 3.0",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            if (viewModel.isPipelineEngine) "NSE Pipeline · PCM cache active"
                            else "NSE Standard · Android TTS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
            }

            // ── Language ───────────────────────────────────────────────────
            SettingsSection(title = "Language") {
                TtsDropdown(
                    options  = languageOptions,
                    selected = ttsLang,
                    onSelect = { scope.launch { settings.setTtsDefaultLanguage(it) } },
                )
            }

            // ── Voice ──────────────────────────────────────────────────────
            SettingsSection(title = "Voice") {
                TtsDropdown(
                    options  = voiceOptions,
                    selected = selectedVoice,
                    onSelect = viewModel::onVoiceSelected,
                )
                if (filteredVoices.isEmpty()) {
                    Text(
                        "No voices found for the selected language.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Voice Parameters ───────────────────────────────────────────
            SettingsSection(title = "Voice Parameters") {
                Text(
                    "Speed: ${"%.1f".format(speechRate)}×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Speech speed: ${"%.1f".format(speechRate)} times"
                    },
                )
                Slider(
                    value         = speechRate,
                    onValueChange = viewModel::onSpeechRateChange,
                    valueRange    = 0.25f..3.0f,
                    steps         = 27,
                    modifier      = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pitch: ${"%.1f".format(pitch)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Voice pitch: ${"%.1f".format(pitch)}"
                    },
                )
                Slider(
                    value         = pitch,
                    onValueChange = viewModel::onPitchChange,
                    valueRange    = 0.5f..2.0f,
                    steps         = 14,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }

            // ── System TTS settings shortcut ───────────────────────────────
            Button(
                onClick  = {
                    runCatching {
                        context.startActivity(
                            Intent("com.android.settings.TTS_SETTINGS")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.onFailure {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Open system Text-to-Speech settings." },
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open System TTS Settings")
            }

            // ── Info ───────────────────────────────────────────────────────
            Text(
                "Nexus Speech Engine is registered as a system TTS provider. " +
                "Select it in Settings → Accessibility → Text-to-Speech output to use it device-wide.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Private sub-components ────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> TtsDropdown(
    options:  List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected.toString()

    ExposedDropdownMenuBox(
        expanded          = expanded,
        onExpandedChange  = { expanded = !expanded },
        modifier          = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value          = selectedLabel,
            onValueChange  = {},
            readOnly       = true,
            trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors         = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier       = Modifier.menuAnchor().fillMaxWidth(),
            singleLine     = true,
        )
        ExposedDropdownMenu(
            expanded          = expanded,
            onDismissRequest  = { expanded = false },
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text             = { Text(label) },
                    onClick          = { onSelect(value); expanded = false },
                    contentPadding   = ExposedDropdownMenuDefaults.ItemContentPadding,
                    leadingIcon      = if (value == selected) {
                        { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun EngineStatusBanner(state: NseState) {
    AnimatedVisibility(
        visible = state != NseState.Ready && state != NseState.Speaking,
        enter   = fadeIn(tween(200)) + expandVertically(),
        exit    = fadeOut(tween(150)) + shrinkVertically(),
    ) {
        val (text, color) = when (state) {
            is NseState.Initialising -> "Nexus Speech Engine initialising…" to
                MaterialTheme.colorScheme.onSurfaceVariant
            is NseState.Error        -> "⚠ ${state.message}" to
                MaterialTheme.colorScheme.error
            else                     -> "" to MaterialTheme.colorScheme.onSurface
        }
        if (text.isNotEmpty()) {
            Text(
                text     = text,
                color    = color,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { contentDescription = text },
            )
        }
    }
}
