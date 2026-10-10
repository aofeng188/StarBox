package com.tianaofeng.xingxia.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tianaofeng.xingxia.core.SeededGenerator
import com.tianaofeng.xingxia.data.AppSettings
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

enum class SoundEffect(val variants: Int = 1) {
    TICK(3), TAP(3), POP, WHOOSH, COIN_FLIP, COIN_LAND, DICE_HIT(3), STICK_RATTLE, STICK_DROP,
    CHIME, SUCCESS, FAIL, CARD_FLIP, SCRATCH(3), SPARKLE, DRUM,
}

/**
 * 音效全部用代码合成（与 iOS 版同一套合成器），首次启动时写成 WAV 缓存，再交给 SoundPool 低延迟播放。
 * 使用「游戏/提示音」音频属性：不打断用户正在听的音乐。
 */
object SoundEngine {
    private const val RATE = 44_100
    private var pool: SoundPool? = null
    private val ids = HashMap<SoundEffect, MutableList<Int>>()
    private val executor = Executors.newSingleThreadExecutor()

    fun prepare(context: Context) {
        if (pool != null) return
        val soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        pool = soundPool
        val dir = File(context.cacheDir, "sfx-v1").apply { mkdirs() }
        executor.execute {
            for (effect in SoundEffect.entries) {
                for (variant in 0 until effect.variants) {
                    val file = File(dir, "${effect.name.lowercase()}_$variant.wav")
                    if (!file.exists()) writeWav(file, Synth.render(effect, variant))
                    val id = soundPool.load(file.path, 1)
                    synchronized(ids) { ids.getOrPut(effect) { mutableListOf() }.add(id) }
                }
            }
        }
    }

    fun play(effect: SoundEffect, volume: Float = 1f) {
        if (!AppSettings.soundEnabled) return
        val soundPool = pool ?: return
        val options = synchronized(ids) { ids[effect]?.toList() } ?: return
        if (options.isEmpty()) return
        val v = (volume * AppSettings.soundVolume / 100f).coerceIn(0f, 1f)
        soundPool.play(options.random(), v, v, 1, 0, 1f)
    }

    private fun writeWav(file: File, samples: FloatArray) {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) data.putShort((s.coerceIn(-1f, 1f) * 32767).toInt().toShort())
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + samples.size * 2); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(RATE); putInt(RATE * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(samples.size * 2)
        }
        val tmp = File(file.path + ".tmp")
        FileOutputStream(tmp).use {
            it.write(header.array())
            it.write(data.array())
        }
        tmp.renameTo(file)
    }

    /** 简单的加法/噪声合成器（移植自 iOS 版） */
    private object Synth {
        const val rate = RATE.toDouble()

        class Noise(var state: Int) {
            fun next(): Double {
                state = state * 1_664_525 + 1_013_904_223
                return state.toDouble() / Int.MAX_VALUE
            }
        }

        fun render(effect: SoundEffect, variant: Int): FloatArray {
            val noise = Noise(1234 + variant * 977)
            val v = variant.toDouble()
            return when (effect) {
                SoundEffect.TICK -> tone(0.035) { t ->
                    val f = 2100 + v * 260
                    (0.45 * noise.next() + 0.8 * sin(2 * PI * f * t)) * exp(-t / 0.0045)
                }
                SoundEffect.TAP -> tone(0.07) { t ->
                    val f = 820 - 4000 * t + v * 60
                    sin(2 * PI * f * t) * exp(-t / 0.018) * 0.7
                }
                SoundEffect.POP -> tone(0.09) { t ->
                    val f = 380 + 9000 * t
                    sin(2 * PI * f * t) * sin(PI * min(1.0, t / 0.09)) * 0.6
                }
                SoundEffect.WHOOSH -> {
                    var low = 0.0
                    val duration = 0.42
                    tone(duration) { t ->
                        val p = t / duration
                        val cutoff = 0.02 + 0.28 * sin(PI * p)
                        low += cutoff * (noise.next() - low)
                        low * sin(PI * p).pow(2) * 1.4
                    }
                }
                SoundEffect.COIN_FLIP -> tone(0.18) { t ->
                    (sin(2 * PI * 3_300 * t) * 0.5 + sin(2 * PI * 4_870 * t) * 0.3) * exp(-t / 0.04)
                }
                SoundEffect.COIN_LAND -> {
                    val partials = listOf(Triple(1_780.0, 1.0, 0.38), Triple(2_640.0, 0.6, 0.26), Triple(3_910.0, 0.42, 0.18), Triple(5_380.0, 0.25, 0.12))
                    tone(1.0) { t ->
                        var s = 0.0
                        for ((f, a, d) in partials) {
                            s += a * sin(2 * PI * f * t) * exp(-t / d)
                            if (t > 0.13) s += 0.35 * a * sin(2 * PI * f * (t - 0.13)) * exp(-(t - 0.13) / (d * 0.6))
                        }
                        val click = if (t < 0.004) noise.next() * 0.6 else 0.0
                        s * 0.28 + click
                    }
                }
                SoundEffect.DICE_HIT -> {
                    var low = 0.0
                    tone(0.07) { t ->
                        val n = noise.next()
                        low += 0.25 * (n - low)
                        ((n - low) * 0.8 + 0.25 * sin(2 * PI * (760 + v * 140) * t)) * exp(-t / 0.011)
                    }
                }
                SoundEffect.STICK_RATTLE -> {
                    val out = FloatArray((0.5 * rate).toInt())
                    val gen = SeededGenerator(99uL)
                    for (i in 0 until 9) {
                        val start = ((i * 0.052 + gen.double(0.0, 0.018)) * rate).toInt()
                        val f = gen.double(880.0, 1_450.0)
                        val clack = tone(0.04) { t ->
                            (sin(2 * PI * f * t) * 0.7 + (if (t < 0.003) noise.next() * 0.5 else 0.0)) * exp(-t / 0.012)
                        }
                        mix(clack, out, start, gen.double(0.5, 1.0).toFloat())
                    }
                    out
                }
                SoundEffect.STICK_DROP -> {
                    val out = tone(0.5) { t ->
                        (sin(2 * PI * 210 * t) * exp(-t / 0.07) + 0.5 * sin(2 * PI * 660 * t) * exp(-t / 0.03) +
                            (if (t < 0.004) noise.next() * 0.5 else 0.0)) * 0.8
                    }
                    mix(tone(0.15) { t -> sin(2 * PI * 720 * t) * exp(-t / 0.02) * 0.35 }, out, (0.16 * rate).toInt())
                    out
                }
                SoundEffect.CHIME -> {
                    val out = bell(1_046.5, 1.8, 0.5)
                    mix(bell(1_318.5, 1.4, 0.32), out, (0.11 * rate).toInt())
                    mix(bell(1_568.0, 1.2, 0.24), out, (0.22 * rate).toInt())
                    out
                }
                SoundEffect.SUCCESS -> {
                    val out = FloatArray((0.9 * rate).toInt())
                    listOf(523.25, 659.25, 783.99, 1_046.5).forEachIndexed { i, f ->
                        mix(tone(0.5) { t -> (sin(2 * PI * f * t) + 0.3 * sin(4 * PI * f * t)) * exp(-t / 0.18) * 0.4 }, out, (i * 0.085 * rate).toInt())
                    }
                    out
                }
                SoundEffect.FAIL -> {
                    val out = FloatArray((0.55 * rate).toInt())
                    listOf(392.0, 293.66).forEachIndexed { i, f ->
                        mix(tone(0.3) { t -> asin(sin(2 * PI * f * t)) * 2 / PI * exp(-t / 0.12) * 0.45 }, out, (i * 0.17 * rate).toInt())
                    }
                    out
                }
                SoundEffect.CARD_FLIP -> {
                    var low = 0.0
                    tone(0.14) { t ->
                        val n = noise.next()
                        low += 0.12 * (n - low)
                        (n - low) * min(1.0, t / 0.01) * exp(-t / 0.035) * 0.8
                    }
                }
                SoundEffect.SCRATCH -> {
                    var low = 0.0
                    tone(0.09) { t ->
                        val n = noise.next()
                        low += 0.35 * (n - low)
                        (n - low) * sin(PI * t / 0.09) * (0.25 + 0.05 * v)
                    }
                }
                SoundEffect.SPARKLE -> {
                    val out = FloatArray((0.7 * rate).toInt())
                    val gen = SeededGenerator(7uL)
                    for (i in 0 until 7) {
                        val f = gen.double(2_400.0, 5_200.0)
                        mix(tone(0.25) { t -> sin(2 * PI * f * t) * exp(-t / 0.05) * 0.25 }, out, (i * 0.06 * rate).toInt())
                    }
                    out
                }
                SoundEffect.DRUM -> tone(0.35) { t ->
                    val f = 140 * exp(-t / 0.08) + 50
                    (sin(2 * PI * f * t) * exp(-t / 0.12) + (if (t < 0.005) noise.next() * 0.4 else 0.0)) * 0.9
                }
            }
        }

        inline fun tone(duration: Double, sample: (Double) -> Double): FloatArray {
            val count = (duration * rate).toInt()
            val out = FloatArray(count)
            for (i in 0 until count) {
                val t = i / rate
                // 末尾 5ms 淡出，避免爆音
                val fade = min(1.0, (count - i) / (0.005 * rate))
                out[i] = max(-1.0, min(1.0, sample(t) * fade)).toFloat()
            }
            return out
        }

        fun bell(frequency: Double, duration: Double, gain: Double): FloatArray {
            val ratios = listOf(Triple(1.0, 1.0, 1.0), Triple(2.76, 0.45, 0.45), Triple(5.4, 0.25, 0.22), Triple(8.93, 0.12, 0.1))
            return tone(duration) { t ->
                var s = 0.0
                for ((r, a, d) in ratios) s += a * sin(2 * PI * frequency * r * t) * exp(-t / d)
                s * gain * min(1.0, t / 0.002)
            }
        }

        fun mix(source: FloatArray, target: FloatArray, offset: Int, gain: Float = 1f) {
            for (i in source.indices) {
                val index = offset + i
                if (index >= target.size) break
                target[index] = (target[index] + source[i] * gain).coerceIn(-1f, 1f)
            }
        }
    }
}
