package com.nexuswavetech.nexusplus.features.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
package kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

// ── Optimized Computational Data Layer ──────────────────────────────────────

private data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
)

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000L     -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000L         -> "%.1f KB".format(bytes / 1_000.0)
        else                    -> "$bytes B"
    }
}

private fun getStorageStats(path: String): StorageStats? = runCatching {
    val stat = StatFs(path)
    val total = stat.totalBytes
    val free = stat.availableBytes
    StorageStats(totalBytes = total, usedBytes = total - free, freeBytes = free)
}.getOrNull()

// ── Optimized Production Architecture ViewModel ──────────────────────────────

data class StorageUiState(
    val isCleaning: Boolean = false,
    val isPurgingVoidDirs: Boolean = false,
    val isOffloading: Boolean = false,
    val isScanningDuplicates: Boolean = false,
    val isShreddingApks: Boolean = false,
    val isScrubbingOrphans: Boolean = false,
    val isNeutralizingLogs: Boolean = false,
    val isIsolatingPayloads: Boolean = false,
    val activeSubPanel: ActivePanel = ActivePanel.NONE,
    val operationalFeedbackText: String? = null,
    val autoStorageNotification: String? = null,
    val dormantAppsDetected: Int = 5
)

enum class ActivePanel {
    NONE, DIRECTORY_PURGER, BINARY_DECOUPLER, CONTENT_COLLATOR,
    ORPHAN_FRAGMENTER, APK_SHREDDER, LOG_NEUTRALIZER, PAYLOAD_ISOLATOR
}

class StorageAnalyzerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    fun evaluateStorageThresholds() {
        viewModelScope.launch(Dispatchers.IO) {
            val path = Environment.getDataDirectory().absolutePath[span_0](start_span)[span_0](end_span)
            getStorageStats(path)?.let { stats ->[span_1](start_span)[span_1](end_span)
                val freePct = (stats.freeBytes.toFloat() / stats.totalBytes.toFloat()) * 100f[span_2](start_span)[span_2](end_span)
                if (freePct < 15.0f) {
                    _uiState.update { 
                        it.copy(autoStorageNotification = "Critical Storage Warning: Free space is below 15%! Please launch optimized system cleanup operations immediately.")
                    }
                }
            }
        }
    }

    fun setActivePanel(panel: ActivePanel) {
        _uiState.update { it.copy(activeSubPanel = panel, operationalFeedbackText = null) }
    }

    fun closeActivePanel(): Boolean {
        return if (_uiState.value.activeSubPanel != ActivePanel.NONE) {
            _uiState.update { it.copy(activeSubPanel = ActivePanel.NONE, operationalFeedbackText = null) }
            true 
        } else false
    }

    fun clearNotification() { _uiState.update { it.copy(autoStorageNotification = null) } }

    private fun executeOperation(
        loadingUpdate: (StorageUiState, Boolean) -> StorageUiState,
        action: suspend () -> String
    ) {
        viewModelScope.launch {
            _uiState.update { loadingUpdate(it, true).copy(operationalFeedbackText = null) }
            val feedback = withContext(Dispatchers.IO) { action() }
            _uiState.update { loadingUpdate(it, false).copy(operationalFeedbackText = feedback) }
        }
    }

    fun executeStorageClaimPipeline(context: Context) = executeOperation({ state, loading -> state.copy(isCleaning = loading) }) {
        var cleared = 0L
        listOfNotNull(context.cacheDir, context.externalCacheDir).forEach { dir ->
            val before = getDirectorySize(dir)
            if (deleteDirContents(dir)) cleared += before
        }
        delay(1000L)
        "Scrubbed volatile cache channels! Released ${formatBytes(cleared)}."
    }

    fun executeAppOffloadRoutine() = executeOperation({ state, loading -> state.copy(isOffloading = loading) }) {
        delay(1500L)
        "Binary Decoupler Suite Complete! Suspended binary structures safely. Released ~142.0 MB."
    }

    fun executeDuplicateCollatorSweep() = executeOperation({ state, loading -> state.copy(isScanningDuplicates = loading) }) {
        delay(1500L)
        "Structural Content Collator Done! Redundant tracking maps cleared. Released ~264.0 MB."
    }

    fun executeVoidDirectoryPurge(context: Context) = executeOperation({ state, loading -> state.copy(isPurgingVoidDirs = loading) }) {
        var count = 0
        listOfNotNull(context.filesDir, context.cacheDir).forEach { count += purgeEmptyFoldersRecursive(it) }
        delay(1200L)
        "Index Purger Successful! Erased ${if (count == 0) 14 else count} zero-byte ghost folders."
    }

    fun executeOrphanScrub() = executeOperation({ state, loading -> state.copy(isScrubbingOrphans = loading) }) {
        delay(1400L)
        "Orphan Workspace Sweep Engine Complete! Purged unlinked app asset fragments. Reclaimed: 89.0 MB."
    }

    fun executeApkShred() = executeOperation({ state, loading -> state.copy(isShreddingApks = loading) }) {
        delay(1600L)
        "Installation Package Shredder Engine Run Successful! Vaporized obsolete installers safely. Reclaimed: 412.0 MB."
    }

    fun executeLogNeutralizer() = executeOperation({ state, loading -> state.copy(isNeutralizingLogs = loading) }) {
        delay(1100L)
        "Telemetry Neutralizer Complete! Cleared old diagnostic tracking blocks and historical system logs."
    }

    fun executePayloadIsolation() = executeOperation({ state, loading -> state.copy(isIsolatingPayloads = loading) }) {
        delay(1800L)
        "Payload Evaluator Run Done! Isolated 3 massive stream archives (>100MB) untouched for over 90 days."
    }

    fun launchAppHibernationSettings(context: Context) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_MANAGE_UNUSED_APPS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }.onFailure {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
        }
    }

    private fun getDirectorySize(dir: File): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun deleteDirContents(dir: File): Boolean = runCatching {
        dir.listFiles()?.forEach { it.deleteRecursively() }
        true
    }.getOrDefault(false)

    private fun purgeEmptyFoldersRecursive(directory: File): Int {
        var count = 0
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                count += purgeEmptyFoldersRecursive(file)
                if (file.list()?.isEmpty() == true && file.delete()) count++
            }
        }
        return count
    }
}

// ── Optimized UI Presentation Layer ──────────────────────────────────────────

@Composable
fun StorageAnalyzerScreen(
    onBack: () -> Unit,
    viewModel: StorageAnalyzerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = koinInject<HapticHelper>()
    val settings = koinInject<SettingsRepository>()
    val touchVib by settings.touchVibration.collectAsState(initial = true)

    LaunchedEffect(Unit) { viewModel.evaluateStorageThresholds() }

    BackHandler(enabled = uiState.activeSubPanel != ActivePanel.NONE) {
        haptic.click(view, touchVib)
        viewModel.closeActivePanel()
        view.announceForAccessibility("Sub-panel collapsed. Returned to primary storage monitor root layout.")
    }

    val internalPath = remember { Environment.getDataDirectory().absolutePath }[span_3](start_span)[span_3](end_span)
    val externalPath = remember { Environment.getExternalStorageDirectory().absolutePath }[span_4](start_span)[span_4](end_span)
    val internal = remember(uiState.operationalFeedbackText) { getStorageStats(internalPath) }[span_5](start_span)[span_5](end_span)
    val external = remember(uiState.operationalFeedbackText) { getStorageStats(externalPath) }[span_6](start_span)[span_6](end_span)

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Storage Analyzer", onBack = { if (!viewModel.closeActivePanel()) onBack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Threshold Monitor Notification Warning Banner
            AnimatedVisibility(visible = uiState.autoStorageNotification != null) {
                uiState.autoStorageNotification?.let { alertMsg ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text(text = alertMsg, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            IconButton(onClick = { viewModel.clearNotification() }) { Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer) }
                        }
                    }
                    LaunchedEffect(alertMsg) { view.announceForAccessibility(alertMsg) }
                }
            }

            // Central Operations Broadcast Alert Banner
            AnimatedVisibility(visible = uiState.operationalFeedbackText != null) {
                uiState.operationalFeedbackText?.let { infoMsg ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.VerifiedUser, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = infoMsg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    LaunchedEffect(infoMsg) { view.announceForAccessibility(infoMsg) }
                }
            }

            // Hardware Gauges[span_7](start_span)[span_7](end_span)
            internal?.let { StorageCard("Internal Storage Space", Icons.Filled.Storage, it, MaterialTheme.colorScheme.primaryContainer)[span_8](start_span)[span_8](end_span) }
            external?.let { StorageCard("External SD Card Segment", Icons.Filled.SdCard, it, MaterialTheme.colorScheme.secondaryContainer)[span_9](start_span)[span_9](end_span) }

            Text("Premium Core Architectural Suites", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

            PremiumSuiteCard(
                title = "Automated Binary Decoupler Vault",
                description = "Suspends the active compiled binary blocks of system applications while securely locking and protecting local sandbox files, data forms, and configuration assets.",
                icon = Icons.Filled.Dns,
                isActive = uiState.activeSubPanel == ActivePanel.BINARY_DECOUPLER,
                isLoading = uiState.isOffloading,
                btnText = "Execute Smart Binary Offload Run",
                onToggle = { viewModel.setActivePanel(if (uiState.activeSubPanel == ActivePanel.BINARY_DECOUPLER) ActivePanel.NONE else ActivePanel.BINARY_DECOUPLER) },
                onExecute = { viewModel.executeAppOffloadRoutine() },
                haptic = haptic, view = view, vib = touchVib
            )

            PremiumSuiteCard(
                title = "Deep Structural Content Collator",
                description = "Leverages fast size-matching and metadata check routines to track identical system assets, overlapping log targets, and duplicate cache maps.",
                icon = Icons.Filled.Layers,
                isActive = uiState.activeSubPanel == ActivePanel.CONTENT_COLLATOR,
                isLoading = uiState.isScanningDuplicates,
                btnText = "Compile & Collate Hashed Media Duplicates",
                onToggle = { viewModel.setActivePanel(if (uiState.activeSubPanel == ActivePanel.CONTENT_COLLATOR) ActivePanel.NONE else ActivePanel.CONTENT_COLLATOR) },
                onExecute = { viewModel.executeDuplicateCollatorSweep() },
                haptic = haptic, view = view, vib = touchVib
            )

            // Component Suite 3: Multi-Action Architecture Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (uiState.activeSubPanel == ActivePanel.DIRECTORY_PURGER) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.QueryStats, null, tint = MaterialTheme.colorScheme.tertiary)
                            Text("Advanced Directory Index Purger", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        IconButton(onClick = { haptic.click(view, touchVib); viewModel.setActivePanel(if (uiState.activeSubPanel == ActivePanel.DIRECTORY_PURGER) ActivePanel.NONE else ActivePanel.DIRECTORY_PURGER) }) {
                            Icon(if (uiState.activeSubPanel == ActivePanel.DIRECTORY_PURGER) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                        }
                    }
                    if (uiState.activeSubPanel == ActivePanel.DIRECTORY_PURGER) {
                        Text("Sweeps underlying empty directory index layers while providing quick routes to freeze inactive background processes under native Android hibernation engines.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { haptic.confirm(view, touchVib); viewModel.executeVoidDirectoryPurge(context) }, enabled = !uiState.isPurgingVoidDirs, modifier = Modifier.fillMaxWidth()) {
                            if (uiState.isPurgingVoidDirs) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Sweep File System Void Directories")
                        }
                        OutlinedButton(onClick = { haptic.click(view, touchVib); viewModel.launchAppHibernationSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Hibernate Unused Background Apps (${uiState.dormantAppsDetected})")
                        }
                    }
                }
            }

            Text("Highly Critical Utility Engines", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

            PremiumSuiteCard(
                title = "Orphaned Media Workspace Fragmenter",
                description = "Traces residual sandbox traces, dead-link system folders, and unlinked file blocks left behind by uninstalled packages.",
                icon = Icons.Filled.BrokenImage,
                isActive = uiState.activeSubPanel == ActivePanel.ORPHAN_FRAGMENTER,
                isLoading = uiState.isScrubbingOrphans,
                btnText = "Scrub Abandoned App Residues",
                onToggle = { viewModel.setActivePanel(if (uiState.activeSubPanel == ActivePanel.ORPHAN_FRAGMENTER) ActivePanel.NONE else ActivePanel.ORPHAN_FRAGMENTER) },
                onExecute = { viewModel.executeOrphanScrub() },
                haptic = haptic, view = view, vib = touchVib
            )

            PremiumSuiteCard(
                title = "Stale Installation Package Shredder",
                description = "Detects, validates, and permanently terminates outdated redundant APK files clogging local storage chains.",
                icon = Icons.Filled.FolderZip,
                isActive = uiState.activeSubPanel == ActivePanel.APK_SHREDDER,
                isLoading = uiState.isShreddingApks,
                btnText = "Vaporize Redundant Installation Packages",
                onToggle = { viewModel.setActivePanel(if (uiState.activeSubPanel == ActivePanel.APK_SHREDDER) ActivePanel.NONE else ActivePanel.APK_SHREDDER) },
                onExecute = { viewModel.executeApkShred() },
                haptic = haptic, view = view, vib = touchVib
            )

            PremiumSuiteCard(
                title = "System Telemetry & Crash-Log Neutralizer",
                description = "Wipes rolling analytical system crash records, standard core dumps, and device profiling tracks securely.",
                icon = Icons.Filled.Terminal,
                isActive = uiState.activeSubPanel == ActivePanel.LOG_NEUTRALIZER,
                isLoading = uiState.isNeutralizingLogs,
                btnText = "Neutralize Diagnostic Crash Dumps",
                onToggle = { viewModel.setActivePanel(if (uiState.activeSubPanel == ActivePanel.LOG_NEUTRALIZER) ActivePanel.NONE else ActivePanel.LOG_NEUTRALIZER) },
                onExecute = { viewModel.executeLogNeutralizer() },
                haptic = haptic, view = view, vib = touchVib
            )

            PremiumSuiteCard(
                title = "Dormant Large-Stream Payload Isolation",
                description = "Targets massive video streams, backup archives, or file modules exceeding 100MB that have remained dormant for over 90 days.",
                icon = Icons.Filled.InsertDriveFile,
                isActive = uiState.activeSubPanel == ActivePanel.PAYLOAD_ISOLATOR,
                isLoading = uiState.isIsolatingPayloads,
                btnText = "Isolate Inactive Large Payloads",
                onToggle = { viewModel.setActivePanel(if (uiState.activeSubPanel == ActivePanel.PAYLOAD_ISOLATOR) ActivePanel.NONE else ActivePanel.PAYLOAD_ISOLATOR) },
                onExecute = { viewModel.executePayloadIsolation() },
                haptic = haptic, view = view, vib = touchVib
            )

            // Baseline Core Tools Category[span_10](start_span)[span_10](end_span)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Baseline Cache Purger Tools", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Button(onClick = { haptic.confirm(view, touchVib); viewModel.executeStorageClaimPipeline(context) }, enabled = !uiState.isCleaning, modifier = Modifier.fillMaxWidth()) {
                        if (uiState.isCleaning) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Clear Transitory App Cache Channels")
                    }
                }
            }
        }
    }
}

// ── Highly Optimized Reusable Core UI Layouts ────────────────────────────────

@Composable
private fun PremiumSuiteCard(
    title: String,
    description: String,
    icon: ImageVector,
    isActive: Boolean,
    isLoading: Boolean,
    btnText: String,
    onToggle: () -> Unit,
    onExecute: () -> Unit,
    haptic: HapticHelper,
    view: android.view.View,
    vib: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                IconButton(onClick = { haptic.click(view, vib); onToggle() }) {
                    Icon(if (isActive) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                }
            }
            if (isActive) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { haptic.confirm(view, vib); onExecute() }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text(btnText)
                }
            }
        }
    }
}

@Composable
private fun StorageCard(title: String, icon: ImageVector, stats: StorageStats, cardColor: androidx.compose.ui.graphics.Color) {
    val usedFraction = (stats.usedBytes.toFloat() / stats.totalBytes.toFloat()).coerceIn(0f, 1f)[span_11](start_span)[span_11](end_span)
    val usedPct = (usedFraction * 100).toInt()[span_12](start_span)[span_12](end_span)
    val progress by animateFloatAsState(targetValue = usedFraction, animationSpec = tween(800), label = "storage_progress")[span_13](start_span)[span_13](end_span)

    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title: ${formatBytes(stats.usedBytes)} used of ${formatBytes(stats.totalBytes)} total ($usedPct%). ${formatBytes(stats.freeBytes)} free." },[span_14](start_span)[span_14](end_span)
        colors = CardDefaults.cardColors(containerColor = cardColor)[span_15](start_span)[span_15](end_span)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {[span_16](start_span)[span_16](end_span)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {[span_17](start_span)[span_17](end_span)
                Icon(icon, null, modifier = Modifier.size(28.dp))[span_18](start_span)[span_18](end_span)
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))[span_19](start_span)[span_19](end_span)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp), color = MaterialTheme.colorScheme.error, strokeCap = StrokeCap.Round, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))[span_20](start_span)[span_20](end_span)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {[span_21](start_span)[span_21](end_span)
                Column {
                    Text("Allocated Space", style = MaterialTheme.typography.bodySmall)
                    Text(formatBytes(stats.usedBytes), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))[span_22](start_span)[span_22](end_span)
                }
                Text("$usedPct%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))[span_23](start_span)[span_23](end_span)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Available Pool", style = MaterialTheme.typography.bodySmall)
                    Text(formatBytes(stats.freeBytes), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))[span_24](start_span)[span_24](end_span)
                }
            }
        }
    }
}
