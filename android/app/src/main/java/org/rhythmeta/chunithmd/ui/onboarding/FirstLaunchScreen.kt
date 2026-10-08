package org.rhythmeta.chunithmd.ui.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.rhythmeta.chunithmd.R
import org.rhythmeta.chunithmd.shared.CatalogSyncState
import org.rhythmeta.chunithmd.shared.localization.tr
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FirstLaunchScreen(sync: CatalogSyncState, downloading: Boolean, error: String?, onDownload: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.weight(1f).widthIn(max = 560.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("欢迎使用"), style = MiuixTheme.textStyles.title2)
                    Text("chunithmd", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.primary)
                }
                Image(painterResource(R.drawable.ic_launcher_foreground_image), null, Modifier.size(88.dp))
            }
            Text(tr("记录每一次进步。"), style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                OnboardingFeature(Icons.Rounded.CameraAlt, tr("快速扫描"), tr("竖持查歌，横持查分。首次扫描时下载识别模型。"))
                OnboardingFeature(Icons.Rounded.Lock, tr("本地离线"), tr("资源下载后，离线也能查看歌曲与记录成绩。"))
                OnboardingFeature(Icons.Rounded.People, tr("多档案管理"), tr("分别管理不同玩家和服务器的成绩，在设置中创建或切换档案。"))
            }
        }
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().animateContentSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (downloading) {
                sync.progress?.let { LinearProgressIndicator(progress = it.coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth()) }
                    ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(sync.message ?: tr("正在下载歌曲目录"), style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            } else {
                Text(if (error != null) tr("资源下载失败，请检查网络后重试。") else tr("先下载曲库和封面，识别模型可在扫描时按需下载。"),
                    style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Button(onClick = onDownload, enabled = !downloading,
                colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(tr(if (downloading) "正在下载资源…" else if (error != null) "重试下载" else "下载资源并继续"),
                    fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun OnboardingFeature(icon: ImageVector, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, null, tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold)
            Text(detail, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}
