package com.nexuswavetech.nexusplus.features.tts

import java.util.Locale

/**
 * NSE 2.0 — Language & script detector.
 *
 * Uses Unicode block frequency analysis on a sliding window of the input
 * text.  Supports 16 scripts/languages without any ML model dependency,
 * keeping the cold-start latency near zero.
 *
 * Mix-mode support: [segmentByScript] splits a multi-language string into
 * ordered (text, locale) pairs so the engine can synthesise each segment
 * with its own voice.
 */
object NseLanguageDetector {

    // ── Script ranges ──────────────────────────────────────────────────────

    private val DEVANAGARI  = 0x0900..0x097F   // Hindi, Sanskrit, Marathi
    private val BENGALI     = 0x0980..0x09FF
    private val GUJARATI    = 0x0A80..0x0AFF
    private val GURMUKHI    = 0x0A00..0x0A7F   // Punjabi
    private val TELUGU      = 0x0C00..0x0C7F
    private val TAMIL       = 0x0B80..0x0BFF
    private val KANNADA     = 0x0C80..0x0CFF
    private val MALAYALAM   = 0x0D00..0x0D7F
    private val ARABIC      = 0x0600..0x06FF
    private val HEBREW      = 0x0590..0x05FF
    private val THAI        = 0x0E00..0x0E7F
    private val CJK_UNIFIED = 0x4E00..0x9FFF
    private val HIRAGANA    = 0x3040..0x309F
    private val KATAKANA    = 0x30A0..0x30FF
    private val HANGUL      = 0xAC00..0xD7AF
    private val CYRILLIC    = 0x0400..0x04FF   // Russian, Ukrainian…
    private val GREEK       = 0x0370..0x03FF

    private val SPANISH_MARKS = Regex("[áéíóúüñ¿¡]", RegexOption.IGNORE_CASE)
    private val FRENCH_MARKS  = Regex("[àâæçéèêëîïôœùûüÿ]", RegexOption.IGNORE_CASE)
    private val GERMAN_MARKS  = Regex("[äöüÄÖÜß]")

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Detect the dominant language/locale of [text].
     * Uses the first 400 characters for performance.
     */
    fun detect(text: String): Locale {
        val sample = text.take(400)
        val scores = scriptScores(sample)
        val dominant = scores.maxByOrNull { it.second }
        return dominant?.first ?: Locale.ENGLISH
    }

    /**
     * Split [text] into contiguous segments, each paired with the detected
     * locale for that segment.  Used by Mix mode.
     *
     * Strategy: walk character-by-character; when the detected script
     * changes, flush the current segment and start a new one.
     */
    fun segmentByScript(text: String): List<Pair<String, Locale>> {
        if (text.isBlank()) return emptyList()

        val result = mutableListOf<Pair<String, Locale>>()
        val buffer = StringBuilder()
        var currentLocale: Locale? = null

        for (ch in text) {
            val charLocale = charLocale(ch)
            if (currentLocale == null) {
                currentLocale = charLocale
            }
            if (charLocale != currentLocale && buffer.isNotBlank()) {
                result += buffer.toString() to currentLocale
                buffer.clear()
                currentLocale = charLocale
            }
            buffer.append(ch)
        }
        if (buffer.isNotBlank() && currentLocale != null) {
            result += buffer.toString() to currentLocale
        }

        return mergeShortSegments(result, minLength = 3)
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private fun scriptScores(sample: String): List<Pair<Locale, Int>> {
        var devanagari = 0; var bengali = 0; var gujarati = 0
        var gurmukhi = 0;   var telugu = 0;  var tamil = 0
        var kannada = 0;    var malayalam = 0; var arabic = 0
        var hebrew = 0;     var thai = 0;    var cjk = 0
        var hiragana = 0;   var katakana = 0; var hangul = 0
        var cyrillic = 0;   var greek = 0;   var latin = 0

        for (ch in sample) {
            val c = ch.code
            when {
                c in DEVANAGARI  -> devanagari++
                c in BENGALI     -> bengali++
                c in GUJARATI    -> gujarati++
                c in GURMUKHI    -> gurmukhi++
                c in TELUGU      -> telugu++
                c in TAMIL       -> tamil++
                c in KANNADA     -> kannada++
                c in MALAYALAM   -> malayalam++
                c in ARABIC      -> arabic++
                c in HEBREW      -> hebrew++
                c in THAI        -> thai++
                c in CJK_UNIFIED -> cjk++
                c in HIRAGANA    -> hiragana++
                c in KATAKANA    -> katakana++
                c in HANGUL      -> hangul++
                c in CYRILLIC    -> cyrillic++
                c in GREEK       -> greek++
                c in 0x0041..0x024F -> latin++
            }
        }

        val japanese = hiragana + katakana
        val scores = mutableListOf(
            Locale("hi", "IN") to devanagari,
            Locale("bn", "BD") to bengali,
            Locale("gu", "IN") to gujarati,
            Locale("pa", "IN") to gurmukhi,
            Locale("te", "IN") to telugu,
            Locale("ta", "IN") to tamil,
            Locale("kn", "IN") to kannada,
            Locale("ml", "IN") to malayalam,
            Locale("ar", "SA") to arabic,
            Locale("he", "IL") to hebrew,
            Locale("th", "TH") to thai,
            Locale.SIMPLIFIED_CHINESE to cjk,
            Locale.JAPANESE to japanese,
            Locale.KOREAN to hangul,
            Locale("ru", "RU") to cyrillic,
            Locale("el", "GR") to greek,
        )

        if (latin > 0) {
            val bestLatin: Pair<Locale, Int> = when {
                SPANISH_MARKS.containsMatchIn(sample) -> Locale("es", "ES") to latin
                FRENCH_MARKS.containsMatchIn(sample)  -> Locale.FRENCH to latin
                GERMAN_MARKS.containsMatchIn(sample)  -> Locale.GERMAN to latin
                else                                  -> Locale.ENGLISH to latin
            }
            scores += bestLatin
        }

        return scores.filter { it.second > 0 }
    }

    private fun charLocale(ch: Char): Locale {
        val c = ch.code
        return when {
            c in DEVANAGARI  -> Locale("hi", "IN")
            c in BENGALI     -> Locale("bn", "BD")
            c in GUJARATI    -> Locale("gu", "IN")
            c in GURMUKHI    -> Locale("pa", "IN")
            c in TELUGU      -> Locale("te", "IN")
            c in TAMIL       -> Locale("ta", "IN")
            c in KANNADA     -> Locale("kn", "IN")
            c in MALAYALAM   -> Locale("ml", "IN")
            c in ARABIC      -> Locale("ar", "SA")
            c in HEBREW      -> Locale("he", "IL")
            c in THAI        -> Locale("th", "TH")
            c in CJK_UNIFIED -> Locale.SIMPLIFIED_CHINESE
            c in HIRAGANA || c in KATAKANA -> Locale.JAPANESE
            c in HANGUL      -> Locale.KOREAN
            c in CYRILLIC    -> Locale("ru", "RU")
            c in GREEK       -> Locale("el", "GR")
            else             -> Locale.ENGLISH
        }
    }

    /**
     * Merge segments shorter than [minLength] into their neighbour to avoid
     * voice-switching for punctuation and whitespace-only runs.
     */
    private fun mergeShortSegments(
        segments: List<Pair<String, Locale>>,
        minLength: Int,
    ): List<Pair<String, Locale>> {
        if (segments.size <= 1) return segments
        val merged = mutableListOf<Pair<String, Locale>>()
        for ((text, locale) in segments) {
            if (text.trim().length < minLength && merged.isNotEmpty()) {
                val (prevText, prevLocale) = merged.last()
                merged[merged.lastIndex] = (prevText + text) to prevLocale
            } else {
                merged += text to locale
            }
        }
        return merged
    }
}
