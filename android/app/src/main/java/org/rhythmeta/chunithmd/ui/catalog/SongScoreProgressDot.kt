package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.chartProgress

/**
 * Difficulty progress dot shown next to a sheet in the catalog.
 *
 * The hollow ring fills from 975,000 to 1,000,000, then the inner dot fills from 1,000,000 to
 * 1,010,000. Both stages use the same difficulty accent, so a chart at 987,500 shows half of the
 * ring in accent color and a chart at 1,010,000 shows a solid dot.
 */
@Composable
internal fun SongScoreProgressDot(
    sheet: CatalogSheet,
    score: ScoreRecord?,
) {
    val color = if (sheet.type.equals("we", ignoreCase = true)) {
        difficultyColor("world's end")
    } else {
        difficultyColor(sheet.difficulty)
    }
    val isWorldsEnd = sheet.type.equals("we", ignoreCase = true)
    val isUltima = !isWorldsEnd && sheet.difficulty.equals("ultima", ignoreCase = true)
    val accentBrush: Brush = when {
        isWorldsEnd -> Brush.sweepGradient(WORLDS_END_GRADIENT_COLORS)
        isUltima -> ultimaStripedBrush()
        else -> SolidColor(color)
    }
    val trackBrush: Brush = when {
        isWorldsEnd -> Brush.sweepGradient(WORLDS_END_GRADIENT_COLORS.map { it.copy(alpha = 0.3f) })
        isUltima -> ultimaStripedBrush(alpha = 0.3f)
        else -> SolidColor(color.copy(alpha = 0.3f))
    }
    val progress = chartProgress(score?.score)

    Canvas(modifier = Modifier.size(8.dp)) {
        val outerStroke = 1.2.dp.toPx()
        val ringDiameter = size.minDimension - outerStroke
        val ringTopLeft = Offset((size.width - ringDiameter) / 2f, (size.height - ringDiameter) / 2f)

        drawCircle(
            brush = trackBrush,
            radius = ringDiameter / 2f,
            style = Stroke(width = outerStroke),
        )
        if (progress.ring > 0f) {
            drawArc(
                brush = accentBrush,
                startAngle = -90f,
                sweepAngle = progress.ring * 360f,
                useCenter = false,
                topLeft = ringTopLeft,
                size = Size(ringDiameter, ringDiameter),
                style = Stroke(width = outerStroke, cap = StrokeCap.Butt),
            )
        }
        if (progress.dot > 0f) {
            val innerSize = 4.dp.toPx()
            val inset = (size.minDimension - innerSize) / 2f
            drawArc(
                brush = accentBrush,
                startAngle = -90f,
                sweepAngle = progress.dot * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(innerSize, innerSize),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Butt),
            )
        }
    }
}
