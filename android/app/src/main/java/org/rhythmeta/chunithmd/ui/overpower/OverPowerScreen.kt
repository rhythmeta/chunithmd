package org.rhythmeta.chunithmd.ui.overpower

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.rhythmeta.chunithmd.shared.FullComboType
import org.rhythmeta.chunithmd.shared.OverPowerChart
import org.rhythmeta.chunithmd.shared.OverPowerLevel
import org.rhythmeta.chunithmd.shared.OverPowerOrder
import org.rhythmeta.chunithmd.shared.OverPowerPoints
import org.rhythmeta.chunithmd.shared.OverPowerReport
import org.rhythmeta.chunithmd.shared.OverPowerTier
import org.rhythmeta.chunithmd.shared.localization.tr
import org.rhythmeta.chunithmd.ui.catalog.difficultyColor
import org.rhythmeta.chunithmd.ui.components.ScoreEntryBadge
import org.rhythmeta.chunithmd.ui.components.ScoreEntryCard
import org.rhythmeta.chunithmd.ui.components.comboStatusColor
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OverPowerHomeCard(report: OverPowerReport?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val all = report?.all
    Card(
        modifier = modifier.height(140.dp), cornerRadius = 16.dp, insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer), onClick = onClick,
    ) {
        Icon(Icons.Rounded.Bolt, null, Modifier.size(30.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        Spacer(Modifier.height(12.dp))
        Text("OVER POWER", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
        val subtitle = if (all != null && all.recordedCount > 0) tr("全曲 OP 率 {0}", overPowerPercent(all.points.percent))
            else tr("查看全曲与新曲进度")
        DetailText(subtitle)
    }
}

@Composable
internal fun OverPowerScreen(
    scopeIndex: Int, breakdown: Int, onBreakdownChange: (Int) -> Unit,
    order: OverPowerOrder,
    report: OverPowerReport?,
    hasCatalog: Boolean,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onOpenLevel: (String, Boolean) -> Unit,
    onOpenSong: (String) -> Unit,
) {
    var recordFilter by rememberSaveable { mutableIntStateOf(0) }
    val scope = if (scopeIndex == 0) report?.all else report?.hot
    val songs = remember(scope, recordFilter, order, breakdown) {
        if (scope != null && breakdown == 0) order.songs(scope, recorded = recordFilter == 0) else emptyList()
    }
    val levels = remember(scope, order, breakdown) {
        if (scope != null && breakdown == 1) order.levels(scope) else emptyList()
    }
    val listState = key(scopeIndex, breakdown, order, recordFilter) { rememberLazyListState() }
    LazyColumn(modifier.fillMaxSize(), state = listState, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (scope == null) {
            item { ReportStatus(hasCatalog) }
            return@LazyColumn
        }
        if (scope.songs.isEmpty()) {
            item { DetailText(tr("当前范围没有可统计的歌曲")) }
            return@LazyColumn
        }
        item("summary") {
            OverPowerSummaryCard(
                points = scope.points, coverageLabel = tr("已记录歌曲"),
                recordedCount = scope.recordedCount, totalCount = scope.songs.size,
            )
        }
        item("breakdown") {
            TabRowWithContour(
                tabs = listOf(tr("歌曲明细"), tr("按谱面等级")), selectedTabIndex = breakdown,
                onTabSelected = onBreakdownChange, modifier = Modifier.fillMaxWidth(),
            )
        }
        if (breakdown == 1) {
            items(levels, key = { "level-" + it.level }) { level ->
                LevelRow(level) { onOpenLevel(level.level, scopeIndex == 1) }
            }
        } else {
            item("filter") {
                Card(Modifier.fillMaxWidth()) {
                    WindowDropdownPreference(
                        title = tr("成绩记录"), items = listOf(tr("已记录"), tr("未记录")),
                        selectedIndex = recordFilter, onSelectedIndexChange = { recordFilter = it },
                    )
                }
            }
            if (songs.isEmpty()) item { DetailText(tr("没有符合条件的歌曲")) }
            items(songs, key = { "song-" + it.songId }) { song ->
                OverPowerScoreCard(
                    chart = song.displayChart, points = song.points, songTotal = true,
                    jacketBaseUrl = jacketBaseUrl, localJacketPath = localJacketPath,
                    onClick = { onOpenSong(song.songId) },
                )
            }
        }
    }
}

@Composable
internal fun OverPowerLevelScreen(
    order: OverPowerOrder,
    report: OverPowerReport?, hasCatalog: Boolean, level: String, hot: Boolean,
    jacketBaseUrl: String, localJacketPath: (String) -> String?,
    modifier: Modifier, contentPadding: PaddingValues, onOpenSong: (String) -> Unit,
) {
    val scope = if (hot) report?.hot else report?.all
    val group = scope?.levels?.find { it.level == level }
    val charts = remember(group, order) { group?.let(order::charts).orEmpty() }
    val listState = key(order) { rememberLazyListState() }
    LazyColumn(modifier.fillMaxSize(), state = listState, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            report == null -> item { ReportStatus(hasCatalog) }
            group == null -> item { DetailText(tr("没有符合条件的谱面")) }
            else -> {
                item {
                    OverPowerSummaryCard(
                        points = group.points, coverageLabel = tr("已记录谱面"),
                        recordedCount = group.recordedCount, totalCount = group.charts.size,
                        scopeName = if (hot) tr("HOT·新曲") else tr("全曲"),
                    )
                }
                items(charts, key = { it.sheetKey }) { chart ->
                    OverPowerScoreCard(
                        chart = chart, points = chart.points, songTotal = false,
                        jacketBaseUrl = jacketBaseUrl, localJacketPath = localJacketPath,
                        onClick = { onOpenSong(chart.songId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelRow(level: OverPowerLevel, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lv. " + level.level, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(overPowerPercent(level.points.percent))
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp))
            }
            LinearProgressIndicator(progress = (level.points.percent / 100).toFloat(), modifier = Modifier.fillMaxWidth())
            DetailText(tr("已记录谱面 {0} / {1}", level.recordedCount, level.charts.size))
        }
    }
}

@Composable
private fun OverPowerScoreCard(
    chart: OverPowerChart, points: OverPowerPoints, songTotal: Boolean,
    jacketBaseUrl: String, localJacketPath: (String) -> String?, onClick: () -> Unit,
) {
    val difficulty = chart.difficulty
    val best = chart.best
    ScoreEntryCard(
        songId = chart.songId, chartKey = if (songTotal) chart.songId else chart.sheetKey,
        title = chart.title, imageName = chart.imageName, difficulty = difficulty,
        rank = best?.rank, score = best?.score,
        value = overPowerNumber(points.earned), caption = "/ " + overPowerNumber(points.maximum) + " OP",
        valueBrush = overPowerTierBrush(points.tier), jacketBaseUrl = jacketBaseUrl, localJacketPath = localJacketPath,
        onClick = onClick,
    ) {
        val label = if (songTotal && chart.recorded) tr("{0} 计入", difficulty.uppercase()) else difficulty.uppercase()
        ScoreEntryBadge(label, difficultyColor(difficulty))
        FullComboType.displayName(best?.fullCombo)?.let {
            ScoreEntryBadge(tr(it), comboStatusColor(best?.fullCombo))
        }
    }
}

@Composable
internal fun overPowerTierBrush(tier: OverPowerTier): Brush = when (tier) {
    OverPowerTier.White -> SolidColor(MiuixTheme.colorScheme.onSurface)
    OverPowerTier.Silver -> SolidColor(Color(0xFF8493A2))
    OverPowerTier.Gold -> SolidColor(Color(0xFFBA8829))
    OverPowerTier.Platinum -> SolidColor(Color(0xFF799BAF))
    OverPowerTier.Rainbow -> Brush.horizontalGradient(listOf(
        Color(0xFFE46670), Color(0xFFD2A13A), Color(0xFF479F89), Color(0xFF706ECB),
    ))
}

@Composable
private fun ReportStatus(hasCatalog: Boolean) {
    if (hasCatalog) Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    } else DetailText(tr("请先在静态数据中下载歌曲目录。"))
}

@Composable
private fun DetailText(text: String) {
    Text(text, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
}

internal fun overPowerPercent(value: Double) = String.format(Locale.ROOT, "%.2f%%", value)
internal fun overPowerNumber(value: Double): String = String.format(Locale.ROOT, "%,.3f", value).removeSuffix("0")
