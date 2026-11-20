package org.rhythmeta.chunithmd.ui.community

import org.rhythmeta.chunithmd.ui.catalog.rememberSongCoverNavigation
import org.rhythmeta.chunithmd.ui.catalog.SongCardTransitionContent

import org.rhythmeta.chunithmd.shared.localization.tr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import org.rhythmeta.chunithmd.ui.components.fadingMarquee
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
    selectedPage: Int = 0,
    switcherScrollConnection: NestedScrollConnection? = null,
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
    // Approved aliases read newest first: inside a song by approval time, and songs by their most
    // recent approval, mirroring maimaid's board.
    val approvedGroups = remember(state.approvedItems, songsById) {
        state.approvedItems
            .groupBy { it.songIdentifier }
            .mapValues { (_, aliases) -> aliases.sortedByDescending { it.approvedAt.orEmpty() } }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<CommunityApprovedAlias>>> { entry ->
                    entry.value.maxOfOrNull { it.approvedAt.orEmpty() } ?: ""
                }.thenBy { entry ->
                    songsById[entry.key]?.title?.lowercase() ?: entry.key.lowercase()
                },
            )
    }
    LaunchedEffect(store, state.accountId) { store.refreshBoard(); store.syncApproved() }
    fun refresh() { scope.launch { store.refreshBoard(); store.syncApproved(force = true); store.refreshPersonalAliases() } }
    Box(Modifier.fillMaxSize()) {
        PullToRefresh(isRefreshing = state.boardLoading, onRefresh = ::refresh, contentPadding = PaddingValues(top = contentTopPadding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topBarScrollConnection)
                    .then(switcherScrollConnection?.let(Modifier::nestedScroll) ?: Modifier),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentTopPadding + 12.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.syncError?.let { message -> item("sync-error") {
                    CommunityMessageCard(tr("别名同步失败"), tr("{0}\n已有的本地别名仍可使用。", message), tr("重新同步")) { scope.launch { store.syncApproved(force = true) } }
                } }
                if (selectedPage == 0) {
                    if (!state.authenticated) item("login") {
                        CommunityMessageCard(tr("需要登录"), tr("登录后可以投稿和参与社区别名投票。"), tr("登录 Rhythmeta"), onLogin)
                    }
                    state.boardError?.takeIf { state.board.isEmpty() }?.let { message -> item("error") {
                        CommunityMessageCard(tr("加载或投票失败"), message, tr("重试"), ::refresh)
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
                } else {
                    if (approvedGroups.isEmpty()) item("approved-empty") {
                        CommunityMessageCard(tr("暂无已通过的别名"), tr("通过审核的社区别名会显示在这里。"))
                    }
                    approvedGroups.forEach { (songId, aliases) ->
                        item("approved-song:$songId") {
                            CommunitySongHeader(songsById[songId], songId, aliases.size, jacketBaseUrl, localJacketPath, onOpenSong)
                        }
                        items(aliases, key = { "approved:${it.candidateId.ifEmpty { it.songIdentifier + ':' + it.aliasText }}" }) { alias ->
                            CommunityApprovedAliasCard(
                                aliasText = alias.aliasText,
                                approvedAt = alias.approvedAt,
                                isMine = state.personalAliases[songId].orEmpty().any { mine -> mine.equals(alias.aliasText, ignoreCase = true) },
                            )
                        }
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
    val coverNavigation = rememberSongCoverNavigation(song?.songId ?: fallbackTitle,
        { song?.let { onOpenSong(it.songId) } }, 9.dp, cardColor = Color.Transparent, cardCornerRadius = 0.dp)
    SongCardTransitionContent(coverNavigation) { showCover ->
        Row(
            Modifier.fillMaxWidth().then(if (showCover) coverNavigation.cardModifier else Modifier)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    enabled = showCover && song != null,
                    role = Role.Button,
                ) { coverNavigation.open() }
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
                Modifier.size(46.dp).then(if (showCover) coverNavigation.modifier else Modifier).squircleSurface(if (showCover) MiuixTheme.colorScheme.surfaceContainer else Color.Transparent, 9.dp, SquircleExtension),
                contentAlignment = Alignment.Center,
            ) {
                if (showCover) {
                    if (model != null) AsyncImage(model, null, Modifier.fillMaxSize(), placeholder = coverNavigation.source.painter, error = coverNavigation.source.painter, onSuccess = { coverNavigation.onPainter(it.painter) }, contentScale = ContentScale.Crop)
                    else Icon(Icons.Rounded.MusicNote, null)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    song?.let(CatalogSongFormatter::displayTitle) ?: fallbackTitle,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth().fadingMarquee(),
                )
                Text(tr("{0} 个候选别名", count), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
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

/** Candidate/approved switch hosted by the top bar, like maimaid's board switcher. */
@Composable
internal fun CommunityAliasPageSwitcher(
    selectedPage: Int,
    visible: Boolean,
    onSelectedPageChange: (Int) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        TabRowWithContour(
            tabs = listOf(tr("候选"), tr("已通过")),
            selectedTabIndex = selectedPage,
            onTabSelected = onSelectedPageChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            minWidth = 112.dp,
            maxWidth = 180.dp,
        )
    }
}

@Composable
private fun CommunityApprovedAliasCard(
    aliasText: String,
    approvedAt: String?,
    isMine: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    aliasText,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (isMine) {
                    Text(
                        tr("我投稿的"),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .squircleSurface(
                                color = MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
                                cornerRadius = 4.dp,
                                extension = SquircleExtension,
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            approvedAt?.let { value ->
                formatApprovedAt(value)?.let { text ->
                    Text(
                        text,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

private fun formatApprovedAt(value: String): String? = runCatching {
    Instant.parse(value)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}.getOrNull()

private fun formatCommunityDeadline(value: String?): String = value?.let {
    runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) }.getOrNull()
} ?: "--"
