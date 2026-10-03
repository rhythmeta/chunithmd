package org.rhythmeta.chunithmd.ui.community

import org.rhythmeta.chunithmd.shared.localization.tr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.shared.community.*
import org.rhythmeta.chunithmd.ui.components.SongListScrollBar
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CommunityAliasScreen(
    store: CommunityAliasStore,
    songs: List<CatalogSong>,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    contentTopPadding: Dp,
    topBarScrollConnection: NestedScrollConnection,
    onOpenSong: (String) -> Unit,
    onLogin: () -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.boardError) {
        state.boardError?.takeIf { state.board.isNotEmpty() }?.let { message ->
            store.consumeBoardError()
            scope.launch { snackbar.showSnackbar(message, duration = SnackbarDuration.Short) }
        }
    }
    val songsById = remember(songs) { songs.associateBy { it.songId } }
    val groups = remember(state.board, songsById) {
        state.board.groupBy { it.songIdentifier }.entries.sortedBy { songsById[it.key]?.title?.lowercase() ?: it.key }
    }
    LaunchedEffect(store, state.accountId) { store.refreshBoard(); store.syncApproved() }
    fun refresh() { scope.launch { store.refreshBoard(); store.syncApproved(force = true); store.refreshPersonalAliases() } }
    Box(Modifier.fillMaxSize()) {
        PullToRefresh(isRefreshing = state.boardLoading, onRefresh = ::refresh, contentPadding = PaddingValues(top = contentTopPadding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(topBarScrollConnection),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentTopPadding + 12.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!state.authenticated) item("login") {
                    CommunityMessageCard(tr("需要登录"), tr("登录后可以投稿和参与社区别名投票。"), tr("登录 Rhythmeta"), onLogin)
                }
                state.boardError?.takeIf { state.board.isEmpty() }?.let { message -> item("error") {
                    CommunityMessageCard(tr("加载或投票失败"), message, tr("重试"), ::refresh)
                } }
                state.syncError?.let { message -> item("sync-error") {
                    CommunityMessageCard(tr("别名同步失败"), tr("{0}\n已有的本地别名仍可使用。", message), tr("重新同步")) { scope.launch { store.syncApproved(force = true) } }
                } }
                if (state.authenticated && !state.boardLoading && state.board.isEmpty() && state.boardError == null) item("empty") {
                    CommunityMessageCard(tr("当前没有投票中的候选别名"), tr("新的候选别名会在公示期显示于此。"))
                }
                groups.forEach { (songId, candidates) ->
                    item("song:$songId") {
                        CommunitySongHeader(songsById[songId], songId, candidates.size, jacketBaseUrl, localJacketPath, onOpenSong)
                    }
                    items(candidates, key = { "candidate:${it.candidateId}" }) { candidate ->
                        CommunityCandidateCard(candidate, state.authenticated && state.votingId == null && !state.boardLoading, state.votingId == candidate.candidateId) {
                            support -> scope.launch {
                                store.vote(candidate.candidateId, support)
                                if (store.state.value.accountId == state.accountId && store.state.value.boardError == null) {
                                    snackbar.showSnackbar(tr("投票已更新"), duration = SnackbarDuration.Short)
                                }
                            }
                        }
                    }
                }
                if (state.boardHasMore && state.board.isNotEmpty()) item("more") {
                    Button(enabled = !state.boardLoading && state.votingId == null, onClick = { scope.launch { store.refreshBoard(loadMore = true) } }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.boardLoading) tr("正在加载…") else tr("加载更多"))
                    }
                }
            }
        }
        SongListScrollBar(listState, PaddingValues(top = contentTopPadding + 12.dp, bottom = 36.dp))
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }
}

@Composable
private fun CommunitySongHeader(
    song: CatalogSong?,
    fallbackTitle: String,
    count: Int,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onOpenSong: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = song != null, role = Role.Button) { song?.let { onOpenSong(it.songId) } }
            .padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val model = remember(song?.imageName, jacketBaseUrl, localJacketPath) {
            song?.imageName?.let { name ->
                localJacketPath(name)?.let(::File) ?: jacketBaseUrl.takeIf { it.isNotBlank() }?.let { "${it.trimEnd('/')}/$name" }
            }
        }
        Box(
            Modifier.size(46.dp).squircleSurface(MiuixTheme.colorScheme.surfaceContainer, 9.dp, SquircleExtension),
            contentAlignment = Alignment.Center,
        ) {
            if (model != null) AsyncImage(model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Rounded.MusicNote, null)
        }
        Column(Modifier.weight(1f)) {
            Text(
                song?.let(CatalogSongFormatter::displayTitle) ?: fallbackTitle,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth().basicMarquee(),
            )
            Text(tr("{0} 个候选别名", count), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

@Composable
private fun CommunityCandidateCard(candidate: CommunityCandidate, enabled: Boolean, voting: Boolean, onVote: (Boolean) -> Unit) {
    Card(
        Modifier.fillMaxWidth(), cornerRadius = 14.dp, insideMargin = PaddingValues(14.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(candidate.aliasText, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(tr("截止 {0}", formatCommunityDeadline(candidate.voteCloseAt)), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VoteButton(true, candidate.supportCount, candidate.myVote == 1, enabled) { onVote(true) }
                VoteButton(false, candidate.opposeCount, candidate.myVote == -1, enabled) { onVote(false) }
                if (voting) CircularProgressIndicator(size = 24.dp, strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun VoteButton(support: Boolean, count: Int, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val accent = if (support) Color(0xFF36A65C) else Color(0xFFD65C5C)
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            color = if (selected) accent else accent.copy(alpha = 0.12f),
            contentColor = if (selected) Color.White else accent,
        ),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(if (support) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbDown, null, Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        val label = when {
            selected && support -> tr("取消支持")
            selected -> tr("取消反对")
            support -> tr("支持")
            else -> tr("反对")
        }
        Text("$label $count")
    }
}

@Composable
internal fun CommunityMessageCard(title: String, message: String, action: String? = null, onAction: () -> Unit = {}) {
    Card(
        Modifier.fillMaxWidth(), cornerRadius = 14.dp, insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
            Text(message, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            action?.let { Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(it) } }
        }
    }
}

private fun formatCommunityDeadline(value: String?): String = value?.let {
    runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) }.getOrNull()
} ?: "--"
