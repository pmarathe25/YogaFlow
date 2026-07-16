package com.example.game.battle

import android.content.Context
import android.media.SoundPool
import com.example.game.model.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Plays battle sound effects from bundled 44.1kHz WAV assets under assets/sfx/.
 * Missing/corrupt assets fail fast (matching how the other JSON assets are loaded),
 * so users always hear the bundled samples rather than a silent or degraded fallback.
 */
class BattleSoundManager(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .build()

    private var hitSoundId: Int = 0
    private var physicalSoundId: Int = 0
    private var healSoundId: Int = 0
    private var buffSoundId: Int = 0
    private var whooshSoundId: Int = 0
    private var ultimateSoundId: Int = 0
    private val elementSoundIds = mutableMapOf<Element, Int>()

    init {
        hitSoundId = loadAsset(context, "sfx/sfx_hit.wav")
        physicalSoundId = loadAsset(context, "sfx/sfx_physical.wav")
        healSoundId = loadAsset(context, "sfx/sfx_heal.wav")
        buffSoundId = loadAsset(context, "sfx/sfx_buff.wav")
        whooshSoundId = loadAsset(context, "sfx/sfx_whoosh.wav")
        ultimateSoundId = loadAsset(context, "sfx/sfx_ultimate.wav")
        elementSoundIds[Element.FIRE] = loadAsset(context, "sfx/sfx_fire.wav")
        elementSoundIds[Element.WATER] = loadAsset(context, "sfx/sfx_water.wav")
        elementSoundIds[Element.EARTH] = loadAsset(context, "sfx/sfx_earth.wav")
        elementSoundIds[Element.AIR] = loadAsset(context, "sfx/sfx_air.wav")
        elementSoundIds[Element.LIGHT] = loadAsset(context, "sfx/sfx_light.wav")
        elementSoundIds[Element.DARK] = loadAsset(context, "sfx/sfx_dark.wav")
        elementSoundIds[Element.SHADOW] = elementSoundIds[Element.DARK] ?: 0
        elementSoundIds[Element.ELECTRIC] = elementSoundIds[Element.LIGHT] ?: 0
        elementSoundIds[Element.VOID] = elementSoundIds[Element.DARK] ?: 0
    }

    /** Loads a bundled WAV asset, copying it to a cache file SoundPool can open. */
    private fun loadAsset(context: Context, assetPath: String): Int {
        val pcm = readWavPcm(context.assets.open(assetPath).use { it.readBytes() })
        val file = File(context.cacheDir, "sfx_${assetPath.hashCode()}.wav")
        writeWav(file, pcm, 44100)
        return soundPool.load(file.absolutePath, 1)
    }

    fun playHit() { soundPool.play(hitSoundId, 0.7f, 0.7f, 1, 0, 1f) }
    fun playHeal() { soundPool.play(healSoundId, 0.5f, 0.5f, 1, 0, 1f) }
    fun playBuff() { soundPool.play(buffSoundId, 0.55f, 0.55f, 1, 0, 1f) }
    fun playWhoosh() { soundPool.play(whooshSoundId, 0.35f, 0.35f, 1, 0, 1f) }
    fun playUltimate() { soundPool.play(ultimateSoundId, 0.85f, 0.85f, 1, 0, 1f) }

    fun playElementSound(element: Element) {
        val soundId = elementSoundIds[element] ?: whooshSoundId
        soundPool.play(soundId, 0.55f, 0.55f, 1, 0, 1f)
    }

    /** Pick a sound for a skill based on its prevailing damage kind. */
    fun playForSkill(skill: Skill) {
        when {
            skill.healScaling != null -> playHeal()
            skill.shieldScaling != null || skill.buffs.isNotEmpty() -> playBuff()
            skill.damageComponents.isEmpty() -> playWhoosh()
            else -> {
                val element = prevailingElement(skill.damageComponents)
                if (element == null || element == Element.NEUTRAL) playPhysical()
                else playElementSound(element)
            }
        }
    }

    /** Pick a sound for a combo based on its prevailing damage kind. */
    fun playForCombo(combo: ComboSkill) {
        when {
            combo.healScaling != null -> playHeal()
            combo.shieldScaling != null || combo.buffs.isNotEmpty() -> playBuff()
            combo.damageComponents.isEmpty() -> playWhoosh()
            else -> {
                val element = prevailingElement(combo.damageComponents)
                if (element == null || element == Element.NEUTRAL) playPhysical()
                else playElementSound(element)
            }
        }
    }

    private fun playPhysical() {
        soundPool.play(physicalSoundId, 0.7f, 0.7f, 1, 0, 1f)
    }

    private fun prevailingElement(components: List<DamageComponent>): Element? {
        if (components.isEmpty()) return null
        val total = components.sumOf { it.percentage }.coerceAtLeast(1)
        val elemental = components.filter { it.type == DamageType.ELEMENTAL && it.element != null }
        val elementalWeight = elemental.sumOf { it.percentage }
        // Physical if it outweighs elemental, else the dominant elemental element.
        return if (elementalWeight * 2 < total) {
            null
        } else {
            elemental.maxByOrNull { it.percentage }?.element
        }
    }

    fun release() { soundPool.release() }

    private fun readWavPcm(bytes: ByteArray): ShortArray {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buf.int == 0x46464952) { "not RIFF: $buf" }
        buf.int
        require(buf.int == 0x45564157) { "not WAVE" }
        var dataSize = -1
        var pos = 12
        while (pos < bytes.size - 8) {
            val chunkId = buf.int
            val chunkSize = buf.int
            val next = buf.position() + chunkSize
            if (chunkId == 0x20746D66) { // "fmt "
                buf.short // audioFormat
                buf.short // channels
                buf.int // sampleRate
                buf.int // byteRate
                buf.short // blockAlign
                buf.short // bitsPerSample
                if (chunkSize > 16) buf.position(buf.position() + (chunkSize - 16))
            } else if (chunkId == 0x61746164) { // "data"
                dataSize = chunkSize
                break
            }
            buf.position(next)
            pos = next
        }
        require(dataSize >= 0) { "WAV missing data chunk" }
        val shortBuf = ByteBuffer.wrap(bytes, buf.position(), dataSize).order(ByteOrder.LITTLE_ENDIAN)
        val shorts = ShortArray(shortBuf.remaining() / 2)
        shortBuf.asShortBuffer().get(shorts)
        return shorts
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
        FileOutputStream(file).use { fos -> fos.channel.write(buf) }
    }
}
