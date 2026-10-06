package org.rhythmeta.chunithmd.ui.collections

import org.rhythmeta.chunithmd.ui.catalog.rememberSongCoverNavigation
import org.rhythmeta.chunithmd.ui.catalog.SongCoverDecorations

import org.rhythmeta.chunithmd.shared.localization.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import org.rhythmeta.chunithmd.collection.CollectionCard
import org.rhythmeta.chunithmd.collection.SongCollection
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.ui.catalog.*
import org.rhythmeta.chunithmd.ui.components.ZoomableCoverImage
import org.rhythmeta.chunithmd.ui.components.squircleShape
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CollectionSummaryCard(collection: SongCollection, previews: List<CollectionCard>, jacketBaseUrl: String, localJacketPath: (String) -> String?, onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember(collection.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(0.dp), cornerRadius = 16.dp) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.clip(squircleShape(12.dp)).combinedClickable(onClick = onOpen, onLongClick = { menuExpanded = true })
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Folder, null, tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(collection.name, style = MiuixTheme.textStyles.body1, maxLines = 1)
                            Text(tr("{0} 张谱面", collection.entries.size), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1)
                        }
                    }
                    if (previews.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            previews.forEach { card ->
                                val shape = squircleShape(10.dp)
                                CollectionCover(card.song, jacketBaseUrl, localJacketPath, Modifier.size(56.dp).clip(shape).border(2.dp, chartBrush(requireNotNull(card.sheet)), shape))
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.5f))
            }
            CollectionContextMenu(menuExpanded, { menuExpanded = false }) {
                CollectionMenuItem(tr("分享收藏夹"), Icons.Rounded.Share) { menuExpanded = false; onShare() }
                CollectionMenuItem(tr("删除收藏夹"), Icons.Rounded.DeleteOutline, destructive = true) { menuExpanded = false; onDelete() }
            }
        }
    }
}

@Composable
internal fun CollectionChartCard(
    card: CollectionCard,
    grid: Boolean,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    gridImageSize: Int? = null,
    gridColumns: Int = 3,
    animateCoverChanges: Boolean = false,
    progressScale: () -> Float = { 1f },
    filler: Boolean = false,
) {
    var menuExpanded by remember(card.entry.key) { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().then(if (filler) Modifier.clearAndSetSemantics {} else Modifier)) {
        val song = card.song
        val sheet = card.sheet
        if (song == null || sheet == null) {
            Card(Modifier.fillMaxWidth().combinedClickable(enabled = !filler, onClick = { if (song != null) onOpen() }, onLongClick = { menuExpanded = true }), cornerRadius = 14.dp, insideMargin = PaddingValues(14.dp)) {
                Text(card.entry.songId)
                Text("${card.entry.chartType.uppercase()} · ${card.entry.difficulty.uppercase()}", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        } else if (grid) {
            val coverNavigation = rememberSongCoverNavigation(song.songId, onOpen, 8.dp, key = card.entry.key, enabled = !filler,
                cardColor = MiuixTheme.colorScheme.surfaceVariant)
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).then(coverNavigation.modifier).clip(squircleShape(8.dp))
                    .semantics { contentDescription = "${CatalogSongFormatter.displayTitle(song)} ${sheet.difficulty.uppercase()}" }
                    .combinedClickable(enabled = !filler, onClick = coverNavigation::open, onLongClick = { menuExpanded = true }),
            ) {
                CollectionCover(song, jacketBaseUrl, localJacketPath, Modifier.fillMaxSize(), gridImageSize, animateCoverChanges,
                    onPainterChanged = coverNavigation::onPainter, fallbackPainter = coverNavigation.source.painter)
                SongCoverDecorations(coverNavigation) {
                    Row(
                        Modifier.align(Alignment.BottomEnd).padding(if (gridColumns == 3) 6.dp else 4.dp)
                            .graphicsLayer {
                                scaleX = progressScale()
                                scaleY = scaleX
                                transformOrigin = TransformOrigin(1f, 1f)
                            }
                            .squircleSurface(
                                if (MiuixTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF9F7FC).copy(alpha = 0.88f)
                                else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f), 50.dp,
                            ).padding(horizontal = if (gridColumns == 3) 6.dp else 4.dp,
                                vertical = if (gridColumns == 3) 3.dp else 2.dp),
                    ) { SongScoreProgressDot(sheet, null) }
                }
            }
        } else {
            SongCard(song, jacketBaseUrl, localJacketPath, emptyMap(), onClick = onOpen, actualSheet = sheet, onLongClick = { menuExpanded = true })
        }
        CollectionContextMenu(menuExpanded && !filler, { menuExpanded = false }) {
            CollectionMenuItem(tr("移出收藏夹"), Icons.Rounded.DeleteOutline, destructive = true) { menuExpanded = false; onDelete() }
        }
    }
}

@Composable
private fun CollectionContextMenu(show: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    OverlayListPopup(
        show = show, popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
        alignment = PopupPositionProvider.Align.TopEnd, enableWindowDim = true, onDismissRequest = onDismiss,
    ) { ListPopupColumn { content() } }
}

@Composable
private fun CollectionMenuItem(label: String, icon: ImageVector, destructive: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (destructive) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

@Composable
private fun chartBrush(sheet: CatalogSheet): Brush = when {
    sheet.type.equals("we", true) -> Brush.horizontalGradient(WORLDS_END_GRADIENT_COLORS)
    sheet.difficulty.equals("ultima", true) -> ultimaStripedBrush()
    else -> SolidColor(difficultyColor(sheet.difficulty))
}

@Composable
private fun CollectionCover(
    song: CatalogSong?, baseUrl: String, localPath: (String) -> String?, modifier: Modifier,
    imageSize: Int? = null, animateCoverChanges: Boolean = false,
    onPainterChanged: ((androidx.compose.ui.graphics.painter.Painter?) -> Unit)? = null,
    fallbackPainter: androidx.compose.ui.graphics.painter.Painter? = null,
) {
    val model = remember(song?.imageName, baseUrl, localPath) {
        song?.imageName?.let { name -> localPath(name)?.let(::File) ?: baseUrl.takeIf { it.isNotBlank() }?.let { "${it.trimEnd('/')}/${name.trimStart('/')}" } }
    }
    val coverModifier = modifier.background(MiuixTheme.colorScheme.surfaceVariant)
    if (imageSize != null) {
        ZoomableCoverImage(model, imageSize, animateCoverChanges, coverModifier,
            onPainterChanged = onPainterChanged, fallbackPainter = fallbackPainter)
    } else {
        AsyncImage(model, null, coverModifier, contentScale = ContentScale.Crop,
            placeholder = fallbackPainter, error = fallbackPainter, onSuccess = { onPainterChanged?.invoke(it.painter) })
    }
}

@Composable
fun CollectionsHomeCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.height(140.dp), cornerRadius = 16.dp, insideMargin = PaddingValues(16.dp), onClick = onClick) {
        Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(30.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        Spacer(Modifier.height(12.dp))
        Text(tr("收藏夹"), style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
        Text(tr("整理喜爱的歌曲谱面"), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
