package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test
import top.yukonga.miuix.kmp.nav.runtime.NavChange
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope

class CatalogCoverTransitionTest {
    private class Navigation : NavTransitionScope {
        override var relativeDepth = -1f
        override var isRunning = true
        override val role = NavRole.Incoming
        override val change = NavChange.Push
        override val gesture = null
        override val layoutSize = IntSize(390, 844)
        override val layoutDirection = LayoutDirection.Ltr
        override val density = Density(1f)
    }

    @Test fun sourceVisibilityHandsOffAtTheExactDetailEndpointRegardlessOfRunningFlag() {
        val state = CatalogCoverTransition("song", Rect(0f, 100f, 76f, 176f), null, 24.dp) { Offset.Zero }
        val entering = Navigation()
        assertFalse(state.hidesSource)
        state.attach(entering)
        assertFalse(state.hidesSource) // Running, but the container is still at the source endpoint.
        entering.relativeDepth = -0.9999f
        assertTrue(state.hidesSource)
        entering.relativeDepth = 0f
        entering.isRunning = false
        assertTrue(state.hidesSource) // Settled detail continues to own the image.

        val returning = Navigation().apply { relativeDepth = -0.5f }
        state.attach(returning)
        assertTrue(state.hidesSource)
        returning.relativeDepth = -1f
        assertFalse(state.hidesSource) // Restore before disposal, even if isRunning lags a frame.
        returning.relativeDepth = -0.5f
        assertTrue(state.hidesSource)
        state.detach()
        assertFalse(state.hidesSource)
    }

    @Test fun containerConnectsThreeAndFiveColumnTilesToTheFullPage() {
        val page = Size(390f, 844f)
        for (zoom in listOf(1f, 2f)) {
            val grid = CatalogGridGeometry(page.width, 2f, 100, zoom)
            for (column in 0 until grid.columns) {
                val cell = grid.slotCell(0, column)
                val source = Rect(cell.x, 300f, cell.x + cell.size, 300f + cell.size)
                assertEquals(source, catalogContainerBounds(source, page, 0f))
                assertEquals(Rect(Offset.Zero, page), catalogContainerBounds(source, page, 1f))
                for (step in 0..100) {
                    val bounds = catalogContainerBounds(source, page, step / 100f)
                    assertTrue(bounds.width >= source.width - 0.001f)
                    assertTrue(bounds.height >= source.height - 0.001f)
                    assertTrue(bounds.left <= source.left + 0.001f)
                    assertTrue(bounds.top <= source.top + 0.001f)
                    assertTrue(bounds.right >= source.right - 0.001f)
                    assertTrue(bounds.bottom >= source.bottom - 0.001f)
                }
            }
        }
    }

    @Test fun movingSourceUpdatesReturnAnchorWithoutMovingTheFullscreenEndpoint() {
        val page = Size(390f, 844f)
        val source = Rect(80f, 500f, 156f, 576f)
        val scrolled = source.translate(Offset(0f, -120f))
        assertEquals(scrolled, catalogContainerBounds(scrolled, page, 0f))
        assertEquals(catalogContainerBounds(source, page, 1f), catalogContainerBounds(scrolled, page, 1f))
        val before = catalogContainerBounds(source, page, 0.5f)
        val after = catalogContainerBounds(scrolled, page, 0.5f)
        assertEquals(-60f, after.top - before.top, 0.001f)
        assertEquals(before.size, after.size)
    }

    @Test fun sourceAndDetailNeverFadeOverEachOtherInEitherDirection() {
        assertEquals(1f, catalogCoverAlpha(0f), 0f)
        assertEquals(0f, catalogDetailAlpha(0f), 0f)
        assertEquals(0f, catalogCoverAlpha(1f), 0f)
        assertEquals(1f, catalogDetailAlpha(1f), 0f)
        for (step in (0..100) + (100 downTo 0)) {
            val progress = step / 100f
            val cover = catalogCoverAlpha(progress)
            val detail = catalogDetailAlpha(progress)
            assertTrue(cover in 0f..1f && detail in 0f..1f)
            assertEquals(0f, cover * detail, 0.00001f)
        }
    }

    @Test fun verticalGestureFollowsBothDirectionsAndReturnsToTheSameEndpoints() {
        for (delta in listOf(-300f, 300f)) {
            assertEquals(0f, catalogContainerDragOffset(delta, 0f), 0f)
            assertEquals(0f, catalogContainerDragOffset(delta, 1f), 0f)
            assertTrue(catalogContainerDragOffset(delta, 0.5f) * delta > 0f)
            assertEquals(-catalogContainerDragOffset(delta, 0.5f),
                catalogContainerDragOffset(-delta, 0.5f), 0f)
        }
    }
}
