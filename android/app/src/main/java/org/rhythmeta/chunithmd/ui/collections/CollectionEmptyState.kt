package org.rhythmeta.chunithmd.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.ui.components.squircleShape
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CollectionEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 400.dp).fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier.size(64.dp).background(
                        MiuixTheme.colorScheme.primary.copy(alpha = 0.08f), squircleShape(20.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, Modifier.size(32.dp), tint = MiuixTheme.colorScheme.primary.copy(alpha = 0.75f))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.SemiBold)
                    Text(message, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
            if (actions != null) {
                Spacer(Modifier.height(24.dp))
                actions()
            }
        }
    }
}
