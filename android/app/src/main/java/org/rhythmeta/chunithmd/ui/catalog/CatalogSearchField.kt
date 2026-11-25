package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.shared.localization.tr
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CatalogSearchField(
    search: String,
    onSearchChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    visible: Boolean,
    focusRequestToken: Int,
    backEnabled: Boolean,
    interactionSource: MutableInteractionSource,
    onCancelSearch: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        SearchBar(
            inputField = {
                key(focusRequestToken) {
                    InputField(
                        query = search,
                        onQueryChange = onSearchChange,
                        onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        expanded = expanded,
                        onExpandedChange = onExpandedChange,
                        interactionSource = interactionSource,
                        label = tr("歌曲、艺术家、别名..."),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            onExpandedChange = onExpandedChange,
            insideMargin = DpSize(width = 16.dp, height = 10.dp),
            expanded = expanded && backEnabled,
            outsideEndAction = {
                TextButton(
                    text = tr("取消"),
                    onClick = onCancelSearch,
                    modifier = Modifier.padding(end = 16.dp),
                    minWidth = 0.dp,
                    minHeight = 48.dp,
                    insideMargin = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        color = Color.Transparent,
                        textColor = MiuixTheme.colorScheme.primary,
                    ),
                )
            },
            content = {},
        )
    }
}
