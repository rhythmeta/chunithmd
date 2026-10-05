package org.rhythmeta.chunithmd.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

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
 */
@Composable
fun AppPageScaffold(
    title: String,
    pageBackground: Color,
    blurEnabled: Boolean,
    largeTitle: Boolean = true,
    smallTitleVisible: Boolean? = null,
    topBarScrollBehavior: ScrollBehavior = MiuixScrollBehavior(),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues, NestedScrollConnection) -> Unit,
) {
    val topBarBackdrop = rememberPageBackdrop(blurEnabled, pageBackground)
    val topBarBlurEnabled = blurEnabled && topBarBackdrop != null

    MiuixScaffold(
        containerColor = pageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (largeTitle) MiuixTopAppBar(
                title = title,
                largeTitle = title,
                navigationIcon = navigationIcon,
                modifier = if (topBarBlurEnabled) {
                    Modifier.textureBlur(
                        backdrop = topBarBackdrop,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        blurRadius = 25f,
                        colors = BlurColors(
                            blendColors = listOf(
                                BlendColorEntry(pageBackground.copy(alpha = 0.72f)),
                            ),
                        ),
                    )
                } else {
                    Modifier
                },
                color = if (topBarBlurEnabled) Color.Transparent else pageBackground,
                actions = actions,
                bottomContent = bottomContent,
                scrollBehavior = topBarScrollBehavior,
            ) else {
                val barModifier = if (topBarBlurEnabled) {
                    Modifier.textureBlur(
                        backdrop = topBarBackdrop,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        blurRadius = 25f,
                        colors = BlurColors(
                            blendColors = listOf(
                                BlendColorEntry(pageBackground.copy(alpha = 0.72f)),
                            ),
                        ),
                    )
                } else {
                    Modifier
                }
                val barColor = if (topBarBlurEnabled) Color.Transparent else pageBackground
                if (smallTitleVisible != null) {
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
                .pageBackdrop(topBarBackdrop),
        ) {
            content(padding, topBarScrollBehavior.nestedScrollConnection)
        }
    }
}
