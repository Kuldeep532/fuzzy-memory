package com.nexuswavetech.nexusplus.features.emergencyguardian

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EmergencyGuardianScreen(onBack: () -> Unit) {
    val viewModel: EmergencyGuardianViewModel = koinViewModel()
    val state     by viewModel.state.collectAsState()
    val context   = LocalContext.current

    val requiredPermissions = buildList {
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissionsState = rememberMultiplePermissionsState(requiredPermissions)

    LaunchedEffect(Unit) { viewModel.syncGuardState() }

    val guardColor by animateColorAsState(
        targetValue = if (state.isGuardActive) Color(0xFF1B5E20) else Color(0xFF37474F),
        animationSpec = tween(400),
        label = "guardColor",
    )

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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
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

            item {
                GuardStatusCard(
                    isActive   = state.isGuardActive,
                    guardColor = guardColor,
                    onToggle   = { active ->
                        if (active) {
                            if (!permissionsState.allPermissionsGranted) {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                            EmergencyGuardianService.start(context)
                        } else {
                            EmergencyGuardianService.stop(context)
                        }
                        viewModel.setGuardActive(active)
                    },
                )
            }

            item {
                PermissionStatusCard(
                    permissions = permissionsState.permissions.associate {
                        it.permission to it.status.isGranted
                    },
                    onRequestAll = { permissionsState.launchMultiplePermissionRequest() },
                )
            }

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
                    EmptyContactsCard()
                }
            } else {
                items(state.contacts, key = { it.id }) { contact ->
                    ContactCard(
                        contact  = contact,
                        onDelete = { viewModel.deleteContact(contact.id) },
                    )
                }
            }

            item {
                AddContactCard(
                    nameInput  = state.nameInput,
                    phoneInput = state.phoneInput,
                    error      = state.inputError,
                    onNameChange  = viewModel::setNameInput,
                    onPhoneChange = viewModel::setPhoneInput,
                    onAdd         = viewModel::addContact,
                )
            }

            item {
                HowItWorksCard()
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Guard Status Card ─────────────────────────────────────────────────────────

@Composable
private fun GuardStatusCard(
    isActive:   Boolean,
    guardColor: Color,
    onToggle:   (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = if (isActive) "Emergency Guardian is active" else "Emergency Guardian is inactive" },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = guardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(targetState = isActive, label = "shieldIcon") { active ->
                        Icon(
                            imageVector = if (active) Icons.Filled.Shield else Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isActive) "Guardian Active" else "Guardian Inactive",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = if (isActive) "Monitoring for distress signals" else "Tap to activate protection",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                    )
                }
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = MaterialTheme.colorScheme.onSurface,
                    checkedTrackColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                ),
                modifier = Modifier.semantics {
                    contentDescription = if (isActive) "Deactivate Emergency Guardian" else "Activate Emergency Guardian"
                },
            )
        }
    }
}

// ── Permission Status Card ─────────────────────────────────────────────────────

@Composable
private fun PermissionStatusCard(
    permissions:  Map<String, Boolean>,
    onRequestAll: () -> Unit,
) {
    val allGranted = permissions.values.all { it }
    if (allGranted) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Permissions Required",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.semantics { contentDescription = "Permissions required section" },
            )
            Spacer(Modifier.height(8.dp))
            val labels = mapOf(
                Manifest.permission.SEND_SMS            to "Send SMS",
                Manifest.permission.CALL_PHONE          to "Make Calls",
                Manifest.permission.ACCESS_FINE_LOCATION to "Precise Location",
                Manifest.permission.POST_NOTIFICATIONS  to "Notifications",
            )
            permissions.forEach { (perm, granted) ->
                if (!granted) {
                    Text(
                        text = "• ${labels[perm] ?: perm} — not granted",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.semantics {
                            contentDescription = "${labels[perm] ?: perm} permission not granted"
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRequestAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Grant all required permissions" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

// ── Contact Card ──────────────────────────────────────────────────────────────

@Composable
private fun ContactCard(
    contact:  EmergencyContact,
    onDelete: () -> Unit,
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Emergency contact: ${contact.name}, phone: ${contact.phone}" },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = contact.phone,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.semantics { contentDescription = "Delete contact ${contact.name}" },
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── Empty Contacts Card ───────────────────────────────────────────────────────

@Composable
private fun EmptyContactsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "No emergency contacts added yet" },
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No contacts added yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Add at least one contact below",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

// ── Add Contact Card ──────────────────────────────────────────────────────────

@Composable
private fun AddContactCard(
    nameInput:    String,
    phoneInput:   String,
    error:        String?,
    onNameChange:  (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAdd:         () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Add Emergency Contact",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.semantics { contentDescription = "Add emergency contact form" },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value         = nameInput,
                onValueChange = onNameChange,
                label         = { Text("Full Name") },
                leadingIcon   = {
                    Icon(Icons.Filled.Person, contentDescription = null)
                },
                singleLine    = true,
                isError       = error != null && nameInput.isBlank(),
                modifier      = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Contact full name input" },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = phoneInput,
                onValueChange = onPhoneChange,
                label         = { Text("Phone Number") },
                leadingIcon   = {
                    Icon(Icons.Filled.Phone, contentDescription = null)
                },
                singleLine          = true,
                keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError             = error != null && phoneInput.isBlank(),
                modifier            = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Contact phone number input" },
            )
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.semantics { contentDescription = "Input error: $error" },
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick  = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Add contact button" },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Contact")
            }
        }
    }
}

// ── How It Works Card ─────────────────────────────────────────────────────────

@Composable
private fun HowItWorksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "How Emergency Guardian Works",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.semantics { contentDescription = "How Emergency Guardian Works section" },
            )
            Spacer(Modifier.height(10.dp))
            val steps = listOf(
                "1. Activate the Guardian and grant all permissions.",
                "2. Guardian runs silently in the background.",
                "3. A rapid shake (5+ shakes detected) triggers a 10-second countdown.",
                "4. Tap Cancel in the notification to abort.",
                "5. After countdown: GPS location is acquired, SMS is sent to all contacts, and the first contact is called.",
            )
            steps.forEach { step ->
                Text(
                    text = step,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .semantics { contentDescription = step },
                )
            }
        }
    }
}
