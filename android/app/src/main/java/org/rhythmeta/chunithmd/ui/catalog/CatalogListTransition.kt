package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.squircle.squircleBorder
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import top.yukonga.miuix.kmp.basic.Text

internal enum class CatalogTextElement { Title, Artist }

internal data class CatalogTextAnchor(
    val bounds: Rect,
    val layout: TextLayoutResult,
)

/** Live source measurements survive the list row leaving composition during navigation. */
internal class CatalogListTransitionSource(val identityKey: String = "") {
    var cardBounds by mutableStateOf<Rect?>(null)
    var coverBounds by mutableStateOf<Rect?>(null)
    var painter by mutableStateOf<Painter?>(null)
    var appearance by mutableStateOf<CatalogListCardAppearance?>(null)
    var badges by mutableStateOf<CatalogListBadgesData?>(null)
    var badgeBounds by mutableStateOf<Rect?>(null)
    val texts = mutableStateMapOf<CatalogTextElement, CatalogTextAnchor>()
}

internal fun LayoutCoordinates.catalogBoundsInRoot(): Rect = Rect(
    localToRoot(Offset.Zero),
    localToRoot(Offset(size.width.toFloat(), size.height.toFloat())),
)

private class CatalogTextMeasurement {
    var coordinates: LayoutCoordinates? = null
    var layout: TextLayoutResult? = null
}

@Composable
internal fun CatalogListText(
    text: String,
    style: TextStyle,
    color: Color,
    maxLines: Int,
    overflow: TextOverflow,
    modifier: Modifier,
    element: CatalogTextElement,
    source: CatalogListTransitionSource? = null,
    transition: CatalogCoverTransition? = null,
) {
    val measurement = remember(source, transition, element) { CatalogTextMeasurement() }
    fun updateMeasurement() {
        val coordinates = measurement.coordinates?.takeIf { it.isAttached } ?: return
        val layout = measurement.layout ?: return
        source?.texts?.set(element, CatalogTextAnchor(coordinates.catalogBoundsInRoot(), layout))
    }
    // Measure a separate viewport node. The marquee measures its child at the full text width;
    // recording that child's bounds would let the overlay draw across the version/progress badges.
    Box(
        modifier = modifier
            .onGloballyPositioned { measurement.coordinates = it; updateMeasurement() }
            .clipToBounds()
            .graphicsLayer {
                alpha = if (transition?.hidesSource == true) 0f else 1f
            },
        propagateMinConstraints = true,
    ) {
        Text(
            text = text, style = style, color = color, maxLines = maxLines, overflow = overflow,
            modifier = Modifier.fillMaxWidth().basicMarquee(),
            onTextLayout = { measurement.layout = it; updateMeasurement() },
        )
    }
}

/** The two list labels retain their spacing and move as one fading group. */
@Composable
internal fun CatalogListTextOverlay(state: CatalogCoverTransition, progress: () -> Float, modifier: Modifier) {
    Canvas(modifier) {
        val fraction = progress()
        val alpha = catalogListContentAlpha(fraction)
        if (alpha == 0f) return@Canvas
        val offsetY = state.listContentTranslation(fraction) - state.listContentTranslation(0f)
        translate(top = offsetY) {
            CatalogTextElement.entries.forEach { element ->
                val source = state.sourceText(element) ?: return@forEach
                val bounds = source.bounds
                clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
                    drawText(source.layout, topLeft = bounds.topLeft, alpha = alpha)
                }
            }
        }
    }
}

internal fun catalogListContentAlpha(progress: Float): Float = (1f - progress / 0.18f).coerceIn(0f, 1f)

/** Decorations follow exactly the same vertical displacement and opacity as the label group. */
@Composable
internal fun CatalogListDecorationsOverlay(state: CatalogCoverTransition, progress: () -> Float) {
    val source = state.listSource ?: return
    val density = LocalDensity.current
    val appearance = source.appearance
    val card = state.listCardBounds()
    if (card != null && appearance != null) {
        val width = with(density) { card.width.toDp() }
        val height = with(density) { card.height.toDp() }
        Box(Modifier.requiredSize(width, height).clearAndSetSemantics {}.graphicsLayer {
            val fraction = progress()
            alpha = catalogListContentAlpha(fraction)
            translationX = card.left
            translationY = card.top + state.listContentTranslation(fraction) - state.listContentTranslation(0f)
        }.squircleBorder(width = 1.dp, color = appearance.accent.copy(alpha = 0.12f), cornerRadius = 14.dp)) {
            CatalogListCardAccent(appearance, Modifier.align(Alignment.CenterStart).width(4.dp)
                .height((height - 40.dp).coerceAtLeast(0.dp)))
        }
    }
    source.badges?.let { badges ->
        CatalogListCardBadges(badges, Modifier.clearAndSetSemantics {}.graphicsLayer {
            val fraction = progress()
            val bounds = state.listBadgeBounds()
            alpha = if (bounds == null) 0f else catalogListContentAlpha(fraction)
            if (bounds != null) {
                translationX = bounds.left
                translationY = bounds.top + state.listContentTranslation(fraction) - state.listContentTranslation(0f)
            }
        })
    }
}
