package com.nexuswavetech.nexusplus.features.encryptednotes

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptedNotesScreen(onBack: () -> Unit) {
    val context  = LocalContext.current
    val view     = LocalView.current
    val vm: EncryptedNotesViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    // Prevent screenshots for security
    DisposableEffect(Unit) {
        val window = (context as? ComponentActivity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    // Auto-launch biometric when screen opens
    LaunchedEffect(state.isUnlocked) {
        if (!state.isUnlocked) launchBiometric(context, vm)
    }

    var showAddDialog    by remember { mutableStateOf(false) }
    var editingNote      by remember { mutableStateOf<EncryptedNote?>(null) }
    var deleteCandidate  by remember { mutableStateOf<EncryptedNote?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Encrypted Notes", onBack = onBack)

        AnimatedVisibility(visible = !state.isUnlocked, enter = fadeIn(), exit = fadeOut()) {
            LockWall(authError = state.authError, onUnlock = { launchBiometric(context, vm) })
        }

        AnimatedVisibility(visible = state.isUnlocked, enter = fadeIn(), exit = fadeOut()) {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(onClick = { editingNote = EncryptedNote(); showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New Note")
                    }
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    // Search bar
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = vm::setQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search notes…") },
                        leadingIcon  = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { vm.setQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )

                    val notes = vm.filteredNotes
                    if (notes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (state.query.isEmpty()) "No encrypted notes yet.\nTap + to add one."
                                    else "No notes match \"${state.query}\".",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding    = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(notes, key = { it.id }) { note ->
                                NoteCard(
                                    note     = note,
                                    onEdit   = { editingNote = it; showAddDialog = true },
                                    onDelete = { deleteCandidate = it },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit dialog
    if (showAddDialog && editingNote != null) {
        NoteEditorDialog(
            initial = editingNote!!,
            onSave  = { vm.addOrUpdateNote(it); showAddDialog = false; editingNote = null },
            onDismiss = { showAddDialog = false; editingNote = null },
        )
    }

    // Delete confirmation
    deleteCandidate?.let { note ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            icon  = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete Note?") },
            text  = { Text("\"${note.title.ifBlank { "Untitled" }}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteNote(note.id); deleteCandidate = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LockWall(authError: String?, onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Encrypted Notes", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text(
                "Your notes are protected with AES-256-GCM encryption and biometric authentication.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (!authError.isNullOrEmpty()) {
                Text(authError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock with Biometrics")
            }
        }
    }
}

@Composable
private fun NoteCard(note: EncryptedNote, onEdit: (EncryptedNote) -> Unit, onDelete: (EncryptedNote) -> Unit) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    Card(
        onClick  = { onEdit(note) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Note: ${note.title.ifBlank { "Untitled" }}" },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (note.body.isNotBlank()) {
                    Text(note.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "Updated ${fmt.format(Date(note.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(onClick = { onDelete(note) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(
    initial:   EncryptedNote,
    onSave:    (EncryptedNote) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var body  by remember { mutableStateOf(initial.body)  }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.body.isEmpty() && initial.title.isEmpty()) "New Note" else "Edit Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body, onValueChange = { body = it },
                    label = { Text("Note") }, minLines = 4, maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { onSave(initial.copy(title = title.trim(), body = body.trim())) },
                enabled  = title.isNotBlank() || body.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun launchBiometric(context: android.content.Context, vm: EncryptedNotesViewModel) {
    val activity = context as? FragmentActivity ?: return
    val bm = BiometricManager.from(context)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    if (bm.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
        vm.onUnlockSuccess()
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { vm.onUnlockSuccess() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                vm.onAuthError(errString.toString())
            }
        }
        override fun onAuthenticationFailed() { vm.onAuthError("Authentication failed. Try again.") }
    })

    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Encrypted Notes")
            .setSubtitle("Authenticate to access your private notes")
            .setAllowedAuthenticators(authenticators)
            .build()
    )
}
