package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.ScoreRecord
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class CatalogListCardAppearance(
    val surface: Color,
    val accent: Color,
    val worldsEnd: Boolean,
    val ultima: Boolean,
)

internal data class CatalogListBadgesData(
    val version: String,
    val background: Color,
    val foreground: Color,
    val entries: List<Pair<CatalogSheet, ScoreRecord?>>,
)

@Composable
internal fun CatalogListCardAccent(appearance: CatalogListCardAppearance, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(2.dp)).let {
        when {
            appearance.worldsEnd -> it.background(Brush.verticalGradient(WORLDS_END_GRADIENT_COLORS))
            appearance.ultima -> it.background(ultimaStripedBrush())
            else -> it.squircleSurface(color = appearance.accent, cornerRadius = 2.dp)
        }
    })
}

/** Shared by the actual row and its navigation overlay, including badge padding and dot spacing. */
@Composable
internal fun CatalogListCardBadges(data: CatalogListBadgesData, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = data.version,
            modifier = Modifier.squircleSurface(color = data.background, cornerRadius = 4.dp)
                .padding(horizontal = 7.dp, vertical = 3.dp),
            color = data.foreground,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            data.entries.forEachIndexed { index, (sheet, score) ->
                Box(Modifier.padding(start = if (index == 0) 0.dp else 3.dp)) {
                    SongScoreProgressDot(sheet, score)
                }
            }
        }
    }
}
