package com.nexuswavetech.nexusplus.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Nexus Plus — First Launch Manager
 *
 * Responsibilities:
 *  1. Detect first app launch (and upgrades).
 *  2. Check which default models are missing.
 *  3. Queue missing default models for download (Wi-Fi preferred).
 *  4. Track app version to detect updates and re-check models.
 *
 * Implements Offline-first: only default models are queued automatically.
 * Heavy optional models must be triggered explicitly by the user.
 *
 * Build Verification Pending (runtime model file checks needed on device).
 */
class FirstLaunchManager(
    private val context: Context,
    private val downloadManager: ModelDownloadManager,
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── State ─────────────────────────────────────────────────────────────────

    val isFirstLaunch: Boolean get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    val appVersion: Int get() = prefs.getInt(KEY_APP_VERSION, 0)

    val isUpgrade: Boolean get() = !isFirstLaunch && appVersion < currentAppVersion()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Call this once from [NexusPlusApplication.onCreate] after Koin is ready.
     *
     * Actions:
     *  - On first launch: queue all default models.
     *  - On upgrade: check if new default models have been added; queue missing ones.
     *  - Always: mark launch as seen.
     */
    fun checkAndQueue() {
        scope.launch {
            val missing = findMissingDefaultModels()
            if (missing.isNotEmpty()) {
                val onWifi = downloadManager.isOnWifi(context)
                missing.forEach { model ->
                    if (!model.requiresWifi || onWifi) {
                        if (downloadManager.hasStorageFor(model)) {
                            downloadManager.enqueue(model)
                        }
                    }
                }
            }
            markLaunched()
        }
    }

    /**
     * Returns the list of default models that are not yet downloaded.
     */
    fun findMissingDefaultModels(): List<ModelRegistry.NexusModel> =
        ModelRegistry.defaultModels().filter { model -> !downloadManager.isDownloaded(model) }

    /**
     * Returns true when all default models are present.
     */
    fun allDefaultModelsReady(): Boolean =
        ModelRegistry.defaultModels().all { model -> downloadManager.isDownloaded(model) }

    /**
     * Force-queue a specific model (called when user explicitly requests a download).
     */
    fun enqueueModel(model: ModelRegistry.NexusModel) {
        downloadManager.enqueue(model)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun markLaunched() {
        prefs.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .putInt(KEY_APP_VERSION, currentAppVersion())
            .apply()
    }

    private fun currentAppVersion(): Int = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode
    } catch (_: Exception) { 1 }

    companion object {
        private const val PREFS_NAME      = "nexus_first_launch"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_APP_VERSION  = "app_version_code"
    }
}
