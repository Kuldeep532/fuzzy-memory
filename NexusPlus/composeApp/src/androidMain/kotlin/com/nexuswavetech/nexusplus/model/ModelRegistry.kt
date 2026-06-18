package com.nexuswavetech.nexusplus.model

/**
 * Nexus Plus — Model Registry
 *
 * Single source of truth for all downloadable AI/voice models.
 *
 * URL Policy (from NEXUS PLUS MASTER EXECUTION DIRECTIVE):
 *  - Only Official GitHub Releases, Hugging Face Repos, or Official Vendor Sources.
 *  - No guessed or approximated URLs.
 *
 * Sources used here:
 *  - Piper TTS voices: https://huggingface.co/rhasspy/piper-voices (official rhasspy repository)
 */
object ModelRegistry {

    // ── Model types ───────────────────────────────────────────────────────────

    enum class ModelType { VOICE_TTS, LLM_GGUF, ONNX_EMBED }
    enum class ModelQuality { TINY, LOW, MEDIUM, HIGH }

    data class NexusModel(
        val id: String,
        val name: String,
        val description: String,
        val type: ModelType,
        val quality: ModelQuality,
        val language: String,
        val sizeBytes: Long,
        val modelUrl: String,
        val configUrl: String? = null,
        val sha256: String? = null,
        val isDefault: Boolean = false,
        val requiresWifi: Boolean = true,
    )

    // ── Built-in Piper TTS voices (Nexus Hindi & Nexus English) ──────────────
    // Source: https://huggingface.co/rhasspy/piper-voices
    // These are the two default lightweight voices required by the directive.

    val NEXUS_ENGLISH = NexusModel(
        id          = "nexus_english_lessac_medium",
        name        = "Nexus English",
        description = "Clear American English female voice. Offline TTS, fast startup, low memory.",
        type        = ModelType.VOICE_TTS,
        quality     = ModelQuality.MEDIUM,
        language    = "en-US",
        sizeBytes   = 63_000_000L,
        modelUrl    = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/lessac/medium/en_US-lessac-medium.onnx",
        configUrl   = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/lessac/medium/en_US-lessac-medium.onnx.json",
        isDefault   = true,
        requiresWifi= false,
    )

    val NEXUS_HINDI = NexusModel(
        id          = "nexus_hindi_google_medium",
        name        = "Nexus Hindi",
        description = "Natural Hindi voice for offline TTS. Fast startup, low memory.",
        type        = ModelType.VOICE_TTS,
        quality     = ModelQuality.MEDIUM,
        language    = "hi-IN",
        sizeBytes   = 60_000_000L,
        modelUrl    = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/hi/hi_IN/google/medium/hi_IN-google-medium.onnx",
        configUrl   = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/hi/hi_IN/google/medium/hi_IN-google-medium.onnx.json",
        isDefault   = true,
        requiresWifi= false,
    )

    // ── Additional optional voice packs ───────────────────────────────────────
    // Source: https://huggingface.co/rhasspy/piper-voices

    val VOICE_EN_RYAN_MEDIUM = NexusModel(
        id          = "en_us_ryan_medium",
        name        = "Ryan (English Male)",
        description = "American English male voice, medium quality.",
        type        = ModelType.VOICE_TTS,
        quality     = ModelQuality.MEDIUM,
        language    = "en-US",
        sizeBytes   = 64_000_000L,
        modelUrl    = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ryan/medium/en_US-ryan-medium.onnx",
        configUrl   = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ryan/medium/en_US-ryan-medium.onnx.json",
        requiresWifi= true,
    )

    val VOICE_EN_JENNY_DIOCO_MEDIUM = NexusModel(
        id          = "en_gb_jenny_dioco_medium",
        name        = "Jenny (English UK Female)",
        description = "British English female voice, medium quality.",
        type        = ModelType.VOICE_TTS,
        quality     = ModelQuality.MEDIUM,
        language    = "en-GB",
        sizeBytes   = 48_000_000L,
        modelUrl    = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_GB/jenny_dioco/medium/en_GB-jenny_dioco-medium.onnx",
        configUrl   = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_GB/jenny_dioco/medium/en_GB-jenny_dioco-medium.onnx.json",
        requiresWifi= true,
    )

    // ── All registered models ─────────────────────────────────────────────────

    val ALL: List<NexusModel> = listOf(
        NEXUS_ENGLISH,
        NEXUS_HINDI,
        VOICE_EN_RYAN_MEDIUM,
        VOICE_EN_JENNY_DIOCO_MEDIUM,
    )

    val DEFAULT_MODELS: List<NexusModel> = ALL.filter { it.isDefault }

    fun findById(id: String): NexusModel? = ALL.find { it.id == id }
}
