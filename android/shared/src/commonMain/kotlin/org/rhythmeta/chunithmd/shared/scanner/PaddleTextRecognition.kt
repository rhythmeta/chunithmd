package org.rhythmeta.chunithmd.shared.scanner

import org.rhythmeta.chunithmd.shared.CatalogJson
import kotlin.math.ceil

data class PaddleTextSize(val width: Int, val resizedWidth: Int)
data class PaddleTextResult(val text: String, val confidence: Float)

/** PP-OCRv6 small: BGR NCHW, height 48, pixels / 127.5 - 1, zero right padding. */
object PaddleTextRecognition {
    const val height = 48
    const val classes = 18710
    private val widths = listOf(320, 640, 1280, 2560)

    fun size(width: Int, height: Int): PaddleTextSize {
        require(width > 0 && height > 0)
        val resized = ceil(this.height.toDouble() * width / height).toInt().coerceIn(1, widths.last())
        return PaddleTextSize(widths.first { it >= resized }, resized)
    }

    fun vocabulary(json: String): List<String> = CatalogJson.codec.decodeFromString<List<String>>(json).also {
        require(it.size == classes && it.first() == "blank" && it.last() == " ") { "Invalid PP-OCRv6 small vocabulary" }
    }

    /** CTC collapses adjacent equal IDs, then removes blank 0. Tokens are Unicode strings, not bytes. */
    fun decode(ids: IntArray, probabilities: FloatArray, vocabulary: List<String>): PaddleTextResult {
        require(ids.size == probabilities.size && vocabulary.size == classes)
        val text = StringBuilder()
        var previous = -1
        var sum = 0f
        var count = 0
        for (i in ids.indices) {
            val id = ids[i]
            require(id in vocabulary.indices && probabilities[i].isFinite())
            if (id != 0 && id != previous) {
                text.append(vocabulary[id])
                sum += probabilities[i]
                count++
            }
            previous = id
        }
        return PaddleTextResult(text.toString(), if (count == 0) 0f else sum / count)
    }
}
