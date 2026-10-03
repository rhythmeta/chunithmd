package org.rhythmeta.chunithmd.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CommunityAliasHomeCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().height(140.dp)
            .squircleBorder(1.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f), 16.dp, SquircleExtension),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(30.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("社区别名投票", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("浏览公示中的别名并支持/反对", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
