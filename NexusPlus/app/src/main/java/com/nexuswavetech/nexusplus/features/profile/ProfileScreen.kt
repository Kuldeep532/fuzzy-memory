package com.nexuswavetech.nexusplus.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.core.SessionManager
import com.nexuswavetech.nexusplus.core.displayName
import com.nexuswavetech.nexusplus.core.isGuest
import com.nexuswavetech.nexusplus.core.FeatureCatalog
import com.nexuswavetech.nexusplus.core.FavoritesRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val sessionManager: SessionManager      = koinInject()
    val favoritesRepo: FavoritesRepository  = koinInject()
    val scope                               = rememberCoroutineScope()

    val session     by sessionManager.session.collectAsState()
    val favoriteIds by favoritesRepo.favoriteIds.collectAsState(initial = emptySet())
    val pinnedIds   by favoritesRepo.pinnedIds.collectAsState(initial = emptySet())

    val displayName     = session.displayName.ifBlank { "Guest User" }
    val initial         = displayName.firstOrNull()?.uppercase() ?: "G"
    val memberSince     = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val totalFeatures   = FeatureCatalog.allFeatures.size
    val favCount        = favoriteIds.size
    val pinnedCount     = pinnedIds.size

    var showEditDialog by remember { mutableStateOf(false) }
    var editName       by remember { mutableStateOf(displayName) }
    var snackState     = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                NexusTopBar(title = "My Profile", onBack = onBack)
            }

            // ── Profile header ────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                initial,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            if (session.isGuest) "Guest Account" else "Nexus Plus Member",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Text(
                            "Member since $memberSince",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        )
                        if (session.isGuest) {
                            Surface(
                                color  = MaterialTheme.colorScheme.errorContainer,
                                shape  = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    "Sign in to unlock all premium features",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── Usage stats ───────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Activity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("Features", "$totalFeatures", Icons.Filled.Apps)
                            VerticalDivider(modifier = Modifier.height(48.dp))
                            StatItem("Favorites", "$favCount", Icons.Filled.Favorite)
                            VerticalDivider(modifier = Modifier.height(48.dp))
                            StatItem("Pinned", "$pinnedCount", Icons.Filled.PushPin)
                        }
                    }
                }
            }

            // ── Account actions ───────────────────────────────────────────
            item {
                ProfileSectionHeader("Account")
            }
            item {
                ProfileMenuItem(
                    icon     = Icons.Filled.Edit,
                    title    = "Edit Display Name",
                    subtitle = "Shown across the app",
                    onClick  = { editName = displayName; showEditDialog = true },
                )
            }
            item {
                ProfileMenuItem(
                    icon     = Icons.Filled.Fingerprint,
                    title    = "Biometric Settings",
                    subtitle = "Manage fingerprint and device unlock",
                    onClick  = {},
                )
            }

            // ── Privacy & Data ────────────────────────────────────────────
            item {
                ProfileSectionHeader("Privacy & Data")
            }
            item {
                ProfileMenuItem(
                    icon     = Icons.Filled.DeleteSweep,
                    title    = "Clear Favorites",
                    subtitle = "${favCount + pinnedCount} items saved",
                    onClick  = {
                        scope.launch {
                            favoritesRepo.clearAll()
                            snackState.showSnackbar("Favorites cleared")
                        }
                    },
                )
            }
            item {
                ProfileMenuItem(
                    icon     = Icons.Filled.PrivacyTip,
                    title    = "Data & Permissions",
                    subtitle = "Review what Nexus Plus accesses",
                    onClick  = {},
                )
            }

            // ── App info ──────────────────────────────────────────────────
            item { ProfileSectionHeader("App") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppInfoRow(Icons.Filled.Android,  "Version",     "1.2.0")
                        HorizontalDivider()
                        AppInfoRow(Icons.Filled.Business, "Developer",   "Nexus Wave Technologies")
                        HorizontalDivider()
                        AppInfoRow(Icons.Filled.Build,    "Stack",       "Kotlin · Compose · KMP")
                        HorizontalDivider()
                        AppInfoRow(Icons.Filled.Security, "Encryption",  "AES-256-GCM · Android Keystore")
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            icon             = { Icon(Icons.Filled.Edit, null) },
            title            = { Text("Edit Display Name") },
            text             = {
                OutlinedTextField(
                    value         = editName,
                    onValueChange = { editName = it },
                    label         = { Text("Display name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    showEditDialog = false
                    scope.launch { snackState.showSnackbar("Name updated") }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics { contentDescription = "$label: $value" },
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        title,
        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp).semantics { heading() },
    )
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title. $subtitle." },
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun AppInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().semantics { contentDescription = "$label: $value" },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
