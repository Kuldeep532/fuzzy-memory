package com.nexuswavetech.nexusplus.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nexuswavetech.nexusplus.composeapp.R
import com.nexuswavetech.nexusplus.navigation.Screen
import com.nexuswavetech.nexusplus.ui.components.NexusWaveLogo
import com.nexuswavetech.nexusplus.remoteconfig.RemoteConfigRepository
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@Composable
fun WelcomeScreen(
    onNavigateToMain: () -> Unit,
    viewModel: WelcomeViewModel = koinViewModel(),
    navController: NavController? = null,
) {
    val uiState         by viewModel.uiState.collectAsState()
    val privacyAccepted by viewModel.privacyAccepted.collectAsState()
    val termsAccepted   by viewModel.termsAccepted.collectAsState()
    val consentGranted  by viewModel.legalConsentGranted.collectAsState()

    val remoteConfig: RemoteConfigRepository = koinInject()
    val googleSignInEnabled = remember { remoteConfig.googleSignInEnabled }

    val context = LocalContext.current
    val defaultWebClientId = stringResource(id = R.string.default_web_client_id)

    // ── Real Google Sign-In launcher ────────────────────────────────────────
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            runCatching {
                val task    = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.onGoogleSignInTokenReceived(it) }
                    ?: viewModel.onGoogleSignInError("No ID token — check Firebase SHA-1 fingerprint")
            }.onFailure { e ->
                viewModel.onGoogleSignInError("Sign-in failed (code ${(e as? ApiException)?.statusCode ?: "?"})")
            }
        } else {
            viewModel.onGoogleSignInError("Sign-in cancelled")
        }
    }

    LaunchedEffect(uiState.navigateToMain) {
        if (uiState.navigateToMain) {
            onNavigateToMain()
            viewModel.onNavigationConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            NexusWaveLogo(modifier = Modifier.size(100.dp))

            Text(
                text  = "Nexus Plus",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onPrimary,
                    fontSize   = 42.sp,
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier.semantics { heading() },
            )

            Text(
                text  = "Your all-in-one intelligent toolkit",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            LegalConsentSection(
                privacyAccepted  = privacyAccepted,
                termsAccepted    = termsAccepted,
                onPrivacyToggled = viewModel::onPrivacyToggled,
                onTermsToggled   = viewModel::onTermsToggled,
                onOpenPrivacy    = { navController?.navigate(Screen.PrivacyPolicy.route) },
                onOpenTerms      = { navController?.navigate(Screen.TermsConditions.route) },
            )

            // ── Google Sign-In button — visibility controlled by Firebase Remote Config ──
            if (googleSignInEnabled) GoogleSignInButton(
                isLoading = uiState.isLoading,
                enabled   = consentGranted,
                onClick   = {
                    val webClientId = defaultWebClientId
                    if (webClientId.isBlank() ||
                        webClientId == "YOUR_WEB_CLIENT_ID" ||
                        webClientId.startsWith("YOUR_")) {
                        viewModel.onGoogleSignInError(
                            "Firebase not set up yet. Please add google-services.json to enable Google Sign-In, or continue as Guest."
                        )
                        return@GoogleSignInButton
                    }
                    runCatching {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(webClientId)
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        client.signOut().addOnCompleteListener {
                            googleLauncher.launch(client.signInIntent)
                        }
                    }.onFailure { e ->
                        viewModel.onGoogleSignInError(
                            "Sign-in setup failed: ${e.localizedMessage ?: "Unknown error"}. Please continue as Guest."
                        )
                    }
                },
            )

            OutlinedButton(
                onClick  = viewModel::onContinueAsGuestClicked,
                enabled  = consentGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        contentDescription =
                            if (consentGranted) "Continue as Guest" else "Continue as Guest. Accept Privacy Policy and Terms first."
                    },
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor         = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = consentGranted).copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = if (consentGranted) 0.6f else 0.25f),
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = if (consentGranted) 0.6f else 0.25f),
                        )
                    )
                ),
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue as Guest", fontWeight = FontWeight.SemiBold)
            }

            if (uiState.error != null) {
                Text(
                    text  = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (uiState.showGuestNameDialog) {
            GuestNameDialog(
                name          = uiState.guestName,
                error         = uiState.guestNameError,
                onNameChanged = viewModel::onGuestNameChanged,
                onConfirm     = viewModel::onGuestNameSubmitted,
                onDismiss     = viewModel::onGuestDialogDismissed,
            )
        }
    }
}

// ── Compliance gate UI ─────────────────────────────────────────────────────

@Composable
private fun LegalConsentSection(
    privacyAccepted: Boolean,
    termsAccepted: Boolean,
    onPrivacyToggled: (Boolean) -> Unit,
    onTermsToggled: (Boolean) -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text  = "Before continuing, please accept:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Privacy Policy. ${if (privacyAccepted) "Accepted." else "Not yet accepted."}"
                        role = Role.Checkbox
                    },
            ) {
                Checkbox(
                    checked         = privacyAccepted,
                    onCheckedChange = onPrivacyToggled,
                    colors          = CheckboxDefaults.colors(
                        checkedColor   = MaterialTheme.colorScheme.secondary,
                        uncheckedColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    ),
                )
                Text("I have read the ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                Text(
                    text     = "Privacy Policy",
                    style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color    = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable(onClickLabel = "Open Privacy Policy", onClick = onOpenPrivacy),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Terms and Conditions. ${if (termsAccepted) "Accepted." else "Not yet accepted."}"
                        role = Role.Checkbox
                    },
            ) {
                Checkbox(
                    checked         = termsAccepted,
                    onCheckedChange = onTermsToggled,
                    colors          = CheckboxDefaults.colors(
                        checkedColor   = MaterialTheme.colorScheme.secondary,
                        uncheckedColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    ),
                )
                Text("I accept the ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                Text(
                    text     = "Terms & Conditions",
                    style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color    = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable(onClickLabel = "Open Terms and Conditions", onClick = onOpenTerms),
                )
            }
        }
    }
}

// ── Google Sign-In button ──────────────────────────────────────────────────

@Composable
private fun GoogleSignInButton(isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = !isLoading && enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics {
                contentDescription =
                    if (enabled) "Sign in with Google" else "Sign in with Google. Disabled until Privacy Policy and Terms are accepted."
                role = Role.Button
            },
        shape  = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor         = MaterialTheme.colorScheme.surface,
            contentColor           = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Text("G", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Sign in with Google", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

// ── Guest name dialog ──────────────────────────────────────────────────────

@Composable
private fun GuestNameDialog(
    name: String,
    error: String?,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(text = "Welcome, Guest!", modifier = Modifier.semantics { heading() }) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What should we call you?")
                OutlinedTextField(
                    value          = name,
                    onValueChange  = onNameChanged,
                    label          = { Text("Your name") },
                    isError        = error != null,
                    supportingText = { if (error != null) Text(error) },
                    singleLine     = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); onConfirm() }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Name input field." },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { keyboardController?.hide(); onConfirm() }, modifier = Modifier.semantics { contentDescription = "Enter Nexus Plus as guest" }) { Text("Enter App") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
