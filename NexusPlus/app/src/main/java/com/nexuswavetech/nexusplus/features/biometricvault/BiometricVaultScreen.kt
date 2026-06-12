package com.nexuswavetech.nexusplus.features.biometricvault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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

private val Context.vaultStore by preferencesDataStore(name = "nexus_vault")

enum class VaultCategory(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PASSWORD("Passwords", Icons.Filled.Key),
    NOTE("Notes", Icons.Filled.StickyNote2),
    CARD("Cards", Icons.Filled.CreditCard),
    DOCUMENT("Documents", Icons.Filled.Description)
}

data class VaultItem(val id: String, val category: VaultCategory, val title: String, val secret: String)

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
                    category = VaultCategory.valueOf(obj.optString("category", "NOTE")),
                    title = obj.optString("title"),
                    secret = obj.optString("secret")
                )
            }
        }
    }

    fun saveItems(list: List<VaultItem>) {
        scope.launch {
            val arr = JSONArray()
            list.forEach { item ->
                arr.put(JSONObject().apply {
                    put("id", item.id); put("category", item.category.name)
                    put("title", item.title); put("secret", item.secret)
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
                        view.announceForAccessibility("Vault unlocked")
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
            // Fallback: allow access on device without biometrics (show warning)
            isUnlocked = true
            authError = "No biometrics enrolled — vault open without authentication"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Biometric Vault", onBack = onBack)

        if (!isUnlocked) {
            // Lock screen
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Fingerprint, contentDescription = null,
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    authError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = ::launchBiometric,
                        modifier = Modifier.semantics { contentDescription = "Unlock vault with biometrics" }
                    ) {
                        Icon(Icons.Filled.Fingerprint, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock Vault")
                    }
                }
            }
        } else {
            // Category filter
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
                    label = { Text("All") }
                )
                VaultCategory.values().forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) }
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "${item.category.label}: ${item.title}. Double tap to expand." }
                    ) {
                        ListItem(
                            leadingContent = { Icon(item.category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Text(if (showSecret) item.secret else "••••••••",
                                    style = MaterialTheme.typography.bodySmall)
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { showSecret = !showSecret }) {
                                        Icon(
                                            if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showSecret) "Hide" else "Show secret"
                                        )
                                    }
                                    IconButton(onClick = {
                                        items = items.filter { it.id != item.id }
                                        saveItems(items)
                                        view.announceForAccessibility("${item.title} deleted")
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${item.title}", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Item") },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(16.dp)
                    .semantics { contentDescription = "Add new vault item" }
            )
        }
    }

    // Add dialog
    if (showAddDialog) {
        var addTitle by remember { mutableStateOf("") }
        var addSecret by remember { mutableStateOf("") }
        var addCategory by remember { mutableStateOf(VaultCategory.PASSWORD) }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Vault Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VaultCategory.values().forEach { cat ->
                            FilterChip(selected = addCategory == cat, onClick = { addCategory = cat },
                                label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    OutlinedTextField(value = addTitle, onValueChange = { addTitle = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = addSecret, onValueChange = { addSecret = it }, label = { Text("Secret / Value") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (addTitle.isNotBlank()) {
                        val newItem = VaultItem(id = System.currentTimeMillis().toString(), category = addCategory, title = addTitle, secret = addSecret)
                        items = items + newItem
                        saveItems(items)
                        showAddDialog = false
                        view.announceForAccessibility("${addTitle} added to vault")
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}
