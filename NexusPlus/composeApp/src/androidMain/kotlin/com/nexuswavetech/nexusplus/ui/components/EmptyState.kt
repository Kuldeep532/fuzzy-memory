package com.nexuswavetech.nexusplus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable empty-state composable.
 * Use for: no search results, empty favorites, no recent activity, etc.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier          = modifier.semantics {
            contentDescription = "$title. $subtitle"
        },
        contentAlignment  = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(72.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            )
            Text(
                text      = title,
                style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text      = subtitle,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(Modifier.height(4.dp))
                action()
            }
        }
    }
}

/**
 * Reusable loading-state composable.
 */
@Composable
fun LoadingState(
    message: String = "Loading…",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier         = modifier.semantics { contentDescription = message },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Reusable error-state composable.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier         = modifier.semantics { contentDescription = "Error: $message" },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp),
        ) {
            Text(
                text      = "Something went wrong",
                style     = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color     = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Text(
                text      = message,
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}
