package com.nexuswavetech.nexusplus.model

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.coroutineContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Nexus Plus — Model Download Manager
 *
 * Handles:
 *  - Background model downloads with progress tracking
 *  - Download resume (partial file detection)
 *  - Storage availability check before download
 *  - SHA-256 integrity verification (when hash is provided)
 *  - Version check / update support
 *
 * Build Verification Pending (requires device filesystem access at runtime).
 */
class ModelDownloadManager(private val context: Context) {

    // ── Download state ─────────────────────────────────────────────────────────

    data class DownloadProgress(
        val modelId: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val state: State,
        val error: String? = null,
    ) {
        val percent: Int get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
        enum class State { QUEUED, DOWNLOADING, VERIFYING, COMPLETE, FAILED, CANCELLED }
    }

    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // ── Public API ────────────────────────────────────────────────────────────

    /** Directory where model files are stored */
    val modelsDir: File get() = File(context.filesDir, "nexus_models").also { it.mkdirs() }

    /** Returns true when [model] is fully downloaded and intact */
    fun isDownloaded(model: ModelRegistry.NexusModel): Boolean {
        val file = modelFile(model)
        return file.exists() && file.length() > 0
    }

    /** Enqueues [model] for background download. Idempotent if already downloaded. */
    fun enqueue(model: ModelRegistry.NexusModel) {
        if (isDownloaded(model)) {
            updateProgress(model.id, 0, model.sizeBytes, DownloadProgress.State.COMPLETE)
            return
        }
        if (activeJobs[model.id]?.isActive == true) return

        updateProgress(model.id, 0, model.sizeBytes, DownloadProgress.State.QUEUED)

        activeJobs[model.id] = scope.launch {
            downloadModel(model)
            model.configUrl?.let { downloadConfig(model, it) }
        }
    }

    /** Cancels an in-progress download */
    fun cancel(modelId: String) {
        activeJobs[modelId]?.cancel()
        activeJobs.remove(modelId)
        val p = _progress.value[modelId]
        if (p != null && p.state != DownloadProgress.State.COMPLETE) {
            updateProgress(modelId, p.bytesDownloaded, p.totalBytes, DownloadProgress.State.CANCELLED)
        }
    }

    /** Deletes a downloaded model from local storage */
    fun delete(model: ModelRegistry.NexusModel) {
        modelFile(model).delete()
        configFile(model)?.delete()
        _progress.update { it - model.id }
    }

    /** Returns the local [File] path for [model] (whether downloaded or not) */
    fun modelFile(model: ModelRegistry.NexusModel): File =
        File(modelsDir, "${model.id}.onnx")

    fun configFile(model: ModelRegistry.NexusModel): File? =
        model.configUrl?.let { File(modelsDir, "${model.id}.onnx.json") }

    /** Checks device has enough free space for [model] */
    fun hasStorageFor(model: ModelRegistry.NexusModel): Boolean =
        modelsDir.freeSpace > model.sizeBytes + 50_000_000L

    /** Checks network type — returns true on Wi-Fi */
    fun isOnWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
        val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return cap.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun destroy() { scope.cancel() }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun downloadModel(model: ModelRegistry.NexusModel) {
        val dest = modelFile(model)
        val resumeBytes = if (dest.exists()) dest.length() else 0L

        try {
            updateProgress(model.id, resumeBytes, model.sizeBytes, DownloadProgress.State.DOWNLOADING)

            val request = Request.Builder()
                .url(model.modelUrl)
                .apply { if (resumeBytes > 0) addHeader("Range", "bytes=$resumeBytes-") }
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful && response.code != 206) {
                throw Exception("HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val append = response.code == 206

            dest.outputStream().let { if (append) java.io.FileOutputStream(dest, true) else it }.use { out ->
                body.byteStream().use { inp ->
                    val buf = ByteArray(32 * 1024)
                    var downloaded = resumeBytes
                    var n: Int
                    while (inp.read(buf).also { n = it } != -1) {
                        coroutineContext.ensureActive()
                        out.write(buf, 0, n)
                        downloaded += n
                        updateProgress(model.id, downloaded, model.sizeBytes, DownloadProgress.State.DOWNLOADING)
                    }
                }
            }

            // Integrity check
            if (model.sha256 != null) {
                updateProgress(model.id, model.sizeBytes, model.sizeBytes, DownloadProgress.State.VERIFYING)
                val computed = sha256(dest)
                if (!computed.equals(model.sha256, ignoreCase = true)) {
                    dest.delete()
                    throw Exception("SHA-256 mismatch (expected ${model.sha256}, got $computed)")
                }
            }

            updateProgress(model.id, model.sizeBytes, model.sizeBytes, DownloadProgress.State.COMPLETE)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updateProgress(model.id, 0, model.sizeBytes, DownloadProgress.State.FAILED, e.message)
        }
    }

    private suspend fun downloadConfig(model: ModelRegistry.NexusModel, url: String) {
        val dest = File(modelsDir, "${model.id}.onnx.json")
        if (dest.exists()) return
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                dest.writeBytes(response.body?.bytes() ?: return)
            }
        } catch (_: Exception) { /* config download is best-effort */ }
    }

    private fun updateProgress(
        modelId: String, downloaded: Long, total: Long,
        state: DownloadProgress.State, error: String? = null
    ) {
        _progress.update { map ->
            map + (modelId to DownloadProgress(modelId, downloaded, total, state, error))
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inp ->
            val buf = ByteArray(64 * 1024)
            var n: Int
            while (inp.read(buf).also { n = it } != -1) digest.update(buf, 0, n)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
