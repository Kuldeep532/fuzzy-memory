package com.nexuswavetech.nexusplus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nexuswavetech.nexusplus.R

@Composable
actual fun NexusWaveLogo(modifier: Modifier) {
    Image(
        painter            = painterResource(id = R.drawable.nexus_wave_logo),
        contentDescription = "Nexus Wave Technologies",
        modifier           = modifier,
        contentScale       = ContentScale.Fit,
    )
}
