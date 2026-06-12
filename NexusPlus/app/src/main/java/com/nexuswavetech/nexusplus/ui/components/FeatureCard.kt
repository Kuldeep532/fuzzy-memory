package com.nexuswavetech.nexusplus.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.core.FeatureItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeatureCard(
    feature: FeatureItem,
    onTap: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val favoriteLabel = if (feature.isFavorite) "Remove from Favorites" else "Add to Favorites"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = { showContextMenu = true }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(feature.name)
                    append(". ")
                    append(feature.description)
                    append(". Double tap to open.")
                    append(" Long press to $favoriteLabel.")
                    if (feature.isFavorite) append(" Currently in favorites.")
                    customActions = listOf(
                        CustomAccessibilityAction(favoriteLabel) {
                            onToggleFavorite()
                            true
                        }
                    )
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    tint = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = if (feature.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = if (feature.isFavorite)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = feature.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = feature.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }

    // Context action menu on long press
    if (showContextMenu) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text(feature.name, modifier = Modifier.semantics { heading() }) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(favoriteLabel) },
                        leadingContent = {
                            Icon(
                                imageVector = if (feature.isFavorite)
                                    Icons.Filled.BookmarkRemove
                                else
                                    Icons.Filled.BookmarkAdd,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .semantics { contentDescription = favoriteLabel }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleFavorite()
                        showContextMenu = false
                    },
                    modifier = Modifier.semantics { contentDescription = favoriteLabel }
                ) {
                    Text(favoriteLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showContextMenu = false }) { Text("Cancel") }
            }
        )
    }
}
