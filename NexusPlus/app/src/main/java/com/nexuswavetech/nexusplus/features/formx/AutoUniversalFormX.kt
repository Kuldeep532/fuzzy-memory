package com.nexuswavetech.nexusplus.features.formx

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp

// ────────────────────────────────────────────────────────────────
// Reactive validation state — computed instantly from raw field values.
// No manual toggles or switches required; every field locks/unlocks/adapts
// automatically based on content validity.
// ────────────────────────────────────────────────────────────────

private enum class PasswordStrength(val label: String, val color: Color, val fraction: Float) {
    EMPTY("", Color.Transparent, 0f),
    WEAK("Weak", Color(0xFFEF5350), 0.25f),
    FAIR("Fair", Color(0xFFFF9800), 0.6f),
    STRONG("Strong", Color(0xFF43A047), 1f)
}

private data class FormXState(
    val email: String = "",
    val password: String = "",
    val specData: String = "",
    val emailTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val specDataTouched: Boolean = false,
    val passwordVisible: Boolean = false,
    val submitted: Boolean = false
) {
    val emailValid: Boolean
        get() = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    val passwordStrength: PasswordStrength
        get() {
            if (password.isEmpty()) return PasswordStrength.EMPTY
            var score = 0
            if (password.length >= 8) score++
            if (password.any { it.isUpperCase() }) score++
            if (password.any { it.isDigit() }) score++
            if (password.any { "!@#$%^&*()_+-=[]{}|;':,.<>?".contains(it) }) score++
            return when {
                score <= 1 -> PasswordStrength.WEAK
                score == 2 || score == 3 -> PasswordStrength.FAIR
                else -> PasswordStrength.STRONG
            }
        }

    val passwordValid: Boolean
        get() = passwordStrength == PasswordStrength.STRONG || passwordStrength == PasswordStrength.FAIR

    // Antibiotic Web Specification Data:
    // Format: "DrugName | Dosage | Route | Duration"
    // Example: "Amoxicillin | 500mg | Oral | 7 days"
    val specDataValid: Boolean
        get() {
            val parts = specData.split("|").map { it.trim() }
            return parts.size == 4 && parts.all { it.length >= 2 }
        }

    val showEmailError: Boolean get() = emailTouched && !emailValid
    val showPasswordError: Boolean get() = passwordTouched && !passwordValid
    val showSpecDataError: Boolean get() = specDataTouched && !specDataValid

    val allValid: Boolean get() = emailValid && passwordValid && specDataValid
}

// ────────────────────────────────────────────────────────────────
// AutoUniversalFormX — Material 3 reactive form.
//
// Fields auto-lock, unlock, and display validation state instantly without
// any manual switches. The submit button is gated on all-fields-valid.
// ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoUniversalFormX(
    onBack: () -> Unit,
    onSubmitSuccess: (email: String) -> Unit = {}
) {
    var state by remember { mutableStateOf(FormXState()) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Form X",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Header ───────────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            "Universal Form X",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "All fields validate automatically — no manual switches",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // ── Email field ───────────────────────────────────────────────────
            FormXSection(title = "Email Address", icon = Icons.Filled.Email) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { state = state.copy(email = it, emailTouched = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Email address input" },
                    label = { Text("Email") },
                    placeholder = { Text("user@example.com") },
                    singleLine = true,
                    isError = state.showEmailError,
                    supportingText = {
                        AnimatedVisibility(visible = state.showEmailError) {
                            Text(
                                "Enter a valid email address (e.g. user@domain.com)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        AnimatedVisibility(visible = state.emailTouched && state.emailValid) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF43A047),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Valid email",
                                    color = Color(0xFF43A047),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        AnimatedContent(
                            targetState = when {
                                state.emailTouched && state.emailValid -> "ok"
                                state.showEmailError -> "err"
                                else -> "none"
                            },
                            label = "email-icon"
                        ) { s ->
                            when (s) {
                                "ok" -> Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF43A047))
                                "err" -> Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                                else -> {}
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // ── Password field ────────────────────────────────────────────────
            FormXSection(title = "Password", icon = Icons.Filled.Lock) {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { state = state.copy(password = it, passwordTouched = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Password input" },
                    label = { Text("Password") },
                    placeholder = { Text("Min 8 chars, upper, digit, symbol") },
                    singleLine = true,
                    isError = state.showPasswordError,
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { state = state.copy(passwordVisible = !state.passwordVisible) }
                        ) {
                            Icon(
                                if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (state.passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    supportingText = {
                        AnimatedVisibility(visible = state.showPasswordError) {
                            Text(
                                "Password must be at least 'Fair' strength",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp)
                )

                // Reactive strength indicator — updates on every keystroke
                AnimatedVisibility(visible = state.password.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { state.passwordStrength.fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = state.passwordStrength.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Strength: ${state.passwordStrength.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = state.passwordStrength.color
                            )
                            PasswordHintChips()
                        }
                    }
                }
            }

            // ── Antibiotic Web Specification Data field ───────────────────────
            FormXSection(
                title = "Antibiotic Web Specification Data",
                icon = Icons.Filled.Biotech
            ) {
                OutlinedTextField(
                    value = state.specData,
                    onValueChange = { state = state.copy(specData = it, specDataTouched = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Antibiotic specification data input" },
                    label = { Text("Specification Data") },
                    placeholder = { Text("DrugName | Dosage | Route | Duration") },
                    minLines = 3,
                    maxLines = 6,
                    isError = state.showSpecDataError,
                    supportingText = {
                        AnimatedVisibility(visible = state.showSpecDataError) {
                            Text(
                                "Format: DrugName | Dosage | Route | Duration\ne.g. Amoxicillin | 500mg | Oral | 7 days",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        AnimatedVisibility(visible = state.specDataTouched && state.specDataValid) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle, null,
                                    tint = Color(0xFF43A047),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Valid specification",
                                    color = Color(0xFF43A047),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        AnimatedContent(
                            targetState = when {
                                state.specDataTouched && state.specDataValid -> "ok"
                                state.showSpecDataError -> "err"
                                else -> "none"
                            },
                            label = "spec-icon"
                        ) { s ->
                            when (s) {
                                "ok" -> Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF43A047))
                                "err" -> Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                                else -> {}
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp)
                )

                // Auto-populated field breakdown when valid
                AnimatedVisibility(visible = state.specDataValid) {
                    val parts = state.specData.split("|").map { it.trim() }
                    if (parts.size == 4) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Parsed Specification",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                SpecRow("Drug Name", parts[0])
                                SpecRow("Dosage", parts[1])
                                SpecRow("Route", parts[2])
                                SpecRow("Duration", parts[3])
                            }
                        }
                    }
                }
            }

            // ── Reactive form status banner ───────────────────────────────────
            AnimatedVisibility(
                visible = state.emailTouched || state.passwordTouched || state.specDataTouched
            ) {
                val ready = state.allValid
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (ready) Color(0xFF43A047).copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (ready) Icons.Filled.TaskAlt else Icons.Filled.PendingActions,
                            contentDescription = null,
                            tint = if (ready) Color(0xFF43A047) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (ready) "All fields valid — form ready to submit"
                            else {
                                val remaining = listOfNotNull(
                                    if (!state.emailValid) "email" else null,
                                    if (!state.passwordValid) "password" else null,
                                    if (!state.specDataValid) "specification data" else null
                                )
                                "Fix: ${remaining.joinToString(", ")}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ready) Color(0xFF43A047) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ── Submit button — auto-enabled only when ALL fields valid ────────
            Button(
                onClick = {
                    state = state.copy(
                        emailTouched = true,
                        passwordTouched = true,
                        specDataTouched = true
                    )
                    if (state.allValid) {
                        state = state.copy(submitted = true)
                        onSubmitSuccess(state.email)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Submit Form X" },
                enabled = state.allValid,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Submit",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // ── Reset link ────────────────────────────────────────────────────
            TextButton(
                onClick = { state = FormXState() },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics { contentDescription = "Reset form" }
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Reset all fields")
            }

            // ── Success state ─────────────────────────────────────────────────
            AnimatedVisibility(visible = state.submitted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF43A047).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF43A047),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "Submitted successfully",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF43A047)
                        )
                        Text(
                            "Form X data accepted for ${state.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Reusable sub-composables
// ────────────────────────────────────────────────────────────────

@Composable
private fun FormXSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun PasswordHintChips() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("A-Z", "0-9", "!@#").forEach { hint ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
