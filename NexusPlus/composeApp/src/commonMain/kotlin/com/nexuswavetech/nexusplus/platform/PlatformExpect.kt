package com.nexuswavetech.nexusplus.platform

/** Platform-specific interface for TTS operations. */
expect interface PlatformTts {
    fun speak(text: String, language: String? = null)
    fun stop()
    fun isSpeaking(): Boolean
    fun shutdown()
}

/** Platform-specific interface for haptic feedback. */
expect interface PlatformHaptics {
    fun performClick()
    fun performConfirm()
}

/** Platform-specific interface for toast/snackbar messages. */
expect interface PlatformToast {
    fun show(message: String, isLong: Boolean = false)
}

/** Platform-specific interface for OCR (Optical Character Recognition). */
expect interface PlatformOcr {
    /** Recognize text from an image file at the given path. */
    suspend fun recognizeText(imagePath: String): OcrResult
}

/** Platform-specific interface for opening URLs / external links. */
expect interface PlatformUrlHandler {
    /** Open the given URL in the platform's default browser/app. */
    fun openUrl(url: String)
    /** Open an email composer with the given address and subject. */
    fun openEmail(to: String, subject: String)
}

/** Result of OCR operation. */
data class OcrResult(
    val text: String,
    val blocks: List<OcrBlock> = emptyList(),
    val isEmpty: Boolean = text.isBlank()
)

/** A single block of recognized text with optional bounding box. */
data class OcrBlock(
    val text: String,
    val confidence: Float? = null,
)
