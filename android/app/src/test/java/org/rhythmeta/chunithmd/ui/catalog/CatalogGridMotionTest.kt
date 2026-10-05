package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class CatalogGridMotionTest {
    private class Frames : MonotonicFrameClock {
        private val frames = Channel<Pair<Long, CompletableDeferred<Unit>>>()
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            val (time, done) = frames.receive()
            return onFrame(time).also { done.complete(Unit) }
        }
        suspend fun advance(time: Long) = withTimeout(2000) {
            val done = CompletableDeferred<Unit>()
            frames.send(time to done)
            done.await()
        }
    }

    @Test fun boundaryCorrectionStartsAtCurrentFrameAndCanBeInterrupted() {
        val frames = Frames()
        runBlocking(frames) {
            val state = CatalogPhotoGridState(zoom = 2f)
            val ids = (0 until 2000).map(Int::toString)
            state.prepareLayout(390f, 800f, 2f, 100f, 96f, ids)
            val anchor = state.anchorAt(Offset(195f, 400f), centerOnCover = true)!!
            state.transform(1f, anchor)
            assertTrue(state.scrollOffset < 0f)
            val initial = state.scrollOffset
            val motion = CatalogGridMotion(state, this)
            try {
                motion.settle(anchor, 1f)
                frames.advance(1_000_000_000)
                assertEquals(initial, state.scrollOffset, 0.001f)
                frames.advance(1_032_000_000)
                assertTrue(state.scrollOffset > initial && state.scrollOffset < 0f)
                val interrupted = state.scrollOffset
                motion.interrupt()
                state.prepareLayout(390f, 800f, 2f, 100f, 96f, ids)
                assertEquals(interrupted, state.scrollOffset, 0f)
                motion.settle(state.anchorAt(anchor.point, centerOnCover = true), 1f)
                frames.advance(1_064_000_000)
                assertEquals(interrupted, state.scrollOffset, 0.001f)
                frames.advance(6_000_000_000)
                assertEquals(0f, state.scrollOffset, 0.001f)
                assertFalse(state.outsideScrollBounds)
                assertFalse(state.transforming)
            } finally { motion.interrupt() }
        }
    }

    @Test fun springCanBeInterruptedAndReversedWithoutJumpingToEitherEndpoint() {
        val frames = Frames()
        runBlocking(frames) {
            val state = CatalogPhotoGridState(100)
            state.prepareLayout(390f, 800f, 2f, 100f, 96f, (0 until 2000).map(Int::toString))
            val focus = Offset(160f, 400f)
            state.transform(1.4f, state.anchorAt(focus)!!)
            val motion = CatalogGridMotion(state, this)
            try {
                motion.settle(state.anchorAt(focus), 2f)
                frames.advance(1_000_000_000)
                frames.advance(1_032_000_000)
                val interruptedZoom = state.zoom
                val interruptedOffset = state.scrollOffset
                assertTrue(interruptedZoom > 1.4f && interruptedZoom < 2f)
                motion.interrupt()
                assertEquals(interruptedZoom, state.zoom, 0f)
                assertEquals(interruptedOffset, state.scrollOffset, 0f)
                assertFalse(state.transforming)
                motion.settle(state.anchorAt(focus), 1f)
                frames.advance(1_064_000_000)
                assertEquals(interruptedZoom, state.zoom, 0.0001f)
                frames.advance(1_096_000_000)
                assertTrue(state.zoom < interruptedZoom && state.zoom > 1f)
            } finally { motion.interrupt() }
        }
    }
}
