package com.nexuswavetech.nexusplus.features.taskmanager

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class RunningProcess(
    val pid:        Int,
    val appName:    String,
    val pkg:        String,
    val importance: String,
    val memKb:      Long,
)

private fun importanceLabel(imp: Int) = when {
    imp <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "Foreground"
    imp <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE    -> "Visible"
    imp <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE    -> "Service"
    else                                                                -> "Cached"
}

@Suppress("DEPRECATION")
private suspend fun loadProcesses(context: Context): List<RunningProcess> =
    withContext(Dispatchers.Default) {
        val am  = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm  = context.packageManager
        val raw = am.runningAppProcesses ?: return@withContext emptyList()

        val pids     = raw.map { it.pid }.toIntArray()
        val memInfos = try { am.getProcessMemoryInfo(pids) } catch (_: Exception) { emptyArray() }

        raw.mapIndexed { idx, proc ->
            val memKb   = memInfos.getOrNull(idx)?.totalPss?.toLong() ?: 0L
            val appName = try {
                val pkg = proc.pkgList?.firstOrNull() ?: proc.processName
                val ai  = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                pm.getApplicationLabel(ai).toString()
            } catch (_: Exception) { proc.processName }

            RunningProcess(
                pid        = proc.pid,
                appName    = appName,
                pkg        = proc.pkgList?.firstOrNull() ?: proc.processName,
                importance = importanceLabel(proc.importance),
                memKb      = memKb,
            )
        }.sortedWith(compareBy({ -it.memKb }, { it.appName }))
    }

private fun formatMemory(kb: Long): String =
    if (kb >= 1024) String.format("%.1f MB", kb / 1024f) else "$kb KB"

@Composable
fun TaskManagerScreen(onBack: () -> Unit) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val am        = remember { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }

    var processes by remember { mutableStateOf<List<RunningProcess>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var ramInfo   by remember { mutableStateOf(ActivityManager.MemoryInfo()) }

    fun refresh() {
        isLoading = true
        scope.launch {
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            ramInfo   = mi
            processes = loadProcesses(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title   = "Task Manager",
            onBack  = onBack,
            actions = {
                IconButton(onClick = { refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            },
        )

        // RAM overview card
        val totalMb  = ramInfo.totalMem / (1024 * 1024)
        val availMb  = ramInfo.availMem  / (1024 * 1024)
        val usedMb   = totalMb - availMb
        val fraction = if (totalMb > 0) usedMb.toFloat() / totalMb.toFloat() else 0f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("RAM Usage", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Used: $usedMb MB",  style = MaterialTheme.typography.bodyMedium)
                    Text("Free: $availMb MB", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(
                    progress   = { fraction },
                    modifier   = Modifier.fillMaxWidth().height(8.dp),
                    color      = if (fraction > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text("Total: $totalMb MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                "${processes.size} running processes",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(processes, key = { it.pid }) { proc ->
                    ProcessCard(proc)
                }
            }
        }
    }
}

@Composable
private fun importanceColor(importance: String): Color {
    val scheme = MaterialTheme.colorScheme
    return when (importance) {
        "Foreground" -> scheme.primary
        "Visible"    -> scheme.secondary
        "Service"    -> scheme.tertiary
        else         -> scheme.outline
    }
}

@Composable
private fun ProcessCard(proc: RunningProcess) {
    val impColor = importanceColor(proc.importance)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Dashboard, contentDescription = null, tint = impColor)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    proc.appName,
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    proc.pkg,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = impColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        proc.importance,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = impColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (proc.memKb > 0) Text(formatMemory(proc.memKb), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("PID ${proc.pid}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
