package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.ScoreRecord
import top.yukonga.miuix.kmp.squircle.squircleSurface

internal data class CatalogProgressBadgeData(
    val entries: List<Pair<CatalogSheet, ScoreRecord?>>,
    val columns: Int,
    val background: Color,
)

/** The tile and moving jacket use the same badge, including spacing and surface. */
@Composable
internal fun CatalogProgressBadge(data: CatalogProgressBadgeData, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .squircleSurface(color = data.background, cornerRadius = 50.dp)
            .padding(horizontal = if (data.columns == 3) 6.dp else 4.dp,
                vertical = if (data.columns == 3) 3.dp else 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        data.entries.forEach { (sheet, score) -> SongScoreProgressDot(sheet, score) }
    }
}
