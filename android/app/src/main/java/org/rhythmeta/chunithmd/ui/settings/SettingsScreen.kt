package org.rhythmeta.chunithmd.ui.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsHome(
    modifier: Modifier,
    onAppearance: () -> Unit,
    onResources: () -> Unit,
    onProfiles: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 112.dp),
    ) {
        item {
            SettingsSection(title = "用户") {
                SettingsRow(
                    icon = Icons.Rounded.Person,
                    title = "用户档案",
                    summary = "管理本地档案、头像和地区",
                    onClick = onProfiles,
                )
            }
        }
        item {
            SettingsSection(title = "数据同步") {
                SettingsRow(
                    icon = Icons.Rounded.Download,
                    title = "静态数据",
                    summary = "更新并管理本地歌曲数据",
                    onClick = onResources,
                )
            }
        }
        item {
            SettingsSection(title = "主题") {
                SettingsRow(
                    icon = Icons.Rounded.ColorLens,
                    title = "主题",
                    summary = "自定义更多主题选项",
                    onClick = onAppearance,
                )
            }
        }
    }
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
