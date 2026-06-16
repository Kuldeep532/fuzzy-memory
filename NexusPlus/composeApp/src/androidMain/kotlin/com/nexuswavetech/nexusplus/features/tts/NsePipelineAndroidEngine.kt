package com.nexuswavetech.nexusplus.features.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * NSE 3.0 — Pipeline Android TTS Engine.
 *
 * Significantly faster perceived TTS than [NseAndroidEngine] through:
 *
 *  1. **Sentence splitting** — [NseSentenceSplitter] breaks text into chunks.
 *  2. **Pipeline pre-synthesis** — a dedicated synthesis coroutine pre-synthesizes
 *     sentence N+1 (via [TextToSpeech.synthesizeToFile] → WAV → PCM) while a
 *     playback coroutine is still playing sentence N. Uses a [Channel] with
 *     capacity 1 to keep exactly one sentence pre-buffered without concurrent
 *     synthesis (which would corrupt TTS engine state).
 *  3. **AudioTrack playback** — plays PCM bytes via [AudioTrack] in STATIC mode,
 *     skipping MediaPlayer overhead used by [TextToSpeech.speak].
 *  4. **LRU PCM cache** — [NsePcmCache] holds synthesized audio; repeated phrases
 *     skip synthesis entirely.
 *
 * Result: zero sentence gaps, faster first-word start for every sentence
 * after the first, and instant playback for cached phrases.
 */
class NsePipelineAndroidEngine(
    private val context: Context,
    private val audioFocus: NseAudioFocusManager,
    val pcmCache: NsePcmCache,
) : NseEngine {

    override var utteranceResultListener: ((NseUtteranceResult) -> Unit)? = null

    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false

    /** utteranceId → (deferred result, wav temp file) for in-flight synthesizeToFile calls */
    private val pendingFileMap = ConcurrentHashMap<String, Pair<CompletableDeferred<Boolean>, File>>()

    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var speakJob: Job? = null

    // ── Initialisation ───────────────────────────────────────────────────────

    override suspend fun initialise(): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val engine = TextToSpeech(context) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    registerListener()
                    cont.resume(Result.success(Unit))
                } else {
                    cont.resume(
                        Result.failure(
                            IllegalStateException("NSE 3.0 pipeline init failed: status=$status")
                        )
                    )
                }
            }
            tts = engine
            cont.invokeOnCancellation { engine.shutdown() }
        }

    // ── Synthesis ────────────────────────────────────────────────────────────

    override suspend fun speak(request: NseSpeechRequest): Result<Unit> {
        val engine = tts ?: return Result.failure(IllegalStateException("NSE 3.0 not initialised"))
        if (!ttsReady) return Result.failure(IllegalStateException("NSE 3.0 not ready"))

        // Cancel any in-flight speech
        speakJob?.cancel()
        engine.stop()

        if (!audioFocus.requestFocus()) {
            return Result.failure(IllegalStateException("Could not acquire audio focus"))
        }
        audioFocus.onFocusLost = {
            engine.stop()
            speakJob?.cancel()
            audioFocus.abandonFocus()
        }

        speakJob = engineScope.launch {
            try {
                utteranceResultListener?.invoke(NseUtteranceResult.Started(request.utteranceId))

                // Build sentence-level task list
                data class Task(val text: String, val locale: Locale)
                val tasks: List<Task> = when (request.mode) {
                    is NseSpeechMode.Auto ->
                        NseSentenceSplitter.split(request.text)
                            .map { Task(it, NseLanguageDetector.detect(it)) }
                    is NseSpeechMode.SingleVoice ->
                        NseSentenceSplitter.split(request.text)
                            .map { Task(it, request.mode.locale) }
                    is NseSpeechMode.DualVoice ->
                        NseSentenceSplitter.split(request.text).flatMap { s ->
                            NseLanguageDetector.segmentByScript(s)
                                .map { (seg, segLocale) ->
                                    val assigned = if (segLocale == request.mode.primaryLocale || segLocale == request.mode.secondaryLocale) segLocale
                                    else if (NseLanguageDetector.detect(seg) == request.mode.primaryLocale) request.mode.primaryLocale
                                    else request.mode.secondaryLocale
                                    Task(seg, assigned)
                                }
                        }
                    is NseSpeechMode.Mix ->
                        NseSentenceSplitter.split(request.text).flatMap { s ->
                            NseLanguageDetector.segmentByScript(s)
                                .map { (seg, loc) -> Task(seg, loc) }
                        }
                }

                if (tasks.isEmpty()) {
                    utteranceResultListener?.invoke(NseUtteranceResult.Completed(request.utteranceId))
                    audioFocus.abandonFocus()
                    return@launch
                }

                // ── Channel-based pipeline ──────────────────────────────────────
                // Capacity 1: synthesis runs exactly 1 sentence ahead of playback.
                // Only one synthesis call runs at a time → no TTS engine state races.
                val channel = Channel<NsePcmCache.Entry?>(capacity = 1)

                // Synthesis coroutine: runs sequentially, pre-buffers 1 sentence
                val synthJob = launch {
                    for (task in tasks) {
                        if (!isActive) { channel.close(); return@launch }
                        val pcm = synthesizeToPcm(task.text, task.locale, request)
                        channel.send(pcm)
                    }
                    channel.close()
                }

                // Playback loop (current coroutine): consumes from channel
                for (pcm in channel) {
                    if (!isActive) break
                    if (pcm != null) {
                        playPcm(pcm)
                    }
                    // null entry: synthesizeToFile failed for this sentence; skip silently
                }

                synthJob.cancelAndJoin()
                utteranceResultListener?.invoke(NseUtteranceResult.Completed(request.utteranceId))
                audioFocus.abandonFocus()

            } catch (_: CancellationException) {
                utteranceResultListener?.invoke(NseUtteranceResult.Completed(request.utteranceId))
                audioFocus.abandonFocus()
            } catch (e: Exception) {
                utteranceResultListener?.invoke(NseUtteranceResult.Failed(request.utteranceId, -1))
                audioFocus.abandonFocus()
            }
        }

        return Result.success(Unit)
    }

    // ── Pre-synthesis (WAV → PCM) ─────────────────────────────────────────────

    private suspend fun synthesizeToPcm(
        text: String,
        locale: Locale,
        req: NseSpeechRequest,
    ): NsePcmCache.Entry? = withContext(Dispatchers.IO) {
        val engine = tts ?: return@withContext null

        // Cache hit — skip synthesis
        val cacheKey = pcmCache.key(text, locale.toLanguageTag(), req.speechRate, req.pitch)
        pcmCache.get(cacheKey)?.let { return@withContext it }

        // Configure voice (safe: called from synthesis coroutine only, sequential)
        val langStatus   = engine.isLanguageAvailable(locale)
        engine.language  = if (langStatus >= TextToSpeech.LANG_AVAILABLE) locale else Locale.ENGLISH
        engine.setPitch(req.pitch)
        engine.setSpeechRate(req.speechRate)

        // Synthesize to WAV
        val uid      = "nse3f_${System.nanoTime()}"
        val tempFile = File(context.cacheDir, "nse3_$uid.wav")
        val deferred = CompletableDeferred<Boolean>()
        pendingFileMap[uid] = Pair(deferred, tempFile)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, uid)
        }
        val apiResult = engine.synthesizeToFile(text, params, tempFile, uid)
        if (apiResult != TextToSpeech.SUCCESS) {
            pendingFileMap.remove(uid)
            tempFile.delete()
            return@withContext null
        }

        // Wait for synthesis to complete
        val ok = try {
            withTimeout(15_000L) { deferred.await() }
        } catch (_: TimeoutCancellationException) {
            false
        } finally {
            pendingFileMap.remove(uid)
        }

        if (!ok || !tempFile.exists() || tempFile.length() < 44L) {
            tempFile.delete()
            return@withContext null
        }

        // Parse WAV and cache
        val entry = readWavFile(tempFile)
        tempFile.delete()
        if (entry != null) pcmCache.put(cacheKey, entry)
        entry
    }

    private fun readWavFile(file: File): NsePcmCache.Entry? = try {
        RandomAccessFile(file, "r").use { raf ->
            val hdr = ByteArray(44)
            raf.readFully(hdr)
            val bb = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)

            if (String(hdr, 0, 4) != "RIFF" || String(hdr, 8, 4) != "WAVE") return null

            val audioFmt    = bb.getShort(20).toInt() and 0xFFFF
            val channels    = bb.getShort(22).toInt() and 0xFFFF
            val sampleRate  = bb.getInt(24)
            val bitsPerSamp = bb.getShort(34).toInt() and 0xFFFF
            if (audioFmt != 1) return null

            // Find "data" chunk (handle metadata chunks in WAV)
            var dataOffset = 44L
            if (String(hdr, 36, 4) != "data") {
                raf.seek(12L)
                var found = false
                while (raf.filePointer < raf.length() - 8) {
                    val id = ByteArray(4).also { raf.readFully(it) }
                    val sz = ByteBuffer.wrap(ByteArray(4).also { raf.readFully(it) })
                        .order(ByteOrder.LITTLE_ENDIAN).int
                    if (String(id) == "data") { dataOffset = raf.filePointer; found = true; break }
                    raf.seek(raf.filePointer + sz.coerceAtLeast(0))
                }
                if (!found) return null
            }

            val dataSize = (raf.length() - dataOffset).toInt().coerceAtLeast(0)
            if (dataSize == 0) return null

            raf.seek(dataOffset)
            val pcm = ByteArray(dataSize).also { raf.readFully(it) }

            NsePcmCache.Entry(
                pcm          = pcm,
                sampleRate   = sampleRate,
                channelConfig = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                encoding     = if (bitsPerSamp == 16) AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT,
                byteCount    = dataSize,
            )
        }
    } catch (_: Exception) { null }

    // ── AudioTrack playback ───────────────────────────────────────────────────

    private suspend fun playPcm(audio: NsePcmCache.Entry) = withContext(Dispatchers.IO) {
        val minBuf = AudioTrack.getMinBufferSize(audio.sampleRate, audio.channelConfig, audio.encoding)
            .coerceAtLeast(audio.byteCount)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(audio.sampleRate)
                    .setEncoding(audio.encoding)
                    .setChannelMask(audio.channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        val bytesPerSample = if (audio.encoding == AudioFormat.ENCODING_PCM_16BIT) 2 else 1
        val numChannels    = if (audio.channelConfig == AudioFormat.CHANNEL_OUT_MONO) 1 else 2
        val frameCount     = audio.byteCount / (bytesPerSample * numChannels)
        val durationMs     = (audio.byteCount.toLong() * 1000L) /
            (audio.sampleRate.toLong() * bytesPerSample * numChannels)

        val done = CompletableDeferred<Unit>()
        track.setNotificationMarkerPosition(frameCount)
        track.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) { done.complete(Unit) }
                override fun onPeriodicNotification(t: AudioTrack) {}
            }
        )

        try {
            track.write(audio.pcm, 0, audio.byteCount)
            track.play()
            withTimeout(durationMs + 3_000L) { done.await() }
        } finally {
            track.stop()
            track.release()
        }
    }

    // ── Control ──────────────────────────────────────────────────────────────

    override fun stop() {
        speakJob?.cancel()
        tts?.stop()
        audioFocus.abandonFocus()
    }

    override fun availableVoices(locale: Locale?): List<NseVoiceProfile> {
        val engine = tts ?: return emptyList()
        return try {
            engine.voices
                ?.filter { locale == null || it.locale == locale }
                ?.map { v ->
                    NseVoiceProfile(
                        name              = v.name,
                        locale            = v.locale,
                        isNetworkRequired = v.isNetworkConnectionRequired,
                        quality           = v.quality,
                        latency           = v.latency,
                    )
                } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    override fun shutdown() {
        speakJob?.cancel()
        engineScope.cancel()
        audioFocus.abandonFocus()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }

    /** Phrases currently held in the PCM cache. */
    fun cachedPhraseCount(): Int = pcmCache.entryCount()

    // ── Utterance progress listener ──────────────────────────────────────────

    private fun registerListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                // Pipeline file-synthesis calls are internal; don't forward them
                if (!utteranceId.startsWith("nse3f_")) {
                    utteranceResultListener?.invoke(NseUtteranceResult.Started(utteranceId))
                }
            }
            override fun onDone(utteranceId: String) {
                if (utteranceId.startsWith("nse3f_")) {
                    pendingFileMap[utteranceId]?.first?.complete(true)
                } else {
                    audioFocus.abandonFocus()
                    utteranceResultListener?.invoke(NseUtteranceResult.Completed(utteranceId))
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                pendingFileMap[utteranceId]?.first?.complete(false)
                if (!utteranceId.startsWith("nse3f_")) {
                    audioFocus.abandonFocus()
                    utteranceResultListener?.invoke(NseUtteranceResult.Failed(utteranceId, -1))
                }
            }
            override fun onError(utteranceId: String, errorCode: Int) {
                pendingFileMap[utteranceId]?.first?.complete(false)
                if (!utteranceId.startsWith("nse3f_")) {
                    audioFocus.abandonFocus()
                    utteranceResultListener?.invoke(NseUtteranceResult.Failed(utteranceId, errorCode))
                }
            }
        })
    }
}
