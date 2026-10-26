package org.rhythmeta.chunithmd.ui.update

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.BuildConfig
import org.rhythmeta.chunithmd.core.update.AppUpdateFailure
import org.rhythmeta.chunithmd.core.update.AppUpdatePhase
import org.rhythmeta.chunithmd.core.update.AppUpdateState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import org.rhythmeta.chunithmd.shared.localization.tr
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AppUpdateDialog(
    show: Boolean,
    state: AppUpdateState,
    canInstall: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onViewChanges: () -> Unit,
) {
    val manifest = state.manifest
    val maxContentHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * 0.58f }
    val title = when {
        manifest != null -> tr("发现新 CI 构建")
        state.phase == AppUpdatePhase.Latest -> tr("已是最新构建")
        else -> tr("检查应用更新")
    }
    OverlayDialog(show = show, title = title, onDismissRequest = onDismiss) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = maxContentHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (manifest != null) {
                    Text(
                        text = "${BuildConfig.VERSION_CODE} → ${manifest.versionCode}",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    UpdateInfoRow(tr("分支与提交"), "${manifest.branch} · ${manifest.sha.take(8)}", true)
                    UpdateInfoRow(tr("最新提交作者"), manifest.author)
                    UpdateInfoRow(tr("构建时间"), localizedTime(manifest.builtAt, LocalConfiguration.current.locales[0]))
                    Text(tr("自当前构建以来的更改"), style = MiuixTheme.textStyles.subtitle, modifier = Modifier.padding(top = 4.dp))
                    state.changes.commits.take(2).forEach { commit ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• ${commit.title}", style = MiuixTheme.textStyles.body2)
                            Text(commit.sha.take(8), style = MiuixTheme.textStyles.footnote2.copy(fontFamily = FontFamily.Monospace),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                    if (state.changes.incomplete) {
                        Text(tr("变更记录不完整或仅显示最近 20 条"), style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    TextButton(text = tr("查看完整变更"), onClick = onViewChanges, modifier = Modifier.fillMaxWidth())
                }
                when (state.phase) {
                    AppUpdatePhase.Checking -> {
                        Text(tr("正在检查更新…"))
                        LinearProgressIndicator()
                    }
                    AppUpdatePhase.Downloading -> Unit
                    AppUpdatePhase.Verifying -> {
                        Text(tr("正在验证 APK…"))
                        LinearProgressIndicator()
                    }
                    AppUpdatePhase.Ready -> Text(tr(if (canInstall) "下载完成，准备安装" else "允许安装此来源的应用后继续"))
                    AppUpdatePhase.Latest -> Text(tr("当前已安装最新可用的 CI 构建。"))
                    else -> Unit
                }
                state.failure?.let {
                    Text(tr(failureText(it)), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    if (manifest != null && state.phase == AppUpdatePhase.Failed) {
                        TextButton(text = tr("重新检查"), onClick = onCheck, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state.phase) {
                    AppUpdatePhase.Downloading, AppUpdatePhase.Verifying -> TextButton(
                        text = stringResource(android.R.string.cancel), onClick = onCancel, modifier = Modifier.weight(1f))
                    else -> if (manifest != null && state.artifact != null) {
                        TextButton(text = tr("分享"), onClick = onShare, modifier = Modifier.weight(1f))
                    }
                }
                when (state.phase) {
                    AppUpdatePhase.Downloading, AppUpdatePhase.Verifying -> {
                        val progress = if (state.totalBytes > 0) {
                            (state.downloadedBytes.toDouble() / state.totalBytes).toFloat().coerceIn(0f, 1f)
                        } else {
                            null
                        }
                        DownloadProgressButton(
                            progress = progress,
                            label = progress?.let { tr("{0}%", (it * 100).roundToInt()) } ?: tr("正在下载更新…"),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AppUpdatePhase.Ready -> TextButton(
                        text = tr(if (canInstall) "安装更新" else "允许安装"),
                        onClick = onInstall, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColorsPrimary())
                    AppUpdatePhase.Available, AppUpdatePhase.Failed -> TextButton(
                        text = tr(if (manifest != null) "下载更新" else "重试"),
                        onClick = if (manifest != null) onDownload else onCheck,
                        enabled = state.failure != AppUpdateFailure.Signature && state.failure != AppUpdateFailure.Package,
                        modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColorsPrimary())
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressButton(
    progress: Float?,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val animatedProgress = animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = tween(durationMillis = 320),
        label = "downloadProgress",
    )
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = ButtonDefaults.MinWidth, minHeight = ButtonDefaults.MinHeight)
            .semantics {
                progressBarRangeInfo = progress?.let { ProgressBarRangeInfo(it, 0f..1f) }
                    ?: ProgressBarRangeInfo.Indeterminate
            }
            .squircleSurface(
                color = colorScheme.primary.copy(alpha = DownloadTrackAlpha),
                cornerRadius = ButtonDefaults.CornerRadius,
            )
            .drawBehind {
                val fraction = animatedProgress.value.coerceIn(0f, 1f)
                if (fraction > 0f) {
                    drawRect(
                        color = colorScheme.primary,
                        size = Size(width = size.width * fraction, height = size.height),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.button,
            color = colorScheme.primary,
            modifier = Modifier.padding(ButtonDefaults.InsideMargin),
        )
        // Repaint the label on top of the filled part so it stays legible across the wipe.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics { }
                .drawWithContent {
                    clipRect(right = size.width * animatedProgress.value.coerceIn(0f, 1f)) {
                        this@drawWithContent.drawContent()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.button,
                color = colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun UpdateInfoRow(title: String, value: String, monospace: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(0.dp)) {
        BasicComponent {
            Text(title, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Text(value, style = if (monospace) MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace) else MiuixTheme.textStyles.body2)
        }
    }
}

private fun localizedTime(time: String, locale: Locale): String = runCatching {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale)
        .withZone(ZoneId.systemDefault()).format(Instant.parse(time))
}.getOrDefault(time)

private fun failureText(failure: AppUpdateFailure): String = when (failure) {
    AppUpdateFailure.Network -> "无法连接更新服务，请检查网络后重试。"
    AppUpdateFailure.InvalidManifest -> "更新信息无效，请稍后重试。"
    AppUpdateFailure.UnsupportedDevice -> "当前设备不支持此更新。"
    AppUpdateFailure.Storage -> "存储空间不足，无法下载更新。"
    AppUpdateFailure.Download -> "APK 下载失败，请重试。"
    AppUpdateFailure.Integrity -> "下载文件校验失败，请重新下载。"
    AppUpdateFailure.Package -> "更新包与当前应用不匹配。"
    AppUpdateFailure.Signature -> "更新包签名与当前应用不兼容。"
    AppUpdateFailure.Install -> "无法打开系统安装程序。"
}

private const val DownloadTrackAlpha = 0.18f
