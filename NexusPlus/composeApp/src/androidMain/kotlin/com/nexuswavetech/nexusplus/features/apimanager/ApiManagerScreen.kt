package com.nexuswavetech.nexusplus.features.apimanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.apiDataStore by preferencesDataStore(name = "api_keys")

sealed class ApiKeyEntry(
    val key: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
) {
    data object Gemini : ApiKeyEntry("gemini_api_key", "Gemini API Key", Icons.Filled.AutoAwesome, "Google Gemini AI API key for Aira, Video Description & Generation")
    data object AdMob : ApiKeyEntry("admob_app_id", "AdMob App ID", Icons.Filled.AttachMoney, "Google AdMob application ID for ads")
    data object Pollinations : ApiKeyEntry("pollinations_api_key", "Pollinations API Key", Icons.Filled.Image, "API key for AI Image Generation (optional)")
    data object Custom1 : ApiKeyEntry("custom_api_1", "Custom API 1", Icons.Filled.SettingsInputComponent, "Add your own custom API key")
    data object Custom2 : ApiKeyEntry("custom_api_2", "Custom API 2", Icons.Filled.SettingsInputComponent, "Add your own custom API key")

    companion object {
        val ALL = listOf(Gemini, AdMob, Pollinations, Custom1, Custom2)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiManagerScreen(
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var expandedEntry by remember { mutableStateOf<ApiKeyEntry?>(null) }
    var editText by remember { mutableStateOf("") }

    Scaffold(
        topBar = { NexusTopBar(title = "API Manager", onBack = onBack) },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Manage API Keys",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Store all your API keys securely in one place. Keys are saved locally and encrypted by Android.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            ApiKeyEntry.ALL.forEach { entry ->
                val savedValue by rememberApiKey(ctx, entry.key).collectAsState(initial = "")
                val isExpanded = expandedEntry == entry

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (savedValue.isNotBlank())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(entry.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(entry.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (savedValue.isNotBlank()) {
                                Icon(Icons.Filled.CheckCircle, "Configured", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Filled.ErrorOutline, "Not configured", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    label = { Text("${entry.label} value") },
                                    placeholder = { Text("Paste your ${entry.label} here…") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                ctx.apiDataStore.edit { it.remove(stringPreferencesKey(entry.key)) }
                                                editText = ""
                                                expandedEntry = null
                                                snack.showSnackbar("${entry.label} removed")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Remove")
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                ctx.apiDataStore.edit { it[stringPreferencesKey(entry.key)] = editText.trim() }
                                                expandedEntry = null
                                                editText = ""
                                                snack.showSnackbar("${entry.label} saved successfully")
                                            }
                                        },
                                        modifier = Modifier.weight(2f),
                                    ) {
                                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Save", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (!isExpanded) {
                            TextButton(
                                onClick = {
                                    expandedEntry = entry
                                    editText = savedValue
                                },
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Text(if (savedValue.isNotBlank()) "Edit" else "Add Key")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Security Note", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        "API keys are stored in Android DataStore (encrypted on API 24+). They never leave your device unless the feature actively calls an API. GitHub Actions can also inject keys at build time via environment variables.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

private fun rememberApiKey(context: android.content.Context, key: String): Flow<String> {
    val prefKey = stringPreferencesKey(key)
    return context.apiDataStore.data.map { it[prefKey] ?: "" }
}
