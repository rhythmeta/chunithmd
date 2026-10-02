package org.rhythmeta.chunithmd.account

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import org.rhythmeta.chunithmd.shared.account.CloudBackup
import org.rhythmeta.chunithmd.shared.account.RhythmetaClient
import org.rhythmeta.chunithmd.shared.account.RhythmetaUser
import org.rhythmeta.chunithmd.shared.backup.BackupCoordinator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun RhythmetaScreen(client: RhythmetaClient, coordinator: BackupCoordinator, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session by client.session.collectAsStateWithLifecycle()
    val coordinatorBusy by coordinator.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var working by remember { mutableStateOf(false) }
    val busy = working || coordinatorBusy
    var snapshots by remember { mutableStateOf<List<CloudBackup>>(emptyList()) }
    var restoreTarget by remember { mutableStateOf<CloudBackup?>(null) }
    fun message(text: String) { scope.launch { snackbar.showSnackbar(text) } }
    fun run(work: suspend () -> Unit) {
        if (busy) return
        working = true
        scope.launch {
            try { work() } catch (error: CancellationException) { throw error } catch (error: Exception) { message(error.localizedMessage ?: "备份失败") }
            finally { working = false }
        }
    }
    LaunchedEffect(session?.user?.id) {
        if (session?.user != null) run { snapshots = client.listBackups() }
        else snapshots = emptyList()
    }
    Box(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { AccountSummaryCard(session?.user) }
            if (session?.user == null) {
                item { CloudSection("Rhythmeta") {
                    CloudActionRow(Icons.AutoMirrored.Rounded.Login, "登录", !busy) { openWebAuth(context, client, "login", ::message) }
                    CloudActionRow(Icons.Rounded.AddCircleOutline, "注册", !busy) { openWebAuth(context, client, "register", ::message) }
                    CloudActionRow(Icons.Rounded.Key, "忘记密码", !busy) { openWebAuth(context, client, "forgot", ::message) }
                } }
            } else {
                item { CloudSection("数据同步") {
                    CloudActionRow(Icons.Rounded.CloudUpload, "备份到云端", !busy) {
                        run { coordinator.backup("${Build.MANUFACTURER} ${Build.MODEL}"); snapshots = client.listBackups(); message("云端备份完成") }
                    }
                    CloudActionRow(Icons.Rounded.Sync, "刷新备份列表", !busy) { run { snapshots = client.listBackups() } }
                    Text("手动创建备份，保留最近三份。持有下载链接的人可读取备份，请勿分享。", modifier = Modifier.padding(16.dp), style = MiuixTheme.textStyles.footnote1)
                    if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } }
                if (snapshots.isEmpty()) item { Text("暂无云备份。", modifier = Modifier.padding(16.dp)) }
                snapshots.forEach { snapshot -> item(key = snapshot.id) {
                    CloudSection(formatBackupDate(snapshot.committedAt)) {
                        CloudValueRow(snapshot.deviceName, "${snapshot.profileCount} 个档案")
                        CloudActionRow(Icons.Rounded.CloudDownload, "从云端恢复", !busy) { restoreTarget = snapshot }
                    }
                } }
                item { LogoutButton(enabled = !busy) { run { client.logout() } } }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
    if (busy && restoreTarget == null) WindowDialog(
        show = true,
        title = "数据同步",
        onDismissRequest = {},
    ) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
    restoreTarget?.let { snapshot -> WindowDialog(
        show = true,
        title = "从云端恢复",
        summary = "这将替换本机该游戏的全部个人数据。恢复前会保存本地回滚副本。",
        onDismissRequest = { if (!busy) restoreTarget = null },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { restoreTarget = null }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            Button(onClick = { run { coordinator.restore(snapshot); restoreTarget = null; message("云端恢复完成") } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("从云端恢复") }
        }
    } }
}

@Composable
private fun AccountSummaryCard(user: RhythmetaUser?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        insideMargin = PaddingValues(20.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (user == null) Icons.Rounded.AccountCircle else Icons.Rounded.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = user?.handle ?: "登录云端",
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user?.email ?: "通过邮箱登录以启用安全的云端同步",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "账户信息用于启用云端同步，令牌会加密存储在本机。",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun CloudSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column {
        SmallTitle(text = title, insideMargin = PaddingValues(horizontal = 4.dp, vertical = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(0.dp),
            cornerRadius = 16.dp,
            content = content,
        )
    }
}

@Composable
private fun CloudActionRow(icon: ImageVector, title: String, enabled: Boolean, onClick: () -> Unit) {
    BasicComponent(
        title = title,
        enabled = enabled,
        onClick = onClick,
        startAction = { MonochromeIcon(icon) },
        endActions = {
            Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.55f),
            )
        },
    )
}

@Composable
private fun CloudValueRow(title: String, value: String) {
    BasicComponent(
        title = title,
        endActions = {
            Text(
                text = value,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun MonochromeIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(24.dp),
        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}

private fun openWebAuth(
    context: Context,
    client: RhythmetaClient,
    mode: String,
    onError: (String) -> Unit,
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(client.loginUrl(mode))))
    } catch (_: ActivityNotFoundException) {
        onError("未找到可用的浏览器")
    }
}

@Composable
private fun LogoutButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            color = MiuixTheme.colorScheme.errorContainer,
            contentColor = MiuixTheme.colorScheme.error,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.error,
            )
            Text(
                text = "退出登录",
                color = MiuixTheme.colorScheme.error,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun formatBackupDate(value: String): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
    }
    return try {
        Instant.parse(value).atZone(ZoneId.systemDefault()).format(formatter)
    } catch (_: DateTimeParseException) {
        value
    }
}
