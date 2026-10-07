package org.rhythmeta.chunithmd.scanner

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.ChunithmScoreRules
import org.rhythmeta.chunithmd.shared.scanner.ScoreScanner
import org.rhythmeta.chunithmd.ui.catalog.difficultyColor
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max

@Composable
internal fun ScannerResultCard(state: ScannerUiState, catalog: CatalogBundle?, jacketBaseUrl: String,
    localJacketPath: (String) -> String?, onClick: () -> Unit) {
    val fields = state.fields ?: return
    val candidate = state.match ?: return
    val song = catalog?.catalog?.songs?.firstOrNull { it.songId == candidate.songId }
    val isWe = candidate.type == "we"
    val color = difficultyColor(if (isWe) "world's end" else candidate.difficulty)
    val score = ScoreScanner.parseScore(fields.score)
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            .squircleSurface(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = .94f), 16.dp, SquircleExtension)
            .squircleBorder(1.dp, color.copy(alpha = .2f), 16.dp, SquircleExtension)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(4.dp, 46.dp).squircleSurface(color, 2.dp, SquircleExtension))
        Spacer(Modifier.width(12.dp))
        if (song != null) {
            AsyncImage(localJacketPath(song.imageName)?.let(::File)
                ?: (jacketBaseUrl.trimEnd('/') + "/" + song.imageName.trimStart('/')),
                contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).squircleSurface(Color.Transparent, 8.dp, SquircleExtension))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(candidate.title, maxLines = 1,
                fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp, lineHeight = 14.sp),
                modifier = Modifier.fillMaxWidth().basicMarquee())
            Text(if (isWe) "WORLD'S END" else (candidate.difficulty).uppercase(), color = color,
                fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp, lineHeight = 13.sp))
        }
        if (score != null) Column(Modifier.padding(start = 8.dp), horizontalAlignment = Alignment.End) {
            Text("%,d".format(java.util.Locale.ROOT, score), fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp, lineHeight = 14.sp))
            Text(ChunithmScoreRules.rank(score), color = color, fontWeight = FontWeight.Black,
                style = MiuixTheme.textStyles.body2.copy(fontSize = 10.sp, lineHeight = 12.sp))
        }
        Text(if (isWe) ScoreScanner.attribute(fields.level) else candidate.level,
            color = color, fontWeight = FontWeight.Black,
            style = MiuixTheme.textStyles.body1.copy(fontSize = 20.sp, lineHeight = 22.sp),
            modifier = Modifier.padding(start = 8.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = .4f),
            modifier = Modifier.padding(start = 4.dp, end = 16.dp))
    }
}

@Composable
internal fun ScannerDebugOverlay(state: ScannerUiState, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (state.imageWidth <= 0 || state.imageHeight <= 0) return@Canvas
        val sideways = state.previewRotationDegrees == 90 || state.previewRotationDegrees == 270
        val previewWidth = if (sideways) state.imageHeight else state.imageWidth
        val previewHeight = if (sideways) state.imageWidth else state.imageHeight
        val scale = max(size.width / previewWidth, size.height / previewHeight)
        val width = previewWidth * scale
        val height = previewHeight * scale
        val offsetX = (size.width - width) / 2
        val offsetY = (size.height - height) / 2
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 11.sp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        state.observations.forEach { region ->
            val box = rotateScanBoxForPreview(region.box, state.previewRotationDegrees)
            val left = offsetX + box.x * width
            val top = offsetY + box.y * height
            drawRect(Color(0xFFFFD60A), Offset(left, top), Size(box.width * width, box.height * height),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            val label = "${region.field} ${(region.confidence * 100).toInt()}%"
            val pad = 4.dp.toPx()
            val labelWidth = paint.measureText(label) + pad * 2
            val labelHeight = paint.fontMetrics.run { bottom - top } + pad * 2
            val x = left.coerceIn(0f, (size.width - labelWidth).coerceAtLeast(0f))
            val y = (top - labelHeight).coerceIn(0f, (size.height - labelHeight).coerceAtLeast(0f))
            drawRect(Color(0xFFFFD60A), Offset(x, y), Size(labelWidth, labelHeight))
            drawContext.canvas.nativeCanvas.drawText(label, x + pad, y + pad - paint.fontMetrics.top, paint)
        }
    }
}
