package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.progressSheets
import org.rhythmeta.chunithmd.shared.sheetKey
import top.yukonga.miuix.kmp.squircle.squircleSurface
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.shared.localization.tr
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
) {
    val scope = rememberCoroutineScope()
    val motion = remember(state, scope) { CatalogGridMotion(state, scope) }
    val scrollState = rememberScrollableState(state::scrollBy)
    val songIds = remember(songs) { songs.map(CatalogSong::songId) }
    val layoutState = remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(10)) }
    val progressScale = remember(state) { {
        state.zoom // Observe continuous scale without recomposing the progress dots.
        state.geometry?.let { it.maxCellSize / it.cellSize } ?: 1f
    } }
    val zoomIn = tr("放大封面")
    val zoomOut = tr("缩小封面")
    fun centerAnchor() = state.geometry?.let { state.anchorAt(Offset(it.width / 2f, state.viewportHeight / 2f)) }
    DisposableEffect(motion) { onDispose { motion.interrupt() } }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        motion.settle(centerAnchor())
    }
    SubcomposeLayout(
        state = layoutState,
        modifier = modifier.fillMaxSize().clipToBounds()
            .semantics {
                verticalScrollAxisRange = ScrollAxisRange({ state.scrollOffset }, { state.maxScroll })
                val count = catalogGridColumns(state.zoom)
                collectionInfo = CollectionInfo((songs.size + count - 1) / count, count)
                scrollBy { _, y -> state.scrollBy(-y); true }
                scrollToIndex { index -> state.requestScrollToItem(index); true }
                customActions = buildList {
                    if (state.zoom < 2f) add(CustomAccessibilityAction(zoomIn) {
                        motion.settle(centerAnchor(), (settledZoom(state.zoom) + 1f).coerceAtMost(2f)); true
                    })
                    if (state.zoom > 1f) add(CustomAccessibilityAction(zoomOut) {
                        motion.settle(centerAnchor(), (settledZoom(state.zoom) - 1f).coerceAtLeast(1f)); true
                    })
                }
            }
            .pointerInput(state, motion) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    motion.interrupt()
                    var pinching = false
                    var anchor: PhotoGridAnchor? = null
                    var focus = down.position
                    var transformPointers = emptySet<PointerId>()
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pointers = event.changes.filter { it.pressed }.mapTo(mutableSetOf()) { it.id }
                        if (pointers.size >= 2) {
                            val centroid = event.calculateCentroid()
                            if (centroid != Offset.Unspecified) focus = centroid
                            if (!pinching) {
                                pinching = true
                                anchor = state.anchorAt(focus, centerOnCover = true)
                                state.transforming = true
                                scope.launch { scrollState.stopScroll(MutatePriority.PreventUserInput) }
                            }
                            val geometry = state.geometry
                            val pivot = anchor
                            if (geometry != null && pivot != null) {
                                val nextZoom = geometry.zoomForSize(geometry.cellSize * if (pointers == transformPointers) event.calculateZoom() else 1f)
                                state.transform(nextZoom, pivot)
                            }
                        }
                        transformPointers = pointers
                        // Consume until every finger is lifted; a pinch must never become a cover tap.
                        if (pinching) event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    motion.settle(anchor ?: state.anchorAt(focus, centerOnCover = true))
                }
            }
            .scrollable(scrollState, Orientation.Vertical, enabled = !state.transforming),
    ) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val grid = state.prepareLayout(width.toFloat(), height.toFloat(), 2.dp.toPx(),
            (contentTopPadding + 2.dp).toPx(), 96.dp.toPx(), songIds)
        val top = state.scrollOffset - state.topPadding
        val fillEdges = state.transforming || state.zoom != settledZoom(state.zoom) || state.outsideScrollBounds
        val slots = grid.visibleSlots(-grid.step, top - grid.step,
            width + grid.step, top + height + grid.step, fillEdges,
            fillRows = state.outsideScrollBounds)
        // Measure/decode at a fixed maximum size; only placement and layer transforms change each frame.
        val baseSize = grid.maxCellSize.roundToInt().coerceAtLeast(1)
        val tiles = slots.map { slot ->
            val song = songs[slot.index]
            val placeable = subcompose(state.slotKey(slot)) {
                PhotoTile(song, jacketBaseUrl, localJacketPath, scoresBySheetKey, progressScale, Modifier,
                    filler = slot.filler, imageSize = baseSize, columns = grid.columns, onClick = { onSongClick(song) })
            }.single().measure(Constraints.fixed(baseSize, baseSize))
            slot to placeable
        }
        layout(width, height) {
            tiles.forEach { (slot, placeable) ->
                val cell = grid.slotCell(slot.row, slot.column)
                val x = cell.x
                val y = cell.y + state.topPadding - state.scrollOffset
                val integerX = x.roundToInt()
                val integerY = y.roundToInt()
                placeable.placeWithLayer(integerX, integerY) {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = cell.size / baseSize
                    scaleY = scaleX
                    translationX = x - integerX
                    translationY = y - integerY
                }
            }
        }
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
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val title = remember(song) { CatalogSongFormatter.displayTitle(song) }
    val sheets = remember(song) { song.progressSheets() }
    val darkTheme = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val model = remember(song.imageName, jacketBaseUrl, localJacketPath) {
        localJacketPath(song.imageName)?.let(::File)
            ?: song.imageName.takeIf { it.isNotBlank() && jacketBaseUrl.isNotBlank() }
                ?.let { "${jacketBaseUrl.trimEnd('/')}/${it.trimStart('/')}" }
    }
    val image = remember(context, model, imageSize) {
        ImageRequest.Builder(context).data(model).size(imageSize).crossfade(false).build()
    }
    val imagePainter = rememberAsyncImagePainter(image)
    var displayedCover by remember { mutableStateOf<Painter?>(null) }
    val loadedCover = (imagePainter.state as? AsyncImagePainter.State.Success)?.painter
    LaunchedEffect(loadedCover) {
        // Keep the old cover while loading. Compose also fades memory-cache hits and can reverse mid-fade.
        if (loadedCover != null) displayedCover = loadedCover
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "coverPress")
    Box(
        modifier.fillMaxWidth().aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clipToBounds()
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !filler, interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .then(if (filler) Modifier.clearAndSetSemantics {} else Modifier.semantics(mergeDescendants = true) {
                contentDescription = listOf(title, song.artist).filter(String::isNotBlank).joinToString(", ")
            }),
    ) {
        Icon(Icons.Rounded.MusicNote, null,
            Modifier.align(Alignment.Center).size(24.dp).clearAndSetSemantics {},
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Crossfade(targetState = displayedCover, animationSpec = tween(180),
            modifier = Modifier.fillMaxSize(), label = "coverReplacement") { cover ->
            if (cover != null) {
                Image(cover, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
            }
        }
        if (sheets.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(if (columns == 3) 6.dp else 4.dp)
                    .graphicsLayer {
                        // Keep 8 dp dots readable in both densities while covers scale continuously.
                        scaleX = progressScale()
                        scaleY = scaleX
                        transformOrigin = TransformOrigin(1f, 1f)
                    }
                    .squircleSurface(
                        color = if (darkTheme) Color(0xFFF9F7FC).copy(alpha = 0.88f)
                            else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                        cornerRadius = 50.dp,
                    )
                    .padding(horizontal = if (columns == 3) 6.dp else 4.dp,
                        vertical = if (columns == 3) 3.dp else 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                sheets.forEach { sheet ->
                    SongScoreProgressDot(sheet, scoresBySheetKey[song.sheetKey(sheet)])
                }
            }
        }
    }
}
