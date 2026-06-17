package com.nexuswavetech.nexusplus.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

import com.nexuswavetech.nexusplus.core.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * NexusSoundManager — lightweight Android sound feedback system.
 *
 * Plays short UI sound effects for navigation, dialogs, selections, toggles,
 * success, errors, button presses, feature launches, and page transitions.
 *
 * All playback is gated by the Sound Effects setting in [SettingsRepository].
 * Uses [SoundPool] for low-latency non-blocking playback.
 *
 * Usage:
 *   NexusSoundManager.init(context, settings)
 *   NexusSoundManager.play(SoundEvent.NAVIGATE)
 */
object NexusSoundManager {

    enum class SoundEvent {
        NAVIGATE,
        BACK,
        SELECT,
        TOGGLE_ON,
        TOGGLE_OFF,
        SUCCESS,
        ERROR,
        BUTTON_TAP,
        DIALOG_OPEN,
        DIALOG_CLOSE,
        FEATURE_LAUNCH,
        PAGE_TURN_FORWARD,
        PAGE_TURN_BACKWARD,
    }

    private var soundPool: SoundPool? = null
    private var settings: SettingsRepository? = null
    private val soundMap = mutableMapOf<SoundEvent, Int>()
    private var isReady = false

    fun init(context: Context, settingsRepository: SettingsRepository) {
        settings = settingsRepository

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, _ -> isReady = true }

        loadGeneratedSounds(context)
    }

    private fun loadGeneratedSounds(context: Context) {
        soundPool?.let { pool ->
            soundMap[SoundEvent.NAVIGATE]           = generateAndLoad(pool, context, 880f, 80L, 0.4f)
            soundMap[SoundEvent.BACK]               = generateAndLoad(pool, context, 660f, 60L, 0.3f)
            soundMap[SoundEvent.SELECT]             = generateAndLoad(pool, context, 1000f, 50L, 0.5f)
            soundMap[SoundEvent.TOGGLE_ON]          = generateAndLoad(pool, context, 1200f, 70L, 0.45f)
            soundMap[SoundEvent.TOGGLE_OFF]         = generateAndLoad(pool, context, 800f,  70L, 0.35f)
            soundMap[SoundEvent.SUCCESS]            = generateAndLoad(pool, context, 1320f, 120L, 0.6f)
            soundMap[SoundEvent.ERROR]              = generateAndLoad(pool, context, 330f,  150L, 0.5f)
            soundMap[SoundEvent.BUTTON_TAP]         = generateAndLoad(pool, context, 950f,  40L, 0.3f)
            soundMap[SoundEvent.DIALOG_OPEN]        = generateAndLoad(pool, context, 1100f, 100L, 0.4f)
            soundMap[SoundEvent.DIALOG_CLOSE]       = generateAndLoad(pool, context, 900f,  80L, 0.35f)
            soundMap[SoundEvent.FEATURE_LAUNCH]     = generateAndLoad(pool, context, 1400f, 130L, 0.55f)
            soundMap[SoundEvent.PAGE_TURN_FORWARD]  = generateAndLoad(pool, context, 1050f, 60L, 0.35f)
            soundMap[SoundEvent.PAGE_TURN_BACKWARD] = generateAndLoad(pool, context, 850f,  60L, 0.35f)
        }
        isReady = true
    }

    fun play(event: SoundEvent) {
        if (!isReady) return
        val enabled = runCatching {
            runBlocking { settings?.soundEffectsEnabled?.first() ?: true }
        }.getOrDefault(true)
        if (!enabled) return

        soundMap[event]?.let { soundId ->
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        isReady = false
    }

    private fun generateAndLoad(
        pool: SoundPool,
        context: Context,
        frequency: Float,
        durationMs: Long,
        amplitude: Float,
        tag: String = frequency.toInt().toString(),
    ): Int {
        val sampleRate = 44100
        val numSamples = ((durationMs / 1000.0) * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val fadeOutStart = (numSamples * 0.7).toInt()

        for (i in 0 until numSamples) {
            val sample = (amplitude * Short.MAX_VALUE *
                Math.sin(2.0 * Math.PI * frequency * i / sampleRate)).toInt()
            val fade = if (i >= fadeOutStart) {
                1.0 - (i - fadeOutStart).toDouble() / (numSamples - fadeOutStart)
            } else 1.0
            buffer[i] = (sample * fade).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val pcmSize = numSamples * 2 + 44
        val wav = ByteArray(pcmSize)
        writeWavHeader(wav, numSamples * 2, sampleRate)
        for (i in 0 until numSamples) {
            val idx = 44 + i * 2
            wav[idx]     = (buffer[i].toInt() and 0xFF).toByte()
            wav[idx + 1] = (buffer[i].toInt() shr 8 and 0xFF).toByte()
        }

        val tmpFile = java.io.File(context.cacheDir, "nxs_${tag}_${frequency.toInt()}.wav")
        tmpFile.writeBytes(wav)
        return pool.load(tmpFile.absolutePath, 1)
    }

    private fun writeWavHeader(wav: ByteArray, dataSize: Int, sampleRate: Int) {
        val totalSize = dataSize + 36
        wav[0] = 'R'.code.toByte(); wav[1] = 'I'.code.toByte()
        wav[2] = 'F'.code.toByte(); wav[3] = 'F'.code.toByte()
        writeInt(wav, 4, totalSize)
        wav[8] = 'W'.code.toByte(); wav[9] = 'A'.code.toByte()
        wav[10] = 'V'.code.toByte(); wav[11] = 'E'.code.toByte()
        wav[12] = 'f'.code.toByte(); wav[13] = 'm'.code.toByte()
        wav[14] = 't'.code.toByte(); wav[15] = ' '.code.toByte()
        writeInt(wav, 16, 16)
        writeShort(wav, 20, 1)
        writeShort(wav, 22, 1)
        writeInt(wav, 24, sampleRate)
        writeInt(wav, 28, sampleRate * 2)
        writeShort(wav, 32, 2)
        writeShort(wav, 34, 16)
        wav[36] = 'd'.code.toByte(); wav[37] = 'a'.code.toByte()
        wav[38] = 't'.code.toByte(); wav[39] = 'a'.code.toByte()
        writeInt(wav, 40, dataSize)
    }

    private fun writeInt(buf: ByteArray, offset: Int, value: Int) {
        buf[offset]     = (value and 0xFF).toByte()
        buf[offset + 1] = (value shr 8  and 0xFF).toByte()
        buf[offset + 2] = (value shr 16 and 0xFF).toByte()
        buf[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    private fun writeShort(buf: ByteArray, offset: Int, value: Int) {
        buf[offset]     = (value and 0xFF).toByte()
        buf[offset + 1] = (value shr 8  and 0xFF).toByte()
    }

}
