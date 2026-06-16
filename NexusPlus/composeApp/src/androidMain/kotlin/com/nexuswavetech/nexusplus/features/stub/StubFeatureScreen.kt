package com.nexuswavetech.nexusplus.features.stub

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

/**
 * Placeholder screen for extended features (7–30+) that share the same modular
 * slot in the navigation graph. Replace this composable with the real
 * feature implementation when it's built, without changing any routing code.
 */
@Composable
fun StubFeatureScreen(featureKey: String, onBack: () -> Unit) {
    val displayName = featureKey
        .replace("_", " ")
        .replaceFirstChar { it.uppercase() }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = displayName, onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "$displayName feature is under development. It will be available in a future update."
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Filled.Construction,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.semantics { heading() }
                )

                Text(
                    text = "This feature module is registered in the navigation graph and Koin DI. " +
                        "Replace this screen with the full implementation — no other code changes needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "Coming in a future update",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
