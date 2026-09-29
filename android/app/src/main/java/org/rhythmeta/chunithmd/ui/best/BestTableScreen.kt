package org.rhythmeta.chunithmd.ui.best

import android.content.Intent
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
import kotlinx.coroutines.launch
import org.rhythmeta.chunithmd.score.ClearType
import org.rhythmeta.chunithmd.score.FullChainType
import org.rhythmeta.chunithmd.score.FullComboType
import org.rhythmeta.chunithmd.score.ScoreRecordEntity
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.shared.CatalogVersionFormatter
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.RatingChartEntry
import org.rhythmeta.chunithmd.shared.calculatePlayerRating
import org.rhythmeta.chunithmd.shared.calculateSingleRating
import org.rhythmeta.chunithmd.shared.isSheetPlayableIn
import org.rhythmeta.chunithmd.shared.latestPlayableVersion
import org.rhythmeta.chunithmd.ui.catalog.difficultyColor
import org.rhythmeta.chunithmd.ui.catalog.SongVisualUtils
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

private data class BestTableEntry(
    val chartId: String,
    val songId: String,
    val title: String,
    val imageName: String,
    val type: String,
    val difficulty: String,
    val level: String,
    val score: Int,
    val rank: String,
    val rating: Double,
    val isNew: Boolean,
    val clear: String,
    val fullCombo: String?,
    val fullChain: String?,
)

@Composable
fun BestTableScreen(
    bundle: CatalogBundle?,
    activeServer: ProfileServer,
    records: List<ScoreRecordEntity>,
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
            Text("正在加载歌曲目录", style = MiuixTheme.textStyles.body1)
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
    val bestCount = bestCountText.toIntOrNull()?.coerceIn(1, 99) ?: preferences.bestCount
    val newCount = newCountText.toIntOrNull()?.coerceIn(1, 99) ?: preferences.newCount
    val versionOptions = remember(bundle) {
        listOf<String?>(null) + bundle.catalog.versions.asReversed().map { it.version }.distinct()
    }
    val effectiveVersion = selectedVersion?.takeIf { it in versionOptions }
        ?: bundle.latestPlayableVersion(activeServer)
    val versionLabels = versionOptions.map { version ->
        version?.let(CatalogVersionFormatter::badge) ?: "自动"
    }
    val entries = remember(bundle, records, activeServer, effectiveVersion) {
        buildBestTableEntries(bundle, records, activeServer, effectiveVersion)
    }
    val summary = remember(entries, bestCount, newCount) {
        calculatePlayerRating(entries.map { entry ->
            RatingChartEntry(entry.chartId, entry.songId, entry.rating, entry.isNew)
        }, bestSlotCount = bestCount, newSlotCount = newCount)
    }
    val entriesById = remember(entries) { entries.associateBy(BestTableEntry::chartId) }
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
                    level = entry.level,
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
            version = effectiveVersion?.let(CatalogVersionFormatter::badge) ?: "自动",
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
                    "分享 Best 50",
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
                text = "游戏版本",
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
                    title = "当前版本",
                    summary = selectedVersion?.let { "临时覆盖" },
                    onSelectedIndexChange = { index ->
                        selectedVersion = versionOptions[index]
                        scope.launch { preferencesRepository.setVersion(selectedVersion) }
                    },
                )
            }
        }
        item {
            SmallTitle(
                text = "容量设置",
                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            )
            BestCapacityCard(
                bestText = bestCountText,
                newText = newCountText,
                onBestChange = { value -> if (value.length <= 2 && value.all(Char::isDigit)) bestCountText = value },
                onNewChange = { value -> if (value.length <= 2 && value.all(Char::isDigit)) newCountText = value },
                onCommit = {
                    val best = bestCountText.toIntOrNull()?.coerceIn(1, 99) ?: 30
                    val newer = newCountText.toIntOrNull()?.coerceIn(1, 99) ?: 20
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
                Text("暂无可计入 $title 的成绩", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
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
                    "玩家 Rating",
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
            CapacityTextField("新曲数量", bestText, onBestChange, onCommit)
            CapacityTextField("旧曲数量", newText, onNewChange, onCommit)
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
                Modifier.height(52.dp).width(4.dp).squircleSurface(
                    color = accent,
                    cornerRadius = 2.dp,
                    extension = SquircleExtension,
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
                    EntryBadge(
                        ClearType.displayName(entry.clear),
                        clearStatusColor(entry.clear),
                    )
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
                Text("定数 ${entry.level}", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    }
}

@Composable
private fun EntryBadge(text: String, color: Color) {
    Text(
        text,
        style = MiuixTheme.textStyles.footnote2.copy(fontSize = 9.sp),
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.squircleSurface(color.copy(alpha = 0.13f), 4.dp, SquircleExtension).padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

private fun buildBestTableEntries(
    bundle: CatalogBundle,
    records: List<ScoreRecordEntity>,
    activeServer: ProfileServer,
    selectedVersion: String?,
): List<BestTableEntry> {
    val bestScores = records.groupBy(ScoreRecordEntity::sheetKey)
        .mapValues { (_, values) -> values.maxByOrNull(ScoreRecordEntity::score)!! }
    val latestVersion = selectedVersion ?: bundle.latestPlayableVersion(activeServer)
    val selectedVersionIndex = selectedVersion?.let { selected ->
        bundle.catalog.versions.indexOfFirst { it.version.equals(selected, ignoreCase = true) }
            .takeIf { it >= 0 }
    }
    return bundle.catalog.songs.flatMap { song ->
        song.sheets.filter { sheet ->
            val songVersionIndex = song.version?.let { songVersion ->
                bundle.catalog.versions.indexOfFirst { it.version.equals(songVersion, ignoreCase = true) }
            }
            val releasedBySelectedVersion = selectedVersionIndex == null || songVersionIndex == null || songVersionIndex <= selectedVersionIndex
            releasedBySelectedVersion && (selectedVersion != null || song.isSheetPlayableIn(sheet, activeServer.wireValue))
        }.mapNotNull { sheet ->
            val key = scoreSheetKey(song, sheet)
            val record = bestScores[key] ?: return@mapNotNull null
            val constant = if (activeServer == ProfileServer.Cn) {
                song.regionOverrides["cn"]
                    ?.charts
                    ?.get("${sheet.type}:${sheet.difficulty}")
                    ?.levelValue
                    ?: sheet.internalLevelValue
                    ?: sheet.levelValue
            } else {
                sheet.internalLevelValue ?: sheet.levelValue
            } ?: return@mapNotNull null
            val rating = calculateSingleRating(constant, record.score)
            if (rating <= 0.0) return@mapNotNull null
            BestTableEntry(
                chartId = key,
                songId = song.songId,
                title = CatalogSongFormatter.displayTitle(song),
                imageName = song.imageName,
                type = sheet.type,
                difficulty = sheet.difficulty,
                level = String.format(Locale.ROOT, "%.1f", constant),
                score = record.score,
                rank = record.rank,
                rating = rating,
                isNew = latestVersion != null && song.version.equals(latestVersion, ignoreCase = true),
                clear = record.clear,
                fullCombo = record.fullCombo,
                fullChain = record.fullChain,
            )
        }
    }.sortedByDescending(BestTableEntry::rating)
}

private fun scoreSheetKey(song: CatalogSong, sheet: CatalogSheet): String = "${song.songId}:${sheet.type}:${sheet.difficulty}"

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

private fun clearStatusColor(value: String?): Color = when (ClearType.fromWire(value)) {
    ClearType.Catastrophy -> Color(0xFFAF52DE)
    ClearType.Absolute -> Color(0xFF007AFF)
    ClearType.Brave -> Color(0xFF34C759)
    ClearType.Hard -> Color(0xFFFF9500)
    ClearType.Clear -> Color(0xFF5AC8FA)
    ClearType.Failed -> Color(0xFFFF3B30)
}

private fun comboStatusColor(value: String?): Color = when (FullComboType.fromWire(value)) {
    FullComboType.AllJusticeCritical,
    FullComboType.AllJustice,
    -> Color(0xFFFF9500)
    FullComboType.FullCombo -> Color(0xFF34C759)
    null -> Color(0xFF8E8E93)
}

private fun chainStatusColor(value: String?): Color = when (FullChainType.fromWire(value)) {
    FullChainType.FullChain -> Color(0xFFB7C4D6)
    FullChainType.FullChain2 -> Color(0xFFD4A72C)
    null -> Color(0xFF8E8E93)
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
                Text("查看 Best 50 成绩表", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
                Text(
                    "基于 B$bestCount + N$newCount 计算的玩家 Rating",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "打开 Best 50",
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}
