package org.rhythmeta.chunithmd.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Keep the bar and its controls fixed; only the title moves through a short, interruptible fade. */
@Composable
internal fun AnimatedSmallTopAppBar(
    title: String,
    titleVisible: Boolean,
    modifier: Modifier,
    color: Color,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    scrollBehavior: ScrollBehavior,
) {
    val progress = animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(if (titleVisible) 220 else 160, easing = FastOutSlowInEasing),
        label = "small-top-title",
    )
    var navigationWidth by remember { mutableIntStateOf(0) }
    var actionsWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box {
        SmallTopAppBar(
            title = "",
            modifier = modifier,
            color = color,
            navigationIcon = {
                Box(Modifier.onSizeChanged { navigationWidth = it.width }) { navigationIcon() }
            },
            actions = {
                Row(Modifier.onSizeChanged { actionsWidth = it.width }, content = actions)
            },
            scrollBehavior = scrollBehavior,
        )
        // Use the same safe insets, centre height, typography and title padding as Miuix.
        BoxWithConstraints(Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
            .height(TopAppBarDefaults.SmallTopAppBarCenterHeight)) {
            val occupied = with(density) { (navigationWidth + actionsWidth).toDp() } +
                TopAppBarDefaults.NavigationIconPadding + TopAppBarDefaults.ActionIconPadding
            val titleWidth = ((maxWidth - occupied) * 0.9f).coerceAtLeast(0.dp)
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = MiuixTheme.textStyles.title3.fontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).widthIn(max = titleWidth)
                    .padding(horizontal = TopAppBarDefaults.TitlePadding)
                    .graphicsLayer {
                        alpha = progress.value
                        translationY = 6.dp.toPx() * (1f - progress.value)
                    }
                    .then(if (titleVisible) Modifier else Modifier.clearAndSetSemantics {}),
            )
        }
    }
}
