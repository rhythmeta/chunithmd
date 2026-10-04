package org.rhythmeta.chunithmd.ui.settings

import org.rhythmeta.chunithmd.shared.localization.tr

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import org.rhythmeta.chunithmd.ui.theme.AppThemeSettings
import org.rhythmeta.chunithmd.ui.theme.ColorMode
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop

private val keyColors = listOf(
    0, 0xFFE53935.toInt(), 0xFFD81B60.toInt(), 0xFF8E24AA.toInt(), 0xFF5E35B1.toInt(),
    0xFF3949AB.toInt(), 0xFF1E88E5.toInt(), 0xFF00ACC1.toInt(), 0xFF00897B.toInt(),
    0xFF43A047.toInt(), 0xFFFDD835.toInt(), 0xFFFFB300.toInt(), 0xFFFB8C00.toInt(),
    0xFF6D4C41.toInt(), 0xFF546E7A.toInt(), 0xFFFF80AB.toInt(),
)

@Composable
fun ThemeSettingsScreen(
    modifier: Modifier = Modifier,
    settings: AppThemeSettings,
    contentTopPadding: Dp = 0.dp,
    onColorModeChange: (ColorMode) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    onColorSpecChange: (ColorSpec.SpecVersion) -> Unit,
    onEnableBlurChange: (Boolean) -> Unit,
    onEnableFloatingBottomBarChange: (Boolean) -> Unit,
    onEnableFloatingBottomBarBlurChange: (Boolean) -> Unit,
    onEnablePredictiveBackChange: (Boolean) -> Unit,
    onPageScaleChange: (Float) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = contentTopPadding + 24.dp, start = 12.dp, end = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ThemePreviewCard(settings)
            Spacer(Modifier.height(28.dp))
            TabRow(
                tabs = listOf(tr("跟随系统"), tr("浅色"), tr("深色")),
                selectedTabIndex = when { settings.colorMode.isSystem -> 0; settings.colorMode.isDark -> 2; else -> 1 },
                onTabSelected = { index ->
                    val mode = when (index) { 0 -> ColorMode.SYSTEM; 1 -> ColorMode.LIGHT; else -> ColorMode.DARK }
                    onColorModeChange(if (settings.colorMode.isMonet) mode.toMonetMode() else mode)
                },
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                SwitchPreference(
                    title = tr("Monet 动态颜色"),
                    summary = tr("使用系统动态色板"),
                    checked = settings.colorMode.isMonet,
                    onCheckedChange = { onColorModeChange(if (it) settings.colorMode.toMonetMode() else settings.colorMode.toNonMonetMode()) },
                    startAction = { ThemeIcon(Icons.Rounded.Wallpaper) },
                )
                AnimatedVisibility(settings.colorMode.isMonet) {
                    Column {
                        OverlayDropdownPreference(
                            title = tr("种子颜色"),
                            items = listOf(tr("壁纸取色"), tr("红色"), tr("粉色"), tr("紫色"), tr("深紫色"), tr("靛蓝色"), tr("蓝色"), tr("青色"), tr("蓝绿色"), tr("绿色"), tr("黄色"), tr("琥珀色"), tr("橙色"), tr("棕色"), tr("蓝灰色"), tr("樱花色")),
                            selectedIndex = keyColors.indexOf(settings.keyColor).coerceAtLeast(0),
                            onSelectedIndexChange = { onKeyColorChange(keyColors[it]) },
                            startAction = { ThemeIcon(Icons.Rounded.Colorize) },
                        )
                        OverlayDropdownPreference(
                            title = tr("色板样式"),
                            items = PaletteStyle.entries.map { tr(it.name) },
                            selectedIndex = PaletteStyle.entries.indexOf(settings.paletteStyle).coerceAtLeast(0),
                            onSelectedIndexChange = { onPaletteStyleChange(PaletteStyle.entries[it]) },
                            startAction = { ThemeIcon(Icons.Rounded.Style) },
                        )
                        OverlayDropdownPreference(
                            title = tr("色彩规范"),
                            items = ColorSpec.SpecVersion.entries.map { it.name },
                            selectedIndex = ColorSpec.SpecVersion.entries.indexOf(settings.colorSpec).coerceAtLeast(0),
                            onSelectedIndexChange = { onColorSpecChange(ColorSpec.SpecVersion.entries[it]) },
                            startAction = { ThemeIcon(Icons.Rounded.DesignServices) },
                        )
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SwitchPreference(checked = settings.enableBlur, onCheckedChange = onEnableBlurChange, title = tr("模糊"), summary = tr("在支持的设备上模糊顶部栏和页面"), startAction = { ThemeIcon(Icons.Rounded.BlurOn) })
                }
                SwitchPreference(checked = settings.enableFloatingBottomBar, onCheckedChange = onEnableFloatingBottomBarChange, title = tr("浮动底栏"), summary = tr("使用浮动导航底栏"), startAction = { ThemeIcon(Icons.Rounded.CallToAction) })
                AnimatedVisibility(settings.enableFloatingBottomBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SwitchPreference(checked = settings.enableFloatingBottomBarBlur, onCheckedChange = onEnableFloatingBottomBarBlurChange, title = tr("底栏玻璃"), summary = tr("应用液态玻璃背景效果"), startAction = { ThemeIcon(Icons.Rounded.WaterDrop) })
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SwitchPreference(checked = settings.enablePredictiveBack, onCheckedChange = onEnablePredictiveBackChange, title = tr("预测返回"), summary = tr("使用系统返回手势动画"), startAction = { ThemeIcon(Icons.AutoMirrored.Rounded.MenuOpen) })
                }
                var showScale by rememberSaveable { mutableStateOf(false) }
                var scale by remember(settings.pageScale) { mutableFloatStateOf(settings.pageScale) }
                ArrowPreference(
                    title = tr("页面缩放"),
                    summary = tr("调整页面内容大小"),
                    endActions = { Text("${(scale * 100).toInt()}%") },
                    onClick = { showScale = !showScale },
                    holdDownState = showScale,
                    bottomAction = {
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            onValueChangeFinished = { onPageScaleChange(scale) },
                            valueRange = 0.8f..1.1f,
                            showKeyPoints = true,
                            keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                            magnetThreshold = 0.01f,
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        )
                    },
                    startAction = { ThemeIcon(Icons.Rounded.AspectRatio) },
                )
                PageScaleDialog(showScale, { settings.pageScale }, onPageScaleChange) { showScale = false }
            }
        }
    }
}

@Composable
private fun ThemeIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 6.dp), tint = MiuixTheme.colorScheme.onBackground)
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCard(settings: AppThemeSettings) {
    val configuration = LocalConfiguration.current
    val ratio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    val colors = MiuixTheme.colorScheme
    val background = if (settings.colorMode.isAmoled) Color.Black else if (settings.colorMode.isMonet) colors.background else colors.surface
    val card = colors.surfaceContainer
    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
        val shape = RoundedCornerShape(20.dp)
        Box(Modifier.fillMaxWidth(0.42f).aspectRatio(ratio).clip(shape).background(background).border(1.dp, colors.outline, shape)) {
            Column(
                Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(tr("主页"), style = MiuixTheme.textStyles.footnote2, color = colors.onSurface, modifier = Modifier.padding(start = 4.dp))
                // Profile and Best table, followed by the paired home shortcuts.
                Box(Modifier.fillMaxWidth().weight(0.8f).background(card, RoundedCornerShape(6.dp)))
                Box(Modifier.fillMaxWidth().weight(0.55f).background(Color(0xFFFF9500).copy(alpha = 0.16f), RoundedCornerShape(6.dp)))
                repeat(4) { row ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f).fillMaxSize().background(card, RoundedCornerShape(6.dp)))
                        if (row < 3) {
                            Box(Modifier.weight(1f).fillMaxSize().background(card, RoundedCornerShape(6.dp)))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (settings.enableFloatingBottomBar) {
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).height(28.dp)
                        .background(
                            (if (settings.colorMode.isMonet) colors.surfaceContainer else colors.surface)
                                .copy(alpha = if (settings.enableFloatingBottomBarBlur) 0.5f else 1f),
                            RoundedCornerShape(14.dp),
                        )
                        .border(0.5.dp, colors.onSurface.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                ) { repeat(4) { Box(Modifier.size(13.dp).background(if (it == 0) colors.primary else colors.onSurface.copy(alpha = 0.5f), RoundedCornerShape(3.dp))) } }
            } else {
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(34.dp).background(colors.surface),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { Box(Modifier.size(13.dp).background(if (it == 0) colors.primary else colors.onSurface.copy(alpha = 0.5f), RoundedCornerShape(3.dp))) }
                }
            }
        }
    }
}

@Composable
private fun PageScaleDialog(show: Boolean, currentScale: () -> Float, onChange: (Float) -> Unit, onDismiss: () -> Unit) {
    OverlayDialog(show = show, title = tr("页面缩放"), summary = tr("调整页面内容大小"), onDismissRequest = onDismiss, content = {
        var value by remember(show) { mutableStateOf((currentScale() * 100).toInt().toString()) }
        top.yukonga.miuix.kmp.basic.TextField(
            value = value,
            maxLines = 1,
            onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) value = it },
            trailingIcon = { Text("%", Modifier.padding(horizontal = 16.dp), color = MiuixTheme.colorScheme.onSurfaceVariantActions) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(text = tr("取消"), onClick = onDismiss, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(20.dp))
            TextButton(text = tr("确定"), onClick = { onChange((value.toIntOrNull() ?: (currentScale() * 100).toInt()).coerceIn(80, 110) / 100f); onDismiss() }, modifier = Modifier.weight(1f))
        }
    })
}
