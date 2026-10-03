package org.rhythmeta.chunithmd.ui.collections

import org.rhythmeta.chunithmd.shared.localization.tr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.shared.CatalogSort
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

@Stable
class CollectionsUiState(grid: Boolean = false, sort: CatalogSort = CatalogSort.Default, ascending: Boolean = true) {
    var grid by mutableStateOf(grid)
    var sort by mutableStateOf(sort)
    var ascending by mutableStateOf(ascending)
    var createRequested by mutableStateOf(false)
    var importRequested by mutableStateOf(false)
    var importValue by mutableStateOf("")
    var renameRequested by mutableStateOf(false)
}

@Composable
fun rememberCollectionsUiState(): CollectionsUiState = rememberSaveable(saver = listSaver(
    save = { listOf(it.grid, it.sort.name, it.ascending) },
    restore = { CollectionsUiState(it[0] as Boolean, CatalogSort.valueOf(it[1] as String), it[2] as Boolean) },
)) { CollectionsUiState() }

@Composable
fun CollectionsToolbarActions(state: CollectionsUiState, detail: Boolean) {
    var menuExpanded by remember { mutableStateOf(false) }
    if (!detail) {
        Box {
            IconButton(onClick = { menuExpanded = !menuExpanded }) { Icon(Icons.Rounded.Add, tr("添加收藏夹")) }
            WindowListPopup(show = menuExpanded, alignment = PopupPositionProvider.Align.End, enableWindowDim = true, onDismissRequest = { menuExpanded = false }) {
                ListPopupColumn {
                    DropdownImpl(tr("导入收藏夹"), optionSize = 2, isSelected = false, index = 0, onSelectedIndexChange = { menuExpanded = false; state.importRequested = true })
                    DropdownImpl(tr("新建收藏夹"), optionSize = 2, isSelected = false, index = 1, onSelectedIndexChange = { menuExpanded = false; state.createRequested = true })
                }
            }
        }
    } else {
        IconButton(onClick = { state.grid = !state.grid }) {
            Icon(if (state.grid) Icons.AutoMirrored.Rounded.List else Icons.Rounded.GridView, if (state.grid) tr("切换列表") else tr("切换网格"))
        }
        Box {
            IconButton(onClick = { menuExpanded = !menuExpanded }) { Icon(Icons.AutoMirrored.Rounded.Sort, tr("排序")) }
            WindowListPopup(show = menuExpanded, alignment = PopupPositionProvider.Align.End, enableWindowDim = true, onDismissRequest = { menuExpanded = false }) {
                ListPopupColumn {
                    // Match maimaid's catalog sort choices for collection previews and details.
                    val options = listOf(CatalogSort.Default, CatalogSort.VersionDate, CatalogSort.Difficulty)
                    options.forEachIndexed { index, sort ->
                        DropdownImpl(
                            text = when (sort) { CatalogSort.Default -> tr("默认顺序"); CatalogSort.VersionDate -> tr("版本 / 发行日期"); else -> tr("定数") },
                            optionSize = options.size, isSelected = sort == state.sort, index = index,
                            onSelectedIndexChange = { state.sort = sort; menuExpanded = false },
                        )
                    }
                    Row(Modifier.fillMaxWidth().clickable { state.ascending = !state.ascending; menuExpanded = false }.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (state.ascending) "↑" else "↓", style = MiuixTheme.textStyles.title3, color = MiuixTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(if (state.ascending) tr("升序") else tr("降序"))
                    }
                }
            }
        }
        IconButton(onClick = { state.renameRequested = true }) { Icon(Icons.Rounded.Edit, tr("重命名收藏夹")) }
    }
}
