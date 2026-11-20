package org.rhythmeta.chunithmd.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.TopAppBarState
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import kotlin.math.roundToInt

/** Blur radius of the flat top bar material, in dp. */
private const val TopBarBlurRadius = 25f

/** Page tint blended over the flat top bar material. */
private const val TopBarSurfaceAlpha = 0.72f

/** Blur radius at the top edge of the progressive band, in dp. */
private const val TopBarProgressiveBlurRadius = 80f

/**
 * Falloff exponent of the progressive band, matching MIUIX's glass top bar: most of the radius
 * change happens near the strong (top) end, leaving a long soft tail towards the page.
 */
private const val TopBarProgressiveCurve = 0.25f

/** Page tint blended into the progressive band; it fades out together with the blur. */
private const val TopBarProgressiveTintAlpha = 0.3f

/** Room left below the covered chrome before the band's blur reaches zero. */
private val TopBarBandExtra = 20.dp

/** How much taller than the covered chrome the progressive band is drawn. */
private const val TopBarBandScale = 1.2f

/** Fade-in duration of the progressive band once content scrolls underneath the bar. */
private const val TopBarBandFadeMillis = 100

private val TopBarShape = RoundedCornerShape(0.dp)

/** Gradient of the progressive band: full strength at the top edge, clear towards the page. */
private val TopBarProgressiveGradient = ProgressiveBlur.Top.copy(curve = TopBarProgressiveCurve)

@Composable
fun rememberPageBackdrop(enabled: Boolean, surfaceColor: Color): LayerBackdrop? {
    if (!enabled) return null
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

fun Modifier.pageBackdrop(backdrop: LayerBackdrop?): Modifier =
    if (backdrop == null) this else layerBackdrop(backdrop)

/**
 * Shared page shell for root pages and settings detail pages.
 *
 * The source backdrop belongs to this scaffold instance, so pages that coexist
 * during a navigation transition never consume one another's blur source.
 *
 * With progressive blur the bar itself stays transparent: a band behind it fades in once the page
 * scrolls content underneath, blurs strongest at the top edge and clears towards the page, and is
 * drawn taller than the covered chrome so the transition has no hard edge. Without progressive blur
 * the bar keeps the flat blurred material.
 */
@Composable
fun AppPageScaffold(
    title: String,
    pageBackground: Color,
    blurEnabled: Boolean,
    progressiveBlurEnabled: Boolean = true,
    largeTitle: Boolean = true,
    smallTitleVisible: Boolean? = null,
    topBarScrollBehavior: ScrollBehavior = MiuixScrollBehavior(),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues, NestedScrollConnection) -> Unit,
) {
    val topBarBackdrop = rememberPageBackdrop(blurEnabled, pageBackground)
    val blurActive = blurEnabled && topBarBackdrop != null
    val progressive = blurActive && progressiveBlurEnabled
    val scrollState = topBarScrollBehavior.state
    val contentScrolled by remember(scrollState) {
        derivedStateOf { scrollState.contentOffset < -1f || scrollState.overscrollOffset < -1f }
    }
    val bandProgress = animateFloatAsState(
        targetValue = if (contentScrolled) 1f else 0f,
        animationSpec = tween(durationMillis = TopBarBandFadeMillis, easing = LinearEasing),
        label = "top-bar-band",
    )
    val bandVisible by remember(bandProgress) {
        derivedStateOf { bandProgress.value > 0.001f }
    }
    val bandColors = BlurDefaults.blurColors(
        blendColors = remember(pageBackground) {
            listOf(BlendColorEntry(pageBackground.copy(alpha = TopBarProgressiveTintAlpha)))
        },
    )
    val flatBarModifier = if (topBarBackdrop != null) {
        Modifier.textureBlur(
            backdrop = topBarBackdrop,
            shape = TopBarShape,
            blurRadius = TopBarBlurRadius,
            colors = BlurColors(
                blendColors = listOf(BlendColorEntry(pageBackground.copy(alpha = TopBarSurfaceAlpha))),
            ),
        )
    } else {
        Modifier
    }
    val barModifier = when {
        !blurActive -> Modifier
        progressive -> Modifier
        else -> flatBarModifier
    }
    val barColor = if (blurActive) Color.Transparent else pageBackground

    MiuixScaffold(
        containerColor = pageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val measurements = remember { TopBarMeasurements() }
            TopBarSlot(
                progressive = progressive,
                bandProgress = bandProgress,
                bandVisible = bandVisible,
                backdrop = topBarBackdrop,
                colors = bandColors,
                measurements = measurements,
            ) {
                if (largeTitle) MiuixTopAppBar(
                    title = title,
                    largeTitle = title,
                    navigationIcon = navigationIcon,
                    modifier = barModifier,
                    color = barColor,
                    actions = actions,
                    bottomContent = {
                        if (progressive) {
                            MeasuredBottomContent(measurements) { bottomContent() }
                        } else {
                            bottomContent()
                        }
                    },
                    scrollBehavior = topBarScrollBehavior,
                ) else if (smallTitleVisible != null) {
                    AnimatedSmallTopAppBar(
                        title = title,
                        titleVisible = smallTitleVisible,
                        modifier = barModifier,
                        color = barColor,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        scrollBehavior = topBarScrollBehavior,
                    )
                } else top.yukonga.miuix.kmp.basic.SmallTopAppBar(
                    title = title,
                    navigationIcon = navigationIcon,
                    modifier = barModifier,
                    color = barColor,
                    actions = actions,
                    scrollBehavior = topBarScrollBehavior,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pageBackdrop(if (blurActive) topBarBackdrop else null),
        ) {
            content(padding, topBarScrollBehavior.nestedScrollConnection)
        }
    }
}

/** Bottom-content height recorded during measurement, used to size the progressive band. */
private class TopBarMeasurements {
    var bottomContentHeight: Int = 0
}

/** Publishes its measured height to [measurements] while being laid out. */
@Composable
private fun MeasuredBottomContent(
    measurements: TopBarMeasurements,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            measurements.bottomContentHeight = placeable.height
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        },
    ) {
        content()
    }
}

/**
 * Places the top [bar] content, with a progressive-blur band behind it when [progressive].
 *
 * The band covers the chrome the bar keeps once collapsed (status bar, collapsed title row and any
 * bottom content) and is drawn taller than that, like MIUIX's glass top bar, so its gradient reaches
 * zero below the bar instead of ending on the bar's edge. The band overflows the layout on purpose:
 * the top bar slot must only be as tall as the bar itself.
 */
@Composable
private fun TopBarSlot(
    progressive: Boolean,
    bandProgress: State<Float>,
    bandVisible: Boolean,
    backdrop: LayerBackdrop?,
    colors: BlurColors,
    measurements: TopBarMeasurements,
    bar: @Composable () -> Unit,
) {
    if (!progressive || backdrop == null) {
        bar()
        return
    }
    val statusBarTop = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val statusBarPx = with(density) { statusBarTop.toPx() }
    val collapsedPx = with(density) { TopAppBarDefaults.CollapsedHeight.toPx() }
    val extraPx = with(density) { TopBarBandExtra.toPx() }
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = bandProgress.value }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = TopBarProgressiveGradient,
                        blurRadius = TopBarProgressiveBlurRadius,
                        colors = colors,
                        enabled = bandVisible,
                    ),
            )
            bar()
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        // The bar is measured first: its bottom content height sizes the band.
        val barPlaceable = measurables[1].measure(constraints)
        val coveredPx = statusBarPx + collapsedPx + measurements.bottomContentHeight
        val bandHeightPx = ((coveredPx + extraPx) * TopBarBandScale).roundToInt()
        val bandPlaceable = measurables[0].measure(
            Constraints.fixed(barPlaceable.width, bandHeightPx),
        )
        layout(barPlaceable.width, barPlaceable.height) {
            bandPlaceable.placeRelative(0, 0)
            barPlaceable.placeRelative(0, 0)
        }
    }
}
