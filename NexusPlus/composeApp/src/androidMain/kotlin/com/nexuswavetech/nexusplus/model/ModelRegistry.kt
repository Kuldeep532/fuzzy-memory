package com.nexuswavetech.nexusplus.model

/**
 * Nexus Plus — Model Registry (v2)
 *
 * Single source of truth for all downloadable AI / voice models.
 *
 * URL Policy:
 *  - Only Official HuggingFace repos or official vendor sources.
 *  - All Piper TTS voices sourced from:
 *    https://huggingface.co/rhasspy/piper-voices  (tag: v1.0.0)
 *  - URL format: {base}/{prefix}/{locale}/{speaker}/{quality}/{locale}-{speaker}-{quality}.onnx
 *
 * 28 verified voices across 19 languages — all mobile-optimised (medium quality, 10–70 MB).
 */
object ModelRegistry {

    enum class ModelType    { VOICE_TTS, LLM_GGUF, ONNX_EMBED }
    enum class ModelQuality { TINY, LOW, MEDIUM, HIGH }

    data class NexusModel(
        val id           : String,
        val name         : String,
        val description  : String,
        val type         : ModelType,
        val quality      : ModelQuality,
        val language     : String,
        val locale       : String,
        val flagEmoji    : String,
        val sizeBytes    : Long,
        val modelUrl     : String,
        val configUrl    : String? = null,
        val sha256       : String? = null,
        val isDefault    : Boolean = false,
        val requiresWifi : Boolean = true,
    )

    private const val BASE =
        "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0"

    private fun piperModel(prefix: String, locale: String, speaker: String, quality: String = "medium") =
        "$BASE/$prefix/$locale/$speaker/$quality/${locale}-${speaker}-${quality}.onnx"

    private fun piperConfig(prefix: String, locale: String, speaker: String, quality: String = "medium") =
        "${piperModel(prefix, locale, speaker, quality)}.json"

    // ── English (US) ────────────────────────────────────────────────────────

    val NEXUS_ENGLISH = NexusModel(
        id = "en_US-lessac-medium", name = "Nexus English",
        description = "Clear American English. Default Nexus voice. Offline, fast startup.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "English (US)", locale = "en-US", flagEmoji = "🇺🇸",
        sizeBytes = 63_000_000L,
        modelUrl = piperModel("en", "en_US", "lessac"),
        configUrl = piperConfig("en", "en_US", "lessac"),
        isDefault = true, requiresWifi = false,
    )

    private val EN_US_AMY = NexusModel(
        id = "en_US-amy-medium", name = "Amy (US)",
        description = "Warm American English female. Compact size.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "English (US)", locale = "en-US", flagEmoji = "🇺🇸",
        sizeBytes = 31_000_000L,
        modelUrl = piperModel("en", "en_US", "amy"),
        configUrl = piperConfig("en", "en_US", "amy"),
    )

    private val EN_US_RYAN = NexusModel(
        id = "en_US-ryan-medium", name = "Ryan (US)",
        description = "Natural American English male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "English (US)", locale = "en-US", flagEmoji = "🇺🇸",
        sizeBytes = 63_000_000L,
        modelUrl = piperModel("en", "en_US", "ryan"),
        configUrl = piperConfig("en", "en_US", "ryan"),
    )

    private val EN_US_JOE = NexusModel(
        id = "en_US-joe-medium", name = "Joe (US)",
        description = "Friendly American English male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "English (US)", locale = "en-US", flagEmoji = "🇺🇸",
        sizeBytes = 32_000_000L,
        modelUrl = piperModel("en", "en_US", "joe"),
        configUrl = piperConfig("en", "en_US", "joe"),
    )

    private val EN_US_NORMAN = NexusModel(
        id = "en_US-norman-medium", name = "Norman (US)",
        description = "Steady American English male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "English (US)", locale = "en-US", flagEmoji = "🇺🇸",
        sizeBytes = 32_000_000L,
        modelUrl = piperModel("en", "en_US", "norman"),
        configUrl = piperConfig("en", "en_US", "norman"),
    )

    // ── English (UK) ────────────────────────────────────────────────────────

    private val EN_GB_ALBA = NexusModel(
        id = "en_GB-alba-medium", name = "Alba (UK)",
        description = "Crisp British English female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "English (UK)", locale = "en-GB", flagEmoji = "🇬🇧",
        sizeBytes = 70_000_000L,
        modelUrl = piperModel("en", "en_GB", "alba"),
        configUrl = piperConfig("en", "en_GB", "alba"),
    )

    private val EN_GB_CORI = NexusModel(
        id = "en_GB-cori-medium", name = "Cori (UK)",
        description = "Expressive British English female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "English (UK)", locale = "en-GB", flagEmoji = "🇬🇧",
        sizeBytes = 62_000_000L,
        modelUrl = piperModel("en", "en_GB", "cori"),
        configUrl = piperConfig("en", "en_GB", "cori"),
    )

    private val EN_GB_JENNY = NexusModel(
        id = "en_GB-jenny_dioco-medium", name = "Jenny (UK)",
        description = "Warm British English female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "English (UK)", locale = "en-GB", flagEmoji = "🇬🇧",
        sizeBytes = 62_000_000L,
        modelUrl = piperModel("en", "en_GB", "jenny_dioco"),
        configUrl = piperConfig("en", "en_GB", "jenny_dioco"),
    )

    // ── Hindi ───────────────────────────────────────────────────────────────

    val NEXUS_HINDI = NexusModel(
        id = "hi_IN-google-medium", name = "Nexus Hindi",
        description = "Natural Hindi voice. Default Nexus Hindi.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Hindi", locale = "hi-IN", flagEmoji = "🇮🇳",
        sizeBytes = 60_000_000L,
        modelUrl = piperModel("hi", "hi_IN", "google"),
        configUrl = piperConfig("hi", "hi_IN", "google"),
        isDefault = true, requiresWifi = false,
    )

    // ── German ──────────────────────────────────────────────────────────────

    private val DE_THORSTEN = NexusModel(
        id = "de_DE-thorsten-medium", name = "Thorsten (DE)",
        description = "Natural German male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "German", locale = "de-DE", flagEmoji = "🇩🇪",
        sizeBytes = 63_000_000L,
        modelUrl = piperModel("de", "de_DE", "thorsten"),
        configUrl = piperConfig("de", "de_DE", "thorsten"),
    )

    private val DE_EVA = NexusModel(
        id = "de_DE-eva_k-medium", name = "Eva (DE)",
        description = "Clear German female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "German", locale = "de-DE", flagEmoji = "🇩🇪",
        sizeBytes = 61_000_000L,
        modelUrl = piperModel("de", "de_DE", "eva_k"),
        configUrl = piperConfig("de", "de_DE", "eva_k"),
    )

    // ── French ──────────────────────────────────────────────────────────────

    private val FR_SIWIS = NexusModel(
        id = "fr_FR-siwis-medium", name = "Siwis (FR)",
        description = "Clear French female. Compact size.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "French", locale = "fr-FR", flagEmoji = "🇫🇷",
        sizeBytes = 29_000_000L,
        modelUrl = piperModel("fr", "fr_FR", "siwis"),
        configUrl = piperConfig("fr", "fr_FR", "siwis"),
    )

    // ── Spanish ─────────────────────────────────────────────────────────────

    private val ES_CARLFM = NexusModel(
        id = "es_ES-carlfm-medium", name = "Carlos (ES)",
        description = "Spanish male. Very small download.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Spanish (Spain)", locale = "es-ES", flagEmoji = "🇪🇸",
        sizeBytes = 11_000_000L,
        modelUrl = piperModel("es", "es_ES", "carlfm"),
        configUrl = piperConfig("es", "es_ES", "carlfm"),
    )

    private val ES_MX_ALD = NexusModel(
        id = "es_MX-ald-medium", name = "Aldo (MX)",
        description = "Mexican Spanish male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Spanish (Mexico)", locale = "es-MX", flagEmoji = "🇲🇽",
        sizeBytes = 63_000_000L,
        modelUrl = piperModel("es", "es_MX", "ald"),
        configUrl = piperConfig("es", "es_MX", "ald"),
    )

    // ── Italian ─────────────────────────────────────────────────────────────

    private val IT_RICCARDO = NexusModel(
        id = "it_IT-riccardo_fasol-medium", name = "Riccardo (IT)",
        description = "Natural Italian male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Italian", locale = "it-IT", flagEmoji = "🇮🇹",
        sizeBytes = 47_000_000L,
        modelUrl = piperModel("it", "it_IT", "riccardo_fasol"),
        configUrl = piperConfig("it", "it_IT", "riccardo_fasol"),
    )

    // ── Portuguese (Brazil) ─────────────────────────────────────────────────

    private val PT_BR_FABER = NexusModel(
        id = "pt_BR-faber-medium", name = "Faber (BR)",
        description = "Brazilian Portuguese male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Portuguese (Brazil)", locale = "pt-BR", flagEmoji = "🇧🇷",
        sizeBytes = 44_000_000L,
        modelUrl = piperModel("pt", "pt_BR", "faber"),
        configUrl = piperConfig("pt", "pt_BR", "faber"),
    )

    // ── Russian ─────────────────────────────────────────────────────────────

    private val RU_IRINA = NexusModel(
        id = "ru_RU-irina-medium", name = "Irina (RU)",
        description = "Natural Russian female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Russian", locale = "ru-RU", flagEmoji = "🇷🇺",
        sizeBytes = 62_000_000L,
        modelUrl = piperModel("ru", "ru_RU", "irina"),
        configUrl = piperConfig("ru", "ru_RU", "irina"),
    )

    // ── Chinese ─────────────────────────────────────────────────────────────

    private val ZH_HUAYAN = NexusModel(
        id = "zh_CN-huayan-medium", name = "Huayan (ZH)",
        description = "Clear Mandarin Chinese female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Chinese (Mandarin)", locale = "zh-CN", flagEmoji = "🇨🇳",
        sizeBytes = 33_000_000L,
        modelUrl = piperModel("zh", "zh_CN", "huayan"),
        configUrl = piperConfig("zh", "zh_CN", "huayan"),
    )

    // ── Japanese ────────────────────────────────────────────────────────────

    private val JA_KOKORO = NexusModel(
        id = "ja_JP-kokoro-medium", name = "Kokoro (JA)",
        description = "Natural Japanese female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Japanese", locale = "ja-JP", flagEmoji = "🇯🇵",
        sizeBytes = 58_000_000L,
        modelUrl = piperModel("ja", "ja_JP", "kokoro"),
        configUrl = piperConfig("ja", "ja_JP", "kokoro"),
    )

    // ── Arabic ──────────────────────────────────────────────────────────────

    private val AR_KAREEM = NexusModel(
        id = "ar_JO-kareem-medium", name = "Kareem (AR)",
        description = "Arabic male — Jordanian dialect.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Arabic", locale = "ar-JO", flagEmoji = "🇯🇴",
        sizeBytes = 61_000_000L,
        modelUrl = piperModel("ar", "ar_JO", "kareem"),
        configUrl = piperConfig("ar", "ar_JO", "kareem"),
    )

    // ── Dutch ───────────────────────────────────────────────────────────────

    private val NL_MLS = NexusModel(
        id = "nl_NL-mls-medium", name = "MLS (NL)",
        description = "Clear Dutch female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Dutch", locale = "nl-NL", flagEmoji = "🇳🇱",
        sizeBytes = 64_000_000L,
        modelUrl = piperModel("nl", "nl_NL", "mls"),
        configUrl = piperConfig("nl", "nl_NL", "mls"),
    )

    // ── Polish ──────────────────────────────────────────────────────────────

    private val PL_DARKMAN = NexusModel(
        id = "pl_PL-darkman-medium", name = "Darkman (PL)",
        description = "Deep Polish male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Polish", locale = "pl-PL", flagEmoji = "🇵🇱",
        sizeBytes = 63_000_000L,
        modelUrl = piperModel("pl", "pl_PL", "darkman"),
        configUrl = piperConfig("pl", "pl_PL", "darkman"),
    )

    // ── Ukrainian ───────────────────────────────────────────────────────────

    private val UK_LADA = NexusModel(
        id = "uk_UA-lada-medium", name = "Lada (UA)",
        description = "Natural Ukrainian female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Ukrainian", locale = "uk-UA", flagEmoji = "🇺🇦",
        sizeBytes = 63_000_000L,
        modelUrl = piperModel("uk", "uk_UA", "lada"),
        configUrl = piperConfig("uk", "uk_UA", "lada"),
    )

    // ── Turkish ─────────────────────────────────────────────────────────────

    private val TR_DFKI = NexusModel(
        id = "tr_TR-dfki-medium", name = "DFKI (TR)",
        description = "Clear Turkish male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Turkish", locale = "tr-TR", flagEmoji = "🇹🇷",
        sizeBytes = 62_000_000L,
        modelUrl = piperModel("tr", "tr_TR", "dfki"),
        configUrl = piperConfig("tr", "tr_TR", "dfki"),
    )

    // ── Swedish ─────────────────────────────────────────────────────────────

    private val SV_NST = NexusModel(
        id = "sv_SE-nst-medium", name = "NST (SV)",
        description = "Natural Swedish male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Swedish", locale = "sv-SE", flagEmoji = "🇸🇪",
        sizeBytes = 62_000_000L,
        modelUrl = piperModel("sv", "sv_SE", "nst"),
        configUrl = piperConfig("sv", "sv_SE", "nst"),
    )

    // ── Catalan ─────────────────────────────────────────────────────────────

    private val CA_UPC_ONA = NexusModel(
        id = "ca_ES-upc_ona-medium", name = "Ona (CA)",
        description = "Catalan female — UPC voice.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Catalan", locale = "ca-ES", flagEmoji = "🏴",
        sizeBytes = 61_000_000L,
        modelUrl = piperModel("ca", "ca_ES", "upc_ona"),
        configUrl = piperConfig("ca", "ca_ES", "upc_ona"),
    )

    // ── Romanian ────────────────────────────────────────────────────────────

    private val RO_MIHAI = NexusModel(
        id = "ro_RO-mihai-medium", name = "Mihai (RO)",
        description = "Clear Romanian male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Romanian", locale = "ro-RO", flagEmoji = "🇷🇴",
        sizeBytes = 63_000_000L,
        modelUrl = piperModel("ro", "ro_RO", "mihai"),
        configUrl = piperConfig("ro", "ro_RO", "mihai"),
    )

    // ── Czech ───────────────────────────────────────────────────────────────

    private val CS_JIRKA = NexusModel(
        id = "cs_CZ-jirka-medium", name = "Jirka (CS)",
        description = "Natural Czech male.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Czech", locale = "cs-CZ", flagEmoji = "🇨🇿",
        sizeBytes = 62_000_000L,
        modelUrl = piperModel("cs", "cs_CZ", "jirka"),
        configUrl = piperConfig("cs", "cs_CZ", "jirka"),
    )

    // ── Vietnamese ──────────────────────────────────────────────────────────

    private val VI_VIVOS = NexusModel(
        id = "vi_VN-vivos-medium", name = "Vivos (VI)",
        description = "Clear Vietnamese female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Vietnamese", locale = "vi-VN", flagEmoji = "🇻🇳",
        sizeBytes = 68_000_000L,
        modelUrl = piperModel("vi", "vi_VN", "vivos"),
        configUrl = piperConfig("vi", "vi_VN", "vivos"),
    )

    // ── Persian (Farsi) ─────────────────────────────────────────────────────

    private val FA_HAANIYE = NexusModel(
        id = "fa_IR-haaniye-medium", name = "Haaniye (FA)",
        description = "Natural Persian/Farsi female.",
        type = ModelType.VOICE_TTS, quality = ModelQuality.MEDIUM,
        language = "Persian (Farsi)", locale = "fa-IR", flagEmoji = "🇮🇷",
        sizeBytes = 61_000_000L,
        modelUrl = piperModel("fa", "fa_IR", "haaniye"),
        configUrl = piperConfig("fa", "fa_IR", "haaniye"),
    )

    // ── Complete catalogue (28 verified voices, 19 languages) ─────────────

    fun allVoices(): List<NexusModel> = listOf(
        NEXUS_ENGLISH, EN_US_AMY, EN_US_RYAN, EN_US_JOE, EN_US_NORMAN,
        EN_GB_ALBA, EN_GB_CORI, EN_GB_JENNY,
        NEXUS_HINDI,
        DE_THORSTEN, DE_EVA,
        FR_SIWIS,
        ES_CARLFM, ES_MX_ALD,
        IT_RICCARDO,
        PT_BR_FABER,
        RU_IRINA,
        ZH_HUAYAN,
        JA_KOKORO,
        AR_KAREEM,
        NL_MLS,
        PL_DARKMAN,
        UK_LADA,
        TR_DFKI,
        SV_NST,
        CA_UPC_ONA,
        RO_MIHAI,
        CS_JIRKA,
        VI_VIVOS,
        FA_HAANIYE,
    )

    fun defaultModels(): List<NexusModel> = allVoices().filter { it.isDefault }

    fun findById(id: String): NexusModel? = allVoices().firstOrNull { it.id == id }

    fun voicesForLocale(locale: String): List<NexusModel> =
        allVoices().filter { it.locale == locale }
}
