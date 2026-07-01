package com.nexuswavetech.nexusplus.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.graphics.vector.ImageVector

enum class FeatureHub(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
) {
    SECURITY(
        displayName = "Security",
        description = "Encryption, hashing, passwords & secure vault",
        icon        = Icons.Filled.Security,
        route       = "hub/security",
    ),
    DOCUMENTS(
        displayName = "Documents",
        description = "PDF reader, merger, editor & document hub",
        icon        = Icons.Filled.Description,
        route       = "hub/documents",
    ),
    AI(
        displayName = "AI & Voice",
        description = "AI image generation, translation, TTS & object detection",
        icon        = Icons.Filled.AutoAwesome,
        route       = "hub/ai",
    ),
    MEDIA(
        displayName = "Media",
        description = "Radio, music streaming & live TV",
        icon        = Icons.Filled.PlayCircle,
        route       = "hub/media",
    ),
    UTILITIES(
        displayName = "Utilities",
        description = "Calculator, QR, converters & developer tools",
        icon        = Icons.Filled.Build,
        route       = "hub/utilities",
    ),
    SCIENCE(
        displayName = "Science & Space",
        description = "NASA APOD, Mars Rover, space exploration & science news",
        icon        = Icons.Filled.Science,
        route       = "hub/science",
    ),
}
