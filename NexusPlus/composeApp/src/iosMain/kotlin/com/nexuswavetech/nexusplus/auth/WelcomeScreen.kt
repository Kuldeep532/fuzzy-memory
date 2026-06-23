package com.nexuswavetech.nexusplus.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onNavigateToMain: () -> Unit,
    viewModel: WelcomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val privacyAccepted by viewModel.privacyAccepted.collectAsState()
    val termsAccepted by viewModel.termsAccepted.collectAsState()
    val legalGranted by viewModel.legalConsentGranted.collectAsState()

    LaunchedEffect(uiState.navigateToMain) {
        if (uiState.navigateToMain) {
            viewModel.onNavigationConsumed()
            onNavigateToMain()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D1A), Color(0xFF1A0A2E), Color(0xFF0A1A2E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "⚡",
                fontSize = 72.sp,
            )

            Text(
                text = "Nexus Plus",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Your all-in-one super-app",
                fontSize = 16.sp,
                color = Color(0xFF9090A8),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Legal Consent",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = privacyAccepted,
                            onCheckedChange = { viewModel.onPrivacyToggled(it) },
                        )
                        Text(
                            text = "I accept the Privacy Policy",
                            color = Color(0xFFBBBBCC),
                            fontSize = 13.sp,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { viewModel.onTermsToggled(it) },
                        )
                        Text(
                            text = "I accept the Terms & Conditions",
                            color = Color(0xFFBBBBCC),
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.onContinueAsGuestClicked() },
                enabled = legalGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C5CFC),
                )
            ) {
                Icon(Icons.Filled.Person, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Continue as Guest", fontWeight = FontWeight.SemiBold)
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (uiState.showGuestNameDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onGuestDialogDismissed() },
            title = { Text("Enter Your Name") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.guestName,
                        onValueChange = { viewModel.onGuestNameChanged(it) },
                        label = { Text("Your name") },
                        singleLine = true,
                        isError = uiState.guestNameError != null,
                        supportingText = uiState.guestNameError?.let { { Text(it) } },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onGuestNameSubmitted() }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onGuestDialogDismissed() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
