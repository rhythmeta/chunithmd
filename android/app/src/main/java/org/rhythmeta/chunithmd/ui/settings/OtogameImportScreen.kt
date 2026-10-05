package org.rhythmeta.chunithmd.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.rhythmeta.chunithmd.shared.UserProfile
import org.rhythmeta.chunithmd.shared.importing.OtogameImportController
import org.rhythmeta.chunithmd.shared.localization.tr
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OtogameImportScreen(
    controller: OtogameImportController,
    profile: UserProfile?,
    catalogReady: Boolean,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    DisposableEffect(controller) { onDispose { controller.cancel() } }
    val ready = state.eligible && state.profileId == profile?.id && catalogReady
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(20.dp), cornerRadius = 20.dp,
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer)) {
                ImportProfileHeader(profile?.name, state.connected, Icons.Rounded.SportsEsports)
                Spacer(Modifier.height(20.dp))
                Text(tr("仅导入最近四页游玩记录"), style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Spacer(Modifier.height(16.dp))
                if (state.busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(tr("已获取 {0}/{1} 页", state.page, state.totalPages), style = MiuixTheme.textStyles.footnote1)
                    TextButton(tr("取消"), onClick = controller::cancel, modifier = Modifier.fillMaxWidth())
                } else {
                    Button(onClick = if (state.connected) controller::importScores else onLogin,
                        enabled = ready, colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.connected) tr("导入成绩") else tr("登录 Otogame"))
                    }
                    if (state.connected) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(tr("重新登录"), onClick = onLogin, enabled = ready, modifier = Modifier.weight(1f))
                            TextButton(tr("断开连接"), onClick = controller::disconnect, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (!state.eligible) item { Text(tr("需要启用一个日服档案。"), color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
        if (!catalogReady) item { Text(tr("请先下载静态歌曲数据。"), color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
        state.error?.let { error -> item { Text(error, color = MiuixTheme.colorScheme.error) } }
        state.result?.let { result -> item { ImportResultCard(result) } }
    }
}
