package com.nexuswavetech.nexusplus.features.biometricvault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.text.KeyboardOptions

private val Context.vaultStore by preferencesDataStore(name = "nexus_vault")

private val MinimumInteractiveModifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)

private enum class DocumentTag(val label: String) {
    VEHICLE("Vehicle"),
    PERSONAL("Personal")
}

enum class VaultCategory(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PASSWORD("Passwords", Icons.Filled.Key),
    NOTE("Notes", Icons.Filled.StickyNote2),
    CARD("Cards", Icons.Filled.CreditCard),
    DOCUMENT("Documents", Icons.Filled.Description)
}

data class VaultItem(
    val id: String,
    val category: VaultCategory,
    val title: String,
    val secret: String,
    val cardHolder: String = "",
    val cardNumber: String = "",
    val cardExpiry: String = "",
    val cardCvv: String = "",
    val docNumber: String = "",
    val docExpiry: String = "",
    val docTag: String = "",
    val fileUri: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricVaultScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var isUnlocked by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<VaultItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<VaultCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val vaultKey = stringPreferencesKey("vault_items")
    val storeFlow = remember {
        context.vaultStore.data.map { prefs -> prefs[vaultKey] ?: "[]" }
    }
    val rawJson by storeFlow.collectAsState(initial = "[]")

    LaunchedEffect(rawJson) {
        runCatching {
            val arr = JSONArray(rawJson)
            items = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                VaultItem(
                    id = obj.optString("id", i.toString()),
                    category = runCatching { VaultCategory.valueOf(obj.optString("category", "NOTE")) }.getOrDefault(VaultCategory.NOTE),
                    title = obj.optString("title"),
                    secret = obj.optString("secret"),
                    cardHolder = obj.optString("cardHolder"),
                    cardNumber = obj.optString("cardNumber"),
                    cardExpiry = obj.optString("cardExpiry"),
                    cardCvv = obj.optString("cardCvv"),
                    docNumber = obj.optString("docNumber"),
                    docExpiry = obj.optString("docExpiry"),
                    docTag = obj.optString("docTag"),
                    fileUri = obj.optString("fileUri")
                )
            }
        }
    }

    fun saveItems(list: List<VaultItem>) {
        scope.launch {
            val arr = JSONArray()
            list.forEach { item ->
                arr.put(JSONObject().apply {
                    put("id", item.id)
                    put("category", item.category.name)
                    put("title", item.title)
                    put("secret", item.secret)
                    put("cardHolder", item.cardHolder)
                    put("cardNumber", item.cardNumber)
                    put("cardExpiry", item.cardExpiry)
                    put("cardCvv", item.cardCvv)
                    put("docNumber", item.docNumber)
                    put("docExpiry", item.docExpiry)
                    put("docTag", item.docTag)
                    put("fileUri", item.fileUri)
                })
            }
            context.vaultStore.edit { it[vaultKey] = arr.toString() }
        }
    }

    fun launchBiometric() {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                context as FragmentActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        isUnlocked = true
                        view.announceForAccessibility("Vault unlocked successfully")
                    }
                    override fun onAuthenticationError(code: Int, msg: CharSequence) {
                        authError = msg.toString()
                    }
                    override fun onAuthenticationFailed() {
                        authError = "Authentication failed. Try again."
                    }
                }
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Nexus Biometric Vault")
                    .setSubtitle("Authenticate to access your secure vault")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()
            )
        } else {
            isUnlocked = true
            authError = "No biometrics enrolled — vault open without authentication"
            view.announceForAccessibility("Vault unlocked successfully. No biometrics are enrolled on this device.")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Biometric Vault", onBack = onBack)

        if (!isUnlocked) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Vault is Locked",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Authenticate with biometrics or device PIN to access your secure vault.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    authError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = ::launchBiometric,
                        modifier = Modifier.semantics { contentDescription = "Unlock vault with biometrics or device P-I-N" }
                    ) {
                        Icon(Icons.Filled.Fingerprint, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock Vault")
                    }
                }
            }
        } else {
            val filtered = items.filter { selectedCategory == null || it.category == selectedCategory }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") },
                    modifier = MinimumInteractiveModifier.semantics { contentDescription = "Show all vault categories" }
                )
                VaultCategory.values().forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) },
                        modifier = MinimumInteractiveModifier.semantics { contentDescription = "Filter vault items by ${cat.label}" }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No items yet. Tap + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(filtered, key = { it.id }) { item ->
                    var showSecret by remember { mutableStateOf(false) }
                    VaultItemCard(
                        item = item,
                        showSecret = showSecret,
                        onToggleSecret = { showSecret = !showSecret },
                        onDelete = {
                            val deletedTitle = item.title
                            items = items.filter { it.id != item.id }
                            saveItems(items)
                            view.announceForAccessibility("Item $deletedTitle deleted successfully")
                        }
                    )
                }
            }

            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Item") },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(16.dp)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Add new vault item" }
            )
        }
    }

    if (showAddDialog) {
        AddVaultItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                items = items + newItem
                saveItems(items)
                showAddDialog = false
                view.announceForAccessibility("Item ${newItem.title} added to vault successfully")
            }
        )
    }
}

@Composable
private fun VaultItemCard(
    item: VaultItem,
    showSecret: Boolean,
    onToggleSecret: () -> Unit,
    onDelete: () -> Unit
) {
    val details = when (item.category) {
        VaultCategory.CARD -> buildList {
            if (item.cardHolder.isNotBlank()) add("card holder ${item.cardHolder}")
            if (item.cardNumber.isNotBlank()) add("card ending in ${item.cardNumber.takeLast(4)}")
            if (item.cardExpiry.isNotBlank()) add("expires ${item.cardExpiry}")
        }.joinToString(", ").ifBlank { "card details saved" }
        VaultCategory.DOCUMENT -> buildList {
            if (item.docTag.isNotBlank()) add("${item.docTag} document")
            if (item.docNumber.isNotBlank()) add("number ${item.docNumber}")
            if (item.docExpiry.isNotBlank()) add("expires ${item.docExpiry}")
            if (item.fileUri.isNotBlank()) add("file attached")
        }.joinToString(", ").ifBlank { "document details saved" }
        else -> if (showSecret) item.secret else "secret hidden"
    }
    val announcement = "${item.category.label.removeSuffix("s")} item. Title ${item.title}. $details. Actions available: ${if (showSecret) "hide secret" else "show secret"} and delete."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = announcement }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(item.category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                when (item.category) {
                    VaultCategory.CARD -> {
                        Text(maskedCardLine(item), style = MaterialTheme.typography.bodySmall)
                        if (item.cardExpiry.isNotBlank()) Text("Expires ${item.cardExpiry}", style = MaterialTheme.typography.bodySmall)
                    }
                    VaultCategory.DOCUMENT -> {
                        Text(documentSummaryLine(item), style = MaterialTheme.typography.bodySmall)
                        if (item.docExpiry.isNotBlank()) Text("Expires ${item.docExpiry}", style = MaterialTheme.typography.bodySmall)
                    }
                    else -> Text(if (showSecret) item.secret else "••••••••", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row {
                IconButton(onClick = onToggleSecret, modifier = MinimumInteractiveModifier) {
                    Icon(
                        if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showSecret) "Hide secret for ${item.title}" else "Show secret for ${item.title}"
                    )
                }
                IconButton(onClick = onDelete, modifier = MinimumInteractiveModifier) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${item.title}", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AddVaultItemDialog(
    onDismiss: () -> Unit,
    onSave: (VaultItem) -> Unit
) {
    var addTitle by remember { mutableStateOf("") }
    var addSecret by remember { mutableStateOf("") }
    var addCategory by remember { mutableStateOf(VaultCategory.PASSWORD) }
    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var docNumber by remember { mutableStateOf("") }
    var docExpiry by remember { mutableStateOf("") }
    var docTag by remember { mutableStateOf(DocumentTag.PERSONAL.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Vault Item") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VaultCategory.values().forEach { cat ->
                        FilterChip(
                            selected = addCategory == cat,
                            onClick = { addCategory = cat },
                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = MinimumInteractiveModifier.semantics { contentDescription = "Choose ${cat.label} vault item type" }
                        )
                    }
                }
                AccessibleTextField(
                    value = addTitle,
                    onValueChange = { addTitle = it },
                    label = "Title",
                    contentDescription = "Vault item title input field",
                    singleLine = true
                )
                when (addCategory) {
                    VaultCategory.CARD -> {
                        AccessibleTextField(cardHolder, { cardHolder = it }, "Card Holder", "Card holder full name input field", singleLine = true)
                        AccessibleTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it.filter(Char::isDigit).take(16) },
                            label = "16-Digit Card Number",
                            contentDescription = "Sixteen digit card number input field. Numeric keyboard.",
                            keyboardType = KeyboardType.Number,
                            singleLine = true
                        )
                        AccessibleTextField(
                            value = cardExpiry,
                            onValueChange = { cardExpiry = it.take(5) },
                            label = "Expiry Date (MM/YY)",
                            contentDescription = "Card expiry date input field. M-M slash Y-Y format.",
                            keyboardType = KeyboardType.Number,
                            singleLine = true
                        )
                        AccessibleTextField(
                            value = cardCvv,
                            onValueChange = { cardCvv = it.filter(Char::isDigit).take(4) },
                            label = "CVV",
                            contentDescription = "C-V-V number input field. Numeric keyboard. Entry is hidden.",
                            keyboardType = KeyboardType.NumberPassword,
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                    }
                    VaultCategory.DOCUMENT -> {
                        AccessibleTextField(docNumber, { docNumber = it }, "Document / License ID Number", "Document or license I-D number input field", singleLine = true)
                        AccessibleTextField(docExpiry, { docExpiry = it }, "Expiry Date", "Document expiry date input field, if applicable", singleLine = true)
                        Text("Tag", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DocumentTag.values().forEach { tag ->
                                FilterChip(
                                    selected = docTag == tag.label,
                                    onClick = { docTag = tag.label },
                                    label = { Text(tag.label) },
                                    modifier = MinimumInteractiveModifier.semantics { contentDescription = "Select ${tag.label} document tag" }
                                )
                            }
                        }
                    }
                    VaultCategory.PASSWORD, VaultCategory.NOTE -> {
                        AccessibleTextField(
                            value = addSecret,
                            onValueChange = { addSecret = it },
                            label = if (addCategory == VaultCategory.PASSWORD) "Secret / Password" else "Secure Note",
                            contentDescription = if (addCategory == VaultCategory.PASSWORD) {
                                "Secret or password value input field"
                            } else {
                                "Secure note text input field"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (addTitle.isNotBlank()) {
                        onSave(
                            VaultItem(
                                id = System.currentTimeMillis().toString(),
                                category = addCategory,
                                title = addTitle.trim(),
                                secret = addSecret,
                                cardHolder = cardHolder,
                                cardNumber = cardNumber,
                                cardExpiry = cardExpiry,
                                cardCvv = cardCvv,
                                docNumber = docNumber,
                                docExpiry = docExpiry,
                                docTag = docTag,
                                fileUri = ""
                            )
                        )
                    }
                },
                modifier = MinimumInteractiveModifier.semantics { contentDescription = "Save new vault item" }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = MinimumInteractiveModifier.semantics { contentDescription = "Cancel adding vault item" }) { Text("Cancel") }
        }
    )
}

@Composable
private fun AccessibleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription }
    )
}

private fun maskedCardLine(item: VaultItem): String {
    val ending = item.cardNumber.takeLast(4)
    return when {
        item.cardHolder.isNotBlank() && ending.isNotBlank() -> "${item.cardHolder} •••• $ending"
        item.cardHolder.isNotBlank() -> item.cardHolder
        ending.isNotBlank() -> "•••• $ending"
        else -> "Card details saved"
    }
}

private fun documentSummaryLine(item: VaultItem): String {
    return listOf(item.docTag, item.docNumber)
        .filter { it.isNotBlank() }
        .joinToString(" • ")
        .ifBlank { "Document details saved" }
}
