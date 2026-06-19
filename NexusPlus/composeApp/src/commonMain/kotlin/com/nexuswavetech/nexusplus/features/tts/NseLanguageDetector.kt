package com.nexuswavetech.nexusplus.features.tts

/**
 * NSE 2.0 — Language & script detector.
 *
 * Uses Unicode block frequency analysis on a sliding window of the input
 * text. Supports 16 scripts/languages without any ML model dependency,
 * keeping the cold-start latency near zero.
 */
object NseLanguageDetector {

    private val DEVANAGARI  = 0x0900..0x097F
    private val BENGALI     = 0x0980..0x09FF
    private val GUJARATI    = 0x0A80..0x0AFF
    private val GURMUKHI    = 0x0A00..0x0A7F
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
    private val CYRILLIC    = 0x0400..0x04FF
    private val GREEK       = 0x0370..0x03FF

    private val SPANISH_MARKS = Regex("[áéíóúüñ¿¡]", RegexOption.IGNORE_CASE)
    private val FRENCH_MARKS  = Regex("[àâæçéèêëîïôœùûüÿ]", RegexOption.IGNORE_CASE)
    private val GERMAN_MARKS  = Regex("[äöüÄÖÜß]")

    fun detect(text: String): NseLocale {
        val sample = text.take(400)
        val scores = scriptScores(sample)
        val dominant = scores.maxByOrNull { it.second }
        return dominant?.first ?: NseLocale.ENGLISH
    }

    fun segmentByScript(text: String): List<Pair<String, NseLocale>> {
        if (text.isBlank()) return emptyList()

        val result = mutableListOf<Pair<String, NseLocale>>()
        val buffer = StringBuilder()
        var currentLocale: NseLocale? = null

        for (ch in text) {
            val charLocale = charLocale(ch)
            if (currentLocale == null) currentLocale = charLocale
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

    private fun scriptScores(sample: String): List<Pair<NseLocale, Int>> {
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
            NseLocale("hi", "IN") to devanagari,
            NseLocale("bn", "BD") to bengali,
            NseLocale("gu", "IN") to gujarati,
            NseLocale("pa", "IN") to gurmukhi,
            NseLocale("te", "IN") to telugu,
            NseLocale("ta", "IN") to tamil,
            NseLocale("kn", "IN") to kannada,
            NseLocale("ml", "IN") to malayalam,
            NseLocale("ar", "SA") to arabic,
            NseLocale("he", "IL") to hebrew,
            NseLocale("th", "TH") to thai,
            NseLocale.SIMPLIFIED_CHINESE to cjk,
            NseLocale.JAPANESE to japanese,
            NseLocale.KOREAN to hangul,
            NseLocale("ru", "RU") to cyrillic,
            NseLocale("el", "GR") to greek,
        )

        if (latin > 0) {
            val bestLatin: Pair<NseLocale, Int> = when {
                SPANISH_MARKS.containsMatchIn(sample) -> NseLocale("es", "ES") to latin
                FRENCH_MARKS.containsMatchIn(sample)  -> NseLocale.FRENCH to latin
                GERMAN_MARKS.containsMatchIn(sample)  -> NseLocale.GERMAN to latin
                else                                  -> NseLocale.ENGLISH to latin
            }
            scores += bestLatin
        }

        return scores.filter { it.second > 0 }
    }

    private fun charLocale(ch: Char): NseLocale {
        val c = ch.code
        return when {
            c in DEVANAGARI  -> NseLocale("hi", "IN")
            c in BENGALI     -> NseLocale("bn", "BD")
            c in GUJARATI    -> NseLocale("gu", "IN")
            c in GURMUKHI    -> NseLocale("pa", "IN")
            c in TELUGU      -> NseLocale("te", "IN")
            c in TAMIL       -> NseLocale("ta", "IN")
            c in KANNADA     -> NseLocale("kn", "IN")
            c in MALAYALAM   -> NseLocale("ml", "IN")
            c in ARABIC      -> NseLocale("ar", "SA")
            c in HEBREW      -> NseLocale("he", "IL")
            c in THAI        -> NseLocale("th", "TH")
            c in CJK_UNIFIED -> NseLocale.SIMPLIFIED_CHINESE
            c in HIRAGANA || c in KATAKANA -> NseLocale.JAPANESE
            c in HANGUL      -> NseLocale.KOREAN
            c in CYRILLIC    -> NseLocale("ru", "RU")
            c in GREEK       -> NseLocale("el", "GR")
            else             -> NseLocale.ENGLISH
        }
    }

    private fun mergeShortSegments(
        segments: List<Pair<String, NseLocale>>,
        minLength: Int,
    ): List<Pair<String, NseLocale>> {
        if (segments.size <= 1) return segments
        val merged = mutableListOf<Pair<String, NseLocale>>()
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
