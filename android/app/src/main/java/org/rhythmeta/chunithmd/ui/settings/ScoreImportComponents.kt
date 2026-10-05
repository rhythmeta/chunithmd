package org.rhythmeta.chunithmd.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.shared.importing.ScoreImportResult
import org.rhythmeta.chunithmd.shared.localization.tr
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ImportProfileHeader(profileName: String?, connected: Boolean, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MiuixTheme.colorScheme.primary,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tr("当前档案"), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Text(
                profileName ?: tr("无档案"),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            if (connected) tr("已连接") else tr("未连接"),
            style = MiuixTheme.textStyles.footnote1,
            color = if (connected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
internal fun ImportResultCard(result: ScoreImportResult) {
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(20.dp), cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer)) {
        Text(if (result.updated > 0) tr("成绩导入完成") else tr("没有需要更新的成绩"), style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ImportCount(tr("获取"), result.fetched, Modifier.weight(1f))
            ImportCount(tr("更新"), result.updated, Modifier.weight(1f), highlighted = true)
            ImportCount(tr("跳过"), result.skipped, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ImportCount(label: String, count: Int, modifier: Modifier, highlighted: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            count.toString(),
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
            color = if (highlighted) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
        Text(label, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
