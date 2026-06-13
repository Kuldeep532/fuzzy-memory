package com.nexuswavetech.nexusplus.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.core.FeatureItem

/**
 * Universal feature card.
 *
 * ## Interaction model
 *  - Single tap   → open feature
 *  - Long press   → context menu with 5 actions
 *  - TalkBack     → semantic custom actions expose all 5 actions without gestures
 *
 * ## Context-menu actions
 *  1. Open
 *  2. Add / Remove from Favorites
 *  3. Pin / Unpin from Home
 *  4. Share
 *  5. View Information
 *
 * No permanent favorite/pin button is visible on the card — this keeps the
 * UI uncluttered. A subtle icon badge appears in the top-right only when a
 * feature is already favorited or pinned.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeatureCard(
    feature: FeatureItem,
    onTap: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }
    var showInfoDialog  by remember { mutableStateOf(false) }

    val favoriteLabel = if (feature.isFavorite) "Remove from Favorites" else "Add to Favorites"
    val pinLabel      = if (feature.isPinned)   "Unpin from Home"       else "Pin to Home"

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick    = onTap,
                    onLongClick = { showContextMenu = true },
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = buildString {
                        append(feature.name)
                        append(". ")
                        append(feature.description)
                        if (feature.isNew)       append(" New feature.")
                        if (feature.isFavorite)  append(" In your favorites.")
                        if (feature.isPinned)    append(" Pinned to Home.")
                        append(" Double tap to open. Long press for more options.")
                    }
                    customActions = listOf(
                        CustomAccessibilityAction("Open")                { onTap(); true },
                        CustomAccessibilityAction(favoriteLabel)         { onToggleFavorite(); true },
                        CustomAccessibilityAction(pinLabel)              { onTogglePin(); true },
                        CustomAccessibilityAction("Share")               { shareFeature(context, feature); true },
                        CustomAccessibilityAction("View Information")    { showInfoDialog = true; true },
                    )
                },
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // ── Icon row + status badges ──────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    // Feature icon in tinted surface
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector        = feature.icon,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(22.dp),
                            )
                        }
                    }

                    // Subtle status badges — no interactive buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        if (feature.isNew) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                            ) {
                                Text(
                                    text  = "NEW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                )
                            }
                        }
                        if (feature.isPinned) {
                            Icon(
                                imageVector        = Icons.Filled.PushPin,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                modifier           = Modifier.size(14.dp),
                            )
                        }
                        if (feature.isFavorite) {
                            Icon(
                                imageVector        = Icons.Filled.Star,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                                modifier           = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                // ── Feature name ──────────────────────────────────────────
                Text(
                    text  = feature.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )

                // ── Description ───────────────────────────────────────────
                Text(
                    text  = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )

                // ── Category chip ─────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Text(
                        text     = feature.category.label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        // ── Context menu ──────────────────────────────────────────────────
        DropdownMenu(
            expanded          = showContextMenu,
            onDismissRequest  = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text          = { Text("Open") },
                leadingIcon   = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                onClick       = { showContextMenu = false; onTap() },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text          = { Text(favoriteLabel) },
                leadingIcon   = {
                    Icon(
                        if (feature.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (feature.isFavorite) MaterialTheme.colorScheme.secondary
                               else LocalContentColor.current,
                    )
                },
                onClick       = { onToggleFavorite(); showContextMenu = false },
            )
            DropdownMenuItem(
                text          = { Text(pinLabel) },
                leadingIcon   = {
                    Icon(
                        if (feature.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = if (feature.isPinned) MaterialTheme.colorScheme.primary
                               else LocalContentColor.current,
                    )
                },
                onClick       = { onTogglePin(); showContextMenu = false },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text          = { Text("Share") },
                leadingIcon   = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick       = { shareFeature(context, feature); showContextMenu = false },
            )
            DropdownMenuItem(
                text          = { Text("Info") },
                leadingIcon   = { Icon(Icons.Filled.Info, contentDescription = null) },
                onClick       = { showInfoDialog = true; showContextMenu = false },
            )
        }
    }

    // ── Info dialog ───────────────────────────────────────────────────────
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon             = {
                Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title            = {
                Text(feature.name, modifier = Modifier.semantics { heading() })
            },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text  = feature.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text  = feature.category.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    if (feature.keywords.isNotEmpty()) {
                        Text(
                            text  = "Keywords: ${feature.keywords.take(6).joinToString(" · ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    if (feature.isFavorite || feature.isPinned) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (feature.isFavorite) {
                                AssistChip(
                                    onClick = {},
                                    label   = { Text("Favorited") },
                                    leadingIcon = { Icon(Icons.Filled.Star, null, modifier = Modifier.size(16.dp)) },
                                )
                            }
                            if (feature.isPinned) {
                                AssistChip(
                                    onClick = {},
                                    label   = { Text("Pinned") },
                                    leadingIcon = { Icon(Icons.Filled.PushPin, null, modifier = Modifier.size(16.dp)) },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false; onTap() }) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            },
        )
    }
}

// ── Private ───────────────────────────────────────────────────────────────────

private fun shareFeature(context: Context, feature: FeatureItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out ${feature.name} on Nexus Plus!")
        putExtra(
            Intent.EXTRA_TEXT,
            "${feature.name}\n\n${feature.description}\n\n" +
            "Category: ${feature.category.label}\n\n" +
            "Available in Nexus Plus — the Nexus Wave Technologies super-app.",
        )
    }
    context.startActivity(Intent.createChooser(intent, "Share ${feature.name} via…"))
}
