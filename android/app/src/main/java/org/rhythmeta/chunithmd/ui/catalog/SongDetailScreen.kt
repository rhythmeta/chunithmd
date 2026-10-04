package org.rhythmeta.chunithmd.ui.catalog

import org.rhythmeta.chunithmd.shared.localization.tr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.launch
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.ChunithmScoreRules
import org.rhythmeta.chunithmd.shared.calculateSingleRating
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.shared.CatalogNoteCounts
import org.rhythmeta.chunithmd.shared.CatalogVersionFormatter
import org.rhythmeta.chunithmd.shared.worldsEndStars
import org.rhythmeta.chunithmd.score.ScoreRepository
import org.rhythmeta.chunithmd.collection.CollectionEntry
import org.rhythmeta.chunithmd.collection.SongCollectionRepository
import org.rhythmeta.chunithmd.ui.collections.CollectionPickerSheet
import org.rhythmeta.chunithmd.shared.ClearType
import org.rhythmeta.chunithmd.shared.FullChainType
import org.rhythmeta.chunithmd.shared.FullComboType
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.ScoreHistorySort
import org.rhythmeta.chunithmd.shared.bestScore
import org.rhythmeta.chunithmd.shared.BestScoreSummary
import org.rhythmeta.chunithmd.shared.bestScoreSummary
import org.rhythmeta.chunithmd.shared.buildRatingTable
import org.rhythmeta.chunithmd.shared.breakdown
import org.rhythmeta.chunithmd.shared.page
import org.rhythmeta.chunithmd.shared.sheetKey
import org.rhythmeta.chunithmd.shared.sortForHistory
import org.rhythmeta.chunithmd.ui.components.ExpandableBottomSheet
import org.rhythmeta.chunithmd.ui.components.dashedSquircleBorder
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup
import androidx.compose.ui.platform.LocalLocale
import androidx.core.net.toUri

private val CHART_TYPE_ORDER = listOf("std", "standard", "we")

@Composable
fun SongDetailScreen(
    song: CatalogSong?,
    loading: Boolean = false,
    aliases: List<String>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    contentTopPadding: Dp,
    topBarScrollConnection: NestedScrollConnection,
    scoreRepository: ScoreRepository,
    collectionRepository: SongCollectionRepository,
    communityStore: org.rhythmeta.chunithmd.shared.community.CommunityAliasStore,
    onOpenCommunity: () -> Unit,
    onLogin: () -> Unit,
    onBackgroundChanged: (Color?) -> Unit = {},
) {
    if (song == null) {
        if (loading) {
            DetailLoadingState()
        } else {
            DetailEmptyState()
        }
        return
    }

    val communityState by communityStore.state.collectAsState()
    val communityAliasKeys = remember(communityState.approvedAliases, communityState.personalAliases, song.songId) {
        (communityState.approvedAliases[song.songId].orEmpty() + communityState.personalAliases[song.songId].orEmpty())
            .mapTo(mutableSetOf()) { it.trim().lowercase() }
    }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val scoreRecords by scoreRepository.observeSongRecords(song.songId).collectAsState(initial = emptyList())
    val recordsBySheet = remember(scoreRecords) { scoreRecords.groupBy(ScoreRecord::sheetKey) }
    var scoreEntrySheetKey by rememberSaveable(song.songId) { mutableStateOf<String?>(null) }
    var collectionSheetKey by rememberSaveable(song.songId) { mutableStateOf<String?>(null) }
    var recordToDelete by remember { mutableStateOf<ScoreRecord?>(null) }
    var scoreEntrySaving by remember { mutableStateOf(false) }
    fun showMessage(message: String) {
        snackbarScope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    val isDark = SongVisualUtils.isDarkTheme(MiuixTheme.colorScheme.background)
    var jacketAccent by remember(song.songId) { mutableStateOf<Color?>(null) }
    val detailColors = jacketAccent?.let { SongVisualUtils.detailColors(it, isDark) }
    val accent = detailColors?.accent ?: MiuixTheme.colorScheme.primary
    val pageBackground = detailColors?.background ?: MiuixTheme.colorScheme.background
    val surfaceColor = detailColors?.surface ?: MiuixTheme.colorScheme.surfaceContainer
    val selectedSurfaceColor = detailColors?.selectedSurface ?: MiuixTheme.colorScheme.secondaryContainer
    val localCover = remember(song.imageName) {
        localJacketPath(song.imageName)?.let(::File)
    }
    val coverModel = remember(song.imageName, localCover, jacketBaseUrl) {
        localCover ?: jacketBaseUrl.trimEnd('/').takeIf { it.isNotBlank() && song.imageName.isNotBlank() }?.let {
            "$it/${song.imageName.trimStart('/')}"
        }
    }
    val chartTypes = remember(song.sheets) {
        song.sheets.map { it.type.trim() }.filter(String::isNotBlank).distinct().sortedWith(
            compareBy<String> { typeOrder(it) }.thenBy(String::lowercase),
        )
    }
    var selectedType by rememberSaveable(song.songId) { mutableStateOf<String?>(null) }
    LaunchedEffect(chartTypes) {
        if (selectedType !in chartTypes) {
            selectedType = chartTypes.firstOrNull { it.equals("std", true) }
                ?: chartTypes.firstOrNull { it.equals("we", true) }
                ?: chartTypes.firstOrNull()
        }
    }
    LaunchedEffect(song.songId) {
        onBackgroundChanged(null)
    }
    LaunchedEffect(detailColors) {
        onBackgroundChanged(detailColors?.background)
    }
    val visibleSheets = remember(song.sheets, selectedType) {
        song.sheets
            .filter { selectedType == null || it.type.equals(selectedType, true) }
            .sortedWith(compareByDescending<CatalogSheet> { detailDifficultyOrder(it.difficulty) }.thenByDescending { it.levelValue ?: Double.MIN_VALUE })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topBarScrollConnection),
            contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 8.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SongDetailHeader(
                    song = song,
                    aliases = aliases,
                    communityAliasKeys = communityAliasKeys,
                    coverModel = coverModel,
                    coverFile = localCover,
                    surfaceColor = surfaceColor,
                    accentColor = accent,
                    onAccentColor = { jacketAccent = it },
                    onCopyText = { copyText(context, it, ::showMessage) },
                    onCoverAction = { action -> performCoverAction(context, localCover, song.title, action, ::showMessage) },
                )
            }
            item {
                org.rhythmeta.chunithmd.ui.community.SongCommunityAliasSection(
                    songId = song.songId,
                    store = communityStore,
                    surfaceColor = surfaceColor,
                    accentColor = accent,
                    showMessage = ::showMessage,
                    onOpenBoard = onOpenCommunity,
                    onLogin = onLogin,
                )
            }
            item {
                RegionAvailabilityCard(
                    song = song,
                    surfaceColor = surfaceColor,
                    accentColor = accent,
                )
            }
            item {
                ExternalSearchCard(song.title, surfaceColor, accent, context, ::showMessage)
            }
            if (chartTypes.size > 1) {
                item {
                    ChartTypeSelector(
                        types = chartTypes,
                        selected = selectedType,
                        surfaceColor = surfaceColor,
                        selectedSurfaceColor = selectedSurfaceColor,
                        accentColor = accent,
                        onSelected = { selectedType = it },
                    )
                }
            }
            if (visibleSheets.isEmpty()) {
                item { EmptyChartState() }
            } else {
                items(
                    items = visibleSheets,
                    key = { sheet -> "${song.songId}:${sheet.type}:${sheet.difficulty}" },
                ) { sheet ->
                    ChartDetailCard(
                        song = song,
                        sheet = sheet,
                        surfaceColor = surfaceColor,
                        accentColor = accent,
                        actionSurfaceColor = selectedSurfaceColor,
                        records = recordsBySheet[song.sheetKey(sheet)].orEmpty(),
                        onRecord = { scoreEntrySheetKey = song.sheetKey(sheet) },
                        onDeleteRecord = { recordToDelete = it },
                        onAddToCollection = { collectionSheetKey = song.sheetKey(sheet) },
                    )
                }
            }
        }
        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        )
    }

    CollectionPickerSheet(
        repository = collectionRepository,
        entry = collectionSheetKey?.let { key -> song.sheets.firstOrNull { song.sheetKey(it) == key } }
            ?.let { CollectionEntry(song.songId, it.type, it.difficulty) },
        onDismiss = { collectionSheetKey = null },
    )
    val entrySheet = scoreEntrySheetKey?.let { key ->
        song.sheets.firstOrNull { song.sheetKey(it) == key }
    }
    ScoreEntrySheet(
        visible = entrySheet != null,
        song = song,
        sheet = entrySheet,
        bestRecord = entrySheet?.let { sheet -> recordsBySheet[song.sheetKey(sheet)].orEmpty().bestScoreSummary() },
        saving = scoreEntrySaving,
        onDismiss = { if (!scoreEntrySaving) scoreEntrySheetKey = null },
        onSave = { score, clear, fullCombo, fullChain ->
            entrySheet?.let { sheet ->
                scoreEntrySaving = true
                snackbarScope.launch {
                    runCatching {
                        scoreRepository.save(
                            songId = song.songId,
                            sheetKey = song.sheetKey(sheet),
                            score = score,
                            clear = clear,
                            fullCombo = fullCombo,
                            fullChain = fullChain,
                        )
                    }
                        .onSuccess { scoreEntrySheetKey = null }
                        .onFailure { showMessage(tr("保存成绩失败")) }
                    scoreEntrySaving = false
                }
            }
        },
    )
    recordToDelete?.let { record ->
        DeleteScoreRecordDialog(
            onConfirm = {
                recordToDelete = null
                snackbarScope.launch { scoreRepository.delete(record.id) }
            },
            onDismiss = { recordToDelete = null },
        )
    }
}

@Composable
private fun DetailEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MiuixIcon(Icons.Rounded.Info, contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        MiuixText(tr("歌曲不存在或目录尚未加载"), style = MiuixTheme.textStyles.body1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DetailLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        MiuixText(tr("正在加载歌曲目录"), style = MiuixTheme.textStyles.body1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SongDetailHeader(
    song: CatalogSong,
    aliases: List<String>,
    communityAliasKeys: Set<String>,
    coverModel: Any?,
    coverFile: File?,
    surfaceColor: Color,
    accentColor: Color,
    onAccentColor: (Color) -> Unit,
    onCopyText: (String) -> Unit,
    onCoverAction: (CoverAction) -> Unit,
) {
    val context = LocalContext.current
    val coverRequest = remember(coverModel) {
        ImageRequest.Builder(context)
            .data(coverModel)
            .allowHardware(false)
            .build()
    }
    var jacketMenuExpanded by remember(song.songId) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { jacketMenuExpanded = true },
                    )
                    .squircleSurface(color = surfaceColor, cornerRadius = 26.dp, extension = SquircleExtension),
                contentAlignment = Alignment.Center,
            ) {
                if (coverModel == null) {
                    MiuixIcon(Icons.Rounded.MusicNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(56.dp))
                } else {
                    AsyncImage(
                        model = coverRequest,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onSuccess = { result ->
                            runCatching {
                                result.result.drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
                            }.getOrNull()?.let { bitmap ->
                                SongVisualUtils.averageJacketColor(bitmap)?.let { color ->
                                    Log.d("SongDetailScreen", "jacket color=${color.toArgb()} model=$coverModel")
                                    onAccentColor(color)
                                } ?: Log.w("SongDetailScreen", "jacket color unavailable model=$coverModel")
                            }
                        },
                    )
                }
            }
            CoverActionMenu(
                expanded = jacketMenuExpanded,
                enabled = coverFile != null,
                onDismiss = { jacketMenuExpanded = false },
                onAction = { action ->
                    jacketMenuExpanded = false
                    onCoverAction(action)
                },
            )
        }
        Spacer(Modifier.height(14.dp))
        MiuixText(
            text = CatalogSongFormatter.displayTitle(song),
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().basicMarquee().clickable { onCopyText(CatalogSongFormatter.displayTitle(song)) },
        )
        MiuixText(
            text = song.artist.ifBlank { tr("未知艺术家") },
            style = MiuixTheme.textStyles.body1,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().basicMarquee().clickable { onCopyText(song.artist) },
        )
        if (aliases.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                aliases.forEach { alias ->
                    MiuixText(
                        text = alias,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .clickable { onCopyText(alias) }
                            .squircleSurface(color = surfaceColor, cornerRadius = 50.dp, extension = SquircleExtension)
                            .then(
                                if (alias.trim().lowercase() in communityAliasKeys) {
                                    Modifier.dashedSquircleBorder(1.dp, accentColor.copy(alpha = 0.55f), 50.dp)
                                } else Modifier,
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        MetadataGrid(song, surfaceColor, accentColor, onCopyText)
    }
}

@Composable
private fun CoverActionMenu(
    expanded: Boolean,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onAction: (CoverAction) -> Unit,
) {
    WindowListPopup(
        show = expanded,
        alignment = PopupPositionProvider.Align.End,
        onDismissRequest = onDismiss,
    ) {
        ListPopupColumn {
            CoverActionMenuItem(tr("下载封面"), Icons.Rounded.Download, enabled) { onAction(CoverAction.Download) }
            CoverActionMenuItem(tr("复制封面"), Icons.Rounded.ContentCopy, enabled) { onAction(CoverAction.Copy) }
            CoverActionMenuItem(tr("分享封面"), Icons.Rounded.Share, enabled) { onAction(CoverAction.Share) }
        }
    }
}

@Composable
private fun CoverActionMenuItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixIcon(icon, contentDescription = null, tint = if (enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.disabledOnSecondaryVariant, modifier = Modifier.size(20.dp))
        MiuixText(text, style = MiuixTheme.textStyles.body1, color = if (enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.disabledOnSecondaryVariant)
    }
}

@Composable
private fun MetadataGrid(
    song: CatalogSong,
    surfaceColor: Color,
    accentColor: Color,
    onCopyText: (String) -> Unit,
) {
    val values = listOfNotNull(
        song.bpm?.let { "BPM ${it.toInt()}" to Icons.Rounded.Timer },
        song.category.ifBlank { null }?.let { it to Icons.Rounded.GridView },
        song.version?.takeIf(String::isNotBlank)?.let { CatalogVersionFormatter.badge(it) to Icons.Rounded.Album },
        song.releaseDate?.takeIf(String::isNotBlank)?.let { it to Icons.Rounded.CalendarMonth },
    )
    if (values.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val singleRow = values.size == 3 || maxWidth >= 520.dp
        val rows = if (singleRow) listOf(values) else values.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (value, icon) ->
                        MetadataChip(value, icon, Modifier.weight(1f), surfaceColor, accentColor) { onCopyText(value) }
                    }
                    repeat((if (singleRow) values.size else 2) - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    surfaceColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .squircleSurface(color = surfaceColor, cornerRadius = 50.dp, extension = SquircleExtension)
            .border(width = 0.5.dp, color = accentColor.copy(alpha = 0.58f), shape = CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixIcon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        MiuixText(value, style = MiuixTheme.textStyles.footnote2, color = accentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RegionAvailabilityCard(song: CatalogSong, surfaceColor: Color, accentColor: Color) {
    DetailCard(surfaceColor, accentColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RegionFlag("🇯🇵", tr("日本"), songRegionAvailable(song, "jp"), accentColor)
            RegionFlag("🌏", tr("国际"), songRegionAvailable(song, "intl"), accentColor)
            RegionFlag("🇨🇳", tr("中国"), songRegionAvailable(song, "cn"), accentColor)
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExternalSearchCard(
    title: String,
    surfaceColor: Color,
    accentColor: Color,
    context: Context,
    onMessage: (String) -> Unit,
) {
    val encodedTitle = Uri.encode(title)
    val darkTheme = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val youtubeSurface = if (darkTheme) Color(0xFF65373C) else Color(0xFFFFD9DD)
    val youtubeContent = if (darkTheme) Color(0xFFFFA0A9) else Color(0xFFC92D3B)
    val bilibiliSurface = if (darkTheme) Color(0xFF24566A) else Color(0xFFD5F2FC)
    val bilibiliContent = if (darkTheme) Color(0xFF8BDCF7) else Color(0xFF007EAA)
    DetailCard(surfaceColor, accentColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiuixIcon(Icons.Rounded.Search, contentDescription = tr("外部搜索"), tint = accentColor, modifier = Modifier.size(20.dp))
            ExternalSearchButton(
                text = "YouTube",
                icon = Icons.Rounded.PlayArrow,
                onClick = {
                    if (!openExternalSearch(context, "https://www.youtube.com/results?search_query=chunithm+$encodedTitle")) {
                        onMessage(tr("未找到可用的 YouTube 应用"))
                    }
                },
                modifier = Modifier.weight(1f),
                surfaceColor = youtubeSurface,
                contentColor = youtubeContent,
            )
            ExternalSearchButton(
                text = "bilibili",
                icon = Icons.Rounded.MusicNote,
                onClick = {
                    if (!openExternalSearch(context, "bilibili://search?keyword=chunithm+$encodedTitle")) {
                        onMessage(tr("未找到可用的 Bilibili 应用"))
                    }
                },
                modifier = Modifier.weight(1f),
                surfaceColor = bilibiliSurface,
                contentColor = bilibiliContent,
            )
        }
    }
}

@Composable
private fun ExternalSearchButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    surfaceColor: Color,
    contentColor: Color,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .squircleSurface(color = surfaceColor, cornerRadius = 12.dp, extension = SquircleExtension)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixIcon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        MiuixText(text, style = MiuixTheme.textStyles.footnote1, color = contentColor, maxLines = 1)
    }
}

@Composable
private fun RegionFlag(flag: String, label: String, available: Boolean, accentColor: Color) {
    val contentColor = if (available) accentColor else MiuixTheme.colorScheme.onSurfaceVariantSummary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MiuixText(flag, style = MiuixTheme.textStyles.title3, modifier = Modifier.alpha(if (available) 1f else 0.32f))
        MiuixText(label, style = MiuixTheme.textStyles.footnote2, color = contentColor)
    }
}

@Composable
private fun ChartTypeSelector(
    types: List<String>,
    selected: String?,
    surfaceColor: Color,
    selectedSurfaceColor: Color,
    accentColor: Color,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        types.forEach { type ->
            val isSelected = type == selected
            Row(
                modifier = Modifier
                    .squircleSurface(color = if (isSelected) selectedSurfaceColor else surfaceColor, cornerRadius = 12.dp, extension = SquircleExtension)
                    .clickable { onSelected(type) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (type.equals("we", true)) Color.Transparent else difficultyColor("master")))
                Spacer(Modifier.width(7.dp))
                MiuixText(chartTypeLabel(type), color = if (isSelected) accentColor else MiuixTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ChartDetailCard(
    song: CatalogSong,
    sheet: CatalogSheet,
    surfaceColor: Color,
    accentColor: Color,
    actionSurfaceColor: Color,
    records: List<ScoreRecord>,
    onRecord: () -> Unit,
    onDeleteRecord: (ScoreRecord) -> Unit,
    onAddToCollection: () -> Unit,
) {
    var expanded by rememberSaveable(song.songId, sheet.type, sheet.difficulty) { mutableStateOf(false) }
    val bestRecord = records.bestScoreSummary()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "difficulty-chevron",
    )
    val chartAccent = if (sheet.type.equals("we", true)) difficultyColor("world's end") else difficultyColor(sheet.difficulty)
    val isWorldsEnd = sheet.type.equals("we", true)
    val isUltima = !isWorldsEnd && sheet.difficulty.equals("ultima", true)
    val chartTextBrush = when {
        isWorldsEnd -> Brush.horizontalGradient(WORLDS_END_GRADIENT_COLORS)
        isUltima -> ultimaStripedBrush()
        else -> null
    }
    val difficultyLabel = sheet.difficulty.trim().uppercase(Locale.ROOT).ifBlank { tr("未知难度") }
    val worldsEndStars = sheet.worldsEndStars()?.coerceIn(0, 5)
    val levelLabel = if (isWorldsEnd && worldsEndStars != null) {
        "★".repeat(worldsEndStars) + "☆".repeat(5 - worldsEndStars)
    } else {
        (sheet.internalLevelValue ?: sheet.levelValue)?.let { String.format(Locale.ROOT, "%.1f", it) }
            ?: sheet.level.ifBlank { "-" }
    }
    DetailCard(
        color = surfaceColor,
        borderColor = chartAccent.copy(alpha = 0.58f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                    onLongClick = onAddToCollection,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(50.dp)
                    .offset(x = (-16).dp)
                    .let { barModifier ->
                        if (sheet.type.equals("we", true)) {
                            barModifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Brush.verticalGradient(WORLDS_END_GRADIENT_COLORS))
                        } else if (isUltima) {
                            barModifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(ultimaStripedBrush())
                        } else {
                            barModifier.squircleSurface(
                                color = chartAccent,
                                cornerRadius = 50.dp,
                                extension = SquircleExtension,
                            )
                        }
                    },
            )
            Column(modifier = Modifier.weight(1f)) {
                MiuixText(
                    difficultyLabel,
                    style = MiuixTheme.textStyles.title3.copy(brush = chartTextBrush),
                    fontWeight = FontWeight.Bold,
                    color = chartAccent,
                )
                sheet.noteDesigner
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let { designer ->
                        MiuixText(
                            designer,
                            style = MiuixTheme.textStyles.body2,
                            color = accentColor.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }
            if (!expanded && bestRecord != null) {
                ChartScorePreview(bestRecord, accentColor)
                Spacer(Modifier.width(8.dp))
            }
            MiuixText(
                levelLabel,
                style = MiuixTheme.textStyles.title3.copy(brush = chartTextBrush),
                fontWeight = FontWeight.Bold,
                color = chartAccent,
            )
            Spacer(Modifier.width(6.dp))
            MiuixIcon(
                Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) tr("收起谱面") else tr("展开谱面"),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.58f),
                modifier = Modifier.size(18.dp).rotate(chevronRotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScoreSummarySection(
                    bestRecord = bestRecord,
                    accentColor = accentColor,
                )
                NoteCountSection(
                    songId = song.songId,
                    chartType = sheet.type,
                    difficulty = sheet.difficulty,
                    counts = sheet.noteCounts,
                )
                RatingTableSection(
                    constant = sheet.internalLevelValue ?: sheet.levelValue,
                )
                ChartScoreToleranceSection(song.songId, sheet, accentColor)
                if (records.isNotEmpty()) {
                    ScoreHistorySection(
                        records = records,
                        accentColor = accentColor,
                        onDeleteRecord = onDeleteRecord,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollectionActionButton(onRecord, actionSurfaceColor, Modifier.weight(1f)) {
                        MiuixIcon(Icons.Rounded.Edit, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        MiuixText(tr("记录成绩"))
                    }
                    CollectionActionButton(onAddToCollection, actionSurfaceColor, Modifier.weight(1f)) {
                        MiuixIcon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        MiuixText(tr("加入收藏夹"))
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionActionButton(
    onClick: () -> Unit,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onSurface) {
        Row(
            modifier.heightIn(min = 40.dp)
                .squircleSurface(surfaceColor, 8.dp, SquircleExtension)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun NoteCountSection(
    songId: String,
    chartType: String,
    difficulty: String,
    counts: CatalogNoteCounts?,
) {
    if (counts == null) return
    val entries = counts.breakdown()
    if (entries.isEmpty()) return

    var expanded by rememberSaveable(songId, chartType, difficulty) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "note-count-chevron",
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixText(
                tr("音符统计"),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            MiuixIcon(
                Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) tr("收起音符统计") else tr("展开音符统计"),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.58f),
                modifier = Modifier.size(16.dp).rotate(chevronRotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (index % 2 == 0) {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiuixText(
                            entry.label,
                            style = MiuixTheme.textStyles.footnote2,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.width(44.dp),
                        )
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(
                                        maxOf(4.dp, maxWidth * entry.fraction.toFloat()).coerceAtMost(maxWidth),
                                    )
                                    .background(noteCountColor(entry.label).copy(alpha = 0.5f)),
                            )
                        }
                        MiuixText(
                            entry.count.toString(),
                            style = MiuixTheme.textStyles.footnote1,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End,
                        )
                        MiuixText(
                            "${(entry.fraction * 100).toInt()}%",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.width(34.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

private fun noteCountColor(label: String): Color = when (label) {
    "TAP", "HOLD" -> Color(0xFFFF2D78)
    "SLIDE", "FLICK" -> Color(0xFF4D80FF)
    "AIR" -> Color(0xFF34C759)
    else -> Color.Gray
}

@Composable
private fun ScoreSummarySection(
    bestRecord: BestScoreSummary?,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MiuixText(
                tr("当前最佳"),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            if (bestRecord == null) {
                MiuixText(
                    tr("暂无成绩"),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixText(
                        formatScore(bestRecord.score),
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold,
                    )
                    MiuixText(
                        bestRecord.rank,
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold,
                        color = scoreRankColor(bestRecord.rank) ?: accentColor,
                    )
                }
                RecordStatusBadges(bestRecord.clear, bestRecord.fullCombo, bestRecord.fullChain, accentColor)
            }
        }
    }
}

@Composable
private fun ChartScorePreview(record: BestScoreSummary, accentColor: Color) {
    Column(
        modifier = Modifier.widthIn(max = 120.dp),
        horizontalAlignment = Alignment.End,
    ) {
        MiuixText(
            text = formatScore(record.score),
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        RecordStatusBadges(record.clear, record.fullCombo, record.fullChain, accentColor, showClear = false)
    }
}

@Composable
private fun RecordStatusBadges(
    clear: String,
    fullCombo: String?,
    fullChain: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    showClear: Boolean = true,
) {
    val statuses = buildList {
        FullComboType.displayName(fullCombo)?.let { add(it to Color(0xFFFFB300)) }
        FullChainType.displayName(fullChain)?.let { add(it to Color(0xFFB7C4D6)) }
        if (showClear || isEmpty()) add(0, ClearType.displayName(clear) to accentColor)
    }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        statuses.forEach { (text, color) ->
            MiuixText(
                tr(text),
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier
                    .squircleSurface(color.copy(alpha = 0.14f), 5.dp, SquircleExtension)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun RatingTableSection(
    constant: Double?,
) {
    val level = constant?.takeIf { it > 0.0 && it.isFinite() } ?: return
    var expanded by rememberSaveable(level) { mutableStateOf(false) }
    val rows = remember(level) { buildRatingTable(level) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "rating-chevron",
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixText(
                tr("分数 → Rating"),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            MiuixIcon(
                Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) tr("收起 Rating") else tr("展开 Rating"),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.58f),
                modifier = Modifier.size(16.dp).rotate(chevronRotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixText(
                        tr("等级"),
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.width(42.dp),
                    )
                    MiuixText(
                        tr("分数"),
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f),
                    )
                    MiuixText(
                        "Rating",
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(64.dp),
                    )
                    MiuixText(
                        tr("差值"),
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(50.dp),
                    )
                }
                rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (index % 2 == 0) {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiuixText(
                            row.rank,
                            style = MiuixTheme.textStyles.footnote1,
                            fontWeight = FontWeight.Bold,
                            color = scoreRankColor(row.rank) ?: MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.width(42.dp),
                        )
                        MiuixText(
                            formatScore(row.score),
                            style = MiuixTheme.textStyles.footnote1,
                            modifier = Modifier.weight(1f),
                        )
                        MiuixText(
                            formatRating(row.rating),
                            style = MiuixTheme.textStyles.body2,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(64.dp),
                        )
                        MiuixText(
                            row.delta.takeIf { it > 0.0 }?.let { "↑${formatRating(it)}" }.orEmpty(),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(50.dp),
                        )
                    }
                }
            }
        }
    }
}

private const val SCORE_HISTORY_PAGE_SIZE = 10

@Composable
private fun ScoreHistorySection(
    records: List<ScoreRecord>,
    accentColor: Color,
    onDeleteRecord: (ScoreRecord) -> Unit,
) {
    val sheetKey = records.first().sheetKey
    var expanded by rememberSaveable(sheetKey) { mutableStateOf(false) }
    var sortByTime by rememberSaveable(sheetKey) { mutableStateOf(true) }
    var page by rememberSaveable(sheetKey) { mutableIntStateOf(1) }
    val sortedRecords = remember(records, sortByTime) {
        if (sortByTime) {
            records.sortForHistory(ScoreHistorySort.Time)
        } else {
            records.sortForHistory(ScoreHistorySort.Score)
        }
    }
    val totalPages = ((sortedRecords.size + SCORE_HISTORY_PAGE_SIZE - 1) / SCORE_HISTORY_PAGE_SIZE).coerceAtLeast(1)
    val validPage = page.coerceIn(1, totalPages)
    val displayRecords = sortedRecords.page(validPage, SCORE_HISTORY_PAGE_SIZE)
    val bestId = records.bestScore()?.id
    LaunchedEffect(totalPages) { page = page.coerceIn(1, totalPages) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixText(
                tr("历史成绩"),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            ScoreHistorySortOption(
                text = tr("时间"),
                selected = sortByTime,
                accentColor = accentColor,
                onClick = { sortByTime = true; page = 1 },
            )
            ScoreHistorySortOption(
                text = tr("分数"),
                selected = !sortByTime,
                accentColor = accentColor,
                onClick = { sortByTime = false; page = 1 },
            )
            MiuixIcon(
                Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) tr("收起历史成绩") else tr("展开历史成绩"),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.58f),
                modifier = Modifier.size(16.dp).rotate(if (expanded) 90f else 0f),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                displayRecords.forEachIndexed { index, record ->
                    ScoreHistoryRow(
                        record = record,
                        isBest = record.id == bestId,
                        alternate = index % 2 == 0,
                        accentColor = accentColor,
                        onDelete = { onDeleteRecord(record) },
                    )
                }
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiuixIconButton(onClick = { page = (validPage - 1).coerceAtLeast(1) }) {
                            MiuixIcon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = tr("上一页"),
                                tint = if (validPage > 1) accentColor else MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                                modifier = Modifier.size(18.dp).rotate(180f),
                            )
                        }
                        MiuixText(
                            "$validPage / $totalPages",
                            style = MiuixTheme.textStyles.footnote1,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        MiuixIconButton(onClick = { page = (validPage + 1).coerceAtMost(totalPages) }) {
                            MiuixIcon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = tr("下一页"),
                                tint = if (validPage < totalPages) accentColor else MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreHistorySortOption(
    text: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    MiuixText(
        text,
        style = MiuixTheme.textStyles.footnote2,
        color = if (selected) accentColor else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier
            .background(
                color = if (selected) accentColor.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
    )
}

@Composable
private fun ScoreHistoryRow(
    record: ScoreRecord,
    isBest: Boolean,
    alternate: Boolean,
    accentColor: Color,
    onDelete: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val playedAt = Instant.ofEpochMilli(record.playedAt).atZone(ZoneId.systemDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    isBest -> Modifier
                        .squircleSurface(color = accentColor.copy(alpha = 0.1f), cornerRadius = 8.dp, extension = SquircleExtension)
                        .squircleBorder(1.5.dp, accentColor, 8.dp, SquircleExtension)
                    alternate -> Modifier.background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                    else -> Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(72.dp)) {
            MiuixText(playedAt.format(dateFormatter), style = MiuixTheme.textStyles.footnote1, fontWeight = FontWeight.Bold)
            MiuixText(playedAt.format(timeFormatter), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                MiuixText(
                    record.rank,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.Bold,
                    color = scoreRankColor(record.rank) ?: MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                MiuixText(formatScore(record.score), style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Bold)
            }
            RecordStatusBadges(
                clear = record.clear,
                fullCombo = record.fullCombo,
                fullChain = record.fullChain,
                accentColor = accentColor,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        MiuixIconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            MiuixIcon(
                Icons.Rounded.Delete,
                contentDescription = tr("删除成绩记录"),
                tint = MiuixTheme.colorScheme.error.copy(alpha = 0.65f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ScoreEntrySheet(
    visible: Boolean,
    song: CatalogSong,
    sheet: CatalogSheet?,
    bestRecord: BestScoreSummary?,
    saving: Boolean,
    onSave: (Int, ClearType, FullComboType?, FullChainType?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetKey = sheet?.let { song.sheetKey(it) }
    var scoreText by rememberSaveable(sheetKey) { mutableStateOf("") }
    var clear by rememberSaveable(sheetKey) { mutableStateOf(ClearType.Clear.wireValue) }
    var fullCombo by rememberSaveable(sheetKey) { mutableStateOf<String?>(null) }
    var fullChain by rememberSaveable(sheetKey) { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(visible, sheetKey) {
        if (visible) {
            scoreText = ""
            clear = ClearType.Clear.wireValue
            fullCombo = null
            fullChain = null
        }
    }
    val parsedScore = scoreText.toIntOrNull()
    val isValid = parsedScore != null && ChunithmScoreRules.isValid(parsedScore)
    val submit: () -> Unit = {
        focusManager.clearFocus()
        if (!saving && isValid) {
            onSave(
                parsedScore,
                ClearType.fromWire(clear),
                FullComboType.fromWire(fullCombo),
                FullChainType.fromWire(fullChain),
            )
        }
    }
    ExpandableBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        expandActionLabel = tr("展开"),
        collapseActionLabel = tr("收起到半屏"),
        expandedStateDescription = tr("已全屏展开"),
        halfExpandedStateDescription = tr("半屏"),
        header = {
            MiuixIconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                MiuixIcon(Icons.Rounded.Close, contentDescription = tr("取消"))
            }
            MiuixText(tr("记录成绩"), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            if (isValid && !saving) {
                MiuixIconButton(onClick = submit, modifier = Modifier.align(Alignment.CenterEnd)) {
                    MiuixIcon(Icons.Rounded.Check, contentDescription = tr("保存"), tint = MiuixTheme.colorScheme.primary)
                }
            }
        },
    ) { topInset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = topInset + 12.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScoreEntrySongCard(song = song, sheet = sheet)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTitle(
                        text = tr("成绩"),
                        insideMargin = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    )
                    DetailCard(
                        color = MiuixTheme.colorScheme.surfaceContainer,
                        borderColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.12f),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MiuixText(
                            parsedScore?.takeIf { ChunithmScoreRules.isValid(it) }?.let(ChunithmScoreRules::rank) ?: "-",
                            style = MiuixTheme.textStyles.title1,
                            fontWeight = FontWeight.Bold,
                            color = parsedScore?.takeIf { ChunithmScoreRules.isValid(it) }?.let { scoreRankColor(ChunithmScoreRules.rank(it)) }
                                ?: MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        MiuixTextField(
                            value = scoreText,
                            onValueChange = { value ->
                                if (value.length <= 7 && value.all(Char::isDigit) && (value.toIntOrNull() ?: 0) <= ChunithmScoreRules.maximumScore) {
                                    scoreText = value
                                }
                            },
                            label = tr("分数"),
                            useLabelAsPlaceholder = true,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        }
                    }
                    SmallTitle(
                        text = tr("状态"),
                        insideMargin = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    )
                    DetailCard(
                        color = MiuixTheme.colorScheme.surfaceContainer,
                        borderColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.12f),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScoreStatusDropdown(
                                title = tr("CLEAR 状态"),
                                items = ClearType.entries.map { it.displayName },
                                selectedIndex = ClearType.entries.indexOf(ClearType.fromWire(clear)),
                                onSelectedIndexChange = { index -> clear = ClearType.entries[index].wireValue },
                            )
                            ScoreStatusDropdown(
                                title = tr("COMBO 状态"),
                                items = listOf(tr("无")) + FullComboType.entries.map { it.displayName },
                                selectedIndex = fullCombo?.let { value -> FullComboType.entries.indexOf(FullComboType.fromWire(value)) + 1 } ?: 0,
                                onSelectedIndexChange = { index -> fullCombo = FullComboType.entries.getOrNull(index - 1)?.wireValue },
                            )
                            ScoreStatusDropdown(
                                title = tr("CHAIN 状态"),
                                items = listOf(tr("无")) + FullChainType.entries.map { tr(it.displayName) },
                                selectedIndex = fullChain?.let { value -> FullChainType.entries.indexOf(FullChainType.fromWire(value)) + 1 } ?: 0,
                                onSelectedIndexChange = { index -> fullChain = FullChainType.entries.getOrNull(index - 1)?.wireValue },
                            )
                        }

                    }
                }
            }
            bestRecord?.let { best ->
                item {
                    DetailCard(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        borderColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.08f),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            MiuixText(tr("当前最佳"), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                MiuixText(formatScore(best.score), style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Bold)
                                MiuixText(best.rank, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Bold, color = scoreRankColor(best.rank) ?: MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            RecordStatusBadges(best.clear, best.fullCombo, best.fullChain, MiuixTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item {
                MiuixButton(
                    onClick = submit,
                    enabled = isValid && !saving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = MiuixButtonDefaults.buttonColorsPrimary(),
                ) {
                    MiuixIcon(if (saving) Icons.Rounded.Timer else Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    MiuixText(if (saving) tr("保存中…") else tr("保存成绩"))
                }
            }
        }
    }
}

@Composable
private fun ScoreStatusDropdown(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MiuixText(
            title,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .squircleSurface(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        cornerRadius = 14.dp,
                        extension = SquircleExtension,
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixText(
                    items.getOrElse(selectedIndex) { items.firstOrNull().orEmpty() },
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                MiuixIcon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            WindowListPopup(
                show = expanded,
                alignment = PopupPositionProvider.Align.End,
                enableWindowDim = true,
                onDismissRequest = { expanded = false },
            ) {
                ListPopupColumn {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index == selectedIndex) MiuixTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent,
                                )
                                .selectable(
                                    selected = index == selectedIndex,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelectedIndexChange(index)
                                        expanded = false
                                    },
                                )
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiuixText(item, modifier = Modifier.weight(1f))
                            if (index == selectedIndex) {
                                MiuixIcon(Icons.Rounded.Check, contentDescription = null, tint = MiuixTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreEntrySongCard(song: CatalogSong, sheet: CatalogSheet?) {
    val chartColor = sheet?.let { difficultyColor(it.difficulty) } ?: MiuixTheme.colorScheme.primary
    DetailCard(
        color = SongVisualUtils.detailColors(chartColor, SongVisualUtils.isDarkTheme(MiuixTheme.colorScheme.background)).surface,
        borderColor = chartColor.copy(alpha = 0.18f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                MiuixText(song.title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MiuixText(song.artist.ifBlank { tr("未知艺术家") }, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            sheet?.let {
                Column(horizontalAlignment = Alignment.End) {
                    MiuixText(it.difficulty.uppercase(Locale.ROOT), style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Bold, color = chartColor)
                    MiuixText((it.internalLevelValue ?: it.levelValue)?.let { value -> String.format(Locale.ROOT, "%.1f", value) } ?: it.level, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, color = chartColor)
                }
            }
        }
    }
}

@Composable
private fun DeleteScoreRecordDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    WindowDialog(
        show = true,
        title = tr("删除成绩记录"),
        summary = tr("确定删除这条历史成绩吗？"),
        onDismissRequest = onDismiss,
        outsideMargin = DpSize(24.dp, 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiuixButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = MiuixButtonDefaults.buttonColors(),
            ) {
                MiuixText(tr("取消"))
            }
            MiuixButton(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = MiuixButtonDefaults.buttonColorsPrimary(),
            ) {
                MiuixText(tr("删除"))
            }
        }
    }
}

private fun formatScore(score: Int): String = String.format(Locale.ROOT, "%,d", score)

private fun formatRating(rating: Double): String = String.format(Locale.ROOT, "%.2f", rating)

internal fun scoreRankColor(rank: String): Color? = when (rank.uppercase(Locale.ROOT)) {
    "SSS+", "SSS" -> Color(0xFFFFD900)
    "SS+", "SS" -> Color(0xFFFFBF00)
    "S+", "S" -> Color(0xFFFF9900)
    "AAA" -> Color(0xFFCC99FF)
    "AA" -> Color(0xFF99CCFF)
    "A" -> Color(0xFF80E680)
    else -> null
}

@Composable
private fun EmptyChartState() {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        MiuixText(tr("暂无谱面数据"), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun DetailCard(
    color: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    backgroundBrush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .squircleSurface(color = color, cornerRadius = 18.dp, extension = SquircleExtension)
            .then(
                backgroundBrush?.let { brush ->
                    Modifier.background(brush, RoundedCornerShape(18.dp))
                } ?: Modifier,
            )
            .squircleBorder(0.5.dp, borderColor.copy(alpha = 0.48f), 18.dp, SquircleExtension)
            .then(modifier)
            .padding(16.dp),
        content = content,
    )
}

private enum class CoverAction { Download, Copy, Share }

private fun performCoverAction(context: Context, source: File?, title: String, action: CoverAction, onMessage: (String) -> Unit) {
    if (source == null || !source.isFile) return
    runCatching {
        val uri = prepareShareUri(context, source, title)
        when (action) {
            CoverAction.Copy -> {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                    ?: error(tr("Clipboard unavailable"))
                clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, title, uri))
                onMessage(tr("已复制封面"))
            }
            CoverAction.Share -> {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = context.contentResolver.getType(uri) ?: "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, title, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, tr("分享封面")))
            }
            CoverAction.Download -> downloadCover(context, source, title, onMessage)
        }
    }.onFailure { onMessage(tr("封面操作失败")) }
}

private fun prepareShareUri(context: Context, source: File, title: String): Uri {
    val directory = File(context.cacheDir, "shared-covers").apply { mkdirs() }
    val destination = File(directory, "${safeFileName(title)}.${source.extension.ifBlank { "jpg" }}")
    source.copyTo(destination, overwrite = true)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destination)
}

private fun downloadCover(context: Context, source: File, title: String, onMessage: (String) -> Unit) {
    val extension = source.extension.ifBlank { "jpg" }
    val mimeType = when (extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, "${safeFileName(title)}.$extension")
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: error(tr("Unable to create download"))
    context.contentResolver.openOutputStream(uri)?.use { output -> source.inputStream().use { input -> input.copyTo(output) } }
        ?: error(tr("Unable to write download"))
    onMessage(tr("已保存到下载"))
}

private fun copyText(context: Context, value: String, onMessage: (String) -> Unit) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(value, value))
    onMessage(tr("已复制"))
}

private fun openExternalSearch(context: Context, url: String): Boolean = runCatching {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}.isSuccess

private fun safeFileName(value: String): String = value.replace(Regex("[^\\p{L}\\p{N}._-]+"), "_").trim('_').ifBlank { "chunithm-cover" }

private fun songRegionAvailable(song: CatalogSong, region: String): Boolean = when {
    region.equals("cn", true) -> song.regionOverrides["cn"]?.available == true
    else -> song.sheets.any { it.regions[region] == true }
}

private fun typeOrder(value: String): Int = CHART_TYPE_ORDER.indexOfFirst { it.equals(value, true) }.let { if (it >= 0) it else CHART_TYPE_ORDER.size }

private fun chartTypeLabel(value: String): String = when (value.lowercase()) {
    "std", "standard" -> "STD"
    "we" -> "World's End"
    else -> value
}

private fun detailDifficultyOrder(value: String): Int = DIFFICULTIES.indexOfFirst { it.equals(value, true) }.let { if (it >= 0) it else 999 }
