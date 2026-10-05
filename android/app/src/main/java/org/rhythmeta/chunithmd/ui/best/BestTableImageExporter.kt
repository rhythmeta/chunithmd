package org.rhythmeta.chunithmd.ui.best

import org.rhythmeta.chunithmd.shared.localization.tr

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
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.compose.ui.graphics.toArgb
import org.rhythmeta.chunithmd.ui.components.playerRatingColors
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.R
import org.rhythmeta.chunithmd.shared.ClearType
import org.rhythmeta.chunithmd.shared.BestTableShareEntry
import org.rhythmeta.chunithmd.shared.FullChainType
import org.rhythmeta.chunithmd.shared.FullComboType

internal object BestTableImageExporter {
    suspend fun renderToCache(
        context: Context,
        bestEntries: List<BestTableShareEntry>,
        newEntries: List<BestTableShareEntry>,
        rating: Double,
        bestAverage: Double,
        newAverage: Double,
        bestCount: Int,
        newCount: Int,
        version: String,
        userName: String?,
        darkTheme: Boolean,
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val renderer = Renderer(
                bestEntries,
                newEntries,
                rating,
                bestAverage,
                newAverage,
                bestCount,
                newCount,
                version,
                userName.orEmpty(),
                darkTheme,
                context.getDrawable(R.mipmap.ic_launcher),
            )
            val bitmap = renderer.render()
            File(context.cacheDir, "best-table").apply { mkdirs() }
                .resolve("best-table.png")
                .also { file ->
                    file.outputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
                    bitmap.recycle()
                }
        }.getOrNull()
    }

    private class Renderer(
        private val bestEntries: List<BestTableShareEntry>,
        private val newEntries: List<BestTableShareEntry>,
        private val rating: Double,
        private val bestAverage: Double,
        private val newAverage: Double,
        private val bestCount: Int,
        private val newCount: Int,
        private val version: String,
        private val userName: String,
        darkTheme: Boolean,
        private val appIcon: Drawable?,
    ) {
        private val palette = Palette(darkTheme)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)

        fun render(): Bitmap {
            val sections = listOf(
                ExportSection("N$newCount", NewAccent, newEntries, newCount, newAverage),
                ExportSection("B$bestCount", OldAccent, bestEntries, bestCount, bestAverage),
            ).filter { it.entries.isNotEmpty() }
            val logicalHeight = HEADER_HEIGHT + sections.sumOf { sectionHeight(it.entries.size) } + FOOTER_HEIGHT
            val bitmap = createBitmap(CANVAS_WIDTH * OUTPUT_SCALE, logicalHeight * OUTPUT_SCALE)
            val canvas = Canvas(bitmap)
            canvas.drawColor(palette.background)
            canvas.scale(OUTPUT_SCALE.toFloat(), OUTPUT_SCALE.toFloat())
            drawHeader(canvas)
            var top = HEADER_HEIGHT
            sections.forEach { section ->
                drawSection(canvas, section, top)
                top += sectionHeight(section.entries.size)
            }
            drawFooter(canvas, top)
            return bitmap
        }

        private fun drawHeader(canvas: Canvas) {
            val name = userName.trim()
            val labelY = if (name.isNotEmpty()) {
                drawText(canvas, name, SECTION_PADDING.toFloat(), 64f, 24f, palette.primary, true)
                95f
            } else 72f
            drawText(canvas, tr("玩家 Rating"), SECTION_PADDING.toFloat(), labelY, 14f, palette.secondary)
            drawRating(canvas, rating)
            var left = SECTION_PADDING.toFloat()
            left += drawSummaryPill(canvas, left, "N$newCount", newAverage, NewAccent) + 12f
            drawSummaryPill(canvas, left, "B$bestCount", bestAverage, OldAccent)
        }

        private fun drawRating(canvas: Canvas, value: Double) {
            val text = String.format(Locale.ROOT, "%.2f", value)
            setText(56f, Color.WHITE, true)
            val left = CANVAS_WIDTH - SECTION_PADDING - paint.measureText(text)
            paint.shader = ratingShader(value, left, left + paint.measureText(text))
            canvas.drawText(text, left, 84f, paint)
            paint.shader = null
        }

        private fun drawSummaryPill(canvas: Canvas, left: Float, label: String, value: Double, accent: Int): Float {
            setText(13f, palette.secondary)
            val labelWidth = paint.measureText(label)
            val valueText = String.format(Locale.ROOT, "%.2f", value)
            setText(13f, accent, true)
            val valueWidth = paint.measureText(valueText)
            val width = 12f + 8f + 6f + labelWidth + 6f + valueWidth + 12f
            paint.color = withAlpha(accent, 0.12f)
            canvas.drawRoundRect(RectF(left, 126f, left + width, 158f), 16f, 16f, paint)
            paint.color = accent
            canvas.drawCircle(left + 16f, 142f, 4f, paint)
            drawText(canvas, label, left + 26f, 147f, 13f, palette.secondary)
            drawText(canvas, valueText, left + 32f + labelWidth, 147f, 13f, accent, true)
            return width
        }

        private fun drawSection(canvas: Canvas, section: ExportSection, top: Int) {
            val titleBaseline = top + 37f
            drawText(canvas, section.title, SECTION_PADDING.toFloat(), titleBaseline, 20f, palette.primary, true)
            val titleWidth = measure(section.title, 20f, true)
            drawText(canvas, String.format(Locale.ROOT, "%.2f", section.average), SECTION_PADDING + titleWidth + 12f, titleBaseline, 14f, section.accent, true)
            val gridTop = top + SECTION_HEADER_HEIGHT
            section.entries.chunked(COLUMNS).forEachIndexed { row, entries ->
                entries.forEachIndexed { column, entry ->
                    drawSongCard(canvas, entry, SECTION_PADDING + column * (CARD_WIDTH + CARD_SPACING), gridTop + row * (CARD_HEIGHT + CARD_SPACING))
                }
            }
        }

        private fun drawSongCard(canvas: Canvas, entry: BestTableShareEntry, left: Int, top: Int) {
            val difficulty = difficultyColor(entry.difficulty)
            val bounds = RectF(left.toFloat(), top.toFloat(), (left + CARD_WIDTH).toFloat(), (top + CARD_HEIGHT).toFloat())
            paint.style = Paint.Style.FILL
            paint.color = compositeOver(palette.background, difficulty)
            canvas.drawRoundRect(bounds, 6f, 6f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = withAlpha(difficulty, 0.34f)
            canvas.drawRoundRect(bounds, 6f, 6f, paint)
            paint.style = Paint.Style.FILL
            drawJacket(canvas, entry.jacketPath, left + 6, top + 6)
            val contentLeft = left + 76f
            val contentRight = left + CARD_WIDTH - 6f
            drawEllipsizedText(canvas, entry.title, contentLeft, top + 19f, contentRight - contentLeft, palette.primary)
            drawText(canvas, entry.rank, contentLeft, top + 37f, 12f, rankColor(entry.rank), true)
            val rankWidth = measure(entry.rank, 12f, true)
            drawText(canvas, formatScore(entry.score), contentLeft + rankWidth + 4f, top + 37f, 9f, palette.secondary)
            val levelText = "${entry.level} → ${String.format(Locale.ROOT, "%.2f", entry.rating)}"
            drawText(canvas, levelText, contentLeft, top + 53f, 9f, difficulty, true)
            var badgeLeft = contentLeft
            ClearType.displayName(entry.clear)?.let { display ->
                badgeLeft += drawBadge(canvas, display, badgeLeft, top + 55f, clearColor(entry.clear)) + 3f
            }
            entry.fullCombo?.takeIf(String::isNotBlank)?.let { value ->
                badgeLeft += drawBadge(canvas, comboBadgeText(value), badgeLeft, top + 55f, comboColor(value)) + 3f
            }
            entry.fullChain?.takeIf(String::isNotBlank)?.let { value ->
                drawBadge(canvas, tr(FullChainType.displayName(value) ?: value.uppercase()), badgeLeft, top + 55f, chainColor(value))
            }
        }

        private fun drawJacket(canvas: Canvas, path: String?, left: Int, top: Int) {
            val destination = RectF(left.toFloat(), top.toFloat(), (left + JACKET_SIZE).toFloat(), (top + JACKET_SIZE).toFloat())
            val jacket = path?.let { BitmapFactory.decodeFile(it) }
            canvas.withClip(Path().apply { addRoundRect(destination, 4f, 4f, Path.Direction.CW) }) {
                if (jacket == null) {
                    paint.alpha = 255
                    paint.shader = null
                    paint.style = Paint.Style.FILL
                    paint.color = palette.emptyCard
                    drawRect(destination, paint)
                    drawText(this, "♪", left + 24f, top + 40f, 22f, palette.subtle)
                } else {
                    // Card borders use a translucent paint color; reset it before drawing the cover.
                    paint.alpha = 255
                    paint.shader = null
                    paint.style = Paint.Style.FILL
                    paint.isFilterBitmap = true
                    drawBitmap(jacket, centerCropSource(jacket.width, jacket.height), destination, paint)
                    jacket.recycle()
                }
            }
        }

        private fun drawBadge(canvas: Canvas, text: String, left: Float, top: Float, color: Int): Float {
            setText(7f, Color.WHITE, true)
            val width = paint.measureText(text) + 6f
            paint.color = color
            canvas.drawRoundRect(RectF(left, top, left + width, top + 12f), 2f, 2f, paint)
            drawText(canvas, text, left + 3f, top + 8.75f, 7f, Color.WHITE, true)
            return width
        }

        private fun drawFooter(canvas: Canvas, top: Int) {
            val versionText = "●  $version"
            setText(11f, palette.secondary, true)
            canvas.drawText(versionText, (CANVAS_WIDTH - paint.measureText(versionText)) / 2f, top + 27f, paint)
            val watermark = "chunithmd"
            setText(12f, palette.subtle)
            val textWidth = paint.measureText(watermark)
            val iconSize = 16f
            val iconGap = 6f
            val groupWidth = iconSize + iconGap + textWidth
            val groupLeft = (CANVAS_WIDTH - groupWidth) / 2f
            appIcon?.let { icon ->
                icon.setBounds(
                    groupLeft.toInt(),
                    (top + 36f).toInt(),
                    (groupLeft + iconSize).toInt(),
                    (top + 52f).toInt(),
                )
                icon.draw(canvas)
            }
            drawText(canvas, watermark, groupLeft + iconSize + iconGap, top + 49f, 12f, palette.subtle)
        }

        private fun drawEllipsizedText(canvas: Canvas, text: String, x: Float, baseline: Float, maxWidth: Float, color: Int) {
            setText(11f, color, true)
            if (paint.measureText(text) <= maxWidth) {
                canvas.drawText(text, x, baseline, paint)
                return
            }
            val ellipsis = "…"
            val count = paint.breakText(text, true, maxWidth - paint.measureText(ellipsis), null)
            canvas.drawText(text.take(count) + ellipsis, x, baseline, paint)
        }

        private fun drawText(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, alignRight: Boolean = false) {
            setText(size, color, bold)
            paint.textAlign = if (alignRight) Paint.Align.RIGHT else Paint.Align.LEFT
            canvas.drawText(value, x, y, paint)
        }

        private fun setText(size: Float, color: Int, bold: Boolean = false) {
            paint.shader = null
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            else android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun measure(value: String, size: Float, bold: Boolean): Float {
            setText(size, Color.WHITE, bold)
            return paint.measureText(value)
        }

        private fun formatScore(score: Int): String = String.format(Locale.ROOT, "%,d", score)

        private fun sectionHeight(entryCount: Int): Int = SECTION_HEADER_HEIGHT + rowsFor(entryCount) * (CARD_HEIGHT + CARD_SPACING) + SECTION_BOTTOM_PADDING
        private fun rowsFor(entryCount: Int): Int = (entryCount + COLUMNS - 1) / COLUMNS

        private data class ExportSection(
            val title: String,
            val accent: Int,
            val entries: List<BestTableShareEntry>,
            val capacity: Int,
            val average: Double,
        )

        private data class Palette(
            val dark: Boolean,
        ) {
            val background = if (dark) Color.rgb(15, 15, 19) else Color.WHITE
            val primary = if (dark) Color.WHITE else Color.BLACK
            val secondary = if (dark) Color.rgb(158, 158, 160) else Color.rgb(102, 102, 102)
            val subtle = if (dark) Color.rgb(87, 87, 90) else Color.rgb(179, 179, 179)
            val emptyCard = if (dark) Color.rgb(27, 27, 31) else Color.rgb(242, 242, 242)
        }

        private fun centerCropSource(width: Int, height: Int): Rect = if (width > height) {
            val offset = (width - height) / 2
            Rect(offset, 0, offset + height, height)
        } else {
            val offset = (height - width) / 2
            Rect(0, offset, width, offset + width)
        }

        private fun ratingShader(value: Double, left: Float, right: Float): Shader = LinearGradient(
            left,
            0f,
            right,
            0f,
            playerRatingColors(value).map { it.toArgb() }.toIntArray(),
            null,
            Shader.TileMode.CLAMP,
        )

        private fun compositeOver(background: Int, foreground: Int): Int {
            val alpha = 0.15f
            return Color.rgb(
                (Color.red(foreground) * alpha + Color.red(background) * (1f - alpha)).toInt(),
                (Color.green(foreground) * alpha + Color.green(background) * (1f - alpha)).toInt(),
                (Color.blue(foreground) * alpha + Color.blue(background) * (1f - alpha)).toInt(),
            )
        }

        private fun withAlpha(color: Int, alpha: Float): Int = Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

        private fun difficultyColor(value: String): Int = when {
            value.contains("basic", true) -> Color.rgb(54, 191, 99)
            value.contains("advanced", true) -> Color.rgb(252, 161, 59)
            value.contains("expert", true) -> Color.rgb(247, 83, 106)
            value.contains("remaster", true) -> Color.rgb(227, 189, 252)
            value.contains("master", true) -> Color.rgb(163, 78, 228)
            else -> Color.rgb(255, 45, 85)
        }

        private fun rankColor(value: String): Int = when (value.uppercase()) {
            "SSS+", "SSS" -> Color.rgb(255, 217, 0)
            "SS+", "SS" -> Color.rgb(255, 191, 0)
            "S+", "S" -> Color.rgb(255, 153, 0)
            "AAA" -> Color.rgb(204, 153, 255)
            "AA" -> Color.rgb(153, 204, 255)
            "A" -> Color.rgb(128, 230, 128)
            else -> Color.LTGRAY
        }

        private fun clearColor(value: String?): Int = when (ClearType.fromWire(value)) {
            ClearType.Catastrophy -> Color.rgb(175, 82, 222)
            ClearType.Absolute -> Color.rgb(0, 122, 255)
            ClearType.Brave -> Color.rgb(52, 199, 89)
            ClearType.Hard -> Color.rgb(255, 149, 0)
            ClearType.Clear -> Color.rgb(90, 200, 250)
            ClearType.Failed -> Color.rgb(255, 59, 48)
            null -> Color.LTGRAY
        }

        private fun comboColor(value: String): Int = when (FullComboType.fromWire(value)) {
            FullComboType.AllJusticeCritical, FullComboType.AllJustice -> Color.rgb(255, 153, 0)
            FullComboType.FullCombo -> Color.rgb(51, 191, 51)
            null -> Color.LTGRAY
        }

        private fun comboBadgeText(value: String): String = FullComboType.displayName(value).orEmpty()

        private fun chainColor(value: String): Int = when (FullChainType.fromWire(value)) {
            FullChainType.FullChain -> Color.rgb(183, 196, 214)
            FullChainType.FullChain2 -> Color.rgb(212, 167, 44)
            null -> Color.LTGRAY
        }
    }

    private const val OUTPUT_SCALE = 2
    private const val COLUMNS = 5
    private const val CARD_WIDTH = 220
    private const val CARD_HEIGHT = 74
    private const val CARD_SPACING = 8
    private const val SECTION_PADDING = 24
    private const val JACKET_SIZE = 62
    private const val CANVAS_WIDTH = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * CARD_SPACING + SECTION_PADDING * 2
    private const val HEADER_HEIGHT = 184
    private const val SECTION_HEADER_HEIGHT = 50
    private const val SECTION_BOTTOM_PADDING = 16
    private const val FOOTER_HEIGHT = 70
    private const val BestAccent = 0xFFFF9500.toInt()
    private const val OldAccent = 0xFF4D80FF.toInt()
    private const val NewAccent = 0xFFFF6B6B.toInt()
}
