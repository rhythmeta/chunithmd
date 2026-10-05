package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Each rendered chart has its own anchor, even when several cells show the same song. */
internal class SongCoverSource(
    val songId: String,
    val identity: String,
    val cornerRadius: Dp,
    val saturation: Float,
    val imageAlpha: Float,
) {
    var cardBounds by mutableStateOf<Rect?>(null)
    var cardColor by mutableStateOf<Color?>(null)
    var cardCornerRadius by mutableStateOf(0.dp)
    var bodyBounds by mutableStateOf<Rect?>(null)
    var bodySize by mutableStateOf(IntSize.Zero)
    var bodyContent by mutableStateOf<(@Composable (Boolean) -> Unit)?>(null)
    var bounds by mutableStateOf<Rect?>(null)
    var layoutSize by mutableStateOf(IntSize.Zero)
    var painter by mutableStateOf<Painter?>(null)
    var decorations by mutableStateOf<(@Composable BoxScope.() -> Unit)?>(null)
}

internal class SongNavigationHost(
    val active: CatalogCoverTransition?,
    val openCover: (SongCoverSource) -> Unit,
    val openList: (String, CatalogListTransitionSource) -> Unit,
)

internal val LocalSongNavigationHost = staticCompositionLocalOf<SongNavigationHost?> { null }

internal class SongCoverNavigation(
    val source: SongCoverSource,
    private val host: SongNavigationHost?,
    private val fallback: () -> Unit,
) {
    val modifier: Modifier get() = Modifier.onGloballyPositioned {
        source.bounds = it.catalogBoundsInRoot()
        source.layoutSize = it.size
    }.graphicsLayer {
        alpha = if (host?.active?.genericSource === source && host.active.hidesSource) 0f else 1f
    }
    val cardModifier: Modifier get() = Modifier.onGloballyPositioned {
        source.cardBounds = it.catalogBoundsInRoot()
    }.graphicsLayer {
        alpha = if (host?.active?.genericSource === source && host.active.hidesSource) 0f else 1f
    }
    fun onPainter(painter: Painter?) { if (painter != null) source.painter = painter }
    fun open() {
        val bounds = source.bounds
        if (host != null && source.painter != null && bounds != null && bounds.width > 0f && bounds.height > 0f) {
            host.openCover(source)
        } else fallback()
    }
}

@Composable
internal fun rememberSongCoverNavigation(
    songId: String,
    onClick: () -> Unit,
    cornerRadius: Dp = 0.dp,
    key: Any? = songId,
    enabled: Boolean = true,
    saturation: Float = 1f,
    imageAlpha: Float = 1f,
    cardColor: Color? = null,
    cardCornerRadius: Dp = 14.dp,
): SongCoverNavigation {
    val host = LocalSongNavigationHost.current.takeIf { enabled }
    val identity = "${currentCompositeKeyHashCode}:$songId:$key"
    val local = remember(identity) { SongCoverSource(songId, identity, cornerRadius, saturation, imageAlpha) }
    val source = host?.active?.genericSource?.takeIf { it.identity == identity } ?: local
    SideEffect { source.cardColor = cardColor; source.cardCornerRadius = cardCornerRadius }
    return remember(source, host, onClick) { SongCoverNavigation(source, host, onClick) }
}

@Composable
internal fun SongCoverDecorations(navigation: SongCoverNavigation, content: @Composable BoxScope.() -> Unit) {
    val latest by rememberUpdatedState(content)
    val retained: @Composable BoxScope.() -> Unit = remember { { latest() } }
    SideEffect { navigation.source.decorations = retained }
    Box(Modifier.fillMaxSize(), content = content)
}

@Composable
internal fun SongCoverDecorationsOverlay(state: CatalogCoverTransition, progress: () -> Float, pageSize: () -> Size) {
    val source = state.genericSource ?: return
    val decorations = source.decorations ?: return
    if (source.layoutSize.width <= 0 || source.layoutSize.height <= 0) return
    val density = LocalDensity.current
    Box(Modifier.requiredSize(with(density) { source.layoutSize.width.toDp() },
        with(density) { source.layoutSize.height.toDp() }).clearAndSetSemantics {}.graphicsLayer {
        val fraction = progress()
        val target = state.detailBounds
        val bounds = if (target != null) state.sharedCoverBounds(target, fraction)
            else state.fallbackCoverBounds(pageSize(), fraction)
        alpha = catalogProgressBadgeAlpha(fraction) * (if (target != null) 1f else catalogCoverAlpha(fraction))
        transformOrigin = TransformOrigin(0f, 0f)
        scaleX = bounds.width / size.width.coerceAtLeast(1f)
        scaleY = bounds.height / size.height.coerceAtLeast(1f)
        translationX = bounds.left
        translationY = bounds.top
        clip = true
    }, content = decorations)
}

/** Capture the original row layout as one group; the hero's reserved space stays empty in flight. */
@Composable
internal fun SongCardTransitionContent(navigation: SongCoverNavigation, content: @Composable (Boolean) -> Unit) {
    val latest by rememberUpdatedState(content)
    val retained: @Composable (Boolean) -> Unit = remember { { showCover -> latest(showCover) } }
    SideEffect { navigation.source.bodyContent = retained }
    Box(Modifier.onGloballyPositioned {
        navigation.source.bodyBounds = it.catalogBoundsInRoot()
        navigation.source.bodySize = it.size
    }) { content(true) }
}

@Composable
internal fun SongCardContentOverlay(state: CatalogCoverTransition, progress: () -> Float) {
    val source = state.genericSource ?: return
    val body = source.bodyContent ?: return
    if (source.bodySize.width <= 0 || source.bodySize.height <= 0) return
    val density = LocalDensity.current
    Box(Modifier.requiredSize(with(density) { source.bodySize.width.toDp() },
        with(density) { source.bodySize.height.toDp() }).clearAndSetSemantics {}.graphicsLayer {
        val fraction = progress()
        val bounds = state.genericBodyBounds()
        alpha = if (bounds == null) 0f else catalogListContentAlpha(fraction)
        if (bounds != null) {
            translationX = bounds.left
            translationY = bounds.top + state.listContentTranslation(fraction) - state.listContentTranslation(0f)
        }
    }) { body(false) }
}
