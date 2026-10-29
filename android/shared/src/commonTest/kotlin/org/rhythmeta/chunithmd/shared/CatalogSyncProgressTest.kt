package org.rhythmeta.chunithmd.shared

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogSyncProgressTest {
    @Test fun manifestTotalIsTheDenominatorForMultiItemStages() = runTest {
        val states = mutableListOf<CatalogSyncState>()
        val reporter = DownloadProgressReporter(
            stage = CatalogSyncStage.Downloading,
            totalItems = 2,
            message = "jackets",
            baselineBytes = 1_000,
            expectedTotalBytes = 3_000,
            onState = { states += it },
        )
        reporter.start()
        // Per-response lengths must never be summed into the published total: it would grow while
        // the download runs instead of describing the whole first-run download.
        reporter.onTransfer(0, 400)
        reporter.onTransfer(200, null)
        reporter.onItemCompleted()

        val state = states.last()
        assertEquals(3_000, state.totalBytes)
        assertEquals(1_200, state.downloadedBytes)
        assertEquals(1, state.completedItems)
        assertEquals(2, state.totalItems)
    }

    @Test fun singleResponseStageFallsBackToItsContentLength() = runTest {
        val states = mutableListOf<CatalogSyncState>()
        val reporter = DownloadProgressReporter(
            stage = CatalogSyncStage.Downloading,
            totalItems = 1,
            message = "bundle",
            progressByBytes = true,
            onState = { states += it },
        )
        reporter.start()
        reporter.onTransfer(0, 2_000)
        reporter.onTransfer(500, null)

        val state = states.last()
        assertEquals(2_000, state.totalBytes)
        assertEquals(500, state.downloadedBytes)
        assertEquals(0.25f, state.progress!!)
    }

    @Test fun stagesWithoutATrustedTotalPublishNone() = runTest {
        val states = mutableListOf<CatalogSyncState>()
        val reporter = DownloadProgressReporter(
            stage = CatalogSyncStage.Downloading,
            totalItems = 3,
            message = "jackets",
            onState = { states += it },
        )
        reporter.start()
        reporter.onTransfer(0, 900)
        reporter.onTransfer(120, null)

        val state = states.last()
        assertNull(state.totalBytes)
        assertEquals(120, state.downloadedBytes)
        assertEquals("jackets (0/3) · 120 B · 计算速度中", state.message)
    }

    @Test fun resumedRunsCountAlreadyInstalledBytesAndNeverExceedTheTotal() = runTest {
        val states = mutableListOf<CatalogSyncState>()
        val reporter = DownloadProgressReporter(
            stage = CatalogSyncStage.Downloading,
            totalItems = 1,
            message = "jackets",
            baselineBytes = 2_800,
            expectedTotalBytes = 3_000,
            onState = { states += it },
        )
        reporter.start()
        reporter.onTransfer(500, null)

        val state = states.last()
        // Retried downloads can report the same bytes twice, so the display clamps to the total.
        assertEquals(3_000, state.downloadedBytes)
        assertEquals(2_800L + 500L, reporter.transferredBytes())
        reporter.complete()
        assertEquals(1f, states.last().progress!!)
    }
}
