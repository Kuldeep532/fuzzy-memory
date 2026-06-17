package com.nexuswavetech.nexusplus.features.tts

/**
 * NSE 3.0 — Sentence pipeline splitter.
 *
 * Splits input text into sentence-level chunks so the pipeline engine can
 * pre-synthesize the next sentence while the current one plays.
 *
 * Handles:
 *  - English / Latin sentence terminators: . ! ?
 *  - Devanagari/Hindi purna viram: ।
 *  - Abbreviations (Mr., Dr., etc.) — NOT split on those dots
 *  - Ellipsis (…) — treated as a single pause, not a split
 *  - Very long clauses — force-split at natural pause (comma/semicolon)
 *  - Mixed-script text
 */
object NseSentenceSplitter {

    private const val MAX_CHUNK = 180   // chars; avoids TTS engine limits
    private const val MIN_CHUNK = 8     // don't create tiny useless chunks

    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr",
        "vs", "etc", "approx", "est", "dept", "govt",
        // Hindi abbreviations
        "श्री", "डॉ", "प्रो"
    )

    /** Split [text] into a list of non-blank sentence strings. */
    fun split(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.length <= MAX_CHUNK) return listOf(trimmed).filter { it.isNotBlank() }

        val result  = mutableListOf<String>()
        val current = StringBuilder()

        var i = 0
        while (i < trimmed.length) {
            val ch = trimmed[i]
            current.append(ch)

            when {
                // ── Devanagari sentence end ──────────────────────────────────
                ch == '।' || ch == '॥' -> {
                    flush(current, result)
                }

                // ── Western sentence end ─────────────────────────────────────
                ch in ".!?" -> {
                    val nextCh   = trimmed.getOrNull(i + 1)
                    val prevWord = currentWord(trimmed, i)
                    val isAbbrev = ch == '.' && prevWord in ABBREVIATIONS
                    val isEllipsis = ch == '.' &&
                        (trimmed.getOrNull(i - 1) == '.' || trimmed.getOrNull(i + 1) == '.')

                    if (!isAbbrev && !isEllipsis) {
                        // Only split if next meaningful char is uppercase / digit / EOT
                        val peekNext = peekNextNonSpace(trimmed, i + 1)
                        if (peekNext == null ||
                            peekNext.isUpperCase() ||
                            peekNext.isDigit() ||
                            peekNext == '"' || peekNext == '\'' || peekNext == '।' || peekNext == '-'
                        ) {
                            flush(current, result)
                        }
                    }
                }

                // ── Force-break very long clauses ───────────────────────────
                current.length >= MAX_CHUNK -> {
                    val breakIdx = current.indexOfLast { it == ',' || it == ';' || it == ':' }
                    if (breakIdx > MAX_CHUNK / 2) {
                        val part = current.substring(0, breakIdx + 1).trim()
                        val rest = current.substring(breakIdx + 1)
                        if (part.isNotBlank()) result.add(part)
                        current.clear()
                        current.append(rest)
                    } else {
                        flush(current, result)
                    }
                }
            }
            i++
        }

        // Flush remaining text
        flush(current, result)

        // Merge very short fragments into the previous sentence
        return mergeTinyFragments(result)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun flush(sb: StringBuilder, list: MutableList<String>) {
        val s = sb.toString().trim()
        if (s.isNotBlank()) list.add(s)
        sb.clear()
    }

    private fun currentWord(text: String, dotPos: Int): String {
        var start = dotPos - 1
        while (start >= 0 && text[start].isLetter()) start--
        return text.substring(start + 1, dotPos).lowercase()
    }

    private fun peekNextNonSpace(text: String, from: Int): Char? {
        var idx = from
        while (idx < text.length && text[idx] == ' ') idx++
        return text.getOrNull(idx)
    }

    private fun mergeTinyFragments(parts: List<String>): List<String> {
        if (parts.size <= 1) return parts
        val merged = mutableListOf<String>()
        for (part in parts) {
            if (part.length < MIN_CHUNK && merged.isNotEmpty()) {
                merged[merged.size - 1] = "${merged.last()} $part"
            } else {
                merged.add(part)
            }
        }
        return merged
    }
}
