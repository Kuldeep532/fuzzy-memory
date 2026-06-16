package com.nexuswavetech.nexusplus.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Shared scaffold wrapper used by every full-screen feature and hub.
 * Provides a consistent top bar, optional actions, and content slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            NexusTopBar(title = title, onBack = onBack, actions = actions)
        },
        content = content,
    )
}

/**
 * Icon action button used in [AppScaffold] or [NexusTopBar] action slots.
 */
@Composable
fun TopBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick  = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        Icon(imageVector = icon, contentDescription = null)
    }
}
