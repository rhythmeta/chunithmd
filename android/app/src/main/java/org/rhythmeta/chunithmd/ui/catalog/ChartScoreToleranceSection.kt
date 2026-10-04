package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.ChunithmScoreRules
import org.rhythmeta.chunithmd.shared.ScoreToleranceCalculator
import org.rhythmeta.chunithmd.shared.localization.tr
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ChartScoreToleranceSection(songId: String, sheet: CatalogSheet, accentColor: Color) {
    var targetScore by rememberSaveable(songId, sheet.type, sheet.difficulty) {
        mutableIntStateOf(ChunithmScoreRules.rankThresholds.last().score)
    }
    val tolerance = remember(sheet.noteCounts?.total, targetScore) {
        ScoreToleranceCalculator.calculate(sheet.noteCounts?.total, targetScore)
    } ?: return

    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                tr("容错计算器"),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "≥ ${String.format(Locale.ROOT, "%,d", targetScore)}",
                style = MiuixTheme.textStyles.footnote2,
                color = accentColor,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChunithmScoreRules.rankThresholds.asReversed().forEach { target ->
                val selected = targetScore == target.score
                Text(
                    target.rank,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accentColor else {
                        scoreRankColor(target.rank) ?: MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    modifier = Modifier
                        .squircleSurface(
                            accentColor.copy(alpha = if (selected) 0.16f else 0.08f),
                            50.dp,
                            SquircleExtension,
                        )
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { targetScore = target.score })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToleranceResult("JUSTICE", tolerance.justice, Color(0xFFE69A24), Modifier.weight(1f))
            ToleranceResult("ATTACK", tolerance.attack, Color(0xFF34C759), Modifier.weight(1f))
            ToleranceResult("MISS", tolerance.miss, MiuixTheme.colorScheme.onSurfaceVariantSummary, Modifier.weight(1f))
        }
        Text(
            tr("各项独立计算，其余音符均为 JUSTICE CRITICAL，不可相加。"),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun ToleranceResult(title: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .squircleSurface(color.copy(alpha = 0.08f), 12.dp, SquircleExtension)
            .squircleBorder(1.dp, color.copy(alpha = 0.15f), 12.dp, SquircleExtension)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MiuixTheme.textStyles.footnote2, fontWeight = FontWeight.Bold, color = color)
        Text(value.toString(), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, color = color)
        Text(tr("上限"), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
