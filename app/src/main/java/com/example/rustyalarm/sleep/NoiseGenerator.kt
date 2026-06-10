package com.example.rustyalarm.sleep

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class NoiseColor(val label: String, val emoji: String) {
    WHITE ("화이트 노이즈", "🌫"),
    PINK  ("핑크 노이즈",   "🌸"),
    BROWN ("브라운 노이즈", "🟤"),
    RAIN  ("빗소리",        "🌧"),
    OCEAN ("파도",          "🌊"),
}

/**
 * Generates and streams a chosen [NoiseColor] through an AudioTrack with
 * USAGE_MEDIA so it ducks for calls but does not steal STREAM_ALARM.
 *
 * Implementation notes:
 *  - White noise: uniform random samples
 *  - Pink noise: white passed through a 16-pole Voss-McCartney summing
 *    network (close enough to 1/f for sleep ambience)
 *  - Brown noise: integrated white (1/f^2), DC drift clamped via leak
 *  - Rain: brown noise with periodic short white "drops" layered in
 *  - Ocean: low-passed brown with slow LFO amplitude envelope
 */
class NoiseGenerator {

    private var track: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    fun start(color: NoiseColor) {
        stop()
        val sampleRate = 44_100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(8 * 1024)

        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track?.play()

        job = scope.launch {
            val state = GenState()
            val buf = ShortArray(bufferSize / 2)
            while (isActive) {
                generate(color, buf, state)
                track?.write(buf, 0, buf.size)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        track?.runCatching { stop(); release() }
        track = null
    }

    private data class GenState(
        val pinkRows: FloatArray = FloatArray(16),
        var pinkRunning: Float = 0f,
        var pinkCount: Int = 0,
        var brown: Float = 0f,
        var oceanPhase: Float = 0f,
    )

    private fun generate(color: NoiseColor, buf: ShortArray, s: GenState) {
        when (color) {
            NoiseColor.WHITE -> for (i in buf.indices) {
                buf[i] = (Random.nextFloat() * 2 - 1).toPcm() * 0.55f
            }
            NoiseColor.PINK -> for (i in buf.indices) {
                buf[i] = pink(s).toPcm() * 0.7f
            }
            NoiseColor.BROWN -> for (i in buf.indices) {
                buf[i] = brown(s).toPcm() * 0.9f
            }
            NoiseColor.RAIN -> for (i in buf.indices) {
                val base = brown(s) * 0.85f
                val drop = if (Random.nextFloat() < 0.0025f) (Random.nextFloat() * 0.5f) else 0f
                buf[i] = (base + drop).toPcm() * 0.95f
            }
            NoiseColor.OCEAN -> for (i in buf.indices) {
                s.oceanPhase += 0.00010f
                val env = (0.45f + 0.55f * kotlin.math.sin(s.oceanPhase.toDouble()).toFloat())
                buf[i] = (brown(s) * env).toPcm() * 0.95f
            }
        }
    }

    private fun pink(s: GenState): Float {
        val idx = trailingZeros(++s.pinkCount).coerceAtMost(s.pinkRows.size - 1)
        val v = Random.nextFloat() * 2 - 1
        s.pinkRunning -= s.pinkRows[idx]
        s.pinkRows[idx] = v
        s.pinkRunning += v
        return s.pinkRunning / s.pinkRows.size
    }

    private fun brown(s: GenState): Float {
        val v = Random.nextFloat() * 2 - 1
        s.brown = (s.brown + 0.02f * v).coerceIn(-1f, 1f) * 0.999f
        return s.brown
    }

    private fun trailingZeros(n: Int): Int = if (n == 0) 0 else Integer.numberOfTrailingZeros(n)
    private fun Float.toPcm(): Short = (this.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
    private operator fun Short.times(f: Float): Short =
        ((this.toFloat() * f).coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())).toInt().toShort()

    fun release() {
        stop()
        scope.cancel()
    }
}
