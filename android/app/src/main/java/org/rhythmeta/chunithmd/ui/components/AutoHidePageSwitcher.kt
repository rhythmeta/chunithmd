package org.rhythmeta.chunithmd.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.TabRowWithContour

@Composable
internal fun AutoHidePageSwitcher(
    tabs: List<String>, selectedPage: Int, visible: Boolean, onSelectedPageChange: (Int) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        TabRowWithContour(
            tabs = tabs, selectedTabIndex = selectedPage, onTabSelected = onSelectedPageChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            minWidth = 112.dp, maxWidth = 180.dp,
        )
    }
}

@Composable
internal fun rememberPageSwitcherScroll(visible: Boolean, onVisibleChange: (Boolean) -> Unit): NestedScrollConnection {
    val update by rememberUpdatedState(onVisibleChange)
    val threshold = with(LocalDensity.current) { (if (visible) 36.dp else 56.dp).toPx() }
    return remember(visible, threshold) {
        object : NestedScrollConnection {
            private var distance = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val delta = if (visible) -available.y else available.y
                    distance = if (delta > 0f) distance + delta else 0f
                    if (distance >= threshold) {
                        distance = 0f
                        update(!visible)
                    }
                }
                return Offset.Zero
            }
        }
    }
}
