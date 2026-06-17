package com.nexuswavetech.nexusplus.platform

/** Desktop implementation of TTS using freetts or espeak (stub). */
actual class PlatformTts {
    actual fun speak(text: String, language: String?) {
        // Desktop TTS via espeak or FreeTTS
    }

    actual fun stop() {}

    actual fun isSpeaking(): Boolean = false

    actual fun shutdown() {}
}

/** Desktop implementation of haptic feedback (no-op). */
actual class PlatformHaptics {
    actual fun performClick() {}

    actual fun performConfirm() {}
}

/** Desktop implementation of toast using SystemTray or console (stub). */
actual class PlatformToast {
    actual fun show(message: String, isLong: Boolean) {
        println("[Nexus Plus] $message")
    }
}

/** Desktop implementation of OCR using Tesseract (stub). */
actual class PlatformOcr {
    actual suspend fun recognizeText(imagePath: String): OcrResult {
        return OcrResult("Desktop OCR not yet implemented. Use Tesseract JNI.", isEmpty = false)
    }
}

/** Desktop implementation of URL handler using java.awt.Desktop. */
actual class PlatformUrlHandler {
    actual fun openUrl(url: String) {
        runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) }
    }
    actual fun openEmail(to: String, subject: String) {
        runCatching { java.awt.Desktop.getDesktop().mail(java.net.URI("mailto:$to?subject=$subject")) }
    }
}
