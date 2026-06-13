package com.nexuswavetech.nexusplus.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings = koinInject<SettingsRepository>()
    val scope    = rememberCoroutineScope()

    val theme        by settings.theme.collectAsState(initial = SettingsRepository.THEME_SYSTEM)
    val dynamicColor by settings.dynamicColor.collectAsState(initial = false)
    val highContrast by settings.highContrast.collectAsState(initial = false)
    val reduceMotion by settings.reduceMotion.collectAsState(initial = false)
    val fontScale    by settings.fontScale.collectAsState(initial = SettingsRepository.FONT_NORMAL)

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Settings", onBack = onBack)

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // ── Appearance ────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    title = "Appearance",
                    icon  = Icons.Filled.Palette,
                )
            }

            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Theme",
                            style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color    = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        listOf(
                            Triple(SettingsRepository.THEME_SYSTEM, "Follow system",  Icons.Filled.SettingsBrightness),
                            Triple(SettingsRepository.THEME_LIGHT,  "Light",          Icons.Filled.LightMode),
                            Triple(SettingsRepository.THEME_DARK,   "Dark",           Icons.Filled.DarkMode),
                        ).forEach { (value, label, icon) ->
                            ThemeOption(
                                icon      = icon,
                                label     = label,
                                selected  = theme == value,
                                onClick   = { scope.launch { settings.setTheme(value) } },
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard {
                    SettingsToggleRow(
                        icon    = Icons.Filled.ColorLens,
                        title   = "Dynamic Colour",
                        subtitle = "Use Material You wallpaper-based colours (Android 12+)",
                        checked  = dynamicColor,
                        onToggle = { scope.launch { settings.setDynamicColor(it) } },
                    )
                }
            }

            // ── Text Size ─────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    title = "Text Size",
                    icon  = Icons.Filled.TextFields,
                )
            }

            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Font Scale",
                            style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color    = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        listOf(
                            SettingsRepository.FONT_NORMAL to "Normal",
                            SettingsRepository.FONT_LARGE  to "Large",
                            SettingsRepository.FONT_XLARGE to "Extra Large",
                        ).forEach { (value, label) ->
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "$label font scale. " +
                                            if (fontScale == value) "Currently selected." else "Double tap to select."
                                    },
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                RadioButton(
                                    selected = fontScale == value,
                                    onClick  = { scope.launch { settings.setFontScale(value) } },
                                )
                            }
                        }
                    }
                }
            }

            // ── Accessibility ─────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    title = "Accessibility",
                    icon  = Icons.Filled.Accessibility,
                )
            }

            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsToggleRow(
                            icon     = Icons.Filled.Contrast,
                            title    = "High Contrast",
                            subtitle = "Increases contrast for better legibility",
                            checked  = highContrast,
                            onToggle = { scope.launch { settings.setHighContrast(it) } },
                        )
                        HorizontalDivider()
                        SettingsToggleRow(
                            icon     = Icons.Filled.Animation,
                            title    = "Reduce Motion",
                            subtitle = "Minimises animations and transitions",
                            checked  = reduceMotion,
                            onToggle = { scope.launch { settings.setReduceMotion(it) } },
                        )
                    }
                }
            }

            // ── App Info ──────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    title = "App Info",
                    icon  = Icons.Filled.Info,
                )
            }

            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsInfoRow(
                            icon  = Icons.Filled.Android,
                            title = "Version",
                            value = "1.2.0",
                        )
                        HorizontalDivider()
                        SettingsInfoRow(
                            icon  = Icons.Filled.Business,
                            title = "Developer",
                            value = "Nexus Wave Technologies",
                        )
                        HorizontalDivider()
                        SettingsInfoRow(
                            icon  = Icons.Filled.Build,
                            title = "Build",
                            value = "Kotlin 2.0 · Compose BOM 2024.06 · KMP",
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Private helpers ─────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier              = Modifier
            .padding(vertical = 8.dp)
            .semantics { heading() },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content             = content,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $subtitle. " +
                    if (checked) "Currently enabled. Double tap to disable." else "Currently disabled. Double tap to enable."
            },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$label theme. " +
                    if (selected) "Currently selected." else "Double tap to select."
            },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title: $value" },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
