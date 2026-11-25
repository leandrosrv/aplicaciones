package com.example.gamification.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

object GamificationSoundPreferences {
    private const val PREFS_NAME = "gamification_ui_prefs"
    private const val KEY_LEVEL_UP_SOUND = "level_up_sound_enabled"

    fun isLevelUpSoundEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LEVEL_UP_SOUND, true)
    }

    fun setLevelUpSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LEVEL_UP_SOUND, enabled)
            .apply()
    }
}

object LevelUpSoundPlayer {
    private val playMutex = Mutex()
    private const val SAMPLE_RATE = 22_050

    /**
     * Synthesizes a short, uplifting ascending arpeggio fanfare (C5 - E5 - G5 - C6)
     * using AudioTrack with zero external audio assets or extra permissions,
     * falling back to ToneGenerator if AudioTrack fails on a device.
     */
    suspend fun playLevelUpFanfare(context: Context) {
        if (!GamificationSoundPreferences.isLevelUpSoundEnabled(context)) return

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT) return

        playMutex.withLock {
            withContext(Dispatchers.Default) {
                val playedPcm = runCatching {
                    playSynthesizedArpeggio()
                }.isSuccess
                if (!playedPcm) {
                    runCatching {
                        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                        try {
                            tone.startTone(ToneGenerator.TONE_PROP_ACK, 350)
                            Thread.sleep(380L)
                        } finally {
                            runCatching { tone.release() }
                        }
                    }
                }
            }
        }
    }

    private fun playSynthesizedArpeggio() {
        val notes = listOf(
            523.25 to 95,   // C5
            659.25 to 95,   // E5
            783.99 to 105,  // G5
            1046.50 to 230, // C6
        )

        val totalSamples = notes.sumOf { (it.second * SAMPLE_RATE) / 1000 }
        val pcmBuffer = ShortArray(totalSamples)
        var offset = 0

        for ((freqHz, durationMs) in notes) {
            val count = (durationMs * SAMPLE_RATE) / 1000
            val attackSamples = (SAMPLE_RATE * 0.012f).toInt().coerceAtLeast(1)
            val releaseSamples = (SAMPLE_RATE * 0.035f).toInt().coerceAtLeast(1)

            for (i in 0 until count) {
                val attackEnv = min(1f, i.toFloat() / attackSamples.toFloat())
                val releaseEnv = min(1f, (count - 1 - i).toFloat() / releaseSamples.toFloat())
                val envelope = attackEnv * releaseEnv
                val t = i.toDouble() / SAMPLE_RATE.toDouble()
                // Warm harmonic mix (fundamental + soft octave overtone)
                val sample = 0.78 * sin(2.0 * PI * freqHz * t) + 0.22 * sin(2.0 * PI * (freqHz * 2.0) * t)
                val amplitude = (sample * envelope * 0.42 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcmBuffer[offset + i] = amplitude.toShort()
            }
            offset += count
        }

        val byteCount = pcmBuffer.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBuf, byteCount)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bufferSize)
            .build()

        try {
            check(track.state == AudioTrack.STATE_INITIALIZED) { "AudioTrack not initialized" }
            track.write(pcmBuffer, 0, pcmBuffer.size)
            track.play()
            val totalDurationMs = notes.sumOf { it.second }.toLong() + 80L
            Thread.sleep(totalDurationMs)
        } catch (e: CancellationException) {
            throw e
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }
}
