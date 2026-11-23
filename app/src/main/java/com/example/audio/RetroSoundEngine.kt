package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SfxType {
    FIST,
    PISTOL,
    SHOTGUN,
    CHAINGUN,
    PLASMA,
    EXPLOSION,
    PICKUP,
    KEYCARD,
    HURT,
    ENEMY_HIT,
    ENEMY_DEATH,
    DOOR,
    SWITCH
}

/**
 * Generates crisp retro 90s PCM WAV sound effects in cache and plays them with low-latency SoundPool.
 */
class RetroSoundEngine(context: Context) {
    private val sampleRate = 22050
    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<SfxType, Int>()
    private var volume: Float = 0.85f
    private var released = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()

        val cacheDir = File(context.cacheDir, "doom93_sfx").apply { mkdirs() }
        SfxType.entries.forEach { type ->
            val samples = synthesize(type)
            val wavFile = File(cacheDir, "${type.name.lowercase()}.wav")
            writeWav(wavFile, samples)
            soundIds[type] = soundPool.load(wavFile.absolutePath, 1)
        }
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
    }

    fun play(type: SfxType, rate: Float = 1.0f) {
        if (released || volume <= 0.01f) return
        val id = soundIds[type] ?: return
        soundPool.play(id, volume, volume, 1, 0, rate)
    }

    fun release() {
        if (!released) {
            released = true
            soundPool.release()
        }
    }

    private fun synthesize(type: SfxType): ShortArray {
        val rng = Random(type.ordinal * 7919 + 42)
        return when (type) {
            SfxType.FIST -> buildSamples(0.14f) { t, _ ->
                val env = exp(-t * 24f)
                val body = sin(2.0 * PI * (120.0 - t * 380.0) * t).toFloat()
                val noise = (rng.nextFloat() * 2f - 1f) * 0.45f
                (body * 0.7f + noise) * env
            }
            SfxType.PISTOL -> buildSamples(0.18f) { t, _ ->
                val snap = exp(-t * 38f) * (rng.nextFloat() * 2f - 1f)
                val punch = exp(-t * 22f) * sin(2.0 * PI * (190.0 - t * 600.0) * t).toFloat()
                (snap * 0.75f + punch * 0.55f)
            }
            SfxType.SHOTGUN -> buildSamples(0.42f) { t, _ ->
                if (t < 0.25f) {
                    val boomEnv = exp(-t * 13f)
                    val noise = (rng.nextFloat() * 2f - 1f)
                    val sub = sin(2.0 * PI * (110.0 - t * 220.0) * t).toFloat()
                    (noise * 0.8f + sub * 0.6f) * boomEnv
                } else {
                    // Pump clack-clack
                    val pt = t - 0.25f
                    val clack = if (pt < 0.04f || (pt in 0.08f..0.12f)) 1f else 0f
                    clack * (rng.nextFloat() * 2f - 1f) * 0.35f
                }
            }
            SfxType.CHAINGUN -> buildSamples(0.11f) { t, _ ->
                val env = exp(-t * 42f)
                val bark = (rng.nextFloat() * 2f - 1f) * 0.75f +
                    sin(2.0 * PI * 240.0 * t).toFloat() * 0.4f
                bark * env
            }
            SfxType.PLASMA -> buildSamples(0.20f) { t, _ ->
                val env = exp(-t * 16f)
                val freq = 980.0 - t * 3200.0
                val sq = if (sin(2.0 * PI * freq * t) >= 0) 0.6f else -0.6f
                val mod = sin(2.0 * PI * 65.0 * t).toFloat() * 0.35f + 0.65f
                sq * mod * env
            }
            SfxType.EXPLOSION -> buildSamples(0.65f) { t, _ ->
                val env = exp(-t * 5.2f)
                val low = sin(2.0 * PI * (72.0 - t * 40.0) * t).toFloat()
                val grit = (rng.nextFloat() * 2f - 1f)
                (grit * 0.75f + low * 0.65f) * env
            }
            SfxType.PICKUP -> buildSamples(0.18f) { t, _ ->
                val freq = if (t < 0.08f) 660.0 else 990.0
                val env = exp(-(if (t < 0.08f) t else t - 0.08f) * 20f)
                val wave = if (sin(2.0 * PI * freq * t) >= 0) 0.45f else -0.45f
                wave * env
            }
            SfxType.KEYCARD -> buildSamples(0.28f) { t, _ ->
                val freq = when {
                    t < 0.09f -> 740.0
                    t < 0.18f -> 987.0
                    else -> 1318.0
                }
                val env = exp(-(t % 0.09f) * 18f)
                sin(2.0 * PI * freq * t).toFloat() * 0.55f * env
            }
            SfxType.HURT -> buildSamples(0.24f) { t, _ ->
                val env = exp(-t * 11f)
                val saw = ((t * 125f) % 1f) * 2f - 1f
                val grit = (rng.nextFloat() * 2f - 1f) * 0.25f
                (saw * 0.6f + grit) * env
            }
            SfxType.ENEMY_HIT -> buildSamples(0.14f) { t, _ ->
                val env = exp(-t * 22f)
                val freq = 280.0 - t * 900.0
                (sin(2.0 * PI * freq * t).toFloat() * 0.5f + (rng.nextFloat() * 2f - 1f) * 0.4f) * env
            }
            SfxType.ENEMY_DEATH -> buildSamples(0.38f) { t, _ ->
                val env = exp(-t * 6.5f)
                val freq = 220.0 - t * 420.0
                val growl = ((t * freq.toFloat().coerceAtLeast(50f)) % 1f) * 2f - 1f
                (growl * 0.65f + (rng.nextFloat() * 2f - 1f) * 0.35f) * env
            }
            SfxType.DOOR -> buildSamples(0.35f) { t, _ ->
                val env = sin(PI * (t / 0.35f)).toFloat()
                val hum = sin(2.0 * PI * 95.0 * t).toFloat() * 0.45f
                val hiss = (rng.nextFloat() * 2f - 1f) * 0.35f
                (hum + hiss) * env * 0.7f
            }
            SfxType.SWITCH -> buildSamples(0.16f) { t, _ ->
                val env = exp(-t * 25f)
                val click = if (sin(2.0 * PI * 420.0 * t) >= 0) 0.5f else -0.5f
                val thud = sin(2.0 * PI * 110.0 * t).toFloat() * 0.5f
                (click + thud) * env
            }
        }
    }

    private fun buildSamples(durationSec: Float, generator: (t: Float, idx: Int) -> Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val sample = generator(t, i).coerceIn(-1f, 1f)
            out[i] = (sample * 28000f).toInt().toShort()
        }
        return out
    }

    private fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // PCM chunk size
        buffer.putShort(1)  // PCM format
        buffer.putShort(1)  // Mono
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2)  // Block align
        buffer.putShort(16) // Bits per sample
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
        for (s in samples) {
            buffer.putShort(s)
        }
        FileOutputStream(file).use { it.write(buffer.array()) }
    }
}
