package org.rhythmeta.chunithmd.ui.collections

import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.collection.*
import org.rhythmeta.chunithmd.shared.*
import org.rhythmeta.chunithmd.ui.components.SongListScrollBar
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun SongCollectionsScreen(
    repository: SongCollectionRepository,
    uiState: CollectionsUiState,
    collectionId: String? = null,
    bundle: CatalogBundle?,
    activeServer: ProfileServer,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    contentTopPadding: Dp,
    topBarScrollConnection: NestedScrollConnection,
    onOpenSong: (String) -> Unit,
    onOpenCollection: (String) -> Unit,
) {
    val collections by repository.collections.collectAsState(initial = null)
    val collection = collections?.firstOrNull { it.id == collectionId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun perform(action: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                error = failure.message ?: "操作失败，请重试"
                scope.launch { snackbar.showSnackbar(error.orEmpty(), duration = SnackbarDuration.Short) }
            }
            finally { busy = false }
        }
    }
    fun share(target: SongCollection) = perform {
        val code = withContext(Dispatchers.Default) { SongCollectionCodec.encode(target) }
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, code)
        }, "分享收藏夹"))
    }
    LaunchedEffect(uiState.importRequested) {
        if (collectionId == null && uiState.importRequested) {
            uiState.importRequested = false
            perform {
                val code = context.getSystemService(ClipboardManager::class.java)?.primaryClip
                    ?.getItemAt(0)?.coerceToText(context)?.toString()
                require(!code.isNullOrBlank()) { "剪贴板中没有收藏夹分享码" }
                val source = withContext(Dispatchers.Default) { SongCollectionCodec.decode(code) }
                repository.importCollection(source)
                scope.launch { snackbar.showSnackbar("收藏夹导入成功", duration = SnackbarDuration.Short) }
            }
        }
    }
    val cards = remember(collection, bundle, uiState.sort, uiState.ascending, activeServer) {
        collection?.let { collectionCards(it, bundle, uiState.sort, uiState.ascending, activeServer) }.orEmpty()
    }
    Box(Modifier.fillMaxSize()) {
        val scrollModifier = Modifier.fillMaxSize().nestedScroll(topBarScrollConnection)
        val emptyModifier = Modifier.fillMaxSize().padding(top = contentTopPadding).navigationBarsPadding()
        when {
            collections == null -> Box(scrollModifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            collectionId == null && collections.orEmpty().isEmpty() -> CollectionEmptyState(
                icon = Icons.Rounded.FolderOpen,
                title = "从第一个收藏夹开始",
                message = "把喜欢的谱面、练习目标整理在一起，随时回来查看。",
                modifier = emptyModifier,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { uiState.createRequested = true },
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    ) { Text("新建收藏夹") }
                    Button(
                        enabled = !busy,
                        onClick = { uiState.importRequested = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("从剪贴板导入") }
                }
            }
            collectionId == null -> {
                LazyColumn(
                    state = listState, modifier = scrollModifier,
                    contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 10.dp, end = 16.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(collections.orEmpty(), key = { it.id }) { item ->
                        val previews = remember(item, bundle, uiState.sort, uiState.ascending, activeServer) {
                            collectionCards(item, bundle, uiState.sort, uiState.ascending, activeServer)
                                .filter { it.song != null && it.sheet != null }.take(4)
                        }
                        CollectionSummaryCard(item, previews, jacketBaseUrl, localJacketPath,
                            onOpen = { onOpenCollection(item.id) }, onShare = { share(item) },
                            onDelete = { perform { repository.delete(item.id) } })
                    }
                }
                SongListScrollBar(listState, PaddingValues(top = contentTopPadding + 10.dp, bottom = 36.dp))
            }
            collection == null -> CollectionEmptyState(
                Icons.Rounded.FolderOpen, "收藏夹已不存在", "返回收藏夹列表，查看其他收藏夹。", emptyModifier,
            )
            cards.isEmpty() -> CollectionEmptyState(
                icon = Icons.Rounded.LibraryMusic,
                title = "还没有收藏谱面",
                message = "在歌曲详情中长按谱面标题，选择这个收藏夹，即可加入。",
                modifier = emptyModifier,
            )
            uiState.grid -> LazyVerticalGrid(
                columns = GridCells.Fixed(3), modifier = scrollModifier,
                contentPadding = PaddingValues(start = 12.dp, top = contentTopPadding + 12.dp, end = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                gridItems(cards, key = { it.entry.key }) { card ->
                    CollectionChartCard(card, true, jacketBaseUrl, localJacketPath,
                        onOpen = { onOpenSong(card.entry.songId) },
                        onDelete = { perform { repository.setMembership(collectionId, card.entry, false) } })
                }
            }
            else -> LazyColumn(
                state = listState, modifier = scrollModifier,
                contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cards, key = { it.entry.key }) { card ->
                    CollectionChartCard(card, false, jacketBaseUrl, localJacketPath,
                        onOpen = { onOpenSong(card.entry.songId) },
                        onDelete = { perform { repository.setMembership(collectionId, card.entry, false) } })
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp))
    }
    val creating = collectionId == null && uiState.createRequested
    val renaming = collectionId != null && uiState.renameRequested && collection != null
    if (creating || renaming) {
        var draft by remember(collectionId, creating, renaming) { mutableStateOf(if (renaming) collection.name else "") }
        fun dismiss() {
            if (!busy) {
                uiState.createRequested = false
                uiState.renameRequested = false
                error = null
            }
        }
        WindowDialog(show = true, title = if (renaming) "重命名收藏夹" else "新建收藏夹", onDismissRequest = ::dismiss) {
            TextField(draft, { draft = it.take(40) }, label = "收藏夹名称", modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MiuixTheme.colorScheme.error) }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton("取消", enabled = !busy, onClick = ::dismiss, modifier = Modifier.weight(1f))
                Button(enabled = !busy && draft.isNotBlank(), modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary(), onClick = {
                    perform {
                        if (renaming) repository.rename(requireNotNull(collection).id, draft) else repository.create(draft)
                        uiState.createRequested = false
                        uiState.renameRequested = false
                    }
                }) { Text("完成") }
            }
        }
    }
}

internal data class CollectionCard(val entry: CollectionEntry, val song: CatalogSong?, val sheet: CatalogSheet?)

internal fun collectionCards(collection: SongCollection, bundle: CatalogBundle?, sort: CatalogSort, ascending: Boolean, server: ProfileServer): List<CollectionCard> {
    val songs = bundle?.catalog?.songs.orEmpty()
    val byId = songs.associateBy { it.songId }
    val order = songs.mapIndexed { index, song -> song.songId to index }.toMap()
    val versions = bundle?.catalog?.versions.orEmpty().mapIndexed { index, version -> version.version to index }.toMap()
    val cards = collection.entries.map { entry ->
        val song = byId[entry.songId]
        val sheet = song?.sheets?.firstOrNull { it.type.equals(entry.chartType, true) && it.difficulty.equals(entry.difficulty, true) }
        val regionLevel = if (server == ProfileServer.Cn) song?.regionOverrides?.get("cn")?.charts?.get("${entry.chartType}:${entry.difficulty}") else null
        CollectionCard(entry, song, sheet?.let { if (regionLevel == null) it else it.copy(level = regionLevel.level ?: it.level, internalLevelValue = regionLevel.levelValue ?: it.internalLevelValue) })
    }
    val comparator = when (sort) {
        CatalogSort.Default -> compareBy<CollectionCard> { order[it.entry.songId] ?: Int.MAX_VALUE }
        CatalogSort.Title -> compareBy { it.song?.let(CatalogSongFormatter::displayTitle)?.lowercase() ?: it.entry.songId }
        CatalogSort.VersionDate -> compareBy<CollectionCard> { versions[it.song?.version] ?: Int.MAX_VALUE }.thenBy { it.song?.releaseDate.orEmpty() }
        CatalogSort.Difficulty -> compareBy { it.sheet?.internalLevelValue ?: it.sheet?.levelValue ?: 0.0 }
    }
    return cards.sortedWith(if (ascending) comparator else comparator.reversed())
}
