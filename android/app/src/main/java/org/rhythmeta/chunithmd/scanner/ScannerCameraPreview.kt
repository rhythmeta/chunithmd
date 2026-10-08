package org.rhythmeta.chunithmd.scanner

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.OrientationEventListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.concurrent.Executors
import java.util.Locale
import org.rhythmeta.chunithmd.shared.localization.tr

internal class ScannerCameraController {
    var imageCapture: ImageCapture? = null

    fun capture(
        context: Context,
        description: String,
        onResult: (Boolean) -> Unit,
    ) {
        val capture = imageCapture ?: run {
            onResult(false)
            return
        }
        val name = "chunithmd_${LegacyFileTimestamp.format(System.currentTimeMillis())}"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.TITLE, name)
            put(MediaStore.Images.Media.ARTIST, "chunithmd")
            put(MediaStore.Images.Media.DESCRIPTION, description)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/chunithmd")
        }
        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onResult(true)
                override fun onError(exception: ImageCaptureException) = onResult(false)
            },
        )
    }

    private companion object {
        @Suppress("DEPRECATION")
        val LegacyFileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT)
    }
}

@Composable
internal fun ScannerCameraPreview(
    enabled: Boolean,
    analyzing: Boolean,
    isProcessingFrame: () -> Boolean,
    controller: ScannerCameraController,
    onFrame: (Bitmap, Int, Boolean) -> Unit,
    onLandscapeChanged: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val currentFrame by rememberUpdatedState(onFrame)
    val currentLandscape by rememberUpdatedState(onLandscapeChanged)
    val currentError by rememberUpdatedState(onError)
    val currentAnalyzing by rememberUpdatedState(analyzing)
    val currentProcessingFrame by rememberUpdatedState(isProcessingFrame)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // TextureView follows the app's horizontal root-tab transition.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
    DisposableEffect(enabled, owner) {
        var disposed = false
        var provider: ProcessCameraProvider? = null
        val executor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val selector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY).build()
        // CameraX otherwise defaults to a 640x480 analysis bound, too small for title/level OCR.
        val analysisSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
            .build()
        val preview = Preview.Builder().setResolutionSelector(selector).build()
        val analysis = ImageAnalysis.Builder().setResolutionSelector(analysisSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
        val capture = ImageCapture.Builder().setResolutionSelector(selector)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
        val landscapeHeld = AtomicBoolean(false)
        val previewSensorRotation = AtomicInteger(0)
        var cameraInfo: CameraInfo? = null
        // Analysis/capture orientation follows how the phone is held, independently of the portrait UI.
        analysis.targetRotation = Surface.ROTATION_0
        capture.targetRotation = Surface.ROTATION_0
        var physicalRotation = Surface.ROTATION_0
        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (disposed) return
                val target = scannerTargetRotation(orientation, physicalRotation)
                if (target == physicalRotation) return
                physicalRotation = target
                val landscape = target == Surface.ROTATION_90 || target == Surface.ROTATION_270
                landscapeHeld.set(landscape)
                currentLandscape(landscape)
                analysis.targetRotation = target
                capture.targetRotation = target
            }
        }
        val displays = context.getSystemService(DisplayManager::class.java)
        fun updateRotation() {
            val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
            preview.targetRotation = rotation
            cameraInfo?.let { previewSensorRotation.set(it.getSensorRotationDegrees(rotation)) }
        }
        val rotationListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) { if (!disposed) updateRotation() }
        }
        if (enabled) {
            currentLandscape(false)
            orientationListener.enable()
            displays.registerDisplayListener(rotationListener, Handler(Looper.getMainLooper()))
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (!disposed) {
                    try {
                        val cameraProvider = future.get()
                        provider = cameraProvider
                        updateRotation()
                        preview.surfaceProvider = previewView.surfaceProvider
                        var lastFrame = 0L
                        analysis.setAnalyzer(executor) { image ->
                            image.use {
                                val now = SystemClock.elapsedRealtime()
                                if (currentAnalyzing && !currentProcessingFrame() && now - lastFrame >= 150) {
                                    lastFrame = now
                                    val frameLandscape = landscapeHeld.get()
                                    val previewRotation = (previewSensorRotation.get() - image.imageInfo.rotationDegrees + 360) % 360
                                    val frame = runCatching { image.toUprightBitmap() }.getOrElse { error ->
                                        main.execute { if (!disposed) currentError(error.localizedMessage ?: tr("识别失败")) }
                                        null
                                    }
                                    if (frame != null) main.execute {
                                        if (disposed || !currentAnalyzing || (frame.width > frame.height) != frameLandscape) frame.recycle() else currentFrame(frame, previewRotation, frameLandscape)
                                    }
                                }
                            }
                        }
                        cameraInfo = cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis, capture).cameraInfo
                        updateRotation()
                        previewView.post { if (!disposed) updateRotation() }
                        controller.imageCapture = capture
                        currentError(null)
                    } catch (error: Exception) { currentError(error.localizedMessage ?: tr("无法启动相机")) }
                }
            }, main)
        }
        onDispose {
            disposed = true
            orientationListener.disable()
            displays.unregisterDisplayListener(rotationListener)
            if (controller.imageCapture === capture) controller.imageCapture = null
            analysis.clearAnalyzer()
            provider?.unbind(preview, analysis, capture)
            executor.shutdown()
        }
    }
}

private fun ImageProxy.toUprightBitmap(): Bitmap {
    val source = toBitmap()
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return source
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return try { Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true) }
    finally { source.recycle() }
}
