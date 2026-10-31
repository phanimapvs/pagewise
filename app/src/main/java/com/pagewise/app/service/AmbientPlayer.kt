package com.pagewise.app.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSound(val label: String, val emoji: String) {
    RAIN("Soft rain", "🌧"),
    OCEAN("Ocean waves", "🌊"),
    FIREPLACE("Fireplace", "🔥"),
    FOREST("Forest breeze", "🌲"),
    STREAM("Brook", "💧"),
    CAFE("Busy café", "☕"),
    THUNDER("Distant thunder", "⛈"),
    TRAIN("Train ride", "🚆"),
    BROWN("Deep focus", "🎧"),
}

/**
 * Synthesizes ambient soundscapes in real time from filtered noise,
 * so the app ships no audio files.
 */
class AmbientPlayer {
    private val sampleRate = 22050
    @Volatile private var thread: Thread? = null
    @Volatile private var running = false
    @Volatile var volume = 0.6f

    var current: AmbientSound? = null
        private set

    fun play(sound: AmbientSound) {
        stop()
        current = sound
        running = true
        thread = Thread({ loop(sound) }, "ambient").apply { start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
        current = null
    }

    private fun loop(sound: AmbientSound) {
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(minBuf * 4)
            .build()
        val gen = Generator(sound, sampleRate)
        val buf = ShortArray(1024)
        track.play()
        var fadeIn = 0f
        try {
            while (running) {
                for (i in buf.indices) {
                    fadeIn = (fadeIn + 1f / sampleRate).coerceAtMost(1f)
                    val v = (gen.next() * volume * fadeIn).coerceIn(-1f, 1f)
                    buf[i] = (v * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
            }
        } finally {
            track.stop()
            track.release()
        }
    }

    private class Generator(private val sound: AmbientSound, private val sr: Int) {
        private val rnd = Random(System.nanoTime())
        private var t = 0L
        private var brown = 0f
        private var lp1 = 0f
        private var lp2 = 0f
        // pink noise state (Paul Kellet's economy filter)
        private var p0 = 0f; private var p1 = 0f; private var p2 = 0f
        private var crackle = 0f
        private var drop = 0f
        private var dropFreq = 0f
        private var chirpT = -1f
        private var chirpBase = 0f
        private var lp3 = 0f
        // café: a few overlapping "voices" whose loudness drifts, plus occasional cup clinks
        private val voiceLevel = FloatArray(4)
        private val voiceTarget = FloatArray(4)
        private var clinkT = -1f
        private var clinkFreq = 0f
        // thunder: one rumble at a time with its own length and strength
        private var rumbleT = -1f
        private var rumbleLen = 0f
        private var rumbleGain = 0f

        private fun white() = rnd.nextFloat() * 2f - 1f

        private fun pink(): Float {
            val w = white()
            p0 = 0.99765f * p0 + w * 0.0990460f
            p1 = 0.96300f * p1 + w * 0.2965164f
            p2 = 0.57000f * p2 + w * 1.0526913f
            return (p0 + p1 + p2 + w * 0.1848f) * 0.2f
        }

        private fun brownNoise(): Float {
            brown = (brown + white() * 0.02f) / 1.02f
            return brown * 3.5f
        }

        fun next(): Float {
            t++
            val sec = t.toFloat() / sr
            return when (sound) {
                AmbientSound.RAIN -> {
                    lp1 += 0.35f * (white() - lp1)
                    if (rnd.nextFloat() < 0.0009f) { drop = 0.5f + rnd.nextFloat() * 0.4f; dropFreq = 1500f + rnd.nextFloat() * 2500f }
                    drop *= 0.996f
                    lp1 * 0.35f + drop * sin(2 * PI * dropFreq * sec).toFloat() * 0.15f
                }
                AmbientSound.OCEAN -> {
                    val swell = 0.5f + 0.5f * sin(2 * PI * sec / 8.0).toFloat()
                    val surge = swell * swell * swell
                    lp1 += (0.02f + 0.25f * surge) * (white() - lp1)
                    lp1 * (0.25f + 0.9f * surge) + brownNoise() * 0.25f
                }
                AmbientSound.FIREPLACE -> {
                    if (rnd.nextFloat() < 0.0015f) crackle = 0.4f + rnd.nextFloat() * 0.6f
                    crackle *= 0.93f
                    brownNoise() * 0.55f + white() * crackle * 0.6f
                }
                AmbientSound.FOREST -> {
                    val gust = 0.6f + 0.4f * sin(2 * PI * sec / 11.0).toFloat()
                    lp1 += 0.05f * (pink() - lp1)
                    if (chirpT < 0 && rnd.nextFloat() < 0.00004f) { chirpT = 0f; chirpBase = 2600f + rnd.nextFloat() * 1800f }
                    var bird = 0f
                    if (chirpT >= 0) {
                        chirpT += 1f / sr
                        val env = exp(-chirpT * 18f) * sin(PI * (chirpT * 6 % 1)).toFloat()
                        bird = sin(2 * PI * (chirpBase + 900f * sin(2 * PI * chirpT * 9)) * chirpT).toFloat() * env * 0.12f
                        if (chirpT > 0.6f) chirpT = -1f
                    }
                    lp1 * gust * 1.6f + bird
                }
                AmbientSound.STREAM -> {
                    lp1 += 0.5f * (white() - lp1)
                    lp2 += 0.08f * (lp1 - lp2)
                    val bubble = 0.7f + 0.3f * sin(2 * PI * sec * (3 + 2 * sin(2 * PI * sec * 0.3))).toFloat()
                    (lp1 - lp2) * 0.55f * bubble
                }
                AmbientSound.CAFE -> cafe()
                AmbientSound.THUNDER -> thunder(sec)
                AmbientSound.TRAIN -> train(sec)
                AmbientSound.BROWN -> brownNoise() * 0.8f
            }
        }

        private fun cafe(): Float {
            // Speech-band murmur: pink noise between ~150 Hz and ~1.5 kHz.
            val pn = pink()
            lp1 += 0.35f * (pn - lp1)
            lp2 += 0.04f * (lp1 - lp2)
            val band = lp1 - lp2
            var level = 0f
            for (i in voiceLevel.indices) {
                if (rnd.nextFloat() < 0.00012f) voiceTarget[i] = if (rnd.nextFloat() < 0.35f) 0f else 0.3f + rnd.nextFloat() * 0.7f
                voiceLevel[i] += 0.0004f * (voiceTarget[i] - voiceLevel[i])
                level += voiceLevel[i]
            }
            if (clinkT < 0 && rnd.nextFloat() < 0.00005f) { clinkT = 0f; clinkFreq = 2400f + rnd.nextFloat() * 1800f }
            var clink = 0f
            if (clinkT >= 0) {
                clinkT += 1f / sr
                val env = exp(-clinkT * 30f)
                clink = (sin(2 * PI * clinkFreq * clinkT).toFloat() + 0.5f * sin(2 * PI * clinkFreq * 2.76 * clinkT).toFloat()) * env * 0.07f
                if (clinkT > 0.4f) clinkT = -1f
            }
            return band * (0.5f + level * 0.45f) + brownNoise() * 0.12f + clink
        }

        private fun thunder(sec: Float): Float {
            // Soft rain bed.
            lp1 += 0.3f * (white() - lp1)
            val rain = lp1 * 0.2f
            // Occasional rolling rumble: slow swell, long uneven decay, deep low-pass.
            if (rumbleT < 0 && rnd.nextFloat() < 0.000005f) {
                rumbleT = 0f
                rumbleLen = 4f + rnd.nextFloat() * 5f
                rumbleGain = 0.6f + rnd.nextFloat() * 0.6f
            }
            brown = (brown + white() * 0.02f) / 1.02f
            lp3 += 0.006f * (brown * 9f - lp3)
            var rumble = 0f
            if (rumbleT >= 0) {
                rumbleT += 1f / sr
                val attack = (rumbleT / 0.6f).coerceAtMost(1f)
                val decay = exp(-rumbleT * 4f / rumbleLen)
                val roll = 0.65f + 0.35f * sin(2 * PI * rumbleT * (1.3 + 0.6 * sin(2 * PI * sec * 0.07))).toFloat()
                rumble = lp3 * attack * decay * roll * rumbleGain
                if (rumbleT > rumbleLen) rumbleT = -1f
            }
            return rain + rumble
        }

        private val clackOffsets = floatArrayOf(0f, 0.11f, 0.58f, 0.69f)

        private fun train(sec: Float): Float {
            // Low cabin rumble that sways slowly.
            val sway = 0.8f + 0.2f * sin(2 * PI * sec / 6.0).toFloat()
            val rumble = brownNoise() * 0.45f * sway
            lp1 += 0.08f * (white() - lp1)
            // Wheels crossing rail joints: two pairs of clacks per cycle.
            val phase = sec % 1.15f
            var clack = 0f
            for (o in clackOffsets) {
                val dt = phase - o
                if (dt in 0f..0.12f) {
                    val env = exp(-dt * 45f)
                    clack += (sin(2 * PI * 140 * dt).toFloat() * 0.6f + lp1 * 1.5f) * env * 0.35f
                }
            }
            return rumble + clack + lp1 * 0.08f
        }
    }
}
