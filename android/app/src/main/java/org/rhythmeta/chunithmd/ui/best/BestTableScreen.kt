package org.rhythmeta.chunithmd.ui.best

import org.rhythmeta.chunithmd.shared.localization.tr

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
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
import org.rhythmeta.chunithmd.ui.catalog.ultimaStripedBrush
import org.rhythmeta.chunithmd.ui.catalog.SongVisualUtils
import org.rhythmeta.chunithmd.ui.components.clearStatusColor
import org.rhythmeta.chunithmd.ui.components.comboStatusColor
import org.rhythmeta.chunithmd.ui.components.chainStatusColor
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.SmallTitle
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
    onOpenSong: (String) -> Unit,
    preferencesRepository: BestTablePreferencesRepository,
    profileName: String? = null,
    shareRequested: Boolean = false,
    onShareRequestHandled: () -> Unit = {},
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
    ) {
        value = null
        value = withContext(Dispatchers.Default) {
            buildBestTableEntries(bundle, records, activeServer, selectedVersion)
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
    val summary = remember(loadedEntries, bestCount, newCount) {
        calculatePlayerRating(loadedEntries.map { entry ->
            RatingChartEntry(entry.chartId, entry.songId, entry.rating, entry.isNew)
        }, bestSlotCount = bestCount, newSlotCount = newCount)
    }
    val entriesById = remember(loadedEntries) { loadedEntries.associateBy(BestTableEntry::chartId) }
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
        modifier = Modifier.fillMaxSize().nestedScroll(topBarScrollConnection),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 12.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BestRatingSummary(summary = summary)
        }
        item {
            SmallTitle(
                text = tr("游戏版本"),
                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp),
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
            ) {
                WindowDropdownPreference(
                    items = versionLabels,
                    selectedIndex = versionOptions.indexOf(selectedVersion).coerceAtLeast(0),
                    title = tr("当前版本"),
                    summary = selectedVersion?.let { tr("临时覆盖") },
                    onSelectedIndexChange = { index ->
                        selectedVersion = versionOptions[index]
                        scope.launch { preferencesRepository.setVersion(selectedVersion) }
                    },
                )
            }
        }
        item {
            SmallTitle(
                text = tr("容量设置"),
                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            )
            BestCapacityCard(
                bestText = bestCountText,
                newText = newCountText,
                onBestChange = { value -> if (value.length <= 2 && value.all(Char::isDigit)) bestCountText = value },
                onNewChange = { value -> if (value.length <= 2 && value.all(Char::isDigit)) newCountText = value },
                onCommit = {
                    val best = bestCountText.toIntOrNull()?.coerceIn(BestTablePreferences.MIN_COUNT, BestTablePreferences.MAX_COUNT)
                        ?: BestTablePreferences.DEFAULT_BEST_COUNT
                    val newer = newCountText.toIntOrNull()?.coerceIn(BestTablePreferences.MIN_COUNT, BestTablePreferences.MAX_COUNT)
                        ?: BestTablePreferences.DEFAULT_NEW_COUNT
                    bestCountText = best.toString()
                    newCountText = newer.toString()
                    focusManager.clearFocus()
                    scope.launch { preferencesRepository.setCapacity(best, newer) }
                },
            )
        }
        bestTableSection(
            sectionKey = "best",
            title = "B$bestCount",
            entries = bestEntries,
            jacketBaseUrl = jacketBaseUrl,
            localJacketPath = localJacketPath,
            onOpenSong = onOpenSong,
        )
        bestTableSection(
            sectionKey = "new",
            title = "N$newCount",
            entries = newEntries,
            jacketBaseUrl = jacketBaseUrl,
            localJacketPath = localJacketPath,
            onOpenSong = onOpenSong,
        )
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
    item(key = "$sectionKey-title") {
        SmallTitle(
            text = title,
            insideMargin = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        )
    }
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

@Composable
private fun BestRatingSummary(
    summary: org.rhythmeta.chunithmd.shared.PlayerRatingSummary,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tr("玩家 Rating"),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    formatRating(summary.rating),
                    style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.Black),
                    color = BestAccent,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("B${summary.bestSlotCount}  ${formatRating(summary.best30Average)}", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text("N${summary.newSlotCount}  ${formatRating(summary.new20Average)}", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
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
    val accent = difficultyColor(entry.difficulty)
    val isUltima = entry.difficulty.equals("ultima", ignoreCase = true)
    val imageModel = remember(entry.imageName, jacketBaseUrl) {
        localJacketPath(entry.imageName)?.let(::File)
            ?: jacketBaseUrl.trimEnd('/').takeIf { it.isNotBlank() }?.let { "$it/${entry.imageName.trimStart('/')}" }
    }
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(start = 8.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(84.dp).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .height(52.dp)
                    .width(4.dp)
                    .then(
                        if (isUltima) {
                            Modifier.clip(RoundedCornerShape(2.dp)).background(ultimaStripedBrush())
                        } else {
                            Modifier.squircleSurface(
                                color = accent,
                                cornerRadius = 2.dp,
                                extension = SquircleExtension,
                            )
                        },
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(56.dp).squircleSurface(
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    cornerRadius = 10.dp,
                    extension = SquircleExtension,
                ),
            ) {
                if (imageModel == null) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = accent, modifier = Modifier.align(Alignment.Center))
                } else {
                    AsyncImage(imageModel, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().basicMarquee())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.rank,
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.Black,
                        color = rankColor(entry.rank).takeUnless { it == Color.Unspecified }
                            ?: MiuixTheme.colorScheme.onSurface,
                    )
                    Text(formatScore(entry.score), style = MiuixTheme.textStyles.footnote1.copy(fontFamily = FontFamily.Monospace), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ClearType.displayName(entry.clear)?.let { display ->
                        EntryBadge(display, clearStatusColor(entry.clear))
                    }
                    FullComboType.displayName(entry.fullCombo)?.let { display ->
                        EntryBadge(display, comboStatusColor(entry.fullCombo))
                    }
                    FullChainType.displayName(entry.fullChain)?.let { display ->
                        EntryBadge(display, chainStatusColor(entry.fullChain))
                    }
                }
            }
            Column(modifier = Modifier.padding(end = 14.dp), horizontalAlignment = Alignment.End) {
                Text(formatRating(entry.rating), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Black, color = BestAccent)
                Text(tr("定数 {0}", formatLevel(entry.constant)), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    }
}

@Composable
private fun EntryBadge(text: String, color: Color) {
    Text(
        tr(text),
        style = MiuixTheme.textStyles.footnote2.copy(fontSize = 9.sp),
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.squircleSurface(color.copy(alpha = 0.13f), 4.dp, SquircleExtension).padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

private fun formatLevel(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

private fun formatRating(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

private fun formatScore(value: Int): String = String.format(Locale.ROOT, "%,d", value)

private fun rankColor(rank: String): Color = when (rank.uppercase(Locale.ROOT)) {
    "SSS+", "SSS" -> Color(0xFFFFD900)
    "SS+", "SS" -> Color(0xFFFFBF00)
    "S+", "S" -> Color(0xFFFF9900)
    "AAA" -> Color(0xFFCC99FF)
    "AA" -> Color(0xFF99CCFF)
    "A" -> Color(0xFF80E680)
    else -> Color.Unspecified
}

private val BestAccent = Color(0xFFFF9500)

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
                Text(tr("查看 Best 50 成绩表"), style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
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
