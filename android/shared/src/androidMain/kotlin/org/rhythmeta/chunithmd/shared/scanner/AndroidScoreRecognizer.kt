package org.rhythmeta.chunithmd.shared.scanner

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.graphics.*
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.*

/** One request owns its native resources, so cancellation cannot close an in-flight session. */
class AndroidScoreRecognizer(context: Context, private val models: ScannerModelManager) {
    private val context = context.applicationContext

    suspend fun recognize(uri: Uri): List<ScanObservation> = withContext(Dispatchers.Default) {
        currentCoroutineContext().ensureActive()
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val ratio = min(1.0, 2560.0 / max(info.size.width, info.size.height))
            decoder.setTargetSize(max(1, (info.size.width * ratio).roundToInt()), max(1, (info.size.height * ratio).roundToInt()))
        }
        try { recognize(bitmap) } finally { bitmap.recycle() }
    }

    /** The caller owns camera frames and recycles them after this suspending call finishes. */
    suspend fun recognize(bitmap: Bitmap): List<ScanObservation> = withContext(Dispatchers.Default) {
        currentCoroutineContext().ensureActive()
        val snapshot = checkNotNull(models.snapshot) { "Download scanner models first" }
        val detections = detect(bitmap, snapshot)
        currentCoroutineContext().ensureActive()
        if (detections.isEmpty()) return@withContext emptyList()
        AndroidPaddleRecognizer(snapshot).use { recognizer ->
            detections.map { detection ->
                currentCoroutineContext().ensureActive()
                val crop = cropForOcr(bitmap, ScoreDetection.cropBox(detection))
                try {
                    val result = recognizer.recognize(crop)
                    ScanObservation(detection.field, result.text, detection.confidence, detection.box)
                } finally { if (crop !== bitmap) crop.recycle() }
            }
        }
    }

    private fun detect(source: Bitmap, snapshot: ScannerModelSnapshot): List<ScanDetection> {
        val n = ScoreDetection.inputSize
        val fit = ScoreDetection.letterbox(source.width, source.height)
        val input = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val values = FloatArray(3 * n * n)
        try {
            Canvas(input).apply {
                drawColor(Color.rgb(114, 114, 114))
                drawBitmap(source, null, Rect(fit.left, fit.top, fit.left + fit.width, fit.top + fit.height), Paint(Paint.FILTER_BITMAP_FLAG))
            }
            val pixels = IntArray(n * n); input.getPixels(pixels, 0, n, 0, 0, n, n)
            for (i in pixels.indices) {
                values[i] = Color.red(pixels[i]) / 255f
                values[n * n + i] = Color.green(pixels[i]) / 255f
                values[2 * n * n + i] = Color.blue(pixels[i]) / 255f
            }
        } finally { input.recycle() }
        val env = OrtEnvironment.getEnvironment()
        OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            val model = snapshot.file("ScoreDetector.onnx")
            env.createSession(model, options).use { session ->
                val inputInfo = session.inputInfo.values.single().info as TensorInfo
                require(inputInfo.shape.contentEquals(longArrayOf(1, 3, n.toLong(), n.toLong()))) { "Unsupported detector input" }
                OnnxTensor.createTensor(env, FloatBuffer.wrap(values), inputInfo.shape).use { tensor ->
                    session.run(mapOf(session.inputNames.single() to tensor)).use { result ->
                        val output = result[0] as OnnxTensor
                        val shape = output.info.shape
                        require(shape.size == 3 && shape[0] == 1L && shape[1] == 10L) { "Unsupported detector output" }
                        val raw = FloatArray(shape[1].toInt() * shape[2].toInt()); output.floatBuffer.get(raw)
                        return ScoreDetection.decode(raw, shape[1].toInt(), shape[2].toInt(), source.width, source.height)
                    }
                }
            }
        }
    }

    private fun cropForOcr(source: Bitmap, box: ScanBox): Bitmap {
        val left = floor(box.x * source.width).toInt().coerceIn(0, source.width - 1)
        val top = floor(box.y * source.height).toInt().coerceIn(0, source.height - 1)
        val right = ceil((box.x + box.width) * source.width).toInt().coerceIn(left + 1, source.width)
        val bottom = ceil((box.y + box.height) * source.height).toInt().coerceIn(top + 1, source.height)
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }
}
