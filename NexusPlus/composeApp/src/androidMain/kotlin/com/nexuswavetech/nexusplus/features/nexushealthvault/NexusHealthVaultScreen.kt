package com.nexuswavetech.nexusplus.features.nexushealthvault

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import org.koin.androidx.compose.koinViewModel

private data class HealthCategory(val name: String, val icon: ImageVector, val color: Color)

private val categories = listOf(
    HealthCategory("Vitals",       Icons.Filled.MonitorHeart,  Color(0xFFEF5350)),
    HealthCategory("Medications",  Icons.Filled.Medication,    Color(0xFFAB47BC)),
    HealthCategory("Lab Results",  Icons.Filled.Science,       Color(0xFF42A5F5)),
    HealthCategory("Appointments", Icons.Filled.CalendarMonth, Color(0xFF66BB6A)),
    HealthCategory("Allergies",    Icons.Filled.Warning,       Color(0xFFFFA726)),
    HealthCategory("Vaccinations", Icons.Filled.Vaccines,      Color(0xFF26A69A)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusHealthVaultScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: HealthVaultViewModel = koinViewModel()

    val records by vm.records.collectAsStateWithLifecycle()

    var isLocked     by remember { mutableStateOf(true) }
    var authError    by remember { mutableStateOf<String?>(null) }
    var selectedCat  by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    var newCategory  by remember { mutableStateOf(categories.first().name) }
    var newLabel     by remember { mutableStateOf("") }
    var newValue     by remember { mutableStateOf("") }
    var newUnit      by remember { mutableStateOf("") }
    var catExpanded  by remember { mutableStateOf(false) }
    val today = remember {
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    fun launchBiometric() {
        val bm = BiometricManager.from(context)
        val canAuth = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(context)
            BiometricPrompt(
                context as FragmentActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) {
                        isLocked = false; authError = null
                    }
                    override fun onAuthenticationError(code: Int, msg: CharSequence) {
                        authError = msg.toString()
                    }
                    override fun onAuthenticationFailed() {
                        authError = "Authentication failed. Please try again."
                    }
                }
            ).authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Health Vault")
                    .setSubtitle("Authenticate to access your health records")
                    .setDescription("Your data is encrypted and synced securely via Firebase")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()
            )
        } else {
            isLocked = false
        }
    }

    LaunchedEffect(Unit) { launchBiometric() }

    if (isLocked) {
        HealthVaultLockScreen(error = authError, onUnlockClicked = ::launchBiometric, onBack = onBack)
        return
    }

    val filtered = remember(records, selectedCat) {
        if (selectedCat == null) records else records.filter { it.category == selectedCat }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Health Vault", onBack = onBack, actions = {
            IconButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add health record")
            }
        })

        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.HealthAndSafety, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Health Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.semantics { heading() })
                            Text("Biometric-locked · Firebase-synced text records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text("Cards & documents stay on-device with AES-256", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Category filter chips
            item {
                Row(
                    modifier            = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = selectedCat == null, onClick = { selectedCat = null }, label = { Text("All (${records.size})") })
                    categories.forEach { cat ->
                        FilterChip(
                            selected    = selectedCat == cat.name,
                            onClick     = { selectedCat = if (selectedCat == cat.name) null else cat.name },
                            label       = { Text(cat.name) },
                            leadingIcon = { Icon(cat.icon, null, modifier = Modifier.size(16.dp), tint = cat.color) },
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("No records yet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap + to add your first health record", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { showAddSheet = true }) { Text("Add Record") }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { record ->
                    val cat = categories.firstOrNull { it.name == record.category }
                    HealthRecordCard(
                        record   = record,
                        catColor = cat?.color ?: MaterialTheme.colorScheme.primary,
                        catIcon  = cat?.icon  ?: Icons.Filled.HealthAndSafety,
                        onDelete = { vm.deleteRecord(record.id) },
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Add Health Record", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value     = newCategory,
                        onValueChange = {},
                        readOnly  = true,
                        label     = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier  = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text     = { Text(cat.name) },
                                leadingIcon = { Icon(cat.icon, null, tint = cat.color) },
                                onClick  = { newCategory = cat.name; catExpanded = false },
                            )
                        }
                    }
                }

                OutlinedTextField(value = newLabel, onValueChange = { newLabel = it }, label = { Text("Label (e.g. Blood Pressure)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newValue, onValueChange = { newValue = it }, label = { Text("Value") }, singleLine = true, modifier = Modifier.weight(2f))
                    OutlinedTextField(value = newUnit, onValueChange = { newUnit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        if (newLabel.isNotBlank() && newValue.isNotBlank()) {
                            vm.addRecord(newCategory, newLabel, newValue, newUnit, today)
                            newLabel = ""; newValue = ""; newUnit = ""
                            showAddSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save Record") }
            }
        }
    }
}

// ── Lock screen ────────────────────────────────────────────────────────────────

@Composable
private fun HealthVaultLockScreen(error: String?, onUnlockClicked: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Health Vault", onBack = onBack)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier            = Modifier.padding(32.dp),
            ) {
                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Health Vault is Locked", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Authenticate to access your health records",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                Button(onClick = onUnlockClicked, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Filled.Fingerprint, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock with Biometrics")
                }
            }
        }
    }
}

// ── Record card ────────────────────────────────────────────────────────────────

@Composable
private fun HealthRecordCard(
    record:   HealthRecord,
    catColor: Color,
    catIcon:  ImageVector,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${record.category}: ${record.label} ${record.value} ${record.unit}. ${record.recordedOn}" },
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(catIcon, null, modifier = Modifier.size(28.dp), tint = catColor)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(record.label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Text("${record.value} ${record.unit}".trim(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = catColor)
                Text(record.recordedOn, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete record", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
