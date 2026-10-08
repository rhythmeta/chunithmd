package org.rhythmeta.chunithmd.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.shared.localization.tr
import org.rhythmeta.chunithmd.shared.scanner.ScannerModelManager
import org.rhythmeta.chunithmd.shared.scanner.ScannerModelState
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.squircle.squircleSurface

@Composable
internal fun ScannerModelDownloadPanel(state: ScannerModelState, models: ScannerModelManager,
    modifier: Modifier = Modifier, compact: Boolean = false) {
    if (state.stage == "loading") return
    Column(modifier.widthIn(max = 460.dp).squircleSurface(Color(0xEE202020), 24.dp, SquircleExtension).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr(when (state.stage) {
            "checking" -> "正在检查识别模型…"
            "downloading" -> "正在下载识别模型…"
            "update" -> "识别模型有更新"
            "failed" -> "模型下载失败"
            else -> "下载识别模型"
        }), color = Color.White)
        if (!compact) Text(tr("首次扫描需要下载模型，下载后可离线识别。"), color = Color.White.copy(alpha = .75f))
        when (state.stage) {
            "checking" -> CircularProgressIndicator(size = 24.dp)
            "downloading" -> {
                val percent = if (state.totalBytes > 0) (state.downloadedBytes * 100 / state.totalBytes).toInt() else 0
                LinearProgressIndicator(progress = percent / 100f, modifier = Modifier.fillMaxWidth())
                Text("${modelSize(state.downloadedBytes)} / ${modelSize(state.totalBytes)} · $percent%", color = Color.White)
                TextButton(text = tr("取消"), onClick = models::cancelDownload)
            }
            "failed" -> {
                Text(tr("请检查网络连接后重试。"), color = Color.White.copy(alpha = .75f))
                Button(onClick = models::check, colors = ButtonDefaults.buttonColorsPrimary()) { Text(tr("重试")) }
            }
            else -> {
                Button(onClick = models::download, colors = ButtonDefaults.buttonColorsPrimary()) {
                    Text(tr(if (state.usable) "更新模型" else "下载模型") + " · " + modelSize(state.totalBytes))
                }
            }
        }
        if (state.usable) Text(tr("现有模型仍可离线使用。"), color = Color.White.copy(alpha = .65f))
    }
}

private fun modelSize(bytes: Long): String = "%.1f MB".format(bytes / 1_000_000.0)
