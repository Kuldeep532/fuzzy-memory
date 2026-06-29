package com.nexuswavetech.nexusplus.features.appinfo

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class AppInfo(
    val name        : String,
    val packageName : String,
    val version     : String,
    val sizeMb      : Float,
    val installDate : String,
)

private suspend fun loadApps(pm: PackageManager, showSystem: Boolean): List<AppInfo> =
    withContext(Dispatchers.Default) {
        val fmt  = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        pm.getInstalledPackages(PackageManager.GET_META_DATA)
            .mapNotNull { pkg ->
                val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!showSystem && isSystem) return@mapNotNull null
                val name    = pm.getApplicationLabel(appInfo).toString()
                val version = pkg.versionName ?: "N/A"
                val sizeMb  = runCatching {
                    File(appInfo.sourceDir).length() / (1024f * 1024f)
                }.getOrDefault(0f)
                val install = fmt.format(Date(pkg.firstInstallTime))
                AppInfo(
                    name        = name,
                    packageName = pkg.packageName,
                    version     = version,
                    sizeMb      = sizeMb,
                    installDate = install,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

@Composable
fun AppInfoCenterScreen(onBack: () -> Unit) {
    val context    = LocalContext.current
    val pm         = context.packageManager

    var apps       by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }
    var query      by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }

    LaunchedEffect(showSystem) {
        isLoading = true
        apps      = loadApps(pm, showSystem)
        isLoading = false
    }

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(
            title   = "Installed Apps",
            onBack  = onBack,
            actions = {
                IconButton(
                    onClick  = { showSystem = !showSystem },
                    modifier = Modifier.semantics {
                        contentDescription = if (showSystem) "Hide system apps" else "Show system apps"
                    }
                ) {
                    Icon(
                        if (showSystem) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            placeholder   = { Text("Search apps or package name") },
            leadingIcon   = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon  = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick  = { query = "" },
                        modifier = Modifier.semantics { contentDescription = "Clear search" }
                    ) { Icon(Icons.Filled.Clear, contentDescription = null) }
                }
            },
            singleLine    = true,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { contentDescription = "Search installed apps" }
        )

        Text(
            "${filtered.size} ${if (showSystem) "apps" else "user apps"}",
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading apps...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppCard(
                        app     = app,
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCard(app: AppInfo, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${app.name}, version ${app.version}, tap to open app settings" }
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text  = app.name.firstOrNull()?.uppercase() ?: "A",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    app.name,
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.packageName,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "v${app.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (app.sizeMb > 0f) {
                        Text(
                            "%.1f MB".format(app.sizeMb),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        app.installDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
