package org.rhythmeta.chunithmd.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import org.rhythmeta.chunithmd.ui.components.FishIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun DivingFishImportScreen(
    profiles: ProfileRepository,
    scores: ScoreRepository,
    catalog: CatalogBundle?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val client = (context.applicationContext as ChunithmdApplication).divingFishClient
    val scope = rememberCoroutineScope()
    val latestCatalog by rememberUpdatedState(catalog)
    val controller = remember(client, profiles, scores, scope) {
        DivingFishImportController(client, scope) { profileId, payload ->
            scores.importDivingFish(profileId, payload, latestCatalog ?: throw DivingFishException("catalog_empty"))
        }
    }
    val profile by profiles.activeProfile.collectAsStateWithLifecycle(initialValue = null)
    val state by controller.state.collectAsStateWithLifecycle()
    var browserError by remember { mutableStateOf(false) }
    LaunchedEffect(profile?.id) { controller.selectProfile(profile?.id) }
    DisposableEffect(controller) { onDispose { controller.cancel() } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(20.dp),
                cornerRadius = 20.dp,
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
            ) {
                ImportProfileHeader(profile?.name, state.connected, FishIcon)
                Spacer(Modifier.height(24.dp))
                if (!state.busy) {
                    Button(
                        onClick = { browserError = false; if (state.connected) controller.importScores() else controller.authorizeAndImport() },
                        enabled = profile != null && profile?.id == state.profileId && catalog?.catalog?.songs?.isNotEmpty() == true,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (state.connected) tr("导入成绩") else tr("授权并导入")) }
                    if (state.connected) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(text = tr("断开连接"), onClick = controller::disconnect, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            when (state.phase) {
                                DivingFishImportPhase.Authorizing -> tr("正在获取授权码…")
                                DivingFishImportPhase.Waiting -> tr("等待授权")
                                else -> tr("正在导入…")
                            },
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        state.userCode?.let { code ->
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SelectionContainer {
                                    Text(code, fontSize = 28.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                }
                                Text(tr("请核对授权页上的用户码"), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                        }
                        state.authorizationUrl?.let { url ->
                            Button(
                                onClick = {
                                    browserError = false
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                    catch (_: ActivityNotFoundException) { browserError = true }
                                },
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(tr("前往授权")) }
                        }
                        TextButton(text = tr("取消"), onClick = controller::cancel, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        if (catalog == null || catalog.catalog.songs.isEmpty()) item {
            Text(tr("请先下载静态歌曲数据。"), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        if (browserError) item { Text(tr("无法打开浏览器，请安装浏览器后重试。"), color = MiuixTheme.colorScheme.error) }
        state.error?.let { error -> item { Text(error, color = MiuixTheme.colorScheme.error) } }
        state.result?.let { result -> item { ImportResultCard(result) } }
    }
}
