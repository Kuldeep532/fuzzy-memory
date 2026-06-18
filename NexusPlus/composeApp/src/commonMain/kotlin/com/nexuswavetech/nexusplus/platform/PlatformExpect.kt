package com.nexuswavetech.nexusplus.platform

/** Platform-specific class for TTS operations. */
expect class PlatformTts {
    fun speak(text: String, language: String? = null)
    fun stop()
    fun isSpeaking(): Boolean
    fun shutdown()
}

/** Platform-specific class for haptic feedback. */
expect class PlatformHaptics {
    fun performClick()
    fun performConfirm()
}

/** Platform-specific class for toast/snackbar messages. */
expect class PlatformToast {
    fun show(message: String, isLong: Boolean = false)
}

/** Platform-specific class for OCR (Optical Character Recognition). */
expect class PlatformOcr {
    suspend fun recognizeText(imagePath: String): OcrResult
}

/** Platform-specific class for opening URLs / external links. */
expect class PlatformUrlHandler {
    fun openUrl(url: String)
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
