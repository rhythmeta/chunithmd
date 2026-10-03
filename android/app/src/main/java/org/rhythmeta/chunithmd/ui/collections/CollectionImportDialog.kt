package org.rhythmeta.chunithmd.ui.collections

import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun CollectionImportDialog(
    initialValue: String,
    busy: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    val context = LocalContext.current
    var draft by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var clipboardError by remember { mutableStateOf<String?>(null) }
    WindowDialog(show = true, title = "导入收藏夹", onDismissRequest = { if (!busy) onDismiss() }) {
        Text("输入分享链接或 CHMD1 分享码，即可导入收藏夹。")
        Spacer(Modifier.height(12.dp))
        TextField(
            value = draft,
            onValueChange = { draft = it; clipboardError = null; onClearError() },
            label = "收藏夹链接或分享码",
            enabled = !busy,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton("从剪贴板粘贴", enabled = !busy, onClick = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)?.primaryClip
            val value = clipboard?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
            onClearError()
            if (value.isNullOrBlank()) clipboardError = "剪贴板为空，请输入链接或分享码"
            else { draft = value; clipboardError = null }
        })
        (clipboardError ?: error)?.let { Text(it, color = MiuixTheme.colorScheme.error) }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton("取消", enabled = !busy, onClick = onDismiss, modifier = Modifier.weight(1f))
            Button(
                enabled = !busy && draft.isNotBlank(),
                onClick = { onImport(draft) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text(if (busy) "正在导入…" else "导入") }
        }
    }
}
