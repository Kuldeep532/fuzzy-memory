package com.nexuswavetech.nexusplus.features.nexusintelligence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.navigation.Screen
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

private data class IntelligenceModule(
    val id:          String,
    val name:        String,
    val tagline:     String,
    val description: String,
    val icon:        ImageVector,
    val color:       Color,
    val route:       String,
    val isLive:      Boolean = true,
    val badgeText:   String? = null,
)

private val modules = listOf(
    IntelligenceModule(
        id          = "text_translator",
        name        = "Neural Translator",
        tagline     = "50+ languages, 100% offline",
        description = "On-device ML Kit translation. No internet, no data leaks.",
        icon        = Icons.Filled.Translate,
        color       = Color(0xFF7C4DFF),
        route       = Screen.TextTranslator.route,
        badgeText   = "Offline",
    ),
    IntelligenceModule(
        id          = "ai_image",
        name        = "AI Image Studio",
        tagline     = "Text-to-image generation",
        description = "Create stunning visuals from natural language prompts.",
        icon        = Icons.Filled.AutoAwesome,
        color       = Color(0xFFFF6090),
        route       = Screen.AiImageGenerator.route,
        badgeText   = "AI",
    ),
    IntelligenceModule(
        id          = "tts",
        name        = "Nexus Speech Engine",
        tagline     = "Multi-language text-to-speech",
        description = "NSE 2.0 with auto-locale detection, SSML, and high-fidelity synthesis.",
        icon        = Icons.Filled.RecordVoiceOver,
        color       = Color(0xFF00BCD4),
        route       = Screen.NexusTts.route,
        badgeText   = "NSE 2.0",
    ),
    IntelligenceModule(
        id          = "voice_typer",
        name        = "Voice Typer",
        tagline     = "Speech-to-text input",
        description = "Native STT. Type using your voice in any language.",
        icon        = Icons.Filled.Mic,
        color       = Color(0xFF4CAF50),
        route       = Screen.VoiceTyper.route,
    ),
    IntelligenceModule(
        id          = "object_detector",
        name        = "Object Detector",
        tagline     = "Real-time vision AI",
        description = "ML Kit on-device object detection and classification via camera.",
        icon        = Icons.Filled.CenterFocusStrong,
        color       = Color(0xFFFF9800),
        route       = Screen.ObjectDetector.route,
        badgeText   = "ML Kit",
    ),
    IntelligenceModule(
        id          = "color_detector",
        name        = "Colour Detector",
        tagline     = "AI colour identification",
        description = "Point camera at any surface to identify its colour name and hex code.",
        icon        = Icons.Filled.Colorize,
        color       = Color(0xFFE91E63),
        route       = Screen.ColorDetector.route,
        badgeText   = "Vision AI",
    ),
    IntelligenceModule(
        id          = "compass",
        name        = "Smart Compass",
        tagline     = "Sensor-fused navigation",
        description = "Rotation vector sensor with animated compass dial and tilt data.",
        icon        = Icons.Filled.Explore,
        color       = Color(0xFF009688),
        route       = Screen.Compass.route,
    ),
)

@Composable
fun NexusIntelligenceScreen(navController: NavController, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Nexus Intelligence", onBack = onBack)

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // Hero
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("AI & Intelligence Hub", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.semantics { heading() })
                            Text("On-device ML, translation, vision AI, and speech — all without sending your data to external servers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            item {
                Text("Available Modules", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
            }

            items(modules) { module ->
                Card(
                    onClick  = { navController.navigate(module.route) },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${module.name}. ${module.tagline}. ${module.description}. Tap to open." },
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(color = module.color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.medium) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Icon(module.icon, null, tint = module.color, modifier = Modifier.size(28.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(module.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                module.badgeText?.let { badge ->
                                    Surface(color = module.color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraSmall) {
                                        Text(badge, style = MaterialTheme.typography.labelSmall, color = module.color, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(module.tagline, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.primary)
                            Text(module.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.OfflineBolt, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Column {
                            Text("Privacy-First AI", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("All intelligence features process data on-device. Nothing leaves your phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }
        }
    }
}
