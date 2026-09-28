package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogFilters
import org.rhythmeta.chunithmd.shared.CatalogQuery
import org.rhythmeta.chunithmd.shared.CatalogVersionFormatter
import org.rhythmeta.chunithmd.ui.components.ExpandableBottomSheet
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import org.rhythmeta.chunithmd.ui.components.squircleShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun CatalogFilterDialog(
    show: Boolean,
    bundle: CatalogBundle,
    settings: CatalogFilters,
    onSettingsChange: (CatalogFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    val darkTheme = MiuixTheme.colorScheme.background.luminance() < 0.5f
    ExpandableBottomSheet(
        visible = show,
        onDismissRequest = onDismiss,
        expandActionLabel = "展开",
        collapseActionLabel = "收起到半屏",
        expandedStateDescription = "已全屏展开",
        halfExpandedStateDescription = "半屏",
        header = {
            IconButton(
                onClick = { onSettingsChange(CatalogFilters()) },
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = "重置筛选")
            }
            Text(
                text = "筛选歌曲",
                style = MiuixTheme.textStyles.title3,
                modifier = Modifier.align(Alignment.Center),
                maxLines = 1,
            )
            IconButton(
                onClick = {
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "完成", tint = MiuixTheme.colorScheme.primary)
            }
        },
    ) { topInset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = topInset + 12.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                CatalogFilterSection("快速筛选") {
                    CatalogFilterToggleRow(
                        icon = Icons.Rounded.PlayCircle,
                        title = "仅显示 JP 可玩",
                        checked = settings.playableOnly,
                        onCheckedChange = { onSettingsChange(settings.copy(playableOnly = it)) },
                    )
                }
            }
            item {
                CatalogFilterSection("难度") {
                    CatalogFilterChipGroup(
                        values = DIFFICULTIES,
                        selectedValues = settings.difficulties,
                        colorForValue = { difficultyFilterColor(it, darkTheme) },
                        onToggle = { value -> onSettingsChange(settings.copy(difficulties = settings.difficulties.toggled(value))) },
                        displayValue = { it.uppercase() },
                        rainbowValue = "world's end",
                    )
                }
            }
            item {
                CatalogFilterSection("分类") {
                    CatalogFilterChipGroup(
                        values = CatalogQuery.availableCategories(bundle),
                        selectedValues = settings.categories,
                        colorForValue = { MiuixTheme.colorScheme.primary },
                        onToggle = { value -> onSettingsChange(settings.copy(categories = settings.categories.toggled(value))) },
                    )
                }
            }
            item {
                CatalogFilterSection("版本") {
                    CatalogFilterChipGroup(
                        values = CatalogQuery.availableVersions(bundle),
                        selectedValues = settings.versions,
                        colorForValue = { MiuixTheme.colorScheme.primary },
                        onToggle = { value -> onSettingsChange(settings.copy(versions = settings.versions.toggled(value))) },
                        displayValue = CatalogVersionFormatter::badge,
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogFilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(start = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            insideMargin = PaddingValues(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
        }
    }
}

@Composable
private fun CatalogFilterToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MiuixTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MiuixTheme.textStyles.body1, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CatalogFilterChipGroup(
    values: List<String>,
    selectedValues: Set<String>,
    colorForValue: @Composable (String) -> Color,
    onToggle: (String) -> Unit,
    displayValue: (String) -> String = { it },
    rainbowValue: String? = null,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        values.forEach { value ->
            CatalogFilterChip(
                title = displayValue(value),
                selected = value in selectedValues,
                color = colorForValue(value),
                rainbow = value.equals(rainbowValue, ignoreCase = true),
                onClick = { onToggle(value) },
            )
        }
    }
}

@Composable
private fun CatalogFilterChip(
    title: String,
    selected: Boolean,
    color: Color,
    rainbow: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) color else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        label = "catalog-filter-chip-background",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected && rainbow) Color.White.copy(alpha = 0.72f)
        else if (selected) color.copy(alpha = 0.45f)
        else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.09f),
        label = "catalog-filter-chip-border",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MiuixTheme.colorScheme.onSurface,
        label = "catalog-filter-chip-content",
    )
    Box(
        modifier = Modifier
            .then(
                if (selected && rainbow) {
                    Modifier
                        .clip(squircleShape(50.dp))
                        .background(Brush.horizontalGradient(WORLDS_END_FILTER_GRADIENT))
                } else {
                    Modifier.squircleSurface(color = backgroundColor, cornerRadius = 50.dp, extension = SquircleExtension)
                },
            )
            .squircleBorder(width = 1.dp, color = borderColor, cornerRadius = 50.dp, extension = SquircleExtension)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title, style = MiuixTheme.textStyles.footnote1, color = contentColor, maxLines = 1)
    }
}

private fun Set<String>.toggled(value: String): Set<String> = if (value in this) this - value else this + value

private fun difficultyFilterColor(value: String, darkTheme: Boolean): Color = when (value.lowercase()) {
    "basic" -> Color(0xFF65B94A)
    "advanced" -> Color(0xFFE6BD31)
    "expert" -> Color(0xFFE34A47)
    "master" -> Color(0xFF9A50C9)
    "ultima" -> if (darkTheme) Color(0xFFB7B7C0) else Color(0xFF222222)
    else -> Color(0xFF4AA8C2)
}

private val WORLDS_END_FILTER_GRADIENT = listOf(
    Color(0xFF65B94A),
    Color(0xFFE6BD31),
    Color(0xFFE34A47),
    Color(0xFF9A50C9),
    Color(0xFF5D5D66),
    Color(0xFF4AA8C2),
)
