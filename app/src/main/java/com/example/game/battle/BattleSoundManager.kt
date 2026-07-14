package com.example.game.battle

import android.content.Context
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import com.example.game.model.Element

class BattleSoundManager(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    private var hitSoundId: Int = 0
    private var healSoundId: Int = 0
    private var whooshSoundId: Int = 0
    private var ultimateSoundId: Int = 0
    private var fireSoundId: Int = 0
    private var waterSoundId: Int = 0
    private var earthSoundId: Int = 0
    private var airSoundId: Int = 0
    private var lightSoundId: Int = 0
    private var darkSoundId: Int = 0

    init {
        hitSoundId = loadSound(context, generateHitPcm(), "hit")
        healSoundId = loadSound(context, generateHealPcm(), "heal")
        whooshSoundId = loadSound(context, generateWhooshPcm(), "whoosh")
        ultimateSoundId = loadSound(context, generateUltimatePcm(), "ultimate")
        fireSoundId = loadSound(context, generateFirePcm(), "fire")
        waterSoundId = loadSound(context, generateWaterPcm(), "water")
        earthSoundId = loadSound(context, generateEarthPcm(), "earth")
        airSoundId = loadSound(context, generateAirPcm(), "air")
        lightSoundId = loadSound(context, generateLightPcm(), "light")
        darkSoundId = loadSound(context, generateDarkPcm(), "dark")
    }

    fun playHit() { soundPool.play(hitSoundId, 0.7f, 0.7f, 1, 0, 1f) }
    fun playHeal() { soundPool.play(healSoundId, 0.5f, 0.5f, 1, 0, 1f) }
    fun playWhoosh() { soundPool.play(whooshSoundId, 0.3f, 0.3f, 1, 0, 1f) }
    fun playUltimate() { soundPool.play(ultimateSoundId, 0.8f, 0.8f, 1, 0, 1f) }

    fun playElementSound(element: Element) {
        val soundId = when (element) {
            Element.FIRE -> fireSoundId
            Element.WATER -> waterSoundId
            Element.EARTH -> earthSoundId
            Element.AIR -> airSoundId
            Element.LIGHT -> lightSoundId
            Element.DARK -> darkSoundId
            Element.SHADOW -> darkSoundId
            Element.ELECTRIC -> lightSoundId
            Element.VOID -> darkSoundId
            else -> whooshSoundId
        }
        soundPool.play(soundId, 0.5f, 0.5f, 1, 0, 1f)
    }

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

        fun generateFirePcm(): ShortArray {
            val durationSec = 0.3
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val rumble = sin(2.0 * PI * 150.0 * t)
                val noise = Math.random() * 2.0 - 1.0
                val am = 0.5 + 0.5 * sin(2.0 * PI * 30.0 * t)
                val envelope = exp(-8.0 * t)
                val sampleVal = ((rumble * 0.4 + noise * 0.6) * am * envelope * 14000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = sampleVal.toInt().toShort()
            }
            return buffer
        }

        fun generateWaterPcm(): ShortArray {
            val durationSec = 0.3
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val phase = 2.0 * PI * (500.0 * t - 700.0 * durationSec / PI * (cos(PI * t / durationSec) - 1.0))
                val main = sin(phase)
                val echoDelay = 0.05
                val echoT = t - echoDelay
                var echo = 0.0
                if (echoT > 0) {
                    val echoPhase = 2.0 * PI * (500.0 * echoT - 700.0 * durationSec / PI * (cos(PI * echoT / durationSec) - 1.0))
                    echo = sin(echoPhase) * 0.3 * exp(-5.0 * echoT)
                }
                val envelope = exp(-5.0 * t)
                val sampleVal = ((main + echo) * envelope * 12000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = sampleVal.toInt().toShort()
            }
            return buffer
        }

        fun generateEarthPcm(): ShortArray {
            val durationSec = 0.4
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = 1.0 - exp(-15.0 * t)
                val decay = exp(-5.0 * t)
                val main = sin(2.0 * PI * 80.0 * t)
                val h2 = sin(2.0 * PI * 160.0 * t) * 0.5
                val h3 = sin(2.0 * PI * 240.0 * t) * 0.25
                val sampleVal = ((main + h2 + h3) * attack * decay * 14000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = sampleVal.toInt().toShort()
            }
            return buffer
        }

        fun generateAirPcm(): ShortArray {
            val durationSec = 0.25
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val noise = Math.random() * 2.0 - 1.0
                val sweep = 0.3 + 0.7 * sin(PI * t / durationSec)
                val envelope = sin(PI * t / durationSec)
                val sampleVal = (noise * sweep * envelope * 10000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = sampleVal.toInt().toShort()
            }
            return buffer
        }

        fun generateLightPcm(): ShortArray {
            val durationSec = 0.3
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            val f0 = 440.0
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = 1.0 - exp(-30.0 * t)
                val decay = exp(-8.0 * t)
                val tremolo = 0.7 + 0.3 * sin(2.0 * PI * 12.0 * t)
                val h1 = sin(2.0 * PI * f0 * t) * 0.4
                val h2 = sin(2.0 * PI * f0 * 2.0 * t) * 0.8
                val h3 = sin(2.0 * PI * f0 * 3.0 * t) * 0.6
                val h4 = sin(2.0 * PI * f0 * 4.0 * t) * 0.3
                val sampleVal = ((h1 + h2 + h3 + h4) * attack * decay * tremolo * 12000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = sampleVal.toInt().toShort()
            }
            return buffer
        }

        fun generateDarkPcm(): ShortArray {
            val durationSec = 0.5
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = 1.0 - exp(-8.0 * t)
                val decay = exp(-3.0 * t)
                val main = sin(2.0 * PI * 60.0 * t)
                val distorted = (main * 1.5).coerceIn(-1.0, 1.0)
                val sub = sin(2.0 * PI * 30.0 * t) * 0.3
                val sampleVal = ((distorted + sub) * attack * decay * 14000.0).coerceIn(-32768.0, 32767.0)
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
