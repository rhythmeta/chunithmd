package org.rhythmeta.chunithmd.ui.collections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.rhythmeta.chunithmd.collection.CollectionEntry
import org.rhythmeta.chunithmd.collection.SongCollectionRepository
import org.rhythmeta.chunithmd.ui.components.ExpandableBottomSheet
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CollectionPickerSheet(repository: SongCollectionRepository, entry: CollectionEntry?, onDismiss: () -> Unit) {
    val collections by repository.collections.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember(entry?.key) { mutableStateOf<String?>(null) }
    ExpandableBottomSheet(
        visible = entry != null,
        onDismissRequest = { if (!busy) onDismiss() },
        expandActionLabel = "展开收藏夹列表", collapseActionLabel = "收起收藏夹列表",
        expandedStateDescription = "收藏夹列表已展开", halfExpandedStateDescription = "收藏夹列表半展开",
        header = {
            IconButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.align(Alignment.CenterStart)) { Icon(Icons.Rounded.Close, "取消") }
            Text("加入收藏夹", style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Rounded.Check, "完成", tint = MiuixTheme.colorScheme.primary) }
        },
    ) { topInset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = topInset + 12.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            error?.let { message -> item("error") { Text(message, color = MiuixTheme.colorScheme.error) } }
            if (collections == null) item("loading") { CircularProgressIndicator() }
            else if (collections.orEmpty().isEmpty()) item("empty") {
                CollectionEmptyState(
                    Icons.Rounded.FolderOpen,
                    "还没有收藏夹",
                    "前往主页的「收藏夹」新建一个，再回来收下这张谱面。",
                    Modifier.fillParentMaxSize(),
                )
            }
            items(collections.orEmpty(), key = { it.id }) { collection ->
                CheckboxPreference(
                    title = collection.name,
                    checked = collection.entries.any { it.key == entry?.normalized()?.key },
                    enabled = !busy && entry != null,
                    checkboxLocation = CheckboxLocation.End,
                    onCheckedChange = { included ->
                        entry?.let { chart ->
                            scope.launch {
                                busy = true
                                error = null
                                try { repository.setMembership(collection.id, chart, included) }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (failure: Exception) { error = failure.message ?: "保存失败，请重试" }
                                finally { busy = false }
                            }
                        }
                    },
                    checkboxColors = CheckboxDefaults.checkboxColors(
                        checkedBackgroundColor = MiuixTheme.colorScheme.primary,
                        checkedForegroundColor = MiuixTheme.colorScheme.onPrimary,
                        uncheckedBackgroundColor = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.38f),
                        uncheckedForegroundColor = MiuixTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth().squircleSurface(MiuixTheme.colorScheme.surfaceContainer, 12.dp, extension = SquircleExtension),
                )
            }
        }
    }
}
