package com.nexuswavetech.nexusplus.features.nexusautomation

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

private data class AutoModule(
    val name:   String,
    val desc:   String,
    val icon:   ImageVector,
    val color:  Color,
    val route:  String,
    val badge:  String? = null,
)

private val modules = listOf(
    AutoModule("My Reminder", "Local notifications with exact scheduling", Icons.Filled.NotificationsActive, Color(0xFFFF9800), Screen.MyReminder.route, "WorkManager"),
    AutoModule("Alarm Clock", "Precise alarms with AlarmManager", Icons.Filled.Alarm, Color(0xFF5C6BC0), Screen.AlarmClock.route),
    AutoModule("Stopwatch", "Precision lap timer with centisecond accuracy", Icons.Filled.Timer, Color(0xFF26A69A), Screen.Stopwatch.route),
    AutoModule("World Clock", "Track multiple time zones simultaneously", Icons.Filled.Language, Color(0xFF9C27B0), Screen.WorldClock.route),
    AutoModule("Form X", "Universal reactive form with real-time validation", Icons.Filled.Assignment, Color(0xFF00BCD4), Screen.FormX.route, "New"),
    AutoModule("Clipboard Manager", "Persistent clipboard history across sessions", Icons.Filled.ContentPaste, Color(0xFF607D8B), Screen.ClipboardManager.route),
    AutoModule("Voice Typer", "Voice-to-text for hands-free input", Icons.Filled.Mic, Color(0xFF4CAF50), Screen.VoiceTyper.route),
)

@Composable
fun NexusAutomationScreen(navController: NavController, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Nexus Automation", onBack = onBack)

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.Bolt, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text("Productivity & Automation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.semantics { heading() })
                            Text("Streamline your daily workflow with smart reminders, scheduling, voice input, and productivity tools.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            item { Text("Automation Tools", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary) }

            items(modules) { module ->
                Card(
                    onClick  = { navController.navigate(module.route) },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${module.name}: ${module.desc}. Tap to open." },
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(color = module.color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.medium) {
                            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Icon(module.icon, null, tint = module.color, modifier = Modifier.size(26.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(module.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                module.badge?.let { Surface(color = module.color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraSmall) { Text(it, style = MaterialTheme.typography.labelSmall, color = module.color, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }
                            }
                            Text(module.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            }

            // Roadmap
            item {
                Spacer(Modifier.height(8.dp))
                Text("Coming Soon", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
            }
            items(listOf("Tasker-style rule builder", "App usage tracker", "Screen time management", "Auto-reply engine")) { feature ->
                ListItem(
                    headlineContent = { Text(feature, style = MaterialTheme.typography.bodyMedium) },
                    leadingContent  = { Icon(Icons.Filled.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier        = Modifier.semantics { contentDescription = "Upcoming: $feature" },
                )
            }
        }
    }
}
