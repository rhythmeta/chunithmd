package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test

class CatalogGridGeometryTest {
    @Test fun headerAndFirstRowStayAtPageTopWhilePinchingBetweenThreeAndFiveColumns() {
        val ids = (0 until 193).map(Int::toString)
        for (initialZoom in listOf(1f, 2f)) {
            val state = CatalogPhotoGridState(zoom = initialZoom)
            state.prepareLayout(390f, 800f, 2f, 340f, 32f, ids, hasHeader = true)
            val anchor = state.anchorAt(Offset(195f, 550f), centerOnCover = true)!!
            for (zoom in listOf(1.8f, 1.5001f, 1.4999f, 1f, 1.3f, 1.7f, 2f)) {
                state.transform(zoom, anchor)
                val grid = state.prepareLayout(390f, 800f, 2f, 340f, 32f, ids, hasHeader = true)
                assertEquals(0f, state.scrollOffset, 0f)
                assertEquals(340f, grid.cell(0).y + state.topPadding - state.scrollOffset, 0f)
                assertFalse(state.outsideScrollBounds)
            }
            // Away from the top, keep the usual cover-centered pinch behavior.
            state.scrollBy(-600f)
            val scrolledAnchor = state.anchorAt(Offset(195f, 550f), centerOnCover = true)!!
            assertEquals(0.5f, scrolledAnchor.fractionY, 0f)
            state.transform(1.4f, scrolledAnchor)
            val cell = state.geometry!!.cell(scrolledAnchor.index)
            assertEquals(scrolledAnchor.point.y,
                cell.y + state.topPadding - state.scrollOffset + cell.size / 2f, 0.001f)
        }
    }

    @Test fun settledLayoutsHaveExactSquareCellsAndTwoPixelGaps() {
        for ((zoom, columns) in listOf(1f to 5, 2f to 3)) {
            val grid = CatalogGridGeometry(390f, 2f, 100, zoom)
            val size = (390f - 2f * (columns - 1)) / columns
            assertEquals(size, grid.cellSize, 0.0001f)
            assertEquals(390f, grid.cell(columns - 1).x + size, 0.0001f)
            assertEquals(size + 2f, grid.cell(columns).y, 0.0001f)
        }
    }

    @Test fun legacyZoomAndOutOfRangeGesturesStayWithinThreeAndFiveColumns() {
        for (zoom in listOf(-10f, 0f, 0.5f, 1f, 1.4f, 1.6f, 2f, 10f)) {
            val state = CatalogPhotoGridState(zoom = zoom)
            val grid = state.prepareLayout(390f, 800f, 2f, 100f, 96f, (0 until 100).map(Int::toString))
            assertTrue(grid.columns in listOf(3, 5))
            assertTrue(state.zoom in 1f..2f)
            assertEquals(1f, grid.zoomForSize(0f), 0f)
            assertEquals(2f, grid.zoomForSize(1000f), 0f)
        }
        assertEquals(5, catalogGridColumns(0f))
    }

    @Test fun densityChangesReplaceCoversWithoutMovingTheirSlots() {
        for (boundary in listOf(1.5f)) {
            for (direction in listOf(-1f, 1f)) {
                val state = CatalogPhotoGridState(300)
                state.prepareLayout(390f, 800f, 2f, 100f, 96f, (0 until 2000).map(Int::toString))
                val anchor = state.anchorAt(Offset(160f, 400f))!!
                state.transform(boundary - direction * 0.00001f, anchor)
                fun positions(): Map<Pair<Int, Int>, Pair<Offset, Int>> {
                    val grid = state.geometry!!
                    return grid.visibleSlots(0f, state.scrollOffset - state.topPadding,
                        390f, state.scrollOffset - state.topPadding + 800f, true)
                        .associate { slot ->
                            val cell = grid.slotCell(slot.row, slot.column)
                            state.slotKey(slot) to (Offset(cell.x,
                                cell.y + state.topPadding - state.scrollOffset) to slot.index)
                        }
                }
                val before = positions()
                state.transform(boundary + direction * 0.00001f, anchor)
                val after = positions()
                val common = before.keys.intersect(after.keys)
                assertTrue(common.size > 8)
                assertTrue(common.any { before.getValue(it).second != after.getValue(it).second })
                common.forEach { key ->
                    val old = before.getValue(key).first
                    val new = after.getValue(key).first
                    assertEquals(old.x, new.x, 0.02f)
                    assertEquals(old.y, new.y, 0.02f)
                }
            }
        }
    }

    @Test fun exposedSidesAreFilledWithRepeatedCoversAndUniqueSlots() {
        for (zoom in listOf(1.1f, 1.3f, 1.6f, 1.9f)) {
            val grid = CatalogGridGeometry(390f, 2f, 2000, zoom)
            for (translation in listOf(-310f, -80f, 0f, 95f, 310f)) {
                val slots = grid.visibleSlots(-translation, 0f, 390f - translation, 800f, true)
                assertEquals(slots.size, slots.map { it.row to it.column }.distinct().size)
                slots.groupBy { it.row }.forEach { (_, row) ->
                    assertTrue(grid.slotCell(row.first().row, row.first().column).x + translation <= 2f)
                    val last = grid.slotCell(row.last().row, row.last().column)
                    assertTrue(last.x + last.size + translation >= 388f)
                    row.zipWithNext().forEach { (left, right) ->
                        assertEquals(left.column + 1, right.column)
                    }
                    row.forEach { assertEquals(it.row, it.index / grid.columns) }
                }
                slots.filter { it.filler }.forEach {
                    assertEquals(Math.floorMod(it.column, grid.columns), it.index % grid.columns)
                }
            }
        }
    }

    @Test fun settledGridDoesNotExposeDuplicateItemsOrInventSongsInLastRow() {
        val grid = CatalogGridGeometry(390f, 2f, 11, 1f)
        val settled = grid.visibleSlots(-200f, 0f, 600f, 800f, false)
        assertEquals((0 until 11).toList(), settled.map { it.index })
        assertTrue(settled.none { it.filler })
        val pinching = grid.visibleSlots(-200f, 0f, 600f, 800f, true)
        assertTrue(pinching.filter { it.row == 2 }.all { it.index == 10 })
        assertTrue(CatalogGridGeometry(390f, 2f, 0, 1f).visibleSlots(-100f, 0f, 600f, 800f, true).isEmpty())
    }

    @Test fun pinchSizeTracksFingersWithoutDensityThresholds() {
        val grid = CatalogGridGeometry(390f, 2f, 2000, 1.4f)
        for (scale in listOf(0.8f, 0.95f, 1.01f, 1.05f, 1.2f)) {
            val next = CatalogGridGeometry(390f, 2f, 2000, grid.zoomForSize(grid.cellSize * scale))
            assertEquals(grid.cellSize * scale, next.cellSize, 0.0001f)
        }
    }

    @Test fun visibleRangeMatchesBruteForceAtEveryDensity() {
        for (zoom in listOf(1f, 1.2f, 1.4f, 1.6f, 1.8f, 2f)) {
            val grid = CatalogGridGeometry(390f, 2f, 2000, zoom)
            for (top in listOf(-150f, 0f, 800f, 16000f, grid.contentHeight - 400f)) {
                val expected = (0 until 2000).filter {
                    val cell = grid.cell(it)
                    cell.y + cell.size > top && cell.y < top + 800f
                }
                assertEquals(expected, grid.visibleIndices(top, top + 800f).toList())
            }
        }
        assertTrue(CatalogGridGeometry(390f, 2f, 0, 1f).visibleIndices(0f, 800f).isEmpty())
    }

    @Test fun verticalFocalPointStaysFixedAndCanBeReanchoredAtAnIntermediateFrame() {
        val state = CatalogPhotoGridState(100)
        state.prepareLayout(390f, 800f, 2f, 100f, 96f, (0 until 2000).map(Int::toString))
        val focus = Offset(160f, 400f)
        val anchor = state.anchorAt(focus)!!
        state.transform(1.4f, anchor)
        val cell = state.geometry!!.cell(anchor.index)
        assertEquals(focus.y, cell.y + state.topPadding - state.scrollOffset + cell.size * anchor.fractionY, 0.01f)
        val oldOffset = state.scrollOffset
        state.transform(state.zoom, state.anchorAt(focus)!!)
        assertEquals(oldOffset, state.scrollOffset, 0.01f)
    }

    @Test fun horizontalFingerMovementCannotPanOrReassignColumns() {
        val state = CatalogPhotoGridState(100)
        state.prepareLayout(390f, 800f, 2f, 100f, 96f, (0 until 2000).map(Int::toString))
        val anchor = state.anchorAt(Offset(160f, 400f))!!
        for (zoom in listOf(1.4f, 1.6f, 2f, 1.2f, 1.1f, 1f)) {
            state.transform(zoom, anchor)
            val offset = state.scrollOffset
            val grid = state.geometry!!
            val slots = grid.visibleSlots(0f, offset, 390f, offset + 800f, true)
            val keys = slots.map(state::slotKey)
            val cells = slots.map { grid.slotCell(it.row, it.column) }
            for (x in listOf(-200f, 0f, 390f, 700f)) {
                assertEquals(state.anchorAt(Offset(195f, 400f)), state.anchorAt(Offset(x, 400f)))
                state.transform(zoom, anchor)
                assertEquals(offset, state.scrollOffset, 0.001f)
                assertEquals(keys, slots.map(state::slotKey))
                assertEquals(cells, slots.map { state.geometry!!.slotCell(it.row, it.column) })
            }
            val middle = grid.slotCell(0, grid.centerColumn)
            assertEquals(195f, middle.x + middle.size / 2f, 0.0001f)
            assertTrue(keys.zip(slots).all { (key, slot) -> key.second == slot.column - grid.centerColumn })
        }
    }

    @Test fun anchorAlwaysUsesMiddleColumnRegardlessOfFingerPosition() {
        for (zoom in listOf(1f, 1.2f, 1.49f, 1.51f, 1.8f, 2f)) {
            val state = CatalogPhotoGridState(100, zoom = zoom)
            val grid = state.prepareLayout(390f, 800f, 2f, 100f, 96f, (0 until 2000).map(Int::toString))
            val anchor = state.anchorAt(Offset(195f, 400f))!!
            assertEquals(grid.centerColumn, anchor.index % grid.columns)
            for (x in listOf(-100f, 0f, 100f, 390f, 500f)) {
                assertEquals(anchor, state.anchorAt(Offset(x, 400f)))
            }
            val center = grid.slotCell(0, grid.centerColumn)
            assertEquals(195f, center.x + center.size / 2, 0.0001f)
            val left = grid.slotCell(0, grid.centerColumn - 1)
            val right = grid.slotCell(0, grid.centerColumn + 1)
            assertEquals(390f, left.x + right.x + grid.cellSize, 0.0001f)
        }
    }

    @Test fun coverCenterStaysFixedThroughDensityChangesAtBothEndsAndRemeasure() {
        val ids = (0 until 2000).map(Int::toString)
        var extendedPastBoundary = false
        for (start in listOf(0, 1999)) {
            val state = CatalogPhotoGridState(start, zoom = 2f)
            state.prepareLayout(390f, 800f, 2f, 100f, 96f, ids)
            val anchor = state.anchorAt(Offset(195f, 437f), centerOnCover = true)!!
            assertEquals(0.5f, anchor.fractionY, 0f)
            for (zoom in listOf(1.4f, 1.4999f, 1.5001f, 2f, 1.5001f, 1.4999f, 1f)) {
                state.transform(zoom, anchor)
                val offset = state.scrollOffset
                extendedPastBoundary = extendedPastBoundary || state.outsideScrollBounds
                val grid = state.prepareLayout(390f, 800f, 2f, 100f, 96f, ids)
                assertEquals(offset, state.scrollOffset, 0f)
                val cell = grid.cell(anchor.index)
                assertEquals(anchor.point.y, cell.y + cell.size / 2 + state.topPadding - state.scrollOffset, 0.02f)
                val nextAnchor = state.anchorAt(anchor.point, centerOnCover = true)!!
                state.transform(zoom, nextAnchor)
                assertEquals(offset, state.scrollOffset, 0.02f)
            }
        }
        assertTrue(extendedPastBoundary)
    }

    @Test fun overscrollFillsLeadingRowsButNeverExtendsBelowTheLastRow() {
        for (count in listOf(1, 11, 2000)) for (zoom in listOf(1f, 1.4f, 1.6f, 2f)) {
            val grid = CatalogGridGeometry(390f, 2f, count, zoom)
            for (top in listOf(-300f, grid.contentHeight - 300f)) {
                val slots = grid.visibleSlots(0f, top, 390f, top + 800f, true, fillLeadingRows = true)
                assertTrue(slots.isNotEmpty())
                assertTrue(slots.all { it.index in 0 until count })
                assertEquals(slots.size, slots.map { it.row to it.column }.distinct().size)
                val rows = slots.map { it.row }.distinct()
                assertTrue(grid.slotCell(rows.first(), 0).y <= top + grid.gap)
                val last = grid.slotCell(rows.last(), 0)
                assertTrue(last.y + last.size >= minOf(top + 800f, grid.contentHeight) - grid.gap)
                assertTrue(slots.all { it.row < grid.rows })
                slots.filter { it.row !in 0 until grid.rows }.forEach { assertTrue(it.filler) }
                if (top < 0f) assertTrue(slots.any { it.row < 0 })
            }
            assertTrue(grid.visibleSlots(0f, grid.contentHeight + grid.step,
                390f, grid.contentHeight + 800f, true, fillLeadingRows = true).isEmpty())
        }
    }

    @Test fun scrollRestorationAndFilteringKeepAValidAnchor() {
        val state = CatalogPhotoGridState()
        val ids = (0 until 2000).map(Int::toString)
        state.prepareLayout(390f, 800f, 2f, 100f, 96f, ids)
        state.requestScrollToItem(100)
        state.prepareLayout(390f, 800f, 2f, 100f, 96f, ids)
        val originalSong = ids[state.firstVisibleItemIndex]
        state.prepareLayout(780f, 800f, 2f, 100f, 96f, ids.reversed())
        // Restoration retains the song in the first row, which can start in an earlier column.
        assertTrue(ids.reversed().drop(state.firstVisibleItemIndex).take(5).contains(originalSong))
        state.prepareLayout(390f, 800f, 2f, 100f, 96f, listOf("only"))
        assertEquals(0, state.firstVisibleItemIndex)
        assertEquals(0f, state.scrollOffset, 0f)
    }
}
