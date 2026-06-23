package com.nexuswavetech.nexusplus.ios.features.emergencyguardian

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import platform.Foundation.NSUserDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── iOS Emergency Contact Model ───────────────────────────────────────────────

@Serializable
data class IosEmergencyContact(
    val id: String,
    val name: String,
    val phone: String,
)

// ── iOS Emergency Guardian Repository (NSUserDefaults-backed) ─────────────────

private object IosGuardianStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val json = Json { ignoreUnknownKeys = true }
    private const val KEY = "ios_emergency_contacts"

    fun loadContacts(): List<IosEmergencyContact> {
        val raw = defaults.stringForKey(KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<IosEmergencyContact>>(raw) }.getOrElse { emptyList() }
    }

    fun saveContacts(contacts: List<IosEmergencyContact>) {
        defaults.setObject(json.encodeToString(contacts), forKey = KEY)
        defaults.synchronize()
    }
}

// ── iOS Guardian UI State ─────────────────────────────────────────────────────

private data class IosGuardianUiState(
    val contacts: List<IosEmergencyContact> = emptyList(),
    val isGuardActive: Boolean = false,
    val nameInput: String = "",
    val phoneInput: String = "",
    val inputError: String? = null,
)

// ── iOS Emergency Guardian Screen ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosEmergencyGuardianScreen(onBack: () -> Unit) {
    var state by remember {
        mutableStateOf(
            IosGuardianUiState(contacts = IosGuardianStore.loadContacts())
        )
    }

    val guardColor by animateColorAsState(
        targetValue = if (state.isGuardActive) Color(0xFF1B5E20) else Color(0xFF37474F),
        animationSpec = tween(400),
        label = "guardColor",
    )

    fun addContact() {
        val name = state.nameInput.trim()
        val phone = state.phoneInput.trim()
        when {
            name.isEmpty() -> { state = state.copy(inputError = "Name cannot be empty"); return }
            phone.isEmpty() -> { state = state.copy(inputError = "Phone cannot be empty"); return }
            phone.any { !it.isDigit() && it != '+' && it != '-' && it != ' ' } -> {
                state = state.copy(inputError = "Invalid phone number"); return
            }
        }
        val newContact = IosEmergencyContact(
            id = kotlin.random.Random.nextLong().toString(),
            name = name,
            phone = phone,
        )
        val updated = state.contacts + newContact
        IosGuardianStore.saveContacts(updated)
        state = state.copy(contacts = updated, nameInput = "", phoneInput = "", inputError = null)
    }

    fun deleteContact(id: String) {
        val updated = state.contacts.filter { it.id != id }
        IosGuardianStore.saveContacts(updated)
        state = state.copy(contacts = updated)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Emergency Guardian",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { contentDescription = "Emergency Guardian screen" },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Go back" },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Guard Toggle Card ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (state.isGuardActive)
                                "Emergency Guardian is active" else "Emergency Guardian is inactive"
                        },
                    colors = CardDefaults.cardColors(containerColor = guardColor),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.White,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (state.isGuardActive) "Guardian Active" else "Guardian Inactive",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = if (state.isGuardActive)
                                    "Monitoring for SOS trigger"
                                else
                                    "Toggle to activate emergency monitoring",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        Switch(
                            checked = state.isGuardActive,
                            onCheckedChange = { state = state.copy(isGuardActive = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF43A047),
                            ),
                        )
                    }
                }
            }

            // ── iOS Info Card ─────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "iOS Emergency Guardian",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "On iOS, use the Phone app or Siri to place emergency calls. Contacts saved here will be shown when Guardian is active. Press the side button 5 times quickly to trigger iPhone's built-in Emergency SOS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            // ── Contacts Section ──────────────────────────────────────────────
            item {
                Text(
                    text = "Emergency Contacts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { contentDescription = "Emergency Contacts section" },
                )
            }

            if (state.contacts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "No contacts added yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                items(state.contacts, key = { it.id }) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Contact ${contact.name}" },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column {
                                    Text(
                                        text = contact.name,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = contact.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(
                                onClick = { deleteContact(contact.id) },
                                modifier = Modifier.semantics { contentDescription = "Delete ${contact.name}" }
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            // ── Add Contact Card ──────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Add Emergency Contact",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = state.nameInput,
                            onValueChange = { state = state.copy(nameInput = it, inputError = null) },
                            label = { Text("Contact Name") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = state.inputError != null && state.nameInput.isEmpty(),
                        )
                        OutlinedTextField(
                            value = state.phoneInput,
                            onValueChange = { state = state.copy(phoneInput = it, inputError = null) },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = state.inputError != null,
                            supportingText = state.inputError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        )
                        Button(
                            onClick = { addContact() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Contact")
                        }
                    }
                }
            }

            // ── How It Works Card ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "How Guardian Works on iPhone",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val steps = listOf(
                            "📱 Add emergency contacts below",
                            "🛡️ Enable Guardian to stay ready",
                            "🆘 Press side button 5× for iPhone Emergency SOS",
                            "📲 Your contacts will be shown for quick dial",
                            "📍 iPhone automatically shares your location",
                        )
                        steps.forEach { step ->
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
