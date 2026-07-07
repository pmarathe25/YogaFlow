package com.example.game.battle

import android.content.Context
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class BattleSoundManager(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    private var hitSoundId: Int = 0
    private var healSoundId: Int = 0
    private var whooshSoundId: Int = 0
    private var ultimateSoundId: Int = 0
    private var clickSoundId: Int = 0

    init {
        hitSoundId = loadSound(context, generateHitPcm(), "hit")
        healSoundId = loadSound(context, generateHealPcm(), "heal")
        whooshSoundId = loadSound(context, generateWhooshPcm(), "whoosh")
        ultimateSoundId = loadSound(context, generateUltimatePcm(), "ultimate")
        clickSoundId = loadSound(context, generateClickPcm(), "click")
    }

    fun playHit() { soundPool.play(hitSoundId, 0.7f, 0.7f, 1, 0, 1f) }
    fun playHeal() { soundPool.play(healSoundId, 0.5f, 0.5f, 1, 0, 1f) }
    fun playWhoosh() { soundPool.play(whooshSoundId, 0.3f, 0.3f, 1, 0, 1f) }
    fun playUltimate() { soundPool.play(ultimateSoundId, 0.8f, 0.8f, 1, 0, 1f) }
    fun playClick() { soundPool.play(clickSoundId, 0.4f, 0.4f, 1, 0, 1f) }

    fun release() { soundPool.release() }

    private fun loadSound(context: Context, pcmData: ShortArray, name: String): Int {
        val sampleRate = 22050
        val file = File(context.cacheDir, "${name}_${System.nanoTime()}.wav")
        file.deleteOnExit()
        writeWav(file, pcmData, sampleRate)
        return soundPool.load(file.absolutePath, 1)
    }

    companion object {
        private const val SAMPLE_RATE = 22050

        fun generateHitPcm(): ShortArray {
            val durationSec = 0.1
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val noise = (Math.random() * 2.0 - 1.0)
                val envelope = exp(-25.0 * t)
                val sampleVal = (noise * envelope * 14000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = sampleVal.toInt().toShort()
            }
            return buffer
        }

        fun generateHealPcm(): ShortArray {
            val durationSec = 0.2
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            val fStart = 400.0
            val fEnd = 900.0
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val freq = fStart + (fEnd - fStart) * (t / durationSec)
                val envelope = 1.0 - exp(-10.0 * t)
                val sampleVal = (sin(2.0 * PI * freq * t) * envelope * 12000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = sampleVal.toInt().toShort()
            }
            return buffer
        }

        fun generateWhooshPcm(): ShortArray {
            val durationSec = 0.15
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val noise = (Math.random() * 2.0 - 1.0)
                val sweep = 0.3 + 0.7 * (t / durationSec)
                val envelope = sin(PI * t / durationSec)
                val sampleVal = (noise * sweep * envelope * 10000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = sampleVal.toInt().toShort()
            }
            return buffer
        }

        fun generateUltimatePcm(): ShortArray {
            val durationSec = 0.5
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            val harmonics = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
            val amplitudes = doubleArrayOf(0.6, 0.4, 0.25, 0.15, 0.1)
            val f0 = 220.0
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = 1.0 - exp(-20.0 * t)
                val decay = exp(-3.0 * t)
                var sampleVal = 0.0
                for (h in harmonics.indices) {
                    val freq = f0 * harmonics[h]
                    sampleVal += amplitudes[h] * sin(2.0 * PI * freq * t)
                }
                val finalVal = (sampleVal * attack * decay * 12000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = finalVal.toInt().toShort()
            }
            return buffer
        }

        fun generateClickPcm(): ShortArray {
            val durationSec = 0.03
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            val freq = 8000.0
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = exp(-100.0 * t)
                val sampleVal = (sin(2.0 * PI * freq * t) * envelope * 12000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = sampleVal.toInt().toShort()
            }
            return buffer
        }

        private fun writeWav(file: File, pcmData: ShortArray, sampleRate: Int) {
            val channelCount = 1
            val bitsPerSample = 16
            val byteRate = sampleRate * channelCount * bitsPerSample / 8
            val dataSize = pcmData.size * bitsPerSample / 8
            val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

            buf.put("RIFF".toByteArray())
            buf.putInt(36 + dataSize)
            buf.put("WAVE".toByteArray())
            buf.put("fmt ".toByteArray())
            buf.putInt(16)
            buf.putShort(1)
            buf.putShort(channelCount.toShort())
            buf.putInt(sampleRate)
            buf.putInt(byteRate)
            buf.putShort((channelCount * bitsPerSample / 8).toShort())
            buf.putShort(bitsPerSample.toShort())
            buf.put("data".toByteArray())
            buf.putInt(dataSize)

            val byteBuf = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            byteBuf.asShortBuffer().put(pcmData)
            buf.put(byteBuf.array())

            buf.flip()
            FileOutputStream(file).use { fos ->
                fos.channel.write(buf)
            }
        }
    }
}
