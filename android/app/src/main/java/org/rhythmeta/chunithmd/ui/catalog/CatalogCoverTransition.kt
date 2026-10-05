package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

internal val LocalCatalogCoverTransition = staticCompositionLocalOf<CatalogCoverTransition?> { null }

/** One navigation driver owns the page container and the optional shared jacket path. */
internal class CatalogCoverTransition(
    val songId: String,
    source: Rect,
    val coverPainter: Painter?,
    private val deviceCornerRadius: Dp,
    private val isReturning: () -> Boolean = { false },
    private val navigationOrigin: () -> Offset,
) {
    var isPresented by mutableStateOf(true)
        private set
    private var source by mutableStateOf(source)
    var genericSource by mutableStateOf<SongCoverSource?>(null)
    var listSource by mutableStateOf<CatalogListTransitionSource?>(null)
    private val sourceCover: Rect get() = (genericSource?.bounds ?: listSource?.coverBounds)?.translate(-navigationOrigin()) ?: source
    private val sourceContainer: Rect get() = (genericSource?.cardBounds ?: genericSource?.bounds ?: listSource?.cardBounds)?.translate(-navigationOrigin()) ?: source
    val usesListMotion: Boolean get() = listSource != null || genericSource?.cardBounds != null
    fun genericBodyBounds(): Rect? = genericSource?.bodyBounds?.translate(-navigationOrigin())

    fun listCardBounds(): Rect? = listSource?.cardBounds?.translate(-navigationOrigin())
    fun listBadgeBounds(): Rect? = listSource?.badgeBounds?.translate(-navigationOrigin())

    fun sourceText(element: CatalogTextElement): CatalogTextAnchor? = listSource?.texts?.get(element)?.let {
        it.copy(bounds = it.bounds.translate(-navigationOrigin()))
    }

    // Both outgoing and incoming text groups share this vertical displacement. Only the jacket
    // has its own persistent path; text never travels sideways to chase individual destinations.
    fun listContentTranslation(progress: Float): Float = sourceContainer.top * (1f - progress.coerceIn(0f, 1f))

    private var navigation by mutableStateOf<NavTransitionScope?>(null)
    val hidesSource: Boolean get() = isPresented && navigation?.let {
        catalogContainerProgress(it.relativeDepth) > 0f
    } == true

    private var detailRoot: LayoutCoordinates? = null
    private var detailCover: LayoutCoordinates? = null
    private var detailViewportTop = 0f
    var detailBounds by mutableStateOf<Rect?>(null)
        private set
    var detailClip by mutableStateOf<Rect?>(null)
        private set
    val hidesDetailCover: Boolean get() = detailBounds != null && navigation?.let {
        catalogContainerProgress(it.relativeDepth) < 1f
    } == true

    fun updateDetailRoot(coordinates: LayoutCoordinates) {
        detailRoot = coordinates
        updateDetailBounds()
    }

    fun updateDetailCover(coordinates: LayoutCoordinates?, viewportTop: Float) {
        detailCover = coordinates
        detailViewportTop = viewportTop
        updateDetailBounds()
    }

    private fun updateDetailBounds() {
        val root = detailRoot?.takeIf { it.isAttached }
        val cover = detailCover?.takeIf { it.isAttached }
        if (root == null || cover == null) {
            detailBounds = null
            detailClip = null
            return
        }
        // Measure inside the scaled page, so its animated ancestor never feeds back into the anchor.
        val bounds = root.localBoundingBoxOf(cover, clipBounds = false)
        val visible = root.localBoundingBoxOf(cover, clipBounds = true)
            .intersect(Rect(0f, detailViewportTop, root.size.width.toFloat(), root.size.height.toFloat()))
        detailBounds = bounds.takeIf { visible.width > 0f && visible.height > 0f }
        detailClip = visible.takeIf { detailBounds != null }
    }

    fun sharedCoverBounds(target: Rect, progress: Float): Rect = catalogSharedCoverBounds(sourceCover, target, progress)

    var progressBadge by mutableStateOf<CatalogProgressBadgeData?>(null)
        private set
    private var progressBadgeBounds by mutableStateOf<Rect?>(null)

    fun updateProgressBadge(badge: CatalogProgressBadgeData, boundsInRoot: Rect) {
        progressBadge = badge
        progressBadgeBounds = boundsInRoot.translate(-navigationOrigin())
    }

    fun badgeBounds(coverBounds: Rect): Rect? = progressBadgeBounds?.let { badge ->
        val scale = coverBounds.width / source.width.coerceAtLeast(1f)
        Rect(
            coverBounds.left + (badge.left - source.left) * scale,
            coverBounds.top + (badge.top - source.top) * scale,
            coverBounds.left + (badge.right - source.left) * scale,
            coverBounds.top + (badge.bottom - source.top) * scale,
        )
    }

    fun updateSource(boundsInRoot: Rect) {
        source = boundsInRoot.translate(-navigationOrigin())
    }

    fun attach(scope: NavTransitionScope) {
        navigation = scope
        isPresented = true
    }
    fun detach() {
        isPresented = false
        navigation = null
        detailRoot = null
        detailCover = null
        detailBounds = null
        detailClip = null
    }

    fun bounds(pageSize: Size, progress: Float): Rect = catalogContainerBounds(sourceContainer, pageSize, progress)
    fun fallbackCoverBounds(pageSize: Size, progress: Float): Rect = catalogContainerBounds(sourceCover, pageSize, progress)

    fun coverCornerRadiusPx(density: Density): Float = with(density) {
        val cover = genericSource
        if (cover != null) {
            // Pinch grids scale their cells outside layout; match the visible jacket's radius.
            val scale = if (cover.layoutSize.width > 0) sourceCover.width / cover.layoutSize.width else 1f
            cover.cornerRadius.toPx() * scale
        } else if (listSource != null) 12.dp.toPx() else 0f
    }

    // Both layouts share the driver timing, so cover, labels, badges and color settle together.
    // Keep velocity continuity for gesture commits; a tween here would introduce a release hitch.
    private val commitMotion = NavSettleSpec.Spring(dampingRatio = 1f, stiffness = 700f)
    private val enterMotion = NavMotion(
        commit = commitMotion,
        programmatic = NavSettleSpec.Tween(400, FastOutSlowInEasing),
    )
    private val returnMotion = NavMotion(
        commit = commitMotion,
        programmatic = NavSettleSpec.Tween(350, CubicBezierEasing(0.3f, 0f, 0.65f, 1f)),
    )

    private val visualTransition = navGraphicsTransition(
        scrim = { 0f },
    ) { scope ->
        scaleX = 1f
        scaleY = 1f
        translationX = 0f
        translationY = 0f
        alpha = 1f
        clip = false
        shape = RectangleShape
        // The catalog stays stationary. Only the covering container changes its bounds.
        if (scope.relativeDepth <= 0f) {
            val progress = catalogContainerProgress(scope.relativeDepth)
            // At the exact handoff frame the real tile owns all drawing, including its badges.
            // The source reads this same entry's progress, not the root's animation lifecycle.
            alpha = if (progress > 0f) 1f else 0f
            scope.gesture?.let { gesture ->
                translationY = catalogContainerDragOffset(gesture.touchY - gesture.initialTouchY, progress)
            }
            clip = progress < 1f
            val sourceRadius = with(scope.density) {
                val card = genericSource?.takeIf { it.cardBounds != null }
                when {
                    card != null -> card.cardCornerRadius.toPx()
                    listSource != null -> 14.dp.toPx()
                    else -> coverCornerRadiusPx(scope.density)
                }
            }
            val radius = interpolate(sourceRadius, with(scope.density) { deviceCornerRadius.toPx() }, progress)
            shape = CoverContainerShape(bounds(size, progress),
                if (usesListMotion) radius else radius.coerceAtLeast(sourceRadius))
        }
    }

    val transition: NavTransition = object : NavTransition by visualTransition {
        // Read the target stack when the driver resolves its motion, before entry scopes update.
        override val motion: NavMotion get() = if (isReturning()) returnMotion else enterMotion
    }
}

internal fun catalogContainerProgress(relativeDepth: Float): Float = (1f + relativeDepth).coerceIn(0f, 1f)

internal fun catalogContainerBounds(source: Rect, pageSize: Size, progress: Float): Rect {
    val fraction = progress.coerceIn(0f, 1f)
    return Rect(
        interpolate(source.left, 0f, fraction),
        interpolate(source.top, 0f, fraction),
        interpolate(source.right, pageSize.width, fraction),
        interpolate(source.bottom, pageSize.height, fraction),
    )
}

// The cover finishes fading before detail content appears; reversing progress reverses the phases.
internal fun catalogCoverAlpha(progress: Float): Float = 1f - ((progress - 0.10f) / 0.18f).coerceIn(0f, 1f)
internal fun catalogDetailAlpha(progress: Float): Float = ((progress - 0.28f) / 0.28f).coerceIn(0f, 1f)

internal fun catalogContainerDragOffset(deltaY: Float, progress: Float): Float {
    val fraction = progress.coerceIn(0f, 1f)
    return deltaY * 0.35f * 4f * fraction * (1f - fraction)
}

private data class CoverContainerShape(val bounds: Rect, val cornerRadius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(bounds, CornerRadius(cornerRadius.coerceIn(0f, bounds.minDimension / 2f))))
}

/** Square endpoints keep the jacket proportional throughout both directions of the same path. */
internal fun catalogSharedCoverBounds(source: Rect, target: Rect, progress: Float): Rect {
    val fraction = progress.coerceIn(0f, 1f)
    return Rect(
        interpolate(source.left, target.left, fraction),
        interpolate(source.top, target.top, fraction),
        interpolate(source.right, target.right, fraction),
        interpolate(source.bottom, target.bottom, fraction),
    )
}

// Returning: begin at halfway, reach full opacity before handing drawing back to the tile.
// The same curve naturally fades the badge out on entry and during a cancelled back gesture.
internal fun catalogProgressBadgeAlpha(progress: Float): Float = ((0.5f - progress) / 0.25f).coerceIn(0f, 1f)
