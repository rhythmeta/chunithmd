package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A new gesture cancels the spring without changing its last rendered geometry. */
internal class CatalogGridMotion(private val state: CatalogPhotoGridState, private val scope: CoroutineScope) {
    private var settling: Job? = null
    private var generation = 0

    fun interrupt() {
        generation++
        settling?.cancel()
        settling = null
        state.transforming = false
    }

    fun settle(anchor: PhotoGridAnchor?, target: Float = settledZoom(state.zoom)) {
        interrupt()
        if (anchor == null) return
        val targetZoom = target.coerceIn(1f, 2f)
        val initialZoom = state.zoom
        if (initialZoom == targetZoom && !state.outsideScrollBounds) return
        val initialCorrection = state.scrollOffset - state.anchoredScroll(initialZoom, anchor)
        val finalCorrection = state.settlingCorrection(targetZoom, anchor)
        val current = generation
        state.transforming = true
        settling = scope.launch {
            try {
                animate(0f, 1f, animationSpec = spring(dampingRatio = 1f, stiffness = 400f)) { progress, _ ->
                    state.transform(interpolate(initialZoom, targetZoom, progress), anchor,
                        scrollCorrection = interpolate(initialCorrection, finalCorrection, progress))
                }
                // Finish exactly inside the bounds; rounding must not leave edge fillers active.
                state.transform(targetZoom, anchor, scrollCorrection = finalCorrection)
                state.scrollBy(0f)
            } finally {
                if (current == generation) state.transforming = false
            }
        }
    }
}
