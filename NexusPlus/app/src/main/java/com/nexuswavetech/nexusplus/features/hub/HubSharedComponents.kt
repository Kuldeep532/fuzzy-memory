package com.nexuswavetech.nexusplus.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Polished gradient header used at the top of every hub screen.
 * Shows the hub icon, title, description, and feature count.
 */
@Composable
internal fun HubHeader(
    title: String,
    icon: ImageVector,
    description: String,
    count: Int,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.12f),
                        primary.copy(alpha = 0.04f),
                    )
                )
            )
            .semantics { contentDescription = "$title. $count features." }
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier           = Modifier.size(28.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text     = title,
                    style    = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text  = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Column(
                    modifier            = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text  = "$count",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text  = "tools",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}
