package org.rhythmeta.chunithmd.ui.catalog

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.roundToInt

/** Shared visual treatment for the song detail surface and its jacket. */
internal object SongVisualUtils {
    data class DetailColors(
        val background: Color,
        val surface: Color,
        val selectedSurface: Color,
        val accent: Color,
    )

    fun isDarkTheme(background: Color): Boolean = background.luminance() < 0.5f

    fun detailColors(raw: Color, darkTheme: Boolean): DetailColors {
        val accentHsv = raw.toHsv().apply {
            this[1] = (this[1] * 0.85f).coerceIn(0.25f, 0.75f)
            this[2] = if (darkTheme) 0.82f else 0.62f
        }

        fun surface(selected: Boolean): Color {
            val hsv = raw.toHsv()
            hsv[1] = if (darkTheme) {
                (hsv[1] * if (selected) 0.48f else 0.34f)
                    .coerceIn(0.08f, if (selected) 0.30f else 0.22f)
            } else {
                (hsv[1] * if (selected) 0.28f else 0.16f)
                    .coerceIn(0.03f, if (selected) 0.18f else 0.11f)
            }
            hsv[2] = when {
                darkTheme && selected -> 0.30f
                darkTheme -> 0.20f
                selected -> 0.94f
                else -> 0.98f
            }
            return hsv.toColor()
        }

        return DetailColors(
            background = tonedBackground(raw, darkTheme),
            surface = surface(selected = false),
            selectedSurface = surface(selected = true),
            accent = accentHsv.toColor(),
        )
    }

    /** Matches maimaid: downsample to a small bitmap, then average all RGBA channels. */
    fun averageJacketColor(bitmap: Bitmap): Color? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null

        val scaleFactor = minOf(1f, 96f / max(bitmap.width, bitmap.height).toFloat())
        val sampledWidth = (bitmap.width * scaleFactor).roundToInt().coerceAtLeast(1)
        val sampledHeight = (bitmap.height * scaleFactor).roundToInt().coerceAtLeast(1)
        val sampledBitmap = if (sampledWidth == bitmap.width && sampledHeight == bitmap.height) {
            bitmap
        } else {
            bitmap.scale(sampledWidth, sampledHeight)
        }

        return try {
            val pixels = IntArray(sampledWidth * sampledHeight)
            sampledBitmap.getPixels(pixels, 0, sampledWidth, 0, 0, sampledWidth, sampledHeight)
            var red = 0L
            var green = 0L
            var blue = 0L
            var alpha = 0L
            pixels.forEach { pixel ->
                red += android.graphics.Color.red(pixel)
                green += android.graphics.Color.green(pixel)
                blue += android.graphics.Color.blue(pixel)
                alpha += android.graphics.Color.alpha(pixel)
            }
            val pixelCount = pixels.size.toFloat()
            Color(
                red = red / pixelCount / 255f,
                green = green / pixelCount / 255f,
                blue = blue / pixelCount / 255f,
                alpha = alpha / pixelCount / 255f,
            )
        } finally {
            if (sampledBitmap !== bitmap) sampledBitmap.recycle()
        }
    }

    private fun tonedBackground(raw: Color, darkTheme: Boolean): Color {
        val hsv = raw.toHsv()
        hsv[1] = if (darkTheme) {
            (hsv[1] * 0.75f).coerceIn(0.20f, 0.45f)
        } else {
            (hsv[1] * 0.45f).coerceIn(0.08f, 0.30f)
        }
        hsv[2] = if (darkTheme) {
            (hsv[2] * 0.35f).coerceIn(0.12f, 0.28f)
        } else {
            (0.88f + (hsv[2] - 0.5f) * 0.08f).coerceIn(0.84f, 0.94f)
        }
        return hsv.toColor()
    }

    private fun Color.toHsv(): FloatArray = FloatArray(3).also {
        android.graphics.Color.colorToHSV(toArgb(), it)
    }

    private fun FloatArray.toColor(): Color = Color(android.graphics.Color.HSVToColor(this))
}
