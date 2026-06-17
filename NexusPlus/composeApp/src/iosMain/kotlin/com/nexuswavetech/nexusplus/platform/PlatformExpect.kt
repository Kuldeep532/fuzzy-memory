package com.nexuswavetech.nexusplus.platform

import platform.AVFoundation.AVSpeechSynthesizer
import platform.AVFoundation.AVSpeechUtterance

/** iOS implementation of TTS using AVFoundation. */
actual class PlatformTts {
    private val synthesizer = AVSpeechSynthesizer()

    actual fun speak(text: String, language: String?) {
        val utterance = AVSpeechUtterance(string = text)
        language?.let { utterance.voice = platform.AVFoundation.AVSpeechSynthesisVoice(language = it) }
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(platform.AVFoundation.AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual fun isSpeaking(): Boolean = synthesizer.speaking

    actual fun shutdown() {
        stop()
    }
}

/** iOS implementation of haptic feedback using UIImpactFeedbackGenerator. */
actual class PlatformHaptics {
    actual fun performClick() {
        // UIImpactFeedbackGenerator(style = .light).impactOccurred()
    }

    actual fun performConfirm() {
        // UIImpactFeedbackGenerator(style = .medium).impactOccurred()
    }
}

/** iOS implementation of toast using UIAlertController. */
actual class PlatformToast {
    actual fun show(message: String, isLong: Boolean) {
        // UIAlertController with single action or custom toast view
    }
}

/** iOS implementation of OCR using Vision framework (placeholder). */
actual class PlatformOcr {
    actual suspend fun recognizeText(imagePath: String): OcrResult {
        return OcrResult("iOS OCR not yet implemented. Use Vision framework.", isEmpty = false)
    }
}

/** iOS implementation of URL handler using UIApplication. */
actual class PlatformUrlHandler {
    actual fun openUrl(url: String) {
        // platform.UIApplication.sharedApplication.openURL(platform.NSURL(string = url))
    }
    actual fun openEmail(to: String, subject: String) {
        // platform.UIApplication.sharedApplication.openURL(platform.NSURL(string = "mailto:$to?subject=$subject"))
    }
}
