package org.rhythmeta.chunithmd.shared.scanner

import kotlinx.serialization.Serializable
import kotlin.math.*

@Serializable
data class ScanBox(val x: Float, val y: Float, val width: Float, val height: Float)

@Serializable
data class ScanDetection(val field: String, val confidence: Float, val box: ScanBox)

/** Coordinates are normalized against the upright source image, with a top-left origin. */
data class ScanLetterbox(val width: Int, val height: Int, val left: Int, val top: Int)

object ScoreDetection {
    const val inputSize = 1024
    val fields = listOf("title", "difficulty", "level", "score", "clear", "combo")

    fun letterbox(width: Int, height: Int): ScanLetterbox {
        require(width > 0 && height > 0)
        val ratio = inputSize.toDouble() / max(width, height)
        val w = (width * ratio).roundToInt().coerceAtLeast(1)
        val h = (height * ratio).roundToInt().coerceAtLeast(1)
        return ScanLetterbox(w, h, (inputSize - w) / 2, (inputSize - h) / 2)
    }

    /** Pinned export contract: [1, 10, N], pixel xywh followed by six class probabilities.
     * No objectness column and no embedded NMS. Reject incompatible replacement models. */
    fun decode(values: FloatArray, channels: Int, count: Int, sourceWidth: Int, sourceHeight: Int): List<ScanDetection> {
        return decodeFields(values, channels, count, sourceWidth, sourceHeight, fields, 0.25f)
    }

    fun decodeTitle(values: FloatArray, channels: Int, count: Int, sourceWidth: Int, sourceHeight: Int): List<ScanDetection> =
        decodeFields(values, channels, count, sourceWidth, sourceHeight, listOf("title"), 0.35f)

    private fun decodeFields(values: FloatArray, channels: Int, count: Int, sourceWidth: Int, sourceHeight: Int,
        fields: List<String>, threshold: Float): List<ScanDetection> {
        require(channels == fields.size + 4 && count > 0 && values.size == channels * count) { "Unsupported detector output" }
        val fit = letterbox(sourceWidth, sourceHeight)
        val candidates = mutableListOf<ScanDetection>()
        for (i in 0 until count) {
            var cls = 0
            for (c in 1 until fields.size) if (values[(c + 4) * count + i] > values[(cls + 4) * count + i]) cls = c
            val confidence = values[(cls + 4) * count + i]
            if (!confidence.isFinite() || confidence !in threshold..1f) continue
            val cx = values[i]; val cy = values[count + i]
            val w = values[2 * count + i]; val h = values[3 * count + i]
            if (!listOf(cx, cy, w, h).all(Float::isFinite) || w <= 0 || h <= 0) continue
            val x1 = ((cx - w / 2 - fit.left) / fit.width).coerceIn(0f, 1f)
            val y1 = ((cy - h / 2 - fit.top) / fit.height).coerceIn(0f, 1f)
            val x2 = ((cx + w / 2 - fit.left) / fit.width).coerceIn(0f, 1f)
            val y2 = ((cy + h / 2 - fit.top) / fit.height).coerceIn(0f, 1f)
            if (x2 > x1 && y2 > y1) candidates += ScanDetection(fields[cls], confidence, ScanBox(x1, y1, x2 - x1, y2 - y1))
        }
        val kept = mutableListOf<ScanDetection>()
        for (candidate in candidates.sortedByDescending { it.confidence }.take(600)) {
            if (kept.none { it.field == candidate.field && iou(it.box, candidate.box) > 0.45f }) kept += candidate
        }
        // A single photographed result has one primary region per field.
        return kept.distinctBy { it.field }.sortedBy { fields.indexOf(it.field) }
    }

    /** Small OCR margin; level deliberately gets no extra top padding (WE stars). */
    fun cropBox(detection: ScanDetection): ScanBox {
        val b = detection.box
        val px = b.height * 0.10f
        val py = if (detection.field == "level") 0f else b.height * 0.10f
        val x = max(0f, b.x - px); val y = max(0f, b.y - py)
        return ScanBox(x, y, min(1f, b.x + b.width + px) - x, min(1f, b.y + b.height + py) - y)
    }

    private fun iou(a: ScanBox, b: ScanBox): Float {
        val intersection = max(0f, min(a.x + a.width, b.x + b.width) - max(a.x, b.x)) *
            max(0f, min(a.y + a.height, b.y + b.height) - max(a.y, b.y))
        return intersection / (a.width * a.height + b.width * b.height - intersection).coerceAtLeast(1e-9f)
    }
}
