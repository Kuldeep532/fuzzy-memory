package com.nexuswavetech.nexusplus.features.tts

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.nexuswavetech.nexusplus.core.SettingsRepository
import org.koin.android.ext.android.inject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * NSE 4.0 — Auto TTS Screen Reader Accessibility Service.
 *
 * Receives accessibility events system-wide and automatically speaks
 * screen content using the Nexus Auto Speech Engine pipeline. Supports four
 * screen-reader modes: Auto, Single, Dual, and Mixed.
 *
 * Features:
 *  - Smart event filtering: skips non-actionable events, suppresses
 *    duplicate announcements, and optionally ignores notifications.
 *  - Window-change detection: announces app title changes when navigating.
 *  - Focus tracking: follows accessibility focus in real-time.
 *  - Continuous reading: reads all visible content in a window.
 *  - Deduplication: prevents repeated speech for the same content.
 *  - Dynamic content detection: responds to live regions (chat, feeds).
 */
class NseAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "NseA11yService"
        private const val DEDUP_WINDOW_MS = 2_000L
        private const val MIN_TEXT_LENGTH = 2
        private const val WINDOW_CHANGE_DEBOUNCE_MS = 400L
    }

    private val settings: com.nexuswavetech.nexusplus.core.SettingsRepository by inject()
    private val repository: NseRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var lastSpokenText: String = ""
    private var lastSpokenTime: Long = 0L
    private var lastWindowTitle: String = ""
    private var lastWindowChangeTime: Long = 0L
    private var lastFocusedNode: String = ""
    private var currentPackage: String = ""
    private var isServiceEnabled: Boolean = false

    private val spokenTexts = ConcurrentHashMap<String, Long>()
    private var isContinuousReading: Boolean = false

    // ── Service lifecycle ────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "onServiceConnected")

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_SELECTED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_HOVER_ENTER or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_ANNOUNCEMENT or
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
            notificationTimeout = 100L
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }

        isServiceEnabled = true
        repository.initialise()

        scope.launch {
            settings.ttsAutoStart.collect { autoStart ->
                if (autoStart && !isServiceEnabled) {
                    isServiceEnabled = true
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isServiceEnabled) return
        if (isServiceBlocked()) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                handleWindowStateChanged(event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                handleWindowContentChanged(event)
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_SELECTED,
            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER ->
                handleFocusEvent(event)
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ->
                handleNotificationEvent(event)
            AccessibilityEvent.TYPE_ANNOUNCEMENT ->
                handleAnnouncementEvent(event)
            else -> Unit
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
        repository.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        isServiceEnabled = false
        handler.removeCallbacksAndMessages(null)
        repository.stop()
        scope.cancel()
    }

    // ── Event handlers ───────────────────────────────────────────────────────

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        currentPackage = pkg

        val windowTitle = event.className?.toString() ?: ""
        if (windowTitle.isBlank() || windowTitle == lastWindowTitle) return

        // Debounce rapid window changes
        val now = System.currentTimeMillis()
        if (now - lastWindowChangeTime < WINDOW_CHANGE_DEBOUNCE_MS) return

        scope.launch {
            val detectWindow = settings.ttsWindowChangeDetection.first()
            if (detectWindow) {
                lastWindowTitle = windowTitle
                lastWindowChangeTime = now
                speakIfNew("Opened $windowTitle", priority = true)
            }
        }
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        val source = event.source ?: return
        val text = extractText(source) ?: return

        scope.launch {
            val continuous = settings.ttsContinuousRead.first()
            if (continuous && !isContinuousReading) {
                // Start a delayed continuous read of the window
                startContinuousReading(source)
            }
        }
        source.recycle()
    }

    private fun handleFocusEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        val text = extractText(source) ?: return

        scope.launch {
            val focusTrack = settings.ttsFocusTracking.first()
            if (focusTrack) {
                val nodeId = source.viewIdResourceName ?: text.take(40)
                if (nodeId != lastFocusedNode) {
                    lastFocusedNode = nodeId
                    speakIfNew(text)
                }
            }
        }
        source.recycle()
    }

    private fun handleNotificationEvent(event: AccessibilityEvent) {
        scope.launch {
            val filter = settings.ttsNotificationFilter.first()
            if (filter) return@launch

            val text = event.text?.joinToString(" ") ?: return@launch
            if (text.isNotBlank()) {
                speakIfNew("Notification: $text", priority = true)
            }
        }
    }

    private fun handleAnnouncementEvent(event: AccessibilityEvent) {
        val text = event.text?.joinToString(" ") ?: return
        if (text.isNotBlank()) {
            speakIfNew(text, priority = true)
        }
    }

    // ── Continuous reading ───────────────────────────────────────────────────

    private fun startContinuousReading(root: AccessibilityNodeInfo) {
        isContinuousReading = true
        val texts = mutableListOf<String>()
        collectAllText(root, texts)
        root.recycle()

        if (texts.isNotEmpty()) {
            val combined = texts.joinToString(". ")
            speakText(combined)
        }

        // Reset after a short delay
        handler.postDelayed({ isContinuousReading = false }, 3_000L)
    }

    private fun collectAllText(node: AccessibilityNodeInfo, out: MutableList<String>) {
        if (!node.isVisibleToUser) return
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank() && text.length >= MIN_TEXT_LENGTH) {
            out.add(text)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectAllText(child, out)
            child.recycle()
        }
    }

    // ── Speech helpers ───────────────────────────────────────────────────────

    private fun speakIfNew(text: String, priority: Boolean = false) {
        if (text.length < MIN_TEXT_LENGTH) return

        val now = System.currentTimeMillis()
        val dedup = spokenTexts[text]
        if (dedup != null && (now - dedup) < DEDUP_WINDOW_MS) {
            return
        }

        spokenTexts[text] = now
        cleanSpokenTexts(now)

        // Also check immediate duplicate
        if (text == lastSpokenText && (now - lastSpokenTime) < DEDUP_WINDOW_MS) {
            return
        }

        lastSpokenText = text
        lastSpokenTime = now

        speakText(text)
    }

    private fun speakText(text: String) {
        scope.launch {
            val mode = resolveScreenReaderMode()
            val rate = settings.ttsDefaultRate.first().coerceIn(0.25f, 3.0f)
            val pitch = settings.ttsPitch.first().coerceIn(0.5f, 2.0f)

            repository.speak(
                NseSpeechRequest(
                    text = text,
                    mode = mode,
                    pitch = pitch,
                    speechRate = rate,
                )
            )
        }
    }

    private suspend fun resolveScreenReaderMode(): NseSpeechMode {
        val modeStr = settings.ttsScreenReaderMode.first()
        val lang = settings.ttsDefaultLanguage.first()
        val secondary = settings.ttsSecondaryLanguage.first()
        val voiceSel = settings.ttsVoiceSelection.first()

        val primaryLocale = if (lang == SettingsRepository.TTS_LANG_AUTO)
            NseLocale(Locale.getDefault().language, Locale.getDefault().country)
        else NseLocale.forLanguageTag(lang)
        val secondaryLocale = if (secondary == SettingsRepository.TTS_LANG_AUTO) NseLocale.ENGLISH
        else NseLocale.forLanguageTag(secondary)

        return when (modeStr) {
            SettingsRepository.TTS_MODE_SINGLE -> {
                val voiceLocale = if (voiceSel.isNotBlank()) NseLocale.forLanguageTag(voiceSel)
                else primaryLocale
                NseSpeechMode.SingleVoice(voiceLocale)
            }
            SettingsRepository.TTS_MODE_DUAL ->
                NseSpeechMode.DualVoice(primaryLocale, secondaryLocale)
            SettingsRepository.TTS_MODE_MIXED -> NseSpeechMode.Mix
            else -> NseSpeechMode.Auto
        }
    }

    // ── Text extraction ──────────────────────────────────────────────────────

    private fun extractText(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        val text = node.text?.toString()
            ?: node.contentDescription?.toString()
            ?: node.hintText?.toString()
        return text?.trim()?.takeIf { it.length >= MIN_TEXT_LENGTH }
    }

    private fun isServiceBlocked(): Boolean {
        return currentPackage in BLOCKED_PACKAGES
    }

    private fun cleanSpokenTexts(now: Long) {
        val cutoff = now - DEDUP_WINDOW_MS * 5
        spokenTexts.entries.removeAll { it.value < cutoff }
    }

    private val BLOCKED_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.nexuswavetech.nexusplus",
    )
}
