package org.rhythmeta.chunithmd.shared.scanner

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.File
import android.graphics.Bitmap
import android.graphics.Color
import java.io.Closeable
import java.nio.FloatBuffer

/** One session per scan, reused for all six crops. All resources stay on the calling worker. */
class AndroidPaddleRecognizer(snapshot: ScannerModelSnapshot) : Closeable {
    private val vocabulary = PaddleTextRecognition.vocabulary(
        File(snapshot.file("PaddleOCRv6SmallVocab.json")).readText(Charsets.UTF_8))
    private val env = OrtEnvironment.getEnvironment()
    private val session = OrtSession.SessionOptions().use { options ->
        options.setIntraOpNumThreads(2)
        env.createSession(snapshot.file("PaddleOCRv6Small.onnx"), options)
    }

    fun recognize(crop: Bitmap): PaddleTextResult {
        val size = PaddleTextRecognition.size(crop.width, crop.height)
        val h = PaddleTextRecognition.height
        val input = FloatArray(3 * h * size.width)
        val resized = Bitmap.createScaledBitmap(crop, size.resizedWidth, h, true)
        try {
            val pixels = IntArray(size.resizedWidth * h)
            resized.getPixels(pixels, 0, size.resizedWidth, 0, 0, size.resizedWidth, h)
            for (y in 0 until h) for (x in 0 until size.resizedWidth) {
                val p = pixels[y * size.resizedWidth + x]
                val i = y * size.width + x
                input[i] = Color.blue(p) / 127.5f - 1f
                input[h * size.width + i] = Color.green(p) / 127.5f - 1f
                input[2 * h * size.width + i] = Color.red(p) / 127.5f - 1f
            }
        } finally { if (resized !== crop) resized.recycle() }
        val info = session.inputInfo.values.single().info as TensorInfo
        require(info.shape.size == 4 && info.shape[1] == 3L && info.shape[2] == h.toLong()) { "Invalid OCR input" }
        OnnxTensor.createTensor(env, FloatBuffer.wrap(input), longArrayOf(1, 3, h.toLong(), size.width.toLong())).use { tensor ->
            session.run(mapOf(session.inputNames.single() to tensor)).use { output ->
                val result = output[0] as OnnxTensor
                val shape = result.info.shape
                require(shape.size == 3 && shape[0] == 1L && shape[2] == PaddleTextRecognition.classes.toLong()) { "Invalid OCR output" }
                val steps = shape[1].toInt()
                val data = result.floatBuffer
                val ids = IntArray(steps)
                val probabilities = FloatArray(steps)
                for (t in 0 until steps) {
                    var best = Float.NEGATIVE_INFINITY
                    for (c in 0 until PaddleTextRecognition.classes) {
                        val value = data.get()
                        if (value > best) { best = value; ids[t] = c }
                    }
                    probabilities[t] = best
                }
                return PaddleTextRecognition.decode(ids, probabilities, vocabulary)
            }
        }
    }

    override fun close() = session.close()
}
