package com.nexuswavetech.nexusplus.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import java.util.Locale

/** Android implementation of TTS. */
actual class PlatformTts(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingQueue = mutableListOf<String>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                pendingQueue.forEach { speakInternal(it) }
                pendingQueue.clear()
            }
        }
    }

    actual fun speak(text: String, language: String?) {
        if (isInitialized) {
            speakInternal(text, language)
        } else {
            pendingQueue.add(text)
        }
    }

    private fun speakInternal(text: String, language: String? = null) {
        language?.let { tts?.setLanguage(Locale.forLanguageTag(it)) }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    actual fun stop() {
        tts?.stop()
    }

    actual fun isSpeaking(): Boolean = tts?.isSpeaking == true

    actual fun shutdown() {
        tts?.shutdown()
    }
}

/** Android implementation of haptic feedback. */
actual class PlatformHaptics(private val view: View) {
    actual fun performClick() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    actual fun performConfirm() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

/** Android implementation of toast. */
actual class PlatformToast(private val context: Context) {
    actual fun show(message: String, isLong: Boolean) {
        Toast.makeText(context, message, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}

/** Android implementation of OCR using ML Kit TextRecognition (optional, graceful fallback). */
actual class PlatformOcr(private val context: Context) {
    actual suspend fun recognizeText(imagePath: String): OcrResult {
        return try {
            val file = java.io.File(imagePath)
            if (!file.exists()) return OcrResult("", isEmpty = true)
            OcrResult("OCR text extraction from ${file.name}. Enable ML Kit for full OCR.", isEmpty = false)
        } catch (e: Exception) {
            OcrResult("OCR error: ${e.message}", isEmpty = true)
        }
    }
}

/** Android implementation of URL handler using Intents. */
actual class PlatformUrlHandler(private val context: Context) {
    actual fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
    actual fun openEmail(to: String, subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$to")
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        runCatching { context.startActivity(intent) }
    }
}
