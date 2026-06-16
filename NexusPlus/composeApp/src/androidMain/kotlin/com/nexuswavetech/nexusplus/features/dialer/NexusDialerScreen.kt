package com.nexuswavetech.nexusplus.features.dialer

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhoneContact(
    val id: String,
    val name: String,
    val number: String,
)

private suspend fun loadContacts(resolver: ContentResolver): List<PhoneContact> =
    withContext(Dispatchers.IO) {
        val list = mutableListOf<PhoneContact>()
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )
        cursor?.use {
            val idIdx   = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx  = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                list += PhoneContact(
                    id     = it.getString(idIdx) ?: continue,
                    name   = it.getString(nameIdx) ?: "Unknown",
                    number = it.getString(numIdx) ?: continue,
                )
            }
        }
        list.distinctBy { it.number.filter(Char::isDigit) }
    }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NexusDialerScreen(onBack: () -> Unit) {
    val context      = LocalContext.current
    var dialNumber   by remember { mutableStateOf("") }
    var searchQuery  by remember { mutableStateOf("") }
    var contacts     by remember { mutableStateOf(listOf<PhoneContact>()) }
    var showContacts by remember { mutableStateOf(false) }

    val perms = rememberMultiplePermissionsState(
        listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
    )

    LaunchedEffect(perms.allPermissionsGranted) {
        if (perms.allPermissionsGranted) contacts = loadContacts(context.contentResolver)
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.number.contains(searchQuery)
        }
    }

    fun dial(number: String) {
        val clean = number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (clean.isBlank()) return
        val intent = if (perms.permissions.any { it.permission == Manifest.permission.CALL_PHONE && it.status.isGranted })
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$clean"))
        else
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "Nexus Dialer",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showContacts = !showContacts }) {
                        Icon(
                            if (showContacts) Icons.Filled.DialerSip else Icons.Filled.Contacts,
                            contentDescription = if (showContacts) "Show dialer" else "Show contacts"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!showContacts) {
                DialerPadSection(
                    number    = dialNumber,
                    onKey     = { dialNumber += it },
                    onDelete  = { if (dialNumber.isNotEmpty()) dialNumber = dialNumber.dropLast(1) },
                    onClear   = { dialNumber = "" },
                    onDial    = { dial(dialNumber) },
                )
            } else {
                if (!perms.allPermissionsGranted) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Contacts, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Contacts permission needed", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Allow access to view and call your contacts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { perms.launchMultiplePermissionRequest() }) {
                            Text("Grant Permission")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search contacts…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(filteredContacts, key = { it.id + it.number }) { contact ->
                            ListItem(
                                headlineContent = {
                                    Text(contact.name, fontWeight = FontWeight.Medium)
                                },
                                supportingContent = { Text(contact.number) },
                                leadingContent = {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(
                                            onClick = { dialNumber = contact.number; showContacts = false },
                                            modifier = Modifier.semantics { contentDescription = "Edit number for ${contact.name}" }
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = null,
                                                modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { dial(contact.number) },
                                            modifier = Modifier.semantics { contentDescription = "Call ${contact.name}" }
                                        ) {
                                            Icon(Icons.Filled.Call, contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dial(contact.number) }
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialerPadSection(
    number   : String,
    onKey    : (String) -> Unit,
    onDelete : () -> Unit,
    onClear  : () -> Unit,
    onDial   : () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // Number display
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = number.ifEmpty { "Enter number" },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                ),
                color = if (number.isEmpty()) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            if (number.isNotEmpty()) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.BackspaceOutlined, contentDescription = "Delete digit")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        // Dialer pad 3×4
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#"),
        )
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    DialKey(label = key, onClick = { onKey(key) })
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Dial button
        FloatingActionButton(
            onClick = onDial,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp).semantics { contentDescription = "Call" }
        ) {
            Icon(Icons.Filled.Call, contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimary)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DialKey(label: String, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        shape    = CircleShape,
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(72.dp)
            .semantics { contentDescription = "Dial $label" }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text  = label,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
