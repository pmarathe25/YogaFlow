package com.example.audio

import android.content.Context
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ZenSoundSynthesizer(context: Context) {
    private val soundPool: SoundPool
    private var woodTapSoundId = 0
    private var isLoaded = false

    init {
        soundPool = SoundPool.Builder().setMaxStreams(2).build()
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) isLoaded = true
        }
        woodTapSoundId = loadSound(context, generateWoodTapPcm(), "woodtap")
    }

    private fun loadSound(context: Context, pcmData: ShortArray, name: String): Int {
        val sampleRate = 22050
        val file = File(context.cacheDir, "${name}_${System.nanoTime()}.wav")
        file.deleteOnExit()
        writeWav(file, pcmData, sampleRate)
        return soundPool.load(file.absolutePath, 1)
    }

    fun playWoodTap() {
        if (isLoaded) soundPool.play(woodTapSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }

    companion object {
        fun generateWoodTapPcm(): ShortArray {
            val sampleRate = 22050
            val durationSec = 0.2
            val totalSamples = (sampleRate * durationSec).toInt()
            val buffer = ShortArray(totalSamples)
            val f0 = 350.0
            val harmonics = doubleArrayOf(1.0, 2.7, 5.4)
            val amplitudes = doubleArrayOf(0.8, 0.3, 0.1)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val decay = kotlin.math.exp(-15.0 * t)
                var sampleVal = 0.0
                for (h in harmonics.indices) {
                    val freq = f0 * harmonics[h]
                    sampleVal += amplitudes[h] * kotlin.math.sin(2.0 * Math.PI * freq * t)
                }
                val finalVal = (sampleVal * decay * 14000.0).coerceIn(-32768.0, 32767.0)
                buffer[i] = finalVal.toInt().toShort()
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

            FileOutputStream(file).use { fos ->
                fos.channel.write(buf.array())
            }
        }
    }
}
