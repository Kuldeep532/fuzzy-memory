package com.nexuswavetech.nexusplus.features.nexusdevkit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.navigation.Screen
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

private data class DevTool(
    val name:  String,
    val icon:  ImageVector,
    val color: Color,
    val route: String,
    val badge: String? = null,
)

private val tools = listOf(
    DevTool("JSON Formatter",    Icons.Filled.DataObject,          Color(0xFFF4A261), Screen.JsonFormatter.route),
    DevTool("Regex Tester",      Icons.Filled.Code,                Color(0xFF457B9D), Screen.RegexTester.route),
    DevTool("Hash Generator",    Icons.Filled.Tag,                 Color(0xFF2A9D8F), Screen.HashGenerator.route),
    DevTool("Base64 Tool",       Icons.Filled.SwapVert,            Color(0xFF6A4C93), Screen.Base64Tool.route),
    DevTool("Barcode Generator", Icons.Filled.QrCodeScanner,       Color(0xFF1D3557), Screen.BarcodeGenerator.route),
    DevTool("QR Generator",      Icons.Filled.QrCode,              Color(0xFFE63946), Screen.QrCode.route),
    DevTool("Number Systems",    Icons.Filled.Tag,                 Color(0xFF264653), Screen.NumberSystem.route),
    DevTool("Encrypter",         Icons.Filled.EnhancedEncryption,  Color(0xFFE9C46A), Screen.EncrypterDecrypter.route),
    DevTool("Wi-Fi Analyzer",    Icons.Filled.Wifi,                Color(0xFF219EBC), Screen.WifiAnalyzer.route),
    DevTool("Storage Analyzer",  Icons.Filled.Storage,             Color(0xFF8338EC), Screen.StorageAnalyzer.route),
    DevTool("Battery Monitor",   Icons.Filled.BatteryFull,         Color(0xFF06D6A0), Screen.BatteryMonitor.route),
    DevTool("Unit Converter",    Icons.Filled.SwapHoriz,           Color(0xFFFB5607), Screen.UnitConverter.route),
    DevTool("Password Gen",      Icons.Filled.Key,                 Color(0xFF3A0CA3), Screen.PasswordGenerator.route),
    DevTool("Flashlight",        Icons.Filled.FlashOn,             Color(0xFFFFB703), Screen.Flashlight.route),
    DevTool("Compass",           Icons.Filled.Explore,             Color(0xFF023047), Screen.Compass.route),
    DevTool("Calculator",        Icons.Filled.Calculate,           Color(0xFFF77F00), Screen.CalculatorCenter.route),
)

@Composable
fun NexusDevKitScreen(navController: NavController, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Nexus DevKit", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Filled.Build, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Column {
                        Text("Developer Toolkit", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.semantics { heading() })
                        Text("16 tools for developers, debuggers, and power users — all offline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Tools", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns               = GridCells.Adaptive(minSize = 100.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                items(tools) { tool ->
                    Card(
                        onClick  = { navController.navigate(tool.route) },
                        modifier = Modifier
                            .aspectRatio(1f)
                            .semantics { contentDescription = "${tool.name}. Tap to open." },
                        colors   = CardDefaults.cardColors(containerColor = tool.color.copy(alpha = 0.12f)),
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(tool.icon, null, tint = tool.color, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                tool.name,
                                style     = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center,
                                maxLines  = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}
