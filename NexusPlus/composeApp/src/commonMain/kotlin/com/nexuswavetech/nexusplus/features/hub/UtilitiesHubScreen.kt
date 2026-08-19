package com.nexuswavetech.nexusplus.features.hub

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

@Composable
fun UtilitiesHubScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    NexusTopBar(title = "Utilities Hub (Removed)", onBack = onBack)
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("This hub has been removed.", color = MaterialTheme.colorScheme.onBackground)
    }
}
