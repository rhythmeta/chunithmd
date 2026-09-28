package org.rhythmeta.chunithmd.ui.catalog

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import coil.compose.AsyncImage
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.shared.CatalogNoteCounts
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

private val DETAIL_REGIONS = listOf("jp" to "JP", "intl" to "INTL", "cn" to "CN")
private val CHART_TYPE_ORDER = listOf("std", "standard", "dx", "we", "utage")

@Composable
fun SongDetailScreen(
    song: CatalogSong?,
    loading: Boolean = false,
    aliases: List<String>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    contentTopPadding: Dp,
    topBarScrollConnection: NestedScrollConnection,
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    fun showMessage(message: String) {
        snackbarScope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    var jacketAccent by remember(song.songId) { mutableStateOf<Color?>(null) }
    val detailColors = jacketAccent?.let { detailColors(it, isDark) }
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
                    coverModel = coverModel,
                    coverFile = localCover,
                    surfaceColor = surfaceColor,
                    accentColor = accent,
                    onAccentColor = { jacketAccent = it },
                    onCopyText = { copyText(context, it, ::showMessage) },
                    onCoverAction = { action -> performCoverAction(context, localCover, song.title, action, ::showMessage) },
                )
            }
            // TODO: Connect community aliases after the shared account/community contract is available.
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
        MiuixText("歌曲不存在或目录尚未加载", style = MiuixTheme.textStyles.body1, textAlign = TextAlign.Center)
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
        MiuixText("正在加载歌曲目录", style = MiuixTheme.textStyles.body1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SongDetailHeader(
    song: CatalogSong,
    aliases: List<String>,
    coverModel: Any?,
    coverFile: File?,
    surfaceColor: Color,
    accentColor: Color,
    onAccentColor: (Color) -> Unit,
    onCopyText: (String) -> Unit,
    onCoverAction: (CoverAction) -> Unit,
) {
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
                        model = coverModel,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onSuccess = { result ->
                            runCatching {
                                result.result.drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
                            }.getOrNull()?.let { bitmap ->
                                averageBitmapColor(bitmap)?.let { color ->
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
            text = song.artist.ifBlank { "未知艺术家" },
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
                        color = accentColor,
                        modifier = Modifier
                            .squircleSurface(color = surfaceColor, cornerRadius = 50.dp, extension = SquircleExtension)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable { onCopyText(alias) },
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
            CoverActionMenuItem("下载封面", Icons.Rounded.Download, enabled) { onAction(CoverAction.Download) }
            CoverActionMenuItem("复制封面", Icons.Rounded.ContentCopy, enabled) { onAction(CoverAction.Copy) }
            CoverActionMenuItem("分享封面", Icons.Rounded.Share, enabled) { onAction(CoverAction.Share) }
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
        song.version?.takeIf(String::isNotBlank)?.let { it to Icons.Rounded.Album },
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
            RegionFlag("🇯🇵", "日本", songRegionAvailable(song, "jp"), accentColor)
            RegionFlag("🌏", "国际", songRegionAvailable(song, "intl"), accentColor)
            RegionFlag("🇨🇳", "中国", songRegionAvailable(song, "cn"), accentColor)
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
            MiuixIcon(Icons.Rounded.Search, contentDescription = "外部搜索", tint = accentColor, modifier = Modifier.size(20.dp))
            ExternalSearchButton(
                text = "YouTube",
                icon = Icons.Rounded.PlayArrow,
                onClick = {
                    if (!openExternalSearch(context, "https://www.youtube.com/results?search_query=chunithm+$encodedTitle")) {
                        onMessage("未找到可用的 YouTube 应用")
                    }
                },
                modifier = Modifier.weight(1f),
                surfaceColor = youtubeSurface,
                contentColor = youtubeContent,
            )
            ExternalSearchButton(
                text = "Bilibili",
                icon = Icons.Rounded.MusicNote,
                onClick = {
                    if (!openExternalSearch(context, "bilibili://search?keyword=chunithm+$encodedTitle")) {
                        onMessage("未找到可用的 Bilibili 应用")
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
private fun ChartDetailCard(song: CatalogSong, sheet: CatalogSheet, surfaceColor: Color, accentColor: Color) {
    var expanded by rememberSaveable(song.songId, sheet.type, sheet.difficulty) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "difficulty-chevron",
    )
    val chartAccent = if (sheet.type.equals("we", true)) difficultyColor("world's end") else difficultyColor(sheet.difficulty)
    val difficultyLabel = sheet.difficulty.trim().uppercase(Locale.ROOT).ifBlank { "未知难度" }
    val internalLevel = sheet.levelValue?.let { String.format(Locale.ROOT, "%.1f", it) }
        ?: sheet.level.ifBlank { "-" }
    DetailCard(
        color = surfaceColor,
        borderColor = chartAccent.copy(alpha = 0.58f),
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(50.dp)
                    .offset(x = (-16).dp)
                    .squircleSurface(
                        color = chartAccent,
                        cornerRadius = 50.dp,
                        extension = SquircleExtension,
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                MiuixText(
                    difficultyLabel,
                    style = MiuixTheme.textStyles.title3,
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
            MiuixText(internalLevel, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, color = chartAccent)
            Spacer(Modifier.width(6.dp))
            MiuixIcon(
                Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) "收起谱面" else "展开谱面",
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
                NoteCountSection(
                    songId = song.songId,
                    chartType = sheet.type,
                    difficulty = sheet.difficulty,
                    counts = sheet.noteCounts,
                )
            }
        }
    }
}

private data class NoteCountEntry(
    val label: String,
    val count: Int,
    val weight: Double,
    val color: Color,
)

@Composable
private fun NoteCountSection(
    songId: String,
    chartType: String,
    difficulty: String,
    counts: CatalogNoteCounts?,
) {
    if (counts == null) return
    val entries = listOfNotNull(
        counts.tap?.let { NoteCountEntry("TAP", it, 1.0, Color(0xFFFF2D78)) },
        counts.hold?.let { NoteCountEntry("HOLD", it, 2.0, Color(0xFFFF2D78)) },
        counts.slide?.let { NoteCountEntry("SLIDE", it, 3.0, Color(0xFF4D80FF)) },
        counts.touch?.let { NoteCountEntry("TOUCH", it, 1.0, Color(0xFF4D80FF)) },
        counts.breakCount?.let { NoteCountEntry("BREAK", it, 5.0, Color(0xFFFF9500)) },
    ).filter { it.count > 0 }
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
                "音符统计",
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            MiuixIcon(
                Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) "收起音符统计" else "展开音符统计",
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.58f),
                modifier = Modifier.size(16.dp).rotate(chevronRotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            val totalWeight = entries.sumOf { it.count * it.weight }
            Column {
                entries.forEachIndexed { index, entry ->
                    val fraction = if (totalWeight > 0.0) entry.count * entry.weight / totalWeight else 0.0
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
                                        maxOf(4.dp, maxWidth * fraction.toFloat()).coerceAtMost(maxWidth),
                                    )
                                    .background(entry.color.copy(alpha = 0.5f)),
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
                            "${(fraction * 100).toInt()}%",
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

@Composable
private fun EmptyChartState() {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        MiuixText("暂无谱面数据", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun DetailCard(
    color: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .squircleSurface(color = color, cornerRadius = 18.dp, extension = SquircleExtension)
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
                    ?: error("Clipboard unavailable")
                clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, title, uri))
                onMessage("已复制封面")
            }
            CoverAction.Share -> {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = context.contentResolver.getType(uri) ?: "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, title, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "分享封面"))
            }
            CoverAction.Download -> downloadCover(context, source, title, onMessage)
        }
    }.onFailure { onMessage("封面操作失败") }
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
        ?: error("Unable to create download")
    context.contentResolver.openOutputStream(uri)?.use { output -> source.inputStream().use { input -> input.copyTo(output) } }
        ?: error("Unable to write download")
    onMessage("已保存到下载")
}

private fun copyText(context: Context, value: String, onMessage: (String) -> Unit) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(value, value))
    onMessage("已复制")
}

private fun openExternalSearch(context: Context, url: String): Boolean = runCatching {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}.isSuccess

private fun safeFileName(value: String): String = value.replace(Regex("[^\\p{L}\\p{N}._-]+"), "_").trim('_').ifBlank { "chunithm-cover" }

private fun averageBitmapColor(bitmap: Bitmap): Color? {
    if (bitmap.width <= 0 || bitmap.height <= 0) return null
    val scaleFactor = minOf(1f, 96f / maxOf(bitmap.width, bitmap.height).toFloat())
    val sampledWidth = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
    val sampledHeight = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)
    val sampledBitmap = if (sampledWidth == bitmap.width && sampledHeight == bitmap.height) bitmap
    else bitmap.scale(sampledWidth, sampledHeight)
    return try {
        val pixels = IntArray(sampledWidth * sampledHeight)
        sampledBitmap.getPixels(pixels, 0, sampledWidth, 0, 0, sampledWidth, sampledHeight)
        var red = 0L
        var green = 0L
        var blue = 0L
        var alpha = 0L
        pixels.forEach { pixel ->
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
            alpha += android.graphics.Color.alpha(pixel)
        }
        val count = pixels.size.toFloat()
        Color(red / count / 255f, green / count / 255f, blue / count / 255f, alpha / count / 255f)
    } finally {
        if (sampledBitmap !== bitmap) sampledBitmap.recycle()
    }
}

private data class DetailColors(
    val background: Color,
    val surface: Color,
    val selectedSurface: Color,
    val accent: Color,
)

private fun detailColors(raw: Color, darkTheme: Boolean): DetailColors {
    val rawHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(raw.toArgb(), rawHsv)
    val accentHsv = rawHsv.copyOf().apply {
        this[1] = (this[1] * 0.85f).coerceIn(0.25f, 0.75f)
        this[2] = if (darkTheme) 0.82f else 0.62f
    }
    fun colorWith(saturationScale: Float, value: Float): Color {
        val hsv = rawHsv.copyOf().apply {
            this[1] = (this[1] * saturationScale).coerceIn(if (darkTheme) 0.08f else 0.03f, if (darkTheme) 0.30f else 0.18f)
            this[2] = value
        }
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
    val backgroundHsv = rawHsv.copyOf().apply {
        this[1] = if (darkTheme) (this[1] * 0.75f).coerceIn(0.20f, 0.45f) else (this[1] * 0.45f).coerceIn(0.08f, 0.30f)
        this[2] = if (darkTheme) (this[2] * 0.35f).coerceIn(0.12f, 0.28f) else (0.88f + (this[2] - 0.5f) * 0.08f).coerceIn(0.84f, 0.94f)
    }
    return DetailColors(
        background = Color(android.graphics.Color.HSVToColor(backgroundHsv)),
        surface = colorWith(if (darkTheme) 0.34f else 0.16f, if (darkTheme) 0.20f else 0.98f),
        selectedSurface = colorWith(if (darkTheme) 0.48f else 0.28f, if (darkTheme) 0.30f else 0.94f),
        accent = Color(android.graphics.Color.HSVToColor(accentHsv)),
    )
}

private fun songRegionAvailable(song: CatalogSong, region: String): Boolean = when {
    region.equals("cn", true) -> song.regionOverrides["cn"]?.available == true
    else -> song.sheets.any { it.regions[region] == true }
}

private fun typeOrder(value: String): Int = CHART_TYPE_ORDER.indexOfFirst { it.equals(value, true) }.let { if (it >= 0) it else CHART_TYPE_ORDER.size }

private fun chartTypeLabel(value: String): String = when (value.lowercase()) {
    "std", "standard" -> "STD"
    "dx" -> "DX"
    "we" -> "World's End"
    "utage" -> "Utage"
    else -> value
}

private fun detailDifficultyOrder(value: String): Int = DIFFICULTIES.indexOfFirst { it.equals(value, true) }.let { if (it >= 0) it else 999 }
