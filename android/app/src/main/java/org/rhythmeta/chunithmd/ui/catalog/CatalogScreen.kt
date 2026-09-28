package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.CatalogSort
import org.rhythmeta.chunithmd.shared.CatalogSyncStage
import org.rhythmeta.chunithmd.shared.CatalogSyncState
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop

@Composable
fun CatalogScreen(
    modifier: Modifier,
    contentTopPadding: Dp,
    bundle: CatalogBundle?,
    sync: CatalogSyncState,
    error: String?,
    songs: List<CatalogSong>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    listState: LazyListState,
    navigationBackdrop: LayerBackdrop,
    searchScrollConnection: NestedScrollConnection,
    topBarScrollConnection: NestedScrollConnection,
    onRetry: () -> Unit,
    onSongClick: (CatalogSong) -> Unit,
) {
    if (bundle == null) {
        InitialLoad(sync, error, onRetry, modifier.padding(top = contentTopPadding))
    } else {
        SongList(
            modifier = modifier
                .kyantLayerBackdrop(navigationBackdrop)
                .nestedScroll(searchScrollConnection)
                .nestedScroll(topBarScrollConnection),
            contentTopPadding = contentTopPadding,
            songs = songs,
            jacketBaseUrl = jacketBaseUrl,
            localJacketPath = localJacketPath,
            listState = listState,
            onSongClick = onSongClick,
        )
    }
}

@Composable
fun CatalogSearchField(
    search: String,
    onSearchChange: (String) -> Unit,
    visible: Boolean,
    expanded: Boolean,
    backEnabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    interactionSource: MutableInteractionSource,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        SearchBar(
            inputField = {
                InputField(
                    query = search,
                    onQueryChange = onSearchChange,
                    onSearch = {},
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    interactionSource = interactionSource,
                    label = "歌曲、艺术家、别名...",
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            onExpandedChange = onExpandedChange,
            insideMargin = DpSize(width = 16.dp, height = 10.dp),
            expanded = expanded && backEnabled,
            content = {},
        )
    }
}

@Composable
fun CatalogToolbarActions(
    sortOpen: Boolean,
    sort: CatalogSort,
    ascending: Boolean,
    filterActive: Boolean,
    onSortToggle: () -> Unit,
    onSort: (CatalogSort) -> Unit,
    onAscending: () -> Unit,
    onFilter: () -> Unit,
) {
    MiuixIconButton(onClick = {}) {
        MiuixIcon(Icons.Rounded.GridView, contentDescription = "网格视图")
    }
    SortAction(
        expanded = sortOpen,
        sort = sort,
        ascending = ascending,
        onToggle = onSortToggle,
        onSort = onSort,
        onAscending = onAscending,
    )
    MiuixIconButton(onClick = onFilter) {
        MiuixIcon(
            Icons.Rounded.FilterList,
            contentDescription = "筛选",
            tint = if (filterActive) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SortAction(
    expanded: Boolean,
    sort: CatalogSort,
    ascending: Boolean,
    onToggle: () -> Unit,
    onSort: (CatalogSort) -> Unit,
    onAscending: () -> Unit,
) {
    Box {
        MiuixIconButton(onClick = onToggle) {
            MiuixIcon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "排序")
        }
        WindowListPopup(
            show = expanded,
            alignment = PopupPositionProvider.Align.End,
            enableWindowDim = true,
            onDismissRequest = onToggle,
        ) {
            ListPopupColumn {
                CatalogSort.entries.forEachIndexed { index, option ->
                    DropdownImpl(
                        text = sortLabel(option),
                        optionSize = CatalogSort.entries.size,
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
                    MiuixText(if (ascending) "升序" else "降序", style = MiuixTheme.textStyles.body1)
                }
            }
        }
    }
}

@Composable
private fun SongList(
    modifier: Modifier,
    contentTopPadding: Dp,
    songs: List<CatalogSong>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    listState: LazyListState,
    onSongClick: (CatalogSong) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 6.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(songs, key = CatalogSong::songId) { song ->
            SongCard(song, jacketBaseUrl, localJacketPath, onClick = { onSongClick(song) })
        }
    }
}

@Composable
private fun InitialLoad(sync: CatalogSyncState, error: String?, onRetry: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        MiuixText("CHUNITHM", style = MiuixTheme.textStyles.title1, color = MiuixTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        MiuixText(error ?: sync.message ?: "正在下载歌曲目录", style = MiuixTheme.textStyles.body1)
        Spacer(Modifier.height(18.dp))
        if (sync.stage in setOf(CatalogSyncStage.Checking, CatalogSyncStage.Downloading, CatalogSyncStage.Validating, CatalogSyncStage.Applying)) {
            val downloadProgress = sync.progress
            if (sync.stage == CatalogSyncStage.Downloading && downloadProgress != null) {
                LinearProgressIndicator(
                    progress = downloadProgress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            } else {
                CircularProgressIndicator()
            }
        } else {
            MiuixButton(onClick = onRetry) { MiuixText("重试") }
        }
    }
}

private fun sortLabel(sort: CatalogSort) = when (sort) {
    CatalogSort.Default -> "默认顺序"
    CatalogSort.Title -> "标题"
    CatalogSort.VersionDate -> "版本 / 发行日期"
    CatalogSort.Difficulty -> "最高定数"
}
