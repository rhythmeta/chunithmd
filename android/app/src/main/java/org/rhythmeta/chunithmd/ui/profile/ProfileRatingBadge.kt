package org.rhythmeta.chunithmd.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.rhythmeta.chunithmd.shared.localization.tr
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ProfileRatingBadge(rating: Double, modifier: Modifier = Modifier) {
    // Match the Rating palette used by the Best table export.
    val colors = when {
        rating >= 15.0 -> listOf(Color(0xFFFF5E5E), Color(0xFFFFF75E), Color(0xFF5EFF5E), Color(0xFF5EBAFF), Color(0xFFBA5EFF))
        rating >= 14.5 -> listOf(Color.LightGray, Color.White, Color.LightGray)
        rating >= 14.0 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500))
        else -> listOf(Color(0xFFFF9500), Color(0xFFFF9500))
    }
    val text = String.format(Locale.ROOT, "%.2f", rating)
    val label = tr("玩家 Rating")
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        maxLines = 1,
        modifier = modifier
            .background(Brush.horizontalGradient(colors), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clearAndSetSemantics { contentDescription = "$label $text" },
    )
}
