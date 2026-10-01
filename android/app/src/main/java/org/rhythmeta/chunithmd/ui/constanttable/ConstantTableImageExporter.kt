package org.rhythmeta.chunithmd.ui.constanttable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.shared.ConstantTableSection
import org.rhythmeta.chunithmd.ui.catalog.ULTIMA_STRIPE_STOPS

internal object ConstantTableImageExporter {
    suspend fun renderToCache(
        context: Context,
        baseLevelLabel: String,
        sections: List<ConstantTableSection>,
        includeScores: Boolean,
        localJacketPath: (String) -> String?,
        darkTheme: Boolean,
    ): File? = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = Renderer(sections, baseLevelLabel, includeScores, localJacketPath, darkTheme).render()
            withContext(Dispatchers.IO) {
                File(context.cacheDir, "constant-table").apply { mkdirs() }
                    .resolve("constant-table-${baseLevelLabel.replace('~', '-')}.png")
                    .also { file ->
                        file.outputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
                        bitmap.recycle()
                    }
            }
        }.getOrNull()
    }

    private class Renderer(
        private val sections: List<ConstantTableSection>,
        private val baseLevelLabel: String,
        private val includeScores: Boolean,
        private val localJacketPath: (String) -> String?,
        darkTheme: Boolean,
    ) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        private val background = if (darkTheme) Color.rgb(18, 19, 24) else Color.rgb(250, 247, 252)
        private val foreground = if (darkTheme) Color.WHITE else Color.rgb(35, 31, 39)
        private val secondary = if (darkTheme) Color.rgb(190, 188, 198) else Color.rgb(100, 94, 105)
        private val spacing = 10
        private val columns = 11
        private val width = 1440
        private val horizontalPadding = 36
        private val jacketSize = (width - horizontalPadding * 2 - spacing * (columns - 1)) / columns
        private val sectionHeader = 72
        private val ultimaColors = ULTIMA_STRIPE_STOPS.map { it.second.toArgb() }.toIntArray()
        private val ultimaPositions = ULTIMA_STRIPE_STOPS.map { it.first }.toFloatArray()

        fun render(): Bitmap {
            val rows = sections.sumOf { (it.entries.size + columns - 1) / columns }
            val height = 150 + sections.size * sectionHeader + rows * (jacketSize + spacing) + sections.size * 28 + 72
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            canvas.drawColor(background)
            drawText(canvas, "定数表  $baseLevelLabel", horizontalPadding.toFloat(), 62f, 42f, foreground, true)
            drawText(canvas, "${sections.size} 个定数 · ${sections.sumOf { it.entries.size }} 张谱面", horizontalPadding.toFloat(), 96f, 18f, secondary)
            var top = 130
            sections.forEachIndexed { index, section ->
                drawSection(canvas, section, index, top)
                top += sectionHeader + ((section.entries.size + columns - 1) / columns) * (jacketSize + spacing) + 28
            }
            drawText(canvas, "chunithmd", horizontalPadding.toFloat(), height - 24f, 15f, secondary, true)
            return bitmap
        }

        private fun drawSection(canvas: Canvas, section: ConstantTableSection, index: Int, top: Int) {
            drawText(canvas, section.constantLabel, horizontalPadding.toFloat(), top + 42f, 30f, levelColor(section.constantLabel, index), true)
            setText(30f, levelColor(section.constantLabel, index), true)
            val constantWidth = paint.measureText(section.constantLabel)
            drawText(canvas, "${section.entries.size} 张谱面", horizontalPadding + constantWidth + 12f, top + 42f, 16f, secondary)
            val gridTop = top + sectionHeader
            section.entries.forEachIndexed { entryIndex, entry ->
                val left = horizontalPadding + (entryIndex % columns) * (jacketSize + spacing)
                val cellTop = gridTop + (entryIndex / columns) * (jacketSize + spacing)
                drawJacket(canvas, entry.imageName, left, cellTop)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = difficultyColor(entry.difficulty)
                paint.shader = if (entry.difficulty.equals("ultima", ignoreCase = true)) {
                    LinearGradient(
                        left.toFloat(), cellTop + 64f,
                        left + 64f, cellTop.toFloat(),
                        ultimaColors, ultimaPositions, Shader.TileMode.REPEAT,
                    )
                } else {
                    null
                }
                canvas.drawRoundRect(RectF(left.toFloat(), cellTop.toFloat(), (left + jacketSize).toFloat(), (cellTop + jacketSize).toFloat()), 10f, 10f, paint)
                paint.shader = null
                paint.style = Paint.Style.FILL
                if (includeScores) drawBadges(canvas, entry.rank, entry.fullCombo, entry.fullChain, left, cellTop)
            }
        }

        private fun drawJacket(canvas: Canvas, imageName: String, left: Int, top: Int) {
            val destination = RectF(left.toFloat(), top.toFloat(), (left + jacketSize).toFloat(), (top + jacketSize).toFloat())
            val bitmap = localJacketPath(imageName)?.let(BitmapFactory::decodeFile)
            canvas.withClip(Path().apply { addRoundRect(destination, 10f, 10f, Path.Direction.CW) }) {
                if (bitmap == null) {
                    paint.color = if (background == Color.WHITE) Color.LTGRAY else Color.rgb(45, 45, 53)
                    drawRect(destination, paint)
                    drawText(this, "♪", left + jacketSize * 0.42f, top + jacketSize * 0.62f, 32f, secondary)
                } else {
                    paint.isFilterBitmap = true
                    drawBitmap(bitmap, centerCropSource(bitmap.width, bitmap.height), destination, paint)
                    bitmap.recycle()
                }
            }
        }

        private fun drawBadges(canvas: Canvas, rank: String?, combo: String?, chain: String?, left: Int, top: Int) {
            val values = listOfNotNull(rank, combo, chain)
            values.asReversed().forEachIndexed { index, value ->
                setText(14f, Color.WHITE, true)
                val badgeWidth = paint.measureText(value) + 12f
                val badgeTop = top + jacketSize - 22f - index * 25f
                val badgeLeft = left + jacketSize - badgeWidth - 5f
                paint.color = statusColor(value)
                canvas.drawRoundRect(RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + 20f), 5f, 5f, paint)
                drawText(canvas, value, badgeLeft + 6f, badgeTop + 15f, 14f, Color.WHITE, true)
            }
        }

        private fun drawText(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
            setText(size, color, bold)
            canvas.drawText(value, x, y, paint)
        }

        private fun setText(size: Float, color: Int, bold: Boolean) {
            paint.style = Paint.Style.FILL
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }

        private fun centerCropSource(width: Int, height: Int): Rect = if (width > height) {
            val offset = (width - height) / 2
            Rect(offset, 0, offset + height, height)
        } else {
            val offset = (height - width) / 2
            Rect(0, offset, width, offset + width)
        }

        private fun levelColor(label: String, index: Int): Int = when (((label.toDoubleOrNull() ?: 0.0) * 10).toInt() % 10) {
            0, 5 -> Color.rgb(211, 74, 99)
            1, 6 -> Color.rgb(77, 120, 255)
            2, 7 -> Color.rgb(63, 155, 116)
            3, 8 -> Color.rgb(180, 91, 255)
            else -> if (index % 2 == 0) Color.rgb(200, 74, 123) else Color.rgb(84, 137, 255)
        }

        private fun difficultyColor(value: String): Int = when (value.lowercase(Locale.ROOT)) {
            "basic" -> Color.rgb(101, 185, 74)
            "advanced" -> Color.rgb(230, 189, 49)
            "expert" -> Color.rgb(227, 74, 71)
            "master" -> Color.rgb(154, 80, 201)
            "ultima" -> Color.rgb(227, 38, 85)
            else -> Color.rgb(74, 168, 194)
        }

        private fun statusColor(value: String): Int = when (value.uppercase(Locale.ROOT)) {
            "SSS+", "SSS" -> Color.rgb(255, 217, 0)
            "AJC" -> Color.rgb(226, 185, 59)
            "AJ" -> Color.rgb(240, 166, 74)
            "FC" -> Color.rgb(86, 166, 217)
            else -> if (value.startsWith("金")) Color.rgb(213, 166, 42) else Color.rgb(158, 172, 190)
        }
    }
}
