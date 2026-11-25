package org.rhythmeta.chunithmd.ui.components

import org.rhythmeta.chunithmd.shared.localization.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
internal fun <T> SortAction(
    options: List<T>,
    label: (T) -> String,
    expanded: Boolean,
    sort: T,
    ascending: Boolean,
    onToggle: () -> Unit,
    onSort: (T) -> Unit,
    onAscending: () -> Unit,
) {
    Box {
        MiuixIconButton(onClick = onToggle) {
            MiuixIcon(Icons.AutoMirrored.Rounded.Sort, contentDescription = tr("排序"))
        }
        WindowListPopup(
            show = expanded,
            alignment = PopupPositionProvider.Align.End,
            enableWindowDim = true,
            onDismissRequest = onToggle,
        ) {
            ListPopupColumn {
                options.forEachIndexed { index, option ->
                    DropdownImpl(
                        text = label(option),
                        optionSize = options.size,
                        isSelected = option == sort,
                        index = index,
                        onSelectedIndexChange = { onSort(option) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onAscending)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixText(if (ascending) "↑" else "↓", style = MiuixTheme.textStyles.title3, color = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    MiuixText(if (ascending) tr("升序") else tr("降序"), style = MiuixTheme.textStyles.body1)
                }
            }
        }
    }
}
