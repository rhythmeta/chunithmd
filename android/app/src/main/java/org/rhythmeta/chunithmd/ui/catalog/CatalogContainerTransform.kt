package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import kotlin.math.max

/** Content phases share the navigation driver's progress, including interrupted and cancelled back. */
@Composable
internal fun CatalogContainerTransform(
    state: CatalogCoverTransition?,
    background: Color,
    content: @Composable () -> Unit,
) {
    if (state == null) {
        content()
        return
    }
    val navigation = LocalNavTransitionScope.current
    SideEffect { state.attach(navigation) }
    DisposableEffect(state) {
        onDispose { state.detach() }
    }
    Box(Modifier.fillMaxSize().drawBehind {
        val progress = catalogContainerProgress(navigation.relativeDepth)
        val color = if (state.usesListMotion) {
            val sourceColor = state.genericSource?.cardColor ?: state.listSource?.appearance?.surface
            // A card owns the whole starting container; its surface can blend into the page.
            if (sourceColor != null) lerp(sourceColor, background, (progress / 0.56f).coerceIn(0f, 1f))
                else background
        } else {
            // A jacket's translucent placeholder is not a page surface. Fade away the page
            // around the hero instead of spreading that pale placeholder over the whole container.
            background.copy(alpha = background.alpha * catalogDetailAlpha(progress))
        }
        drawRect(color)
    }) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val progress = catalogContainerProgress(navigation.relativeDepth)
            val bounds = state.bounds(size, progress)
            alpha = catalogDetailAlpha(progress)
            transformOrigin = TransformOrigin(0f, 0f)
            if (state.usesListMotion) {
                // One upward/downward axis for all detail content. The jacket alone stays shared.
                scaleX = 1f
                scaleY = 1f
                translationX = 0f
                translationY = state.listContentTranslation(progress)
            } else {
                // The grid retains its existing proportional container transform.
                val scale = max(bounds.width / size.width.coerceAtLeast(1f),
                    bounds.height / size.height.coerceAtLeast(1f))
                scaleX = scale
                scaleY = scale
                translationX = bounds.center.x - size.width * scale / 2f
                translationY = bounds.top
            }
        }) {
            Box(Modifier.fillMaxSize().onGloballyPositioned(state::updateDetailRoot)) {
                CompositionLocalProvider(LocalCatalogCoverTransition provides state) {
                    content()
                }
            }
        }
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            val progress = catalogContainerProgress(navigation.relativeDepth)
            val target = state.detailBounds
            val shared = target != null
            val coverAlpha = if (shared) {
                if (progress < 1f) 1f else 0f
            } else catalogCoverAlpha(progress)
            val painter = state.coverPainter
            if (coverAlpha > 0f && painter != null) {
                val bounds = if (target != null) state.sharedCoverBounds(target, progress)
                    else state.fallbackCoverBounds(size, progress)
                val intrinsic = painter.intrinsicSize
                val imageSize = if (intrinsic.isSpecified && intrinsic.width.isFinite() && intrinsic.height.isFinite() &&
                    intrinsic.width > 0f && intrinsic.height > 0f) intrinsic else Size(1f, 1f)
                val scale = max(bounds.width / imageSize.width, bounds.height / imageSize.height)
                val drawSize = Size(imageSize.width * scale, imageSize.height * scale)
                val visible = if (shared) state.sharedCoverBounds(state.detailClip ?: target, progress) else bounds
                val sourceRadius = state.coverCornerRadiusPx(this)
                val radius = if (shared) interpolate(sourceRadius, 26.dp.toPx(), progress) else sourceRadius
                val path = Path().apply { addRoundRect(RoundRect(bounds, CornerRadius(radius))) }
                // Keep one opaque jacket moving between the two real positions. Only the rest of
                // the page fades; the in-page jacket takes over at the exact settled endpoint.
                clipRect(visible.left, visible.top, visible.right, visible.bottom) {
                    clipPath(path) {
                        if (!state.usesListMotion) {
                            state.genericSource?.cardColor?.let { surface ->
                                drawRect(surface, topLeft = bounds.topLeft, size = bounds.size, alpha = coverAlpha)
                            }
                        }
                        translate(bounds.center.x - drawSize.width / 2f, bounds.center.y - drawSize.height / 2f) {
                            val source = state.genericSource
                            val imageAlpha = interpolate(source?.imageAlpha ?: 1f, 1f, progress)
                            val filter = source?.takeIf { it.saturation != 1f }?.let {
                                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(interpolate(it.saturation, 1f, progress)) })
                            }
                            with(painter) { draw(drawSize, alpha = coverAlpha * imageAlpha, colorFilter = filter) }
                        }
                    }
                }
            }
        }
        SongCardContentOverlay(state, { catalogContainerProgress(navigation.relativeDepth) })
        SongCoverDecorationsOverlay(state, { catalogContainerProgress(navigation.relativeDepth) },
            { Size(navigation.layoutSize.width.toFloat(), navigation.layoutSize.height.toFloat()) })
        state.progressBadge?.takeIf { it.entries.isNotEmpty() }?.let { badge ->
            CatalogProgressBadge(badge, Modifier.clearAndSetSemantics {}.graphicsLayer {
                val progress = catalogContainerProgress(navigation.relativeDepth)
                val target = state.detailBounds
                val pageSize = Size(navigation.layoutSize.width.toFloat(), navigation.layoutSize.height.toFloat())
                val cover = if (target != null) state.sharedCoverBounds(target, progress)
                    else state.bounds(pageSize, progress)
                val bounds = state.badgeBounds(cover)
                alpha = if (bounds == null) 0f else catalogProgressBadgeAlpha(progress) *
                    (if (target != null) 1f else catalogCoverAlpha(progress))
                if (bounds != null) {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = bounds.width / size.width.coerceAtLeast(1f)
                    scaleY = bounds.height / size.height.coerceAtLeast(1f)
                    translationX = bounds.left
                    translationY = bounds.top
                }
            })
        }
        if (state.listSource != null) {
            CatalogListDecorationsOverlay(state, { catalogContainerProgress(navigation.relativeDepth) })
            CatalogListTextOverlay(state, { catalogContainerProgress(navigation.relativeDepth) },
                Modifier.fillMaxSize().clearAndSetSemantics {})
        }
    }
}
