package org.rhythmeta.chunithmd.ui.community

import org.rhythmeta.chunithmd.shared.localization.tr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.rhythmeta.chunithmd.shared.community.*
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SongCommunityAliasSection(
    songId: String,
    store: CommunityAliasStore,
    surfaceColor: Color,
    accentColor: Color,
    showMessage: (String) -> Unit,
    onOpenBoard: () -> Unit,
    onLogin: () -> Unit,
) {
    val state by store.state.collectAsState()
    val songState = state.song(songId)
    val scope = rememberCoroutineScope()
    var showAll by rememberSaveable(songId, state.accountId) { mutableStateOf(false) }
    LaunchedEffect(store, songId, state.accountId) {
        store.refreshSong(songId)
        store.syncApproved()
    }
    LaunchedEffect(songState.message, songState.error) {
        (songState.error ?: songState.message)?.let {
            showMessage(it)
            store.consumeSongFeedback(songId)
        }
    }
    fun submit() {
        if (state.canSubmit(songId)) scope.launch { store.submit(songId) }
    }

    Column(
        Modifier.fillMaxWidth()
            .squircleSurface(surfaceColor, 18.dp, SquircleExtension)
            .squircleBorder(0.5.dp, accentColor.copy(alpha = 0.58f), 18.dp, SquircleExtension)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("社区别名"), style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Bold)
                val approvedCount = state.approvedAliases[songId].orEmpty().size
                Text(
                    if (approvedCount == 0) tr("暂无已通过的别名") else tr("{0} 个已通过别名", approvedCount),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Text(
                tr("公示投票"),
                style = MiuixTheme.textStyles.footnote1,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenBoard),
            )
        }
        Spacer(Modifier.height(12.dp))
        if (!state.authenticated) {
            Text(
                tr("登录后可以投稿和参与社区别名投票"),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.clickable(role = Role.Button, onClick = onLogin),
            )
        } else {
            Text(
                tr("今日已投稿：{0}/{1}", state.dailyUsed?.toString() ?: "—", CommunityAliasDailyQuota),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (state.dailyUsed ?: 0).toFloat() / CommunityAliasDailyQuota,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = quotaColor(state.dailyUsed ?: 0),
                    backgroundColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                ),
                height = 6.dp,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = songState.draft,
                    onValueChange = { store.setDraft(songId, it) },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = accentColor.copy(alpha = 0.14f).compositeOver(surfaceColor),
                        labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        borderColor = accentColor,
                    ),
                    label = tr("输入新别名"),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    enabled = !songState.submitting,
                    modifier = Modifier.weight(1f).height(52.dp),
                    cornerRadius = 12.dp,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Button(
                    onClick = ::submit,
                    enabled = state.canSubmit(songId),
                    modifier = Modifier.height(52.dp),
                    minWidth = 50.dp,
                    cornerRadius = 8.dp,
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        color = accentColor.copy(alpha = 0.14f),
                        contentColor = accentColor,
                    ),
                ) {
                    if (songState.submitting) {
                        CircularProgressIndicator(size = 18.dp, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, tr("提交别名"), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        if (songState.candidates.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                tr("我的投稿"),
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(4.dp))
            (if (showAll) songState.candidates else songState.candidates.take(4)).forEach { candidate ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(candidate.aliasText, Modifier.weight(1f), style = MiuixTheme.textStyles.footnote1)
                    val (label, color) = candidateStatus(candidate.status)
                    Text(label, style = MiuixTheme.textStyles.footnote2, fontWeight = FontWeight.Bold, color = color)
                }
            }
            if (songState.candidates.size > 4) {
                Text(
                    if (showAll) tr("收起") else tr("查看全部 {0} 条", songState.candidates.size),
                    color = accentColor,
                    style = MiuixTheme.textStyles.footnote2,
                    modifier = Modifier.padding(top = 6.dp).clickable(role = Role.Button) { showAll = !showAll },
                )
            }
        }
        if (state.syncError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                tr("社区别名同步失败，点击重试"),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.clickable(role = Role.Button) {
                    scope.launch { store.syncApproved(force = true); store.refreshSong(songId) }
                },
            )
        }
    }
}

private fun quotaColor(used: Int): Color = when {
    used >= CommunityAliasDailyQuota -> Color(0xFFD65C5C)
    used >= CommunityAliasDailyQuota - 1 -> Color(0xFFE59A3A)
    else -> Color(0xFF36A65C)
}

private fun candidateStatus(status: String): Pair<String, Color> = when (status) {
    "pool_private", "voting" -> tr("投票中") to Color(0xFF4385D8)
    "approved" -> tr("已通过") to Color(0xFF36A65C)
    "rejected" -> tr("已拒绝") to Color(0xFFD65C5C)
    else -> tr("未知") to Color.Gray
}
