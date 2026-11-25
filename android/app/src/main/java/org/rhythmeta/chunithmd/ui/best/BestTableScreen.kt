package org.rhythmeta.chunithmd.ui.best


import org.rhythmeta.chunithmd.shared.localization.tr

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.core.content.FileProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.rhythmeta.chunithmd.shared.calculateSingleRating
import kotlin.math.roundToInt
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogVersionFormatter
import org.rhythmeta.chunithmd.shared.BestTableEntry
import org.rhythmeta.chunithmd.shared.BestTablePreferences
import org.rhythmeta.chunithmd.shared.BestTableShareEntry
import org.rhythmeta.chunithmd.shared.ClearType
import org.rhythmeta.chunithmd.shared.FullChainType
import org.rhythmeta.chunithmd.shared.FullComboType
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.RatingChartEntry
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.buildBestTableEntries
import org.rhythmeta.chunithmd.shared.calculatePlayerRating
import org.rhythmeta.chunithmd.shared.latestPlayableVersion
import org.rhythmeta.chunithmd.ui.catalog.difficultyColor
import org.rhythmeta.chunithmd.ui.catalog.SongVisualUtils
import org.rhythmeta.chunithmd.ui.components.ScoreEntryBadge
import org.rhythmeta.chunithmd.ui.components.ScoreEntryCard
import org.rhythmeta.chunithmd.ui.components.rankColor
import org.rhythmeta.chunithmd.ui.components.clearStatusColor
import org.rhythmeta.chunithmd.ui.components.comboStatusColor
import org.rhythmeta.chunithmd.ui.components.chainStatusColor
import org.rhythmeta.chunithmd.ui.components.ExpandableBottomSheet
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import org.rhythmeta.chunithmd.ui.components.playerRatingColors
import org.rhythmeta.chunithmd.ui.components.ratingTierColor
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BestTableScreen(
    bundle: CatalogBundle?,
    activeServer: ProfileServer,
    records: List<ScoreRecord>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    contentTopPadding: Dp,
    topBarScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    selectedPage: Int = 0,
    switcherScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null,
    onOpenSong: (String) -> Unit,
    preferencesRepository: BestTablePreferencesRepository,
    profileName: String? = null,
    shareRequested: Boolean = false,
    onShareRequestHandled: () -> Unit = {},
    settingsRequested: Boolean = false,
    onSettingsRequestHandled: () -> Unit = {},
) {
    if (bundle == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(tr("正在加载歌曲目录"), style = MiuixTheme.textStyles.body1)
        }
        return
    }

    val context = LocalContext.current
    val darkTheme = SongVisualUtils.isDarkTheme(MiuixTheme.colorScheme.background)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val preferences by preferencesRepository.preferences.collectAsState(initial = BestTablePreferences())
    var bestCountText by rememberSaveable { mutableStateOf("30") }
    var newCountText by rememberSaveable { mutableStateOf("20") }
    var selectedVersion by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var difficultyFilter by remember { mutableStateOf(emptySet<String>()) }
    var rankFilter by remember { mutableStateOf(emptySet<String>()) }
    var categoryFilter by remember { mutableStateOf(emptySet<String>()) }
    var clearFilter by remember { mutableStateOf(emptySet<String>()) }
    var comboFilter by remember { mutableStateOf(emptySet<String>()) }
    var chainFilter by remember { mutableStateOf(emptySet<String>()) }
    var theoretical by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settingsRequested) {
        if (settingsRequested) {
            showSettings = true
            onSettingsRequestHandled()
        }
    }
    LaunchedEffect(preferences) {
        bestCountText = preferences.bestCount.toString()
        newCountText = preferences.newCount.toString()
        selectedVersion = preferences.selectedVersion
    }
    val bestCount = bestCountText.toIntOrNull()?.coerceIn(BestTablePreferences.MIN_COUNT, BestTablePreferences.MAX_COUNT) ?: preferences.bestCount
    val newCount = newCountText.toIntOrNull()?.coerceIn(BestTablePreferences.MIN_COUNT, BestTablePreferences.MAX_COUNT) ?: preferences.newCount
    val versionOptions = remember(bundle) {
        listOf<String?>(null) + bundle.catalog.versions.asReversed().map { it.version }.distinct()
    }
    val effectiveVersion = selectedVersion?.takeIf { it in versionOptions }
        ?: bundle.latestPlayableVersion(activeServer)
    val versionLabels = versionOptions.map { version ->
        version?.let(CatalogVersionFormatter::badge) ?: tr("自动")
    }
    val entries by produceState<List<BestTableEntry>?>(
        null,
        bundle,
        records,
        activeServer,
        selectedVersion,
        theoretical,
    ) {
        value = withContext(Dispatchers.Default) {
            buildBestTableEntries(bundle, records, activeServer, selectedVersion, theoretical)
        }
    }
    val loadedEntries = entries
    if (loadedEntries == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(tr("正在计算 Best 表"), style = MiuixTheme.textStyles.body1)
        }
        return
    }
    val difficulties = remember(loadedEntries) {
        val present = loadedEntries.map { it.difficulty.lowercase() }.toSet()
        DifficultyFilterOrder + (present - DifficultyFilterOrder.toSet()).sorted()
    }
    val ranks = remember(loadedEntries) {
        loadedEntries.map { it.rank.uppercase() }.distinct()
            .sortedBy { rank -> RankFilterOrder.indexOf(rank).takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }
    val categories = remember(bundle) {
        bundle.catalog.songs.mapNotNull { song -> song.category.takeIf(String::isNotBlank) }.distinct().sorted()
    }
    val songCategories = remember(bundle) { bundle.catalog.songs.associate { it.songId to it.category } }
    // Offer every status the game defines, strongest first, instead of only the ones present.
    val clears = remember { ClearType.entries.map(ClearType::wireValue) }
    val combos = remember { FullComboType.entries.map(FullComboType::wireValue) }
    val chains = remember { listOf(FullChainType.FullChain2, FullChainType.FullChain).map(FullChainType::wireValue) }
    val filteredEntries = remember(
        loadedEntries,
        difficultyFilter,
        categoryFilter,
        rankFilter,
        clearFilter,
        comboFilter,
        chainFilter,
    ) {
        if (difficultyFilter.isEmpty() && categoryFilter.isEmpty() && rankFilter.isEmpty() &&
            clearFilter.isEmpty() && comboFilter.isEmpty() && chainFilter.isEmpty()
        ) {
            loadedEntries
        } else {
            loadedEntries.filter { entry ->
                (difficultyFilter.isEmpty() || entry.difficulty.lowercase() in difficultyFilter) &&
                    (categoryFilter.isEmpty() || songCategories[entry.songId] in categoryFilter) &&
                    (rankFilter.isEmpty() || entry.rank.uppercase() in rankFilter) &&
                    (clearFilter.isEmpty() || entry.clear in clearFilter) &&
                    (comboFilter.isEmpty() || entry.fullCombo in comboFilter) &&
                    (chainFilter.isEmpty() || entry.fullChain in chainFilter)
            }
        }
    }
    val summary = remember(filteredEntries, bestCount, newCount) {
        calculatePlayerRating(filteredEntries.map { entry ->
            RatingChartEntry(entry.chartId, entry.songId, entry.rating, entry.isNew)
        }, bestSlotCount = bestCount, newSlotCount = newCount)
    }
    val entriesById = remember(filteredEntries) { filteredEntries.associateBy(BestTableEntry::chartId) }
    val bestEntries = summary.best30.mapNotNull { entriesById[it.chartId] }
    val newEntries = summary.new20.mapNotNull { entriesById[it.chartId] }
    val listState = rememberLazyListState()

    LaunchedEffect(shareRequested, summary, effectiveVersion, bestCount, newCount) {
        if (!shareRequested) return@LaunchedEffect
        val shareEntries = { source: List<BestTableEntry> ->
            source.map { entry ->
                BestTableShareEntry(
                    title = entry.title,
                    difficulty = entry.difficulty,
                    type = entry.type,
                    score = entry.score,
                    rank = entry.rank,
                    rating = entry.rating,
                    level = formatLevel(entry.constant),
                    jacketPath = localJacketPath(entry.imageName),
                    clear = entry.clear,
                    fullCombo = entry.fullCombo,
                    fullChain = entry.fullChain,
                )
            }
        }
        val file = BestTableImageExporter.renderToCache(
            context = context,
            bestEntries = shareEntries(bestEntries),
            newEntries = shareEntries(newEntries),
            rating = summary.rating,
            bestAverage = summary.best30Average,
            newAverage = summary.new20Average,
            bestCount = bestCount,
            newCount = newCount,
            version = effectiveVersion?.let(CatalogVersionFormatter::badge) ?: tr("自动"),
            userName = profileName,
            darkTheme = darkTheme,
        )
        val uri = file?.let { runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) }.getOrNull() }
        if (uri != null) {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    tr("分享 Best 50"),
                ),
            )
        }
        onShareRequestHandled()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topBarScrollConnection)
            .then(switcherScrollConnection?.let(Modifier::nestedScroll) ?: Modifier),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 12.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BestRatingSummary(summary = summary, bestEntries = bestEntries, newEntries = newEntries)
        }
        if (selectedPage == 0) {
            bestTableSection(
                sectionKey = "new",
                title = "N$newCount",
                entries = newEntries,
                jacketBaseUrl = jacketBaseUrl,
                localJacketPath = localJacketPath,
                onOpenSong = onOpenSong,
            )
        } else {
            bestTableSection(
                sectionKey = "best",
                title = "B$bestCount",
                entries = bestEntries,
                jacketBaseUrl = jacketBaseUrl,
                localJacketPath = localJacketPath,
                onOpenSong = onOpenSong,
            )
        }
    }

    if (showSettings) {
        ExpandableBottomSheet(
            visible = showSettings,
            onDismissRequest = { showSettings = false },
            expandActionLabel = tr("展开"),
            collapseActionLabel = tr("收起到半屏"),
            expandedStateDescription = tr("已全屏展开"),
            halfExpandedStateDescription = tr("半屏"),
            header = {
                IconButton(
                    onClick = {
                        difficultyFilter = emptySet()
                        rankFilter = emptySet()
                        categoryFilter = emptySet()
                        clearFilter = emptySet()
                        comboFilter = emptySet()
                        chainFilter = emptySet()
                        theoretical = false
                        selectedVersion = null
                        bestCountText = BestTablePreferences.DEFAULT_BEST_COUNT.toString()
                        newCountText = BestTablePreferences.DEFAULT_NEW_COUNT.toString()
                        scope.launch {
                            preferencesRepository.setVersion(null)
                            preferencesRepository.setCapacity(
                                BestTablePreferences.DEFAULT_BEST_COUNT,
                                BestTablePreferences.DEFAULT_NEW_COUNT,
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = tr("重置"))
                }
                Text(
                    text = tr("Best 表设置"),
                    style = MiuixTheme.textStyles.title3,
                    modifier = Modifier.align(Alignment.Center),
                    maxLines = 1,
                )
                IconButton(
                    onClick = { showSettings = false },
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = tr("完成"), tint = MiuixTheme.colorScheme.primary)
                }
            },
        ) { topInset ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = topInset + 12.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        tr("理论 B50"),
                                        style = MiuixTheme.textStyles.body1,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        tr("显示当前版本的理论最高 Rating"),
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                                top.yukonga.miuix.kmp.basic.Switch(
                                    checked = theoretical,
                                    onCheckedChange = { theoretical = it },
                                )
                            }
                            WindowDropdownPreference(
                                items = versionLabels,
                                selectedIndex = versionOptions.indexOf(selectedVersion).coerceAtLeast(0),
                                title = tr("游戏版本"),
                                summary = selectedVersion?.let { tr("临时覆盖") },
                                onSelectedIndexChange = { index ->
                                    selectedVersion = versionOptions[index]
                                    scope.launch { preferencesRepository.setVersion(selectedVersion) }
                                },
                            )
                            if (!theoretical) Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        tr("容量设置"),
                                        fontSize = MiuixTheme.textStyles.headline1.fontSize,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        tr("当前总计 {0} 首", bestCount + newCount),
                                        style = MiuixTheme.textStyles.footnote1,
                                        fontWeight = FontWeight.Bold,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                                BestCapacitySlider(
                                    label = tr("旧曲"),
                                    value = bestCount,
                                    snapValue = BestTablePreferences.DEFAULT_BEST_COUNT,
                                    onValueChange = { bestCountText = it.toString() },
                                    onCommit = { scope.launch { preferencesRepository.setCapacity(bestCount, newCount) } },
                                )
                                BestCapacitySlider(
                                    label = tr("新曲"),
                                    value = newCount,
                                    snapValue = BestTablePreferences.DEFAULT_NEW_COUNT,
                                    onValueChange = { newCountText = it.toString() },
                                    onCommit = { scope.launch { preferencesRepository.setCapacity(bestCount, newCount) } },
                                )
                            }
                        }
                    }
                }
                if (!theoretical) {
                item {
                    BestSettingsSection(tr("难度等级")) {
                        BestFilterChips(
                            values = difficulties,
                            selected = difficultyFilter,
                            display = { value -> value.replaceFirstChar { it.uppercase() } },
                            color = { value -> difficultyColor(value) },
                            onToggle = { value ->
                                difficultyFilter = if (value in difficultyFilter) difficultyFilter - value else difficultyFilter + value
                            },
                        )
                    }
                }
                if (categories.isNotEmpty()) item {
                    BestSettingsSection(tr("分类")) {
                        BestFilterChips(
                            values = categories,
                            selected = categoryFilter,
                            display = { value -> value },
                            color = { MiuixTheme.colorScheme.primary },
                            onToggle = { value ->
                                categoryFilter = if (value in categoryFilter) categoryFilter - value else categoryFilter + value
                            },
                        )
                    }
                }
                item {
                    BestSettingsSection(tr("Rank")) {
                        BestFilterChips(
                            values = ranks,
                            selected = rankFilter,
                            display = { value -> value },
                            color = { value -> rankColor(value).takeUnless { it == Color.Unspecified } ?: MiuixTheme.colorScheme.onSurfaceVariantSummary },
                            onToggle = { value ->
                                rankFilter = if (value in rankFilter) rankFilter - value else rankFilter + value
                            },
                        )
                    }
                }
                if (clears.isNotEmpty()) item {
                    BestSettingsSection(tr("CLEAR")) {
                        BestFilterChips(
                            values = clears,
                            selected = clearFilter,
                            display = { value -> ClearType.displayName(value) ?: value },
                            color = { value -> clearStatusColor(value) },
                            onToggle = { value ->
                                clearFilter = if (value in clearFilter) clearFilter - value else clearFilter + value
                            },
                        )
                    }
                }
                if (combos.isNotEmpty()) item {
                    BestSettingsSection(tr("FULL COMBO")) {
                        BestFilterChips(
                            values = combos,
                            selected = comboFilter,
                            display = { value -> FullComboType.displayName(value) ?: value },
                            color = { value -> comboStatusColor(value) },
                            onToggle = { value ->
                                comboFilter = if (value in comboFilter) comboFilter - value else comboFilter + value
                            },
                        )
                    }
                }
                if (chains.isNotEmpty()) item {
                    BestSettingsSection(tr("FULL CHAIN")) {
                        BestFilterChips(
                            values = chains,
                            selected = chainFilter,
                            display = { value -> FullChainType.displayName(value) ?: value },
                            color = { value -> chainStatusColor(value) },
                            onToggle = { value ->
                                chainFilter = if (value in chainFilter) chainFilter - value else chainFilter + value
                            },
                        )
                    }
                }
                }
            }
        }
    }
}

private val DifficultyFilterOrder = listOf("basic", "advanced", "expert", "master", "ultima")

private val RankFilterOrder = listOf("SSS+", "SSS", "SS+", "SS", "S+", "S", "AAA", "AA", "A", "BBB", "BB", "B", "C", "D")

@Composable
private fun BestCapacitySlider(
    label: String,
    value: Int,
    snapValue: Int,
    onValueChange: (Int) -> Unit,
    onCommit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.weight(1f),
            )
            Text(
                tr("{0} 首", value),
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
        top.yukonga.miuix.kmp.basic.Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            onValueChangeFinished = onCommit,
            valueRange = BestTablePreferences.MIN_COUNT.toFloat()..BestTablePreferences.MAX_COUNT.toFloat(),
            keyPoints = listOf(snapValue.toFloat()),
            showKeyPoints = true,
            magnetThreshold = 0.03f,
            hapticEffect = top.yukonga.miuix.kmp.basic.SliderDefaults.SliderHapticEffect.Step,
        )
    }
}

@Composable
private fun BestSettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(start = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            insideMargin = PaddingValues(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BestFilterChips(
    values: List<String>,
    selected: Set<String>,
    display: (String) -> String,
    color: @Composable (String) -> Color,
    onToggle: (String) -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        values.forEach { value ->
            val isSelected = value in selected
            val chipColor = color(value)
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) chipColor else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                label = "best-filter-chip-background",
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) chipColor.copy(alpha = 0.45f) else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.09f),
                label = "best-filter-chip-border",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface,
                label = "best-filter-chip-content",
            )
            Box(
                modifier = Modifier
                    .squircleSurface(color = backgroundColor, cornerRadius = 50.dp, extension = SquircleExtension)
                    .border(width = 1.dp, color = borderColor, shape = CircleShape)
                    .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle(value) })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = display(value),
                    style = MiuixTheme.textStyles.footnote1,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bestTableSection(
    sectionKey: String,
    title: String,
    entries: List<BestTableEntry>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onOpenSong: (String) -> Unit,
) {
    if (entries.isEmpty()) {
        item(key = "$sectionKey-empty") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
            ) {
                Text(tr("暂无可计入 {0} 的成绩", title), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    } else {
        items(entries, key = { entry -> "$sectionKey-${entry.chartId}" }) { entry ->
            BestTableEntryCard(
                entry = entry,
                jacketBaseUrl = jacketBaseUrl,
                localJacketPath = localJacketPath,
                onClick = { onOpenSong(entry.songId) },
            )
        }
    }
}

private val BestShareColor = Color(0xFFFF9500)
private val NewShareColor = Color(0xFF3E9BFF)

@Composable
private fun BestRatingSummary(
    summary: org.rhythmeta.chunithmd.shared.PlayerRatingSummary,
    bestEntries: List<BestTableEntry>,
    newEntries: List<BestTableEntry>,
) {
    val bestRating = bestEntries.sumOf(BestTableEntry::rating)
    val newRating = newEntries.sumOf(BestTableEntry::rating)
    val totalRating = bestRating + newRating
    val entryCount = bestEntries.size + newEntries.size
    val averageRating = if (entryCount > 0) totalRating / entryCount else 0.0
    val bestPercent = if (totalRating > 0) bestRating * 100.0 / totalRating else 0.0
    val newPercent = if (totalRating > 0) newRating * 100.0 / totalRating else 0.0
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tr("玩家 Rating"),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        formatRating(summary.rating),
                        style = MiuixTheme.textStyles.headline1.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            brush = Brush.horizontalGradient(playerRatingColors(summary.rating)),
                        ),
                        color = Color.Unspecified,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "B${summary.bestSlotCount}  ${formatRating(summary.best30Average)}",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        "N${summary.newSlotCount}  ${formatRating(summary.new20Average)}",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        tr("平均 {0}", formatRating(averageRating)),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RatingShareBar(newRating = newRating, oldRating = bestRating)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        tr("N{0} 占比 {1}%", summary.newSlotCount, String.format(Locale.ROOT, "%.1f", newPercent)),
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        color = NewShareColor,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        tr("B{0} 占比 {1}%", summary.bestSlotCount, String.format(Locale.ROOT, "%.1f", bestPercent)),
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        color = BestShareColor,
                        textAlign = TextAlign.End,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(0.6f))
                    Text(
                        tr("平均分数"),
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.4f),
                    )
                }
                BestScoreStatRow(
                    label = "B${bestEntries.size + newEntries.size}",
                    entries = bestEntries + newEntries,
                    background = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                )
                BestScoreStatRow(
                    label = "B${bestEntries.size}",
                    entries = bestEntries,
                    background = BestShareColor.copy(alpha = 0.12f),
                )
                BestScoreStatRow(
                    label = "N${newEntries.size}",
                    entries = newEntries,
                    background = NewShareColor.copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
private fun BestScoreStatRow(
    label: String,
    entries: List<BestTableEntry>,
    background: Color,
) {
    val average = if (entries.isEmpty()) null else entries.sumOf { it.score.toLong() } / entries.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .squircleSurface(color = background, cornerRadius = 8.dp, extension = SquircleExtension)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.weight(0.6f),
        )
        Text(
            text = average?.let { formatScore(it.toInt()) } ?: "-",
            style = MiuixTheme.textStyles.footnote2.copy(fontFamily = FontFamily.Monospace),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}

/** Horizontal weighting of the Best and New halves of the player rating. */
@Composable
private fun RatingShareBar(newRating: Double, oldRating: Double) {
    val newWeight = newRating.coerceAtLeast(0.0).toFloat()
    val oldWeight = oldRating.coerceAtLeast(0.0).toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (newWeight > 0f) {
                Box(Modifier.weight(newWeight).fillMaxHeight().background(NewShareColor))
            }
            if (oldWeight > 0f) {
                Box(Modifier.weight(oldWeight).fillMaxHeight().background(BestShareColor))
            }
        }
    }
}

@Composable
private fun BestCapacityCard(
    bestText: String,
    newText: String,
    onBestChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onCommit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(14.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CapacityTextField(tr("Best 数量"), bestText, onBestChange, onCommit)
            CapacityTextField(tr("New 数量"), newText, onNewChange, onCommit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tr("当前总计"),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    ((bestText.toIntOrNull() ?: 0) + (newText.toIntOrNull() ?: 0)).toString(),
                    style = MiuixTheme.textStyles.headline1,
                    fontWeight = FontWeight.Black,
                    color = BestAccent,
                )
            }
        }
    }
}

@Composable
private fun CapacityTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
) {
    var wasFocused by remember { mutableStateOf(false) }
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        useLabelAsPlaceholder = false,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onCommit() }),
        modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
            if (wasFocused && !focusState.isFocused) onCommit()
            wasFocused = focusState.isFocused
        },
    )
}

@Composable
private fun BestTableEntryCard(
    entry: BestTableEntry,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onClick: () -> Unit,
) {
    val darkTheme = SongVisualUtils.isDarkTheme(MiuixTheme.colorScheme.background)
    ScoreEntryCard(
        songId = entry.songId, chartKey = entry.chartId, title = entry.title, imageName = entry.imageName,
        difficulty = entry.difficulty, score = entry.score, rank = entry.rank,
        value = formatRating(entry.rating), caption = tr("定数 {0}", formatLevel(entry.constant)),
        valueBrush = ratingTierBrush(entry.rating, darkTheme),
        jacketBaseUrl = jacketBaseUrl, localJacketPath = localJacketPath, onClick = onClick,
    ) {
        ClearType.displayName(entry.clear)?.let { ScoreEntryBadge(tr(it), clearStatusColor(entry.clear)) }
        FullComboType.displayName(entry.fullCombo)?.let { ScoreEntryBadge(tr(it), comboStatusColor(entry.fullCombo)) }
        FullChainType.displayName(entry.fullChain)?.let { ScoreEntryBadge(tr(it), chainStatusColor(entry.fullChain)) }
    }
}

private fun formatLevel(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

private fun formatRating(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

private fun formatScore(value: Int): String = String.format(Locale.ROOT, "%,d", value)

private val BestAccent = Color(0xFFFF9500)

private fun ratingTierBrush(rating: Double, darkTheme: Boolean): Brush {
    fun Color.forTheme(): Color = if (darkTheme) {
        this
    } else {
        Color(red * 0.72f, green * 0.72f, blue * 0.72f, alpha)
    }

    return when {
        rating >= 16.0 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF5E5E),
                Color(0xFFFFBA5E),
                Color(0xFFFFF75E),
                Color(0xFF5EFF5E),
                Color(0xFF5EBAFF),
                Color(0xFFBA5EFF),
                Color(0xFFFF5EBA),
            ).map { it.forTheme() },
            start = Offset.Zero,
            end = Offset.Infinite,
        )
        rating >= 15.25 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFD3D3D3),
                Color.White,
                Color(0xFFD3D3D3),
            ).map { it.forTheme() },
            start = Offset.Zero,
            end = Offset.Infinite,
        )
        rating >= 14.50 -> Brush.verticalGradient(
            colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500)).map { it.forTheme() },
        )
        else -> Brush.linearGradient(
            listOf(ratingTierColor(rating).forTheme(), ratingTierColor(rating).forTheme()),
        )
    }
}

@Composable
fun BestTableHomeCard(
    bestCount: Int = 30,
    newCount: Int = 20,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .squircleBorder(
                width = 1.dp,
                color = BestAccent.copy(alpha = 0.2f),
                cornerRadius = 16.dp,
                extension = SquircleExtension,
            ),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        colors = CardDefaults.defaultColors(color = BestAccent.copy(alpha = 0.1f)),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tr("查看 Best {0} 成绩表", bestCount + newCount),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    tr("基于 B{0} + N{1} 计算的玩家 Rating", bestCount, newCount),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = tr("打开 Best 50"),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}
