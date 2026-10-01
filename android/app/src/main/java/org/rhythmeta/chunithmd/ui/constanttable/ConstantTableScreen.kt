package org.rhythmeta.chunithmd.ui.constanttable

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogFilters
import org.rhythmeta.chunithmd.shared.ConstantTableEntry
import org.rhythmeta.chunithmd.shared.ConstantTableResponse
import org.rhythmeta.chunithmd.shared.ConstantTableSection
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.buildConstantTableResponse
import org.rhythmeta.chunithmd.shared.constantTableBaseLevelLabel
import org.rhythmeta.chunithmd.shared.filterConstantTableEntries
import org.rhythmeta.chunithmd.ui.catalog.WORLDS_END_GRADIENT_COLORS
import org.rhythmeta.chunithmd.ui.catalog.difficultyColor
import org.rhythmeta.chunithmd.ui.catalog.ultimaStripedBrush
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import org.rhythmeta.chunithmd.ui.components.squircleShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConstantTableScreen(
    bundle: CatalogBundle?,
    activeServer: ProfileServer,
    records: List<ScoreRecord>,
    favoriteSongIds: Set<String>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    contentTopPadding: Dp,
    topBarScrollConnection: NestedScrollConnection,
    filterSettings: CatalogFilters,
    filterOpen: Boolean,
    exportRequested: Boolean,
    onFilterSettingsChange: (CatalogFilters) -> Unit,
    onFilterDismiss: () -> Unit,
    onExportRequestHandled: () -> Unit,
    onOpenSong: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkTheme = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val response by produceState<ConstantTableResponse?>(null, bundle, records, activeServer, favoriteSongIds) {
        value = bundle?.let { catalog ->
            withContext(Dispatchers.Default) {
                buildConstantTableResponse(catalog, records, activeServer, favoriteSongIds)
            }
        }
    }
    var includeScores by rememberSaveable { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var requestedBaseLevel by remember { mutableStateOf<Int?>(null) }
    val filteredEntries = remember(response, filterSettings) {
        response?.let { filterConstantTableEntries(it.entries, filterSettings) }.orEmpty()
    }
    val filteredResponse = remember(filteredEntries) { ConstantTableResponse(filteredEntries) }
    val availableBaseLevels = filteredResponse.availableBaseLevels
    val selectedBaseLevel = requestedBaseLevel
        ?.takeIf(availableBaseLevels::contains)
        ?: 15.takeIf(availableBaseLevels::contains)
        ?: availableBaseLevels.firstOrNull()
    val sections = remember(filteredResponse, selectedBaseLevel) {
        selectedBaseLevel?.let(filteredResponse::sections).orEmpty()
    }
    val loadedResponse = response

    androidx.compose.runtime.LaunchedEffect(exportRequested, sections, selectedBaseLevel, includeScores) {
        if (!exportRequested || selectedBaseLevel == null || sections.isEmpty()) return@LaunchedEffect
        shareConstantTable(
            context = context,
            baseLevelLabel = constantTableBaseLevelLabel(selectedBaseLevel),
            sections = sections,
            includeScores = includeScores,
            localJacketPath = localJacketPath,
            darkTheme = darkTheme,
        )
        onExportRequestHandled()
    }

    if (response == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(topBarScrollConnection),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 12.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (availableBaseLevels.isEmpty()) {
                item(key = "empty") { ConstantTableEmpty() }
            } else {
                item(key = "settings") {
                    ConstantTableSettings(
                        availableBaseLevels = availableBaseLevels,
                        selectedBaseLevel = selectedBaseLevel,
                        includeScores = includeScores,
                        onSelectBaseLevel = { requestedBaseLevel = it },
                        onIncludeScoresChange = { includeScores = it },
                    )
                }
                item(key = "summary") {
                    ConstantTableSummary(
                        chartCount = sections.sumOf { it.entries.size },
                        sectionCount = sections.size,
                        filtered = loadedResponse != null && filteredEntries.size != loadedResponse.entries.size,
                    )
                }
                item(key = "export") {
                    Button(
                        onClick = {
                            val level = selectedBaseLevel ?: return@Button
                            if (isExporting) return@Button
                            isExporting = true
                            scope.launch {
                                shareConstantTable(
                                    context = context,
                                    baseLevelLabel = constantTableBaseLevelLabel(level),
                                    sections = sections,
                                    includeScores = includeScores,
                                    localJacketPath = localJacketPath,
                                    darkTheme = darkTheme,
                                )
                                isExporting = false
                            }
                        },
                        enabled = sections.isNotEmpty() && !isExporting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        insideMargin = PaddingValues(vertical = 14.dp),
                    ) {
                        if (isExporting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(if (isExporting) "正在生成图片" else "导出定数表图片", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                sections.forEachIndexed { index, section ->
                    item(key = "section-${section.constantLabel}") {
                        ConstantTableSectionView(
                            section = section,
                            index = index,
                            includeScores = includeScores,
                            jacketBaseUrl = jacketBaseUrl,
                            localJacketPath = localJacketPath,
                            onOpenSong = onOpenSong,
                        )
                    }
                }
            }
        }
    }
    ConstantTableFilterSheet(
        visible = filterOpen,
        bundle = bundle,
        settings = filterSettings,
        onSettingsChange = onFilterSettingsChange,
        onDismiss = onFilterDismiss,
    )
}

@Composable
fun ConstantTableToolbarActions(
    filterActive: Boolean,
    onFilter: () -> Unit,
) {
    IconButton(onClick = onFilter) {
        Icon(
            Icons.Rounded.FilterList,
            contentDescription = "筛选",
            tint = if (filterActive) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ConstantTableSettings(
    availableBaseLevels: List<Int>,
    selectedBaseLevel: Int?,
    includeScores: Boolean,
    onSelectBaseLevel: (Int) -> Unit,
    onIncludeScoresChange: (Boolean) -> Unit,
) {
    SmallTitle(text = "定数范围", insideMargin = PaddingValues(horizontal = 4.dp, vertical = 6.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Column {
            WindowDropdownPreference(
                items = availableBaseLevels.map(::constantTableBaseLevelLabel),
                selectedIndex = availableBaseLevels.indexOf(selectedBaseLevel).coerceAtLeast(0),
                title = "定数档位",
                onSelectedIndexChange = { index -> availableBaseLevels.getOrNull(index)?.let(onSelectBaseLevel) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("显示成绩徽标", style = MiuixTheme.textStyles.body1)
                    Text("在曲绘上显示 Rank、FC/AJ/AJC 和 FULL CHAIN", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                Switch(checked = includeScores, onCheckedChange = onIncludeScoresChange)
            }
        }
    }
}

@Composable
private fun ConstantTableSummary(chartCount: Int, sectionCount: Int, filtered: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("普通谱面定数表", style = MiuixTheme.textStyles.body1)
                Text("WE 谱面不纳入统计", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Text(
                text = "$chartCount 张谱面 · $sectionCount 个定数${if (filtered) " · 已筛选" else ""}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

@Composable
private fun ConstantTableSectionView(
    section: ConstantTableSection,
    index: Int,
    includeScores: Boolean,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onOpenSong: (String) -> Unit,
) {
    val levelColor = constantLevelColor(section.constantLabel, index)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(section.constantLabel, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, color = levelColor)
            Text("${section.entries.size} 张谱面", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(start = 8.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .squircleSurface(
                    MiuixTheme.colorScheme.surfaceContainer.copy(alpha = if (index % 2 == 0) 0.82f else 0.62f),
                    14.dp,
                    extension = SquircleExtension,
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            section.entries.chunked(5).forEach { rowEntries ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowEntries.forEach { entry ->
                        Box(Modifier.weight(1f)) {
                            ConstantTableJacket(entry, includeScores, jacketBaseUrl, localJacketPath) { onOpenSong(entry.songId) }
                        }
                    }
                    repeat(5 - rowEntries.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ConstantTableJacket(
    entry: ConstantTableEntry,
    includeScores: Boolean,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onClick: () -> Unit,
) {
    val accent = difficultyColor(if (entry.type.equals("we", true)) "world's end" else entry.difficulty)
    val brush = constantTableBrush(entry)
    val imageModel = remember(entry.imageName, jacketBaseUrl) {
        localJacketPath(entry.imageName)?.let(::File)
            ?: jacketBaseUrl.trimEnd('/').takeIf { it.isNotBlank() }?.let { "$it/${entry.imageName.trimStart('/')}" }
    }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(squircleShape(9.dp)).clickable(onClick = onClick),
    ) {
        if (imageModel != null) {
            AsyncImage(imageModel, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surfaceVariant))
        }
        if (includeScores) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                entry.rank?.let { ConstantTableBadge(it, rankColor(it)) }
                entry.fullCombo?.let { ConstantTableBadge(it, comboColor(it)) }
                entry.fullChain?.let { ConstantTableBadge(it, chainColor(it), compact = true) }
            }
        }
        Box(
            Modifier.fillMaxSize().then(
                if (brush != null) Modifier.border(1.5.dp, brush, RoundedCornerShape(9.dp))
                else Modifier.squircleBorder(1.5.dp, accent, 9.dp, extension = SquircleExtension),
            ),
        )
    }
}

@Composable
private fun ConstantTableBadge(text: String, color: Color, compact: Boolean = false) {
    Text(
        text = text,
        color = Color.White,
        style = MiuixTheme.textStyles.footnote2.copy(fontSize = if (compact) 6.sp else 7.sp),
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier.squircleSurface(color, 3.dp, extension = SquircleExtension).padding(horizontal = 3.dp, vertical = 1.dp),
    )
}

@Composable
private fun ConstantTableEmpty() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.GridView, contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.size(34.dp))
        Text("没有可用的定数数据", style = MiuixTheme.textStyles.title3)
        Text("请先更新歌曲目录，或调整筛选条件。", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun ConstantTableFilterSheet(
    visible: Boolean,
    bundle: CatalogBundle?,
    settings: CatalogFilters,
    onSettingsChange: (CatalogFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    if (bundle == null) return
    org.rhythmeta.chunithmd.ui.catalog.CatalogFilterDialog(
        show = visible,
        bundle = bundle,
        settings = settings,
        onSettingsChange = onSettingsChange,
        onDismiss = onDismiss,
        includeDifficultyAndType = false,
    )
}

private fun constantTableBrush(entry: ConstantTableEntry): Brush? = when {
    entry.type.equals("we", true) -> Brush.horizontalGradient(WORLDS_END_GRADIENT_COLORS)
    entry.difficulty.equals("ultima", true) -> ultimaStripedBrush()
    else -> null
}

private suspend fun shareConstantTable(
    context: Context,
    baseLevelLabel: String,
    sections: List<ConstantTableSection>,
    includeScores: Boolean,
    localJacketPath: (String) -> String?,
    darkTheme: Boolean,
) {
    val file = ConstantTableImageExporter.renderToCache(
        context = context,
        baseLevelLabel = baseLevelLabel,
        sections = sections,
        includeScores = includeScores,
        localJacketPath = localJacketPath,
        darkTheme = darkTheme,
    ) ?: return
    val uri = runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) }.getOrNull() ?: return
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "分享定数表",
        ),
    )
}

private fun rankColor(value: String): Color = when (value.uppercase(Locale.ROOT)) {
    "SSS+", "SSS" -> Color(0xFFFFD900)
    "SS+", "SS" -> Color(0xFFFFB300)
    "S+", "S" -> Color(0xFFFF8F00)
    "AAA" -> Color(0xFFB66DDB)
    "AA" -> Color(0xFF5B9BD5)
    "A" -> Color(0xFF5DAE68)
    else -> Color(0xFF8E8E93)
}

private fun comboColor(value: String): Color = when (value) {
    "AJC" -> Color(0xFFE2B93B)
    "AJ" -> Color(0xFFF0A64A)
    else -> Color(0xFF56A6D9)
}

private fun chainColor(value: String): Color = if (value.startsWith("金")) Color(0xFFD5A62A) else Color(0xFF9EACBE)

private fun constantLevelColor(label: String, index: Int): Color = when (((label.toDoubleOrNull() ?: 0.0) * 10).toInt() % 10) {
    0, 5 -> Color(0xFFD34A63)
    1, 6 -> Color(0xFF4D78FF)
    2, 7 -> Color(0xFF3F9B74)
    3, 8 -> Color(0xFFB45BFF)
    else -> if (index % 2 == 0) Color(0xFFC84A7B) else Color(0xFF5489FF)
}

@Composable
fun ConstantTableHomeCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(30.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        Spacer(Modifier.size(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("定数表", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
            Text("按定数查看全部谱面", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 2)
        }
    }
}
