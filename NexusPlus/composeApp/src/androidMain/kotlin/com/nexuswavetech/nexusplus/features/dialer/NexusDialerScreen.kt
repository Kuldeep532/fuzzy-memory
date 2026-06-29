package com.nexuswavetech.nexusplus.features.dialer

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
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
                title = "Dialer",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showContacts = !showContacts },
                        modifier = Modifier.semantics {
                            contentDescription = if (showContacts) "Switch to dial pad" else "Show contacts"
                        }
                    ) {
                        AnimatedContent(showContacts, label = "tab") { isContacts ->
                            Icon(
                                if (isContacts) Icons.Filled.DialerSip else Icons.Filled.Contacts,
                                contentDescription = null,
                            )
                        }
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
            // Tab indicator strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                DialerTab(
                    label = "Keypad",
                    icon = Icons.Filled.DialerSip,
                    selected = !showContacts,
                    onClick = { showContacts = false },
                    modifier = Modifier.weight(1f),
                )
                DialerTab(
                    label = "Contacts",
                    icon = Icons.Filled.Contacts,
                    selected = showContacts,
                    onClick = { showContacts = true },
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AnimatedContent(
                targetState = showContacts,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "dialer_content",
            ) { isContacts ->
                if (!isContacts) {
                    DialerPadSection(
                        number   = dialNumber,
                        onKey    = { dialNumber += it },
                        onDelete = { if (dialNumber.isNotEmpty()) dialNumber = dialNumber.dropLast(1) },
                        onClear  = { dialNumber = "" },
                        onDial   = { dial(dialNumber) },
                    )
                } else {
                    ContactsSection(
                        perms            = perms,
                        searchQuery      = searchQuery,
                        onSearchChanged  = { searchQuery = it },
                        filteredContacts = filteredContacts,
                        onEditNumber     = { num -> dialNumber = num; showContacts = false },
                        onDial           = { num -> dial(num) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DialerTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Number display
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = number.ifEmpty { "Enter number" },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                    ),
                    color = if (number.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = if (number.isEmpty()) "No number entered" else "Number: $number" },
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedVisibility(visible = number.isNotEmpty()) {
                    Row {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.semantics { contentDescription = "Delete last digit" }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.semantics { contentDescription = "Clear all digits" }
                        ) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Dialer pad 3×4
        val keys = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to ""),
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { (key, sub) ->
                    DialKey(
                        label = key,
                        sublabel = sub,
                        onClick = { onKey(key) },
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Call button
        Surface(
            onClick = onDial,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(76.dp)
                .semantics { contentDescription = "Call ${number.ifBlank { "number" }}" },
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DialKey(
    label: String,
    sublabel: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(74.dp)
            .semantics { contentDescription = "Dial $label" },
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (sublabel.isNotBlank()) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ContactsSection(
    perms: com.google.accompanist.permissions.MultiplePermissionsState,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    filteredContacts: List<PhoneContact>,
    onEditNumber: (String) -> Unit,
    onDial: (String) -> Unit,
) {
    if (!perms.allPermissionsGranted) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Contacts,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    "Contacts Permission Needed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Allow Nexus Dialer to access your contacts to view and call them directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = { perms.launchMultiplePermissionRequest() },
                    modifier = Modifier.height(52.dp),
                ) {
                    Icon(Icons.Filled.Lock, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Permission", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            placeholder = { Text("Search contacts…") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Filled.Clear, "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
        )

        if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.PersonSearch,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Text(
                        if (searchQuery.isBlank()) "No contacts found" else "No results for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(filteredContacts, key = { it.id + it.number }) { contact ->
                    ContactItem(
                        contact  = contact,
                        onEdit   = { onEditNumber(contact.number) },
                        onDial   = { onDial(contact.number) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: PhoneContact,
    onEdit: () -> Unit,
    onDial: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Call ${contact.name}",
                onClick = onDial,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                contact.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                contact.number,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Actions
        Row {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.semantics { contentDescription = "Edit number for ${contact.name}" },
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Surface(
                onClick = onDial,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = "Call ${contact.name}" },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
