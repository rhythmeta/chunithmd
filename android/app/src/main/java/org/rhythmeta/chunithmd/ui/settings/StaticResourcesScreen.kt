package org.rhythmeta.chunithmd.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.shared.CatalogSyncStage
import org.rhythmeta.chunithmd.shared.CatalogSyncState
import org.rhythmeta.chunithmd.shared.StaticManifest
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun StaticResourcesScreen(
    modifier: Modifier = Modifier,
    manifest: StaticManifest?,
    sync: CatalogSyncState,
    error: String?,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
) {
    LaunchedEffect(Unit) { onCheck() }

    val isSyncing = sync.stage in setOf(
        CatalogSyncStage.Checking,
        CatalogSyncStage.Downloading,
        CatalogSyncStage.Validating,
        CatalogSyncStage.Applying,
    )
    val hasError = error != null || sync.stage == CatalogSyncStage.Failed
    val updateAvailable = sync.message == "发现可用更新"
    val upToDate = manifest != null && !isSyncing && !hasError && !updateAvailable
    val statusIcon = when {
        hasError -> Icons.Rounded.ErrorOutline
        isSyncing -> Icons.Rounded.Sync
        updateAvailable -> Icons.Rounded.CloudDownload
        upToDate -> Icons.Rounded.CheckCircleOutline
        else -> Icons.Rounded.Sync
    }
    val statusCardColor = when {
        hasError -> MiuixTheme.colorScheme.errorContainer
        updateAvailable -> MiuixTheme.colorScheme.tertiaryContainer
        upToDate -> MiuixTheme.colorScheme.secondaryContainer
        else -> MiuixTheme.colorScheme.surfaceContainerHighest
    }
    val statusContentColor = when {
        hasError -> MiuixTheme.colorScheme.onErrorContainer
        updateAvailable -> MiuixTheme.colorScheme.onTertiaryContainer
        upToDate -> MiuixTheme.colorScheme.onSecondaryContainer
        else -> MiuixTheme.colorScheme.onSurface
    }
    val statusTitle = when {
        hasError -> "检查失败：${error.orEmpty()}"
        isSyncing -> "正在检查更新…"
        updateAvailable -> "发现可用更新"
        upToDate -> "已是最新静态数据"
        else -> "准备检查更新"
    }
    val statusDescription = when {
        hasError -> "请检查网络或后端状态后重试。"
        isSyncing -> "正在从后端获取最新清单。"
        updateAvailable -> "点击下方按钮下载并应用完整更新。"
        upToDate -> "当前本地数据与服务端最新版本一致。"
        else -> "进入页面后会自动检查静态数据更新。"
    }
    val actionTitle = when {
        updateAvailable -> "下载并更新"
        upToDate -> "重新安装当前版本"
        else -> "立即更新"
    }
    val actionIcon: ImageVector = when {
        updateAvailable -> Icons.Rounded.ArrowDownward
        upToDate -> Icons.Rounded.Refresh
        else -> Icons.Rounded.Sync
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(16.dp),
                colors = CardDefaults.defaultColors(
                    color = statusCardColor,
                    contentColor = statusContentColor,
                ),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 65.dp, y = 35.dp),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusContentColor.copy(alpha = 0.18f),
                            modifier = Modifier.size(132.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(end = 64.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = statusTitle, style = MiuixTheme.textStyles.title3)
                        Text(
                            text = statusDescription,
                            style = MiuixTheme.textStyles.body2,
                            color = statusContentColor.copy(alpha = 0.82f),
                        )
                        manifest?.let {
                            Text(
                                text = "版本：${it.version} · 构建：${it.createdAt.ifBlank { "未知时间" }}",
                                style = MiuixTheme.textStyles.footnote2,
                                color = statusContentColor.copy(alpha = 0.68f),
                            )
                        }
                    }
                }
            }
        }
        item {
            SmallTitle(
                text = "更新操作",
                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        item {
            if (isSyncing) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "正在检查更新…",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StaticResourcesActionButton(
                        title = actionTitle,
                        icon = actionIcon,
                        primary = true,
                        onClick = onDownload,
                    )
                    StaticResourcesActionButton(
                        title = "重新检查更新",
                        icon = Icons.Rounded.Search,
                        onClick = onCheck,
                    )
                }
            }
        }
    }
}

@Composable
private fun StaticResourcesActionButton(
    title: String,
    icon: ImageVector,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (primary) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = MiuixTheme.textStyles.button)
    }
}
