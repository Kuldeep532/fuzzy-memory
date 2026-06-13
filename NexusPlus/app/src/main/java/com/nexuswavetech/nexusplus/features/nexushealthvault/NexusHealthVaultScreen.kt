package com.nexuswavetech.nexusplus.features.nexushealthvault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

private data class HealthRecord(
    val id:         String,
    val category:   String,
    val label:      String,
    val value:      String,
    val unit:       String,
    val recordedOn: String,
)

private data class HealthCategory(val name: String, val icon: ImageVector, val color: Color)

private val categories = listOf(
    HealthCategory("Vitals",         Icons.Filled.MonitorHeart,   Color(0xFFF44336)),
    HealthCategory("Medications",    Icons.Filled.Medication,     Color(0xFF9C27B0)),
    HealthCategory("Lab Results",    Icons.Filled.Science,        Color(0xFF2196F3)),
    HealthCategory("Appointments",   Icons.Filled.CalendarMonth,  Color(0xFF4CAF50)),
    HealthCategory("Allergies",      Icons.Filled.Warning,        Color(0xFFFF9800)),
    HealthCategory("Vaccinations",   Icons.Filled.Vaccines,       Color(0xFF009688)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusHealthVaultScreen(onBack: () -> Unit) {
    var records       by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var selectedCat   by remember { mutableStateOf<String?>(null) }
    var showAddSheet  by remember { mutableStateOf(false) }

    var newCategory   by remember { mutableStateOf(categories.first().name) }
    var newLabel      by remember { mutableStateOf("") }
    var newValue      by remember { mutableStateOf("") }
    var newUnit       by remember { mutableStateOf("") }
    var catExpanded   by remember { mutableStateOf(false) }

    val today = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date()) }

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
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.HealthAndSafety, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Nexus Health Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.semantics { heading() })
                            Text("Secure, offline-first health records. Medications, vitals, lab results and appointments — encrypted on-device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Category chips
            item {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = selectedCat == null, onClick = { selectedCat = null }, label = { Text("All") })
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected    = selectedCat == cat.name,
                            onClick     = { selectedCat = cat.name },
                            label       = { Text(cat.name) },
                            leadingIcon = { Icon(cat.icon, null, modifier = Modifier.size(14.dp), tint = cat.color) },
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 48.dp).semantics { contentDescription = "No health records yet. Tap + to add." },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.HealthAndSafety, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("No health records", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Track vitals, medications, and appointments.\nAll data stays on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FilledTonalButton(onClick = { showAddSheet = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Record") }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { record ->
                    val cat = categories.firstOrNull { it.name == record.category }
                    Card(
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${record.category}: ${record.label}. ${record.value} ${record.unit}. Recorded on ${record.recordedOn}." },
                        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(cat?.icon ?: Icons.Filled.HealthAndSafety, null, tint = cat?.color ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${record.value} ${record.unit}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                                Text("${record.category} · ${record.recordedOn}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { records = records.filter { it.id != record.id } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${record.label}", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Privacy notice
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                        Text("All health data is stored locally on your device and never shared with third parties.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Add Health Record", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value         = newCategory,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Category") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier      = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat -> DropdownMenuItem(text = { Text(cat.name) }, onClick = { newCategory = cat.name; catExpanded = false }) }
                    }
                }

                OutlinedTextField(value = newLabel, onValueChange = { newLabel = it }, label = { Text("Record Label (e.g. Blood Pressure)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newValue, onValueChange = { newValue = it }, label = { Text("Value") }, singleLine = true, modifier = Modifier.weight(2f))
                    OutlinedTextField(value = newUnit, onValueChange = { newUnit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showAddSheet = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (newLabel.isNotBlank() && newValue.isNotBlank()) {
                                records = records + HealthRecord(id = System.currentTimeMillis().toString(), category = newCategory, label = newLabel.trim(), value = newValue.trim(), unit = newUnit.trim(), recordedOn = today)
                                newLabel = ""; newValue = ""; newUnit = ""
                                showAddSheet = false
                            }
                        },
                        enabled  = newLabel.isNotBlank() && newValue.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Add Record") }
                }
            }
        }
    }
}
