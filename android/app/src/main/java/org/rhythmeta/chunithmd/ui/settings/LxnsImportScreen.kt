package org.rhythmeta.chunithmd.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.rhythmeta.chunithmd.ChunithmdApplication
import org.rhythmeta.chunithmd.profile.ProfileRepository
import org.rhythmeta.chunithmd.score.ScoreRepository
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.importing.*
import org.rhythmeta.chunithmd.shared.localization.tr
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LxnsImportScreen(profiles: ProfileRepository, scores: ScoreRepository, catalog: CatalogBundle?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val client = (context.applicationContext as ChunithmdApplication).lxnsClient
    val scope = rememberCoroutineScope()
    val latestCatalog by rememberUpdatedState(catalog)
    val controller = remember(client, profiles, scores, scope) {
        LxnsImportController(client, scope) { id, payload ->
            scores.importLxns(id, payload, latestCatalog ?: throw LxnsException("catalog_empty"))
        }
    }
    val profile by profiles.activeProfile.collectAsStateWithLifecycle(initialValue = null)
    val state by controller.state.collectAsStateWithLifecycle()
    var browserError by remember { mutableStateOf(false) }
    LaunchedEffect(profile?.id) { browserError = false; controller.selectProfile(profile?.id) }
    DisposableEffect(controller) { onDispose { controller.cancel() } }
    val ready = profile != null && profile?.id == state.profileId && catalog?.catalog?.songs?.isNotEmpty() == true

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(20.dp), cornerRadius = 20.dp,
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer)) {
                ImportProfileHeader(profile?.name, state.connected, Icons.Rounded.AcUnit)
                Spacer(Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        state.busy -> {
                            Text(tr("正在导入…"), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            TextButton(text = tr("取消"), onClick = controller::cancel, modifier = Modifier.fillMaxWidth())
                        }
                        state.authorizationUrl != null -> {
                            Button(onClick = {
                                browserError = false
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.authorizationUrl))) }
                                catch (_: ActivityNotFoundException) { browserError = true }
                            }, enabled = ready, colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.fillMaxWidth()) { Text(tr("前往授权")) }
                            TextField(state.code, controller::setCode, label = tr("粘贴浏览器中的授权码"), modifier = Modifier.fillMaxWidth())
                            Button(onClick = controller::exchangeAndImport, enabled = ready && state.code.isNotBlank(),
                                colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.fillMaxWidth()) { Text(tr("导入成绩")) }
                            TextButton(text = tr("重新授权"), onClick = controller::beginAuthorization, enabled = ready, modifier = Modifier.fillMaxWidth())
                        }
                        state.connected -> {
                            Button(onClick = controller::importScores, enabled = ready, colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.fillMaxWidth()) { Text(tr("导入成绩")) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(text = tr("重新授权"), onClick = controller::beginAuthorization, enabled = ready, modifier = Modifier.weight(1f))
                                TextButton(text = tr("断开连接"), onClick = controller::disconnect, modifier = Modifier.weight(1f))
                            }
                        }
                        else -> Button(onClick = controller::beginAuthorization, enabled = ready,
                            colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.fillMaxWidth()) { Text(tr("授权并导入")) }
                    }
                }
            }
        }
        if (catalog?.catalog?.songs.isNullOrEmpty()) item {
            Text(tr("请先下载静态歌曲数据。"), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        if (browserError) item { Text(tr("无法打开浏览器，请安装浏览器后重试。"), color = MiuixTheme.colorScheme.error) }
        state.error?.let { error -> item { Text(error, color = MiuixTheme.colorScheme.error) } }
        state.result?.let { result -> item { ImportResultCard(result) } }
    }
}
