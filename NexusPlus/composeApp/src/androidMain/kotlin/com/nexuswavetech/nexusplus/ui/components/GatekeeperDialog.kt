package com.nexuswavetech.nexusplus.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*

@Composable
fun GatekeeperDialog(
    featureName: String,
    onSignInClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                "Full Account Required",
                modifier = Modifier.semantics { heading() }
            )
        },
        text = {
            Text(
                "\"$featureName\" requires a Google-authenticated account. " +
                "Sign in to unlock all premium features in Nexus Plus.",
                modifier = Modifier.semantics {
                    contentDescription = "$featureName requires Google Sign-In. " +
                        "This feature is restricted for guest users. " +
                        "Tap Sign In with Google to authenticate."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onSignInClicked,
                modifier = Modifier.semantics {
                    contentDescription = "Sign In with Google to unlock $featureName"
                    role = Role.Button
                }
            ) {
                Text("Sign In with Google")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    )
}
