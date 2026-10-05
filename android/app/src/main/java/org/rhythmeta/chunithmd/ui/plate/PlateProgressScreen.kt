package org.rhythmeta.chunithmd.ui.plate

import org.rhythmeta.chunithmd.ui.catalog.rememberSongCoverNavigation
import org.rhythmeta.chunithmd.ui.catalog.SongCoverDecorations

import org.rhythmeta.chunithmd.shared.localization.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.shared.PlateChartEntry
import org.rhythmeta.chunithmd.shared.PlateLevelSection
import org.rhythmeta.chunithmd.shared.PlateProgressCalculator
import org.rhythmeta.chunithmd.shared.PlateProgressResponse
import org.rhythmeta.chunithmd.shared.PlateType
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.ui.catalog.difficultyColor
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import org.rhythmeta.chunithmd.ui.components.squircleShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PlateProgressScreen(
    bundle: CatalogBundle?,
    activeServer: ProfileServer,
    records: List<ScoreRecord>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    contentTopPadding: Dp,
    topBarScrollConnection: NestedScrollConnection,
    onOpenSong: (String) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val preferencesRepository = remember(context) { PlateProgressPreferencesRepository(context) }
    val preferences by preferencesRepository.preferences.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val selectedVersion = preferences?.selectedVersion
    val plateType = preferences?.plateType ?: PlateType.Spirit
    val difficulty = preferences?.difficulty
    val remainingOnly = preferences?.remainingOnly ?: false
    val preferencesLoaded = preferences != null
    val listState = rememberLazyListState()
    val response by produceState<PlateProgressResponse?>(null, bundle, records, activeServer, selectedVersion, plateType, preferencesLoaded) {
        value = null
        if (!preferencesLoaded) return@produceState
        value = bundle?.let {
            withContext(Dispatchers.Default) {
                PlateProgressCalculator.calculate(it, records, activeServer, selectedVersion, plateType)
            }
        }
    }
    val loaded = response
    val sections = remember(loaded, difficulty, remainingOnly) {
        loaded?.sections(difficulty, remainingOnly).orEmpty()
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().nestedScroll(topBarScrollConnection),
        contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 12.dp, end = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            bundle == null -> item("unavailable") {
                PlateMessage(tr("暂无歌曲目录"), tr("请先在静态数据中下载歌曲目录。"))
            }
            loaded == null -> item("loading") {
                Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            loaded.groups.isEmpty() -> item("empty") {
                PlateMessage(tr("暂无牌子进度"), tr("当前服务器没有可用的 BASIC～MASTER 谱面。"))
            }
            else -> {
                item("summary") { PlateSummary(loaded, difficulty) }
                item("filters") {
                    Card(
                        modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp,
                        insideMargin = PaddingValues(0.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                    ) {
                        Column {
                            WindowDropdownPreference(
                                title = tr("版本"), items = loaded.groups.map { it.name },
                                selectedIndex = loaded.groups.indexOf(loaded.selectedGroup).coerceAtLeast(0),
                                onSelectedIndexChange = { index -> scope.launch { preferencesRepository.setVersion(loaded.groups[index].version) } },
                            )
                            WindowDropdownPreference(
                                title = tr("牌子类型"), items = PlateType.entries.map { "${it.title} · ${it.requirement}" },
                                selectedIndex = PlateType.entries.indexOf(plateType),
                                onSelectedIndexChange = { index -> scope.launch { preferencesRepository.setPlateType(PlateType.entries[index]) } },
                            )
                            WindowDropdownPreference(
                                title = tr("展示难度"), items = listOf(tr("全部难度")) + PlateProgressCalculator.difficulties.map { it.uppercase() },
                                selectedIndex = difficulty?.let { PlateProgressCalculator.difficulties.indexOf(it) + 1 } ?: 0,
                                onSelectedIndexChange = { index -> scope.launch { preferencesRepository.setDifficulty(PlateProgressCalculator.difficulties.getOrNull(index - 1)) } },
                            )
                            SwitchPreference(
                                title = tr("只看未完成"), checked = remainingOnly,
                                onCheckedChange = { value -> scope.launch { preferencesRepository.setRemainingOnly(value) } },
                            )
                        }
                    }
                }
                if (sections.isEmpty()) {
                    item("no-charts") {
                        PlateMessage(tr("没有符合条件的谱面"), if (remainingOnly) tr("当前展示范围内没有未完成谱面。") else tr("试试切换展示难度。"))
                    }
                }
                sections.forEach { section ->
                    item("level-${section.level}") { PlateSectionHeader(section, loaded.plateType) }
                    // Keep individual rows lazy even when the full version contains hundreds of charts.
                    section.charts.chunked(5).forEach { charts ->
                        item("row-${charts.first().sheetKey}") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                charts.forEach { chart ->
                                    Box(Modifier.weight(1f)) {
                                        PlateJacket(
                                            chart = chart,
                                            plateType = loaded.plateType,
                                            showDifficultyBorder = difficulty == null,
                                            jacketBaseUrl = jacketBaseUrl,
                                            localJacketPath = localJacketPath,
                                            onClick = { onOpenSong(chart.song.songId) },
                                        )
                                    }
                                }
                                repeat(5 - charts.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlateSummary(response: PlateProgressResponse, difficulty: String?) {
    val accent = plateColor(response.plateType)
    val charts = remember(response.charts, difficulty) {
        response.charts.filter { difficulty == null || it.sheet.difficulty.equals(difficulty, ignoreCase = true) }
    }
    val completedCount = charts.count { it.achieved }
    Card(
        modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(response.title, modifier = Modifier.weight(1f), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
                Text(
                    "${(response.progress * 100).toInt()}%",
                    style = MiuixTheme.textStyles.title4, color = accent, fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .squircleSurface(accent.copy(alpha = 0.12f), 12.dp, extension = SquircleExtension)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            PlateProgressBar(response.progress, accent)
            Row(Modifier.fillMaxWidth()) {
                PlateMetric(tr("已完成"), completedCount, Modifier.weight(1f))
                PlateMetric(tr("未完成"), charts.size - completedCount, Modifier.weight(1f))
                PlateMetric(difficulty?.let { tr("{0} 谱面", it.uppercase()) } ?: tr("总谱面"), charts.size, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlateMetric(label: String, value: Int, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Text(value.toString(), style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlateProgressBar(progress: Float, accent: Color) {
    LinearProgressIndicator(
        progress = progress,
        colors = ProgressIndicatorDefaults.progressIndicatorColors(foregroundColor = accent, backgroundColor = accent.copy(alpha = 0.14f)),
    )
}

@Composable
private fun PlateSectionHeader(section: PlateLevelSection, plateType: PlateType) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lv. ${section.level}", style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold)
            Text(
                tr("{0} / {1} 已完成", section.completedCount, section.charts.size),
                modifier = Modifier.padding(start = 10.dp), style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        PlateProgressBar(section.completedCount.toFloat() / section.charts.size, plateColor(plateType))
    }
}

@Composable
private fun PlateJacket(
    chart: PlateChartEntry,
    plateType: PlateType,
    showDifficultyBorder: Boolean,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onClick: () -> Unit,
) {
    val coverNavigation = rememberSongCoverNavigation(chart.song.songId, onClick, 9.dp, key = chart,
        saturation = if (chart.achieved) 1f else 0.08f, imageAlpha = if (chart.achieved) 1f else 0.55f, cardColor = MiuixTheme.colorScheme.surfaceVariant)
    val accent = plateColor(plateType)
    val title = CatalogSongFormatter.displayTitle(chart.song)
    val status = if (chart.achieved) tr("已完成") else tr("未完成")
    val imageModel = remember(chart.song.imageName, jacketBaseUrl, localJacketPath) {
        localJacketPath(chart.song.imageName)?.let(::File)
            ?: jacketBaseUrl.trimEnd('/').takeIf { it.isNotBlank() }?.let { "$it/${chart.song.imageName.trimStart('/')}" }
    }
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).then(coverNavigation.modifier).clip(squircleShape(9.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = "$title ${chart.sheet.difficulty.uppercase()} $status" }
            .clickable(onClick = coverNavigation::open),
    ) {
        AsyncImage(
            placeholder = coverNavigation.source.painter, error = coverNavigation.source.painter,
            onSuccess = { coverNavigation.onPainter(it.painter) },
            model = imageModel, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            colorFilter = if (chart.achieved) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.08f) }),
            alpha = if (chart.achieved) 1f else 0.55f,
        )
        SongCoverDecorations(coverNavigation) {
            if (chart.achieved) {
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(24.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.78f)))),
                )
            }
            chart.achievementLabel?.let { label ->
                Text(
                    label, color = Color.White, fontWeight = FontWeight.Bold,
                    style = MiuixTheme.textStyles.footnote2.copy(fontSize = 9.sp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)
                        .squircleSurface(achievementColor(label).copy(alpha = 0.96f), 4.dp).padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            if (showDifficultyBorder || chart.achieved) {
                Box(
                    Modifier.fillMaxSize().squircleBorder(
                        width = if (chart.achieved) 2.dp else 1.dp,
                        color = if (showDifficultyBorder) difficultyColor(chart.sheet.difficulty) else accent,
                        cornerRadius = 9.dp,
                        extension = SquircleExtension,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PlateMessage(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MiuixTheme.textStyles.title3)
        Text(message, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

private fun plateColor(type: PlateType): Color = when (type) {
    PlateType.Spirit -> Color(0xFF39A66B)
    PlateType.Tribute -> Color(0xFFD49A2B)
    PlateType.Legend -> Color(0xFFE45D7B)
}

private fun achievementColor(label: String): Color = when (label) {
    "SSS", "SSS+", "AJ", "AJC" -> Color(0xFFE2B93B)
    else -> Color(0xFFFF8F00)
}

@Composable
fun PlateProgressHomeCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(140.dp), cornerRadius = 16.dp, insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer), onClick = onClick,
    ) {
        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(30.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        Spacer(Modifier.height(12.dp))
        Text(tr("牌子进度"), style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
        Text(tr("查看各版本牌子达成情况"), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
