package com.nexuswavetech.nexusplus.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text      = title,
                style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color     = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier.semantics {
                        contentDescription = "Back"
                    }
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = { actions() },
        colors  = TopAppBarDefaults.topAppBarColors(
            containerColor        = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor     = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        windowInsets = TopAppBarDefaults.windowInsets,
    )
}
