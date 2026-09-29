package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.scoreProgress

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
    val worldsEndBrush = if (isWorldsEnd) {
        Brush.sweepGradient(WORLDS_END_GRADIENT_COLORS)
    } else {
        null
    }
    val worldsEndOuterBrush = if (isWorldsEnd) {
        Brush.sweepGradient(WORLDS_END_GRADIENT_COLORS.map { it.copy(alpha = 0.3f) })
    } else {
        null
    }
    val progress = scoreProgress(score?.score)

    Canvas(modifier = Modifier.size(8.dp)) {
        val outerStroke = 1.2.dp.toPx()
        if (worldsEndOuterBrush != null) {
            drawCircle(
                brush = worldsEndOuterBrush,
                radius = (size.minDimension - outerStroke) / 2f,
                style = Stroke(width = outerStroke),
            )
        } else {
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = (size.minDimension - outerStroke) / 2f,
                style = Stroke(width = outerStroke),
            )
        }
        if (progress > 0f) {
            val innerSize = 4.dp.toPx()
            val inset = (size.minDimension - innerSize) / 2f
            if (worldsEndBrush != null) {
                drawArc(
                    brush = worldsEndBrush,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(innerSize, innerSize),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Butt),
                )
            } else {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(innerSize, innerSize),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Butt),
                )
            }
        }
    }
}
