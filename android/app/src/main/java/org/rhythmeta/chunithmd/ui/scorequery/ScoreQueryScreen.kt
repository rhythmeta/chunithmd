package org.rhythmeta.chunithmd.ui.scorequery

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.ScoreQueryEntry
import org.rhythmeta.chunithmd.shared.ScoreQueryFilterSettings
import org.rhythmeta.chunithmd.shared.ScoreQueryResponse
import org.rhythmeta.chunithmd.shared.ScoreQuerySortMode
import org.rhythmeta.chunithmd.shared.ScoreQueryStats
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.buildScoreQueryResponse
import org.rhythmeta.chunithmd.shared.displayFullChain
import org.rhythmeta.chunithmd.shared.displayFullCombo
import org.rhythmeta.chunithmd.shared.filterAndSortScoreQueryEntries
import org.rhythmeta.chunithmd.ui.catalog.difficultyColor
import org.rhythmeta.chunithmd.ui.catalog.ultimaStripedBrush
import org.rhythmeta.chunithmd.ui.catalog.WORLDS_END_GRADIENT_COLORS
import org.rhythmeta.chunithmd.ui.components.ExpandableBottomSheet
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import org.rhythmeta.chunithmd.ui.components.squircleShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun ScoreQueryScreen(
    bundle: CatalogBundle?,
    activeServer: ProfileServer,
    records: List<ScoreRecord>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    contentTopPadding: Dp,
    searchScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    topBarScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    searchText: String,
    filterSettings: ScoreQueryFilterSettings,
    displayMode: org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode,
    sortMode: ScoreQuerySortMode,
    ascending: Boolean,
    filterOpen: Boolean,
    onFilterSettingsChange: (ScoreQueryFilterSettings) -> Unit,
    onFilterDismiss: () -> Unit,
    onOpenSong: (String) -> Unit,
) {
    if (bundle == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val response by produceState<ScoreQueryResponse?>(null, bundle, records, activeServer) {
        value = withContext(Dispatchers.Default) { buildScoreQueryResponse(bundle, records, activeServer) }
    }
    if (response == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val entries = remember(response, searchText, filterSettings, sortMode, ascending) {
        filterAndSortScoreQueryEntries(response!!.entries, searchText, filterSettings, sortMode, ascending)
    }
    if (displayMode == org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode.Grid) {
        ScoreQueryGrid(entries, response!!.stats, contentTopPadding, jacketBaseUrl, localJacketPath, searchScrollConnection, topBarScrollConnection, onOpenSong)
    } else {
        ScoreQueryList(entries, response!!.stats, contentTopPadding, jacketBaseUrl, localJacketPath, searchScrollConnection, topBarScrollConnection, onOpenSong)
    }
    ScoreQueryFilterSheet(filterOpen, filterSettings, onFilterSettingsChange, onFilterDismiss)
}

@Composable
fun ScoreQueryToolbarActions(
    displayMode: org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode,
    sortMode: ScoreQuerySortMode,
    ascending: Boolean,
    filterActive: Boolean,
    onDisplayModeChange: (org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode) -> Unit,
    onSortModeChange: (ScoreQuerySortMode) -> Unit,
    onAscendingChange: (Boolean) -> Unit,
    onFilter: () -> Unit,
) {
    IconButton(onClick = {
        onDisplayModeChange(
            if (displayMode == org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode.Grid) {
                org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode.List
            } else {
                org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode.Grid
            },
        )
    }) {
        Icon(
            if (displayMode == org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode.Grid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
            contentDescription = if (displayMode == org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode.Grid) "列表视图" else "网格视图",
        )
    }
    var sortOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { sortOpen = !sortOpen }) { Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "排序") }
        WindowListPopup(show = sortOpen, alignment = PopupPositionProvider.Align.End, enableWindowDim = true, onDismissRequest = { sortOpen = false }) {
            ListPopupColumn {
                ScoreQuerySortMode.entries.forEachIndexed { index, option ->
                    DropdownImpl(
                        text = when (option) {
                            ScoreQuerySortMode.Rating -> "Rating"
                            ScoreQuerySortMode.Score -> "分数"
                            ScoreQuerySortMode.Level -> "定数"
                        },
                        optionSize = ScoreQuerySortMode.entries.size,
                        isSelected = option == sortMode,
                        index = index,
                        onSelectedIndexChange = { onSortModeChange(option); sortOpen = false },
                    )
                }
                Row(Modifier.fillMaxWidth().clickable {
                    onAscendingChange(!ascending)
                    sortOpen = false
                }.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ascending) "↑" else "↓", style = MiuixTheme.textStyles.title3, color = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(if (ascending) "升序" else "降序", style = MiuixTheme.textStyles.body1)
                }
            }
        }
    }
    IconButton(onClick = onFilter) {
        Icon(Icons.Rounded.FilterList, contentDescription = "筛选", tint = if (filterActive) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ScoreQueryGrid(
    entries: List<ScoreQueryEntry>,
    stats: ScoreQueryStats,
    contentTopPadding: Dp,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    searchScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    topBarScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    onOpenSong: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(searchScrollConnection)
            .nestedScroll(topBarScrollConnection),
        contentPadding = PaddingValues(start = 12.dp, top = contentTopPadding + 8.dp, end = 12.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        item(key = "stats", span = { GridItemSpan(maxLineSpan) }) { ScoreQueryStatsHeader(stats) }
        if (entries.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { ScoreQueryEmpty() }
        } else {
            gridItems(entries, key = ScoreQueryEntry::sheetKey) { entry ->
                ScoreQueryGridCell(entry, jacketBaseUrl, localJacketPath) { onOpenSong(entry.songId) }
            }
        }
    }
}

@Composable
private fun ScoreQueryGridCell(
    entry: ScoreQueryEntry,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onClick: () -> Unit,
) {
    val accent = difficultyColor(if (entry.type.equals("we", true)) "world's end" else entry.difficulty)
    val accentBrush = scoreDifficultyBrush(entry)
    val model = remember(entry.imageName, jacketBaseUrl) {
        localJacketPath(entry.imageName)?.let(::File)
            ?: jacketBaseUrl.trimEnd('/').takeIf { it.isNotBlank() }?.let { it + "/" + entry.imageName.trimStart('/') }
    }
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f)
            .squircleSurface(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f), 8.dp, extension = SquircleExtension)
            .clickable(onClick = onClick),
    ) {
        if (model != null) AsyncImage(model, null, Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (accentBrush != null) {
                        Modifier.border(2.dp, accentBrush, RoundedCornerShape(8.dp))
                    } else {
                        Modifier.squircleBorder(2.dp, accent, 8.dp, extension = SquircleExtension)
                    },
                ),
        )
        Column(Modifier.align(Alignment.BottomEnd).padding(4.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ScoreBadge(entry.rank, rankColor(entry.rank))
            displayFullCombo(entry.fullCombo)?.let { ScoreBadge(it, comboColor(it)) }
            displayFullChain(entry.fullChain)?.let { ScoreBadge(it, chainColor(it), small = true) }
        }
    }
}

@Composable
private fun ScoreBadge(text: String, color: Color, small: Boolean = false) {
    Text(
        text = text,
        fontSize = if (small) 6.sp else 8.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        maxLines = 1,
        modifier = Modifier
            .squircleSurface(color, 3.dp, extension = SquircleExtension)
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}

@Composable
private fun ScoreQueryList(
    entries: List<ScoreQueryEntry>,
    stats: ScoreQueryStats,
    contentTopPadding: Dp,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    searchScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    topBarScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    onOpenSong: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(searchScrollConnection)
            .nestedScroll(topBarScrollConnection),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 8.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "stats") { ScoreQueryStatsHeader(stats) }
        if (entries.isEmpty()) item(key = "empty") { ScoreQueryEmpty() }
        else items(entries, key = ScoreQueryEntry::sheetKey) { entry ->
            ScoreQueryListRow(entry, jacketBaseUrl, localJacketPath) { onOpenSong(entry.songId) }
        }
    }
}

@Composable
private fun ScoreQueryStatsHeader(stats: ScoreQueryStats) {
    Card(Modifier.fillMaxWidth(), cornerRadius = 16.dp, insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp), colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth()) {
                ScoreStat(stats.chartCount.toString(), "谱面")
                ScoreStat(stats.songCount.toString(), "歌曲")
                ScoreStat(stats.sssPlusCount.toString(), "SSS+")
                ScoreStat(stats.sssCount.toString(), "SSS")
            }
            Row(Modifier.fillMaxWidth()) {
                ScoreStat(stats.fcCount.toString(), "FC")
                ScoreStat(stats.ajCount.toString(), "AJ")
                ScoreStat(stats.ajcCount.toString(), "AJC")
                ScoreStat((stats.platinumFullChainCount + stats.goldFullChainCount).toString(), "FULL CHAIN")
            }
        }
    }
}

@Composable
private fun RowScope.ScoreStat(value: String, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
        Text(label, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun ScoreQueryListRow(
    entry: ScoreQueryEntry,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onClick: () -> Unit,
) {
    val accent = difficultyColor(if (entry.type.equals("we", true)) "world's end" else entry.difficulty)
    val accentBrush = scoreDifficultyBrush(entry, vertical = true)
    val model = remember(entry.imageName, jacketBaseUrl) {
        localJacketPath(entry.imageName)?.let(::File)
            ?: jacketBaseUrl.trimEnd('/').takeIf { it.isNotBlank() }?.let { it + "/" + entry.imageName.trimStart('/') }
    }
    Row(
        Modifier.fillMaxWidth().height(78.dp)
            .squircleSurface(MiuixTheme.colorScheme.surfaceContainer, 14.dp, extension = SquircleExtension)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(2.dp))
                .then(
                    if (accentBrush != null) {
                        Modifier.background(accentBrush)
                    } else {
                        Modifier.squircleSurface(accent, 2.dp, extension = SquircleExtension)
                    },
                ),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(10.dp))) {
            if (model != null) AsyncImage(model, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(entry.title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().basicMarquee())
            Text(
                String.format(Locale.ROOT, "%,d", entry.score),
                style = MiuixTheme.textStyles.footnote2.copy(fontFamily = FontFamily.Monospace),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                ScoreTintBadge(entry.difficulty.uppercase(Locale.ROOT), accent, brush = scoreDifficultyTintBrush(entry))
                displayFullCombo(entry.fullCombo)?.let { ScoreTintBadge(it, comboColor(it)) }
                displayFullChain(entry.fullChain)?.let { ScoreTintBadge(it, chainColor(it), compact = true) }
            }
        }
        Column(Modifier.padding(end = 12.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.rank, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Black, color = rankColor(entry.rank))
            Text(String.format(Locale.ROOT, "%.2f", entry.rating), style = MiuixTheme.textStyles.footnote2.copy(fontFamily = FontFamily.Monospace), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

@Composable
private fun ScoreTintBadge(text: String, color: Color, compact: Boolean = false, brush: Brush? = null) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2.copy(fontSize = if (compact) 8.sp else 9.sp, fontWeight = FontWeight.Bold),
        color = color,
        maxLines = 1,
        modifier = Modifier
            .then(
                if (brush != null) {
                    Modifier.clip(squircleShape(4.dp)).background(brush)
                } else {
                    Modifier.squircleSurface(color.copy(alpha = 0.15f), 4.dp, extension = SquircleExtension)
                },
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

private fun scoreDifficultyBrush(entry: ScoreQueryEntry, alpha: Float = 1f, vertical: Boolean = false): Brush? = when {
    entry.type.equals("we", ignoreCase = true) -> {
        val colors = WORLDS_END_GRADIENT_COLORS.map { it.copy(alpha = alpha) }
        if (vertical) Brush.verticalGradient(colors) else Brush.horizontalGradient(colors)
    }
    entry.difficulty.equals("ultima", ignoreCase = true) -> ultimaStripedBrush(alpha)
    else -> null
}

private fun scoreDifficultyTintBrush(entry: ScoreQueryEntry): Brush? = scoreDifficultyBrush(entry, alpha = 0.15f)

@Composable
private fun ScoreQueryEmpty() {
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Text("没有符合条件的成绩", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun ScoreQueryFilterSheet(
    visible: Boolean,
    settings: ScoreQueryFilterSettings,
    onSettingsChange: (ScoreQueryFilterSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ExpandableBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        expandActionLabel = "展开",
        collapseActionLabel = "收起到半屏",
        expandedStateDescription = "已全屏展开",
        halfExpandedStateDescription = "半屏",
        header = {
            IconButton(onClick = { onSettingsChange(ScoreQueryFilterSettings()) }, modifier = Modifier.align(Alignment.CenterStart)) { Icon(Icons.Rounded.RestartAlt, contentDescription = "重置筛选") }
            Text("筛选", style = MiuixTheme.textStyles.title3, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Rounded.Check, contentDescription = "完成", tint = MiuixTheme.colorScheme.primary) }
        },
    ) { topInset ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, top = topInset + 12.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { ScoreFilterGroup("难度", DifficultyOptions, settings.difficulties, ::difficultyChipColor, uppercaseOptions = true) { onSettingsChange(settings.copy(difficulties = settings.difficulties.toggle(it))) } }
            item { ScoreFilterGroup("段位", RankOptions, settings.ranks, ::rankColor) { onSettingsChange(settings.copy(ranks = settings.ranks.toggle(it))) } }
            item { ScoreFilterGroup("Full Combo", ComboOptions, settings.fullCombos, ::comboColor) { onSettingsChange(settings.copy(fullCombos = settings.fullCombos.toggle(it))) } }
            item { ScoreFilterGroup("Full Chain", ChainOptions, settings.fullChains, ::chainColor) { onSettingsChange(settings.copy(fullChains = settings.fullChains.toggle(it))) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoreFilterGroup(
    title: String,
    options: List<String>,
    selected: Set<String>,
    colorFor: @Composable (String) -> Color,
    uppercaseOptions: Boolean = false,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(start = 4.dp))
        Card(Modifier.fillMaxWidth(), cornerRadius = 16.dp, insideMargin = PaddingValues(14.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                options.forEach { option ->
                    ScoreFilterChip(
                        title = if (uppercaseOptions) option.uppercase(Locale.ROOT) else option,
                        selected = option in selected,
                        color = colorFor(option),
                        rainbow = option.equals("world's end", ignoreCase = true),
                        striped = option.equals("ultima", ignoreCase = true),
                        onClick = { onToggle(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreFilterChip(
    title: String,
    selected: Boolean,
    color: Color,
    rainbow: Boolean,
    striped: Boolean,
    onClick: () -> Unit,
) {
    val background = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val borderColor = if (selected) color.copy(alpha = 0.55f) else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val contentColor = if (selected) Color.White else MiuixTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .then(
                if (selected && rainbow) {
                    Modifier.clip(squircleShape(50.dp)).background(Brush.horizontalGradient(WORLDS_END_GRADIENT_COLORS))
                } else if (selected && striped) {
                    Modifier.clip(squircleShape(50.dp)).background(ultimaStripedBrush())
                } else {
                    Modifier.squircleSurface(if (selected) color else background, 50.dp, extension = SquircleExtension)
                },
            )
            .border(1.dp, borderColor, RoundedCornerShape(50.dp))
            .toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = MiuixTheme.textStyles.footnote1, color = contentColor, maxLines = 1)
    }
}

@Composable
fun ScoreQueryHomeCard(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier, onClick: () -> Unit) {
    Card(modifier.height(140.dp), cornerRadius = 16.dp, insideMargin = PaddingValues(16.dp), colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer), onClick = onClick) {
        Icon(Icons.AutoMirrored.Rounded.ViewList, contentDescription = null, modifier = Modifier.size(30.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("成绩查询", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
            Text("查询歌曲成绩", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 2)
        }
    }
}

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value
private val DifficultyOptions = listOf("basic", "advanced", "expert", "master", "ultima", "world's end")
private val RankOptions = listOf("SSS+", "SSS", "SS+", "SS", "S+", "S", "AAA", "AA", "A", "BBB", "BB", "B", "C", "D")
private val ComboOptions = listOf("FC", "AJ", "AJC")
private val ChainOptions = listOf("铂 FC", "金 FC")

@Composable private fun difficultyChipColor(value: String): Color = difficultyColor(value)
@Composable private fun rankColor(value: String): Color = when (value) {
    "SSS+", "SSS" -> Color(0xFFFFD900)
    "SS+", "SS" -> Color(0xFFFFB300)
    "S+", "S" -> Color(0xFFFF8F00)
    "AAA" -> Color(0xFFB66DDB)
    "AA" -> Color(0xFF5B9BD5)
    "A" -> Color(0xFF5DAE68)
    else -> MiuixTheme.colorScheme.primary
}
@Composable private fun comboColor(value: String): Color = when (value) {
    "AJC" -> Color(0xFFE2B93B)
    "AJ" -> Color(0xFFF0A64A)
    else -> Color(0xFF56A6D9)
}
@Composable private fun chainColor(value: String): Color = if (value.startsWith("金")) Color(0xFFD5A62A) else Color(0xFF9EACBE)
