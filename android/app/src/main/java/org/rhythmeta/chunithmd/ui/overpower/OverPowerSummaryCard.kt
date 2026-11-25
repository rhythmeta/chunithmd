package org.rhythmeta.chunithmd.ui.overpower

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.rhythmeta.chunithmd.shared.OverPowerPoints
import org.rhythmeta.chunithmd.shared.OverPowerTier
import org.rhythmeta.chunithmd.shared.localization.tr
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OverPowerSummaryCard(
    points: OverPowerPoints,
    coverageLabel: String,
    recordedCount: Int,
    totalCount: Int,
    scopeName: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (scopeName != null) Text(scopeName, style = MiuixTheme.textStyles.footnote1)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                RateRing(points)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryMetric(tr("已获得 OP"), overPowerNumber(points.earned))
                    SummaryMetric(tr("理论 OP"), overPowerNumber(points.maximum))
                    SummaryMetric(coverageLabel, "$recordedCount / $totalCount")
                }
            }
            HorizontalDivider()
            Text(nextGoal(points), style = MiuixTheme.textStyles.body2)
        }
    }
}

@Composable
private fun RateRing(points: OverPowerPoints) {
    val ringSize = with(LocalDensity.current) { 126.sp.toDp() }
    val track = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val fill = overPowerTierBrush(points.tier)
    Box(Modifier.size(ringSize).semantics(mergeDescendants = true) {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = Stroke(width = 7.dp.toPx())
            val diameter = size.minDimension - stroke.width
            val bounds = Offset(center.x - diameter / 2, center.y - diameter / 2)
            drawCircle(track, radius = diameter / 2, style = stroke)
            drawArc(
                brush = fill, startAngle = -90f, sweepAngle = (points.percent * 3.6).toFloat(),
                useCenter = false, topLeft = bounds, size = Size(diameter, diameter), style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tr("OP 率"), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Text(overPowerPercent(points.percent), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Text(value, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.SemiBold)
    }
}

private fun nextGoal(points: OverPowerPoints): String {
    if (points.pointsToNext == 0.0) return tr("已达理论值")
    val gap = overPowerNumber(points.pointsToNext)
    val tier = when (points.nextTier) {
        OverPowerTier.Silver -> tr("银色")
        OverPowerTier.Gold -> tr("金色")
        OverPowerTier.Platinum -> tr("铂色")
        OverPowerTier.Rainbow -> tr("虹色")
        OverPowerTier.White, null -> return tr("距 100.00% 还差 {0} OP", gap)
    }
    return tr("距{0} {1} 还差 {2} OP", tier, overPowerPercent(points.nextPercent.toDouble()), gap)
}
