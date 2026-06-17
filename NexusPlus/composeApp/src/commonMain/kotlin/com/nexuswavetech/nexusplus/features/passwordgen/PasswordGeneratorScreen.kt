package com.nexuswavetech.nexusplus.features.passwordgen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import org.koin.compose.koinViewModel
import kotlin.math.roundToInt

data class PasswordConfig(
    val length: Int = 16,
    val useUppercase: Boolean = true,
    val useLowercase: Boolean = true,
    val useDigits: Boolean = true,
    val useSymbols: Boolean = true,
    val excludeAmbiguous: Boolean = false
)

class PasswordGeneratorViewModel : ViewModel() {
    var config   by mutableStateOf(PasswordConfig())
        private set
    var password by mutableStateOf("")
        private set
    var strength by mutableStateOf(0)
        private set

    init { generate() }

    fun updateConfig(newConfig: PasswordConfig) { config = newConfig; generate() }

    fun generate() {
        var charset = ""
        if (config.useUppercase)  charset += if (config.excludeAmbiguous) "ABCDEFGHJKLMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (config.useLowercase)  charset += if (config.excludeAmbiguous) "abcdefghjkmnpqrstuvwxyz"  else "abcdefghijklmnopqrstuvwxyz"
        if (config.useDigits)     charset += if (config.excludeAmbiguous) "23456789"                  else "0123456789"
        if (config.useSymbols)    charset += "!@#\$%^&*()-_=+[]{}|;:,.<>?"
        if (charset.isBlank())    charset  = "abcdefghijklmnopqrstuvwxyz"

        password = (1..config.length).map { charset.random() }.joinToString("")

        var score = 0
        if (config.length >= 12)                        score++
        if (config.length >= 20)                        score++
        if (config.useUppercase && config.useLowercase) score++
        if (config.useDigits)                           score++
        if (config.useSymbols)                          score++
        strength = score.coerceAtMost(4)
    }
}

private val strengthLabels = listOf("Very Weak", "Weak", "Fair", "Strong", "Very Strong")
private val strengthColors = listOf(
    androidx.compose.ui.graphics.Color(0xFFEF5350),
    androidx.compose.ui.graphics.Color(0xFFFF7043),
    androidx.compose.ui.graphics.Color(0xFFFFCA28),
    androidx.compose.ui.graphics.Color(0xFF66BB6A),
    androidx.compose.ui.graphics.Color(0xFF43A047)
)

@Composable
fun PasswordGeneratorScreen(onBack: () -> Unit, viewModel: PasswordGeneratorViewModel = koinViewModel()) {
    val clipboard = LocalClipboardManager.current
    val c         = viewModel.config

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Password Generator", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text     = viewModel.password,
                        style    = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.semantics {
                            contentDescription = "Generated password: ${viewModel.password}"
                        }
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            strengthLabels[viewModel.strength],
                            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color    = strengthColors[viewModel.strength],
                            modifier = Modifier.semantics {
                                contentDescription = "Password strength: ${strengthLabels[viewModel.strength]}"
                            }
                        )
                        Row {
                            IconButton(
                                onClick  = { clipboard.setText(AnnotatedString(viewModel.password)) },
                                modifier = Modifier.semantics { contentDescription = "Copy password to clipboard" }
                            ) { Icon(Icons.Filled.ContentCopy, null) }
                            IconButton(
                                onClick  = { viewModel.generate() },
                                modifier = Modifier.semantics { contentDescription = "Generate new password" }
                            ) { Icon(Icons.Filled.Refresh, null) }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (viewModel.strength + 1) / 5f },
                        modifier = Modifier.fillMaxWidth(),
                        color    = strengthColors[viewModel.strength]
                    )
                }
            }

            HorizontalDivider()
            Text(
                "Configuration",
                style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )

            Column {
                Text(
                    "Length: ${c.length} characters",
                    modifier = Modifier.semantics { contentDescription = "Password length: ${c.length} characters" }
                )
                Slider(
                    value         = c.length.toFloat(),
                    onValueChange = { viewModel.updateConfig(c.copy(length = it.roundToInt())) },
                    valueRange    = 6f..64f,
                    steps         = 57,
                    modifier      = Modifier.semantics { contentDescription = "Password length slider. Current: ${c.length}" }
                )
            }

            listOf(
                Triple("Uppercase letters (A–Z)", c.useUppercase)    { v: Boolean -> viewModel.updateConfig(c.copy(useUppercase = v)) },
                Triple("Lowercase letters (a–z)", c.useLowercase)    { v: Boolean -> viewModel.updateConfig(c.copy(useLowercase = v)) },
                Triple("Numbers (0–9)",           c.useDigits)        { v: Boolean -> viewModel.updateConfig(c.copy(useDigits = v)) },
                Triple("Symbols (!@#\$…)",         c.useSymbols)      { v: Boolean -> viewModel.updateConfig(c.copy(useSymbols = v)) },
                Triple("Exclude ambiguous (0, O, l, 1…)", c.excludeAmbiguous) { v: Boolean -> viewModel.updateConfig(c.copy(excludeAmbiguous = v)) }
            ).forEach { (label, checked, onToggle) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$label. ${if (checked) "Enabled" else "Disabled"}."
                        },
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = checked, onCheckedChange = onToggle)
                }
            }

            Button(
                onClick  = { viewModel.generate() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.Key, null)
                Spacer(Modifier.width(8.dp))
                Text("Generate New Password")
            }
        }
    }
}
