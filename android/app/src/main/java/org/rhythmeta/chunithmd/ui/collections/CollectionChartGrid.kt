package org.rhythmeta.chunithmd.ui.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.rhythmeta.chunithmd.ui.catalog.CatalogPhotoGridState
import org.rhythmeta.chunithmd.ui.components.ZoomableCoverGrid

@Composable
internal fun CollectionChartGrid(
    cards: List<CollectionCard>,
    state: CatalogPhotoGridState,
    modifier: Modifier,
    contentTopPadding: Dp,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onOpen: (CollectionCard) -> Unit,
    onDelete: (CollectionCard) -> Unit,
) {
    val itemKeys = remember(cards) { cards.map { it.entry.key } }
    val progressScale = remember(state) { {
        state.zoom
        state.geometry?.let { it.maxCellSize / it.cellSize } ?: 1f
    } }
    ZoomableCoverGrid(modifier, contentTopPadding, itemKeys, state) { index, columns, imageSize, filler ->
        val card = cards[index]
        CollectionChartCard(card, true, jacketBaseUrl, localJacketPath,
            onOpen = { onOpen(card) }, onDelete = { onDelete(card) },
            gridImageSize = imageSize, gridColumns = columns,
            animateCoverChanges = state.transforming, progressScale = progressScale, filler = filler)
    }
}
