package com.nexuswavetech.nexusplus.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun NexusWaveLogo(modifier: Modifier) {
    Icon(
        imageVector        = Icons.Filled.AutoAwesome,
        contentDescription = "Nexus Wave Technologies",
        modifier           = modifier,
        tint               = MaterialTheme.colorScheme.primary,
    )
}
