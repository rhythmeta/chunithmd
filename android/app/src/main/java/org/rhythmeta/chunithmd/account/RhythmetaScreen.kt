package org.rhythmeta.chunithmd.account

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.rhythmeta.chunithmd.shared.account.*
import org.rhythmeta.chunithmd.shared.backup.BackupCoordinator
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RhythmetaScreen(client: RhythmetaClient, coordinator: BackupCoordinator, modifier: Modifier = Modifier) {
    val session by client.session.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var backups by remember { mutableStateOf(emptyList<CloudBackup>()) }
    var message by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<CloudBackup?>(null) }
    fun perform(action: suspend () -> Unit) {
        scope.launch {
            working=true
            try { action() } catch(error: Exception) { message=error.message ?: "操作失败" }
            finally { working=false }
        }
    }
    LaunchedEffect(session?.user?.id) {
        if(session!=null) try { backups=client.listBackups() } catch(error: Exception) { message=error.message }
        else backups=emptyList()
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text(session?.user?.handle ?: "Rhythmeta 账号", style=MiuixTheme.textStyles.title3)
            Text("与 maimaid 共用账号。备份包含本地档案、成绩、收藏和设置，保留最近三份。")
        }
        item { message?.let { Text(it) } }
        if(session==null) {
            item { Button(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(client.loginUrl()))) }) { Text("登录 / 注册") } }
        } else {
            item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Button(enabled=!working,onClick={perform { coordinator.backup("${Build.MANUFACTURER} ${Build.MODEL}"); backups=client.listBackups(); message="备份完成" }}) { Text("立即备份") }
                TextButton(text="刷新", enabled=!working,onClick={perform { backups=client.listBackups() }})
            } }
            items(backups,key={it.id}) { backup ->
                Card(insideMargin=PaddingValues(16.dp)) {
                    Text(backup.committedAt)
                    Text("${backup.deviceName} · ${backup.profileCount} 个档案 · ${backup.size / 1024} KB")
                    TextButton(text="恢复此备份",enabled=!working,onClick={selected=backup})
                }
            }
            item { TextButton(text="退出账号",enabled=!working,onClick={perform { client.logout() }}) }
        }
    }
    selected?.let { backup ->
        Dialog(onDismissRequest={selected=null}) {
            Card(insideMargin=PaddingValues(20.dp)) {
                Text("替换全部本地数据？",style=MiuixTheme.textStyles.title3)
                Text("将覆盖本地所有档案、成绩、收藏和设置。开始前会保存回滚副本，失败时自动恢复。")
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    TextButton(text="取消",onClick={selected=null})
                    Button(onClick={selected=null;perform {coordinator.restore(backup);message="恢复完成"}}) {Text("替换并恢复")}
                }
            }
        }
    }
    val busy by coordinator.busy.collectAsState()
    if(busy) Dialog(onDismissRequest={},properties=DialogProperties(dismissOnBackPress=false,dismissOnClickOutside=false)) {
        Card(insideMargin=PaddingValues(24.dp)) { Text("正在处理备份，请稍候…") }
    }
}
