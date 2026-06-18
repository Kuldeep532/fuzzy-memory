package com.nexuswavetech.nexusplus.features.biometricvault

import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// ── Domain model ─────────────────────────────────────────────────────────────

enum class VaultCategory(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PASSWORD("Passwords",  Icons.Filled.Key),
    NOTE    ("Notes",      Icons.AutoMirrored.Filled.StickyNote2),
    CARD    ("Cards",      Icons.Filled.CreditCard),
    DOCUMENT("Documents",  Icons.Filled.Description),
}

private val documentSubTypes = listOf(
    "Aadhaar Card", "PAN Card", "Passport", "Driving Licence",
    "Voter ID (EPIC)", "Ration Card", "Health Insurance Card",
    "Vehicle Registration (RC)", "Income Tax / ITR", "Birth Certificate",
    "Educational Certificate", "GST Certificate", "Other",
)

data class VaultItem(
    val id:         String,
    val category:   VaultCategory,
    val title:      String,
    val secret:     String      = "",
    val cardHolder: String      = "",
    val cardNumber: String      = "",
    val cardExpiry: String      = "",
    val cardCvv:    String      = "",
    val docNumber:  String      = "",
    val docExpiry:  String      = "",
    val docTag:     String      = "",
    val docSubType: String      = "",
    val fileUri:    String      = "",
)

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricVaultScreen(onBack: () -> Unit) {
    val context  = LocalContext.current
    val view     = LocalView.current
    val scope    = rememberCoroutineScope()
    val haptic   = koinInject<HapticHelper>()
    val settings = koinInject<SettingsRepository>()
    val hapticEnabled by settings.touchVibration.collectAsState(initial = true)
    // Activity-scoped so the ViewModel survives navigation back/forward within the same task.
    // Previously per-backstack-entry, which caused a re-lock on every navigate().
    val activity = context as FragmentActivity
    val vm: BiometricVaultViewModel = koinViewModel(viewModelStoreOwner = activity)
    val state by vm.state.collectAsStateWithLifecycle()

    // FLAG_SECURE — blocks screenshots and recent-apps thumbnails
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    var showAddSheet by remember { mutableStateOf(false) }

    fun launchBiometric() {
        val bm = BiometricManager.from(context)
        val canAuth = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) {
                        vm.onUnlockSuccess()
                        view.announceForAccessibility("Vault unlocked successfully")
                    }
                    override fun onAuthenticationError(code: Int, msg: CharSequence) { vm.onAuthError(msg.toString()) }
                    override fun onAuthenticationFailed() { vm.onAuthError("Authentication failed. Try again.") }
                }
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Nexus Biometric Vault")
                    .setSubtitle("Authenticate to access your encrypted vault")
                    .setDescription("All data is AES-256-GCM encrypted via Android Keystore")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()
            )
        } else {
            vm.onUnlockSuccess()
            vm.onAuthError("No biometrics enrolled — vault opened with device fallback")
            view.announceForAccessibility("Vault unlocked. No biometrics enrolled.")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title   = "Biometric Vault",
            onBack  = onBack,
            actions = {
                if (state.isUnlocked) {
                    if (state.sessionSecsLeft >= 0) {
                        // Show countdown only when auto-lock is enabled (sessionSecsLeft >= 0)
                        val mins = state.sessionSecsLeft / 60
                        val secs = state.sessionSecsLeft % 60
                        Text(
                            "%d:%02d".format(mins, secs),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.sessionSecsLeft < 60) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 8.dp).semantics {
                                contentDescription = "Auto-lock in ${state.sessionSecsLeft} seconds"
                            },
                        )
                    }
                    IconButton(onClick = { vm.lock(); view.announceForAccessibility("Vault locked") }) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock vault now")
                    }
                }
            },
        )

        AnimatedVisibility(visible = !state.isUnlocked, enter = fadeIn(), exit = fadeOut()) {
            // ── Locked screen ─────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(Icons.Filled.Fingerprint, null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Vault is Locked", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Authenticate with biometrics or device PIN to access your AES-256-GCM encrypted vault.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    // Security badge
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                            Text("Hardware-backed AES-256-GCM encryption", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }

                    state.authError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }

                    Button(onClick = { haptic.click(view, hapticEnabled); launchBiometric() }, modifier = Modifier.semantics { contentDescription = "Unlock vault with biometrics or device PIN" }) {
                        Icon(Icons.Filled.Fingerprint, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock Vault")
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.isUnlocked, enter = fadeIn(), exit = fadeOut()) {
            // ── Unlocked screen ───────────────────────────────────────────
            val filtered = remember(state.items, state.selectedCategory) {
                if (state.selectedCategory == null) state.items else state.items.filter { it.category == state.selectedCategory }
            }

            Scaffold(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick  = { haptic.click(view, hapticEnabled); showAddSheet = true; vm.onUserActivity() },
                        icon     = { Icon(Icons.Filled.Add, null) },
                        text     = { Text("Add Item") },
                        modifier = Modifier.semantics { contentDescription = "Add new vault item" },
                    )
                }
            ) { innerPadding ->
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    // Category filter chips
                    LazyRow(
                        modifier          = Modifier.fillMaxWidth(),
                        contentPadding    = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedCategory == null,
                                onClick  = { vm.setCategory(null); vm.onUserActivity() },
                                label    = { Text("All (${state.items.size})") },
                            )
                        }
                        items(VaultCategory.entries.toList()) { cat ->
                            val count = state.items.count { it.category == cat }
                            FilterChip(
                                selected = state.selectedCategory == cat,
                                onClick  = { vm.setCategory(cat); vm.onUserActivity() },
                                label    = { Text("${cat.label} ($count)") },
                                leadingIcon = { Icon(cat.icon, null, modifier = Modifier.size(16.dp)) },
                            )
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier         = Modifier.fillMaxSize().semantics { contentDescription = "No vault items. Tap Add Item to store a secret." },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Text("No items yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Tap + to add passwords, notes, cards\nor government documents.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier       = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                var showSecret by remember { mutableStateOf(false) }
                                VaultItemCard(
                                    item           = item,
                                    showSecret     = showSecret,
                                    onToggleSecret = { showSecret = !showSecret; vm.onUserActivity() },
                                    onDelete       = {
                                        vm.deleteItem(item.id)
                                        view.announceForAccessibility("${item.title} deleted from vault")
                                    },
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddVaultItemSheet(
            onDismiss = { showAddSheet = false },
            onSave    = { newItem ->
                scope.launch { vm.addItem(newItem) }
                showAddSheet = false
                view.announceForAccessibility("${newItem.title} added to vault")
            },
        )
    }
}

// ── Vault item card ───────────────────────────────────────────────────────────

@Composable
private fun VaultItemCard(
    item: VaultItem,
    showSecret: Boolean,
    onToggleSecret: () -> Unit,
    onDelete: () -> Unit,
) {
    val desc = buildString {
        append("${item.category.label.removeSuffix("s")} item: ${item.title}. ")
        when (item.category) {
            VaultCategory.CARD     -> append("Card ending in ${item.cardNumber.takeLast(4).ifBlank { "hidden" }}.")
            VaultCategory.DOCUMENT -> append("${item.docSubType.ifBlank { item.docTag }.ifBlank { "Document" }}. Number: ${item.docNumber.ifBlank { "hidden" }}.")
            else                   -> append(if (showSecret) "Secret visible." else "Secret hidden.")
        }
    }
    Card(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = desc }) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(item.category.icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                when (item.category) {
                    VaultCategory.CARD -> {
                        Text(
                            buildString {
                                if (item.cardHolder.isNotBlank()) append(item.cardHolder)
                                if (item.cardNumber.isNotBlank()) append(" · •••• ${item.cardNumber.takeLast(4)}")
                            }.trim(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (item.cardExpiry.isNotBlank()) Text("Expires ${item.cardExpiry}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VaultCategory.DOCUMENT -> {
                        if (item.docSubType.isNotBlank()) Text(item.docSubType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (showSecret && item.docNumber.isNotBlank()) Text(item.docNumber, style = MaterialTheme.typography.bodySmall)
                        else if (!showSecret && item.docNumber.isNotBlank()) Text("•••••••••", style = MaterialTheme.typography.bodySmall)
                        if (item.docExpiry.isNotBlank()) Text("Expires ${item.docExpiry}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> Text(if (showSecret) item.secret else "••••••••••", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row {
                IconButton(onClick = onToggleSecret) {
                    Icon(
                        if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showSecret) "Hide ${item.title}" else "Reveal ${item.title}",
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${item.title}", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ── Add item bottom sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVaultItemSheet(onDismiss: () -> Unit, onSave: (VaultItem) -> Unit) {
    var category    by remember { mutableStateOf(VaultCategory.PASSWORD) }
    var title       by remember { mutableStateOf("") }
    var secret      by remember { mutableStateOf("") }
    var cardHolder  by remember { mutableStateOf("") }
    var cardNumber  by remember { mutableStateOf("") }
    var cardExpiry  by remember { mutableStateOf("") }
    var cardCvv     by remember { mutableStateOf("") }
    var docNumber   by remember { mutableStateOf("") }
    var docExpiry   by remember { mutableStateOf("") }
    var docSubType  by remember { mutableStateOf(documentSubTypes.first()) }
    var docExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Add to Vault", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            // Category selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VaultCategory.entries.toList()) { cat ->
                    FilterChip(
                        selected    = category == cat,
                        onClick     = { category = cat },
                        label       = { Text(cat.label, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = { Icon(cat.icon, null, modifier = Modifier.size(16.dp)) },
                    )
                }
            }

            // Title field (common)
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )

            // Category-specific fields
            when (category) {
                VaultCategory.PASSWORD, VaultCategory.NOTE -> {
                    OutlinedTextField(
                        value         = secret,
                        onValueChange = { secret = it },
                        label         = { Text(if (category == VaultCategory.PASSWORD) "Password / Secret" else "Secure Note") },
                        minLines      = if (category == VaultCategory.NOTE) 3 else 1,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }
                VaultCategory.CARD -> {
                    OutlinedTextField(value = cardHolder, onValueChange = { cardHolder = it }, label = { Text("Card Holder") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value           = cardNumber,
                        onValueChange   = { cardNumber = it.filter(Char::isDigit).take(16) },
                        label           = { Text("Card Number (16 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value           = cardExpiry,
                            onValueChange   = { cardExpiry = it.take(5) },
                            label           = { Text("Expiry (MM/YY)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine      = true,
                            modifier        = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value                  = cardCvv,
                            onValueChange          = { cardCvv = it.filter(Char::isDigit).take(4) },
                            label                  = { Text("CVV") },
                            keyboardOptions        = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation   = PasswordVisualTransformation(),
                            singleLine             = true,
                            modifier               = Modifier.weight(1f),
                        )
                    }
                }
                VaultCategory.DOCUMENT -> {
                    // Document sub-type dropdown
                    ExposedDropdownMenuBox(expanded = docExpanded, onExpandedChange = { docExpanded = it }) {
                        OutlinedTextField(
                            value         = docSubType,
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Document Type") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(docExpanded) },
                            modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = docExpanded, onDismissRequest = { docExpanded = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                            documentSubTypes.forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { docSubType = type; docExpanded = false })
                            }
                        }
                    }
                    OutlinedTextField(value = docNumber, onValueChange = { docNumber = it }, label = { Text("ID / Document Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = docExpiry, onValueChange = { docExpiry = it }, label = { Text("Expiry Date (if applicable)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            // Save / Cancel
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick  = {
                        if (title.isNotBlank()) {
                            onSave(VaultItem(
                                id         = System.currentTimeMillis().toString(),
                                category   = category,
                                title      = title.trim(),
                                secret     = secret,
                                cardHolder = cardHolder,
                                cardNumber = cardNumber,
                                cardExpiry = cardExpiry,
                                cardCvv    = cardCvv,
                                docNumber  = docNumber,
                                docExpiry  = docExpiry,
                                docSubType = docSubType,
                            ))
                        }
                    },
                    enabled  = title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("Save to Vault") }
            }
        }
    }
}
