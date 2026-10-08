package org.rhythmeta.chunithmd.ui.settings

import org.rhythmeta.chunithmd.shared.localization.tr

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.Icons
import org.rhythmeta.chunithmd.ui.components.FishIcon
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.BuildConfig
import org.rhythmeta.chunithmd.shared.account.RhythmetaClient
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsHome(
    modifier: Modifier,
    onAppearance: () -> Unit,
    onResources: () -> Unit,
    onProfiles: () -> Unit,
    onAccount: () -> Unit,
    accountClient: RhythmetaClient,
    onSendLogs: () -> Unit,
    onDivingFish: () -> Unit,
    onLxns: () -> Unit,
    onOtogame: () -> Unit,
    showScannerBoundingBoxes: Boolean,
    onShowScannerBoundingBoxesChange: (Boolean) -> Unit,
) {
    var backendAvailable by remember(accountClient) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(accountClient) {
        backendAvailable = accountClient.isHealthy()
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 112.dp),
    ) {
        item {
            SettingsSection(title = tr("用户")) {
                SettingsRow(
                    icon = Icons.Rounded.People,
                    title = tr("用户档案"),
                    summary = tr("管理本地档案、头像和地区"),
                    onClick = onProfiles,
                )
            }
        }
        item {
            SettingsSection(title = tr("数据同步")) {
                SettingsRow(
                    icon = Icons.Rounded.Download,
                    title = tr("静态数据"),
                    summary = tr("更新并管理本地歌曲数据"),
                    onClick = onResources,
                )
                SettingsRow(
                    icon = Icons.Rounded.Cloud,
                    title = tr("云端账户"),
                    summary = tr("登录账户并通过云端安全同步应用数据"),
                    onClick = onAccount,
                )
            }
        }
        item {
            SettingsSection(title = tr("成绩同步")) {
                SettingsRow(
                    icon = FishIcon,
                    title = tr("水鱼导入"),
                    summary = tr("导入成绩"),
                    onClick = onDivingFish,
                )
                SettingsRow(
                    icon = Icons.Rounded.AcUnit,
                    title = tr("落雪导入"),
                    summary = tr("导入成绩"),
                    onClick = onLxns,
                )
                SettingsRow(
                    icon = Icons.Rounded.SportsEsports,
                    title = tr("Otogame 导入"),
                    summary = tr("导入成绩"),
                    onClick = onOtogame,
                )
            }
        }
        item {
            SettingsSection(title = tr("主题")) {
                SettingsRow(
                    icon = Icons.Rounded.ColorLens,
                    title = tr("主题"),
                    summary = tr("自定义更多主题选项"),
                    onClick = onAppearance,
                )
                SwitchPreference(
                    title = tr("显示识别框"),
                    checked = showScannerBoundingBoxes,
                    onCheckedChange = onShowScannerBoundingBoxesChange,
                    startAction = { SettingsPreferenceIcon(Icons.Rounded.CropFree) },
                )
            }
        }
        item {
            SettingsSection(title = tr("关于")) {
                SettingsHealthRow(backendAvailable)
                SettingsRow(
                    icon = Icons.Rounded.BugReport,
                    title = tr("发送日志"),
                    summary = tr("收集应用诊断信息并分享，用于排查问题"),
                    onClick = onSendLogs,
                )
                BasicComponent(
                    title = tr("版本"),
                    startAction = { SettingsPreferenceIcon(Icons.Rounded.Info) },
                    endActions = {
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsHealthRow(available: Boolean?) {
    val (status, color) = when (available) {
        null -> tr("检查中…") to MiuixTheme.colorScheme.onSurfaceVariantActions
        true -> tr("可用") to Color(0xFF2E7D32)
        false -> tr("不可用") to Color(0xFFC62828)
    }
    BasicComponent(
        title = tr("后端状态"),
        startAction = { SettingsPreferenceIcon(Icons.Rounded.Cloud) },
        endActions = { Text(status, style = MiuixTheme.textStyles.body2, color = color) },
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SmallTitle(text = title)
    MiuixCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        insideMargin = PaddingValues(0.dp),
        cornerRadius = 14.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        content = content,
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = title,
        summary = summary,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        startAction = { SettingsPreferenceIcon(icon) },
    )
}

@Composable
private fun SettingsPreferenceIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MiuixIcon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.padding(end = 6.dp).size(24.dp),
        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}
