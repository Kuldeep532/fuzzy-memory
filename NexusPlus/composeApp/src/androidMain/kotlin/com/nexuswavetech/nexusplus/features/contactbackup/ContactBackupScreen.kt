package com.nexuswavetech.nexusplus.features.contactbackup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── VCF Export helpers ─────────────────────────────────────────────────────

private data class Contact(
    val displayName: String,
    val phones:      List<String>,
    val emails:      List<String>,
    val org:         String,
)

private suspend fun loadContacts(cr: ContentResolver): List<Contact> =
    withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val cursor = cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ContactsContract.Contacts.HAS_PHONE_NUMBER),
            null, null, "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC",
        ) ?: return@withContext emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val id          = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val name        = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)) ?: continue
                val hasPhone    = it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))

                val phones = if (hasPhone > 0) {
                    val pc = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id), null,
                    )
                    val list = mutableListOf<String>()
                    pc?.use { c -> while (c.moveToNext()) list.add(c.getString(0) ?: "") }
                    list.filter { p -> p.isNotBlank() }
                } else emptyList()

                val emails = run {
                    val ec = cr.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(id), null,
                    )
                    val list = mutableListOf<String>()
                    ec?.use { c -> while (c.moveToNext()) list.add(c.getString(0) ?: "") }
                    list.filter { e -> e.isNotBlank() }
                }

                val org = run {
                    val oc = cr.query(
                        ContactsContract.Data.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Organization.COMPANY),
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(id, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE), null,
                    )
                    var company = ""
                    oc?.use { c -> if (c.moveToFirst()) company = c.getString(0) ?: "" }
                    company
                }

                contacts.add(Contact(name, phones, emails, org))
            }
        }
        contacts
    }

private fun Contact.toVcf(): String = buildString {
    appendLine("BEGIN:VCARD")
    appendLine("VERSION:3.0")
    appendLine("FN:$displayName")
    phones.forEach { appendLine("TEL;TYPE=CELL:$it") }
    emails.forEach { appendLine("EMAIL:$it") }
    if (org.isNotBlank()) appendLine("ORG:$org")
    appendLine("END:VCARD")
}

private suspend fun exportToVcf(context: Context, contacts: List<Contact>, uri: Uri): Int =
    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            contacts.forEach { writer.write(it.toVcf()) }
        }
        contacts.size
    }

private suspend fun countVcfContacts(context: Context, uri: Uri): Int =
    withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.lines().filter { it.trim() == "BEGIN:VCARD" }.count().toInt()
        } ?: 0
    }

// ── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ContactBackupScreen(onBack: () -> Unit) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val permission = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)

    var contactCount  by remember { mutableIntStateOf(-1) }
    var isLoading     by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError       by remember { mutableStateOf(false) }
    var contacts      by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val snackState    = remember { SnackbarHostState() }

    // File picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-vcard")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isLoading = true
            runCatching {
                val count = exportToVcf(context, contacts, uri)
                statusMessage = "✓ Exported $count contacts successfully"
                isError = false
                snackState.showSnackbar("Exported $count contacts to VCF")
            }.onFailure {
                statusMessage = "Export failed: ${it.message}"
                isError = true
            }
            isLoading = false
        }
    }

    // File picker for import (info-only, shows count)
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isLoading = true
            runCatching {
                val count = countVcfContacts(context, uri)
                statusMessage = "VCF file contains $count contacts. Use your phone's Contacts app to import this file."
                isError = false
                snackState.showSnackbar("$count contacts found in file")
            }.onFailure {
                statusMessage = "Could not read file: ${it.message}"
                isError = true
            }
            isLoading = false
        }
    }

    // Load contact count when permission granted
    LaunchedEffect(permission.status.isGranted) {
        if (permission.status.isGranted) {
            isLoading = true
            contacts     = loadContacts(context.contentResolver)
            contactCount = contacts.size
            isLoading    = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
    ) { innerPad ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPad)) {
            NexusTopBar(title = "Contact Backup", onBack = onBack)

            when {
                !permission.status.isGranted -> PermissionRequest(
                    onRequest = { permission.launchPermissionRequest() }
                )
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator()
                        Text("Loading contacts…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Stats card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(Icons.Filled.Contacts, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("$contactCount", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
                                Text("Contacts on device", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Export section
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Export Contacts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Text(
                                "Save all contacts as a VCF file. You can use this file to restore contacts on any Android or iOS device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    exportLauncher.launch("nexus_contacts_$stamp.vcf")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled  = contactCount > 0,
                            ) {
                                Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Export as VCF")
                            }
                        }
                    }

                    // Import section
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text("Restore Contacts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Text(
                                "Select a VCF backup file to see how many contacts it contains. Use your device's Contacts app to complete the import.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(
                                onClick  = { importLauncher.launch(arrayOf("text/x-vcard", "text/vcard", "*/*")) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open VCF File")
                            }
                        }
                    }

                    // Status message
                    statusMessage?.let { msg ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isError) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    // Info chip
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "VCF files are an industry-standard format supported by all major contact managers. Your contact data never leaves your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Filled.Contacts, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Contacts Permission Required", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Text(
                "Nexus Contact Backup needs access to your contacts to export them as a VCF backup file.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Grant Permission")
            }
        }
    }
}
