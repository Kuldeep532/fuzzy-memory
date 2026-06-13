package com.nexuswavetech.nexusplus.features.totp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.delay
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// ── TOTP Math ─────────────────────────────────────────────────────────────────

private object TotpHelper {
    private const val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decodeBase32(input: String): ByteArray {
        val cleaned = input.uppercase().replace(Regex("[= \\-]"), "")
        var bits = 0
        var bitsLen = 0
        val out = mutableListOf<Byte>()
        for (ch in cleaned) {
            val idx = BASE32.indexOf(ch)
            if (idx < 0) continue
            bits = (bits shl 5) or idx
            bitsLen += 5
            if (bitsLen >= 8) {
                bitsLen -= 8
                out.add(((bits shr bitsLen) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }

    fun generateTotp(secret: String, timeMs: Long = System.currentTimeMillis(), period: Long = 30, digits: Int = 6): String {
        return runCatching {
            val key   = decodeBase32(secret)
            val step  = java.nio.ByteBuffer.allocate(8).putLong(timeMs / 1000 / period).array()
            val hmac  = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(key, "HmacSHA1")) }
            val hash  = hmac.doFinal(step)
            val off   = hash.last().toInt() and 0xF
            val code  = ((hash[off].toInt() and 0x7F) shl 24) or
                        ((hash[off + 1].toInt() and 0xFF) shl 16) or
                        ((hash[off + 2].toInt() and 0xFF) shl 8) or
                        (hash[off + 3].toInt() and 0xFF)
            (code % 1_000_000).toString().padStart(digits, '0')
        }.getOrDefault("------")
    }

    fun secondsRemaining(period: Long = 30): Int {
        return (period - System.currentTimeMillis() / 1000 % period).toInt()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpAuthenticatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view    = LocalView.current

    var accounts    by remember { mutableStateOf(listOf<TotpAccount>()) }
    var showAddSheet by remember { mutableStateOf(false) }
    var tick        by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title   = "Authenticator",
            onBack  = onBack,
            actions = {
                IconButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add TOTP account")
                }
            },
        )

        if (accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Security, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    Text("No accounts yet", style = MaterialTheme.typography.titleMedium)
                    Text("Tap + to add a TOTP account", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { showAddSheet = true }) { Text("Add Account") }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(accounts, key = { it.id }) { account ->
                    val code    = TotpHelper.generateTotp(account.secret, tick)
                    val secsLeft = TotpHelper.secondsRemaining()
                    TotpCard(
                        account  = account,
                        code     = code,
                        secsLeft = secsLeft,
                        onCopy   = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("OTP", code))
                            view.announceForAccessibility("Code copied")
                        },
                        onDelete = { accounts = accounts.filter { it.id != account.id } },
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTotpSheet(
            onAdd = { name, secret ->
                accounts = accounts + TotpAccount(
                    id     = java.util.UUID.randomUUID().toString(),
                    name   = name,
                    secret = secret,
                )
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false },
        )
    }
}

@Composable
private fun TotpCard(
    account:  TotpAccount,
    code:     String,
    secsLeft: Int,
    onCopy:   () -> Unit,
    onDelete: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue    = secsLeft / 30f,
        animationSpec  = tween(800, easing = LinearEasing),
        label          = "timer_progress",
    )
    val codeColor = when {
        secsLeft <= 5  -> MaterialTheme.colorScheme.error
        secsLeft <= 10 -> Color(0xFFFF9800)
        else           -> MaterialTheme.colorScheme.primary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier            = Modifier.padding(16.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Countdown ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                CircularProgressIndicator(
                    progress        = { progress },
                    color           = codeColor,
                    trackColor      = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth     = 4.dp,
                    modifier        = Modifier.size(48.dp),
                )
                Text("$secsLeft", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = codeColor)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text  = code.chunked(3).joinToString(" "),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily  = FontFamily.Monospace,
                        fontWeight  = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    ),
                    color = codeColor,
                    modifier = Modifier.semantics { contentDescription = "OTP code: ${code.map { it }.joinToString(" ")}" },
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete account", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTotpSheet(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name   by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var error  by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Add TOTP Account", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("Account name (e.g. Google)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = secret, onValueChange = { secret = it.uppercase().replace(" ", ""); error = null },
                label = { Text("Secret key (Base32)") }, singleLine = true,
                supportingText = { Text("From your 2FA setup QR code or text key") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = {
                    when {
                        name.isBlank()   -> error = "Account name required"
                        secret.isBlank() -> error = "Secret key required"
                        secret.length < 8 -> error = "Secret too short — check your key"
                        else -> {
                            runCatching { TotpHelper.generateTotp(secret) }
                                .onSuccess { onAdd(name.trim(), secret.trim()) }
                                .onFailure { error = "Invalid secret key — use the Base32 key from your service" }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add Account") }
        }
    }
}

// ── Model ─────────────────────────────────────────────────────────────────────

private data class TotpAccount(val id: String, val name: String, val secret: String)
