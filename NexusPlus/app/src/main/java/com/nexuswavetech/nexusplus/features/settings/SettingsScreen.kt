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

    // ── Appearance ──────────────────────────────────────────────────────────
    val theme        by settings.theme.collectAsState(initial = SettingsRepository.THEME_SYSTEM)
    val dynamicColor by settings.dynamicColor.collectAsState(initial = false)
    val highContrast by settings.highContrast.collectAsState(initial = false)
    val reduceMotion by settings.reduceMotion.collectAsState(initial = false)
    val fontScale    by settings.fontScale.collectAsState(initial = SettingsRepository.FONT_NORMAL)

    // ── Security ────────────────────────────────────────────────────────────
    val vaultLockMins by settings.vaultAutoLockMinutes.collectAsState(initial = SettingsRepository.VAULT_LOCK_DEFAULT)

    // ── Haptics ─────────────────────────────────────────────────────────────
    val touchVibration by settings.touchVibration.collectAsState(initial = true)

    // ── Feature settings ────────────────────────────────────────────────────
    val ttsRate             by settings.ttsDefaultRate.collectAsState(initial = 1.0f)
    val morseSpeed          by settings.morseVibrationSpeed.collectAsState(initial = SettingsRepository.MORSE_SPEED_NORMAL)
    val bufferQuality       by settings.bufferQuality.collectAsState(initial = SettingsRepository.BUFFER_NORMAL)
    val translatorAuto      by settings.translatorAutoDetect.collectAsState(initial = true)
    val reminderSnooze      by settings.reminderSnoozeMins.collectAsState(initial = SettingsRepository.REMINDER_SNOOZE_DEFAULT)
    val calcAngleUnit       by settings.calculatorAngleUnit.collectAsState(initial = SettingsRepository.ANGLE_DEG)

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Settings", onBack = onBack)

        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                                icon     = icon,
                                label    = label,
                                selected = theme == value,
                                onClick  = { scope.launch { settings.setTheme(value) } },
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard {
                    SettingsToggleRow(
                        icon     = Icons.Filled.ColorLens,
                        title    = "Dynamic Colour",
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
                                    style    = MaterialTheme.typography.bodyMedium,
                                    color    = MaterialTheme.colorScheme.onSurface,
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

            // ── Security ──────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    title = "Security",
                    icon  = Icons.Filled.Security,
                )
            }

            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Vault Auto-Lock", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (vaultLockMins == 0) "Never" else "After $vaultLockMins minute${if (vaultLockMins != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Lock timeout",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        SettingsRepository.VAULT_LOCK_OPTIONS.forEach { mins ->
                            val label = if (mins == 0) "Never" else "$mins min${if (mins != 1) "s" else ""}"
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "Vault auto-lock: $label. " +
                                            if (vaultLockMins == mins) "Currently selected." else "Double tap to select."
                                    },
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                RadioButton(
                                    selected = vaultLockMins == mins,
                                    onClick  = { scope.launch { settings.setVaultAutoLockMinutes(mins) } },
                                )
                            }
                        }
                    }
                }
            }

            // ── Haptics ───────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    title = "Haptics",
                    icon  = Icons.Filled.Vibration,
                )
            }

            item {
                SettingsCard {
                    SettingsToggleRow(
                        icon     = Icons.Filled.Vibration,
                        title    = "Touch Vibration",
                        subtitle = "Vibrate on button taps and key presses",
                        checked  = touchVibration,
                        onToggle = { scope.launch { settings.setTouchVibration(it) } },
                    )
                }
            }

            // ── Feature Settings ──────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    title = "Feature Settings",
                    icon  = Icons.Filled.Tune,
                )
            }

            // TTS
            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("Speech Engine (TTS)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "Default speed: ${"%.1f".format(ttsRate)}×",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value         = ttsRate,
                            onValueChange = { scope.launch { settings.setTtsDefaultRate(it) } },
                            valueRange    = 0.25f..3.0f,
                            steps         = 27,
                            modifier      = Modifier.semantics { contentDescription = "TTS default speed slider. Current: ${"%.1f".format(ttsRate)}x" },
                        )
                    }
                }
            }

            // Morse Code
            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("Morse Code", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("Vibration speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        listOf(
                            SettingsRepository.MORSE_SPEED_SLOW   to "Slow",
                            SettingsRepository.MORSE_SPEED_NORMAL to "Normal",
                            SettingsRepository.MORSE_SPEED_FAST   to "Fast",
                        ).forEach { (value, label) ->
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "Morse vibration: $label. " +
                                            if (morseSpeed == value) "Currently selected." else "Double tap to select."
                                    },
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                RadioButton(
                                    selected = morseSpeed == value,
                                    onClick  = { scope.launch { settings.setMorseVibrationSpeed(value) } },
                                )
                            }
                        }
                    }
                }
            }

            // Radio / IPTV
            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Radio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("Radio & IPTV", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("Stream buffer quality", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        listOf(
                            SettingsRepository.BUFFER_LOW    to "Low (saves data)",
                            SettingsRepository.BUFFER_NORMAL to "Normal",
                            SettingsRepository.BUFFER_HIGH   to "High (best quality)",
                        ).forEach { (value, label) ->
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "Buffer quality: $label. " +
                                            if (bufferQuality == value) "Currently selected." else "Double tap to select."
                                    },
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                RadioButton(
                                    selected = bufferQuality == value,
                                    onClick  = { scope.launch { settings.setBufferQuality(value) } },
                                )
                            }
                        }
                    }
                }
            }

            // Translator
            item {
                SettingsCard {
                    SettingsToggleRow(
                        icon     = Icons.Filled.Translate,
                        title    = "Translator Auto-detect",
                        subtitle = "Automatically detect source language from input text",
                        checked  = translatorAuto,
                        onToggle = { scope.launch { settings.setTranslatorAutoDetect(it) } },
                    )
                }
            }

            // Reminder
            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("Reminders", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("Default snooze duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        SettingsRepository.REMINDER_SNOOZE_OPTIONS.forEach { mins ->
                            val label = "$mins min${if (mins != 1) "s" else ""}"
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "Snooze $label. " +
                                            if (reminderSnooze == mins) "Currently selected." else "Double tap to select."
                                    },
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                RadioButton(
                                    selected = reminderSnooze == mins,
                                    onClick  = { scope.launch { settings.setReminderSnoozeMins(mins) } },
                                )
                            }
                        }
                    }
                }
            }

            // Calculator
            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("Calculator", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("Angle unit for trigonometric functions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        listOf(
                            SettingsRepository.ANGLE_DEG to "Degrees (°)",
                            SettingsRepository.ANGLE_RAD to "Radians (rad)",
                        ).forEach { (value, label) ->
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "Angle unit: $label. " +
                                            if (calcAngleUnit == value) "Currently selected." else "Double tap to select."
                                    },
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                RadioButton(
                                    selected = calcAngleUnit == value,
                                    onClick  = { scope.launch { settings.setCalculatorAngleUnit(value) } },
                                )
                            }
                        }
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
