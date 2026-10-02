package ai.spatialreal.examples.sdkmode

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioClip {
    /**
     * The mono PCM16 samples inside a WAV file in assets/, as the SDK expects them.
     * The clips here are 16 kHz, the rate the session is created with.
     */
    fun pcm16(context: Context, name: String): ByteArray {
        val wav = context.assets.open("$name.wav").use { it.readBytes() }
        val le = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12 // after "RIFF", the file size and "WAVE"
        while (pos + 8 <= wav.size) {
            val chunk = String(wav, pos, 4, Charsets.US_ASCII)
            val size = le.getInt(pos + 4)
            if (chunk == "data") return wav.copyOfRange(pos + 8, minOf(pos + 8 + size, wav.size))
            pos += 8 + size + (size and 1)
        }
        error("$name.wav has no audio data")
    }
}
