package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.ui.components.ZoomableCoverImage
import org.rhythmeta.chunithmd.ui.components.ZoomableCoverGrid
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.progressSheets
import org.rhythmeta.chunithmd.shared.sheetKey
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CatalogPhotoGrid(
    modifier: Modifier,
    contentTopPadding: Dp,
    songs: List<CatalogSong>,
    scoresBySheetKey: Map<String, ScoreRecord>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    state: CatalogPhotoGridState,
    onSongClick: (CatalogSong) -> Unit,
    onCoverClick: ((CatalogSong, Rect, Painter?) -> Unit)? = null,
) {
    val progressScale = remember(state) { {
        state.zoom // Observe continuous scale without recomposing the progress dots.
        state.geometry?.let { it.maxCellSize / it.cellSize } ?: 1f
    } }
    ZoomableCoverGrid(modifier, contentTopPadding, songs, state, CatalogSong::songId) { song, columns, imageSize, filler ->
        PhotoTile(song, jacketBaseUrl, localJacketPath, scoresBySheetKey, progressScale, Modifier,
            filler = filler, imageSize = imageSize, columns = columns,
            animateCoverChanges = state.transforming, onClick = { bounds, painter ->
                if (bounds != null && painter != null && onCoverClick != null) onCoverClick(song, bounds, painter)
                else onSongClick(song)
            })
    }
}

@Composable
private fun PhotoTile(
    song: CatalogSong,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    scoresBySheetKey: Map<String, ScoreRecord>,
    progressScale: () -> Float,
    modifier: Modifier,
    filler: Boolean,
    imageSize: Int,
    columns: Int,
    animateCoverChanges: Boolean,
    onClick: (Rect?, Painter?) -> Unit,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var displayedPainter by remember(song.songId) { mutableStateOf<Painter?>(null) }
    val coverTransition = LocalCatalogCoverTransition.current?.takeIf { !filler && it.songId == song.songId }
    val title = remember(song) { CatalogSongFormatter.displayTitle(song) }
    val sheets = remember(song) { song.progressSheets() }
    val darkTheme = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val badgeBackground = if (darkTheme) Color(0xFFF9F7FC).copy(alpha = 0.88f)
        else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f)
    val badge = remember(sheets, scoresBySheetKey, song.songId, columns, badgeBackground) {
        CatalogProgressBadgeData(sheets.map { it to scoresBySheetKey[song.sheetKey(it)] }, columns,
            badgeBackground)
    }
    var badgeCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    SideEffect {
        badgeCoordinates?.takeIf { it.isAttached }?.let {
            coverTransition?.updateProgressBadge(badge, Rect(it.localToRoot(Offset.Zero),
                it.localToRoot(Offset(it.size.width.toFloat(), it.size.height.toFloat()))))
        }
    }
    val model = remember(song.imageName, jacketBaseUrl, localJacketPath) {
        localJacketPath(song.imageName)?.let(::File)
            ?: song.imageName.takeIf { it.isNotBlank() && jacketBaseUrl.isNotBlank() }
                ?.let { "${jacketBaseUrl.trimEnd('/')}/${it.trimStart('/')}" }
    }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier.fillMaxWidth().aspectRatio(1f)
            .graphicsLayer {
                // Share the covering entry's exact handoff boundary instead of isRunning.
                alpha = if (coverTransition?.hidesSource == true) 0f else 1f
            }
            .onGloballyPositioned {
                coordinates = it
                coverTransition?.updateSource(Rect(it.localToRoot(Offset.Zero),
                    it.localToRoot(Offset(it.size.width.toFloat(), it.size.height.toFloat()))))
            }
            .clipToBounds()
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !filler, interactionSource = interactionSource, indication = null, role = Role.Button, onClick = {
                val bounds = coordinates?.takeIf { it.isAttached }?.let {
                    Rect(it.localToRoot(Offset.Zero), it.localToRoot(Offset(it.size.width.toFloat(), it.size.height.toFloat())))
                }
                onClick(bounds, displayedPainter)
            })
            .then(if (filler) Modifier.clearAndSetSemantics {} else Modifier.semantics(mergeDescendants = true) {
                contentDescription = listOf(title, song.artist).filter(String::isNotBlank).joinToString(", ")
            }),
    ) {
        Icon(Icons.Rounded.MusicNote, null,
            Modifier.align(Alignment.Center).size(24.dp).clearAndSetSemantics {},
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        ZoomableCoverImage(model, imageSize, animateCoverChanges, Modifier.fillMaxSize(),
            onPainterChanged = { displayedPainter = it },
            fallbackPainter = coverTransition?.coverPainter)
        if (sheets.isNotEmpty()) {
            CatalogProgressBadge(badge,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(if (columns == 3) 6.dp else 4.dp)
                    .graphicsLayer {
                        // Keep 8 dp dots readable in both densities while covers scale continuously.
                        scaleX = progressScale()
                        scaleY = scaleX
                        transformOrigin = TransformOrigin(1f, 1f)
                    }
                    .onGloballyPositioned {
                        badgeCoordinates = it
                        coverTransition?.updateProgressBadge(badge, Rect(it.localToRoot(Offset.Zero),
                            it.localToRoot(Offset(it.size.width.toFloat(), it.size.height.toFloat()))))
                    },
            )
        }
    }
}
